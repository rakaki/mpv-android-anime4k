package com.fam4k007.videoplayer.utils

import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log

/**
 * MediaStore 同步工具
 * 用于触发系统媒体扫描和清理已删除文件的缓存
 */
object MediaStoreSync {
    private const val TAG = "MediaStoreSync"
    
    /**
     * 触发全盘媒体扫描（慎用，耗时较长）
     */
    fun triggerFullMediaScan(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ 使用 MediaStore.scanVolume
                val volumeName = MediaStore.VOLUME_EXTERNAL_PRIMARY
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf("/storage/emulated/0/"),
                    null
                ) { path, uri ->
                    Log.d(TAG, "媒体扫描完成: $path -> $uri")
                }
            } else {
                // Android 9 及以下使用广播
                context.sendBroadcast(
                    Intent(Intent.ACTION_MEDIA_MOUNTED).apply {
                        data = Uri.parse("file://${android.os.Environment.getExternalStorageDirectory()}")
                    }
                )
            }
            Log.d(TAG, "已触发全盘媒体扫描")
        } catch (e: Exception) {
            Log.e(TAG, "触发媒体扫描失败", e)
        }
    }
    
    /**
     * 扫描指定文件夹
     * @param context Context
     * @param folderPath 文件夹路径
     */
    fun scanFolder(context: Context, folderPath: String, callback: ((String, Uri?) -> Unit)? = null) {
        try {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(folderPath),
                null
            ) { path, uri ->
                Log.d(TAG, "文件夹扫描完成: $path -> $uri")
                callback?.invoke(path, uri)
            }
        } catch (e: Exception) {
            Log.e(TAG, "扫描文件夹失败: $folderPath", e)
        }
    }
    
    /**
     * 扫描单个文件
     * @param context Context
     * @param filePath 文件路径
     */
    fun scanFile(context: Context, filePath: String, callback: ((String, Uri?) -> Unit)? = null) {
        try {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(filePath),
                null
            ) { path, uri ->
                Log.d(TAG, "文件扫描完成: $path -> $uri")
                callback?.invoke(path, uri)
            }
        } catch (e: Exception) {
            Log.e(TAG, "扫描文件失败: $filePath", e)
        }
    }
    
    /**
     * 清理MediaStore中已删除文件的记录
     * 注意：这个方法需要在后台线程执行
     * 
     * @param context Context
     * @return 清理的文件数量
     */
    fun cleanupDeletedFiles(context: Context): Int {
        var deletedCount = 0
        try {
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DATA
            )
            
            val cursor = context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                null
            )
            
            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val pathColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                
                val idsToDelete = mutableListOf<Long>()
                
                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val path = it.getString(pathColumn)
                    
                    // 检查文件是否存在
                    if (!java.io.File(path).exists()) {
                        idsToDelete.add(id)
                        Log.d(TAG, "发现已删除文件: $path")
                    }
                }
                
                // 批量删除MediaStore记录
                for (id in idsToDelete) {
                    try {
                        val uri = Uri.withAppendedPath(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            id.toString()
                        )
                        context.contentResolver.delete(uri, null, null)
                        deletedCount++
                        Log.d(TAG, "已清理MediaStore记录: ID=$id")
                    } catch (e: Exception) {
                        Log.e(TAG, "清理记录失败: ID=$id", e)
                    }
                }
            }
            
            Log.d(TAG, "MediaStore清理完成，共清理 $deletedCount 条记录")
        } catch (e: Exception) {
            Log.e(TAG, "清理MediaStore失败", e)
        }
        
        return deletedCount
    }
}
