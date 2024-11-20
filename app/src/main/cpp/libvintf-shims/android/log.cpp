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

#include <android/log.h>
#include <android/set_abort_message.h>
#include <cstdlib>
#include <mutex>
#include <iostream>
#include <cinttypes>

__BEGIN_DECLS

#if __ANDROID_API__ < 30
void __android_log_logd_logger(const struct __android_log_message* log_message) __INTRODUCED_IN(1);
__android_logger_function __android_log_current_logger = __android_log_logd_logger;
std::mutex __android_log_current_logger_lock;

void __android_log_write_log_message(struct __android_log_message* log_message) {
    std::unique_lock<std::mutex> lock(__android_log_current_logger_lock);
    __android_logger_function logger = __android_log_current_logger;
    lock.unlock();
    logger(log_message);
}

void __android_log_set_logger(__android_logger_function logger) {
    std::lock_guard<std::mutex> lock(__android_log_current_logger_lock);
    __android_log_current_logger = logger;
}

void __android_log_logd_logger(const struct __android_log_message* log_message) {
    if (log_message->file != nullptr) {
        __android_log_print(log_message->priority, (log_message->tag == nullptr ? getprogname() : log_message->tag), "%s:%" PRIdLEAST32 ": %s",
                log_message->file, log_message->line, log_message->message);
    } else {
        __android_log_write(log_message->priority, (log_message->tag == nullptr ? getprogname() : log_message->tag), log_message->message);
    }
}

void __android_log_stderr_logger(const struct __android_log_message* log_message) {
    std::cerr << log_message->buffer_id << ':' << log_message->priority << ':' << (log_message->tag == nullptr ? getprogname() : log_message->tag) << ':';
    if (log_message->file != nullptr) {
        std::cerr << log_message->file << ':' << log_message->line << ':';
    }
    std::cerr << log_message->message;
}

void __android_log_set_aborter(__android_aborter_function aborter) {
    // TODO
}

void __attribute__((noreturn)) __android_log_default_aborter(const char* abort_message) {
#ifdef __ANDROID__
    android_set_abort_message(abort_message);
#endif
    std::abort();
}

void __android_log_call_aborter(const char* abort_message) {
    // TODO
    __android_log_default_aborter(abort_message);
}

int __android_log_is_loggable(int prio, const char* tag, int default_prio) {
    // TODO
    return prio >= default_prio;
}

int __android_log_is_loggable_len(int prio, const char* tag, size_t len, int default_prio) {
    // TODO
    return prio >= default_prio;
}

int32_t __android_log_set_minimum_priority(int32_t priority) {
    // TODO
    return ANDROID_LOG_DEFAULT;
}

int32_t __android_log_get_minimum_priority(void) {
    // TODO
    return ANDROID_LOG_DEFAULT;
}

void __android_log_set_default_tag(const char* tag) {
    // TODO
}
#endif

__END_DECLS
