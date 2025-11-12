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
    private var currentFragment: Fragment? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 默认显示首页
        if (savedInstanceState == null) {
            switchFragment(HomeFragment())
        }
        
        // 底部导航切换
        binding.bottomNavigationView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> {
                    switchFragment(HomeFragment())
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    true
                }
                else -> false
            }
        }
    }
    
    private fun switchFragment(fragment: Fragment) {
        if (currentFragment?.javaClass == fragment.javaClass) return
        
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.fade_in,
                R.anim.fade_out
            )
            .replace(R.id.fragment_container, fragment)
            .commit()
        
        currentFragment = fragment
    }
    
    override fun onResume() {
        super.onResume()
        // 从设置页面返回时,重置底部导航栏到当前状态
        if (currentFragment is HomeFragment) {
            binding.bottomNavigationView.selectedItemId = R.id.nav_home
        }
    }
}
