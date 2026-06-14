package com.example.journeymap

import android.database.Cursor
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.io.IOException
import java.util.Locale

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var dbHelper: DBHelper
    private var googleMap: GoogleMap? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dbHelper = DBHelper(requireContext())

        // 내장된 SupportMapFragment 초기화
        val mapFragment = childFragmentManager.findFragmentById(R.id.google_map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // 기본 중심점: 순천향대학교
        val defaultCenter = LatLng(36.7692, 126.9348)
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultCenter, 7f)) // 전국이 다 보이도록 줌레벨 조정

        // DB에서 모든 기록 가져와서 지도에 마커 표시하기
        displayAllRecordsOnMap()
    }

    private fun displayAllRecordsOnMap() {
        googleMap?.clear() // 기존 마커 청소
        val cursor: Cursor = dbHelper.getAllRecords()
        val geocoder = Geocoder(requireContext(), Locale.KOREA)

        if (cursor.moveToFirst()) {
            do {
                val placeName = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_PLACE))
                val visitDate = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_VISIT_DATE))
                
                // DB에서 위도, 경도 추출 (null일 수 있음)
                val latIndex = cursor.getColumnIndex(DBHelper.COLUMN_LATITUDE)
                val lngIndex = cursor.getColumnIndex(DBHelper.COLUMN_LONGITUDE)
                
                var latLng: LatLng? = null
                
                if (latIndex != -1 && lngIndex != -1 && !cursor.isNull(latIndex) && !cursor.isNull(lngIndex)) {
                    latLng = LatLng(cursor.getDouble(latIndex), cursor.getDouble(lngIndex))
                }

                if (latLng != null) {
                    // 1. 사진에서 추출한 GPS 좌표가 있으면 우선 사용
                    addMarkerToMap(latLng, placeName, visitDate)
                } else {
                    // 2. 없으면 지오코딩을 통해 여행지 이름의 좌표 획득
                    try {
                        val addresses = geocoder.getFromLocationName(placeName, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val address = addresses[0]
                            latLng = LatLng(address.latitude, address.longitude)
                            addMarkerToMap(latLng, placeName, visitDate)
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            } while (cursor.moveToNext())
        }
        cursor.close()
    }

    private fun addMarkerToMap(latLng: LatLng, title: String, date: String) {
        googleMap?.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(title)
                .snippet("방문일: $date")
        )
    }

    // 다른 탭 갔다가 다시 지도로 돌아왔을 때 최신 DB 데이터 반영하도록 리프레시
    override fun onResume() {
        super.onResume()
        if (googleMap != null) {
            displayAllRecordsOnMap()
        }
    }
}