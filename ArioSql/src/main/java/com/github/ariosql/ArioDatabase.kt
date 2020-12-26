package com.github.ariosql

import android.content.Context
import android.database.sqlite.SQLiteDatabase


class ArioDatabase(private val context: Context) {
    //
    companion object {
        val PRIMARY_KEY = "INTEGER PRIMARY KEY AUTOINCREMENT"
        val ID = "id"
        val BLOB = "BLOB"
        val BIGINT = "BIGINT"
        val BOOLEAN = "BOOLEAN"
        val CHAR = "CHAR"
        val DATE = "DATE"
        val DATETIME = "DATETIME"
        val DECIMAL = "DECIMAL"
        val DOUBLE = "DOUBLE"
        val INTEGER = "INTEGER"
        val INT = "INT"
        val NONE = "NONE"
        val NUMERIC = "NUMERIC"
        val REAL = "REAL"
        val STRING = "STRING"
        val TEXT = "TEXT"
        val TIME = "TIME"
        val VARCHAR = "VARCHAR"
    }

    //
    private lateinit var TABLE_NAME: String
    private lateinit var DATABASE_NAME: String
    private var DATABASE_VERSION: Int = 1
    private var COLUMNS: String = ""
    private var OnCreateTabels: MutableList<String> = ArrayList()
    private var CustomOnUpgradeTabels: MutableList<String> = ArrayList()
    private var OnUpgrade: MutableList<String> = ArrayList()
    private lateinit var db: SQLiteDatabase
    /**
     * set Database Name
     * @property Name Database Name :String
     */
    fun dbName(Name: String): ArioDatabase {
        DATABASE_NAME = Name
        if (!DATABASE_NAME.endsWith(".db")) {
            DATABASE_NAME += ".db";
        }
        DATABASE_NAME = DATABASE_NAME.replace("\\s".toRegex(), "_")
        return this
    }
    /**
     * set Database Version
     * @property Version Current Version: Int
     */
    fun version(Version: Int): ArioDatabase {
        DATABASE_VERSION = Version
        return this
    }

    /**
     * set Table Name
     * @property Name Table Name : String
     */
    fun table(Name: String): ArioDatabase {
        if (COLUMNS != "") {
            this.save()
        }
        TABLE_NAME = Name.replace("\\s".toRegex(), "_")
        return this
    }

    /**
     * add unique id column
     * each row will be get a unique id to define it
     */
    fun id(): ArioDatabase {
        COLUMNS += "id $PRIMARY_KEY,"
        return this
    }

    /**
     * active timestamp feature for your table
     * this feature automatically create 2 column -> created_at And updated_at
     * and manage theme automatically
     * the value of theme will be timestamp in millisecond
     */
    fun timestamp(): ArioDatabase {
        COLUMNS += "created_at BIGINT NULL,updated_at BIGINT NULL,"
        return this
    }

    /**
     * add new column
     * @property columnName column Name : String
     * @property DataType column Data Type : String
     */
    fun column(columnName: String, DataType: String): ArioDatabase {
        COLUMNS += "$columnName $DataType,"
        return this
    }
    /**
     * run a sql query when database version upgraded
     * you can add multiple queries
     * @property sql - String - Query
     */
    fun upgradeRaw(sql: String) {
        CustomOnUpgradeTabels.add(sql)
    }

    /**
     * in upgrading the database will remove the table and recreate it
     */
    fun justReCreateIt(): ArioDatabase {
        CustomOnUpgradeTabels.add("DROP TABLE IF EXISTS $TABLE_NAME")
     return this
    }

    private fun save() {
        COLUMNS = COLUMNS.substring(0, COLUMNS.length - 1)
        OnCreateTabels.add("CREATE TABLE $TABLE_NAME ($COLUMNS);")
        OnUpgrade.add("DROP TABLE IF EXISTS $TABLE_NAME;")
        COLUMNS = ""
    }

    /**
     * initialize the database
     */
    fun init() {
        this.save()
        db = context.openOrCreateDatabase(
            DATABASE_NAME,
            Context.MODE_PRIVATE,
            null
        )
        val currentDatabaseVersion = db.version
        if (currentDatabaseVersion == DATABASE_VERSION || currentDatabaseVersion == 0) {
            if (!isTableExists(TABLE_NAME)) {
                createTables(db)
                db.version = DATABASE_VERSION
            }
        } else {
            if (CustomOnUpgradeTabels.size > 0) {
                upgradeTables(db, CustomOnUpgradeTabels)
                db.version = DATABASE_VERSION
            }
           /* else {
                upgradeTables(db, OnUpgrade)
                db.version = DATABASE_VERSION
            }*/
        }
    }

    private fun createTables(db: SQLiteDatabase) {
        OnCreateTabels.forEach {
            db.execSQL(it)
        }
    }

    private fun upgradeTables(db: SQLiteDatabase, UpgradeQueries: MutableList<String>) {
        UpgradeQueries.forEach {
            db.execSQL(it)
        }
        createTables(db)
    }

    private fun isTableExists(tableName: String): Boolean {
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



