package com.fam4k007.videoplayer

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import com.fam4k007.videoplayer.compose.LicenseItem
import com.fam4k007.videoplayer.compose.LicenseScreen
import com.fam4k007.videoplayer.ui.theme.getThemeColors
import com.fam4k007.videoplayer.utils.ThemeManager

/**
 * 许可证书页面
 */
class LicenseActivity : BaseActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val licenseItems = buildLicenseItems()
        val activity = this

        setContent {
            val themeColors = getThemeColors(ThemeManager.getCurrentTheme(activity).themeName)

            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = themeColors.primary,
                    onPrimary = themeColors.onPrimary,
                    primaryContainer = themeColors.primaryVariant,
                    secondary = themeColors.secondary,
                    background = themeColors.background,
                    onBackground = themeColors.onBackground,
                    surface = themeColors.surface,
                    surfaceVariant = themeColors.surfaceVariant,
                    onSurface = themeColors.onSurface
                )
            ) {
                LicenseScreen(
                    licenses = licenseItems,
                    onBack = {
                        activity.finish()
                        activity.overridePendingTransition(R.anim.no_anim, R.anim.slide_out_down)
                    }
                )
            }
        }
    }
    
    private fun buildLicenseItems(): List<LicenseItem> {
        return listOf(
            LicenseItem(
                name = "MPV Player",
                licenseTag = "GPL-2.0",
                summary = "跨平台高性能媒体播放器内核，负责解码与渲染。",
                website = "https://mpv.io/",
                licenseText = getMpvLicense()
            ),
            LicenseItem(
                name = "Anime4K",
                licenseTag = "MIT",
                summary = "实时动漫超分辨率算法，用于提升画质。",
                website = "https://github.com/bloc97/Anime4K",
                licenseText = getMitLicense()
            ),
            LicenseItem(
                name = "DanmakuFlameMaster",
                licenseTag = "Apache-2.0",
                summary = "哔哩哔哩开源弹幕引擎，支撑弹幕显示功能。",
                website = "https://github.com/bilibili/DanmakuFlameMaster",
                licenseText = getApacheLicense()
            ),
            LicenseItem(
                name = "Glide",
                licenseTag = "BSD/Apache-2.0",
                summary = "图片加载与缓存库，用于封面与缩略图。",
                website = "https://github.com/bumptech/glide",
                licenseText = getGlideLicense()
            ),
            LicenseItem(
                name = "AndroidX & Material",
                licenseTag = "Apache-2.0",
                summary = "现代化 Android UI 组件与官方支持库。",
                website = "https://developer.android.com/jetpack/androidx",
                licenseText = getApacheLicense()
            ),
            LicenseItem(
                name = "Kotlin",
                licenseTag = "Apache-2.0",
                summary = "JetBrains 出品的现代语言，是应用主要开发语言。",
                website = "https://kotlinlang.org/",
                licenseText = getApacheLicense()
            ),
            LicenseItem(
                name = "mpv-android",
                licenseTag = "MIT",
                summary = "mpv 在 Android 平台的移植项目，提供底层接口。",
                website = "https://github.com/mpv-android/mpv-android",
                licenseText = getMpvAndroidLicense()
            ),
            LicenseItem(
                name = "mpvKt",
                licenseTag = "Apache-2.0",
                summary = "为 mpv 提供 Kotlin 封装的实用库。",
                website = "https://github.com/L0uckY/mpvKt",
                licenseText = getApacheLicense()
            ),
            LicenseItem(
                name = "DanDanPlayForAndroid",
                licenseTag = "Apache-2.0",
                summary = "哔哩哔哩番剧数据与弹幕下载参考实现。",
                website = "https://github.com/xyoye/DanDanPlayForAndroid",
                licenseText = getApacheLicense()
            )
        )
    }
    
    private fun getMpvLicense(): String {
        return """
GNU GENERAL PUBLIC LICENSE
Version 2, June 1991

Copyright (C) 1989, 1991 Free Software Foundation, Inc.
51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA

Everyone is permitted to copy and distribute verbatim copies
of this license document, but changing it is not allowed.

Preamble

The licenses for most software are designed to take away your freedom to share and change it. By contrast, the GNU General Public License is intended to guarantee your freedom to share and change free software--to make sure the software is free for all its users.

This General Public License applies to most of the Free Software Foundation's software and to any other program whose authors commit to using it.

When we speak of free software, we are referring to freedom, not price. Our General Public Licenses are designed to make sure that you have the freedom to distribute copies of free software (and charge for this service if you wish), that you receive source code or can get it if you want it, that you can change the software or use pieces of it in new free programs; and that you know you can do these things.

完整许可证请访问: https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
        """.trimIndent()
    }
    
    private fun getMitLicense(): String {
        return """
MIT License

Copyright (c) 2019 bloc97

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
        """.trimIndent()
    }
    
    private fun getApacheLicense(): String {
        return """
Apache License
Version 2.0, January 2004
http://www.apache.org/licenses/

TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

1. Definitions.

"License" shall mean the terms and conditions for use, reproduction, and distribution as defined by Sections 1 through 9 of this document.

"Licensor" shall mean the copyright owner or entity authorized by the copyright owner that is granting the License.

"You" (or "Your") shall mean an individual or Legal Entity exercising permissions granted by this License.

2. Grant of Copyright License. Subject to the terms and conditions of this License, each Contributor hereby grants to You a perpetual, worldwide, non-exclusive, no-charge, royalty-free, irrevocable copyright license to reproduce, prepare Derivative Works of, publicly display, publicly perform, sublicense, and distribute the Work and such Derivative Works in Source or Object form.

3. Grant of Patent License. Subject to the terms and conditions of this License, each Contributor hereby grants to You a perpetual, worldwide, non-exclusive, no-charge, royalty-free, irrevocable patent license to make, have made, use, offer to sell, sell, import, and otherwise transfer the Work.

4. Redistribution. You may reproduce and distribute copies of the Work or Derivative Works thereof in any medium, with or without modifications, provided that You meet the following conditions:
   - You must give any other recipients of the Work a copy of this License
   - You must retain all copyright, patent, trademark notices
   - If the Work includes a "NOTICE" text file, you must include a copy

完整许可证请访问: http://www.apache.org/licenses/LICENSE-2.0
        """.trimIndent()
    }
    
    private fun getGlideLicense(): String {
        return """
License for Glide

Glide is licensed under the BSD License and the Apache License 2.0.

BSD License:
Copyright 2014 Google, Inc. All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

3. Neither the name of Google Inc. nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES ARE DISCLAIMED.

完整许可证请访问: https://github.com/bumptech/glide/blob/master/LICENSE
        """.trimIndent()
    }
    
    private fun getMpvAndroidLicense(): String {
        return """
MIT License

Copyright (c) 2016 Ilya Zhuravlev
Copyright (c) 2016 sfan5 <sfan5@live.de>

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

完整许可证请访问: https://github.com/mpv-android/mpv-android/blob/master/LICENSE
        """.trimIndent()
    }
}
