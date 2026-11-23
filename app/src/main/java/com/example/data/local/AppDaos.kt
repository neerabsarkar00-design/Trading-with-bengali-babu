package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT COUNT(*) FROM users WHERE role = 'ADMIN'")
    fun observeAdminCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM users WHERE role = 'ADMIN'")
    suspend fun getAdminCount(): Int

    @Query("SELECT * FROM users WHERE email = :normalizedEmail LIMIT 1")
    suspend fun findByEmail(normalizedEmail: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun findById(userId: Long): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun observeUserById(userId: Long): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE role = 'STUDENT' ORDER BY createdAt DESC")
    fun observeAllStudents(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY role ASC, createdAt DESC")
    suspend fun getAllUsersOnce(): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: Long)
}

@Dao
interface TradeDao {
    @Query("SELECT * FROM trades WHERE userId = :userId ORDER BY tradeDateMillis DESC, id DESC")
    fun observeTradesForUser(userId: Long): Flow<List<TradeEntity>>

    @Query("SELECT * FROM trades ORDER BY tradeDateMillis DESC, id DESC")
    fun observeAllTrades(): Flow<List<TradeEntity>>

    @Query("SELECT * FROM trades WHERE userId = :userId ORDER BY tradeDateMillis DESC, id DESC")
    suspend fun getTradesForUserOnce(userId: Long): List<TradeEntity>

    @Query("SELECT * FROM trades ORDER BY tradeDateMillis DESC, id DESC")
    suspend fun getAllTradesOnce(): List<TradeEntity>

    @Query("SELECT * FROM trades WHERE id = :tradeId LIMIT 1")
    suspend fun getTradeById(tradeId: Long): TradeEntity?

    @Query("SELECT COUNT(*) FROM trades")
    suspend fun getTotalTradeCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrade(trade: TradeEntity): Long

    @Update
    suspend fun updateTrade(trade: TradeEntity)

    @Query("DELETE FROM trades WHERE id = :tradeId")
    suspend fun deleteTradeById(tradeId: Long)
}

@Dao
interface DisciplineDao {
    @Query("SELECT * FROM discipline_profiles WHERE userId = :userId LIMIT 1")
    fun observeProfile(userId: Long): Flow<DisciplineProfileEntity?>

    @Query("SELECT * FROM discipline_profiles WHERE userId = :userId LIMIT 1")
    suspend fun getProfileOnce(userId: Long): DisciplineProfileEntity?

    @Query("SELECT * FROM discipline_profiles")
    fun observeAllProfiles(): Flow<List<DisciplineProfileEntity>>

    @Query("SELECT * FROM discipline_profiles")
    suspend fun getAllProfilesOnce(): List<DisciplineProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: DisciplineProfileEntity)

    @Query("SELECT * FROM discipline_reviews WHERE userId = :userId ORDER BY reviewDateMillis DESC, id DESC")
    fun observeReviewsForUser(userId: Long): Flow<List<DisciplineReviewEntity>>

    @Query("SELECT * FROM discipline_reviews ORDER BY reviewDateMillis DESC, id DESC")
    fun observeAllReviews(): Flow<List<DisciplineReviewEntity>>

    @Query("SELECT * FROM discipline_reviews")
    suspend fun getAllReviewsOnce(): List<DisciplineReviewEntity>

    @Query("SELECT * FROM discipline_reviews WHERE id = :reviewId LIMIT 1")
    suspend fun getReviewById(reviewId: Long): DisciplineReviewEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: DisciplineReviewEntity): Long

    @Update
    suspend fun updateReview(review: DisciplineReviewEntity)

    @Query("DELETE FROM discipline_reviews WHERE id = :reviewId")
    suspend fun deleteReviewById(reviewId: Long)
}
