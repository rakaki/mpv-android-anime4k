package com.fam4k007.videoplayer.dandanplay

import com.fam4k007.videoplayer.dandanplay.model.DanDanComment
import java.io.File
import java.io.FileOutputStream

/**
 * 弹弹play 弹幕格式转换器
 * 将 JSON 格式的弹幕转换为 Bilibili XML 格式
 */
object DanDanConverter {

    /**
     * 将弹弹play评论列表转换为 Bilibili XML 格式字符串
     */
    fun convertToXml(comments: List<DanDanComment>): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<i>\n")
        sb.append("    <chatserver>chat.bilibili.com</chatserver>\n")
        sb.append("    <chatid>1</chatid>\n")
        sb.append("    <mission>0</mission>\n")
        sb.append("    <maxlimit>1000</maxlimit>\n")
        sb.append("    <source>k-v</source>\n")
        sb.append("    <ds>0</ds>\n")
        sb.append("    <de>0</de>\n")
        sb.append("    <max_count>1000</max_count>\n")

        for (comment in comments) {
            val p = comment.p
            val text = escapeXml(comment.m)
            
            // p属性格式: 时间,类型,颜色,用户ID
            // B站格式: 时间,类型,字号,颜色,时间戳,池ID,用户Hash,弹幕ID
            // 我们需要补全缺失的字段，主要是字号(25)和时间戳等
            
            val parts = p.split(",")
            // 增加容错性：允许只有3个部分的旧数据，或未来可能变化的格式
            if (parts.size >= 3) {
                val time = parts[0]
                val type = parts[1]
                val color = parts[2]
                val uid = if (parts.size > 3) parts[3] else "0" // 如果缺少用户ID，使用默认值
                
                // 构造 B站格式的 p 属性
                // 默认字号 25, 时间戳用当前时间, 池ID 0, 弹幕ID 用 cid
                val newP = "$time,$type,25,$color,${System.currentTimeMillis() / 1000},0,$uid,${comment.cid}"
                
                sb.append("    <d p=\"$newP\">$text</d>\n")
            }
        }

        sb.append("</i>")
        return sb.toString()
    }

    /**
     * 保存弹幕到 XML 文件
     */
    fun saveToXmlFile(comments: List<DanDanComment>, outputFile: File) {
        val xmlContent = convertToXml(comments)
        FileOutputStream(outputFile).use { fos ->
            fos.write(xmlContent.toByteArray())
        }
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&")
            .replace("<", "<")
            .replace(">", ">")
            .replace("\"", """)
            .replace("'", "'")
    }
}
