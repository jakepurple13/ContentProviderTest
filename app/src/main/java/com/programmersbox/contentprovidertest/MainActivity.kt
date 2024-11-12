package com.programmersbox.contentprovidertest

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.BaseColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import com.programmersbox.contentprovidertest.ui.theme.ContentProviderTestTheme
import kotlinx.coroutines.flow.Flow

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = Room
            .databaseBuilder(this, AppDatabase::class.java, "person")
            .build()

        setContent {
            ContentProviderTestTheme {
                val people by db.getDao()
                    .getPeopleFlow()
                    .collectAsStateWithLifecycle(emptyList())

                Scaffold(
                    topBar = { CenterAlignedTopAppBar(title = { Text("People") }) },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    LazyColumn(
                        contentPadding = innerPadding
                    ) {
                        items(people) {
                            ListItem(
                                headlineContent = { Text(it.name) },
                                overlineContent = { Text(it.id.toString()) },
                                leadingContent = { Text(it.age.toString()) }
                            )
                        }
                    }
                }
            }
        }
    }
}
