package com.example.Eventadder

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper


class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "events.db"
        const val DATABASE_VERSION = 1
        const val TABLE_NAME = "events"
        const val COLUMN_ID = "id"
        const val COLUMN_CATEGORY = "category"
        const val COLUMN_NAME = "name"
        const val COLUMN_IMAGE_URI = "image_uri"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = "CREATE TABLE $TABLE_NAME (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COLUMN_CATEGORY TEXT," +
                "$COLUMN_NAME TEXT," +
                "$COLUMN_IMAGE_URI TEXT)"
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertEvent(category: String, name: String, imageUri: String?) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_CATEGORY, category)
            put(COLUMN_NAME, name)
            put(COLUMN_IMAGE_URI, imageUri)
        }
        db.insert(TABLE_NAME, null, values)
        db.close()
    }

    @SuppressLint("Range")
    fun getAllEvents(): MutableList<EventItem> {
        val events = mutableListOf<EventItem>()
        val db = readableDatabase
        val query = "SELECT * FROM $TABLE_NAME"
        val cursor = db.rawQuery(query, null)

        val columnIndexCategory = cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)
        val columnIndexName = cursor.getColumnIndexOrThrow(COLUMN_NAME)
        val columnIndexImageUri = cursor.getColumnIndexOrThrow(COLUMN_IMAGE_URI)

        while (cursor.moveToNext()) {
            val category = cursor.getString(columnIndexCategory)
            val name = cursor.getString(columnIndexName)
            val imageUri = cursor.getString(columnIndexImageUri)
            events.add(EventItem(category, name, imageUri))
        }

        cursor.close()
        db.close()
        return events
    }
}


