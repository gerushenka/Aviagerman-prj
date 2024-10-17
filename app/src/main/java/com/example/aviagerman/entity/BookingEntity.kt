package com.example.aviagerman.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookings")
data class Booking(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "user_id") val userId: Int,
    @ColumnInfo(name = "flight_id") val flightId: Int,
    @ColumnInfo(name = "booking_date") val bookingDate: String,
    @ColumnInfo(name = "status") val status: String
)
