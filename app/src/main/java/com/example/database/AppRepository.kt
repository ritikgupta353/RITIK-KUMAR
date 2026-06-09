package com.example.database

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class AppRepository(private val database: AppDatabase) {
    private val userDao = database.userDao()
    private val savedQrDao = database.savedQrDao()

    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsersFlow()
    val allSavedQrs: Flow<List<SavedQrEntity>> = savedQrDao.getAllSavedQrsFlow()
    val usersCount: Flow<Int> = userDao.getUsersCountFlow()
    val qrsCount: Flow<Int> = savedQrDao.getQrsCountFlow()

    fun getSavedQrsByUser(username: String): Flow<List<SavedQrEntity>> {
        return savedQrDao.getSavedQrsByUserFlow(username)
    }

    suspend fun getUserByUsername(username: String): UserEntity? {
        return userDao.getUserByUsername(username)
    }

    suspend fun insertUser(user: UserEntity) {
        userDao.insertUser(user)
    }

    suspend fun deleteUser(username: String) {
        userDao.deleteByUsername(username)
    }

    suspend fun insertSavedQr(qr: SavedQrEntity) {
        savedQrDao.insertSavedQr(qr)
    }

    suspend fun deleteSavedQr(id: Int) {
        savedQrDao.deleteById(id)
    }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val hasAdmin = userDao.getUserByUsername("admin")
                if (hasAdmin == null) {
                    userDao.insertUser(
                        UserEntity(
                            username = "admin",
                            email = "admin@forge.com",
                            passwordPlain = "admin123",
                            role = "admin"
                        )
                    )
                }
                val hasUser = userDao.getUserByUsername("user")
                if (hasUser == null) {
                    userDao.insertUser(
                        UserEntity(
                            username = "user",
                            email = "user@forge.com",
                            passwordPlain = "user123",
                            role = "user"
                        )
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("AppRepository", "Failed to insert initial demo credentials", e)
            }
        }
    }
}
