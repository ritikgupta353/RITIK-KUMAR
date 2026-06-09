package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val username: String,
    val email: String,
    val passwordPlain: String, // Plain-text for simpler demo validation, secure for local database sandbox
    val role: String, // "user" or "admin"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_qrs")
data class SavedQrEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String, // associated creator (or "anonymous" if loaded while not logged in)
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String, // e.g. "URL Link", "Plain Text", "Phone Number", "Wi-Fi", "UPI Payment", "Image Customization"
    val content: String, // raw QR text/data representation
    val foreColorHex: Int,
    val backColorHex: Int,
    val roundness: Float
)
