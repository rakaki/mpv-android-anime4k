package com.fam4k007.videoplayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fam4k007.videoplayer.R
import com.fam4k007.videoplayer.utils.ThemeManager

/**
 * 通用选择适配器（用于音频轨道、字幕轨道等单选列表）
 */
class SelectionAdapter(
    private val items: List<String>,
    private var selectedPosition: Int,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<SelectionAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemText: TextView = view.findViewById(R.id.itemText)
        val innerLayout: LinearLayout = view.findViewById(R.id.innerLayout)

        fun bind(position: Int) {
            itemText.text = items[position]

            // 禁用点击反馈效果
            itemView.isClickable = true
            itemView.isFocusable = true
            itemView.background = null
            innerLayout.background = null

            // 如果 selectedPosition 为 -1，表示所有项都显示为未选中状态（如"更多"菜单）
            if (selectedPosition == -1) {
                itemText.setTextColor(android.graphics.Color.parseColor("#333333"))
            } else {
                // 正常的选中/未选中状态（如字幕轨道）
                val isSelected = position == selectedPosition
                if (isSelected) {
                    itemText.setTextColor(
                        ThemeManager.getThemeColor(
                            itemView.context,
                            com.google.android.material.R.attr.colorPrimary
                        )
                    )
                    itemText.setTypeface(null, android.graphics.Typeface.BOLD)
                } else {
                    itemText.setTextColor(android.graphics.Color.parseColor("#333333"))
                    itemText.setTypeface(null, android.graphics.Typeface.NORMAL)
                }
            }

            itemView.setOnClickListener {
                onItemClick(position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.dialog_selection_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount() = items.size
}
