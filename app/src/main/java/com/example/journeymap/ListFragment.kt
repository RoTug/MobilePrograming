package com.example.journeymap

import android.app.AlertDialog
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.journeymap.databinding.FragmentListBinding

class ListFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!

    private lateinit var dbHelper: DBHelper
    private lateinit var travelAdapter: TravelAdapter
    private var currentList: List<TravelItem> = emptyList()

    // Activity가 종료되고 다시 돌아왔을 때 리스트를 새롭게 리로드하는 처리기
    private val addEditLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // 새로 저장하거나 수정하고 돌아오면 무조건 DB를 다시 읽어와 화면을 갱신합니다.
        loadDataFromDB()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DBHelper(requireContext())

        // 1. 어댑터 초기화 및 이벤트 연결
        travelAdapter = TravelAdapter(
            itemList = emptyList(),
            onItemClick = { item ->
                // [필수 구현] 항목 클릭 시 -> 수정 모드로 데이터를 담아서 인텐트 실행
                val intent = Intent(requireContext(), AddEditActivity::class.java).apply {
                    putExtra("RECORD_NO", item.no)
                    putExtra("RECORD_PLACE", item.place)
                    putExtra("RECORD_DATE", item.visitDate)
                    putExtra("RECORD_MEMO", item.memo)
                    putExtra("RECORD_PHOTO", item.photoUri)
                }
                addEditLauncher.launch(intent)
            },
            onItemLongClick = { item, position ->
                // 롱클릭 시 어댑터 내부에서 컨텍스트 메뉴가 활성화됩니다.
            }
        )

        binding.rvTravelList.adapter = travelAdapter

        // 2. 우측 하단 팅 액션 버튼 클릭 시 -> 새 기록 추가 모드로 인텐트 실행
        binding.fabAdd.setOnClickListener {
            val intent = Intent(requireContext(), AddEditActivity::class.java)
            addEditLauncher.launch(intent)
        }

        // 3. 컨텍스트 메뉴를 Fragment에 등록 (이거 누락되면 롱클릭 메뉴 안 뜹니다!)
        registerForContextMenu(binding.rvTravelList)

        // Initial Data Load
        loadDataFromDB()
    }

    // 4. [필수 구현] 컨텍스트 메뉴(롱클릭 메뉴) 항목 선택 시 이벤트 처리
    override fun onContextItemSelected(item: MenuItem): Boolean {
        // 어댑터에서 방금 롱클릭한 아이템의 index 위치를 가져옵니다.
        val position = travelAdapter.longClickedPosition
        if (position < 0 || position >= currentList.size) return super.onContextItemSelected(item)

        val selectedItem = currentList[position]

        return when (item.itemId) {
            R.id.context_edit -> {
                // 수정 선택 시 클릭과 동일하게 인텐트 전달
                val intent = Intent(requireContext(), AddEditActivity::class.java).apply {
                    putExtra("RECORD_NO", selectedItem.no)
                    putExtra("RECORD_PLACE", selectedItem.place)
                    putExtra("RECORD_DATE", selectedItem.visitDate)
                    putExtra("RECORD_MEMO", selectedItem.memo)
                    putExtra("RECORD_PHOTO", selectedItem.photoUri)
                }
                addEditLauncher.launch(intent)
                true
            }
            R.id.context_delete -> {
                // [필수 구현] 삭제 시 경고 AlertDialog 띄우기
                AlertDialog.Builder(requireContext()).apply {
                    setTitle("기록 삭제")
                    setMessage("${selectedItem.place} 여행 기록을 정말 삭제하시겠습니까?")
                    setPositiveButton("삭제") { _, _ ->
                        dbHelper.deleteRecord(selectedItem.no)
                        Toast.makeText(requireContext(), "기록이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                        loadDataFromDB() // 삭제 후 리스트 갱신
                    }
                    setNegativeButton("취소", null)
                    show()
                }
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    // SQLite DB에서 최신 목록 가져오기
    fun loadDataFromDB() {
        val itemList = mutableListOf<TravelItem>()
        val cursor: Cursor = dbHelper.getAllRecords()

        if (cursor.moveToFirst()) {
            do {
                val no = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_NO))
                val place = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_PLACE))
                val visitDate = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_VISIT_DATE))
                val memo = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_MEMO))
                val photoUri = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_PHOTO_URI))

                itemList.add(TravelItem(no, place, visitDate, memo, photoUri))
            } while (cursor.moveToNext())
        }
        cursor.close()

        currentList = itemList

        if (itemList.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.rvTravelList.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.rvTravelList.visibility = View.VISIBLE
        }

        travelAdapter.updateData(itemList)
    }

    // MainActivity의 옵션 메뉴에서 정렬을 바꿀 때 호출되는 함수
    fun toggleSortOrder(descending: Boolean) {
        val cursor: Cursor = dbHelper.getAllRecords()
        val itemList = mutableListOf<TravelItem>()

        if (cursor.moveToFirst()) {
            do {
                val no = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_NO))
                val place = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_PLACE))
                val visitDate = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_VISIT_DATE))
                val memo = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_MEMO))
                val photoUri = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_PHOTO_URI))

                itemList.add(TravelItem(no, place, visitDate, memo, photoUri))
            } while (cursor.moveToNext())
        }
        cursor.close()

        // descending 값에 따라 리스트 정렬 뒤집기
        if (!descending) {
            // 오래된 순정렬 (no가 작은 순서대로)
            itemList.sortBy { it.no }
        } else {
            // 최신 순정렬 (no가 큰 순서대로)
            itemList.sortByDescending { it.no }
        }

        currentList = itemList
        travelAdapter.updateData(itemList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}