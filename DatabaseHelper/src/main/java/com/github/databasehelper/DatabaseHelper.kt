package com.github.databasehelper

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase


class DatabaseHelper(private val context: Context, private val DATABASE_NAME: String){

    // Query
    private lateinit var QUERY :String
    private fun getQueryBuilder(){
        QUERY = "SELECT $SELECT "
        QUERY += "FROM $TABLE_NAME "
        if(SELECTION != null){
           QUERY += "where $SELECTION "
            if(SELECTION_ARG?.size!! > 1){
                QUERY += "in $SELECTION_ARG"
            }else{
                val selection_arg = SELECTION_ARG!![0]
                QUERY += "= $selection_arg"
            }
        }
        if(LIMIT != 0){
            QUERY += "LIMIT $LIMIT "
        }
        if(OFFSET != 0){
            QUERY += "OFFSET $OFFSET "
        }
        if(OrderByColumnName != null){
            QUERY += "ORDER BY $OrderByColumnName $ORDER "
        }
    }
    // Table
    private lateinit var TABLE_NAME :String
    fun table(TableName: String){
        TABLE_NAME = TableName

    }
    // Select
    private var SELECT = "*"
    fun select(Select: Array<String>){
        SELECT = ""
        Select.forEach {
            SELECT += "$it,"
        }
        SELECT = SELECT.substring(0, SELECT.length - 1);
    }
    fun select(Select: String){
        SELECT = Select
    }
    // Where
    private var SELECTION: Array<String>? = null
    private var SELECTION_ARG : Array<String>? = null
    fun where(Selection: String, SelectionArg: String){
        SELECTION = arrayOf(Selection)
        SELECTION_ARG = arrayOf(SelectionArg)
    }
    fun where(Selection: String, SelectionArg: Array<String>){
        SELECTION = arrayOf(Selection)
        SELECTION_ARG = SelectionArg
    }
    // Limit And Offset
    private var LIMIT :Int = 0
    private var OFFSET :Int = 0
    fun limit(limitCount:Int, Offset: Int = 0){
        LIMIT = limitCount
        if(Offset != 0){
            OFFSET = Offset
        }
    }
    // OrderBy
    private var OrderByColumnName: String? = null
    private lateinit var ORDER:String
    fun orderBy(ColumnName: String,Order:String){
        ORDER = Order
        OrderByColumnName = ColumnName
    }
    // First
    @SuppressLint("Recycle")
    fun first(): Cursor? {
        getQueryBuilder()
        val db:SQLiteDatabase = context.openOrCreateDatabase(
            DATABASE_NAME,
            Context.MODE_PRIVATE,
            null
        )
        val cursor = db.rawQuery(QUERY, null)
        cursor.moveToFirst()
        db.close()
        return cursor
    }
    // Get
    fun get(): Cursor? {
        getQueryBuilder()
        val db:SQLiteDatabase = context.openOrCreateDatabase(
                DATABASE_NAME,
                Context.MODE_PRIVATE,
                null
        )
        val cursor = db.rawQuery(QUERY, null)
        cursor.moveToFirst()
        db.close()
        return cursor
    }
    // Insert
    fun insert(ColumnName: Array<String>, ColumnData: Array<String>): Boolean {
        val db:SQLiteDatabase = context.openOrCreateDatabase(
            DATABASE_NAME,
            Context.MODE_PRIVATE,
            null
        )
        db.beginTransaction()
        QUERY = "INSERT INTO $TABLE_NAME ("
        var columns :String = ""
        ColumnName.forEach {
            columns += "$it,"
        }
        columns = columns.substring(0, columns.length - 1)
        QUERY += "$columns) VALUES ("
        var columndataString = ""
        ColumnData.forEach {
            columndataString += "?,"
        }
        columndataString = columndataString.substring(0, columndataString.length - 1)
        QUERY += "$columndataString)"
        val stmt = db.compileStatement(QUERY)
        var forEachIndex = 1
        ColumnData.forEach {
            stmt.bindString(forEachIndex, it)
            forEachIndex++
        }
        stmt.executeInsert()
        stmt.close()
        try{
            db.setTransactionSuccessful()
        }catch (e :Exception){
            db.endTransaction()
            db.close()
            return false
        }
        db.endTransaction()
        db.close()
        return true

    }
}