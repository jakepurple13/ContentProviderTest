package com.programmersbox.contentproviderread

import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.BaseColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.contentproviderread.ui.theme.ContentProviderTestTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    private val personHelper by lazy { PersonResolverHelper(this) }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ContentProviderTestTheme {

                val people by personHelper
                    .listenToPeople()
                    .collectAsStateWithLifecycle(emptyList())

                val scope = rememberCoroutineScope()

                Scaffold(
                    topBar = { CenterAlignedTopAppBar(title = { Text("People Modify") }) },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    LazyColumn(
                        contentPadding = innerPadding,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Button(
                                onClick = {
                                    scope.launch {
                                        personHelper.insertDomain(
                                            Person(
                                                name = "Jacob" + Random.nextInt(10, 100),
                                                age = Random.nextInt(10, 100)
                                            )
                                        )
                                    }
                                }
                            ) { Text("Add Person") }
                        }

                        items(people) {
                            ListItem(
                                headlineContent = { Text(it.name) },
                                overlineContent = { Text(it.id.toString()) },
                                leadingContent = { Text(it.age.toString()) },
                                trailingContent = {
                                    Column {
                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    personHelper.update(
                                                        it.copy(
                                                            name = "Jacob" + Random.nextInt(
                                                                10,
                                                                100
                                                            ),
                                                            age = Random.nextInt(10, 100)
                                                        )
                                                    )
                                                }
                                            }
                                        ) { Icon(Icons.Default.ThumbUp, null) }
                                        IconButton(
                                            onClick = { scope.launch { personHelper.delete(it) } }
                                        ) { Icon(Icons.Default.Delete, null) }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

object CustomContentProviderTest {
    val AUTHORITY = "com.programmersbox.contentprovidertest.PersonProvider"
    val AUTHORITY_URI = Uri.parse("content://$AUTHORITY")!!

    object Person : BaseColumns {
        val TABLE_NAME = "person"
        val PERSON_NAME = "$TABLE_NAME/person"
        val DOMAIN_URI = Uri.withAppendedPath(AUTHORITY_URI, TABLE_NAME)
        val TITLE = "title"
    }
}

class PersonResolverHelper(private val context: Context) {

    private val personHolder = MutableStateFlow<List<Person>>(emptyList())

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            println("onChange")
            runBlocking { personHolder.emit(getAllDomains()) }
        }
    }

    /*init {
        context.contentResolver.registerContentObserver(
            CustomContentProviderTest.Person.DOMAIN_URI,
            true,
            observer
        )
    }*/

    fun listenToPeople(): Flow<List<Person>> = personHolder
        .onStart {
            emit(getAllDomains())
            startListening()
        }
        .onCompletion { cleanup() }
        .onEach { println(it) }

    fun startListening() {
        context.contentResolver.registerContentObserver(
            CustomContentProviderTest.Person.DOMAIN_URI,
            true,
            observer
        )
    }

    fun cleanup() {
        context.contentResolver.unregisterContentObserver(observer)
    }

    suspend fun getAllDomains(): List<Person> {
        return withContext(Dispatchers.IO) {
            val titles = mutableListOf<Person>()

            val cursor = context.contentResolver!!.query(
                CustomContentProviderTest.Person.DOMAIN_URI,
                null,
                null,
                null,
                null
            )

            cursor?.moveToFirst()
            while (cursor?.moveToNext() == true) {
                titles.add(
                    cursor.let {
                        val titleIndex = it.getColumnIndex("name")
                        val idIndex = it.getColumnIndex("id")
                        val ageIndex = it.getColumnIndex("age")
                        Person(
                            id = it.getInt(idIndex),
                            name = it.getString(titleIndex),
                            age = it.getInt(ageIndex)
                        )
                    }
                )
            }

            cursor?.close()
            return@withContext titles
        }
    }

    suspend fun insertDomain(domainData: Person) {
        withContext(Dispatchers.IO) {
            context.contentResolver!!.insert(
                CustomContentProviderTest.Person.DOMAIN_URI,
                ContentValues().apply {
                    put("name", domainData.name)
                    put("age", domainData.age)
                },
            )
        }
    }

    suspend fun delete(person: Person) {
        withContext(Dispatchers.IO) {
            val d = context.contentResolver!!.delete(
                CustomContentProviderTest.Person.DOMAIN_URI,
                "name=?&age=?&id=?",
                arrayOf(person.name, person.age.toString(), person.id.toString())
            )

            println(d)
        }
    }

    suspend fun update(person: Person) {
        withContext(Dispatchers.IO) {
            val d = context.contentResolver!!.update(
                CustomContentProviderTest.Person.DOMAIN_URI,
                ContentValues().apply {
                    put("name", person.name)
                    put("age", person.age)
                    put("id", person.id)
                },
                "name=?&age=?&id=?",
                arrayOf(person.name, person.age.toString(), person.id.toString())
            )

            println(d)
        }
    }
}

data class Person(
    val id: Int = 0,
    val name: String,
    val age: Int,
)
