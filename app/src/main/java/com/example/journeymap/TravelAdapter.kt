package com.example.journeymap

import android.net.Uri
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    // 롱클릭된 아이템 위치 저장 (Fragment에서 사용)
    var longClickedPosition: Int = -1
        private set

    init {
        updateDisplayList()
    }

    private fun updateDisplayList() {
        val newList = mutableListOf<Any>()
        val grouped = itemList.groupBy { it.groupName ?: "기타" }
        
        for ((groupName, items) in grouped) {
            newList.add(groupName)
            newList.addAll(items)
        }
        displayList = newList
    }

    inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvHeader: TextView = view.findViewById(R.id.tv_header_title)
    }

    inner class TravelViewHolder(private val binding: ItemTravelBinding) :
        RecyclerView.ViewHolder(binding.root), View.OnCreateContextMenuListener {

        init {
            // 다시 컨텍스트 메뉴 리스너 등록
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
                    false // Fragment의 컨텍스트 메뉴를 띄우기 위해 false 반환
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

    // 외부에서 특정 아이템을 시작으로 선택 모드를 켜는 함수
    fun startSelectionMode(position: Int) {
        val item = getItemAt(position) ?: return
        isSelectionMode = true
        selectedNos.add(item.no)
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
        return if (displayList[position] is String) VIEW_TYPE_HEADER else VIEW_TYPE_ITEM
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
        if (holder is HeaderViewHolder && item is String) {
            holder.tvHeader.text = item
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