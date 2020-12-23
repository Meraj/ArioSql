# SqliteDatabaseHelper
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
	        implementation 'com.github.MerajV:SqliteDatabaseHelperKotlin:-SNAPSHOT'
	}
```

## how use it ?
### Create Database
for creating a database simply use CreateDatabase Class ,see below :
```kotlin
        val database = CreateDatabase(context)
        database.database("DbName") // set Database name
        database.table("TableName") // set Table Name
        database.column("id","INTEGER PRIMARY KEY AUTOINCREMENT") // Add Column
        database.column("text","VARCHAR (255)") // Add Column
        database.init()
```

### Build Queries
