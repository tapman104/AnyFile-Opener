package com.openbridge

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.openbridge.databinding.ItemRecentFileBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecentFileAdapter(
    private var items: List<RecentFile>,
    private val onItemClick: (RecentFile) -> Unit
) : RecyclerView.Adapter<RecentFileAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemRecentFileBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecentFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.fileNameText.text = item.fileName
        holder.binding.fileMimeText.text = item.mimeType
        
        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        holder.binding.fileTimeText.text = sdf.format(Date(item.timestamp))

        holder.binding.root.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<RecentFile>) {
        items = newItems
        notifyDataSetChanged()
    }
}
