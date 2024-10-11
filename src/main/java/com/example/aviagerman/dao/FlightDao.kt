package com.example.aviagerman.dao

import android.util.Log
import androidx.room.*
import com.example.aviagerman.entity.Flight

@Dao
interface FlightDao {
    @Insert
    suspend fun insertFlight(flight: Flight)

    @Query("SELECT * FROM flights")
    suspend fun getAllFlights(): List<Flight>

    @Query("SELECT * FROM flights WHERE id = :flightId")
    suspend fun getFlightById(flightId: Int): Flight?

    @Delete
    suspend fun deleteFlight(flight: Flight)
}
