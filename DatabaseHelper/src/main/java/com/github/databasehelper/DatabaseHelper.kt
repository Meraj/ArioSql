package com.github.databasehelper

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase


class DatabaseHelper(private val context: Context, private var DATABASE_NAME: String){
    init {
        if (!DATABASE_NAME.endsWith(".db")){
            DATABASE_NAME += ".db";
        }
        DATABASE_NAME = DATABASE_NAME.replace(" ", "_");
    }
    //
    private var preparedStatements :MutableList<String> = ArrayList()
    // Query
    private var QUERY :String = ""
    private fun getQueryBuilder(){
        if(SELECT == "*"){
            QUERY = "SELECT $SELECT "
        }else{
            QUERY = SELECT
        }
        QUERY += "FROM $TABLE_NAME "
        if(Where != ""){
            QUERY += Where
        }
        if(OrderByColumnName != ""){
           QUERY += "ORDER BY $OrderByColumnName $ORDER "  
        }
        
        if(LimitQuery != ""){
            QUERY = LimitQuery
        }

    }
    // Table
    private lateinit var TABLE_NAME :String
    fun table(TableName: String): DatabaseHelper {
        TABLE_NAME = TableName
        return this

    }
    // Select
    private var SELECT = "*"
    fun select(Select: Array<String>): DatabaseHelper {
        SELECT = "SELECT "
        Select.forEach {
            SELECT += "$it,"
        }
        SELECT = SELECT.substring(0, SELECT.length - 1) + " "
        return this
    }
    fun select(Select: String): DatabaseHelper {
        SELECT = "SELECT $Select "
        return this
    }
    // Where
    private var Where : String? = ""
    fun where(Selection: String, SelectionArg: String): DatabaseHelper {
        Where = "where $Selection = ? "
        preparedStatements.add(SelectionArg)
        return this
    }
    fun whereRange(Selection: String, from: String ,to:String,Between: Boolean = true): DatabaseHelper {
        var before = "where"
        if(Where != ""){
            before = "AND"
        }
        if(Between){
            Where += "$before $Selection BETWEEN ? to ? "
        }else{
            Where += "$before $Selection NOT BETWEEN ? to ? "
        }
        preparedStatements.add(from)
        preparedStatements.add(to)
        return this
    }
    fun andWhere(Selection: String, SelectionArg: String): DatabaseHelper {
        Where += "AND $Selection = ? "
        preparedStatements.add(SelectionArg)
        return this
    }
    fun orWhere(Selection: String, SelectionArg: String): DatabaseHelper {
        Where += "OR $Selection = ? "
        preparedStatements.add(SelectionArg)
        return this
    }
    // Limit And Offset
    private var LimitQuery :String = ""
    fun limit(Limit:Int, Offset: Int = 0): DatabaseHelper {
        LimitQuery = "LIMIT $Limit "
        if(Offset != 0){
            LimitQuery += "OFFSET $Offset "
        }
        return this
    }
    // OrderBy
    private var OrderByColumnName: String? = null
    private lateinit var ORDER:String
    fun orderBy(ColumnName: String,Order:String): DatabaseHelper {
        OrderByColumnName = ColumnName
        ORDER = Order
        return this
    }
    // First
    @SuppressLint("Recycle")
    fun first(): Cursor? {
        if(QUERY == ""){
            QUERY = "SELECT * FROM"
        }
        val db:SQLiteDatabase = context.openOrCreateDatabase(
            DATABASE_NAME,
            Context.MODE_PRIVATE,
            null
        )
        val cursor = db.rawQuery(QUERY, preparedStatements.toTypedArray())
        cursor.moveToFirst()
        db.close()
        QUERY = ""
        preparedStatements = ArrayList()
        return cursor
    }
    // Get
    fun get(): Cursor? {
        val db:SQLiteDatabase = context.openOrCreateDatabase(
                DATABASE_NAME,
                Context.MODE_PRIVATE,
                null
        )
        val cursor = db.rawQuery(QUERY, preparedStatements.toTypedArray())
        cursor.moveToFirst()
        db.close()
        QUERY = ""
        preparedStatements = ArrayList()
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
