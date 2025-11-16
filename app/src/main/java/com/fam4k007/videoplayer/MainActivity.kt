package com.fam4k007.videoplayer

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.fam4k007.videoplayer.databinding.ActivityMainBinding
import com.fam4k007.videoplayer.fragments.HomeFragment
import com.fam4k007.videoplayer.utils.ThemeManager

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        
        // 检查用户是否已同意协议
        if (!UserAgreementActivity.isAgreed(this)) {
            // 未同意，跳转到协议页面
            val intent = Intent(this, UserAgreementActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 默认显示首页（唯一页面）
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()
        }
    }
}

