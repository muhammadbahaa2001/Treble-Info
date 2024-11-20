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

import io.mockk.declaringKotlinFile
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import tk.hack5.treblecheck.data.ABDetector

@RunWith(Parameterized::class)
class ABDetectorTest(private val expected: Boolean?, private val slotSuffix: String?) {
    companion object {
        @Suppress("BooleanLiteralArgument")
        @Parameterized.Parameters
        @JvmStatic
        fun data() = listOf(
            arrayOf(false, ""),
            arrayOf(true, "a"),
            arrayOf(true, "b"),
            arrayOf(true, "weird-suffix 1.40$"),
            arrayOf<Any?>(null, null),
        )
    }


    @Test
    fun checkAB() = mockkStatic(::propertyGet.declaringKotlinFile) {
        every { propertyGet("ro.boot.slot_suffix") } returns slotSuffix
        assertEquals(expected, ABDetector.checkAB())
    }
}