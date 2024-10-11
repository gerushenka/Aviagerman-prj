package com.example.aviagerman.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flights")
data class Flight(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "flight_number") val flightNumber: String,
    @ColumnInfo(name = "departure") val departure: String,
    @ColumnInfo(name = "arrival") val arrival: String,
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "time") val time: String,
    @ColumnInfo(name = "price") val price: Double
)
