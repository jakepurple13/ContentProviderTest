package com.programmersbox.contentprovidertest

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns
import androidx.room.Room
import kotlinx.coroutines.runBlocking

private const val PROVIDER = "com.programmersbox.contentprovidertest.PersonProvider"

class CustomContentProvider : ContentProvider() {
    companion object {
        val AUTHORITY = "com.programmersbox.contentprovidertest"
        val AUTHORITY_URI = Uri.parse("content://$AUTHORITY")!!

        object Person : BaseColumns {
            val TABLE_NAME = "person"
            val PERSON_NAME = "$TABLE_NAME/person"
            val DOMAIN_URI = Uri.withAppendedPath(AUTHORITY_URI, TABLE_NAME)
            val TITLE = "title"
        }
    }

    private lateinit var appDatabase: AppDatabase

    // Defines a Data Access Object to perform the database operations
    private lateinit var userDao: PersonDao

    override fun onCreate(): Boolean {

        // Creates a new database object
        appDatabase = Room.databaseBuilder(context!!, AppDatabase::class.java, "person").build()

        // Gets a Data Access Object to perform the database operations
        userDao = appDatabase.getDao()

        return true
    }

    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
        addURI(PROVIDER, Person.TABLE_NAME, 1)
        addURI(PROVIDER, Person.PERSON_NAME, 2)
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        return when (uriMatcher.match(uri)) {
            1 -> {
                return userDao?.selectAll()
            }

            2 -> {
                return userDao?.selectByName(uri.pathSegments[1])
            }

            else -> null
        }
    }

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            1 -> "vnd.android.cursor.dir/$PROVIDER/${Person.TABLE_NAME}"
            2 -> "vnd.android.cursor.item/$PROVIDER/${Person.PERSON_NAME}"
            else -> null
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return when (uriMatcher.match(uri)) {
            1 -> {
                val id = runBlocking {
                    userDao.insert(
                        Person(
                            name = values?.get("name") as String,
                            age = values?.get("age") as Int
                        )
                    )
                }
                ContentUris.withAppendedId(uri, id)
                    .also { context!!.contentResolver.notifyChange(it, null) }
            }

            else -> null
        }
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        return when (uriMatcher.match(uri)) {
            1 -> {
                val count = runBlocking {
                    val array = selectionArgs
                        ?.toList()
                        .orEmpty()
                    val select = selection
                        ?.replace("?", "%s")
                        ?.format(*array.toTypedArray())
                        .orEmpty()
                        .split("&")
                        .associate { it.split("=").let { it.first() to it.last() } }

                    val id = requireNotNull(select["id"]?.toInt()) { "Id is required" }

                    val p = Person(
                        id = id,
                        name = select["name"] ?: "",
                        age = select["age"]?.toInt() ?: 0
                    )

                    println(p)

                    userDao.deleteById(id)
                }
                if (count == 0) 0
                else {
                    context!!.contentResolver.notifyChange(uri, null)
                    count
                }
            }

            else -> 0
        }
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int {
        return when (uriMatcher.match(uri)) {
            1 -> {
                val count = runBlocking {

                    val currentPerson = userDao.getPersonById(
                        requireNotNull(values?.get("id") as Int) { "Id required" }
                    )

                    userDao.update(
                        currentPerson.copy(
                            name = (values.get("name") as? String) ?: currentPerson.name,
                            age = (values.get("age") as? Int) ?: currentPerson.age
                        )
                    )
                }
                if (count == 0) 0
                else {
                    context!!.contentResolver.notifyChange(uri, null)
                    count
                }
            }

            else -> 0
        }
    }
}