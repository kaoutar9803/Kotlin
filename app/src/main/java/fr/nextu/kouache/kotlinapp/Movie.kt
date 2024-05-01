package fr.nextu.kouache.kotlinapp

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "movie")
data class Movie(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "title") val title: String,

    ){
    @ColumnInfo(name = "poster", defaultValue = "")
    var poster: String? =  ""
        set(value) { field = value ?: "" }
}

data class Movies(val movies: List<Movie>)