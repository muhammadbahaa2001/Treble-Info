#!/bin/bash
#
#     Treble Info
#     Copyright (C) 2022-2023 Hackintosh Five
#
#     This program is free software: you can redistribute it and/or modify
#     it under the terms of the GNU General Public License as published by
#     the Free Software Foundation, either version 3 of the License, or
#     (at your option) any later version.
#
#     This program is distributed in the hope that it will be useful,
#     but WITHOUT ANY WARRANTY; without even the implied warranty of
#     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#     GNU General Public License for more details.
#
#     You should have received a copy of the GNU General Public License
#     along with this program.  If not, see <https://www.gnu.org/licenses/>.
#
# SPDX-License-Identifier: GPL-3.0-or-later

IFS=$'\n'

mkdir -p vendor/etc/selinux
mkdir -p vendor/etc/vintf/manifest
mkdir -p odm/etc/vintf/manifest

vendor_sku="$(adb shell getprop ro.boot.product.vendor.sku)"
adb shell run-as tk.hack5.treblecheck cat "/vendor/etc/vintf/manifest_$vendor_sku.xml" > "vendor/etc/vintf/manifest_sku.xml" || rm "vendor/etc/vintf/manifest_sku.xml"
adb shell run-as tk.hack5.treblecheck cat "/vendor/etc/vintf/manifest.xml" > "vendor/etc/vintf/manifest.xml" || rm "vendor/etc/vintf/manifest.xml"
adb shell run-as tk.hack5.treblecheck cat "/vendor/manifest.xml" > "vendor/manifest.xml" || rm "vendor/manifest.xml"

rm "vendor/etc/vintf/manifest/*"
i=0
for file in $(adb shell run-as tk.hack5.treblecheck find "/vendor/etc/vintf/manifest/" -maxdepth 1 -iname '*.xml'); do
  adb shell run-as tk.hack5.treblecheck cat "$file" > "vendor/etc/vintf/manifest/$i" || rm "vendor/etc/vintf/manifest/$i"
  i=$((i+1))
done

odm_sku="$(adb shell getprop ro.boot.product.hardware.sku)"
adb shell run-as tk.hack5.treblecheck cat "/odm/etc/vintf/manifest_$odm_sku.xml" > "odm/etc/vintf/manifest_sku.xml" || rm "odm/etc/vintf/manifest_sku.xml"
adb shell run-as tk.hack5.treblecheck cat "/odm/etc/vintf/manifest.xml" > "odm/etc/vintf/manifest.xml" || rm "odm/etc/vintf/manifest.xml"
adb shell run-as tk.hack5.treblecheck cat "/odm/etc/manifest_$odm_sku.xml" > "odm/etc/manifest_sku.xml" || rm "odm/etc/manifest_sku.xml"
adb shell run-as tk.hack5.treblecheck cat "/odm/etc/manifest.xml" > "odm/etc/manifest.xml" || rm "odm/etc/manifest.xml"

rm "odm/etc/vintf/manifest/*"
i=0
for file in $(adb shell run-as tk.hack5.treblecheck find "/odm/etc/vintf/manifest/" -maxdepth 1 -iname '*.xml'); do
  adb shell run-as tk.hack5.treblecheck cat "$file" > "odm/etc/vintf/manifest/$i" || rm "odm/etc/manifest/$i"
  i=$((i+1))
done

adb shell run-as tk.hack5.treblecheck cat "/vendor/etc/vintf/compatibility_matrix.xml" > "vendor/etc/vintf/compatibility_matrix.xml" || rm "vendor/etc/vintf/compatibility_matrix.xml"
adb shell run-as tk.hack5.treblecheck cat "/vendor/compatibility_matrix.xml" > "vendor/compatibility_matrix.xml" || rm "vendor/compatibility_matrix.xml"

rm "vendor/etc/selinux/*"
i=0
for file in $(adb shell run-as tk.hack5.treblecheck find "/vendor/etc/selinux/" -maxdepth 1 -iname '*.cil'); do
  adb shell run-as tk.hack5.treblecheck cat "$file" > "vendor/etc/selinux/$i" || rm "vendor/etc/selinux/$i"
  i=$((i+1))
done

adb shell run-as tk.hack5.treblecheck cat "/vendor/etc/selinux/plat_sepolicy_vers.txt" > "vendor/etc/selinux/plat_sepolicy_vers.txt" || rm "vendor/etc/selinux/plat_sepolicy_vers.txt"
