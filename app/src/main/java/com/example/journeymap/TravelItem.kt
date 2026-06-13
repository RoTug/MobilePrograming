package com.example.journeymap

data class TravelItem(
    val no: Int,           // 기록 순번 (Primary Key)
    val place: String,     // 여행지명
    val visitDate: String, // 방문 날짜
    val memo: String?,     // 여행 메모
    val photoUri: String?  // 선택한 사진의 경로
)