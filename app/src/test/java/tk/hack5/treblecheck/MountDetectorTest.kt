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
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import tk.hack5.treblecheck.data.Mount
import tk.hack5.treblecheck.data.MountDetector
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.StringReader

@RunWith(Parameterized::class)
class MountDetectorTest(private val expected: List<Mount>, private val mountsFile: Answer<String>) {
    companion object {
        @Suppress("BooleanLiteralArgument")
        @Parameterized.Parameters
        @JvmStatic
        fun data() = listOf(
            arrayOf(
                listOf(
                    Mount(
                        "/dev/block/dm-0",
                        "/",
                        "ext4",
                        listOf("ro", "seclabel", "relatime", "discard"),
                        0,
                        0
                    )
                ),
                ConstantAnswer("/dev/block/dm-0 / ext4 ro,seclabel,relatime,discard 0 0")
            ),
            arrayOf(
                listOf(
                    Mount(
                        "none",
                        "/dev/cpuset",
                        "cgroup",
                        listOf("rw", "nosuid", "nodev", "noexec", "relatime", "cpuset", "noprefix", "release_agent=/sbin/cpuset_release_agent"),
                        0,
                        0
                    ),
                    Mount(
                        "sysfs",
                        "/sys",
                        "sysfs",
                        listOf("rw", "seclabel", "relatime"),
                        0,
                        0
                    )
                ),
                ConstantAnswer("none /dev/cpuset cgroup rw,nosuid,nodev,noexec,relatime,cpuset,noprefix,release_agent=/sbin/cpuset_release_agent 0 0\n\nsysfs /sys sysfs rw,seclabel,relatime 0 0\n")
            ),
        )
    }

    @Test
    fun checkMounts() = mockkObject(MountDetector) {
        every { MountDetector.getMountsStream() } answers { call ->
            BufferedReader(
                StringReader(
                    mountsFile.answer(call)
                )
            )
        }
        assertEquals(expected, MountDetector.getMounts().toList())
    }
}

@RunWith(Parameterized::class)
class SARDetectorTest(private val expected: Boolean, private val mountsFile: Answer<InputStream>, private val sar: String?, private val dynamicPartitions: String?) {
    companion object {
        @Suppress("BooleanLiteralArgument")
        @Parameterized.Parameters
        @JvmStatic
        fun data() = listOf(
            arrayOf(true, ThrowingAnswer(IllegalStateException()), "true", "false"),
            arrayOf(true, ThrowingAnswer(IllegalStateException()), "false", "true"),
            arrayOf(true, ConstantAnswer(SARDetectorTest::class.java.classLoader!!.getResourceAsStream("mounts1.txt")), "false", ""),
            arrayOf(true, ConstantAnswer(SARDetectorTest::class.java.classLoader!!.getResourceAsStream("mounts2.txt")), "", ""),
            arrayOf(true, ConstantAnswer(SARDetectorTest::class.java.classLoader!!.getResourceAsStream("mounts3.txt")), "", ""),
            arrayOf(true, ConstantAnswer(SARDetectorTest::class.java.classLoader!!.getResourceAsStream("mounts4.txt")), "", ""),
            arrayOf(false, ConstantAnswer(SARDetectorTest::class.java.classLoader!!.getResourceAsStream("mounts5.txt")), "", ""),
            arrayOf(true, ConstantAnswer(SARDetectorTest::class.java.classLoader!!.getResourceAsStream("mounts6.txt")), "", ""),
            arrayOf(true, ConstantAnswer(SARDetectorTest::class.java.classLoader!!.getResourceAsStream("mounts7.txt")), "", ""),
            arrayOf(true, ConstantAnswer(SARDetectorTest::class.java.classLoader!!.getResourceAsStream("mounts8.txt")), "", ""),
            arrayOf(true, ConstantAnswer(SARDetectorTest::class.java.classLoader!!.getResourceAsStream("mounts9.txt")), "", ""),
            arrayOf(true, ConstantAnswer(SARDetectorTest::class.java.classLoader!!.getResourceAsStream("mounts10.txt")), "", ""),
            arrayOf(true, ConstantAnswer(SARDetectorTest::class.java.classLoader!!.getResourceAsStream("mounts11.txt")), "", ""),
            arrayOf(false, ConstantAnswer(SARDetectorTest::class.java.classLoader!!.getResourceAsStream("mounts12.txt")), "", ""),
            arrayOf(false, ConstantAnswer(SARDetectorTest::class.java.classLoader!!.getResourceAsStream("mounts13.txt")), "", "")
        )
    }

    @Test
    fun testIsSAR() = mockkObject(MountDetector) {
        every { MountDetector.getMountsStream() } answers { call -> BufferedReader(InputStreamReader(mountsFile.answer(call))).also { it.readLine() } }
        mockkStatic(::propertyGet.declaringKotlinFile) {
            every { propertyGet("ro.build.system_root_image") } returns sar
            every { propertyGet("ro.boot.dynamic_partitions") } returns dynamicPartitions
            assertEquals(expected, MountDetector.isSAR())
        }
    }
}