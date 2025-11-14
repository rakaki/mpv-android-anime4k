package com.fam4k007.videoplayer

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView

/**
 * 许可证书页面
 */
class LicenseActivity : BaseActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_license)
        
        // 设置返回按钮
        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
        
        // 设置点击事件
        setupClickListeners()
    }
    
    private fun setupClickListeners() {
        findViewById<CardView>(R.id.cardMpv).setOnClickListener {
            showLicenseDialog("MPV Player", getMpvLicense())
        }
        
        findViewById<CardView>(R.id.cardAnime4k).setOnClickListener {
            showLicenseDialog("Anime4K", getMitLicense())
        }
        
        findViewById<CardView>(R.id.cardDanmaku).setOnClickListener {
            showLicenseDialog("DanmakuFlameMaster", getApacheLicense())
        }
        
        findViewById<CardView>(R.id.cardGlide).setOnClickListener {
            showLicenseDialog("Glide", getGlideLicense())
        }
        
        findViewById<CardView>(R.id.cardAndroidX).setOnClickListener {
            showLicenseDialog("AndroidX & Material Components", getApacheLicense())
        }
        
        findViewById<CardView>(R.id.cardKotlin).setOnClickListener {
            showLicenseDialog("Kotlin", getApacheLicense())
        }
        
        findViewById<CardView>(R.id.cardMpvAndroid).setOnClickListener {
            showLicenseDialog("mpv-android", getMpvAndroidLicense())
        }
        
        findViewById<CardView>(R.id.cardMpvKt).setOnClickListener {
            showLicenseDialog("mpvKt", getApacheLicense())
        }
        
        findViewById<CardView>(R.id.cardDanDanPlay).setOnClickListener {
            showLicenseDialog("DanDanPlayForAndroid", getApacheLicense())
        }
    }
    
    private fun showLicenseDialog(title: String, licenseText: String) {
        val dialogView = layoutInflater.inflate(android.R.layout.simple_list_item_1, null)
        
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(licenseText)
            .setPositiveButton("关闭", null)
            .show()
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
