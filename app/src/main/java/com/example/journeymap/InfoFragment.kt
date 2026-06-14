package com.example.journeymap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.journeymap.databinding.FragmentInfoBinding

class InfoFragment : Fragment() {

    private var _binding: FragmentInfoBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbHelper: DBHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dbHelper = DBHelper(requireContext())
        
        updateStatistics()
    }

    private fun updateStatistics() {
        val db = dbHelper.readableDatabase
        
        // 1. 총 기록 수 조회
        val recordCursor = db.rawQuery("SELECT COUNT(*) FROM ${DBHelper.TABLE_NAME}", null)
        var totalRecords = 0
        if (recordCursor.moveToFirst()) {
            totalRecords = recordCursor.getInt(0)
        }
        recordCursor.close()

        // 2. 고유 그룹(폴더) 수 조회 ('기타' 제외)
        val groupCursor = db.rawQuery(
            "SELECT COUNT(DISTINCT ${DBHelper.COLUMN_GROUPNAME}) FROM ${DBHelper.TABLE_NAME} WHERE ${DBHelper.COLUMN_GROUPNAME} != '기타'",
            null
        )
        var totalGroups = 0
        if (groupCursor.moveToFirst()) {
            totalGroups = groupCursor.getInt(0)
        }
        groupCursor.close()

        // UI 반영
        binding.tvTotalRecords.text = totalRecords.toString()
        binding.tvTotalGroups.text = totalGroups.toString()
    }

    override fun onResume() {
        super.onResume()
        // 다른 탭에서 추가/삭제 후 돌아왔을 때 통계 갱신
        updateStatistics()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}