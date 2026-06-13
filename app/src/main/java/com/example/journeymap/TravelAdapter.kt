package com.example.journeymap

import android.net.Uri
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.journeymap.databinding.ItemTravelBinding

class TravelAdapter(
    private var itemList: List<TravelItem>,
    private val onItemClick: (TravelItem) -> Unit,
    private val onItemLongClick: (TravelItem, position: Int) -> Unit
) : RecyclerView.Adapter<TravelAdapter.TravelViewHolder>() {

    // 롱클릭된 아이템의 위치를 기억하기 위한 변수 (컨텍스트 메뉴에서 활용)
    var longClickedPosition: Int = -1
        private set

    inner class TravelViewHolder(private val binding: ItemTravelBinding) :
        RecyclerView.ViewHolder(binding.root), View.OnCreateContextMenuListener {

        init {
            // 컨텍스트 메뉴 리스너 등록
            binding.root.setOnCreateContextMenuListener(this)
        }

        fun bind(item: TravelItem) {
            binding.tvPlace.text = item.place
            binding.tvDate.text = item.visitDate

            // 이미지 URI가 있다면 로드, 없으면 기본 이미지
            if (!item.photoUri.isNullOrEmpty()) {
                binding.ivThumbnail.setImageURI(Uri.parse(item.photoUri))
            } else {
                binding.ivThumbnail.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            // [필수 구현] 항목 클릭 이벤트 처리
            binding.root.setOnClickListener {
                onItemClick(item)
            }

            // 롱클릭 시 현재 위치 저장 및 콜백 호출
            binding.root.setOnLongClickListener {
                longClickedPosition = adapterPosition
                onItemLongClick(item, adapterPosition)
                false // false를 반환해야 onCreateContextMenu가 정상 호출됨
            }
        }

        // [필수 구현] 컨텍스트 메뉴 생성
        override fun onCreateContextMenu(
            menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?
        ) {
            menu?.setHeaderTitle("선택한 여행 기록")
            menu?.add(0, R.id.context_edit, 0, "기록 수정")
            menu?.add(0, R.id.context_delete, 1, "기록 삭제")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TravelViewHolder {
        val binding = ItemTravelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TravelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TravelViewHolder, position: Int) {
        holder.bind(itemList[position])
    }

    override fun getItemCount(): Int = itemList.size

    // 데이터가 갱신되었을 때 리스트를 업데이트하는 함수
    fun updateData(newList: List<TravelItem>) {
        this.itemList = newList
        notifyDataSetChanged()
    }
}