package com.fam4k007.videoplayer.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.fam4k007.videoplayer.*
import com.fam4k007.videoplayer.utils.ThemeManager

/**
 * 设置Fragment
 * 在底部导航栏中显示，保持底部导航可见
 */
class SettingsFragment : Fragment() {

    private lateinit var layoutThemeSettings: View
    private lateinit var layoutPlaybackHistory: View
    private lateinit var layoutPlaybackSettings: View
    private lateinit var layoutBiliBiliDanmaku: View
    private lateinit var layoutHelp: View
    private lateinit var layoutAboutInfo: View
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings_custom, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 初始化视图
        layoutThemeSettings = view.findViewById(R.id.layoutThemeSettings)
        layoutPlaybackHistory = view.findViewById(R.id.layoutPlaybackHistory)
        layoutPlaybackSettings = view.findViewById(R.id.layoutPlaybackSettings)
        layoutBiliBiliDanmaku = view.findViewById(R.id.layoutBiliBiliDanmaku)
        layoutHelp = view.findViewById(R.id.layoutHelp)
        layoutAboutInfo = view.findViewById(R.id.layoutAboutInfo)
        
        // 设置监听器
        setupListeners()
    }
    
    /**
     * 设置监听器
     */
    private fun setupListeners() {
        // 主题设置点击
        layoutThemeSettings.setOnClickListener {
            showThemeDialog()
        }
        
        // 播放历史记录入口点击
        layoutPlaybackHistory.setOnClickListener {
            startActivity(Intent(requireContext(), PlaybackHistoryActivity::class.java))
            requireActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
        
        // 播放设置入口点击
        layoutPlaybackSettings.setOnClickListener {
            startActivity(Intent(requireContext(), PlaybackSettingsActivity::class.java))
            requireActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
        
        // 哔哩哔哩弹幕下载点击
        layoutBiliBiliDanmaku.setOnClickListener {
            startActivity(Intent(requireContext(), BiliBiliDanmakuActivity::class.java))
            requireActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
        
        // 使用说明入口点击
        layoutHelp.setOnClickListener {
            startActivity(Intent(requireContext(), HelpActivity::class.java))
            requireActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
        
        // 关于信息入口点击
        layoutAboutInfo.setOnClickListener {
            startActivity(Intent(requireContext(), AboutActivity::class.java))
            requireActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
    }
    
    /**
     * 显示主题选择对话框
     */
    private fun showThemeDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_theme_selector, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        
        // 获取控件
        val radioGroupTheme = dialogView.findViewById<RadioGroup>(R.id.radioGroupTheme)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirm)
        
        // 设置当前主题选中状态
        when (ThemeManager.getCurrentTheme(requireContext())) {
            ThemeManager.Theme.BLUE_PURPLE -> dialogView.findViewById<RadioButton>(R.id.radioThemeBluePurple).isChecked = true
            ThemeManager.Theme.GREEN -> dialogView.findViewById<RadioButton>(R.id.radioThemeGreen).isChecked = true
            ThemeManager.Theme.ORANGE -> dialogView.findViewById<RadioButton>(R.id.radioThemeOrange).isChecked = true
            ThemeManager.Theme.PINK -> dialogView.findViewById<RadioButton>(R.id.radioThemePink).isChecked = true
            ThemeManager.Theme.INDIGO -> dialogView.findViewById<RadioButton>(R.id.radioThemeIndigo).isChecked = true
        }
        
        // 取消按钮
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        // 确定按钮
        btnConfirm.setOnClickListener {
            // 获取选中的主题
            val selectedTheme = when (radioGroupTheme.checkedRadioButtonId) {
                R.id.radioThemeBluePurple -> ThemeManager.Theme.BLUE_PURPLE
                R.id.radioThemeGreen -> ThemeManager.Theme.GREEN
                R.id.radioThemeOrange -> ThemeManager.Theme.ORANGE
                R.id.radioThemePink -> ThemeManager.Theme.PINK
                R.id.radioThemeIndigo -> ThemeManager.Theme.INDIGO
                else -> ThemeManager.Theme.BLUE_PURPLE
            }
            
            // 应用主题
            ThemeManager.setTheme(requireContext(), selectedTheme)
            
            dialog.dismiss()
            
            // 重启当前Activity以应用主题
            requireActivity().recreate()
        }
        
        // 设置窗口背景为透明，让布局的圆角生效
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        dialog.show()
    }
}
