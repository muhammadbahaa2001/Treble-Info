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

#include <android/fdsan.h>

__BEGIN_DECLS

#if __ANDROID_API__ < 29
uint64_t android_fdsan_create_owner_tag(enum android_fdsan_owner_type type, uint64_t tag) {
    return 0;
}

void android_fdsan_exchange_owner_tag(int fd, uint64_t expected_tag, uint64_t new_tag) {
}

int android_fdsan_close_with_tag(int fd, uint64_t tag) {
    return 0;
}

uint64_t android_fdsan_get_owner_tag(int fd) {
    return 0;
}

const char* android_fdsan_get_tag_type(uint64_t tag) {
    return "";
}

uint64_t android_fdsan_get_tag_value(uint64_t tag) {
    return 0;
}

enum android_fdsan_error_level android_fdsan_get_error_level() {
    return ANDROID_FDSAN_ERROR_LEVEL_DISABLED;
}

enum android_fdsan_error_level android_fdsan_set_error_level(enum android_fdsan_error_level new_level) {
    return ANDROID_FDSAN_ERROR_LEVEL_DISABLED;
}
#endif


/*
 * Set the error level to the global setting if available, or a default value.
 */

#if __ANDROID_API__ < 30
enum android_fdsan_error_level android_fdsan_set_error_level_from_property(enum android_fdsan_error_level default_level) {
    return ANDROID_FDSAN_ERROR_LEVEL_DISABLED;
}
#endif

__END_DECLS