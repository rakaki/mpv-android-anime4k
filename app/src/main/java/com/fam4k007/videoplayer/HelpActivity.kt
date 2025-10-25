package com.fam4k007.videoplayer

import android.os.Bundle

/**
 * 帮助说明页面
 */
class HelpActivity : BaseActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)
        
        // 设置Toolbar
        setupToolbar(R.id.toolbar, getString(R.string.help), showBackButton = true)
    }
}
