package com.example.journeymap

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.example.journeymap.databinding.ActivityLocationPickerBinding

class LocationPickerActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityLocationPickerBinding
    private var googleMap: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocationPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        title = "위치 선택"

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_picker) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.btnConfirmLocation.setOnClickListener {
            googleMap?.let { map ->
                val center = map.cameraPosition.target
                val resultIntent = Intent().apply {
                    putExtra("PICKED_LAT", center.latitude)
                    putExtra("PICKED_LNG", center.longitude)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
        // 기본 위치 (서울역 부근)
        val startPoint = LatLng(37.5559, 126.9723)
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(startPoint, 15f))
        
        // 내 위치 버튼 활성화 (권한 있을 경우)
        try {
            googleMap?.isMyLocationEnabled = true
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}