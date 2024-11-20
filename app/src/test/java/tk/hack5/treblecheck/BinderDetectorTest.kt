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

import io.mockk.every
import io.mockk.mockkObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import tk.hack5.treblecheck.data.BinderArch
import tk.hack5.treblecheck.data.BinderDetector

@RunWith(Parameterized::class)
class BinderDetectorTest(private val expected: BinderArch, private val binderVersion: Int?) {
    companion object {
        @Suppress("BooleanLiteralArgument")
        @Parameterized.Parameters
        @JvmStatic
        fun data() = listOf(
            arrayOf(BinderArch.Unknown(6), 6),
            arrayOf(BinderArch.Unknown(null), null),
            arrayOf(BinderArch.Binder7, 7),
            arrayOf(BinderArch.Binder8, 8),
            arrayOf(BinderArch.Unknown(9), 9),
        )
    }

    @Test
    fun getArch() = mockkObject(BinderDetector) {
        if (binderVersion != null) {
            every { BinderDetector.getBinderVersion() } returns binderVersion
        } else {
            every { BinderDetector.getBinderVersion() } throws UnsatisfiedLinkError()
        }

        assertEquals(expected, BinderDetector.getBinderArch())
    }
}
