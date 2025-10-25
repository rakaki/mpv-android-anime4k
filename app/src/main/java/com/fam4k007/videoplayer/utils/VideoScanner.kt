package com.fam4k007.videoplayer.utils

import android.content.Context
import android.provider.MediaStore

data class VideoItem(val name: String, val path: String, val folder: String)

object VideoScanner {
    fun getAllVideos(context: Context): List<VideoItem> {
        val videos = mutableListOf<VideoItem>()
        val projection = arrayOf(
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA
        )
        val cursor = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection, null, null, null
        )
        cursor?.use {
            val nameIdx = it.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
            val pathIdx = it.getColumnIndex(MediaStore.Video.Media.DATA)
            while (it.moveToNext()) {
                val name = it.getString(nameIdx)
                val path = it.getString(pathIdx)
                val folder = path.substringBeforeLast('/')
                videos.add(VideoItem(name, path, folder))
            }
        }
        return videos
    }
}
