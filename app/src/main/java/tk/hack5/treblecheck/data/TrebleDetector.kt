/*
 *     Treble Info
 *     Copyright (C) 2019-2023 Hackintosh Five
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
// SPDX-License-Identifier: GPL-3.0-or-later

package tk.hack5.treblecheck.data

import android.util.Log
import androidx.annotation.Keep
import androidx.annotation.VisibleForTesting
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import tk.hack5.treblecheck.*
import java.io.File

data class TrebleResult(val legacy: Boolean, val lite: Boolean, val upgradeCompliant: Boolean,
                        val vndkVersion: Int, val vndkSubVersion: Int)


enum class PassthroughResult {
    FULLY_COMPLIANT,
    UPGRADE_COMPLIANT,
    NOT_COMPLIANT,
}

object TrebleDetector {
    private val SELINUX_REGEX = Regex("""\Winit_(\d+)_(\d+)\W""")

    // https://source.android.com/docs/core/architecture/hal
    private val LEGACY_REQUIRED_BINDERIZED = setOf("android.hardware.biometrics.fingerprint", "android.hardware.configstore", "android.hardware.dumpstate", "android.hardware.graphics.allocator", "android.hardware.radio", "android.hardware.usb", "android.hardware.wifi", "android.hardware.wifi.supplicant")
    private val ALLOWED_PASSTHROUGHS = setOf("android.hardware.graphics.mapper", "android.hardware.renderscript")

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var root: File? = null

    fun getVndkData(): TrebleResult? {
        Mock.data?.let { return it.treble.get() }

        var supportsTreble = false

        val vndkVersionProp = propertyGet("ro.vndk.version")?.let {
            Log.v(tag, "vndk prop: $it")
            parseVersion(it)
        }?.also {
            Log.v(tag, "vndk prop result: $it")
            supportsTreble = true
        }

        propertyGet("ro.treble.enabled")?.let {
            Log.v(tag, "trebleEnabled prop: $it")
            if (parseBool(it) == true) {
                supportsTreble = true
            }
        }

        val liteProp = propertyGet("ro.vndk.lite")
        Log.v(tag, "lite: $liteProp")
        if (liteProp == null) {
            throw ParseException("Can't check lite status")
        }
        val lite = parseBool(liteProp) ?: false
        if (lite) {
            supportsTreble = true
        }

        val (manifests, legacy) = locateManifestFiles()
        Log.v(tag, "manifests: ${manifests.joinToString { it.absolutePath }}, legacy: $legacy")


        var targetLevel: Int? = null
        val (versions, passthroughs) = manifests
            .asSequence()
            .map {
                parseManifest(it)
                    .let { res ->
                        Log.v(tag, "manifest ${it.absolutePath}: $res")
                        targetLevel = targetLevel ?: res.third?.toIntOrNull()
                        res.first to res.second
                    }
            }
            .unzip()

        val version = versions.filterNotNull().firstOrNull()

        if (version != null) {
            when (checkCompatibilityMatrix(targetLevel, version)) {
                false -> {
                    if (supportsTreble) {
                        throw ParseException("Device claims Treble support but fails compatibility checks with AOSP matrices")
                    } else {
                        return null
                    }
                }
                true -> supportsTreble = true
                else -> { }
            }
        }

        val upgradePassthrough = when (checkPassthroughs(passthroughs.flatten().toSet())) {
            PassthroughResult.NOT_COMPLIANT -> {
                // they claim Treble support but are non-compliant
                if (supportsTreble) {
                    throw ParseException("Device reports Treble compliance but does not meet VNDK requirements")
                }
                return null
            }
            PassthroughResult.UPGRADE_COMPLIANT -> {
                Log.d(tag, "VNDK upgrade compliant")
                true
            }
            PassthroughResult.FULLY_COMPLIANT -> {
                // if we fail to read any manifests, we certainly won't find any passthrough HALs, so this is the default case
                false
            }
        }

        vndkVersionProp?.let {
            return TrebleResult(legacy, lite, upgradePassthrough, it.first, it.second)
        }

        version?.let {
            if (supportsTreble) {
                return TrebleResult(legacy, lite, upgradePassthrough, it.first, it.second)
            } else {
                // checkCompatibilityMatrix is unavailable, no props support existence of Treble.
                // The only case where the device can be Treble is VNDK <= 27 running non-GSI system.
                // In that case, we need more confirmation...
                if (version >= 28 to 0) {
                    return null
                }
                Log.w(tag, "Manifest contains sepolicy version but support unconfirmed")
            }
        }

        val matrix = locateVendorCompatibilityMatrix()
        matrix?.let {
            Log.v(tag, "matrix: ${matrix.absolutePath}")
            parseMatrix(it)
        }?.let {
            Log.v(tag, "vendor matrix result: $it")
            if (supportsTreble) {
                return TrebleResult(legacy, lite, upgradePassthrough, it.first, it.second)
            } else {
                // This should never happen on Treble devices as (VNDK <= 27 intersect DCM version != 0) should be empty.
                // But if it does happen, we can treat it as proof
                Log.w(tag, "Unexpectedly found matrix sepolicy version on (VNDK <= 27 OR missing manifest version)")
                if (version != null) {
                    if (version == it) {
                        return TrebleResult(legacy, lite, upgradePassthrough, it.first, it.second)
                    } else {
                        throw ParseException("Found differing versions in manifest ($version) and matrix ($it)")
                    }
                }
            }
        }

        parseSelinuxData()?.let {
            Log.v(tag, "selinux result: $it")
            if (supportsTreble) {
                return TrebleResult(legacy, lite, upgradePassthrough, it.first, it.second)
            }
        }

        throw ParseException("No method to detect version ($supportsTreble)")
    }

    private fun checkPassthroughs(passthroughs: Set<String>): PassthroughResult {
        if ((passthroughs - ALLOWED_PASSTHROUGHS).none { it.startsWith("android.") }) {
            return PassthroughResult.FULLY_COMPLIANT
        }
        if (passthroughs.intersect(LEGACY_REQUIRED_BINDERIZED).none { it.startsWith("android.") }) {
            return PassthroughResult.UPGRADE_COMPLIANT
        }
        return PassthroughResult.NOT_COMPLIANT
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun getFrameworkCompatibilityMatrices(sepolicyVersion: Pair<Int, Int>) = sequence {
        // Although SP usually contains a valid FCM,
        // we could be running a device-specific image which has a weird FCM,
        // or it could be older than vendor.
        // Therefore, we don't fallback to SP, even when we don't have a matching FCM.
        arrayOf(
            "compatibility_matrix.legacy.xml",
            "compatibility_matrix.1.xml",
            "compatibility_matrix.2.xml",
            "compatibility_matrix.3.xml",
            "compatibility_matrix.4.xml",
            "compatibility_matrix.5.xml",
            "compatibility_matrix.6.xml",
            "compatibility_matrix.7.xml",
        ).forEach { name ->
            val stream = this::class.java.classLoader!!.getResourceAsStream(name)
            require(stream != null) { "$name not found" }
            val original = stream.bufferedReader().readText()

            val insertIndex = original.lastIndexOf("</compatibility-matrix>")
            yield(original.substring(0, insertIndex) +
                    "<sepolicy><kernel-sepolicy-version>0</kernel-sepolicy-version><sepolicy-version>${sepolicyVersion.first}.${sepolicyVersion.second}</sepolicy-version></sepolicy>" +
                    original.substring(insertIndex))
        }
    } to 7

    private fun checkCompatibilityMatrix(level: Int?, sepolicyVersion: Pair<Int, Int>): Boolean? {
        val (matrices, maxLevel) = getFrameworkCompatibilityMatrices(sepolicyVersion)
        for (matrix in matrices) {
            val result = try {
                checkCompatibilityMatrix(matrix, propertyGet("ro.boot.product.vendor.sku") ?: "", propertyGet("ro.boot.product.hardware.sku") ?: "")
            } catch (e: UnsatisfiedLinkError) {
                Log.w(tag, "Native library unavailable", e)
                return null
            }
            return when (result) {
                0 -> continue
                1 -> true
                // error already logged in native code
                -1 -> null // failed to parse built-in manifest, should never happen
                -2 -> if (level != null && level > maxLevel) null else false // device manifest load failed, so it can't support Treble unless it is newer than us and we don't recognise it properly
                else -> throw ParseException("Unknown return value from check_compatibility_matrix")
            }
        }
        return if (level != null && level > maxLevel) null else false // nothing matched
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun parseMatrix(matrix: File) = parseXml(matrix) { xpp ->
        val versions = mutableListOf<String>()
        val versionBuilder = StringBuilder(2) // 2 is the normal size of the version number, 'xy'

        var inVndkTag = false
        var inVersionTag = false
        var event = xpp.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                if (xpp.name == "vendor-ndk") {
                    inVndkTag = true
                } else if (inVndkTag && xpp.name == "version") {
                    inVersionTag = true
                }
            } else if (event == XmlPullParser.END_TAG) {
                if (inVersionTag) {
                    inVersionTag = false
                    versions += versionBuilder.toString()
                    versionBuilder.clear()
                } else if (inVndkTag) {
                    break
                }
            } else if (event == XmlPullParser.TEXT && inVersionTag) {
                // This is the version number
                versionBuilder.append(xpp.text.trim())
            }
            xpp.next()
            event = xpp.eventType
        }
        bestVersion(versions)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun parseManifest(manifest: File) = parseXml(manifest) { xpp ->
        val versions = mutableListOf<String>()
        val passthroughs = mutableSetOf<String>()

        val versionBuilder = StringBuilder(4) // 2 is the normal size of the version number, 'xy'

        // https://source.android.com/docs/core/architecture/vintf/objects#manifest-file-schema
        val nameBuilder = StringBuilder(64)
        val transportBuilder = StringBuilder(11)

        var targetLevel: String? = null

        var inSepolicyTag = false
        var inVersionTag = false

        var inHalTag = false
        var inNameTag = false
        var inTransportTag = false

        var event = xpp.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (xpp.name) {
                        "manifest" -> targetLevel = xpp.getAttributeValue(null, "target-level")

                        "sepolicy" -> inSepolicyTag = true
                        "version" -> inVersionTag = inSepolicyTag

                        "hal" -> inHalTag = true
                        "name" -> inNameTag = inHalTag
                        "transport" -> inTransportTag = inHalTag
                    }
                }
                XmlPullParser.END_TAG -> {
                    when {
                        inVersionTag -> {
                            inVersionTag = false
                            versions += versionBuilder.toString()
                            versionBuilder.clear()
                        }
                        inSepolicyTag -> inSepolicyTag = false

                        inNameTag -> inNameTag = false
                        inTransportTag -> inTransportTag = false
                        inHalTag -> {
                            inHalTag = false
                            if (transportBuilder.toString() == "passthrough") {
                                passthroughs += nameBuilder.toString()
                            }
                            nameBuilder.clear()
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    when {
                        inSepolicyTag && inVersionTag -> versionBuilder.append(xpp.text.trim())

                        inHalTag && inNameTag -> nameBuilder.append(xpp.text.trim())
                        inHalTag && inTransportTag -> transportBuilder.append(xpp.text.trim())
                    }
                }
            }

            xpp.next()
            event = xpp.eventType
        }
        Triple(bestVersion(versions), passthroughs, targetLevel)
    }

    private fun <T>parseXml(file: File, block: (XmlPullParser) -> T): T =
        file.inputStream().use { inputStream ->
            inputStream.reader().use { reader ->
                val factory = XmlPullParserFactory.newInstance()
                factory.isNamespaceAware = false
                val xpp = factory.newPullParser().apply {
                    setInput(reader)
                }

                block(xpp)
            }
        }

    private fun bestVersion(versions: List<String>): Pair<Int, Int>? {
        return versions
            .mapNotNull { parseVersion(it) }
            .maxWithOrNull { left, right -> left.compareTo(right) }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun parseVersion(string: String): Pair<Int, Int>? {
        val split = string.split('.').map(String::trim)
        if (split.isNotEmpty() && split.all { it == "0" }) {
            return null
        }
        if (split.size != 1 && split.size != 2) {
            return null
        }
        if (split[0].any { it !in '0'..'9' } || split[0].isEmpty()) {
            // ASCII only by design
            return null
        }
        val first = split[0].toInt(10)
        if (split.size == 1 || split[1].any { it !in '0'..'9' } || split[1].isEmpty()) {
            // ASCII only by design
            return first to 0
        }
        val second = split[1].toInt(10)
        return first to second
    }

    /**
     * See https://cs.android.com/android/platform/superproject/+/master:system/libvintf/VintfObject.cpp;l=289;drc=0a1c02083dbd6e23f074069ebf45a87ec0757f30
     * 1. Get vendor manifest. Iff found, add vendor fragments.
     * 2. Add ODM manifest if available.
     * 3. Iff vendor manifest was found, add ODM fragments.
     * 4. Iff nothing found so far, use legacy manifest.
     * @return files to legacy
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun locateManifestFiles(): Pair<List<File>, Boolean> {
        val ret = mutableListOf<File>()
        val foundVendorManifest = locateVendorManifest(propertyGet("ro.boot.product.vendor.sku"))?.let {
            ret += it
            ret += locateVendorManifestFragments()
        } != null
        locateOdmManifest(propertyGet("ro.boot.product.hardware.sku"))?.let {
            ret += it
        }
        if (foundVendorManifest) {
            ret += locateOdmManifestFragments()
        }
        if (ret.isEmpty()) {
            locateLegacyManifest()?.let {
                return listOf(it) to true
            }
        }
        return ret to false
    }

    /**
     * See https://cs.android.com/android/platform/superproject/+/master:system/libvintf/VintfObject.cpp;l=343;drc=0a1c02083dbd6e23f074069ebf45a87ec0757f30
     */
    private fun locateVendorManifest(sku: String?): File? {
        if (!sku.isNullOrEmpty()) {
            File(root, "/vendor/etc/vintf/manifest_$sku.xml").let {
                if (it.exists()) {
                    return if (it.canRead()) {
                        it
                    } else {
                        Log.w(tag, "Cannot read ${it.path}")
                        null
                    }
                }
            }
        } else {
            File(root, "/vendor/etc/vintf/manifest.xml").let {
                if (it.exists()) {
                    return if (it.canRead()) {
                        it
                    } else {
                        Log.w(tag, "Cannot read ${it.path}")
                        null
                    }
                }
            }
        }
        return null
    }

    private fun locateVendorManifestFragments(): List<File> {
        val dir = File(root, "/vendor/etc/vintf/manifest")
        return (dir.listFiles() ?: return emptyList()).filter { it.canRead() }
    }

    /**
     * See https://cs.android.com/android/platform/superproject/+/master:system/libvintf/VintfObject.cpp;l=375;drc=0a1c02083dbd6e23f074069ebf45a87ec0757f30
     */
    private fun locateOdmManifest(sku: String?): File? {
        if (!sku.isNullOrEmpty()) {
            File(root, "/odm/etc/vintf/manifest_$sku.xml").let {
                if (it.exists()) {
                    return if (it.canRead()) {
                        it
                    } else {
                        Log.w(tag, "Cannot read ${it.path}")
                        null
                    }
                }
            }
        } else {
            File(root, "/odm/etc/vintf/manifest.xml").let {
                if (it.exists()) {
                    return if (it.canRead()) {
                        it
                    } else {
                        Log.w(tag, "Cannot read ${it.path}")
                        null
                    }
                }
            }
        }
        // legacy locations, treated as normal though
        if (!sku.isNullOrEmpty()) {
            File(root, "/odm/etc/manifest_$sku.xml").let {
                if (it.exists()) {
                    return if (it.canRead()) {
                        it
                    } else {
                        Log.w(tag, "Cannot read ${it.path}")
                        null
                    }
                }
            }
        } else {
            File(root, "/odm/etc/manifest.xml").let {
                if (it.exists()) {
                    return if (it.canRead()) {
                        it
                    } else {
                        Log.w(tag, "Cannot read ${it.path}")
                        null
                    }
                }
            }
        }
        return null
    }

    private fun locateOdmManifestFragments(): List<File> {
        val dir = File(root, "/odm/etc/vintf/manifest")
        return (dir.listFiles() ?: return emptyList()).toList()
    }

    private fun locateLegacyManifest(): File? {
        File(root, "/vendor/manifest.xml").let {
            if (it.exists() && it.canRead())
                return it
        }
        return null
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun locateVendorCompatibilityMatrix(): File? {
        File(root, "/vendor/etc/vintf/compatibility_matrix.xml").let {
            if (it.exists() && it.canRead()) {
                return it
            }
        }
        /*
         * /vendor/compatibility_matrix.xml -> <vndk> does not contain the Android version; it is always 0.0.0 (only present in VNDK 27)
         * We could use this to detect VNDK 27 but we will just ignore it.

        File(root, "/vendor/compatibility_matrix.xml").let {
            if (it.exists() && it.canRead()) {
                return it
            }
        }
         */
        return null
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun parseSelinuxData(): Pair<Int, Int>? {
        // https://android.googlesource.com/platform/system/core/+/refs/tags/android-12.1.0_r2/init/selinux.cpp#281
        val sepolicyVersionFile = File(root, "/vendor/etc/selinux/plat_sepolicy_vers.txt")
        if (sepolicyVersionFile.exists()) {
            return parseVersion(sepolicyVersionFile.bufferedReader().readLine())
        }

        val files = File(root, "/vendor/etc/selinux/").listFiles { it -> it.canRead() && it.extension == "cil" }
        return files?.let { parseSelinuxData(it) }
    }

    private fun parseSelinuxData(files: Array<File>): Pair<Int, Int>? {
        var version = Pair(-1, -1)

        files.forEach { file ->
            file.bufferedReader().lineSequence().forEach { line ->
                SELINUX_REGEX.findAll(line).forEach { match ->
                    Pair(match.groupValues[1].toInt(), match.groupValues[2].toInt()).let {
                        if (it > version) version = it
                    }
                }
            }
        }
        if (version <= Pair(0, 0)) return null
        return version
    }


    private var loaded = false

    @Synchronized
    private fun ensureLoaded() {
        if (!loaded) {
            System.loadLibrary("trebledetector")
            loaded = true
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun checkCompatibilityMatrix(matrix: String, vendorSku: String, hardwareSku: String): Int {
        ensureLoaded()
        return checkCompatibilityMatrixNative(matrix, root?.absolutePath ?: "", vendorSku, hardwareSku)
    }

    @Keep
    @JvmName("check_compatibility_matrix")
    private external fun checkCompatibilityMatrixNative(matrix: String, root: String, vendorSku: String, hardwareSku: String): Int
}

private const val tag = "TrebleDetector"