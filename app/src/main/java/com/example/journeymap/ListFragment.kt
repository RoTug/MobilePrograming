package com.example.journeymap

import android.app.AlertDialog
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.journeymap.databinding.FragmentListBinding

class ListFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!

    private lateinit var dbHelper: DBHelper
    private lateinit var travelAdapter: TravelAdapter

    private val addEditLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
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

        travelAdapter = TravelAdapter(
            itemList = emptyList(),
            onItemClick = { item ->
                val intent = Intent(requireContext(), AddEditActivity::class.java).apply {
                    putExtra("RECORD_NO", item.no)
                    putExtra("RECORD_PLACE", item.place)
                    putExtra("RECORD_DATE", item.visitDate)
                    putExtra("RECORD_MEMO", item.memo)
                    putExtra("RECORD_PHOTO", item.photoUri)
                    putExtra("RECORD_GROUP", item.groupName)
                    item.latitude?.let { putExtra("RECORD_LAT", it) }
                    item.longitude?.let { putExtra("RECORD_LNG", it) }
                }
                addEditLauncher.launch(intent)
            },
            onSelectionChanged = { count ->
                updateSelectionUI(count)
            }
        )

        binding.rvTravelList.adapter = travelAdapter
        
        // 중요: RecyclerView를 컨텍스트 메뉴에 등록
        registerForContextMenu(binding.rvTravelList)
        
        binding.btnCancelSelection.setOnClickListener {
            travelAdapter.clearSelection()
        }

        binding.btnGroupAction.setOnClickListener {
            showGroupRenameDialog()
        }

        loadDataFromDB()

        binding.fabAdd.setOnClickListener {
            val intent = Intent(requireContext(), AddEditActivity::class.java)
            addEditLauncher.launch(intent)
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val position = travelAdapter.longClickedPosition
        val selectedItem = travelAdapter.getItemAt(position) ?: return super.onContextItemSelected(item)

        return when (item.itemId) {
            R.id.context_edit -> {
                val intent = Intent(requireContext(), AddEditActivity::class.java).apply {
                    putExtra("RECORD_NO", selectedItem.no)
                    putExtra("RECORD_PLACE", selectedItem.place)
                    putExtra("RECORD_DATE", selectedItem.visitDate)
                    putExtra("RECORD_MEMO", selectedItem.memo)
                    putExtra("RECORD_PHOTO", selectedItem.photoUri)
                    putExtra("RECORD_GROUP", selectedItem.groupName)
                    selectedItem.latitude?.let { putExtra("RECORD_LAT", it) }
                    selectedItem.longitude?.let { putExtra("RECORD_LNG", it) }
                }
                addEditLauncher.launch(intent)
                true
            }
            R.id.context_group -> {
                // [신규] 그룹으로 묶기 선택 시 선택 모드 시작
                travelAdapter.startSelectionMode(position)
                true
            }
            R.id.context_delete -> {
                AlertDialog.Builder(requireContext()).apply {
                    setTitle("기록 삭제")
                    setMessage("${selectedItem.place} 여행 기록을 정말 삭제하시겠습니까?")
                    setPositiveButton("삭제") { _, _ ->
                        dbHelper.deleteRecord(selectedItem.no)
                        Toast.makeText(requireContext(), "기록이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                        loadDataFromDB()
                    }
                    setNegativeButton("취소", null)
                    show()
                }
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    private fun updateSelectionUI(count: Int) {
        if (count > 0) {
            binding.layoutSelectionBar.visibility = View.VISIBLE
            binding.tvSelectionCount.text = "${count}개 선택됨"
            binding.fabAdd.hide()
        } else {
            binding.layoutSelectionBar.visibility = View.GONE
            binding.fabAdd.show()
        }
    }

    private fun showGroupRenameDialog() {
        val selectedNos = travelAdapter.getSelectedNos()
        if (selectedNos.isEmpty()) return

        val editText = EditText(requireContext()).apply {
            hint = "예: 제주도 가족 여행"
        }

        AlertDialog.Builder(requireContext()).apply {
            setTitle("선택한 기록 그룹 묶기")
            setMessage("새로운 그룹 이름을 입력하세요.")
            setView(editText)
            setPositiveButton("확인") { _, _ ->
                val groupName = editText.text.toString().trim()
                if (groupName.isNotEmpty()) {
                    dbHelper.updateGroupBatch(selectedNos, groupName)
                    Toast.makeText(requireContext(), "그룹으로 묶었습니다.", Toast.LENGTH_SHORT).show()
                    travelAdapter.clearSelection()
                    loadDataFromDB()
                } else {
                    Toast.makeText(requireContext(), "그룹명을 입력해주세요.", Toast.LENGTH_SHORT).show()
                }
            }
            setNegativeButton("취소", null)
            show()
        }
    }

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
                val groupName = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_GROUPNAME))
                
                val latIndex = cursor.getColumnIndex(DBHelper.COLUMN_LATITUDE)
                val lngIndex = cursor.getColumnIndex(DBHelper.COLUMN_LONGITUDE)
                val lat = if (latIndex != -1 && !cursor.isNull(latIndex)) cursor.getDouble(latIndex) else null
                val lng = if (lngIndex != -1 && !cursor.isNull(lngIndex)) cursor.getDouble(lngIndex) else null

                itemList.add(TravelItem(no, place, visitDate, memo, photoUri, lat, lng, groupName))
            } while (cursor.moveToNext())
        }
        cursor.close()

        if (itemList.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.rvTravelList.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.rvTravelList.visibility = View.VISIBLE
        }

        travelAdapter.updateData(itemList)
    }

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
                val groupName = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_GROUPNAME))

                val latIndex = cursor.getColumnIndex(DBHelper.COLUMN_LATITUDE)
                val lngIndex = cursor.getColumnIndex(DBHelper.COLUMN_LONGITUDE)
                val lat = if (latIndex != -1 && !cursor.isNull(latIndex)) cursor.getDouble(latIndex) else null
                val lng = if (lngIndex != -1 && !cursor.isNull(lngIndex)) cursor.getDouble(lngIndex) else null

                itemList.add(TravelItem(no, place, visitDate, memo, photoUri, lat, lng, groupName))
            } while (cursor.moveToNext())
        }
        cursor.close()

        if (!descending) {
            itemList.sortBy { it.no }
        } else {
            itemList.sortByDescending { it.no }
        }

        travelAdapter.updateData(itemList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}