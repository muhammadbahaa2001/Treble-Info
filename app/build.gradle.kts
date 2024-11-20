/*
 *     Treble Info
 *     Copyright (C) 2019-2023 Hackintosh Five
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

import org.jetbrains.kotlin.konan.properties.loadProperties
import com.android.build.api.variant.BuildConfigField

plugins {
    id("com.android.application")
    kotlin("android")
    id("poeditor-android")
    id("materialdesignicons-android")
    id("kotlin-parcelize")
    alias(libs.plugins.aboutlibraries)
}

aboutLibraries {
    configPath = projectDir.resolve("librariesConfig").toString()
    excludeFields = arrayOf("generated")
}

fun com.android.build.api.dsl.BuildType.setupBilling() {
    loadProperties(file("billing.properties").absolutePath).run {
        buildConfigField("String", "GPLAY_PRODUCT", getProperty("gplayProduct"))

        buildConfigField("String", "PAYPAL_EMAIL", getProperty("paypalEmail"))
        buildConfigField("String", "PAYPAL_CURRENCY", getProperty("paypalCurrency"))
        buildConfigField("String", "PAYPAL_DESCRIPTION", getProperty("paypalDescription"))
    }
}

android {
    compileSdk = 34
    buildToolsVersion = "34.0.0"
    defaultConfig {
        applicationId = "tk.hack5.treblecheck"
        minSdk = 22
        targetSdk = 34
        loadProperties(file("version.properties").absolutePath).run {
            versionCode = getProperty("versionCode").toInt()
            versionName = getProperty("versionName")
        }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["notClass"] = "tk.hack5.treblecheck.ScreenshotTaker"
    }

    flavorDimensions += "freedom"
    productFlavors {
        create("free") {
            dimension = "freedom"
        }
        create("nonfree") {
            dimension = "freedom"
        }
    }

    if (file("signing.properties").exists()) {
        loadProperties(file("signing.properties").absolutePath).run {
            signingConfigs {
                create("release") {
                    keyAlias = getProperty("keyAlias")
                    storeFile = file(getProperty("storeFile"))
                    keyPassword = getProperty("keyPassword")
                    storePassword = getProperty("storePassword")
                }
            }
        }
    }

    buildTypes {
        getByName("release") {
            if (file("signing.properties").exists()) {
                signingConfig = signingConfigs["release"]
            }
            setupBilling()
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        getByName("debug") {
            signingConfig = signingConfigs["debug"]
            setupBilling()
        }
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            excludes += "/DebugProbesKt.bin"
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/*.version"
            excludes += "/kotlin-tooling-metadata.json"
            excludes += "/kotlin/**.kotlin_builtins"
        }
    }
    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }
    lint {
        checkDependencies = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }
    sourceSets {
        get("test").java.srcDir("src/sharedTest/java")
        get("test").kotlin.srcDir("src/sharedTest/java")
        get("test").resources.srcDir("src/sharedTest/resources")
        get("androidTest").java.srcDir("src/sharedTest/java")
        get("androidTest").kotlin.srcDir("src/sharedTest/java")
        get("androidTest").resources.srcDir("src/sharedTest/resources")
    }
    namespace = "tk.hack5.treblecheck"
}

if (file("poeditor.properties").exists()) {
    project.poeditor.apiToken = loadProperties(file("poeditor.properties").absolutePath).getProperty("apiToken")
}

project.poeditor.projectId = 285385

tasks.withType(com.github.penn5.ImportPoEditorStringsBaseTask::class) {
    allowFuzzy = false
}


dependencies {
    val composeBom = platform(libs.compose.bom)

    implementation(composeBom)
    implementation(libs.main.compose.ui)
    implementation(libs.main.compose.material3)
    implementation(libs.main.compose.material3.windowsizeclass)
    implementation(libs.main.compose.animation)
    implementation(libs.main.activity.compose)
    implementation(libs.main.navigation.compose)
    implementation(libs.main.aboutlibraries)
    "nonfreeImplementation"(libs.nonfree.billingclient)
    "nonfreeImplementation"(libs.nonfree.billingclient.ktx)
    testImplementation(libs.test.junit)
    testImplementation(libs.test.mockk)
    testImplementation(libs.test.mockk.jvm)
    testImplementation(libs.test.xmlpull)
    testImplementation(libs.test.kxml2)
    androidTestImplementation(composeBom)
    androidTestImplementation(libs.screenshots.runner)
    androidTestImplementation(libs.screenshots.screengrab)
    androidTestImplementation(libs.screenshots.junit.ext)
    androidTestImplementation(libs.screenshots.compose.ui.junit)
    debugImplementation(libs.tooling.compose.ui)
    implementation(libs.tooling.compose.ui.preview)
}

tasks.getByName("preBuild") {
    mustRunAfter("updateDrawables")
    mustRunAfter("importTranslations")
}

tasks.register("versionName") {
    doLast {
        println(android.defaultConfig.versionName)
    }
}
