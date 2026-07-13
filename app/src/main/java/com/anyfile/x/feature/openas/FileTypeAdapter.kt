package com.anyfile.x

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.anyfile.x.databinding.ItemFileTypeBinding

/**
 * Grid adapter for the "Open as" type chooser.
 * Highlights the pre-detected (or user-selected) item.
 */
class FileTypeAdapter(
    private val types: List<MimeDetector.FileType>,
    private var selectedIndex: Int = 0,
    private val onTypeSelected: (MimeDetector.FileType) -> Unit
) : RecyclerView.Adapter<FileTypeAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemFileTypeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFileTypeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val type = types[position]
        holder.binding.emojiText.text = type.emoji
        holder.binding.labelText.text = type.label
        holder.binding.root.isSelected = position == selectedIndex
        holder.binding.root.setOnClickListener {
            val prev = selectedIndex
            selectedIndex = position
            notifyItemChanged(prev)
            notifyItemChanged(selectedIndex)
            onTypeSelected(type)
        }
    }

    override fun getItemCount(): Int = types.size
}
