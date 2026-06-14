package com.example.journeymap

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.journeymap.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dbHelper: DBHelper

    private var isSortDesc = true

    // [추가] 위치 권한 요청 처리를 위한 런처
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            Toast.makeText(this, "위치 권한이 승인되었습니다.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "위치 권한이 거부되었습니다. 지도 기능이 제한될 수 있습니다.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DBHelper(this)

        // 앱 시작 시 위치 권한 체크 및 요청
        checkPermissions()

        if (savedInstanceState == null) {
            loadFragment(ListFragment(), false)
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_list -> {
                    loadFragment(ListFragment(), true)
                    true
                }
                R.id.nav_map -> {
                    loadFragment(MapFragment(), true)
                    true
                }
                R.id.nav_info -> {
                    loadFragment(InfoFragment(), true)
                    true
                }
                else -> false
            }
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            locationPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

        return when (item.itemId) {
            R.id.action_sort -> {
                if (currentFragment is ListFragment) {
                    isSortDesc = !isSortDesc
                    currentFragment.toggleSortOrder(isSortDesc)
                    val msg = if (isSortDesc) "최신순으로 정렬되었습니다." else "오래된순으로 정렬되었습니다."
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "여행 목록 탭에서만 정렬이 가능합니다.", Toast.LENGTH_SHORT).show()
                }
                true
            }
            R.id.action_delete_all -> {
                AlertDialog.Builder(this).apply {
                    setTitle("전체 기록 삭제")
                    setMessage("정말로 모든 여행 기록을 삭제하시겠습니까?\n삭제된 데이터는 복구할 수 없습니다.")
                    setPositiveButton("전체 삭제") { _, _ ->
                        dbHelper.deleteAllRecords()
                        Toast.makeText(this@MainActivity, "모든 기록이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                        if (currentFragment is ListFragment) {
                            currentFragment.loadDataFromDB()
                        } else if (currentFragment is MapFragment) {
                            currentFragment.onResume()
                        }
                    }
                    setNegativeButton("취소", null)
                    show()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadFragment(fragment: Fragment, addToBackStack: Boolean) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        if (addToBackStack) {
            transaction.addToBackStack(null)
        }
        transaction.commit()
    }
}