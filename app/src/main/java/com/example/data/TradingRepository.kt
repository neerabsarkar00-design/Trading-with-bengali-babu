package com.example.data

import android.content.Context
import android.net.Uri
import com.example.data.local.AccountStatus
import com.example.data.local.AppDatabase
import com.example.data.local.DisciplineProfileEntity
import com.example.data.local.DisciplineReviewEntity
import com.example.data.local.TradeEntity
import com.example.data.local.UserEntity
import com.example.data.local.UserRole
import com.example.security.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class SessionContext(
    val userId: Long,
    val fullName: String,
    val email: String,
    val role: UserRole
)

class TradingRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val sessionManager: SessionManager
) {
    private val userDao = db.userDao()
    private val tradeDao = db.tradeDao()
    private val disciplineDao = db.disciplineDao()

    fun observeAdminExists(): Flow<Int> = userDao.observeAdminCount()

    fun observeUser(userId: Long): Flow<UserEntity?> = userDao.observeUserById(userId)

    fun observeAllStudents(session: SessionContext): Flow<List<UserEntity>> {
        if (session.role != UserRole.ADMIN) return emptyFlow()
        return userDao.observeAllStudents()
    }

    fun observeTradesForUser(session: SessionContext, targetUserId: Long): Flow<List<TradeEntity>> {
        if (session.role != UserRole.ADMIN && session.userId != targetUserId) {
            return emptyFlow()
        }
        return tradeDao.observeTradesForUser(targetUserId)
    }

    fun observeAllTradesForAdmin(session: SessionContext): Flow<List<TradeEntity>> {
        if (session.role != UserRole.ADMIN) return emptyFlow()
        return tradeDao.observeAllTrades()
    }

    fun observeDisciplineProfile(session: SessionContext, targetUserId: Long): Flow<DisciplineProfileEntity?> {
        if (session.role != UserRole.ADMIN && session.userId != targetUserId) {
            return emptyFlow()
        }
        return disciplineDao.observeProfile(targetUserId)
    }

    fun observeDisciplineReviews(session: SessionContext, targetUserId: Long): Flow<List<DisciplineReviewEntity>> {
        if (session.role != UserRole.ADMIN && session.userId != targetUserId) {
            return emptyFlow()
        }
        return disciplineDao.observeReviewsForUser(targetUserId)
    }

    fun observeAllReviewsForAdmin(session: SessionContext): Flow<List<DisciplineReviewEntity>> {
        if (session.role != UserRole.ADMIN) return emptyFlow()
        return disciplineDao.observeAllReviews()
    }

    fun observeAllDisciplineProfilesForAdmin(session: SessionContext): Flow<List<DisciplineProfileEntity>> {
        if (session.role != UserRole.ADMIN) return emptyFlow()
        return disciplineDao.observeAllProfiles()
    }

    suspend fun getValidatedSession(userId: Long): SessionContext? = withContext(Dispatchers.IO) {
        val user = userDao.findById(userId) ?: return@withContext null
        if (user.statusEnum == AccountStatus.SUSPENDED) {
            sessionManager.clearSession()
            return@withContext null
        }
        SessionContext(
            userId = user.id,
            fullName = user.fullName,
            email = user.email,
            role = user.roleEnum
        )
    }

    suspend fun claimInitialAdminAccount(
        fullName: String,
        email: String,
        password: String
    ): Result<SessionContext> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanName = fullName.trim()
            val normEmail = SecurityUtils.normalizeEmail(email)
            require(cleanName.length >= 2) { "Enter your full name (at least 2 characters)." }
            require(SecurityUtils.isValidEmail(normEmail)) { "Enter a valid email address." }
            require(password.length >= 6) { "Password must be at least 6 characters." }
            check(userDao.getAdminCount() == 0) { "Admin account has already been claimed. Please sign in." }

            val salt = SecurityUtils.generateSaltBase64()
            val hash = SecurityUtils.hashPassword(password, salt)
            val now = System.currentTimeMillis()
            val newId = userDao.insertUser(
                UserEntity(
                    fullName = cleanName,
                    email = normEmail,
                    passwordHash = hash,
                    passwordSalt = salt,
                    role = UserRole.ADMIN.name,
                    status = AccountStatus.ACTIVE.name,
                    tradingStyle = "Institutional & Price Action",
                    createdAt = now,
                    updatedAt = now
                )
            )
            disciplineDao.upsertProfile(DisciplineProfileEntity(userId = newId))
            sessionManager.saveSession(newId)
            SessionContext(newId, cleanName, normEmail, UserRole.ADMIN)
        }
    }

    suspend fun signIn(email: String, password: String): Result<SessionContext> = withContext(Dispatchers.IO) {
        runCatching {
            val normEmail = SecurityUtils.normalizeEmail(email)
            require(normEmail.isNotBlank() && password.isNotBlank()) {
                "Email and password are required."
            }
            val user = userDao.findByEmail(normEmail)
                ?: throw IllegalArgumentException("Invalid email or password.")
            if (!SecurityUtils.verifyPassword(password, user.passwordSalt, user.passwordHash)) {
                throw IllegalArgumentException("Invalid email or password.")
            }
            if (user.statusEnum == AccountStatus.SUSPENDED) {
                throw SecurityException("This account is suspended. Contact Bangali Babu Admin.")
            }
            sessionManager.saveSession(user.id)
            SessionContext(
                userId = user.id,
                fullName = user.fullName,
                email = user.email,
                role = user.roleEnum
            )
        }
    }

    suspend fun registerStudent(
        fullName: String,
        email: String,
        password: String,
        phone: String,
        tradingStyle: String
    ): Result<SessionContext> = withContext(Dispatchers.IO) {
        runCatching {
            check(userDao.getAdminCount() > 0) { "Admin setup must be completed first." }
            val cleanName = fullName.trim()
            val normEmail = SecurityUtils.normalizeEmail(email)
            require(cleanName.length >= 2) { "Full name must be at least 2 characters." }
            require(SecurityUtils.isValidEmail(normEmail)) { "Please enter a valid email address." }
            require(password.length >= 6) { "Password must be at least 6 characters." }
            if (userDao.findByEmail(normEmail) != null) {
                throw IllegalArgumentException("An account with this email already exists.")
            }

            val salt = SecurityUtils.generateSaltBase64()
            val hash = SecurityUtils.hashPassword(password, salt)
            val now = System.currentTimeMillis()
            // Self-registration ALWAYS creates STUDENT role — privilege escalation impossible
            val newId = userDao.insertUser(
                UserEntity(
                    fullName = cleanName,
                    email = normEmail,
                    passwordHash = hash,
                    passwordSalt = salt,
                    role = UserRole.STUDENT.name,
                    status = AccountStatus.ACTIVE.name,
                    phone = phone.trim(),
                    tradingStyle = tradingStyle.trim().ifBlank { "Price Action" },
                    createdAt = now,
                    updatedAt = now
                )
            )
            disciplineDao.upsertProfile(DisciplineProfileEntity(userId = newId))
            sessionManager.saveSession(newId)
            SessionContext(newId, cleanName, normEmail, UserRole.STUDENT)
        }
    }

    suspend fun signOut() {
        sessionManager.clearSession()
    }

    // --- ADMIN STUDENT MANAGEMENT ---

    suspend fun adminCreateStudent(
        session: SessionContext,
        fullName: String,
        email: String,
        password: String,
        phone: String,
        tradingStyle: String,
        adminNotes: String
    ): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            requireAdmin(session)
            val cleanName = fullName.trim()
            val normEmail = SecurityUtils.normalizeEmail(email)
            require(cleanName.length >= 2) { "Student name is required." }
            require(SecurityUtils.isValidEmail(normEmail)) { "Valid email is required." }
            require(password.length >= 6) { "Initial password must be at least 6 characters." }
            if (userDao.findByEmail(normEmail) != null) {
                throw IllegalArgumentException("Email is already registered.")
            }

            val salt = SecurityUtils.generateSaltBase64()
            val hash = SecurityUtils.hashPassword(password, salt)
            val now = System.currentTimeMillis()
            val id = userDao.insertUser(
                UserEntity(
                    fullName = cleanName,
                    email = normEmail,
                    passwordHash = hash,
                    passwordSalt = salt,
                    role = UserRole.STUDENT.name,
                    status = AccountStatus.ACTIVE.name,
                    phone = phone.trim(),
                    tradingStyle = tradingStyle.trim().ifBlank { "Price Action" },
                    adminNotes = adminNotes.trim(),
                    createdAt = now,
                    updatedAt = now
                )
            )
            disciplineDao.upsertProfile(DisciplineProfileEntity(userId = id))
            id
        }
    }

    suspend fun adminUpdateStudent(
        session: SessionContext,
        studentId: Long,
        fullName: String,
        email: String,
        phone: String,
        tradingStyle: String,
        adminNotes: String,
        newPassword: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            requireAdmin(session)
            val existing = userDao.findById(studentId) ?: throw IllegalArgumentException("Student not found.")
            require(existing.roleEnum == UserRole.STUDENT) { "Cannot edit non-student via student editor." }
            val cleanName = fullName.trim()
            val normEmail = SecurityUtils.normalizeEmail(email)
            require(cleanName.length >= 2) { "Name must be at least 2 characters." }
            require(SecurityUtils.isValidEmail(normEmail)) { "Valid email is required." }
            val emailOwner = userDao.findByEmail(normEmail)
            if (emailOwner != null && emailOwner.id != studentId) {
                throw IllegalArgumentException("Another user already uses that email.")
            }

            val (salt, hash) = if (!newPassword.isNullOrBlank()) {
                require(newPassword.length >= 6) { "New password must be at least 6 characters." }
                val newSalt = SecurityUtils.generateSaltBase64()
                newSalt to SecurityUtils.hashPassword(newPassword, newSalt)
            } else {
                existing.passwordSalt to existing.passwordHash
            }

            userDao.updateUser(
                existing.copy(
                    fullName = cleanName,
                    email = normEmail,
                    phone = phone.trim(),
                    tradingStyle = tradingStyle.trim().ifBlank { "Price Action" },
                    adminNotes = adminNotes.trim(),
                    passwordSalt = salt,
                    passwordHash = hash,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun adminToggleStudentStatus(
        session: SessionContext,
        studentId: Long
    ): Result<AccountStatus> = withContext(Dispatchers.IO) {
        runCatching {
            requireAdmin(session)
            val existing = userDao.findById(studentId) ?: throw IllegalArgumentException("Student not found.")
            require(existing.roleEnum == UserRole.STUDENT) { "Cannot suspend admin account." }
            val nextStatus = if (existing.statusEnum == AccountStatus.ACTIVE) {
                AccountStatus.SUSPENDED
            } else {
                AccountStatus.ACTIVE
            }
            userDao.updateUser(
                existing.copy(
                    status = nextStatus.name,
                    updatedAt = System.currentTimeMillis()
                )
            )
            nextStatus
        }
    }

    suspend fun adminDeleteStudent(
        session: SessionContext,
        studentId: Long
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            requireAdmin(session)
            val existing = userDao.findById(studentId) ?: throw IllegalArgumentException("Student not found.")
            require(existing.roleEnum == UserRole.STUDENT) { "Cannot delete admin account." }
            userDao.deleteUserById(studentId)
        }
    }

    // --- TRADE JOURNAL OPERATIONS ---

    suspend fun saveTrade(
        session: SessionContext,
        trade: TradeEntity
    ): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val activeUser = userDao.findById(session.userId)
                ?: throw SecurityException("Session invalid.")
            if (activeUser.statusEnum == AccountStatus.SUSPENDED) {
                throw SecurityException("Suspended accounts cannot add or edit trades.")
            }

            if (trade.id == 0L) {
                // Creating new trade
                val ownerId = if (session.role == UserRole.ADMIN && trade.userId > 0L) {
                    trade.userId
                } else {
                    session.userId
                }
                val ownerUser = userDao.findById(ownerId) ?: activeUser
                val code = trade.tradeCode.ifBlank { generateTradeCode() }
                val now = System.currentTimeMillis()
                tradeDao.insertTrade(
                    trade.copy(
                        tradeCode = code,
                        userId = ownerId,
                        userName = trade.userName.ifBlank { ownerUser.fullName },
                        createdAt = now,
                        updatedAt = now
                    )
                )
            } else {
                // Updating existing trade — verify ownership or Admin role
                val existing = tradeDao.getTradeById(trade.id)
                    ?: throw IllegalArgumentException("Trade record not found.")
                requireOwnerOrAdmin(session, existing.userId)
                tradeDao.updateTrade(
                    trade.copy(
                        id = existing.id,
                        tradeCode = existing.tradeCode,
                        userId = existing.userId,
                        createdAt = existing.createdAt,
                        updatedAt = System.currentTimeMillis(),
                        adminFeedback = if (session.role == UserRole.ADMIN) trade.adminFeedback else existing.adminFeedback
                    )
                )
                existing.id
            }
        }
    }

    suspend fun updateTradeAdminFeedback(
        session: SessionContext,
        tradeId: Long,
        feedback: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            requireAdmin(session)
            val existing = tradeDao.getTradeById(tradeId)
                ?: throw IllegalArgumentException("Trade record not found.")
            tradeDao.updateTrade(
                existing.copy(
                    adminFeedback = feedback.trim(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun deleteTrade(session: SessionContext, tradeId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val existing = tradeDao.getTradeById(tradeId)
                ?: throw IllegalArgumentException("Trade not found.")
            requireOwnerOrAdmin(session, existing.userId)
            if (existing.screenshotPath.isNotBlank()) {
                runCatching { File(existing.screenshotPath).delete() }
            }
            tradeDao.deleteTradeById(tradeId)
        }
    }

    // --- DISCIPLINE & REVIEWS ---

    suspend fun saveDisciplineProfile(
        session: SessionContext,
        profile: DisciplineProfileEntity
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            requireOwnerOrAdmin(session, profile.userId)
            disciplineDao.upsertProfile(
                profile.copy(updatedAt = System.currentTimeMillis())
            )
        }
    }

    suspend fun saveDisciplineReview(
        session: SessionContext,
        review: DisciplineReviewEntity
    ): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            if (review.id == 0L) {
                requireOwnerOrAdmin(session, review.userId)
                disciplineDao.insertReview(review)
            } else {
                val existing = disciplineDao.getReviewById(review.id)
                    ?: throw IllegalArgumentException("Review not found.")
                requireOwnerOrAdmin(session, existing.userId)
                disciplineDao.updateReview(
                    review.copy(
                        id = existing.id,
                        userId = existing.userId,
                        createdAt = existing.createdAt,
                        adminComment = if (session.role == UserRole.ADMIN) review.adminComment else existing.adminComment
                    )
                )
                existing.id
            }
        }
    }

    suspend fun deleteDisciplineReview(session: SessionContext, reviewId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val existing = disciplineDao.getReviewById(reviewId)
                ?: throw IllegalArgumentException("Review not found.")
            requireOwnerOrAdmin(session, existing.userId)
            disciplineDao.deleteReviewById(reviewId)
        }
    }

    // --- MEDIA PERSISTENCE (Photo Picker -> App Internal Storage) ---

    suspend fun copyImageUriToInternalFile(uri: Uri, folder: String, fileNamePrefix: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val targetDir = if (folder.isBlank()) context.filesDir else File(context.filesDir, folder)
                if (!targetDir.exists()) targetDir.mkdirs()
                val outFile = File(targetDir, "${fileNamePrefix}_${UUID.randomUUID().toString().take(8)}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    outFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: throw IllegalStateException("Could not open selected image.")
                outFile.absolutePath
            }
        }

    suspend fun saveCustomBrandLogo(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val outFile = File(context.filesDir, "custom_brand_logo.png")
            context.contentResolver.openInputStream(uri)?.use { input ->
                outFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("Could not read selected logo image.")
            sessionManager.setCustomLogoPath(outFile.absolutePath)
            outFile.absolutePath
        }
    }

    suspend fun resetCustomBrandLogo() {
        File(context.filesDir, "custom_brand_logo.png").delete()
        sessionManager.setCustomLogoPath(null)
    }

    // --- FULL DATABASE BACKUP & RESTORE (ADMIN ONLY) ---

    suspend fun exportFullBackupJson(session: SessionContext): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            requireAdmin(session)
            val users = userDao.getAllUsersOnce()
            val trades = tradeDao.getAllTradesOnce()
            val profiles = disciplineDao.getAllProfilesOnce()
            val reviews = disciplineDao.getAllReviewsOnce()

            val root = JSONObject().apply {
                put("app", "Trading with Bangali Babu")
                put("backupVersion", 1)
                put("exportedAtMillis", System.currentTimeMillis())

                put("users", JSONArray().apply {
                    users.forEach { u ->
                        put(JSONObject().apply {
                            put("id", u.id)
                            put("fullName", u.fullName)
                            put("email", u.email)
                            put("passwordHash", u.passwordHash)
                            put("passwordSalt", u.passwordSalt)
                            put("role", u.role)
                            put("status", u.status)
                            put("phone", u.phone)
                            put("tradingStyle", u.tradingStyle)
                            put("adminNotes", u.adminNotes)
                            put("createdAt", u.createdAt)
                            put("updatedAt", u.updatedAt)
                        })
                    }
                })

                put("trades", JSONArray().apply {
                    trades.forEach { t ->
                        put(JSONObject().apply {
                            put("id", t.id)
                            put("tradeCode", t.tradeCode)
                            put("userId", t.userId)
                            put("userName", t.userName)
                            put("tradeDateMillis", t.tradeDateMillis)
                            put("instrument", t.instrument)
                            put("direction", t.direction)
                            put("timeframe", t.timeframe)
                            put("entryPrice", t.entryPrice)
                            put("exitPrice", t.exitPrice)
                            put("stopLoss", t.stopLoss)
                            put("takeProfit", t.takeProfit)
                            put("lotSize", t.lotSize)
                            put("riskAmount", t.riskAmount)
                            put("profitLoss", t.profitLoss)
                            put("result", t.result)
                            put("strategy", t.strategy)
                            put("entryReason", t.entryReason)
                            put("exitReason", t.exitReason)
                            put("mistakes", t.mistakes)
                            put("lessonsLearned", t.lessonsLearned)
                            put("screenshotPath", t.screenshotPath)
                            put("notes", t.notes)
                            put("adminFeedback", t.adminFeedback)
                            put("createdAt", t.createdAt)
                            put("updatedAt", t.updatedAt)
                        })
                    }
                })

                put("disciplineProfiles", JSONArray().apply {
                    profiles.forEach { p ->
                        put(JSONObject().apply {
                            put("userId", p.userId)
                            put("dailyProfitGoal", p.dailyProfitGoal)
                            put("dailyMaxLossLimit", p.dailyMaxLossLimit)
                            put("maxTradesPerDay", p.maxTradesPerDay)
                            put("maxRiskPerTrade", p.maxRiskPerTrade)
                            put("updatedAt", p.updatedAt)
                        })
                    }
                })

                put("disciplineReviews", JSONArray().apply {
                    reviews.forEach { r ->
                        put(JSONObject().apply {
                            put("id", r.id)
                            put("userId", r.userId)
                            put("reviewType", r.reviewType)
                            put("reviewDateMillis", r.reviewDateMillis)
                            put("moodTag", r.moodTag)
                            put("followedPlan", r.followedPlan)
                            put("usedStopLoss", r.usedStopLoss)
                            put("respectedMaxLoss", r.respectedMaxLoss)
                            put("mistakesSummary", r.mistakesSummary)
                            put("psychologyNotes", r.psychologyNotes)
                            put("actionPlan", r.actionPlan)
                            put("adminComment", r.adminComment)
                            put("createdAt", r.createdAt)
                        })
                    }
                })
            }
            root.toString(2)
        }
    }

    suspend fun restoreFromBackupJson(session: SessionContext, jsonText: String): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                requireAdmin(session)
                val root = JSONObject(jsonText)
                val usersArr = root.optJSONArray("users") ?: JSONArray()
                val tradesArr = root.optJSONArray("trades") ?: JSONArray()
                val profilesArr = root.optJSONArray("disciplineProfiles") ?: JSONArray()
                val reviewsArr = root.optJSONArray("disciplineReviews") ?: JSONArray()

                var importedCount = 0
                val emailToLocalId = mutableMapOf<Long, Long>()

                for (i in 0 until usersArr.length()) {
                    val obj = usersArr.getJSONObject(i)
                    val backupId = obj.getLong("id")
                    val email = SecurityUtils.normalizeEmail(obj.getString("email"))
                    val existing = userDao.findByEmail(email)
                    if (existing != null) {
                        emailToLocalId[backupId] = existing.id
                    } else {
                        val newId = userDao.insertUser(
                            UserEntity(
                                fullName = obj.getString("fullName"),
                                email = email,
                                passwordHash = obj.getString("passwordHash"),
                                passwordSalt = obj.getString("passwordSalt"),
                                role = if (obj.optString("role") == UserRole.ADMIN.name && userDao.getAdminCount() > 0) {
                                    UserRole.STUDENT.name
                                } else {
                                    obj.optString("role", UserRole.STUDENT.name)
                                },
                                status = obj.optString("status", AccountStatus.ACTIVE.name),
                                phone = obj.optString("phone", ""),
                                tradingStyle = obj.optString("tradingStyle", "Price Action"),
                                adminNotes = obj.optString("adminNotes", ""),
                                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                                updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                            )
                        )
                        emailToLocalId[backupId] = newId
                        importedCount++
                    }
                }

                for (i in 0 until tradesArr.length()) {
                    val obj = tradesArr.getJSONObject(i)
                    val mappedUserId = emailToLocalId[obj.getLong("userId")] ?: session.userId
                    val code = obj.optString("tradeCode").ifBlank { generateTradeCode() }
                    tradeDao.insertTrade(
                        TradeEntity(
                            tradeCode = "${code}_${UUID.randomUUID().toString().take(4)}",
                            userId = mappedUserId,
                            userName = obj.optString("userName", session.fullName),
                            tradeDateMillis = obj.getLong("tradeDateMillis"),
                            instrument = obj.getString("instrument"),
                            direction = obj.getString("direction"),
                            timeframe = obj.getString("timeframe"),
                            entryPrice = obj.getDouble("entryPrice"),
                            exitPrice = obj.getDouble("exitPrice"),
                            stopLoss = obj.getDouble("stopLoss"),
                            takeProfit = obj.getDouble("takeProfit"),
                            lotSize = obj.getDouble("lotSize"),
                            riskAmount = obj.getDouble("riskAmount"),
                            profitLoss = obj.getDouble("profitLoss"),
                            result = obj.getString("result"),
                            strategy = obj.optString("strategy", ""),
                            entryReason = obj.optString("entryReason", ""),
                            exitReason = obj.optString("exitReason", ""),
                            mistakes = obj.optString("mistakes", ""),
                            lessonsLearned = obj.optString("lessonsLearned", ""),
                            screenshotPath = obj.optString("screenshotPath", ""),
                            notes = obj.optString("notes", ""),
                            adminFeedback = obj.optString("adminFeedback", ""),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                    importedCount++
                }

                for (i in 0 until profilesArr.length()) {
                    val obj = profilesArr.getJSONObject(i)
                    val mappedUserId = emailToLocalId[obj.getLong("userId")] ?: continue
                    disciplineDao.upsertProfile(
                        DisciplineProfileEntity(
                            userId = mappedUserId,
                            dailyProfitGoal = obj.optDouble("dailyProfitGoal", 200.0),
                            dailyMaxLossLimit = obj.optDouble("dailyMaxLossLimit", 100.0),
                            maxTradesPerDay = obj.optInt("maxTradesPerDay", 3),
                            maxRiskPerTrade = obj.optDouble("maxRiskPerTrade", 50.0),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }

                for (i in 0 until reviewsArr.length()) {
                    val obj = reviewsArr.getJSONObject(i)
                    val mappedUserId = emailToLocalId[obj.getLong("userId")] ?: continue
                    disciplineDao.insertReview(
                        DisciplineReviewEntity(
                            userId = mappedUserId,
                            reviewType = obj.optString("reviewType", "DAILY_PSYCHOLOGY"),
                            reviewDateMillis = obj.optLong("reviewDateMillis", System.currentTimeMillis()),
                            moodTag = obj.optString("moodTag", "Focused"),
                            followedPlan = obj.optBoolean("followedPlan", true),
                            usedStopLoss = obj.optBoolean("usedStopLoss", true),
                            respectedMaxLoss = obj.optBoolean("respectedMaxLoss", true),
                            mistakesSummary = obj.optString("mistakesSummary", ""),
                            psychologyNotes = obj.optString("psychologyNotes", ""),
                            actionPlan = obj.optString("actionPlan", ""),
                            adminComment = obj.optString("adminComment", ""),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                    importedCount++
                }
                importedCount
            }
        }

    private fun requireAdmin(session: SessionContext) {
        if (session.role != UserRole.ADMIN) {
            throw SecurityException("Admin privileges required for this action.")
        }
    }

    private fun requireOwnerOrAdmin(session: SessionContext, ownerUserId: Long) {
        if (session.role != UserRole.ADMIN && session.userId != ownerUserId) {
            throw SecurityException("Unauthorized: You can only access your own records.")
        }
    }

    private suspend fun generateTradeCode(): String {
        val datePart = SimpleDateFormat("yyMMdd", Locale.US).format(Date())
        val seq = tradeDao.getTotalTradeCount() + 1
        val rand = UUID.randomUUID().toString().take(3).uppercase(Locale.US)
        return "TBB-$datePart-${seq.toString().padStart(4, '0')}-$rand"
    }
}
