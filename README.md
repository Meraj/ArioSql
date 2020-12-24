# SqliteDatabaseHelper
this is a simple library that helps you to build databases and build queries in your java/kotlin project

> this is a beta version
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
	        implementation 'com.github.MerajV:SqliteDatabaseHelperKotlin:-SNAPSHOT'
	}
```

## how use it ?
### Create Database
for creating a database simply use CreateDatabase Class in your MainActivity, see below :
```kotlin
       val database = CreateDatabase(applicationContext)
        database
	.version(1) // Database Version (For Upgrading Database in future) 
	.database("myNewDatabase") // Database Name
        .table("table_one") // New Table
        .column("id","INTEGER PRIMARY KEY AUTOINCREMENT") // table_one column
        .column("my_number","BIGINT (256)") // table_one column
        .save() // Save table_one Data
        .table("table_two") // New Table
        .column("id","INTEGER PRIMARY KEY AUTOINCREMENT") // table_two column
        .column("text","VARCHAR (255)") // table_two column
        .save() // Save table_two Data
        .init() // initialize Database
```

### Build Queries
for get data from database you can use DatabaseHelper Class, see below 
```kotlin
   DatabaseHelper(applicationContext, // Context
  		  "myNewDatabase" // Database Name
	 	 )
        .table("testTbname") // Laod Table Name
	.first()
```
###### Queries :
* select
* where
* orWhere
* andWhere
* whereRange
* limit
* orderBy
	* first // for get a single row (return Cursor)
* get // for get rows (return Cursor)
* insert // insert data
