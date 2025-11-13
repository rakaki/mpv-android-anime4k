package com.fam4k007.videoplayer.webdav

import android.app.Dialog
import android.content.Context
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleCoroutineScope
import com.fam4k007.videoplayer.R
import com.fam4k007.videoplayer.databinding.DialogWebdavConfigBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * WebDAV 配置对话框
 */
class WebDavConfigDialog(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val onConfigSaved: (WebDavConfig) -> Unit
) {

    private lateinit var binding: DialogWebdavConfigBinding
    private var dialog: Dialog? = null
    private val config: WebDavConfig = WebDavConfig.load(context)

    fun show() {
        binding = DialogWebdavConfigBinding.inflate(LayoutInflater.from(context))
        
        // 加载已保存的配置
        binding.etServerUrl.setText(config.serverUrl)
        binding.etDisplayName.setText(config.displayName)
        binding.etAccount.setText(config.account)
        binding.etPassword.setText(config.password)
        
        // 设置登录模式
        updateLoginMode(config.isAnonymous)
        
        // 登录模式切换
        binding.btnAccountLogin.setOnClickListener {
            updateLoginMode(false)
        }
        
        binding.btnAnonymousLogin.setOnClickListener {
            updateLoginMode(true)
        }
        
        // 密码显示/隐藏
        binding.ivPasswordToggle.setOnClickListener {
            if (binding.ivPasswordToggle.isSelected) {
                binding.ivPasswordToggle.isSelected = false
                binding.etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            } else {
                binding.ivPasswordToggle.isSelected = true
                binding.etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
            }
            binding.etPassword.setSelection(binding.etPassword.text.length)
        }
        
        // 测试连接
        binding.btnTestConnection.setOnClickListener {
            testConnection()
        }
        
        // 创建对话框
        dialog = AlertDialog.Builder(context)
            .setTitle("WebDAV 配置")
            .setView(binding.root)
            .setPositiveButton("保存") { _, _ ->
                saveConfig()
            }
            .setNegativeButton("取消", null)
            .create()
        
        dialog?.show()
    }

    private fun updateLoginMode(isAnonymous: Boolean) {
        config.isAnonymous = isAnonymous
        
        binding.btnAccountLogin.isSelected = !isAnonymous
        binding.btnAnonymousLogin.isSelected = isAnonymous
        
        binding.layoutAccountInfo.visibility = if (isAnonymous) View.GONE else View.VISIBLE
        
        if (isAnonymous) {
            binding.etAccount.setText("")
            binding.etPassword.setText("")
        }
    }

    private fun testConnection() {
        // 获取当前输入的值
        val serverUrl = binding.etServerUrl.text.toString().trim()
        val account = binding.etAccount.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val isAnonymous = binding.btnAnonymousLogin.isSelected
        
        // 验证输入
        if (serverUrl.isEmpty()) {
            Toast.makeText(context, "请填写服务器地址", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
            Toast.makeText(context, "服务器地址必须以 http:// 或 https:// 开头", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!isAnonymous) {
            if (account.isEmpty()) {
                Toast.makeText(context, "请填写账号", Toast.LENGTH_SHORT).show()
                return
            }
            if (password.isEmpty()) {
                Toast.makeText(context, "请填写密码", Toast.LENGTH_SHORT).show()
                return
            }
        }
        
        // 创建临时配置进行测试
        val testConfig = WebDavConfig(
            serverUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/",
            account = account,
            password = password,
            isAnonymous = isAnonymous
        )
        
        // 显示测试中状态
        binding.tvConnectionStatus.visibility = View.VISIBLE
        binding.tvConnectionStatus.text = "测试中..."
        binding.tvConnectionStatus.setTextColor(context.getColor(android.R.color.darker_gray))
        binding.btnTestConnection.isEnabled = false
        
        // 异步测试连接
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val client = WebDavClient(testConfig)
                    client.testConnection()
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }
            
            // 更新状态
            binding.btnTestConnection.isEnabled = true
            if (result) {
                binding.tvConnectionStatus.text = "连接成功 ✓"
                binding.tvConnectionStatus.setTextColor(context.getColor(R.color.text_blue))
                Toast.makeText(context, "连接成功！", Toast.LENGTH_SHORT).show()
            } else {
                binding.tvConnectionStatus.text = "连接失败 ✗"
                binding.tvConnectionStatus.setTextColor(context.getColor(R.color.text_red))
                Toast.makeText(context, "连接失败，请检查配置", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveConfig() {
        val serverUrl = binding.etServerUrl.text.toString().trim()
        val displayName = binding.etDisplayName.text.toString().trim()
        val account = binding.etAccount.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val isAnonymous = binding.btnAnonymousLogin.isSelected
        
        // 验证输入
        if (serverUrl.isEmpty()) {
            Toast.makeText(context, "请填写服务器地址", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
            Toast.makeText(context, "服务器地址必须以 http:// 或 https:// 开头", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!isAnonymous && account.isEmpty()) {
            Toast.makeText(context, "请填写账号", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 保存配置
        val newConfig = WebDavConfig(
            serverUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/",
            displayName = displayName.ifEmpty { "WebDAV媒体库" },
            account = account,
            password = password,
            isAnonymous = isAnonymous
        )
        
        WebDavConfig.save(context, newConfig)
        Toast.makeText(context, "配置已保存", Toast.LENGTH_SHORT).show()
        onConfigSaved(newConfig)
    }

    fun dismiss() {
        dialog?.dismiss()
    }
}
