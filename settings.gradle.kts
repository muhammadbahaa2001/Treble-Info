/*
 *     Treble Info
 *     Copyright (C) 2019 Hackintosh Five
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

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google()
    }

    versionCatalogs {
        create("libs") {
            // build
            plugin("agp", "com.android.application").version("8.1.0")
            plugin("kotlin-android", "org.jetbrains.kotlin.android").version("1.9.0")
            version("compose-compiler", "1.5.1")


            // main
            version("aboutlibraries", "10.8.0")
            plugin("aboutlibraries", "com.mikepenz.aboutlibraries.plugin").versionRef("aboutlibraries")
            library("main-aboutlibraries", "com.mikepenz", "aboutlibraries-core").versionRef("aboutlibraries")

            library("compose-bom", "androidx.compose:compose-bom:2023.08.00")
            library("main-compose-ui", "androidx.compose.ui", "ui").withoutVersion()
            library("main-compose-animation", "androidx.compose.animation", "animation").withoutVersion()

            version("compose-material3", "1.1.1")
            library("main-compose-material3", "androidx.compose.material3", "material3").versionRef("compose-material3")
            library("main-compose-material3-windowsizeclass", "androidx.compose.material3", "material3-window-size-class").versionRef("compose-material3")

            library("main-activity-compose", "androidx.activity:activity-compose:1.7.2")

            library("main-navigation-compose", "androidx.navigation:navigation-compose:2.6.0")

            // nonfree
            version("billingclient", "6.0.1")
            library("nonfree-billingclient", "com.android.billingclient", "billing").versionRef("billingclient")
            library("nonfree-billingclient-ktx", "com.android.billingclient", "billing-ktx").versionRef("billingclient")

            // test
            library("test-junit", "junit:junit:4.13.2")

            version("mockk", "1.13.7")
            library("test-mockk", "io.mockk", "mockk").versionRef("mockk")
            library("test-mockk-jvm", "io.mockk", "mockk-agent-jvm").versionRef("mockk")

            library("test-xmlpull", "xmlpull:xmlpull:1.1.3.1")

            library("test-kxml2", "net.sf.kxml:kxml2:2.3.0")

            library("screenshots-runner", "androidx.test:runner:1.5.2")

            library("screenshots-screengrab", "tools.fastlane:screengrab:2.1.1")

            library("screenshots-junit-ext", "androidx.test.ext:junit:1.1.5")

            library("screenshots-compose-ui-junit", "androidx.compose.ui", "ui-test-junit4").withoutVersion()

            // tooling
            library("tooling-compose-ui", "androidx.compose.ui", "ui-tooling").withoutVersion()
            library("tooling-compose-ui-preview", "androidx.compose.ui", "ui-tooling-preview").withoutVersion()
        }
    }
}

rootProject.name = "Treble Info"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    includeBuild("poeditor-android")
    includeBuild("materialdesignicons-android")
}
include(":app")
