/*
 *     Treble Info
 *     Copyright (C) 2019-2022 Hackintosh Five
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

#include <android-base/properties.h>

#include <string>

namespace android {
namespace base {

bool GetBoolProperty(const std::string& key, bool default_value) {
  return default_value;
}

template <typename T>
T GetIntProperty(const std::string& key, T default_value, T min, T max) {
  return default_value;
}

template <typename T>
T GetUintProperty(const std::string& key, T default_value, T max) {
  return default_value;
}

template int8_t GetIntProperty(const std::string&, int8_t, int8_t, int8_t);
template int16_t GetIntProperty(const std::string&, int16_t, int16_t, int16_t);
template int32_t GetIntProperty(const std::string&, int32_t, int32_t, int32_t);
template int64_t GetIntProperty(const std::string&, int64_t, int64_t, int64_t);

template uint8_t GetUintProperty(const std::string&, uint8_t, uint8_t);
template uint16_t GetUintProperty(const std::string&, uint16_t, uint16_t);
template uint32_t GetUintProperty(const std::string&, uint32_t, uint32_t);
template uint64_t GetUintProperty(const std::string&, uint64_t, uint64_t);

std::string GetProperty(const std::string& key, const std::string& default_value) {
  // TODO fix GetProperty (needed for ODM manifest on legacy)
  return default_value;
}

bool SetProperty(const std::string& key, const std::string& value) {
  return false;
}

}  // namespace base
}  // namespace android
