package com.fam4k007.videoplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.cardview.widget.CardView

/**
 * 建议反馈页面
 */
class FeedbackActivity : BaseActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback)
        
        // 设置Toolbar
        setupToolbar(R.id.toolbar, getString(R.string.feedback), showBackButton = true)
        
        // GitHub Issue按钮点击
        findViewById<CardView>(R.id.btnGithubIssue).setOnClickListener {
            openUrl(AppConstants.URLs.GITHUB_ISSUES_URL)
        }
    }
    
    /**
     * 打开URL（使用系统默认浏览器）
     */
    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "无法打开浏览器",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
