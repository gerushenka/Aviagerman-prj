package com.example.aviagerman.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.aviagerman.dao.UserDao
import com.example.aviagerman.dao.FlightDao
import com.example.aviagerman.dao.BookingDao
import com.example.aviagerman.entity.User
import com.example.aviagerman.entity.Flight
import com.example.aviagerman.entity.Booking

@Database(entities = [User::class, Flight::class, Booking::class], version = 3)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun flightDao(): FlightDao
    abstract fun bookingDao(): BookingDao
}
