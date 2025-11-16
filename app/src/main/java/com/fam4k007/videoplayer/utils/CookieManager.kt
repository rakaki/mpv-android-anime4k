package com.fam4k007.videoplayer.utils

import android.content.Context
import com.fam4k007.videoplayer.bilibili.auth.BiliBiliAuthManager

object CookieManager {
    fun getBilibiliCookie(context: Context): String {
        return BiliBiliAuthManager.getInstance(context).getCookieString()
    }
}