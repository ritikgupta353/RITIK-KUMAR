package com.example.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("DELETE FROM users WHERE username = :username")
    suspend fun deleteByUsername(username: String)

    @Query("SELECT COUNT(*) FROM users")
    fun getUsersCountFlow(): Flow<Int>
}

@Dao
interface SavedQrDao {
    @Query("SELECT * FROM saved_qrs ORDER BY timestamp DESC")
    fun getAllSavedQrsFlow(): Flow<List<SavedQrEntity>>

    @Query("SELECT * FROM saved_qrs WHERE username = :username ORDER BY timestamp DESC")
    fun getSavedQrsByUserFlow(username: String): Flow<List<SavedQrEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedQr(qr: SavedQrEntity)

    @Delete
    suspend fun deleteSavedQr(qr: SavedQrEntity)

    @Query("DELETE FROM saved_qrs WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT COUNT(*) FROM saved_qrs")
    fun getQrsCountFlow(): Flow<Int>
}
