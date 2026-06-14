package com.example.journeymap

import android.content.Intent
import android.database.Cursor
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

        val mapFragment = childFragmentManager.findFragmentById(R.id.google_map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        googleMap?.setOnInfoWindowClickListener { marker ->
            val item = marker.tag as? TravelItem
            item?.let {
                val intent = Intent(requireContext(), AddEditActivity::class.java).apply {
                    putExtra("RECORD_NO", it.no)
                    putExtra("RECORD_PLACE", it.place)
                    putExtra("RECORD_DATE", it.visitDate)
                    putExtra("RECORD_MEMO", it.memo)
                    putExtra("RECORD_PHOTO", it.photoUri)
                    putExtra("RECORD_GROUP", it.groupName) // 그룹 정보 추가
                    it.latitude?.let { lat -> putExtra("RECORD_LAT", lat) }
                    it.longitude?.let { lng -> putExtra("RECORD_LNG", lng) }
                }
                startActivity(intent)
            }
        }

        val defaultCenter = LatLng(36.7692, 126.9348)
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultCenter, 7f))

        displayAllRecordsOnMap()
    }

    private fun displayAllRecordsOnMap() {
        val map = googleMap ?: return
        map.clear()

        lifecycleScope.launch(Dispatchers.IO) {
            val cursor: Cursor = dbHelper.getAllRecords()
            val geocoder = Geocoder(requireContext(), Locale.KOREA)
            val markersToDraw = mutableListOf<Pair<LatLng, TravelItem>>()

            if (cursor.moveToFirst()) {
                do {
                    val no = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_NO))
                    val placeName = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_PLACE))
                    val visitDate = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_VISIT_DATE))
                    val memo = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_MEMO))
                    val photoUri = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_PHOTO_URI))
                    val groupName = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_GROUPNAME))
                    
                    val latIndex = cursor.getColumnIndex(DBHelper.COLUMN_LATITUDE)
                    val lngIndex = cursor.getColumnIndex(DBHelper.COLUMN_LONGITUDE)
                    
                    var lat: Double? = null
                    var lng: Double? = null
                    
                    if (latIndex != -1 && lngIndex != -1 && !cursor.isNull(latIndex) && !cursor.isNull(lngIndex)) {
                        lat = cursor.getDouble(latIndex)
                        lng = cursor.getDouble(lngIndex)
                    }

                    val travelItem = TravelItem(no, placeName, visitDate, memo, photoUri, lat, lng, groupName)
                    
                    if (lat != null && lng != null) {
                        markersToDraw.add(LatLng(lat, lng) to travelItem)
                    } else {
                        try {
                            val addresses = geocoder.getFromLocationName(placeName, 1)
                            if (!addresses.isNullOrEmpty()) {
                                val address = addresses[0]
                                markersToDraw.add(LatLng(address.latitude, address.longitude) to travelItem)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } while (cursor.moveToNext())
            }
            cursor.close()

            withContext(Dispatchers.Main) {
                if (markersToDraw.isNotEmpty()) {
                    val builder = LatLngBounds.Builder()
                    for (pair in markersToDraw) {
                        val marker = map.addMarker(
                            MarkerOptions()
                                .position(pair.first)
                                .title(pair.second.place)
                                .snippet("${pair.second.groupName} | ${pair.second.visitDate}")
                        )
                        marker?.tag = pair.second
                        builder.include(pair.first)
                    }
                    
                    try {
                        val bounds = builder.build()
                        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150))
                    } catch (e: Exception) {
                        Log.e("MapFragment", "Error adjusting camera bounds", e)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (googleMap != null) {
            displayAllRecordsOnMap()
        }
    }
}