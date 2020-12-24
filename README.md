# Android Database
this is a simple library that helps you to build databases and build queries in your java/kotlin project

> Database Helper class is in progress
## install
##### step 1
Add it in your root build.gradle at the end of repositories:
```gradle
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```
##### step 2
```gradle
dependencies {
	        implementation 'com.github.MerajV:AndroidDatabase:-SNAPSHOT'
	}
```

## how use it ?
### Create Database
for creating a database simply use CreateDatabase Class in your MainActivity, see below :
```kotlin
        val database = CreateDatabase(applicationContext)
        database
                .version(3) // Database Version (For Upgrading Database in future)
                .database("myNewDatabase") // Database Name
                .table("table_one") // New Table
                   .column("id","INTEGER PRIMARY KEY AUTOINCREMENT") // table_one column
                   .column("contact_number","BIGINT (256)") // table_one column
                   .column("contact_name","VARCHAR (255)") // table_one column
                .table("table_two") // New Table
                    .column("id","INTEGER PRIMARY KEY AUTOINCREMENT") // table_two column
                    .column("text_message","VARCHAR (255)") // table_two column
                .init() // initialize Database
```

### Build Queries
you can use this library to create and run database queries in fluent way

#### database Queries
first of all lets initilize DatabaseHelper Class 
```kotlin
   val dbHelper = DatabaseHelper(applicationContext,"myNewDatabase") // Load Database  
```
after that we need to set table name that we are going to run queries for it 
```kotlin
    dbHelper.table("table_one") // Set Table   
```
bingo ,lets create some queries now 
##### get All Rows From Table
get function let you to retrieve the results of the query :
```kotlin
    dbHelper.get() // get All Rows
```
get() return Cursor

##### Retrieving A Single Row
first() function let you to retrieve the first index of the table :
```kotlin
    dbHelper.first() // get All Rows
```
first() return Cursor
##### Select Column/Columns
```kotlin
 dbHelper.select("contact_name").first() // select single column 
 dbHelper.select(arrayOf("contact_name","contact_number")).first()  // Select multiple Columns
```
##### Use Where Query / Search in Table
```kotlin
    dbHelper.where("contact_name","jafar").first() 
```
for AndWhere :
```kotlin
 dbHelper.where("contact_name","jafar").where("contact_number","09120000000").first() 
 ```
 for OrWhere :
 ```kotlin
  dbHelper.where("contact_name","jafar").orWhere("contact_name","maryam").first() 
```
for whereBetween:
 ```kotlin
 dbHelper.whereBetween("id","0","2").first() // Where Between Query
 ```
 for whereNotBetween:
 ```kotlin:
         dbHelper.whereNotBetween("id","0","2").first() // Where Not Between Query
 ```
 ##### Order By / Sort
 for ordering rows and then get data you can use :
 ```kotlin
         dbHelper.orderBy("id","ASC").first() // sort the result set based on id column in ASC order
	 dbHelper.orderBy("id","DESC").first() // sort the result set based on id column in DESC order
 ```
 ##### Limit
 ```kotlin
 dbHelper.limit(2).get() // limit
 ```
 ##### Count Rows 
 for counting rows you can use count() function
 ```kotlin
         dbHelper.count() // rows Count
	 dbHelper.where("contact_name","jafar").count() // Search in database and count
 	 dbHelper.whereBetween("id","0","2").count() // Search in database and count
 ```
count() function return Int
## Exist/Doesn't exist
for searching in database for a row ,and check if its exist or not ,you can use :
```kotlin
        dbHelper.where("id","1").exists() // return true if exist
        dbHelper.where("id","1").doesntExist() // return true if DOES NOT exist
```

###### Queries :
* [select](#Select-Column/Columns)
* where
* orWhere
* whereBetween
* whereNotBetween
* limit
* orderBy
* first // for get a single row (return Cursor)
* get // for get rows (return Cursor)
* insert // insert data
* count // count rows
* exists // check if a row exist
* doesntExist // check if a row does not exist


### Cursor in Android
```kotlin
val cursor = dbHelper.get()
 if(cursor.moveToNext()){
            text = cursor.getString(cursor.getColumnIndexOrThrow("contact_name"))
        }
```
