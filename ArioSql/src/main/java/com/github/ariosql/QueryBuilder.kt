package com.github.ariosql

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import kotlin.math.ceil


class QueryBuilder(private val context: Context, private var DATABASE_NAME: String) {
    companion object {
        val INSERT = 0
        val GET = 1
        val UPDATE = 2
        val DELETE = 3
    }

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
    private var cursorLists: MutableList<Cursor> =
        ArrayList() // list of cursors (to close theme in close() function)
    private var preparedStatements: MutableList<String> = ArrayList()
    private fun resetPreparedStatements() {
        this.preparedStatements = ArrayList()
    }

    // Query
    private var query = StringBuilder()

    /**
     * @return build sql query and return String
     * @sample buildQuery(QueryCode)
     * @sample buildQuery(QueryBuilder.UPDATE)
     * @author MerajV
     * @since 0.3
     */
    fun buildQuery(queryFunction: Int = 0): String {
        val query = StringBuilder() //Query String Builder
        if (queryFunction == 0) { // check if we want to insert new row or not
            if (this.checkTimestamp()) { // check if user active timestamp feature
                val currentTimestamp =
                    this.getCurrentTimestamp().toString()  // get Current Timestamp
                preparedStatements =
                    ArrayList() // empty the preparedStatements Array (use preparedStatements to prevent SQLInjection )
                preparedStatements.addAll(insertValues.toMutableList()) // add all column Values that user want to add to the row
                insertValues = this.append(
                    insertColumns,
                    "created_at"
                ) // add created_at column to add timestamp
                insertValues = this.append(
                    insertColumns,
                    "updated_at"
                ) // add updated_at column to add timestamp
                preparedStatements.add(currentTimestamp) // add timestamp
                preparedStatements.add(currentTimestamp) // add timestamp
            }
            var bindParams = ""
            preparedStatements.forEach {
                bindParams += "?,"
            }
            bindParams = bindParams.substring(0, bindParams.length - 1)
            query.append("INSERT INTO ").append(this.TABLE_NAME).append(" ($insertColumns) ")
                .append("VALUES ").append("($bindParams)") // create insert into query
        } else {
            if(selectRawQuery != ""){
                selectedColumns +=","
            }
            when (queryFunction) { // check for selected query EXPECT insert
                1 -> query.append("SELECT $selectedColumns ").append(selectRawQuery).append("FROM $TABLE_NAME ")
                2 -> query.append("UPDATE $TABLE_NAME SET ")
                3 -> query.append("DELETE ").append("FROM $TABLE_NAME ")
            }
            if (queryFunction == 2) { // check if want to update a row
                var updateSet = ""
                updateColumnNames.forEach {
                    updateSet += "$it = ?,"
                }
                updateSet = updateSet.substring(0, updateSet.length - 1)
                query.append(updateSet)
                if(checkTimestamp()){
                   query.append(",updated_at = ${getCurrentTimestamp()}")
                }
            }
            selectedColumns = ""
            // Where queries
            query.append(this.whereQueries)
            this.whereQueries = ""
            // group by
            query.append(groupByQuery)
            // order query
            query.append(this.orderQuery)
            this.orderQuery = ""
            // limit query
            query.append(this.limitQuery)
            this.limitQuery = ""
        }
        // return builded query
        return query.toString()
    }

    private var lastExecutedQuery = ""

    /**
     * @return String - last Executed Query
     * @author MerajV
     * @since 0.3
     */
    fun lastExecutedQuery(): String {
        return this.lastExecutedQuery
    }

    private lateinit var TABLE_NAME: String // Table name

    /**
     * set table name
     * @property TableName table name
     */
    fun table(TableName: String): QueryBuilder {
        TABLE_NAME = TableName.replace("\\s".toRegex(), "_")
        return this
    }

    // Select Queries
    private var selectedColumns = "*"
    private var selectRawQuery :String = ""
    /**
     * build select statement
     * @property columnNames Array of the column names that you want to get
     * @sample select(arrayOf("column_one","column_two")) // KOTLIN
     * @sample select({"column_one","column_two"}) // JAVA
     * @author MerajV
     * @since 0.1
     */
    fun select(columnNames: Array<String>): QueryBuilder {
        selectedColumns = ""
        columnNames.forEach {
            selectedColumns += "$it,"
        }
        selectedColumns = selectedColumns.substring(0, selectedColumns.length - 1)
        return this
    }

    /**
     * build select statement
     * @property columnName name of the column that you want to get
     * @sample select("column_name") // KOTLIN AND JAVA
     * @author MerajV
     * @since 0.1
     */
    fun select(columnName: String): QueryBuilder {
        selectedColumns = columnName
        return this
    }

    /**
     * add columns names to the current select statement
     * @property columnNames Array list of the columns that you want to add to the previous select
     * @sample select("column_one").addSelect(arrayOf("column_two","column_three")) // KOTLIN
     * @sample select("column_one").addSelect({"column_two","column_three"}) // JAVA
     * @author MerajV
     * @since 0.1
     */
    fun andSelect(columnNames: Array<String>): QueryBuilder {
        selectedColumns += ","
        columnNames.forEach {
            selectedColumns += "$it,"
        }
        selectedColumns = selectedColumns.substring(0, selectedColumns.length - 1)
        return this
    }

    /**
     * add columns names to the current select statement for example:
     * select("column_one").addSelect("column_two"))
     * @property columnName name of the column that you want to add to the previous select
     * @sample select("column_one").addSelect("column_three") // JAVA
     * @author MerajV
     * @since 0.1
     */
    fun andSelect(columnName: String): QueryBuilder {
        selectedColumns += ",$columnName"
        return this
    }

    /**
     * Select raw (custom query as select)
     * @property sql - String
     * @property bindParamsValues- Nullable - String Array That bind values for preventing sql injection
     * @author MerajV
     * @since 0.3
     */
    fun selectRaw(sql:String, bindParamsValues: Array<String>? = null){
        selectRawQuery = sql
        if(this.countMatches(sql,"?") != bindParamsValues?.size) {
            Log.w("QueryBuilder","check bind params again")
        }
        bindParamsValues?.forEach {
            preparedStatements.add(it)
        }
    }

    // Where
    private var whereQueries: String? = ""

    /**
     * build where statement
     * @property columnName name of the column that you want to add to search in
     * @property columnValue value of the column
     * @sample where("column_one","value of the column")
     * @author MerajV
     * @since 0.1
     */
    fun where(columnName: String, columnValue: String): QueryBuilder {
        var firstWhere = "WHERE"
        if (whereQueries != "") {
            firstWhere = "AND"
        }
        whereQueries = "$firstWhere $columnName = ? "
        preparedStatements.add(columnValue)
        return this
    }

    /**
     * build where statement
     * @property columnName name of the column
     * @property operation the operation that you want to apply
     * @property columnValue value of the column
     * @sample where("column_one","LIKE","%jafar%")
     * @sample where("column_one",">","500")
     * @author MerajV
     * @since 0.1
     */
    fun where(columnName: String, operation: String, columnValue: String): QueryBuilder {
        var firstWhere = "WHERE"
        if (whereQueries != "") {
            firstWhere = "AND"
        }
        whereQueries = "$firstWhere $columnName $operation ? "
        preparedStatements.add(columnValue)
        return this
    }

    /**
     * build where is null statement
     * @property columnName name of the column
     * @sample whereNull("column_one")
     * @author MerajV
     * @since 0.3
     */
    fun whereNull(columnName: String): QueryBuilder {
        var firstWhere = "WHERE"
        if (whereQueries != "") {
            firstWhere = "AND"
        }
        whereQueries = "$firstWhere $columnName IS NULL "
        return this
    }

    /**
     * build or  where is null statement
     * @property columnName name of the column
     * @sample orWhereNull("column_one")
     * @author MerajV
     * @since 0.3
     */
    fun orWhereNull(columnName: String): QueryBuilder {
        whereQueries = "OR $columnName IS NULL "
        return this
    }

    /**
     * build where is NOT null statement
     * @property columnName name of the column
     * @sample whereNull("column_one")
     * @author MerajV
     * @since 0.3
     */
    fun whereNotNull(columnName: String): QueryBuilder {
        var firstWhere = "WHERE"
        if (whereQueries != "") {
            firstWhere = "AND"
        }
        whereQueries = "$firstWhere $columnName IS NOT NULL "
        return this
    }

    /**
     * build or where is null NOT statement
     * @property columnName name of the column
     * @sample orWhereNotNull("column_one")
     * @author MerajV
     * @since 0.3
     */
    fun orWhereNotNull(columnName: String): QueryBuilder {
        whereQueries = "OR $columnName IS NOT NULL "
        return this
    }

    /**
     * select rows between 2 value
     * @property columnName name of the column
     * @property from start from
     * @property to until
     * @sample whereBetween("column_one","5","10")
     * @author MerajV
     * @since 0.1
     */
    fun whereBetween(columnName: String, from: String, to: String): QueryBuilder {
        var firstWhere = "WHERE"
        if (whereQueries != "") {
            firstWhere = "AND"
        }
        whereQueries += "$firstWhere $columnName BETWEEN ? AND ? "
        preparedStatements.add(from)
        preparedStatements.add(to)
        return this
    }

    /**
     * select rows that they aren`t between 2 value
     * @property columnName name of the column
     * @property from start from
     * @property to until
     * @sample whereNotBetween("column_one","5","10")
     * @author MerajV
     * @since 0.1
     */
    fun whereNotBetween(columnName: String, from: String, to: String): QueryBuilder {
        var firstWhere = "where"
        if (whereQueries != "") {
            firstWhere = "AND"
        }
        whereQueries += "$firstWhere $columnName NOT BETWEEN ? AND ? "
        preparedStatements.add(from)
        preparedStatements.add(to)
        return this
    }

    /**
     * use or statement in where
     * @property columnName name of the column
     * @property columnValue start from
     * @sample orWhere("column_one","value two")
     * @sample where("column_one","value one").orWhere("column_one","value two").orWhere("column_two","value three")
     * @sample where("column_one","value one").orWhere("column_two","value two")
     * @author MerajV
     * @since 0.1
     */
    fun orWhere(columnName: String, columnValue: String): QueryBuilder {
        whereQueries += "OR $columnName = ? "
        preparedStatements.add(columnValue)
        return this
    }

    /**
     * Where raw (custom query as where)
     * @property sql - String
     * @property bindParamsValues- Nullable - String Array That bind values for preventing sql injection
     * @author MerajV
     * @since 0.3
     */
    fun whereRaw(sql:String, bindParamsValues: Array<String>? = null): QueryBuilder {
        var firstWhere = "WHERE"
        if (whereQueries != "") {
            firstWhere = "AND"
        }
        whereQueries = "$firstWhere $sql "
        if(this.countMatches(sql,"?") != bindParamsValues?.size) {
            Log.w("QueryBuilder","check bind params again")
        }
        bindParamsValues?.forEach {
            preparedStatements.add(it)
        }
        return this
    }
    /**
     * or Where raw (custom query as where)
     * @property sql - String
     * @property bindParamsValues- Nullable - String Array That bind values for preventing sql injection
     * @author MerajV
     * @since 0.3
     */
    fun orWhereRaw(sql:String, bindParamsValues: Array<String>? = null): QueryBuilder {
        whereQueries = "OR $sql "
        if(this.countMatches(sql,"?") != bindParamsValues?.size) {
            Log.w("QueryBuilder","check bind params again")
        }
        bindParamsValues?.forEach {
            preparedStatements.add(it)
        }
        return this
    }

    private var limitQuery: String = ""
    /**
     * build limit query
     * @property Limit the count of the rows that you want to get
     * @property Offset start getting after / Nullable
     * @sample limit(10,5)
     * @author MerajV
     * @since 0.1
     */
    fun limit(Limit: Int, Offset: Int = 0): QueryBuilder {
        limitQuery = "LIMIT $Limit "
        if (Offset != 0) {
            limitQuery += "OFFSET $Offset "
        }
        return this
    }

    // OrderBy
    private var OrderByColumnName: String? = null
    private var ORDER: String? = null
    private var orderQuery = ""

    /**
     * build Order By query
     * @property columnName the name of the column that you want to sort
     * @property Order order type / default = DESC
     * @sample orderBy("column_one","ASC") // ASC ordering
     * @sample orderBy("column_one") // DESC ordering
     * @author MerajV
     * @since 0.1
     */
    fun orderBy(columnName: String, Order: String = "DESC"): QueryBuilder {
        this.orderQuery = "ORDER BY $columnName $Order "
        return this
    }

    fun orderByRaw(sql :String){
      //  this.orderQuery = "ORDER BY $columnName $Order "
    }
    // Group by
    private var groupByQuery = ""

    /**
     * group by (NOT READY)
     * @author MerajV
     * @since 0.3
     */
    fun groupBy(columnName: String): QueryBuilder {
        groupByQuery = "GROUP BY $columnName "
        return this
    }
    /**
     * group by (NOT READY)
     * @author MerajV
     * @since 0.3
     */
    fun groupBy(columnNames: Array<String>): QueryBuilder {
        var columns = ""
        columnNames.forEach {
            columns += "$it,"
        }
        columns = columns.substring(0, columns.length - 1)
        groupByQuery = "GROUP BY $columns "
        return this
    }
    /**
     * count rows
     * @return Int - cont rows and return Int
     * @author MerajV
     * @since 0.1
     */
    @SuppressLint("Recycle")
    fun count(): Int {
        val query = buildQuery(GET)
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery(query, preparedStatements.toTypedArray())
        } catch (e: Exception) {
            return 0
        }
        val rowsCount = cursor.count
        this.resetPreparedStatements()
        cursor.close()
        return rowsCount
    }

    /**
     * get single row
     * @return cursor with 1 row selected
     * @author MerajV
     * @since 0.1
     */
    fun first(): Cursor? {
        val query = buildQuery(GET)
        lastExecutedQuery = query
        val cursor = db.rawQuery(query, preparedStatements.toTypedArray())
        preparedStatements = ArrayList()
        return if (cursor != null && cursor.moveToFirst()) {
            cursorLists.add(cursor)
            cursor
        } else {
            cursor?.close()
            null
        }
    }

    /**
     * get single rows
     * @return cursor with rows
     * @author MerajV
     * @since 0.1
     */
    fun get(): Cursor? {
        val query = buildQuery(GET)
        lastExecutedQuery = query
        val cursor = db.rawQuery(query, preparedStatements.toTypedArray())
        preparedStatements = ArrayList()
        cursorLists.add(cursor)
        return cursor
    }

    private lateinit var insertColumns: Array<String>
    private lateinit var insertValues: Array<String>

    /**
     * execute insert query
     * @return Long - the row ID of the last row inserted, if this insert is successful. -1 otherwise.
     * @property ColumnName column Name or Names as String or String Array
     * @property ColumnName column Name or Names as String or String Array
     * @sample insert("column_one","jafar") // KOTLIN - JAVA
     * @sample insert(arrayOf("column_one","column_two"),arrayOf("jafar","0912")) // KOTLIN
     * @sample insert({"column_one","column_two"},{"jafar","0912"}) // JAVA
     * @author MerajV
     * @since 0.1
     **/
    fun insert(columnNames: Array<String>, columnValues: Array<String>): Long {
        insertColumns = columnNames
        insertValues = columnValues
        return this.doInsert()
    }

    /**
     * execute insert query
     * @return Long - the row ID of the last row inserted, if this insert is successful. -1 otherwise.
     * @property ColumnName column Name or Names as String or String Array
     * @property ColumnName column Name or Names as String or String Array
     * @sample insert("column_one","jafar") // KOTLIN - JAVA
     * @sample insert(arrayOf("column_one","column_two"),arrayOf("jafar","0912")) // KOTLIN
     * @sample insert({"column_one","column_two"},{"jafar","0912"}) // JAVA
     * @author MerajV
     * @since 0.1
     **/
    fun insert(ColumnName: String, columnValue: String): Long {
        insertColumns = arrayOf(ColumnName)
        insertValues = arrayOf(columnValue)
        return this.doInsert()
    }

    /**
     * this function just prevent duplicate codes
     * we have 2 insert function with 2 different input types
     * so this function will prevent duplicate coding about inserting row
     */
    private fun doInsert(): Long {
        val query = buildQuery(INSERT)
        lastExecutedQuery = query
        db.beginTransaction()
        val stmt = db.compileStatement(query)
        var forEachIndex = 1
        preparedStatements.forEach {
            stmt.bindString(forEachIndex,it)
        }
        val insertedId = stmt.executeInsert()
        stmt.close()
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

    // Execute Update Queries
    private lateinit var updateColumnNames: Array<String>
    private lateinit var updateColumnValues: Array<String>

    /**
     * update column or columns of exist row/rows
     * @return if successfully updated return true, false otherwise
     * @property columnName column Name or column Names as String or String Array
     * @property columnValue column Value or column Values as String or String Array
     * @author MerajV
     * @since 0.1
     */
    fun update(columnName: String, columnValue: String): Boolean {
        updateColumnNames = arrayOf(columnName)
        updateColumnValues = arrayOf(columnValue)
        return this.doUpdate()
    }

    /**
     * update column or columns of exist row/rows
     * @return if successfully updated return true, false otherwise
     * @property columnNames column Name or column Names as String or String Array
     * @property columnValues column Value or column Values as String or String Array
     * @author MerajV
     * @since 0.1
     */
    fun update(columnNames: Array<String>, columnValues: Array<String>): Boolean {
        updateColumnNames = columnNames
        updateColumnValues = columnValues
        return if(updateColumnNames.size == updateColumnValues.size) {
            this.doUpdate()
        }else{
            Log.w("QueryBuilder","ops ,column names and column values are different in size ,check theme again")
            false
        }
    }

    /**
     * this function just prevent duplicate codes
     * we have 2 update function with 2 different input types
     * so this function will prevent duplicate coding about updating the row/rows
     */
    private fun doUpdate(): Boolean {
        val query = buildQuery(UPDATE)
        db.beginTransaction()
        val stmt = db.compileStatement(query)
        var forEachIndex = 1
        updateColumnValues.forEach {
            stmt.bindString(forEachIndex, it)
            forEachIndex++
        }
        preparedStatements.forEach {
            stmt.bindString(forEachIndex, it)
            forEachIndex++
        }
        stmt.executeUpdateDelete()
        stmt.close()
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

    // timestamp System
    /**
     * this function will update updated_at column in row/rows ,with selected timestamp or get current timestamp by default
     * @property timestamp Long - Nullable ,if leave it null will get current timestamp auto
     * @author MerajV
     * @since 0.3
     */
    fun updateCreatedAt(rowId: Long, timestamp: Long? = null) {
        val createdAtTime: String = timestamp?.toString() ?: System.currentTimeMillis().toString()
        this.where("id", rowId.toString()).update("created_at", createdAtTime)
    }

    /**
     * this function will update created_at column in row/rows ,with selected timestamp or get current timestamp by default
     * @property timestamp Long - Nullable ,if leave it null will get current timestamp auto
     * @author MerajV
     * @since 0.3
     */
    fun updateUpdatedAt(timestamp: Long? = null) {
        val newTimestamp = timestamp?.toString() ?: this.getCurrentTimestamp().toString()
        this.update("updated_at", newTimestamp)
    }

    /**
     * check if timestamp feature is enabled
     * @return Boolean - true if supported, false otherwise
     * @author MerajV
     * @since 0.3
     */
    private fun checkTimestamp(): Boolean {
        val getTimestampColumns: Cursor =
            this.rawQuery("SELECT created_at,updated_at FROM $TABLE_NAME", null)!!
            getTimestampColumns.moveToFirst()
            val updatedAtColumn = getTimestampColumns.getColumnIndex("updated_at")
            val createdAtColumn = getTimestampColumns.getColumnIndex("created_at")
            return !(updatedAtColumn == -1 && createdAtColumn == -1)
    }

    /**
     * @return Current Timestamp
     * @author MerajV
     * @since 0.3
     */
    private fun getCurrentTimestamp(): Long {
        return System.currentTimeMillis()
    }

    // Execute Delete Query
    /**
     * run this function will delete selected row/rows
     */
    fun delete(): Boolean {
        val Query = this.buildQuery(DELETE)
        this.lastExecutedQuery = Query
        db.beginTransaction()
        val stmt = db.compileStatement(Query)
        var forEachIndex = 1
        preparedStatements.forEach {
            stmt.bindString(forEachIndex, it)
            forEachIndex++
        }
        stmt.executeUpdateDelete()
        stmt.close()
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
    /**
     * paginating system
     * @return Paginate Class - will return Paginate Class
     * @property resultsPerPage Int - Results Per Page
     * @property CurrentPage Int - Nullable Current Page
     */
    fun paginate(resultsPerPage: Int, CurrentPage: Int = 1): Paginate {
        val SavepreparedStatements = preparedStatements
         val saveSelect = selectedColumns
        val saveWhere = whereQueries
        val saveOrderByColumnName = OrderByColumnName
        val saveORDER = ORDER
        val totalRows = this.count()
        preparedStatements = SavepreparedStatements
        selectedColumns = saveSelect
        whereQueries = saveWhere
        OrderByColumnName = saveOrderByColumnName
        ORDER = saveORDER
        val totalPages = ceil((totalRows / resultsPerPage).toDouble()).toInt()
        val getFrom: Int = (CurrentPage - 1) * resultsPerPage
        this.limit(resultsPerPage, getFrom)
        val rows = this.get()
        return if (rows != null) {
            Paginate(totalPages, rows, CurrentPage)
        } else {
            Paginate(1, null, 1)
        }
    }

    /**
     * check if a row exist or not
     * @return Boolean - true if exist, false otherwise
     * @author MerajV
     * @since 0.1
     */
    fun exists(): Boolean {
        // getQueryBuilder()
        return this.count() > 0
    }

    /**
     * check if a row DOES NOT exist or not
     * @return Boolean - true if DOES NOT exist, false otherwise
     * @author MerajV
     * @since 0.1
     */
    fun doesntExist(): Boolean {
        // getQueryBuilder()
        return this.count() == 0
    }

    /**
     * run custom query
     * @return Cursor
     * @property sql String - Custom Query
     * @property bindParamsValues String Array - Nullable bind params for prevent sql injection
     * @author MerajV
     * @since 0.3
     */
    fun rawQuery(sql:String, bindParamsValues: Array<String>? = null): Cursor? {
        this.lastExecutedQuery = sql
        db.beginTransaction()
        var cursor: Cursor? = null
        try {
            cursor =  db.rawQuery(sql,bindParamsValues)
        } catch (e: Exception) {
            db.endTransaction()
            return cursor
        }
        db.endTransaction()
        return cursor
    }
    /**
     * connection to the database
     * @return SQLiteDatabase - current database connection
     * @author MerajV
     * @since 0.3
     */
    fun connection(): SQLiteDatabase {
        return db
    }
    /**
     * OPEN closed connection to the database
     * @author MerajV
     * @since 0.3
     */
    fun open() {
        db = context.openOrCreateDatabase(
            DATABASE_NAME,
            Context.MODE_PRIVATE,
            null
        )
    }
    /**
     * CLOSE connection to the database And CLOSE cursors
     * @author MerajV
     * @since 0.3
     */
    fun close() {
        cursorLists.forEach {
            it.close()
        }
        db.close()
    }

    private fun append(arr: Array<String>, element: String): Array<String> {
        val list: MutableList<String> = arr.toMutableList()
        list.add(element)
        return list.toTypedArray()
    }
    private fun countMatches(string: String, pattern: String): Int {
        return string.split(pattern)
            .dropLastWhile { it.isEmpty() }
            .toTypedArray().size - 1
    }

}