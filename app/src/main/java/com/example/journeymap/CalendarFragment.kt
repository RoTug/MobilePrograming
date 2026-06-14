package com.example.journeymap

import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.journeymap.databinding.FragmentCalendarBinding
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.spans.DotSpan
import java.text.SimpleDateFormat
import java.util.*

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private lateinit var dbHelper: DBHelper
    private lateinit var travelAdapter: TravelAdapter
    private var selectedDateString: String = ""

    private val addEditLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        loadRecordsForDate(selectedDateString)
        refreshCalendarIndicators()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DBHelper(requireContext())

        // 오늘 날짜 기본 설정
        val today = CalendarDay.today()
        binding.calendarView.setSelectedDate(today)
        updateDateLabel(today.year, today.month, today.day)

        setupRecyclerView()

        // 날짜 선택 이벤트
        binding.calendarView.setOnDateChangedListener { _, day, _ ->
            updateDateLabel(day.year, day.month, day.day)
            loadRecordsForDate(selectedDateString)
        }

        refreshCalendarIndicators()
        loadRecordsForDate(selectedDateString)
    }

    private fun updateDateLabel(year: Int, month: Int, day: Int) {
        selectedDateString = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month, day)
        binding.tvSelectedDate.text = "$selectedDateString 의 여행"
    }

    private fun setupRecyclerView() {
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
            onSelectionChanged = { }
        )
        binding.rvCalendarList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCalendarList.adapter = travelAdapter
    }

    private fun refreshCalendarIndicators() {
        val daysWithPhotos = mutableSetOf<CalendarDay>()
        val db = dbHelper.readableDatabase
        
        // 사진이 있는 모든 기록의 날짜 조회
        val cursor = db.rawQuery(
            "SELECT ${DBHelper.COLUMN_VISIT_DATE} FROM ${DBHelper.TABLE_NAME} WHERE ${DBHelper.COLUMN_PHOTO_URI} IS NOT NULL AND ${DBHelper.COLUMN_PHOTO_URI} != ''",
            null
        )

        if (cursor.moveToFirst()) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            do {
                val dateStr = cursor.getString(0)
                try {
                    val date = sdf.parse(dateStr)
                    date?.let {
                        val cal = Calendar.getInstance()
                        cal.time = it
                        daysWithPhotos.add(CalendarDay.from(
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH) + 1,
                            cal.get(Calendar.DAY_OF_MONTH)
                        ))
                    }
                } catch (e: Exception) { e.printStackTrace() }
            } while (cursor.moveToNext())
        }
        cursor.close()

        // 달력에 도트 표시 적용
        binding.calendarView.removeDecorators()
        binding.calendarView.addDecorator(EventDecorator(Color.RED, daysWithPhotos))
    }

    private fun loadRecordsForDate(date: String) {
        val itemList = mutableListOf<TravelItem>()
        val db = dbHelper.readableDatabase
        
        val cursor: Cursor = db.rawQuery(
            "SELECT * FROM ${DBHelper.TABLE_NAME} WHERE ${DBHelper.COLUMN_VISIT_DATE} = ? ORDER BY ${DBHelper.COLUMN_NO} DESC",
            arrayOf(date)
        )

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
            binding.tvNoRecord.visibility = View.VISIBLE
            binding.rvCalendarList.visibility = View.GONE
        } else {
            binding.tvNoRecord.visibility = View.GONE
            binding.rvCalendarList.visibility = View.VISIBLE
        }

        travelAdapter.updateData(itemList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // 도트 표시를 위한 데코레이터 클래스
    inner class EventDecorator(private val color: Int, private val dates: Collection<CalendarDay>) : DayViewDecorator {
        override fun shouldDecorate(day: CalendarDay): Boolean {
            return dates.contains(day)
        }
        override fun decorate(view: DayViewFacade) {
            view.addSpan(DotSpan(8f, color))
        }
    }
}