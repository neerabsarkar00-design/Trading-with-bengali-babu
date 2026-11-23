package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class UserRole {
    ADMIN,
    STUDENT
}

enum class AccountStatus {
    ACTIVE,
    SUSPENDED
}

enum class TradeDirection {
    BUY,
    SELL
}

enum class TradeResult {
    WIN,
    LOSS,
    BREAK_EVEN
}

enum class TradeTimeframe(val label: String) {
    M1("M1"),
    M5("M5"),
    M15("M15"),
    M30("M30"),
    H1("H1"),
    H4("H4"),
    D1("D1")
}

enum class ReviewType(val label: String) {
    DAILY_PSYCHOLOGY("Daily Psychology"),
    WEEKLY_REVIEW("Weekly Self-Review"),
    MONTHLY_REVIEW("Monthly Review")
}

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val fullName: String,
    val email: String,
    val passwordHash: String,
    val passwordSalt: String,
    val role: String = UserRole.STUDENT.name,
    val status: String = AccountStatus.ACTIVE.name,
    val phone: String = "",
    val tradingStyle: String = "Price Action",
    val adminNotes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val roleEnum: UserRole
        get() = UserRole.entries.firstOrNull { it.name == role } ?: UserRole.STUDENT

    val statusEnum: AccountStatus
        get() = AccountStatus.entries.firstOrNull { it.name == status } ?: AccountStatus.ACTIVE
}

@Entity(
    tableName = "trades",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["tradeDateMillis"]),
        Index(value = ["tradeCode"], unique = true)
    ]
)
data class TradeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val tradeCode: String,
    val userId: Long,
    val userName: String,
    val tradeDateMillis: Long,
    val instrument: String,
    val direction: String,
    val timeframe: String,
    val entryPrice: Double,
    val exitPrice: Double,
    val stopLoss: Double,
    val takeProfit: Double,
    val lotSize: Double,
    val riskAmount: Double,
    val profitLoss: Double,
    val result: String,
    val strategy: String,
    val entryReason: String,
    val exitReason: String,
    val mistakes: String,
    val lessonsLearned: String,
    val screenshotPath: String = "",
    val notes: String = "",
    val adminFeedback: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val directionEnum: TradeDirection
        get() = TradeDirection.entries.firstOrNull { it.name == direction } ?: TradeDirection.BUY

    val resultEnum: TradeResult
        get() = TradeResult.entries.firstOrNull { it.name == result } ?: TradeResult.BREAK_EVEN

    val riskRewardRatio: Double
        get() {
            val riskDist = kotlin.math.abs(entryPrice - stopLoss)
            if (riskDist <= 0.0) return 0.0
            val rewardDist = if (directionEnum == TradeDirection.BUY) {
                exitPrice - entryPrice
            } else {
                entryPrice - exitPrice
            }
            return kotlin.math.abs(rewardDist) / riskDist
        }

    val plannedRiskRewardRatio: Double
        get() {
            val riskDist = kotlin.math.abs(entryPrice - stopLoss)
            val targetDist = kotlin.math.abs(takeProfit - entryPrice)
            return if (riskDist > 0.0 && targetDist > 0.0) targetDist / riskDist else 0.0
        }
}

@Entity(
    tableName = "discipline_profiles",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DisciplineProfileEntity(
    @PrimaryKey val userId: Long,
    val dailyProfitGoal: Double = 200.0,
    val dailyMaxLossLimit: Double = 100.0,
    val maxTradesPerDay: Int = 3,
    val maxRiskPerTrade: Double = 50.0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "discipline_reviews",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"]), Index(value = ["reviewDateMillis"])]
)
data class DisciplineReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val userId: Long,
    val reviewType: String,
    val reviewDateMillis: Long,
    val moodTag: String,
    val followedPlan: Boolean = true,
    val usedStopLoss: Boolean = true,
    val respectedMaxLoss: Boolean = true,
    val mistakesSummary: String = "",
    val psychologyNotes: String = "",
    val actionPlan: String = "",
    val adminComment: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val reviewTypeEnum: ReviewType
        get() = ReviewType.entries.firstOrNull { it.name == reviewType } ?: ReviewType.DAILY_PSYCHOLOGY
}
