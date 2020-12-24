package com.github.androidquerybuilder

import android.content.Context
import android.database.sqlite.SQLiteDatabase


class CreateDatabase(private val context: Context) {
    private lateinit var TABLE_NAME: String
    private lateinit var DATABASE_NAME: String
    private var DATABASE_VERSION: Int = 1
    private var COLUMNS: String = ""
    private var OnCreateTabels: MutableList<String> = ArrayList()
    private var CustomOnUpgradeTabels: MutableList<String> = ArrayList()
    private var OnUpgrade: MutableList<String> = ArrayList()
    private lateinit var db: SQLiteDatabase
    fun database(Name: String): CreateDatabase {
        DATABASE_NAME = Name
        if (!DATABASE_NAME.endsWith(".db")) {
            DATABASE_NAME += ".db";
        }
        DATABASE_NAME = DATABASE_NAME.replace(" ", "_");
        return this
    }

    fun version(Version: Int): CreateDatabase {
        DATABASE_VERSION = Version
        return this
    }

    fun table(Name: String): CreateDatabase {
        if (COLUMNS != "") {
            this.save()
        }
        TABLE_NAME = Name
        return this
    }

    fun column(Name: String, DInfo: String): CreateDatabase {
        COLUMNS += "$Name $DInfo,"
        return this
    }

    fun onUpgrade(ConUpgrade: String) {
        CustomOnUpgradeTabels.add(ConUpgrade)
    }

    private fun save() {
        COLUMNS = COLUMNS.substring(0, COLUMNS.length - 1)
        OnCreateTabels.add("CREATE TABLE $TABLE_NAME ($COLUMNS);")
        OnUpgrade.add("DROP TABLE IF EXISTS $TABLE_NAME;")
        COLUMNS = ""
    }

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
                upgradeTables(db, OnUpgrade)
                db.version = DATABASE_VERSION
            } else {
                upgradeTables(db, OnUpgrade)
                db.version = DATABASE_VERSION
            }
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



