package fr.nextu.kouache.kotlinapp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDAO {
    @Query("SELECT * FROM movie")
    fun getAll(): List<Movie>

    @Query("SELECT * FROM movie")
    fun getFlowData(): Flow<List<Movie>>

    @Query("SELECT * FROM movie WHERE id IN (:id)")
    fun loadAllByIds(id: IntArray): List<Movie>

    @Query("SELECT * FROM movie WHERE title LIKE :title LIMIT 1")
    fun findByTitle(title: String): Movie

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg movies: Movie): List<Long>

    @Delete
    fun delete(movie: Movie)


}
