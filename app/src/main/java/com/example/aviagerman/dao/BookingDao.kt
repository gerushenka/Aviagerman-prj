package com.example.aviagerman.dao

import androidx.room.*
import com.example.aviagerman.entity.Booking

@Dao
interface BookingDao {
    @Insert
    suspend fun insertBooking(booking: Booking)

    @Query("SELECT * FROM bookings WHERE user_id = :userId")
    suspend fun getBookingsByUserId(userId: Int): List<Booking>

    @Query("SELECT * FROM bookings WHERE flight_id = :flightId")
    suspend fun getBookingsByFlightId(flightId: Int): List<Booking>

    @Delete
    suspend fun deleteBooking(booking: Booking)

    @Query("DELETE FROM bookings WHERE id = :id")
    suspend fun deleteBookingById(id: Int)

    @Query("UPDATE bookings SET status = :status WHERE id = :bookingId")
    suspend fun updateBookingStatus(bookingId: Int, status: String)
}
