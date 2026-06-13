package com.example.journeymap

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.journeymap.databinding.ActivityAddEditBinding
import java.io.ByteArrayOutputStream
import java.util.Calendar

class AddEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditBinding
    private lateinit var dbHelper: DBHelper

    private var selectedPhotoUri: String? = null
    private var selectedDate: String = ""
    private var isEditMode = false
    private var recordNo = -1

    // 갤러리 인텐트 결과 처리기
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            // 이미지 선택 시 URI 저장 및 화면 표시
            selectedPhotoUri = it.toString()
            binding.ivPhoto.setImageURI(it)
        }
    }

    // 카메라 촬영 인텐트 결과 처리기
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        bitmap?.let {
            // 촬영한 비트맵을 임시 URI 파일 경로로 변환하여 저장
            val path = MediaStore.Images.Media.insertImage(contentResolver, it, "Travel_${System.currentTimeMillis()}", null)
            selectedPhotoUri = path
            binding.ivPhoto.setImageBitmap(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DBHelper(this)

        // 1. 수정 모드 판별 (Intent로 데이터가 넘어왔는지 확인)
        recordNo = intent.getIntExtra("RECORD_NO", -1)
        if (recordNo != -1) {
            isEditMode = true
            title = "여행 기록 수정"
            loadExistingData()
        } else {
            title = "새 여행 기록 추가"
        }

        // 2. 날짜 선택 버튼 클릭 (DatePickerDialog)
        binding.btnDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                // 날짜 포맷팅 (예: 2026-06-13)
                selectedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                binding.btnDate.text = selectedDate
            }, year, month, day).show()
        }

        // 3. 사진 가져오기 버튼 리스너 연동 (인텐트 활용)
        binding.btnGallery.setOnClickListener { galleryLauncher.launch("image/*") }
        binding.btnCamera.setOnClickListener { cameraLauncher.launch(null) }

        // 4. 저장 버튼 클릭 처리 (DB 연동)
        binding.btnSave.setOnClickListener {
            saveRecord()
        }
    }

    // 수정 모드일 때 기존 데이터를 화면에 로드하는 함수
    private fun loadExistingData() {
        val place = intent.getStringExtra("RECORD_PLACE") ?: ""
        val date = intent.getStringExtra("RECORD_DATE") ?: ""
        val memo = intent.getStringExtra("RECORD_MEMO") ?: ""
        selectedPhotoUri = intent.getStringExtra("RECORD_PHOTO")

        binding.etPlace.setText(place)
        binding.btnDate.text = date
        selectedDate = date
        binding.etMemo.setText(memo)

        if (!selectedPhotoUri.isNullOrEmpty()) {
            binding.ivPhoto.setImageURI(Uri.parse(selectedPhotoUri))
        }
    }

    // 데이터를 검증하고 SQLite DB에 저장/수정하는 함수
    private fun saveRecord() {
        val place = binding.etPlace.text.toString().trim()
        val memo = binding.etMemo.text.toString().trim()

        // 예외 처리 (교수님 강조 사항: 빈 칸 방지 및 안전성)
        if (place.isEmpty()) {
            Toast.makeText(this, "여행지명을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedDate.isEmpty()) {
            Toast.makeText(this, "방문 날짜를 선택해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        if (isEditMode) {
            // [U] Update 수행
            val rows = dbHelper.updateRecord(recordNo, place, selectedDate, memo, selectedPhotoUri)
            if (rows > 0) {
                Toast.makeText(this, "기록이 수정되었습니다.", Toast.LENGTH_SHORT).show()
                finish() // 화면 종료
            }
        } else {
            // [C] Create 수행
            val rowId = dbHelper.insertRecord(place, selectedDate, memo, selectedPhotoUri)
            if (rowId != -1L) {
                Toast.makeText(this, "새 기록이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                finish() // 화면 종료
            }
        }
    }
}