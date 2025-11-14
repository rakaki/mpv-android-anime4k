package com.fam4k007.videoplayer

import android.os.Bundle
import android.view.View

/**
 * 帮助说明页面
 */
class HelpActivity : BaseActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)
        
        // 设置返回按钮
        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }
}
