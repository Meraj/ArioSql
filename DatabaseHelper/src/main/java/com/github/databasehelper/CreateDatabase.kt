package com.github.databasehelper

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper


class CreateDatabase(private val context: Context){
    private lateinit var TABLE_NAME :String
    private lateinit var  DATABASE_NAME:String
    private  var DATABASE_VERSION:Int = 1
    private var COLUMNS:String = ""
    private lateinit var db: SQLiteDatabase
    fun database(Name: String){
        DATABASE_NAME = Name
    }
    fun version(Version: Int){
        DATABASE_VERSION = Version
    }
    fun table(Name: String){
        TABLE_NAME = Name
    }
    fun column(Name: String, DInfo: String){
        COLUMNS += "$Name $DInfo,"
    }

    fun init(){
        COLUMNS = COLUMNS.substring(0, COLUMNS.length - 1);
        var table = "CREATE TABLE $TABLE_NAME ($COLUMNS);"
        db = context.openOrCreateDatabase(
            DATABASE_NAME,
            Context.MODE_PRIVATE,
            null
        )
        if(!isTableExists(TABLE_NAME)){
            db.execSQL(table)
        }
        COLUMNS = ""
        db.close()
        }

    fun isTableExists(tableName: String): Boolean {
        val query =
            "select DISTINCT tbl_name from sqlite_master where tbl_name = '$tableName'"
        db.rawQuery(query, null).use { cursor ->
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    return true
                }
            }
            return false
        }
    }
}



