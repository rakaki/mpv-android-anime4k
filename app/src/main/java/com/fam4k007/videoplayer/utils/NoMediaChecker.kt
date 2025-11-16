package com.fam4k007.videoplayer.utils

import java.io.File

/**
 * .nomedia 文件检测工具
 * 用于检测文件夹中是否存在 .nomedia 文件
 * 
 * .nomedia 文件是 Android 系统的特殊标记文件，
 * 当文件夹中存在该文件时，系统媒体扫描器会跳过该文件夹及其子文件夹
 */
object NoMediaChecker {
    
    /**
     * 检查指定路径或其任何父目录是否包含 .nomedia 文件
     * 
     * @param path 要检查的文件或文件夹路径
     * @return true 如果路径或其任何父目录包含 .nomedia 文件
     */
    fun containsNoMedia(path: String?): Boolean {
        if (path.isNullOrBlank()) {
            return false
        }
        
        return try {
            var currentDir = File(path)
            
            // 如果是文件，从其父目录开始检查
            if (currentDir.isFile) {
                currentDir = currentDir.parentFile ?: return false
            }
            
            // 向上遍历目录层次结构，检查每个目录
            while (currentDir.exists()) {
                val noMediaFile = File(currentDir, ".nomedia")
                if (noMediaFile.exists()) {
                    android.util.Log.d("NoMediaChecker", "发现 .nomedia 文件: ${noMediaFile.absolutePath}")
                    return true
                }
                
                // 移动到父目录
                currentDir = currentDir.parentFile ?: break
            }
            
            false
        } catch (e: Exception) {
            android.util.Log.w("NoMediaChecker", "检查 .nomedia 文件时出错: ${e.message}")
            false
        }
    }
    
    /**
     * 检查指定文件夹是否直接包含 .nomedia 文件（不检查父目录）
     * 
     * @param folderPath 要检查的文件夹路径
     * @return true 如果该文件夹直接包含 .nomedia 文件
     */
    fun folderHasNoMedia(folderPath: String?): Boolean {
        if (folderPath.isNullOrBlank()) {
            return false
        }
        
        return try {
            val folder = File(folderPath)
            if (!folder.exists() || !folder.isDirectory) {
                return false
            }
            
            val noMediaFile = File(folder, ".nomedia")
            noMediaFile.exists()
        } catch (e: Exception) {
            android.util.Log.w("NoMediaChecker", "检查文件夹 .nomedia 时出错: ${e.message}")
            false
        }
    }
    
    /**
     * 从文件路径中提取文件夹路径并检查是否包含 .nomedia
     * 
     * @param filePath 文件完整路径
     * @return true 如果文件所在文件夹或其父目录包含 .nomedia 文件
     */
    fun fileInNoMediaFolder(filePath: String?): Boolean {
        if (filePath.isNullOrBlank()) {
            return false
        }
        
        val folderPath = filePath.substringBeforeLast("/", "")
        return if (folderPath.isNotEmpty()) {
            containsNoMedia(folderPath)
        } else {
            false
        }
    }
}
