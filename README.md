# Android Query Builder
this is a simple library that helps you to build databases and build queries in your java/kotlin project

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
	        	        implementation 'com.github.MerajV:AndroidQueryBuilder:0.12'
             }
```

## how to use it ?
## Create Database
for creating a database simply use CreateDatabase Class in your MainActivity, see below :
```kotlin
        val database = CreateDatabase(applicationContext)
        database
                .version(1) // Database Version (For Upgrading Database in future)
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
---
## Build Queries
you can use this library to create and run database queries in fluent way

### database Queries
first of all lets initilize QueryBuilder Class 
```kotlin
   val queryBuilder = QueryBuilder(applicationContext,"myNewDatabase") // Load Database  
```
after that we need to set table name that we are going to run queries for it 
```kotlin
    queryBuilder.table("table_one") // Set Table   
```
bingo ,lets create some queries now 
###### Queries :
* [select](#select-columncolumns)
* [where / orWhere / where Between / where Not Between](#use-where-query--search-in-table)
* [limit](#limit)
* [order by](#order-by--sort)
* [first](#retrieving-a-single-row) -> for get a single row (return Cursor)
* [get](#get-all-rows-from-table) -> for get rows (return Cursor)
* [insert](#Insert) -> insert data
* [count](#count-rows) -> count rows
* [exists / doesntExist](#existdoesnt-exist) -> check if a row exists or not


#### Insert
for insert use :
```kotlin
queryBuilder.insert(arrayOf("contact_number","contact_name"), arrayOf("09120000000","Jafar")) // insert data
```
insert(Array of column Names, Array of Values)

#### get All Rows From Table
get function let you to retrieve the results of the query :
```kotlin
queryBuilder.get() // get All Rows
```
get() return Cursor

#### Retrieving A Single Row
first() function let you to retrieve the first index of the table :
```kotlin
queryBuilder.first() // get All Rows
```
first() return Cursor
#### Select Column/Columns
```kotlin
queryBuilder.select("contact_name").first() // select single column 

queryBuilder.select(arrayOf("contact_name","contact_number")).first()  // Select multiple Columns
```
select(Column Name String) \
or \
select(Column Names Array)

#### Use Where Query / Search in Table
```kotlin
queryBuilder.where("contact_name","jafar").first() 
```
where(Column Name String, Value String) 

for AndWhere :
```kotlin
 queryBuilder.where("contact_name","jafar").where("contact_number","09120000000").first() 
 ```
 \
 for OrWhere :
 ```kotlin
  queryBuilder.where("contact_name","jafar").orWhere("contact_name","maryam").first() 
```
\
for whereBetween:
 ```kotlin
 queryBuilder.whereBetween("id","0","2").first() // Where Between Query
 ```
  whereBetween(Column Name String ,From String ,To String)


 for whereNotBetween:
 ```kotlin:
queryBuilder.whereNotBetween("id","0","2").first() // Where Not Between Query
 ```
 whereNotBetween(Column Name String ,From String ,To String)
 #### Order By / Sort
 for ordering rows and then get data you can use :
 ```kotlin
         queryBuilder.orderBy("id","ASC").first() // sort the result set based on id column in ASC order
	 queryBuilder.orderBy("id","DESC").first() // sort the result set based on id column in DESC order
 ```
 orderBy(Column Name String, Order Type (Default is DESC))
 #### Limit
 ```kotlin
 queryBuilder.limit(2).get() // limit
 ```
  limit(Limit Count Int)        
 #### Count Rows 
 for counting rows you can use count() function
 ```kotlin
         queryBuilder.count() // rows Count
	 queryBuilder.where("contact_name","jafar").count() // Search in database and count
 	 queryBuilder.whereBetween("id","0","2").count() // Search in database and count
 ```
count() function return Int
#### Exist/Doesn't exist
for searching in database for a row ,and check if its exist or not ,you can use :
```kotlin
        queryBuilder.where("id","1").exists() // return true if exist
        queryBuilder.where("id","1").doesntExist() // return true if DOES NOT exist
```
#### Close
for close the connection to the database :
```kotlin
queryBuilder.close()
```

### Cursor in Android
```kotlin
val cursor = queryBuilder.get()
 if(cursor.moveToNext()){
            text = cursor.getString(cursor.getColumnIndexOrThrow("contact_name"))
        }
```
