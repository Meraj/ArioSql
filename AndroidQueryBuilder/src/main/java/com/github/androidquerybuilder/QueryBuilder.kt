package com.github.androidquerybuilder

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase


class QueryBuilder(private val context: Context, private var DATABASE_NAME: String) {
    private var db: SQLiteDatabase

    init {
        if (!DATABASE_NAME.endsWith(".db")) {
            DATABASE_NAME += ".db";
        }
        DATABASE_NAME = DATABASE_NAME.replace(" ", "_")
        db = context.openOrCreateDatabase(
            DATABASE_NAME,
            Context.MODE_PRIVATE,
            null
        )
    }

    //
    private var CursorLists: MutableList<Cursor> = ArrayList()
    private var preparedStatements: MutableList<String> = ArrayList()

    // Query
    private var QUERY: String = ""
    private fun getQueryBuilder() {
        if (SELECT == "*") {
            QUERY = "SELECT * "
        } else {
            QUERY = SELECT
        }
        QUERY += "FROM $TABLE_NAME "
        if (Where != "") {
            QUERY += Where
        }
        if (OrderByColumnName != null) {
            QUERY += "ORDER BY $OrderByColumnName $ORDER "
        }

        if (LimitQuery != "") {
            QUERY += LimitQuery
        }
        selectColumns = ""
        SELECT = "*"
        Where = ""
    }

    private fun updateQueryBuilder(columnNames: Array<String>) {
        QUERY = "UPDATE "
        QUERY += "$TABLE_NAME SET "
        var setValues = ""
        columnNames.forEach {
            setValues += "$it = ?,"
        }
        setValues = setValues.substring(0, setValues.length - 1)
        QUERY += "$setValues "
        if (Where != "") {
            QUERY += Where
        }
        if (OrderByColumnName != null) {
            QUERY += " ORDER BY $OrderByColumnName $ORDER "
        }
        if (LimitQuery != "") {
            QUERY += LimitQuery
        }
        selectColumns = ""
        SELECT = "*"
        Where = ""
    }

    private fun deleteQueryBuilder() {
        QUERY = "DELETE "

        QUERY += "FROM $TABLE_NAME "
        if (Where != "") {
            QUERY += Where
        }
        if (OrderByColumnName != null) {
            QUERY += "ORDER BY $OrderByColumnName $ORDER "
        }

        if (LimitQuery != "") {
            QUERY += LimitQuery
        }
        selectColumns = ""
        SELECT = "*"
        Where = ""
    }

    // Table
    private lateinit var TABLE_NAME: String
    fun table(TableName: String): QueryBuilder {
        TABLE_NAME = TableName
        return this
    }

    // Select
    private var SELECT = "*"
    private var selectColumns = ""
    fun select(Select: Array<String>): QueryBuilder {
        Select.forEach {
            selectColumns += "$it,"
        }
        SELECT = "SELECT " + selectColumns.substring(0, selectColumns.length - 1) + " "
        return this
    }

    fun select(Select: String): QueryBuilder {
        selectColumns = Select
        SELECT = "SELECT $Select "
        return this
    }

    fun andSelect(Select: Array<String>): QueryBuilder {
        Select.forEach {
            selectColumns += "$it,"
        }
        SELECT = "SELECT " + selectColumns.substring(0, selectColumns.length - 1) + " "
        return this
    }

    fun andSelect(Select: String): QueryBuilder {
        selectColumns += Select
        SELECT = "SELECT $selectColumns "
        return this
    }

    // Where
    private var Where: String? = ""
    fun where(Selection: String, SelectionArg: String): QueryBuilder {
        if (Where != "") {
            Where += "AND $Selection = ? "
            preparedStatements.add(SelectionArg)
        } else {
            Where = "WHERE $Selection = ? "
            preparedStatements.add(SelectionArg)
        }
        return this
    }

    fun where(Selection: String, operation: String, SelectionArg: String): QueryBuilder {
        if (Where != "") {
            Where += "AND $Selection $operation ? "
        } else {
            Where = "WHERE $Selection $operation ? "
        }
        preparedStatements.add(SelectionArg)
        return this
    }

    fun whereBetween(Selection: String, from: String, to: String): QueryBuilder {
        var before = "WHERE"
        if (Where != "") {
            before = "AND"
        }
        Where += "$before $Selection BETWEEN ? AND ? "
        preparedStatements.add(from)
        preparedStatements.add(to)
        return this
    }

    fun whereNotBetween(Selection: String, from: String, to: String): QueryBuilder {
        var before = "where"
        if (Where != "") {
            before = "AND"
        }
        Where += "$before $Selection NOT BETWEEN ? AND ? "
        preparedStatements.add(from)
        preparedStatements.add(to)
        return this
    }

    fun orWhere(Selection: String, SelectionArg: String): QueryBuilder {
        Where += "OR $Selection = ? "
        preparedStatements.add(SelectionArg)
        return this
    }

    // Limit And Offset
    private var LimitQuery: String = ""
    fun limit(Limit: Int, Offset: Int = 0): QueryBuilder {
        LimitQuery = "LIMIT $Limit "
        if (Offset != 0) {
            LimitQuery += "OFFSET $Offset "
        }
        return this
    }

    // OrderBy
    private var OrderByColumnName: String? = null
    private var ORDER: String? = null
    fun orderBy(ColumnName: String, Order: String = "DESC"): QueryBuilder {
        OrderByColumnName = ColumnName
        ORDER = Order
        return this
    }

    // Count
    @SuppressLint("Recycle")
    fun count(): Int {
        getQueryBuilder()
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery(QUERY, preparedStatements.toTypedArray())
        } catch (e: Exception) {
            return 0
        }
        val rowsCount: Int = cursor.count
        db.close()
        QUERY = ""
        preparedStatements = ArrayList()
        cursor.close()
        return rowsCount
    }

    // First
    @SuppressLint("Recycle")
    fun first(): Cursor? {
        getQueryBuilder()

        val cursor = db.rawQuery(QUERY, preparedStatements.toTypedArray())

        QUERY = ""
        preparedStatements = ArrayList()
        return if (cursor != null && cursor.moveToFirst()) {
            CursorLists.add(cursor)
            cursor
        } else {
            cursor?.close()
            null
        }
    }

    // Get
    fun get(): Cursor? {
        getQueryBuilder()
        val cursor = db.rawQuery(QUERY, preparedStatements.toTypedArray())
        QUERY = ""
        preparedStatements = ArrayList()
        CursorLists.add(cursor)
        return cursor
    }

    // Insert
    fun insert(ColumnName: Array<String>, ColumnData: Array<String>): Boolean {
        db.beginTransaction()
        QUERY = "INSERT INTO $TABLE_NAME ("
        var columns = ""
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
        QUERY = ""
        preparedStatements = ArrayList()
        try {
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            db.endTransaction()
            return false
        }
        db.endTransaction()
        return true
    }

    // update
    fun update(columnNames: String, new_values: String): Boolean {
        updateQueryBuilder(arrayOf(columnNames))
        db.beginTransaction()
        val stmt = db.compileStatement(QUERY)
        stmt.bindString(1, new_values)
        var forEachIndex = 2
        preparedStatements.forEach {
            stmt.bindString(forEachIndex, it)
            forEachIndex++
        }

        stmt.executeUpdateDelete()
        stmt.close()
        QUERY = ""
        preparedStatements = ArrayList()
        try {
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            db.endTransaction()
            return false
        }
        db.endTransaction()
        return true
    }

    @SuppressLint("Recycle")
    fun update(columnNames: Array<String>, new_values: Array<String>): Boolean {
        updateQueryBuilder(columnNames)
        db.beginTransaction()
        val stmt = db.compileStatement(QUERY)
        var forEachIndex = 1
        new_values.forEach {
            stmt.bindString(forEachIndex, it)
            forEachIndex++
        }
        preparedStatements.forEach {
            stmt.bindString(forEachIndex, it)
            forEachIndex++
        }

        stmt.executeUpdateDelete()
        stmt.close()
        QUERY = ""
        preparedStatements = ArrayList()
        try {
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            db.endTransaction()
            return false
        }
        db.endTransaction()
        return true
    }

    //delete
    @SuppressLint("Recycle")
    fun delete(): Boolean {
        deleteQueryBuilder()
        db.beginTransaction()
        val stmt = db.compileStatement(QUERY)
        var forEachIndex = 1
        preparedStatements.forEach {
            stmt.bindString(forEachIndex, it)
            forEachIndex++
        }

        stmt.executeUpdateDelete()
        stmt.close()
        QUERY = ""
        preparedStatements = ArrayList()
        try {
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            db.endTransaction()
            return false
        }
        db.endTransaction()
        return true
    }

    // Exist And does not exist
    fun exists(): Boolean {
        getQueryBuilder()
        return this.count() > 0
    }

    fun doesntExist(): Boolean {
        getQueryBuilder()
        return this.count() == 0
    }

    // Open And Close Connection
    fun open() {
        db = context.openOrCreateDatabase(
            DATABASE_NAME,
            Context.MODE_PRIVATE,
            null
        )
    }

    fun close() {
        CursorLists.forEach {
            it.close()
        }
        db.close()
    }
}