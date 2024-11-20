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

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import tk.hack5.treblecheck.data.TrebleDetector

@RunWith(Parameterized::class)
class ValidateCompatibilityTest(private val testName: String, private val vendorSku: String, private val hardwareSku: String, private val sepolicyVersion: Pair<Int, Int>, private val expected: Set<Int>) {
    companion object {
        @Suppress("BooleanLiteralArgument")
        @Parameterized.Parameters
        @JvmStatic
        fun data() = listOf(
            arrayOf("vndk1a", "vendorSku", "hardwareSku", 30 to 0, setOf(5, 6)),
            arrayOf("vndk3a", "vendorSku", "hardwareSku", 32 to 0, setOf(5, 6)),
            arrayOf("vndk4a", "vendorSku", "hardwareSku", 30 to 0, setOf(5, 6)),
            arrayOf("vndk5a", "vendorSku", "hardwareSku", 27 to 0, setOf<Int>()),
            arrayOf("vndk6a", "vendorSku", "hardwareSku", 27 to 0, setOf(0, 1, 2)),
        )
    }

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun extractFiles(name: String) {
        println("Extracting $name $vendorSku $hardwareSku")

        extractFiles(name, allFiles(vendorSku, hardwareSku), temporaryFolder)
    }

    @Test
    fun checkValidateCompatibility() {
        extractFiles(testName)

        TrebleDetector.root = temporaryFolder.root
        val (matrices, maxLevel) = TrebleDetector.getFrameworkCompatibilityMatrices(sepolicyVersion)
        assertEquals(7, maxLevel)
        matrices.forEachIndexed { i, matrix ->
            val result = TrebleDetector.checkCompatibilityMatrix(
                matrix,
                vendorSku,
                hardwareSku
            )
            assertEquals("matrix $i", if (i in expected) 1 else 0, result)
        }
    }
}
