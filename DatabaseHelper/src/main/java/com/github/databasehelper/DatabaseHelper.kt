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
    // Table
    private lateinit var TABLE_NAME :String
    fun table(TableName: String): DatabaseHelper {
        TABLE_NAME = TableName
        return this

    }
    // Select
    private var SELECT = "SELECT * "
    fun select(Select: Array<String>): DatabaseHelper {
        SELECT = "SELECT "
        Select.forEach {
            SELECT += "$it,"
        }
        QUERY += SELECT.substring(0, SELECT.length - 1) + " "
        return this
    }
    fun select(Select: String): DatabaseHelper {
        QUERY += "SELECT $Select"
        return this
    }
    // Where
    fun where(Selection: String, SelectionArg: String): DatabaseHelper {
        QUERY = "where $Selection = ?"
        preparedStatements.add(SelectionArg)
        return this
    }
    fun whereRange(Selection: String, from: String ,to:String,Between: Boolean = true): DatabaseHelper {
        if(Between){
            QUERY = "where $Selection BETWEEN ? to ?"
        }else{
            QUERY = "where $Selection NOT BETWEEN ? to ?"
        }
        preparedStatements.add(from)
        preparedStatements.add(to)
        return this
    }
    fun andWhere(Selection: String, SelectionArg: String): DatabaseHelper {
        QUERY += "AND $Selection = ?"
        preparedStatements.add(SelectionArg)
        return this
    }
    fun orWhere(Selection: String, SelectionArg: String): DatabaseHelper {
        QUERY += "OR $Selection = ?"
        preparedStatements.add(SelectionArg)
        return this
    }
    // Limit And Offset
    fun limit(Limit:Int, Offset: Int = 0): DatabaseHelper {
            QUERY += "LIMIT $Limit "
        if(Offset != 0){
            QUERY += "OFFSET $Offset "
        }
        return this
    }
    // OrderBy
    private var OrderByColumnName: String? = null
    private lateinit var ORDER:String
    fun orderBy(ColumnName: String,Order:String): DatabaseHelper {
        QUERY += "ORDER BY $OrderByColumnName $ORDER "
        return this
    }
    // First
    @SuppressLint("Recycle")
    fun first(): Cursor? {
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
