/*
 *     Treble Info
 *     Copyright (C) 2022-2023 Hackintosh Five
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

package tk.hack5.treblecheck

import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import tk.hack5.treblecheck.data.ParseException
import tk.hack5.treblecheck.data.TrebleDetector
import tk.hack5.treblecheck.data.TrebleResult
import java.io.File
import kotlin.reflect.KClass

typealias AnswerScope<T> = MockKAnswerScope<T, T>.(Call) -> T

sealed class Result<T> {
    companion object {
        fun <T> success(result: T) = Success(result)
        inline fun <T, reified E : Throwable> failure() = Failure<T, E>(E::class)
    }

    data class Success<T>(val value: T) : Result<T>()
    data class Failure<T, E : Throwable>(val throwableClass: KClass<E>) : Result<T>()

    fun fold(onSuccess: (T) -> Unit, onFailure: (KClass<out Throwable>) -> Unit) {
        when (this) {
            is Success<T> -> onSuccess(value)
            is Failure<*, *> -> onFailure(throwableClass)
        }
    }
}


@RunWith(Parameterized::class)
class TrebleDetectorTest(
    private val result: Result<TrebleResult?>,
    private val trebleEnabled: String?,
    private val vndkLite: String?,
    private val vendorSku: String?,
    private val hardwareSku: String?,
    private val vndkVersion: String?,
    private val matchesMatrices: Int?,
    private val testName: String
) {
    companion object {
        @Suppress("BooleanLiteralArgument")
        @Parameterized.Parameters
        @JvmStatic
        // vndk1a is cepheus on MIUI
        // vndk2a is TP1803 with an old ROM
        // vndk3a is TP1803 on LOS 18.1(?)
        // vndk4a is crosshatch-user-11-RQ2A.210305.006-7119741-release-keys
        // vndk5a is bullhead-user-8.1.0-OPM7.181205.001-5080180-release-keys
        fun data() = listOf(
            // data-free tests
            arrayOf(Result.failure<Nothing?, ParseException>(), "", "", null, null, null, null, ""),
            arrayOf(Result.failure<Nothing?, ParseException>(), "false", "", null, null, null, null, ""),
            arrayOf(
                Result.success(TrebleResult(false, false, false, 30, 0)),
                "true",
                "",
                null,
                null,
                "30",
                null,
                ""
            ),
            arrayOf(
                Result.failure<Nothing?, ParseException>(),
                "true",
                "false",
                null,
                null,
                null,
                null,
                ""
            ),
            // tests with cepheus data
            arrayOf(
                Result.success(TrebleResult(false, false, false, 30, 0)),
                "true",
                "false",
                "",
                "",
                "30",
                1,
                "vndk1a"
            ),
            arrayOf(
                Result.success(TrebleResult(false, true, false, 30, 0)),
                "true",
                "true",
                "",
                "",
                null,
                1,
                "vndk1b"
            ),
            arrayOf(
                Result.success(TrebleResult(false, false, false, 30, 0)),
                "true",
                "false",
                "",
                "",
                null,
                1,
                "vndk1c"
            ),
            arrayOf(
                Result.success(TrebleResult(false, true, false, 30, 0)),
                "true",
                "true",
                "",
                "",
                null,
                1,
                "vndk1d"
            ),
            arrayOf(
                Result.success(TrebleResult(false, false, false, 30, 0)),
                "true",
                "false",
                "",
                "",
                null,
                1,
                "vndk1e"
            ),
            // tests with TP1803 data
            arrayOf(
                Result.success(TrebleResult(false, true, false, 30, 0)),
                "true",
                "true",
                "",
                "",
                "30",
                1,
                "vndk2a"
            ),
            arrayOf(
                Result.success(TrebleResult(false, false, false, 30, 0)),
                "true",
                "false",
                "",
                "",
                null,
                1,
                "vndk2b"
            ),
            arrayOf(
                Result.success(TrebleResult(false, true, false, 30, 0)),
                "true",
                "true",
                "",
                "",
                null,
                1,
                "vndk2c"
            ),
            arrayOf(
                Result.success(TrebleResult(false, false, false, 30, 0)),
                "true",
                "false",
                "",
                "",
                null,
                1,
                "vndk2d"
            ),
            arrayOf(
                Result.success(TrebleResult(false, true, false, 30, 0)),
                "true",
                "true",
                "",
                "",
                null,
                1,
                "vndk2e"
            ),
            arrayOf(
                Result.success(TrebleResult(false, false, false, 32, 0)),
                "true",
                "false",
                "",
                "",
                null,
                1,
                "vndk3a"
            ),
            arrayOf(
                Result.success(TrebleResult(false, false, false, 32, 0)),
                "true",
                "false",
                "",
                "",
                null,
                1,
                "vndk3b"
            ),
            arrayOf(
                Result.success(TrebleResult(false, false, false, 32, 0)),
                "true",
                "false",
                "",
                "",
                null,
                1,
                "vndk3c"
            ),
            // crosshatch
            arrayOf(
                Result.success(TrebleResult(false, false, false, 30, 0)),
                "true",
                "false",
                "",
                "",
                null,
                1,
                "vndk4a"
            ),
            arrayOf(
                Result.success(TrebleResult(false, false, false, 30, 0)),
                "true",
                "false",
                "",
                "",
                null,
                1,
                "vndk4b"
            ),
            arrayOf(
                Result.success(TrebleResult(false, false, false, 30, 0)),
                "true",
                "false",
                "",
                "",
                null,
                1,
                "vndk4c"
            ),
            arrayOf(
                Result.success(TrebleResult(false, false, false, 30, 0)),
                "true",
                "false",
                "",
                "",
                null,
                1,
                "vndk4d"
            ),
            arrayOf(
                Result.success(TrebleResult(false, false, false, 30, 0)),
                "true",
                "false",
                "",
                "sku",
                null,
                1,
                "vndk4e"
            ),
            arrayOf(
                Result.success(TrebleResult(false, false, false, 30, 0)),
                "true",
                "false",
                "sku",
                "",
                null,
                1,
                "vndk4f"
            ),
            arrayOf(
                Result.success(TrebleResult(true, false, false, 30, 0)),
                "true",
                "false",
                "",
                "",
                null,
                1,
                "vndk4g"
            ),
            arrayOf(
                Result.success(TrebleResult(false, false, false, 30, 0)),
                "true",
                "false",
                "",
                "",
                null,
                1,
                "vndk4h"
            ),
            arrayOf(
                Result.success(TrebleResult(false, false, false, 30, 0)),
                "true",
                "false",
                "",
                "sku",
                null,
                1,
                "vndk4i"
            ),
            arrayOf(
                Result.success(TrebleResult(false, false, false, 30, 0)),
                "true",
                "false",
                "",
                "",
                null,
                1,
                "vndk4j"
            ),
            // bullhead
            arrayOf(
                Result.failure<Nothing?, ParseException>(),
                "false",
                "false",
                "",
                "",
                null,
                null,
                "vndk5a"
            ),
            arrayOf(
                Result.success(null),
                "false",
                "false",
                "",
                "",
                null,
                0,
                "vndk5a"
            ),
            // cph1823
            arrayOf(
                Result.failure<Nothing?, ParseException>(),
                "false",
                "false",
                "",
                "",
                null,
                null,
                "vndk6a"
            ),
            arrayOf(
                Result.success(TrebleResult(true, false, false, 27, 0)),
                "false",
                "false",
                "",
                "",
                null,
                1,
                "vndk6a"
            ),
        )
    }

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun extractFiles(name: String, vendorSku: String, hardwareSku: String) {
        println("Extracting $name $vendorSku $hardwareSku")

        extractFiles(name, allFiles(vendorSku, hardwareSku), temporaryFolder)
    }

    @Test
    fun getVndkData() {
        val function = if (testName.isEmpty()) {
            {
                testGetVndkData(
                    trebleEnabled,
                    vndkLite,
                    vendorSku,
                    hardwareSku,
                    { emptyList<File>() to false },
                    { null },
                    { null },
                    { -2 },
                    vndkVersion
                )
            }
        } else {
            extractFiles(testName, vendorSku!!, hardwareSku!!);
            {
                testGetVndkData(
                    trebleEnabled,
                    vndkLite,
                    vendorSku,
                    hardwareSku,
                    { callOriginal() },
                    { callOriginal() },
                    { callOriginal() },
                    { matchesMatrices ?: throw UnsatisfiedLinkError() },
                    vndkVersion
                )
            }
        }
        result.fold(
            { expected ->
                assertEquals(expected, function())
            },
            { expectedThrowable ->
                assertThrows(expectedThrowable.java) { function() }
            }
        )
    }

    private fun testGetVndkData(
        trebleEnabled: String?,
        vndkLite: String?,
        vendorSku: String?,
        hardwareSku: String?,
        manifestFiles: AnswerScope<Pair<List<File>, Boolean>>,
        vendorCompatibilityMatrix: AnswerScope<File?>,
        selinuxData: AnswerScope<Pair<Int, Int>?>,
        compatibilityResult: AnswerScope<Int>,
        vndkVersion: String?
    ): TrebleResult? {
        var ret: TrebleResult? = null
        TrebleDetector.root = temporaryFolder.root
        mockkStatic(::propertyGet.declaringKotlinFile) {
            every { propertyGet("ro.treble.enabled") } returns trebleEnabled
            every { propertyGet("ro.vndk.lite") } returns vndkLite
            every { propertyGet("ro.vndk.version") } returns vndkVersion
            every { propertyGet("ro.boot.product.vendor.sku") } returns vendorSku
            every { propertyGet("ro.boot.product.hardware.sku") } returns hardwareSku
            mockkObject(TrebleDetector) {
                every { TrebleDetector.locateManifestFiles() } answers manifestFiles
                every { TrebleDetector.locateVendorCompatibilityMatrix() } answers vendorCompatibilityMatrix
                every { TrebleDetector.parseSelinuxData() } answers selinuxData
                every { TrebleDetector.checkCompatibilityMatrix(any(), any(), any()) } answers compatibilityResult
                ret = TrebleDetector.getVndkData()
            }
        }
        return ret
    }
}


@RunWith(Parameterized::class)
class ParseMatrixTest(private val testName: String, private val matrixPath: String, private val expected: Pair<Int, Int>?) {
    companion object {
        @Suppress("BooleanLiteralArgument")
        @Parameterized.Parameters
        @JvmStatic
        fun data() = listOf(
            arrayOf("vndk1a", "vendor/etc/vintf/manifest.xml", null),
            arrayOf("vndk1a", "vendor/etc/vintf/compatibility_matrix.xml", 30 to 0),
            arrayOf("vndk1a", "odm/etc/vintf/manifest.xml", null),
            arrayOf("vndk2a", "vendor/etc/vintf/manifest.xml", null),
            arrayOf("vndk2a", "vendor/etc/vintf/compatibility_matrix.xml", 30 to 0),
            arrayOf("vndk2a", "odm/etc/vintf/manifest.xml", null),
        )
    }

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun extractFile(name: String, path: String) {
        val files = mapOf(
            path to null
        )

        extractFiles(name, files, temporaryFolder)
    }

    @Test
    fun parseMatrix() {
        extractFile(testName, matrixPath)
        assertEquals(expected, TrebleDetector.parseMatrix(temporaryFolder.root.resolve(matrixPath)))
    }

}


@RunWith(Parameterized::class)
class ParseManifestTest(private val testName: String, private val manifestPath: String, private val expected: Triple<Int, Set<String>, String?>) {
    companion object {
        @Suppress("BooleanLiteralArgument")
        @Parameterized.Parameters
        @JvmStatic
        fun data() = listOf(
            arrayOf("vndk1a", "vendor/etc/vintf/manifest.xml", Triple(30 to 0, emptySet<String>(), "3")),
            arrayOf("vndk1a", "vendor/etc/vintf/compatibility_matrix.xml", Triple(null, emptySet<String>(), null)),
            arrayOf("vndk1a", "odm/etc/vintf/manifest.xml", Triple(null, emptySet<String>(), "3")),
            arrayOf("vndk2a", "vendor/etc/vintf/manifest.xml", Triple(30 to 0, emptySet<String>(), "5")),
            arrayOf("vndk2a", "vendor/etc/vintf/compatibility_matrix.xml", Triple(null, emptySet<String>(), null)),
            arrayOf("vndk2a", "odm/etc/vintf/manifest.xml", Triple(null, emptySet<String>(), "5")),
            // TODO check passthrough results (non-blocker as not yet displayed in UI)
        )
    }

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun extractFile(name: String, path: String) {
        val files = mapOf(
            path to null
        )

        extractFiles(name, files, temporaryFolder)
    }

    @Test
    fun parseManifest() {
        extractFile(testName, manifestPath)
        assertEquals(expected, TrebleDetector.parseManifest(temporaryFolder.root.resolve(manifestPath)))
    }
}


@RunWith(Parameterized::class)
class ParseVersionTests(private val expected: Pair<Int, Int>?, private val input: String) {
    companion object {
        @Suppress("BooleanLiteralArgument")
        @Parameterized.Parameters
        @JvmStatic
        fun data() = listOf(
            arrayOf(30 to 1, "30.1"),
            arrayOf(30 to 1, "\n\n 30.1\u00A0 "),
            arrayOf(30 to 0, "30"),
            arrayOf(3 to 0, "3.a"),
            arrayOf(null, "3b"),
            arrayOf(3 to 0, "3.\u00A0e"),
            arrayOf(null, "-3"),
            arrayOf(1 to 0, "1.+3"),
            arrayOf(null, "+1.+3"),
            arrayOf(null, "\u0DEF")

        )
    }
    @Test
    fun parseVersion() {
        assertEquals(expected, TrebleDetector.parseVersion(input))
    }
}
