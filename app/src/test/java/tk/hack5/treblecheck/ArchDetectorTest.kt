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
import tk.hack5.treblecheck.data.ArchDetector
import tk.hack5.treblecheck.data.CPUArch


@RunWith(Parameterized::class)
class ArchDetectorTest(private val expected: CPUArch, private val supportedAbis: Array<String>) {
    companion object {
        @Suppress("BooleanLiteralArgument")
        @Parameterized.Parameters
        @JvmStatic
        fun data() = listOf(
            arrayOf(CPUArch.ARM32, arrayOf("armeabi-v7a")),
            arrayOf(CPUArch.ARM64, arrayOf("arm64-v8a")),
            arrayOf(CPUArch.X86_64, arrayOf("x86_64")),
            arrayOf(CPUArch.X86, arrayOf("x86")),
            arrayOf(CPUArch.Unknown("fancy new cpu"), arrayOf("fancy new cpu", "x86_64", "x86")),
            arrayOf(CPUArch.Unknown(null), arrayOf<String>()),
        )
    }

    @Test
    fun getArch() = mockkObject(ArchDetector) {
        every { ArchDetector.SUPPORTED_ABIS } returns supportedAbis
        assertEquals(expected, ArchDetector.getCPUArch())
    }
}
