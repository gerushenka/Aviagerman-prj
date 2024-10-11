package com.example.aviagerman.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "nickname") val nickname: String,
    @ColumnInfo(name = "password") val password: String,
    @ColumnInfo(name = "role") val role: String
)
