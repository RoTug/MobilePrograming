package com.example.journeymap

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "TravelRecord.db"
        private const val DATABASE_VERSION = 1

        // 테이블 및 컬럼명 정의
        const val TABLE_NAME = "travel_records"
        const val COLUMN_NO = "record_no"
        const val COLUMN_PLACE = "place"
        const val COLUMN_VISIT_DATE = "visit_date"
        const val COLUMN_MEMO = "memo"
        const val COLUMN_PHOTO_URI = "photo_uri"
    }

    // 1. 테이블 생성 (앱 설치 후 최초 1회 실행)
    override fun onCreate(db: SQLiteDatabase?) {
        val createTableQuery = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_NO INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_PLACE TEXT NOT NULL,
                $COLUMN_VISIT_DATE TEXT NOT NULL,
                $COLUMN_MEMO TEXT,
                $COLUMN_PHOTO_URI TEXT
            )
        """.trimIndent()
        db?.execSQL(createTableQuery)
    }

    // 2. 데이터베이스 버전 업그레이드 시 처리
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    // ==========================================
    // CRUD 기능 구현 (필수 요구사항)
    // ==========================================

    // [C] Create - 여행 기록 추가
    fun insertRecord(place: String, visitDate: String, memo: String, photoUri: String?): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_PLACE, place)
            put(COLUMN_VISIT_DATE, visitDate)
            put(COLUMN_MEMO, memo)
            put(COLUMN_PHOTO_URI, photoUri)
        }
        val result = db.insert(TABLE_NAME, null, values)
        db.close() // 사용 후 DB 닫기
        return result // 성공 시 row ID, 실패 시 -1 반환
    }

    // [R] Read - 전체 여행 기록 조회
    fun getAllRecords(): Cursor {
        val db = this.readableDatabase
        // 최신 방문 날짜순 또는 번호 역순으로 정렬해서 가져옵니다.
        return db.rawQuery("SELECT * FROM $TABLE_NAME ORDER BY $COLUMN_NO DESC", null)
    }

    // [U] Update - 특정 여행 기록 수정
    fun updateRecord(no: Int, place: String, visitDate: String, memo: String, photoUri: String?): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_PLACE, place)
            put(COLUMN_VISIT_DATE, visitDate)
            put(COLUMN_MEMO, memo)
            put(COLUMN_PHOTO_URI, photoUri)
        }
        // no가 일치하는 행을 찾아 업데이트
        val result = db.update(TABLE_NAME, values, "$COLUMN_NO = ?", arrayOf(no.toString()))
        db.close()
        return result // 영향을 받은 행의 개수 반환
    }

    // [D] Delete - 특정 여행 기록 삭제
    fun deleteRecord(no: Int): Int {
        val db = this.writableDatabase
        val result = db.delete(TABLE_NAME, "$COLUMN_NO = ?", arrayOf(no.toString()))
        db.close()
        return result // 삭제된 행의 개수 반환
    }

    // [D] Delete - 전체 여행 기록 삭제 (옵션 메뉴용)
    fun deleteAllRecords() {
        val db = this.writableDatabase
        db.delete(TABLE_NAME, null, null)
        db.close()
    }
}