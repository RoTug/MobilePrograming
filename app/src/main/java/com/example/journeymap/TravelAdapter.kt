package com.example.journeymap

import android.net.Uri
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.journeymap.databinding.ItemTravelBinding

class TravelAdapter(
    private var itemList: List<TravelItem>,
    private val onItemClick: (TravelItem) -> Unit,
    private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
    }

    private var displayList: List<Any> = emptyList()
    var isSelectionMode = false
    private val selectedNos = mutableSetOf<Int>()

    // 열려있는 폴더(그룹) 이름을 저장
    private val expandedGroups = mutableSetOf<String>()

    var longClickedPosition: Int = -1
        private set

    init {
        updateDisplayList()
    }

    private fun updateDisplayList() {
        val newList = mutableListOf<Any>()
        
        // 1. 그룹이 있는 항목들과 없는 항목들을 분리
        val grouped = itemList.groupBy { it.groupName ?: "기타" }
        
        // 2. 그룹이 없는 항목("기타")들을 리스트 최상단에 추가 (헤더 없이)
        grouped["기타"]?.let { ungroupedItems ->
            newList.addAll(ungroupedItems)
        }
        
        // 3. 이름이 있는 그룹들을 폴더 형태로 추가
        for ((groupName, items) in grouped) {
            if (groupName == "기타") continue
            
            newList.add(GroupHeader(groupName, items.size))
            
            // 폴더가 열려있을 때만 하위 아이템 추가
            if (expandedGroups.contains(groupName)) {
                newList.addAll(items)
            }
        }
        displayList = newList
    }

    data class GroupHeader(val name: String, val count: Int)

    inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvHeader: TextView = view.findViewById(R.id.tv_header_title)
        val tvCount: TextView = view.findViewById(R.id.tv_item_count)
        val ivArrow: ImageView = view.findViewById(R.id.iv_expand_arrow)

        fun bind(header: GroupHeader) {
            tvHeader.text = header.name
            tvCount.text = "${header.count}개의 기록"
            
            val isExpanded = expandedGroups.contains(header.name)
            ivArrow.rotation = if (isExpanded) 180f else 0f

            itemView.setOnClickListener {
                if (isExpanded) {
                    expandedGroups.remove(header.name)
                } else {
                    expandedGroups.add(header.name)
                }
                updateDisplayList()
                notifyDataSetChanged()
            }
        }
    }

    inner class TravelViewHolder(private val binding: ItemTravelBinding) :
        RecyclerView.ViewHolder(binding.root), View.OnCreateContextMenuListener {

        init {
            binding.root.setOnCreateContextMenuListener(this)
        }

        fun bind(item: TravelItem) {
            binding.tvPlace.text = item.place
            binding.tvDate.text = item.visitDate

            if (!item.photoUri.isNullOrEmpty()) {
                binding.ivThumbnail.setImageURI(Uri.parse(item.photoUri))
            } else {
                binding.ivThumbnail.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            binding.cbSelect.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
            binding.cbSelect.isChecked = selectedNos.contains(item.no)

            binding.root.setOnClickListener {
                if (isSelectionMode) {
                    toggleSelection(item.no)
                } else {
                    onItemClick(item)
                }
            }

            binding.root.setOnLongClickListener {
                if (!isSelectionMode) {
                    longClickedPosition = adapterPosition
                    false
                } else false
            }
            
            binding.cbSelect.setOnClickListener {
                toggleSelection(item.no)
            }
        }

        override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
            if (!isSelectionMode) {
                menu?.setHeaderTitle("선택한 여행 기록")
                menu?.add(0, R.id.context_edit, 0, "기록 수정")
                menu?.add(0, R.id.context_group, 1, "그룹으로 묶기 (다중 선택)")
                menu?.add(0, R.id.context_delete, 2, "기록 삭제")
            }
        }
    }

    private fun toggleSelection(no: Int) {
        if (selectedNos.contains(no)) {
            selectedNos.remove(no)
        } else {
            selectedNos.add(no)
        }
        notifyDataSetChanged()
        onSelectionChanged(selectedNos.size)
    }

    fun startSelectionMode(position: Int) {
        val item = getItemAt(position) ?: return
        isSelectionMode = true
        selectedNos.add(item.no)
        
        val group = item.groupName ?: "기타"
        if (group != "기타") {
            expandedGroups.add(group)
        }

        updateDisplayList()
        notifyDataSetChanged()
        onSelectionChanged(selectedNos.size)
    }

    fun getSelectedNos(): List<Int> = selectedNos.toList()

    fun clearSelection() {
        isSelectionMode = false
        selectedNos.clear()
        notifyDataSetChanged()
        onSelectionChanged(0)
    }

    override fun getItemViewType(position: Int): Int {
        return if (displayList[position] is GroupHeader) VIEW_TYPE_HEADER else VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_travel_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val binding = ItemTravelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            TravelViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = displayList[position]
        if (holder is HeaderViewHolder && item is GroupHeader) {
            holder.bind(item)
        } else if (holder is TravelViewHolder && item is TravelItem) {
            holder.bind(item)
        }
    }

    override fun getItemCount(): Int = displayList.size

    fun getItemAt(position: Int): TravelItem? {
        if (position < 0 || position >= displayList.size) return null
        return displayList[position] as? TravelItem
    }

    fun updateData(newList: List<TravelItem>) {
        this.itemList = newList
        updateDisplayList()
        notifyDataSetChanged()
    }
}