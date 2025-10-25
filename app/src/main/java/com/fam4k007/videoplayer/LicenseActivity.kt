package com.fam4k007.videoplayer

import android.os.Bundle

/**
 * 许可证书页面
 */
class LicenseActivity : BaseActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_license)
        
        // 设置Toolbar
        setupToolbar(R.id.toolbar, getString(R.string.license), showBackButton = true)
    }
}
