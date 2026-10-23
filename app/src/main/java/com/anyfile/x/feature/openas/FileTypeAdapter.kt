package com.anyfile.x.feature.openas

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.anyfile.x.R
import com.anyfile.x.databinding.ItemFileTypeBinding
import com.anyfile.x.engine.MimeDetector

/**
 * Visual metadata for file categories: native vector icon, container color, icon tint, and display label.
 */
object CategoryVisuals {
    data class Visual(
        val iconRes: Int,
        val containerColor: Int,
        val iconTint: Int,
        val label: String
    )

    fun get(type: MimeDetector.FileType): Visual = when (type) {
        MimeDetector.FileType.VIDEO -> Visual(
            iconRes = R.drawable.ic_cat_video,
            containerColor = 0xFF3B2225.toInt(),
            iconTint = 0xFFFFB4AB.toInt(),
            label = "Video"
        )
        MimeDetector.FileType.AUDIO -> Visual(
            iconRes = R.drawable.ic_cat_audio,
            containerColor = 0xFF382916.toInt(),
            iconTint = 0xFFFFDDB3.toInt(),
            label = "Audio"
        )
        MimeDetector.FileType.IMAGE -> Visual(
            iconRes = R.drawable.ic_cat_image,
            containerColor = 0xFF163234.toInt(),
            iconTint = 0xFF78D9EC.toInt(),
            label = "Image"
        )
        MimeDetector.FileType.DOCUMENT -> Visual(
            iconRes = R.drawable.ic_cat_document,
            containerColor = 0xFF1A2A44.toInt(),
            iconTint = 0xFFA8C7FA.toInt(),
            label = "Document"
        )
        MimeDetector.FileType.ARCHIVE -> Visual(
            iconRes = R.drawable.ic_cat_archive,
            containerColor = 0xFF33271F.toInt(),
            iconTint = 0xFFD7C2B4.toInt(),
            label = "Archive"
        )
        MimeDetector.FileType.APK -> Visual(
            iconRes = R.drawable.ic_cat_apk,
            containerColor = 0xFF163422.toInt(),
            iconTint = 0xFF78DC77.toInt(),
            label = "APK"
        )
        MimeDetector.FileType.TEXT -> Visual(
            iconRes = R.drawable.ic_cat_text,
            containerColor = 0xFF22263F.toInt(),
            iconTint = 0xFFB9C3FF.toInt(),
            label = "Text"
        )
        MimeDetector.FileType.FONT -> Visual(
            iconRes = R.drawable.ic_cat_font,
            containerColor = 0xFF351F3A.toInt(),
            iconTint = 0xFFEEA8EB.toInt(),
            label = "Font"
        )
        MimeDetector.FileType.UNKNOWN -> Visual(
            iconRes = R.drawable.ic_cat_raw,
            containerColor = 0xFF2B2833.toInt(),
            iconTint = 0xFFC7C5D0.toInt(),
            label = "Raw"
        )
    }
}

/**
 * Grid adapter for the "Open as" type chooser.
 * Highlights the pre-detected (or user-selected) item.
 */
class FileTypeAdapter(
    private val types: List<MimeDetector.FileType>,
    private var selectedIndex: Int = -1,
    private val onTypeSelected: (MimeDetector.FileType) -> Unit
) : RecyclerView.Adapter<FileTypeAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemFileTypeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFileTypeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val type = types[position]
        val visual = CategoryVisuals.get(type)
        val context = holder.itemView.context

        holder.binding.labelText.text = visual.label
        holder.binding.iconImage.setImageResource(visual.iconRes)
        holder.binding.iconImage.setColorFilter(visual.iconTint)

        // Rounded-square background with distinct muted accent color
        val cornerPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            12f,
            context.resources.displayMetrics
        )
        val containerDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = cornerPx
            setColor(visual.containerColor)
        }
        holder.binding.iconContainer.background = containerDrawable

        val isSelected = position == selectedIndex
        holder.binding.root.isSelected = isSelected

        if (isSelected) {
            holder.binding.labelText.setTextColor(ContextCompat.getColor(context, R.color.tile_selected_border))
            holder.binding.labelText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        } else {
            holder.binding.labelText.setTextColor(ContextCompat.getColor(context, R.color.on_surface))
            holder.binding.labelText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        holder.binding.root.setOnClickListener {
            val prev = selectedIndex
            val newPos = holder.adapterPosition
            if (newPos != RecyclerView.NO_POSITION) {
                selectedIndex = newPos
                if (prev != RecyclerView.NO_POSITION && prev != selectedIndex) {
                    notifyItemChanged(prev)
                }
                notifyItemChanged(selectedIndex)
                onTypeSelected(type)
            }
        }
    }

    override fun getItemCount(): Int = types.size
}

