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

#pragma once

#include_next <android/log.h>

__BEGIN_DECLS

#if __ANDROID_API__ < 30
void __android_log_write_log_message(struct __android_log_message* log_message) __INTRODUCED_IN(1);

void __android_log_set_logger(__android_logger_function logger) __INTRODUCED_IN(1);

void __android_log_logd_logger(const struct __android_log_message* log_message) __INTRODUCED_IN(1);

void __android_log_stderr_logger(const struct __android_log_message* log_message) __INTRODUCED_IN(1);

void __android_log_set_aborter(__android_aborter_function aborter) __INTRODUCED_IN(1);

void __android_log_call_aborter(const char* abort_message) __INTRODUCED_IN(1);

void __attribute__((noreturn)) __android_log_default_aborter(const char* abort_message) __INTRODUCED_IN(1);

int __android_log_is_loggable(int prio, const char* tag, int default_prio) __INTRODUCED_IN(1);

int __android_log_is_loggable_len(int prio, const char* tag, size_t len, int default_prio) __INTRODUCED_IN(1);

int32_t __android_log_set_minimum_priority(int32_t priority) __INTRODUCED_IN(1);

int32_t __android_log_get_minimum_priority(void) __INTRODUCED_IN(1);

void __android_log_set_default_tag(const char* tag) __INTRODUCED_IN(1);
#endif

__END_DECLS
