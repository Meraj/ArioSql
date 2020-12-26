package com.github.androidquerybuilder

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import kotlin.math.ceil


class QueryBuilder(private val context: Context, private var DATABASE_NAME: String) {
    private var db: SQLiteDatabase // SQLiteDatabase
    init {
        if (!DATABASE_NAME.endsWith(".db")) {
            DATABASE_NAME += ".db" // Add .db extension if user don`t add it
        }
        DATABASE_NAME = DATABASE_NAME.replace("\\s".toRegex(), "_") // replace Spaces with _
        db = context.openOrCreateDatabase(
            DATABASE_NAME,
            Context.MODE_PRIVATE,
            null
        )
    }

    //
    private var cursorLists: MutableList<Cursor> = ArrayList() // list of cursors (to close theme in close() function)
    private var preparedStatements: MutableList<String> = ArrayList()
    // Query
    private var QUERY: String = "" // Builded Query String
    // a function to update get Query
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
    // a function to update update query
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
    // a function to update delete query
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
    private lateinit var TABLE_NAME: String // Table name
    // set Table Name function
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
            cursorLists.add(cursor)
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
        cursorLists.add(cursor)
        return cursor
    }

    // Insert
    /**
    @return the row ID of the last row inserted, if this insert is successful. -1 otherwise.
    **/
    fun insert(ColumnName: Array<String>, ColumnData: Array<String>): Long {
        return this.doInsert(ColumnName, ColumnData)
    }
    /**
    @return the row ID of the last row inserted, if this insert is successful. -1 otherwise.
     **/
    fun insert(ColumnName: String, ColumnData: String): Long {
       return this.doInsert(arrayOf(ColumnName), arrayOf(ColumnData))
    }
    private fun doInsert(ColumnNames: Array<String>, ColumnDatas: Array<String>): Long {
        var newDatas :Array<String>
        var newColumnNames :Array<String>
        if (checkTimestamp()){
            val newListWithTimestamp: MutableList<String> = ColumnNames.toMutableList()
            newListWithTimestamp.add("updated_at")
            newListWithTimestamp.add("created_at")
            newColumnNames = newListWithTimestamp.toTypedArray()

            val newColumnDatas: MutableList<String> = ColumnDatas.toMutableList()
            newColumnDatas.add(System.currentTimeMillis().toString())
            newColumnDatas.add(System.currentTimeMillis().toString())
            newDatas = newColumnDatas.toTypedArray()
        }else{
            newColumnNames = ColumnNames
            newDatas = ColumnDatas
        }
        db.beginTransaction()
        QUERY = "INSERT INTO $TABLE_NAME ("
        var columns = ""
        newColumnNames.forEach {
            columns += "$it,"
        }
        columns = columns.substring(0, columns.length - 1)
        QUERY += "$columns) VALUES ("
        var columnDataString = ""
        newColumnNames.forEach {
            columnDataString += "?,"
        }
        columnDataString = columnDataString.substring(0, columnDataString.length - 1)
        QUERY += "$columnDataString)"
        val stmt = db.compileStatement(QUERY)
        var forEachIndex = 1
        newDatas.forEach {
            stmt.bindString(forEachIndex, it)
            forEachIndex++
        }

        val insertedId =stmt.executeInsert()
        stmt.close()
        QUERY = ""
        preparedStatements = ArrayList()
        try {
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            db.endTransaction()
            return -1
        }
        db.endTransaction()

        return insertedId
    }
    // check for timestamp
    @SuppressLint("Recycle")
    private fun checkTimestamp(): Boolean {
        val getTimestampColumns :Cursor
        try {
            getTimestampColumns = this.connection().rawQuery("SELECT created_at,updated_at FROM $TABLE_NAME",null)
        }catch (e:Exception){
            return false
        }
        return if(getTimestampColumns != null){
            getTimestampColumns.moveToFirst()
            val updatedAtColumn = getTimestampColumns.getColumnIndex("updated_at")
            val createdAtColumn = getTimestampColumns.getColumnIndex("created_at")
            !(updatedAtColumn == -1 && createdAtColumn == -1)
        }else{
            false
        }
    }
    fun updateCreatedAt(rowId:Long,timestamp:Long? = null){
        val createdAtTime : String = timestamp?.toString() ?: System.currentTimeMillis().toString()
        this.where("id",rowId.toString()).update("created_at",createdAtTime)
    }

    // update
    fun update(columnName: String, new_values: String): Boolean {
        if(checkTimestamp()){
            updateQueryBuilder(arrayOf(columnName,"updated_at"))
            preparedStatements.add(System.currentTimeMillis().toString())
        }else{
            updateQueryBuilder(arrayOf(columnName))
        }
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
        var newValues :Array<String>
        if(checkTimestamp()){
            val newListWithUpdatedAt: MutableList<String> = columnNames.toMutableList()
            newListWithUpdatedAt.add("updated_at")
            val newColumnNames = newListWithUpdatedAt.toTypedArray()

            val newListValuesUpdatedAt: MutableList<String> = new_values.toMutableList()
            newListValuesUpdatedAt.add(System.currentTimeMillis().toString())
            newValues = newListValuesUpdatedAt.toTypedArray()
            updateQueryBuilder(newColumnNames)
            preparedStatements.add(System.currentTimeMillis().toString())
        }else{
            updateQueryBuilder(columnNames)
            newValues = new_values
        }
        db.beginTransaction()
        val stmt = db.compileStatement(QUERY)
        var forEachIndex = 1
        newValues.forEach {
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
    fun updateTime(timestamp: Long? = null){
        var newTimestamp :String
        if(timestamp == null){
            newTimestamp = System.currentTimeMillis().toString()
        }else{
            newTimestamp = timestamp.toString()
        }
        this.update("updated_at",newTimestamp)
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
    // pagination
    fun paginate(resultsPerPage:Int ,CurrentPage:Int = 1): Paginate {
        val SavepreparedStatements = preparedStatements
        val saveSelect = SELECT
        val saveWhere = Where
        val saveOrderByColumnName = OrderByColumnName
        val saveORDER = ORDER
        val totalRows = this.count()
        preparedStatements = SavepreparedStatements
        SELECT = saveSelect
        Where = saveWhere
        OrderByColumnName = saveOrderByColumnName
        ORDER = saveORDER
        val totalPages = ceil((totalRows/resultsPerPage).toDouble()).toInt()
        val getFrom:Int = (CurrentPage -1) * resultsPerPage
        this.limit(resultsPerPage,getFrom)
        val rows = this.get()
        return if(rows != null){
            Paginate(totalPages,rows,CurrentPage)
        }else{
            Paginate(1,null,1)
        }
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
    // Custom Query

    // Open And Close Connection
    fun connection(): SQLiteDatabase {
        return db
    }
    fun open() {
        db = context.openOrCreateDatabase(
            DATABASE_NAME,
            Context.MODE_PRIVATE,
            null
        )
    }

    fun close() {
        cursorLists.forEach {
            it.close()
        }
        db.close()
    }
}