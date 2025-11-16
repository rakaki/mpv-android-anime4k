package com.fam4k007.videoplayer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class UserAgreementActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME = "user_agreement_prefs"
        private const val KEY_AGREED = "user_agreed"

        fun isAgreed(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_AGREED, false)
        }

        fun setAgreed(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_AGREED, true).apply()
        }
    }

    private lateinit var agreementText: TextView
    private lateinit var checkBox: CheckBox
    private lateinit var btnAgree: Button
    private lateinit var btnDecline: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_agreement)

        agreementText = findViewById(R.id.agreement_text)
        checkBox = findViewById(R.id.checkbox_agree)
        btnAgree = findViewById(R.id.btn_agree)
        btnDecline = findViewById(R.id.btn_decline)

        // 设置协议内容
        agreementText.movementMethod = ScrollingMovementMethod()
        agreementText.text = getAgreementContent()

        // 初始状态确定按钮不可用
        btnAgree.isEnabled = false

        // 监听复选框状态
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            btnAgree.isEnabled = isChecked
        }

        // 同意按钮
        btnAgree.setOnClickListener {
            if (checkBox.isChecked) {
                setAgreed(this)
                // 启动主界面
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

        // 拒绝按钮
        btnDecline.setOnClickListener {
            // 直接退出应用
            finishAffinity()
        }
    }

    override fun onBackPressed() {
        // 禁用返回键，必须选择同意或拒绝
        // 不调用 super.onBackPressed()
    }

    private fun getAgreementContent(): String {
        return """
            用户服务协议与隐私政策
            
            欢迎使用小喵player！在使用本应用前，请您仔细阅读并充分理解以下条款。
            
            ═══════════════════════════════════
            
            一、重要声明
            
            1. 本应用完全免费且开源，遵守 GPL-3.0-or-later 开源协议
            2. 本应用旨在学习技术与测试代码，切勿滥用
            3. 我们强烈反对且不纵容任何形式的盗版、非法转载、黑产及其他违法用途或行为
            
            ═══════════════════════════════════
            
            二、隐私政策
            
            【数据收集】
            ✓ 本应用不收集任何用户个人信息
            ✓ 本应用不上传任何数据到服务器（我们没有服务器）
            ✓ 本应用不分享用户数据给任何第三方
            ✓ 所有功能均在本地设备上运行
            
            【权限说明】
            本应用需要申请以下权限：
            
            • 存储权限：用于扫描本地视频文件、保存字幕和弹幕文件
            • 网络权限：用于以下功能
              - 哔哩哔哩番剧在线播放
              - 下载弹幕文件
              - 下载视频/番剧（仅供个人学习）
              - WebDAV 云端视频播放
            
            【登录信息安全】
            如您选择使用哔哩哔哩登录功能：
            • 登录凭证使用 AES-256 军事级加密存储在本地
            • 登录密钥由 Android KeyStore 硬件保护，应用无法导出
            • 登录信息仅用于调用B站API，不会上传到任何其他地方
            • 您可随时在设置中一键退出登录
            • 应用卸载后，所有登录数据将自动永久销毁
            
            ═══════════════════════════════════
            
            三、法律风险警告
            
            【视频/番剧下载功能】
            ⚠️ 重要警告：
            
            1. 本功能仅供个人学习与技术交流使用
            2. 严禁用于任何商业用途，包括但不限于：
               × 二次传播、倒卖、去水印上传
               × 商业放映、广告盈利
               × 侵犯版权方权益的任何行为
            
            3. 下载的视频内容版权归原作者所有
            4. 建议下载后24小时内删除
            5. 建议仅下载自己有版权或创作的内容
            
            【免责声明】
            • 因使用本应用而产生的任何后果（包括但不限于非法用途、账号风控或其他损失），均由用户个人承担
            • 与开发者无关，开发者概不负责
            • 若因使用本应用下载功能进行商业活动而造成的法律风险，请用户自行承担
            • 本应用不对下载功能的滥用负责
            
            【版权声明】
            • "哔哩哔哩" 及 "Bilibili" 名称、LOGO及相关图形是上海幻电信息科技有限公司的注册商标
            • 本应用为独立的第三方工具，与哔哩哔哩及其关联公司无任何关联
            • 使用本应用获取的内容，其版权归原权利人所有
            • 请遵守相关法律法规及平台服务协议
            
            ═══════════════════════════════════
            
            四、用户承诺
            
            点击"同意并继续"即表示您：
            
            ✓ 已完整阅读并充分理解以上所有条款
            ✓ 同意遵守本协议的所有内容
            ✓ 承诺不将本应用用于任何违法或商业用途
            ✓ 理解并接受使用本应用的所有风险由您个人承担
            ✓ 同意开发者对本应用功能的滥用不承担任何责任
            
            点击"拒绝"将无法使用本应用。
            
            ═══════════════════════════════════
            
            感谢您的理解与配合！
            
            最后更新时间：2025年11月16日
        """.trimIndent()
    }
}
