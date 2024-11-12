package com.programmersbox.contentprovidertest

import android.database.Cursor
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Database(
    entities = [Person::class],
    version = 1,
    exportSchema = true,
    autoMigrations = []
)

abstract class AppDatabase : RoomDatabase() {
    abstract fun getDao(): PersonDao
}

@Dao
interface PersonDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: Person): Long

    @Delete
    suspend fun delete(item: Person): Int

    @Query("DELETE FROM Person WHERE name=:name")
    suspend fun deleteByName(name: String): Int

    @Update
    suspend fun update(item: Person): Int

    @Query("SELECT * FROM Person")
    suspend fun getPeople(): List<Person>

    @Query("SELECT * FROM Person")
    fun getPeopleFlow(): Flow<List<Person>>

    @Query(value = "SELECT * FROM Person")
    fun selectAll(): Cursor

    @Query(value = "SELECT * FROM Person WHERE name=:name")
    fun selectByName(name: String): Cursor
}

@Entity(tableName = "Person")
data class Person(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val age: Int
)
