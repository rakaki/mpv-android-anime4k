package com.fam4k007.videoplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

/**
 * 关于信息页面（包含关于、许可证、联系方式等）
 */
class AboutActivity : BaseActivity() {
    
    private lateinit var layoutAbout: View
    private lateinit var layoutGithub: View
    private lateinit var layoutLicense: View
    private lateinit var layoutContact: View
    private lateinit var layoutFeedback: View
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        
        // 设置Toolbar
        setupToolbar(R.id.toolbar, getString(R.string.about), showBackButton = true)
        
        // 初始化视图
        layoutAbout = findViewById(R.id.layoutAbout)
        layoutGithub = findViewById(R.id.layoutGithub)
        layoutLicense = findViewById(R.id.layoutLicense)
        layoutContact = findViewById(R.id.layoutContact)
        layoutFeedback = findViewById(R.id.layoutFeedback)
        
        // 设置监听器
        setupListeners()
    }
    
    /**
     * 设置监听器
     */
    private fun setupListeners() {
        // 关于软件项点击
        layoutAbout.setOnClickListener {
            showAboutDialog()
        }
        
        // 开源主页
        layoutGithub.setOnClickListener {
            openUrl(AppConstants.URLs.GITHUB_URL)
        }
        
        // 许可证书
        layoutLicense.setOnClickListener {
            startActivity(Intent(this, LicenseActivity::class.java))
            startActivityWithDefaultTransition()
        }
        
        // 联系作者
        layoutContact.setOnClickListener {
            openUrl(AppConstants.URLs.CONTACT_URL)
        }
        
        // 意见反馈
        layoutFeedback.setOnClickListener {
            startActivity(Intent(this, FeedbackActivity::class.java))
            startActivityWithDefaultTransition()
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
    
    /**
     * 显示关于应用的详细信息对话框
     */
    private fun showAboutDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_about_detail, null)
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        // 设置对话框背景透明
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // 关闭按钮点击事件
        dialogView.findViewById<Button>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
}
