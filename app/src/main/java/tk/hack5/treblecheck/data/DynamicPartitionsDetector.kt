/*
 *     Treble Info
 *     Copyright (C) 2020-2023 Hackintosh Five
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
import tk.hack5.treblecheck.Mock
import tk.hack5.treblecheck.parseBool
import tk.hack5.treblecheck.propertyGet

object DynamicPartitionsDetector {
    fun isDynamic(): Boolean? {
        Mock.data?.let { return it.dynamic }

        val dynamicPartitions = propertyGet("ro.boot.dynamic_partitions")

        Log.v(tag, "dynamicPartitions: $dynamicPartitions")
        return parseBool(dynamicPartitions ?: return null) ?: false
    }
}

private const val tag = "DynamicPartitionsDetect"