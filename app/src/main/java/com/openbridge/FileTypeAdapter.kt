package com.openbridge

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.openbridge.databinding.ItemFileTypeBinding

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
        holder.binding.typeIcon.setImageResource(type.iconRes)
        holder.binding.labelText.text = type.label

        val isSelected = position == selectedIndex
        holder.binding.root.isSelected = isSelected

        val context = holder.itemView.context

        // Premium icon styling
        val tintColor = when(type) {
            MimeDetector.FileType.VIDEO -> context.getColor(R.color.type_video)
            MimeDetector.FileType.AUDIO -> context.getColor(R.color.type_audio)
            MimeDetector.FileType.IMAGE -> context.getColor(R.color.type_image)
            MimeDetector.FileType.DOCUMENT -> context.getColor(R.color.type_document)
            MimeDetector.FileType.ARCHIVE -> context.getColor(R.color.type_archive)
            MimeDetector.FileType.APK -> context.getColor(R.color.type_apk)
            MimeDetector.FileType.TEXT -> context.getColor(R.color.type_text)
            else -> context.getColor(R.color.primary)
        }

        if (isSelected) {
            holder.binding.iconContainer.setCardBackgroundColor(tintColor)
            holder.binding.typeIcon.setColorFilter(context.getColor(R.color.on_surface))
        } else {
            holder.binding.iconContainer.setCardBackgroundColor(context.getColor(R.color.surface_container_highest))
            holder.binding.typeIcon.setColorFilter(tintColor)
        }

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
