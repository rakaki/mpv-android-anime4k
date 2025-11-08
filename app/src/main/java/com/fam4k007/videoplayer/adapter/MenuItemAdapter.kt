package com.fam4k007.videoplayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fam4k007.videoplayer.R

/**
 * 菜单项适配器（用于不需要选中状态的菜单列表）
 */
class MenuItemAdapter(
    private val items: List<String>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<MenuItemAdapter.ViewHolder>() {

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

            // 所有项都显示为未选中状态
            itemText.setTextColor(android.graphics.Color.parseColor("#333333"))
            itemText.setTypeface(null, android.graphics.Typeface.NORMAL)

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
