package com.example.journeymap

import android.app.AlertDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.journeymap.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dbHelper: DBHelper

    // 현재 정렬 상태를 저장하는 변수 (true: 최신순, false: 오래된순)
    private var isSortDesc = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DBHelper(this)

        if (savedInstanceState == null) {
            loadFragment(ListFragment(), false)
        }

        // 하단 네비게이션 바 클릭 이벤트 처리 (MapFragment 분기 추가 완료)
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_list -> {
                    loadFragment(ListFragment(), true)
                    true
                }
                R.id.nav_map -> { // 👈 새로 분리한 여행 지도 탭 연동
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

    // [필수 요구사항] 1. 상단 옵션 메뉴 생성
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    // [필수 요구사항] 2. 옵션 메뉴 아이템 클릭 이벤트 처리
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // 현재 화면에 떠있는 프래그먼트가 어떤 것인지 확인
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

        return when (item.itemId) {
            R.id.action_sort -> {
                if (currentFragment is ListFragment) {
                    // 정렬 상태 반전
                    isSortDesc = !isSortDesc

                    // ListFragment 내부의 리스트 정렬 변경 함수 호출
                    currentFragment.toggleSortOrder(isSortDesc)

                    val msg = if (isSortDesc) "최신순으로 정렬되었습니다." else "오래된순으로 정렬되었습니다."
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "여행 목록 탭에서만 정렬이 가능합니다.", Toast.LENGTH_SHORT).show()
                }
                true
            }
            R.id.action_delete_all -> {
                // 안전장치: 전체 삭제 전 AlertDialog로 한 번 더 확인 (교수님 강조 예외처리)
                AlertDialog.Builder(this).apply {
                    setTitle("전체 기록 삭제")
                    setMessage("정말로 모든 여행 기록을 삭제하시겠습니까?\n삭제된 데이터는 복구할 수 없습니다.")
                    setPositiveButton("전체 삭제") { _, _ ->
                        dbHelper.deleteAllRecords()
                        Toast.makeText(this@MainActivity, "모든 기록이 삭제되었습니다.", Toast.LENGTH_SHORT).show()

                        // 현재 화면에 따라 UI 동기화 리프레시 처리
                        if (currentFragment is ListFragment) {
                            currentFragment.loadDataFromDB()
                        } else if (currentFragment is MapFragment) {
                            // 지도 화면에 떠있을 때도 전체 삭제 시 마커를 비워주도록 리로드 호출
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