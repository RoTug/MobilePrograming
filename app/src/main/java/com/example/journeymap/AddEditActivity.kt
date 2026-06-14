package com.example.journeymap

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import com.example.journeymap.databinding.ActivityAddEditBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class AddEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditBinding
    private lateinit var dbHelper: DBHelper
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var selectedPhotoUri: String? = null
    private var selectedDate: String = ""
    private var isEditMode = false
    private var recordNo = -1
    
    private var photoLat: Double? = null
    private var photoLng: Double? = null
    private var existingGroup: String? = "기타"

    private var currentPhotoPath: String? = null

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedPhotoUri = it.toString()
            binding.ivPhoto.setImageURI(it)
            extractGpsFromUri(it)
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            currentPhotoPath?.let { path ->
                val uri = Uri.fromFile(File(path))
                selectedPhotoUri = uri.toString()
                binding.ivPhoto.setImageURI(uri)
                
                if (!extractGpsFromUri(uri)) {
                    fetchCurrentLocation()
                }
            }
        }
    }

    private val locationPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            photoLat = data?.getDoubleExtra("PICKED_LAT", 0.0)
            photoLng = data?.getDoubleExtra("PICKED_LNG", 0.0)
            Toast.makeText(this, "선택한 위치가 적용되었습니다.", Toast.LENGTH_SHORT).show()
            checkLocationStatus()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DBHelper(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        recordNo = intent.getIntExtra("RECORD_NO", -1)
        if (recordNo != -1) {
            isEditMode = true
            title = "여행 기록 수정"
            loadExistingData()
        } else {
            title = "새 여행 기록 추가"
        }

        binding.btnDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                selectedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                binding.btnDate.text = selectedDate
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.btnGallery.setOnClickListener { galleryLauncher.launch("image/*") }
        binding.btnCamera.setOnClickListener { dispatchTakePictureIntent() }
        
        binding.btnGetLocation.setOnClickListener {
            val intent = Intent(this, LocationPickerActivity::class.java)
            locationPickerLauncher.launch(intent)
        }

        binding.btnSave.setOnClickListener {
            saveRecord()
        }
        
        checkLocationStatus()
    }

    private fun checkLocationStatus() {
        if (photoLat != null && photoLng != null) {
            binding.tvLocationStatus.text = "위치 정보가 연결되었습니다."
            binding.tvLocationStatus.setTextColor(Color.parseColor("#4CAF50"))
        } else {
            binding.tvLocationStatus.text = "위치 정보 없음 (지도로 표시 불가)"
            binding.tvLocationStatus.setTextColor(Color.RED)
        }
    }

    private fun dispatchTakePictureIntent() {
        try {
            val photoFile: File = createImageFile()
            val photoURI: Uri = FileProvider.getUriForFile(
                this,
                "com.example.journeymap.fileprovider",
                photoFile
            )
            currentPhotoPath = photoFile.absolutePath
            cameraLauncher.launch(photoURI)
        } catch (ex: IOException) {
            Toast.makeText(this, "사진 파일을 생성할 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    private fun extractGpsFromUri(uri: Uri): Boolean {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                val latLong = exif.latLong
                if (latLong != null) {
                    photoLat = latLong[0]
                    photoLng = latLong[1]
                    Toast.makeText(this, "사진에서 위치 정보를 추출했습니다.", Toast.LENGTH_SHORT).show()
                    checkLocationStatus()
                    true
                } else {
                    false
                }
            } ?: false
        } catch (e: IOException) {
            false
        }
    }

    private fun fetchCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    photoLat = location.latitude
                    photoLng = location.longitude
                    checkLocationStatus()
                }
            }
        }
    }

    private fun loadExistingData() {
        val place = intent.getStringExtra("RECORD_PLACE") ?: ""
        val date = intent.getStringExtra("RECORD_DATE") ?: ""
        val memo = intent.getStringExtra("RECORD_MEMO") ?: ""
        existingGroup = intent.getStringExtra("RECORD_GROUP") ?: "기타"
        selectedPhotoUri = intent.getStringExtra("RECORD_PHOTO")
        
        photoLat = if (intent.hasExtra("RECORD_LAT")) intent.getDoubleExtra("RECORD_LAT", 0.0) else null
        photoLng = if (intent.hasExtra("RECORD_LNG")) intent.getDoubleExtra("RECORD_LNG", 0.0) else null

        binding.etPlace.setText(place)
        binding.btnDate.text = date
        selectedDate = date
        binding.etMemo.setText(memo)

        if (!selectedPhotoUri.isNullOrEmpty()) {
            binding.ivPhoto.setImageURI(Uri.parse(selectedPhotoUri))
        }
        checkLocationStatus()
    }

    private fun saveRecord() {
        val place = binding.etPlace.text.toString().trim()
        val memo = binding.etMemo.text.toString().trim()

        if (place.isEmpty()) {
            Toast.makeText(this, "여행지명을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedDate.isEmpty()) {
            Toast.makeText(this, "방문 날짜를 선택해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            if (photoLat == null || photoLng == null) {
                val coords = withContext(Dispatchers.IO) {
                    try {
                        val geocoder = Geocoder(this@AddEditActivity, Locale.KOREA)
                        val addresses = geocoder.getFromLocationName(place, 1)
                        if (!addresses.isNullOrEmpty()) {
                            Pair(addresses[0].latitude, addresses[0].longitude)
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                }
                coords?.let {
                    photoLat = it.first
                    photoLng = it.second
                }
            }

            if (isEditMode) {
                dbHelper.updateRecord(recordNo, place, selectedDate, memo, selectedPhotoUri, photoLat, photoLng, existingGroup)
                Toast.makeText(this@AddEditActivity, "기록이 수정되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                dbHelper.insertRecord(place, selectedDate, memo, selectedPhotoUri, photoLat, photoLng, "기타")
                Toast.makeText(this@AddEditActivity, "새 기록이 저장되었습니다.", Toast.LENGTH_SHORT).show()
            }
            
            finish()
        }
    }
}