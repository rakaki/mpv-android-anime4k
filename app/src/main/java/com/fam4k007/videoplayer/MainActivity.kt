package com.fam4k007.videoplayer

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.fam4k007.videoplayer.utils.ThemeManager

class MainActivity : AppCompatActivity() {
    
    private lateinit var bottomNav: BottomNavigationView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bottomNav = findViewById(R.id.bottomNavigationView)
        val btnSelectVideo = findViewById<Button>(R.id.btnSelectVideo)
        bottomNav.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> true
                R.id.nav_settings -> {
                    // 跳转到设置页面
                    startActivity(Intent(this, SettingsActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    true
                }
                else -> false
            }
        }
        btnSelectVideo.setOnClickListener {
            // 打开视频浏览器
            val intent = android.content.Intent(this, VideoBrowserActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 从设置页面返回时,重置底部导航栏到主页状态
        bottomNav.selectedItemId = R.id.nav_home
    }
}
