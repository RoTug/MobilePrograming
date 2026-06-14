package com.example.journeymap

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "TravelRecord.db"
        private const val DATABASE_VERSION = 3

        // 테이블 및 컬럼명 정의
        const val TABLE_NAME = "travel_records"
        const val COLUMN_NO = "record_no"
        const val COLUMN_PLACE = "place"
        const val COLUMN_VISIT_DATE = "visit_date"
        const val COLUMN_MEMO = "memo"
        const val COLUMN_PHOTO_URI = "photo_uri"
        const val COLUMN_LATITUDE = "latitude"
        const val COLUMN_LONGITUDE = "longitude"
        const val COLUMN_GROUPNAME = "group_name"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTableQuery = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_NO INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_PLACE TEXT NOT NULL,
                $COLUMN_VISIT_DATE TEXT NOT NULL,
                $COLUMN_MEMO TEXT,
                $COLUMN_PHOTO_URI TEXT,
                $COLUMN_LATITUDE REAL,
                $COLUMN_LONGITUDE REAL,
                $COLUMN_GROUPNAME TEXT DEFAULT '기타'
            )
        """.trimIndent()
        db?.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db?.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_LATITUDE REAL")
            db?.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_LONGITUDE REAL")
        }
        if (oldVersion < 3) {
            db?.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_GROUPNAME TEXT DEFAULT '기타'")
        }
        // 버전이 많이 차이날 경우를 대비해 안전하게 처리
        if (oldVersion >= newVersion) {
            db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
            onCreate(db)
        }
    }

    // [C] Create - 여행 기록 추가
    fun insertRecord(place: String, visitDate: String, memo: String, photoUri: String?, lat: Double?, lng: Double?, group: String?): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_PLACE, place)
            put(COLUMN_VISIT_DATE, visitDate)
            put(COLUMN_MEMO, memo)
            put(COLUMN_PHOTO_URI, photoUri)
            put(COLUMN_LATITUDE, lat)
            put(COLUMN_LONGITUDE, lng)
            put(COLUMN_GROUPNAME, group ?: "기타")
        }
        val result = db.insert(TABLE_NAME, null, values)
        db.close()
        return result
    }

    // [R] Read - 전체 여행 기록 조회
    fun getAllRecords(): Cursor {
        val db = this.readableDatabase
        // 그룹별로 먼저 정렬하고 그 안에서 날짜순으로 정렬
        return db.rawQuery("SELECT * FROM $TABLE_NAME ORDER BY $COLUMN_GROUPNAME ASC, $COLUMN_VISIT_DATE DESC", null)
    }

    // [U] Update - 특정 여행 기록 수정
    fun updateRecord(no: Int, place: String, visitDate: String, memo: String, photoUri: String?, lat: Double?, lng: Double?, group: String?): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_PLACE, place)
            put(COLUMN_VISIT_DATE, visitDate)
            put(COLUMN_MEMO, memo)
            put(COLUMN_PHOTO_URI, photoUri)
            put(COLUMN_LATITUDE, lat)
            put(COLUMN_LONGITUDE, lng)
            put(COLUMN_GROUPNAME, group ?: "기타")
        }
        val result = db.update(TABLE_NAME, values, "$COLUMN_NO = ?", arrayOf(no.toString()))
        db.close()
        return result
    }

    fun deleteRecord(no: Int): Int {
        val db = this.writableDatabase
        val result = db.delete(TABLE_NAME, "$COLUMN_NO = ?", arrayOf(no.toString()))
        db.close()
        return result
    }

    fun deleteAllRecords() {
        val db = this.writableDatabase
        db.delete(TABLE_NAME, null, null)
        db.close()
    }

    // [U] 여러 기록을 하나의 그룹으로 묶기
    fun updateGroupBatch(noList: List<Int>, groupName: String) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_GROUPNAME, groupName)
        }
        
        db.beginTransaction()
        try {
            for (no in noList) {
                db.update(TABLE_NAME, values, "$COLUMN_NO = ?", arrayOf(no.toString()))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            db.close()
        }
    }
}