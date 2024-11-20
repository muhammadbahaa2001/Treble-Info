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

#include <climits>
#include <utility>
#include <jni.h>

#include <vintf/parse_xml.h>
#include <vintf/CompatibilityMatrix.h>
#include <vintf/CheckFlags.h>
#include <vintf/ObjectFactory.h>
#include <vintf/VintfObject.h>
#include <utils/Errors.h>
#include "libvintf/utils.h"
#include "FileSystem.h"
#include <android-base/logging.h>

struct StubRuntimeInfo : public android::vintf::RuntimeInfo {
    android::status_t fetchAllInformation(FetchFlags) override { return android::UNKNOWN_ERROR; }
};

struct StaticRuntimeInfoFactory : public android::vintf::ObjectFactory<StubRuntimeInfo::RuntimeInfo> {
public:
    StaticRuntimeInfoFactory() = default;
    [[nodiscard]] std::shared_ptr<StubRuntimeInfo::RuntimeInfo> make_shared() const override {
        return std::make_shared<StubRuntimeInfo>();
    }
};

class SkuPropertyFetcher : public android::vintf::details::PropertyFetcherImpl {
public:
    SkuPropertyFetcher(std::string vendorSku, std::string hardwareSku) : vendorSku(std::move(vendorSku)), hardwareSku(std::move(hardwareSku)) {};

    [[nodiscard]] std::string getProperty(const std::string& key,
                                          const std::string& defaultValue) const override {
        if (key == "ro.boot.product.vendor.sku") {
            LOG(INFO) << "ro.boot.product.vendor.sku=" << vendorSku;
            return vendorSku;
        }
        if (key == "ro.boot.product.hardware.sku") {
            LOG(INFO) << "ro.boot.product.hardware.sku=" << hardwareSku;
            return hardwareSku;
        }
        LOG(INFO) << "Prop " << key << " is missing, defaulting to " << defaultValue;
        return defaultValue;
    }
private:
    const std::string vendorSku;
    const std::string hardwareSku;
};

extern "C" JNIEXPORT jint JNICALL
Java_tk_hack5_treblecheck_data_TrebleDetector_check_1compatibility_1matrix(__unused JNIEnv *env, __unused jobject thiz, jstring matrixContentString, jstring rootString, jstring vendorSkuString, jstring hardwareSkuString) {
    using namespace android::vintf::details;
    const char* matrixContent = env->GetStringUTFChars(matrixContentString, nullptr);
    const char* root = env->GetStringUTFChars(rootString, nullptr);
    const char* vendorSku = env->GetStringUTFChars(vendorSkuString, nullptr);
    const char* hardwareSku = env->GetStringUTFChars(hardwareSkuString, nullptr);

    auto fileSystem = std::make_unique<android::vintf::details::FileSystemUnderPath>(FileSystemUnderPath(root));
    std::string error;

    auto matrix = std::make_unique<android::vintf::CompatibilityMatrix>();
    //matrix->setFileName(matrixPathChars);

    if (!fromXml(matrix.get(), matrixContent, &error)) {
        LOG(ERROR) << "Cannot parse packaged matrix: " << error;
        return -1;
    }

    auto propertyFetcher = std::make_unique<SkuPropertyFetcher>(SkuPropertyFetcher(vendorSku, hardwareSku));

    auto vintfObject =
            android::vintf::VintfObject::Builder()
                    .setFileSystem(std::move(fileSystem))
                    .setPropertyFetcher(std::move(propertyFetcher))
                    .setRuntimeInfoFactory(std::make_unique<StaticRuntimeInfoFactory>())
                    .build();
    auto manifest = vintfObject->getDeviceHalManifest();
    if (!manifest) {
        LOG(ERROR) << "Loading device manifest failed";
        return -2;
    }
    bool ret = manifest->checkCompatibility(*matrix.get(), &error, android::vintf::CheckFlags::DISABLE_ALL_CHECKS);
    if (!error.empty()) {
        LOG(ERROR) << "Compatibility check failed: " << error;
    }

    env->ReleaseStringUTFChars(matrixContentString, matrixContent);
    return ret;
}
