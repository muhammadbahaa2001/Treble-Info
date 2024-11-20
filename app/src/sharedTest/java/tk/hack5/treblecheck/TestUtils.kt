/*
 *     Treble Info
 *     Copyright (C) 2023 Hackintosh Five
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

package tk.hack5.treblecheck

import org.junit.rules.TemporaryFolder

fun Any.extractFiles(name: String, files: Map<String, String?>, temporaryFolder: TemporaryFolder) {
    temporaryFolder.delete()
    val classLoader = this::class.java.classLoader!!

    for (file in files.entries) {
        if (file.key.endsWith('/')) {
            var i = 0
            children@ while (true) {
                classLoader.getResourceAsStream("$name/${file.key}$i")?.use { sourceStream ->
                    val destFile = temporaryFolder.root.resolve(file.key).resolve(i.toString() + file.value!!)
                    destFile.parentFile!!.mkdirs()
                    destFile.outputStream().use { destStream ->
                        sourceStream.copyTo(destStream)
                    }
                    i++
                } ?: break
            }
        } else {
            val sourceStream = classLoader.getResourceAsStream("$name/${file.key}") ?: continue
            val destFile = temporaryFolder.root.resolve(file.value ?: file.key)
            destFile.parentFile!!.mkdirs()
            destFile.outputStream().use {
                sourceStream.copyTo(it)
            }
        }
    }
}

fun allFiles(vendorSku: String, productSku: String) = mapOf(
    "vendor/etc/vintf/manifest_sku.xml" to "vendor/etc/vintf/manifest_$vendorSku.xml",
    "vendor/etc/vintf/manifest.xml" to null,
    "vendor/etc/vintf/manifest/" to ".xml",
    "vendor/manifest.xml" to null,
    "odm/etc/vintf/manifest_sku.xml" to "odm/etc/vintf/manifest_$productSku.xml",
    "odm/etc/vintf/manifest.xml" to null,
    "odm/etc/manifest_sku.xml" to "odm/etc/manifest_$productSku.xml",
    "odm/etc/manifest.xml" to null,
    "odm/etc/vintf/manifest/" to ".xml",
    "vendor/etc/vintf/compatibility_matrix.xml" to null,
    "vendor/compatibility_matrix.xml" to null,
    "vendor/etc/selinux/" to ".cil",
    "vendor/etc/selinux/plat_sepolicy_vers.txt" to null
)