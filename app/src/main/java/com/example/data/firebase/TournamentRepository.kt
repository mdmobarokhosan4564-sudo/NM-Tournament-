package com.example.data.firebase

import com.example.data.model.AppConfig
import com.example.data.model.FreeFireRank
import com.example.data.model.LeaderboardEntry
import com.example.data.model.LeaderboardSortMetric
import com.example.data.model.MatchCategory
import com.example.data.model.MatchStatus
import com.example.data.model.Registration
import com.example.data.model.RegistrationStatus
import com.example.data.model.RoomSecret
import com.example.data.model.RosterEntry
import com.example.data.model.TeamType
import com.example.data.model.PaymentMethod
import com.example.data.model.TournamentMatch
import com.example.data.model.UserProfile
import com.example.data.model.WalletRequest
import com.example.data.model.WalletRequestStatus
import com.example.data.model.WalletRequestType
import com.example.data.model.rankTop10Leaderboard
import com.example.notifications.ArenaNotificationHelper
import com.example.util.formatScheduleTextFromMillis
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

data class JoinTournamentResult(
    val slotNumber: Long,
    val updatedPlayerCount: Long,
    val totalSlots: Long
)

class TournamentRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("Please sign in with Google first.")

    private fun sha256Hex(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun <T> Flow<T>.handleFirestoreErrors(
        operationType: OperationType,
        path: String,
        expectedUid: String
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }

    fun observeAppConfig(userId: String): Flow<AppConfig> = flow {
        require(userId.isNotBlank()) { "User ID required" }
        val path = "app_config/main"
        emitAll(
            db.collection("app_config").document("main")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            AppConfig::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ) ?: AppConfig()
                    } else {
                        AppConfig()
                    }
                }
                .handleFirestoreErrors(OperationType.GET, path, userId)
        )
    }

    fun observeActiveCategories(userId: String): Flow<List<MatchCategory>> = flow {
        require(userId.isNotBlank()) { "User ID required" }
        val path = "categories"
        emitAll(
            db.collection("categories")
                .whereEqualTo("active", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MatchCategory::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.sortOrder }
                }
                .handleFirestoreErrors(OperationType.LIST, path, userId)
        )
    }

    fun observeAllCategories(userId: String): Flow<List<MatchCategory>> = flow {
        require(userId.isNotBlank()) { "User ID required" }
        val path = "categories"
        emitAll(
            db.collection("categories")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MatchCategory::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.sortOrder }
                }
                .handleFirestoreErrors(OperationType.LIST, path, userId)
        )
    }

    fun observeTournaments(userId: String): Flow<List<TournamentMatch>> = flow {
        require(userId.isNotBlank()) { "User ID required" }
        val path = "tournaments"
        emitAll(
            db.collection("tournaments")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        TournamentMatch::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareBy<TournamentMatch> {
                            when (MatchStatus.fromRaw(it.status)) {
                                MatchStatus.LIVE -> 0
                                MatchStatus.UPCOMING -> 1
                                MatchStatus.COMPLETED -> 2
                                MatchStatus.CANCELLED -> 3
                            }
                        }.thenByDescending { it.featured }
                            .thenBy { if (it.startTimeMillis > 0L) it.startTimeMillis else Long.MAX_VALUE }
                            .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
                    )
                }
                .handleFirestoreErrors(OperationType.LIST, path, userId)
        )
    }

    fun observeTournamentRoster(userId: String, tournamentId: String): Flow<List<RosterEntry>> = flow {
        require(userId.isNotBlank() && tournamentId.isNotBlank())
        val path = "tournaments/$tournamentId/roster"
        emitAll(
            db.collection("tournaments").document(tournamentId).collection("roster")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        RosterEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareBy<RosterEntry> { if (it.rankPosition > 0) it.rankPosition else 999L }
                            .thenBy { it.slotNumber }
                    )
                }
                .handleFirestoreErrors(OperationType.LIST, path, userId)
        )
    }

    fun observeMyRegistrations(userId: String): Flow<List<Registration>> = flow {
        require(userId.isNotBlank()) { "User ID required" }
        val path = "registrations"
        emitAll(
            db.collection("registrations")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Registration::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .handleFirestoreErrors(OperationType.LIST, path, userId)
        )
    }

    fun observeTournamentRegistrations(userId: String, tournamentId: String): Flow<List<Registration>> = flow {
        require(userId.isNotBlank() && tournamentId.isNotBlank())
        val path = "registrations"
        emitAll(
            db.collection("registrations")
                .whereEqualTo("tournamentId", tournamentId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Registration::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.slotNumber }
                }
                .handleFirestoreErrors(OperationType.LIST, path, userId)
        )
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        require(userId.isNotBlank()) { "User ID required" }
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .handleFirestoreErrors(OperationType.GET, path, userId)
        )
    }

    fun observeLeaderboard(userId: String): Flow<List<LeaderboardEntry>> =
        observeTop10Leaderboard(userId, LeaderboardSortMetric.WINNINGS)

    fun observeTop10Leaderboard(
        userId: String,
        sortBy: LeaderboardSortMetric
    ): Flow<List<LeaderboardEntry>> = flow {
        require(userId.isNotBlank()) { "User ID required" }
        val path = "leaderboard"
        emitAll(
            db.collection("leaderboard")
                .orderBy(sortBy.firestoreField, Query.Direction.DESCENDING)
                .limit(10L)
                .snapshots()
                .map { snapshot ->
                    val raw = snapshot.toObjects(
                        LeaderboardEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                    rankTop10Leaderboard(raw, sortBy)
                }
                .handleFirestoreErrors(OperationType.LIST, path, userId)
        )
    }

    fun observeAdminClaim(userId: String): Flow<Boolean> = flow {
        require(userId.isNotBlank()) { "User ID required" }
        val path = "admins/$userId"
        emitAll(
            db.collection("admins").document(userId)
                .snapshots()
                .map { it.exists() }
                .handleFirestoreErrors(OperationType.GET, path, userId)
        )
    }

    fun observeMyWalletRequests(userId: String): Flow<List<WalletRequest>> = flow {
        require(userId.isNotBlank()) { "User ID required" }
        val path = "wallet_requests"
        emitAll(
            db.collection("wallet_requests")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        WalletRequest::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .handleFirestoreErrors(OperationType.LIST, path, userId)
        )
    }

    fun observeAllWalletRequests(userId: String): Flow<List<WalletRequest>> = flow {
        require(userId.isNotBlank()) { "User ID required" }
        val path = "wallet_requests"
        emitAll(
            db.collection("wallet_requests")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        WalletRequest::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareBy<WalletRequest> {
                            when (WalletRequestStatus.fromRaw(it.status)) {
                                WalletRequestStatus.PENDING -> 0
                                WalletRequestStatus.APPROVED -> 1
                                WalletRequestStatus.REJECTED -> 2
                            }
                        }.thenByDescending { it.createdAt ?: Timestamp(0, 0) }
                    )
                }
                .handleFirestoreErrors(OperationType.LIST, path, userId)
        )
    }

    fun observeRoomSecret(userId: String, tournamentId: String): Flow<RoomSecret?> = flow {
        require(userId.isNotBlank() && tournamentId.isNotBlank())
        val path = "tournaments/$tournamentId/room_secret/credentials"
        emitAll(
            db.collection("tournaments")
                .document(tournamentId)
                .collection("room_secret")
                .document("credentials")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            RoomSecret::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .handleFirestoreErrors(OperationType.GET, path, userId)
        )
    }

    private fun buildMissingUserProfileRepairMap(
        snap: DocumentSnapshot,
        uid: String,
        fallbackDisplayName: String?,
        fallbackEmail: String?
    ): MutableMap<String, Any> {
        val repairs = mutableMapOf<String, Any>()
        if ((snap.getString("userId") ?: "").isBlank()) {
            repairs["userId"] = uid
        }
        val curName = snap.getString("displayName")?.trim() ?: ""
        if (curName.length < 2) {
            val safeName = (fallbackDisplayName ?: "Survivor").trim().ifBlank { "Survivor" }.take(40).let {
                if (it.length < 2) "Survivor" else it
            }
            repairs["displayName"] = safeName
        }
        if (snap.get("ffName") !is String) {
            repairs["ffName"] = ""
        }
        if (snap.get("ffUid") !is String) {
            repairs["ffUid"] = ""
        }
        if (snap.get("inGameRank") !is String) {
            repairs["inGameRank"] = FreeFireRank.UNRANKED.label
        }
        if (snap.get("email") !is String) {
            repairs["email"] = (fallbackEmail ?: "").trim().take(120)
        }
        if (snap.get("matchesJoined") !is Number) {
            repairs["matchesJoined"] = 0L
        }
        if (snap.get("totalKills") !is Number) {
            repairs["totalKills"] = 0L
        }
        if (snap.get("totalWins") !is Number) {
            repairs["totalWins"] = 0L
        }
        if (snap.get("totalWinnings") !is Number) {
            repairs["totalWinnings"] = 0L
        }
        if (snap.get("walletBalance") !is Number) {
            repairs["walletBalance"] = 0L
        }
        if (snap.getTimestamp("createdAt") == null) {
            repairs["createdAt"] = FieldValue.serverTimestamp()
        }
        return repairs
    }

    suspend fun ensureUserProfileInitialized(
        userId: String,
        displayName: String?,
        email: String?
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val ref = db.collection("users").document(uid)
            val snap = ref.get().await()
            if (!snap.exists()) {
                val safeName = (displayName ?: "Survivor").trim().ifBlank { "Survivor" }.take(40).let {
                    if (it.length < 2) "Survivor" else it
                }
                val profile = UserProfile(
                    userId = uid,
                    displayName = safeName,
                    email = (email ?: "").trim().take(120)
                )
                ref.set(profile.toCreateMap()).await()
            } else {
                val repairs = buildMissingUserProfileRepairMap(snap, uid, displayName, email)
                if (repairs.isNotEmpty()) {
                    repairs["updatedAt"] = FieldValue.serverTimestamp()
                    ref.update(repairs).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updatePlayerIdentity(
        userId: String,
        displayName: String,
        ffName: String,
        ffUid: String,
        inGameRank: String? = null
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val cleanDisplay = displayName.trim().ifBlank { "Survivor" }.take(40).let {
                if (it.length < 2) "Survivor" else it
            }
            val cleanIgn = ffName.trim().take(32)
            val cleanUid = ffUid.trim().take(20)
            val cleanRank = inGameRank?.trim()?.ifBlank { FreeFireRank.UNRANKED.label }?.take(32)
            val ref = db.collection("users").document(uid)
            val snap = ref.get().await()
            if (!snap.exists()) {
                val newProfile = UserProfile(
                    userId = uid,
                    displayName = cleanDisplay,
                    ffName = cleanIgn,
                    ffUid = cleanUid,
                    inGameRank = cleanRank ?: FreeFireRank.UNRANKED.label,
                    email = (auth.currentUser?.email ?: "").trim().take(120)
                )
                ref.set(newProfile.toCreateMap()).await()
            } else {
                val updates = buildMissingUserProfileRepairMap(
                    snap = snap,
                    uid = uid,
                    fallbackDisplayName = cleanDisplay,
                    fallbackEmail = auth.currentUser?.email
                )
                updates["displayName"] = cleanDisplay
                updates["ffName"] = cleanIgn
                updates["ffUid"] = cleanUid
                if (cleanRank != null) {
                    updates["inGameRank"] = cleanRank
                }
                updates["updatedAt"] = FieldValue.serverTimestamp()
                ref.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateInGameRank(
        userId: String,
        inGameRank: String,
        currentProfile: UserProfile?
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val cleanRank = inGameRank.trim().ifBlank { FreeFireRank.UNRANKED.label }.take(32)
            val ref = db.collection("users").document(uid)
            val snap = ref.get().await()
            if (!snap.exists()) {
                val safeName = (currentProfile?.displayName ?: auth.currentUser?.displayName ?: "Survivor")
                    .trim().ifBlank { "Survivor" }.take(40).let { if (it.length < 2) "Survivor" else it }
                val newProfile = UserProfile(
                    userId = uid,
                    displayName = safeName,
                    ffName = (currentProfile?.ffName ?: "").trim().take(32),
                    ffUid = (currentProfile?.ffUid ?: "").trim().take(20),
                    inGameRank = cleanRank,
                    email = (currentProfile?.email ?: auth.currentUser?.email ?: "").trim().take(120)
                )
                ref.set(newProfile.toCreateMap()).await()
            } else {
                val updates = buildMissingUserProfileRepairMap(
                    snap = snap,
                    uid = uid,
                    fallbackDisplayName = currentProfile?.displayName ?: auth.currentUser?.displayName,
                    fallbackEmail = currentProfile?.email ?: auth.currentUser?.email
                )
                updates["inGameRank"] = cleanRank
                updates["updatedAt"] = FieldValue.serverTimestamp()
                ref.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveUserFcmToken(userId: String, token: String): Result<Unit> {
        val cleanToken = token.trim().take(512)
        if (cleanToken.isBlank()) return Result.success(Unit)
        val docPath = "users/$userId"
        return try {
            val uid = auth.currentUser?.uid ?: return Result.success(Unit)
            if (uid != userId) return Result.success(Unit)
            val ref = db.collection("users").document(uid)
            val snap = ref.get().await()
            if (!snap.exists()) {
                val safeName = (auth.currentUser?.displayName ?: "Survivor").trim().ifBlank { "Survivor" }.take(40)
                    .let { if (it.length < 2) "Survivor" else it }
                val newProfile = UserProfile(
                    userId = uid,
                    displayName = safeName,
                    email = (auth.currentUser?.email ?: "").trim().take(120),
                    fcmToken = cleanToken
                )
                ref.set(newProfile.toCreateMap()).await()
            } else {
                val currentToken = snap.getString("fcmToken") ?: ""
                val updates = buildMissingUserProfileRepairMap(
                    snap = snap,
                    uid = uid,
                    fallbackDisplayName = auth.currentUser?.displayName,
                    fallbackEmail = auth.currentUser?.email
                )
                if (currentToken != cleanToken) {
                    updates["fcmToken"] = cleanToken
                }
                if (updates.isNotEmpty()) {
                    updates["updatedAt"] = FieldValue.serverTimestamp()
                    ref.update(updates).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun syncMessagingTokenAndTopics(
        userId: String,
        joinedTournamentIds: Set<String>,
        previouslySubscribedIds: Set<String> = emptySet()
    ): Set<String> {
        val messaging = runCatching { FirebaseMessaging.getInstance() }.getOrNull() ?: return previouslySubscribedIds
        val token = runCatching { messaging.token.await() }.getOrNull()
        if (!token.isNullOrBlank()) {
            saveUserFcmToken(userId, token)
        }
        runCatching { messaging.subscribeToTopic(ArenaNotificationHelper.TOPIC_ALL_PLAYERS).await() }
        runCatching { messaging.subscribeToTopic(ArenaNotificationHelper.userTopic(userId)).await() }
        val cleanJoined = joinedTournamentIds.filter { it.isNotBlank() }.toSet()
        (previouslySubscribedIds - cleanJoined).filter { it.isNotBlank() }.forEach { removedTid ->
            runCatching { messaging.unsubscribeFromTopic(ArenaNotificationHelper.tournamentTopic(removedTid)).await() }
        }
        cleanJoined.forEach { tid ->
            runCatching { messaging.subscribeToTopic(ArenaNotificationHelper.tournamentTopic(tid)).await() }
        }
        return cleanJoined
    }

    suspend fun unsubscribeTournamentTopics(tournamentIds: Set<String>) {
        val messaging = runCatching { FirebaseMessaging.getInstance() }.getOrNull() ?: return
        tournamentIds.filter { it.isNotBlank() }.forEach { tid ->
            runCatching { messaging.unsubscribeFromTopic(ArenaNotificationHelper.tournamentTopic(tid)).await() }
        }
    }

    suspend fun unlockAdminWithPasscode(userId: String, enteredPasscode: String): Result<Unit> {
        val docPath = "admins/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val digest = sha256Hex(enteredPasscode)
            if (digest != EXPECTED_ADMIN_SHA256) {
                return Result.failure(IllegalArgumentException("Incorrect admin password."))
            }
            val docRef = db.collection("admins").document(uid)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "passcode" to enteredPasscode,
                        "grantedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun joinTournament(
        userId: String,
        tournament: TournamentMatch,
        ffName: String,
        ffUid: String,
        squadName: String,
        paymentRef: String,
        currentProfile: UserProfile?
    ): Result<JoinTournamentResult> {
        val regId = "${tournament.id}_$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val cleanIgn = ffName.trim()
            val cleanFfUid = ffUid.trim()
            val cleanSquad = squadName.trim().take(32)
            val cleanPayRef = paymentRef.trim().take(64)

            require(cleanIgn.length in 2..32) { "In-game username must be 2 to 32 characters." }
            require(cleanFfUid.isEmpty() || cleanFfUid.length in 5..20) {
                "Free Fire UID must be 5 to 20 digits if provided."
            }

            val tourneyRef = db.collection("tournaments").document(tournament.id)
            val regRef = db.collection("registrations").document(regId)
            val rosterRef = tourneyRef.collection("roster").document(uid)
            val userRef = db.collection("users").document(uid)

            val joinResult = db.runTransaction { transaction ->
                val liveSnap = transaction.get(tourneyRef)
                val liveTourney = liveSnap.toObject(TournamentMatch::class.java)
                    ?: throw IllegalStateException("Tournament no longer exists.")

                if (MatchStatus.fromRaw(liveTourney.status) != MatchStatus.UPCOMING) {
                    throw IllegalStateException("Registration is closed for this match.")
                }

                val regSnap = transaction.get(regRef)
                if (regSnap.exists()) {
                    throw IllegalStateException("You have already joined this match.")
                }

                val currentFilled = (liveSnap.get("filledSlots") as? Number)?.toLong() ?: liveTourney.filledSlots
                val totalSlots = (liveSnap.get("totalSlots") as? Number)?.toLong() ?: liveTourney.totalSlots
                val nextSlot = currentFilled + 1L
                if (nextSlot > totalSlots) {
                    throw IllegalStateException("All slots are full!")
                }

                val userSnap = transaction.get(userRef)

                val initialStatus = if (liveTourney.entryFee > 0L && cleanPayRef.isNotBlank()) {
                    RegistrationStatus.PENDING.name
                } else {
                    RegistrationStatus.CONFIRMED.name
                }

                val safeTitle = liveTourney.title.trim().let { if (it.length < 2) "FF Tournament" else it }.take(80)
                val safeSchedule = liveTourney.scheduleText.trim().let { if (it.length < 2) "TBA" else it }.take(60)
                val safeMap = liveTourney.mapName.trim().let { if (it.length < 2) "Bermuda" else it }.take(40)

                transaction.set(
                    regRef,
                    mapOf(
                        "userId" to uid,
                        "tournamentId" to liveTourney.id,
                        "tournamentTitle" to safeTitle,
                        "scheduleText" to safeSchedule,
                        "teamType" to TeamType.fromRaw(liveTourney.teamType).name,
                        "mapName" to safeMap,
                        "entryFee" to liveTourney.entryFee.coerceIn(0L, 100000L),
                        "ffName" to cleanIgn,
                        "ffUid" to cleanFfUid,
                        "squadName" to cleanSquad,
                        "paymentRef" to cleanPayRef,
                        "slotNumber" to nextSlot,
                        "status" to initialStatus,
                        "kills" to 0L,
                        "rankPosition" to 0L,
                        "prizeWon" to 0L,
                        "prizePaid" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.set(
                    rosterRef,
                    mapOf(
                        "userId" to uid,
                        "ffName" to cleanIgn,
                        "squadName" to cleanSquad,
                        "slotNumber" to nextSlot,
                        "kills" to 0L,
                        "rankPosition" to 0L,
                        "prizeWon" to 0L,
                        "status" to initialStatus,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.update(
                    tourneyRef,
                    mapOf(
                        "filledSlots" to nextSlot,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                if (userSnap.exists()) {
                    val userUpdate = buildMissingUserProfileRepairMap(
                        snap = userSnap,
                        uid = uid,
                        fallbackDisplayName = currentProfile?.displayName ?: auth.currentUser?.displayName,
                        fallbackEmail = currentProfile?.email ?: auth.currentUser?.email
                    )
                    val existingJoined = (userSnap.get("matchesJoined") as? Number)?.toLong() ?: 0L
                    userUpdate["ffName"] = cleanIgn
                    if (cleanFfUid.isNotEmpty()) {
                        userUpdate["ffUid"] = cleanFfUid
                    }
                    userUpdate["matchesJoined"] = existingJoined + 1L
                    userUpdate["updatedAt"] = FieldValue.serverTimestamp()
                    transaction.update(userRef, userUpdate)
                } else {
                    val safeName = (currentProfile?.displayName ?: auth.currentUser?.displayName ?: "Survivor")
                        .trim().ifBlank { "Survivor" }.take(40).let { if (it.length < 2) "Survivor" else it }
                    val newProfile = UserProfile(
                        userId = uid,
                        displayName = safeName,
                        ffName = cleanIgn,
                        ffUid = cleanFfUid,
                        email = (currentProfile?.email ?: auth.currentUser?.email ?: "").trim().take(120)
                    )
                    transaction.set(userRef, newProfile.toCreateMap())
                }
                JoinTournamentResult(
                    slotNumber = nextSlot,
                    updatedPlayerCount = nextSlot,
                    totalSlots = totalSlots
                )
            }.await()

            runCatching {
                FirebaseMessaging.getInstance()
                    .subscribeToTopic(ArenaNotificationHelper.tournamentTopic(tournament.id))
                    .await()
            }
            Result.success(joinResult)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "registrations/$regId")
            Result.failure(e)
        }
    }

    suspend fun fetchRoomSecret(tournamentId: String): Result<RoomSecret?> {
        val docPath = "tournaments/$tournamentId/room_secret/credentials"
        return try {
            requireUserId()
            val snap = db.collection("tournaments")
                .document(tournamentId)
                .collection("room_secret")
                .document("credentials")
                .get()
                .await()
            val secret = if (snap.exists()) {
                snap.toObject(
                    RoomSecret::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else null
            Result.success(secret)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docPath)
            Result.failure(e)
        }
    }

    suspend fun submitAddMoneyRequest(
        userId: String,
        profile: UserProfile?,
        method: PaymentMethod,
        amount: Long,
        senderNumber: String,
        txnId: String
    ): Result<Unit> {
        val reqId = "dep_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val docPath = "wallet_requests/$reqId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val cleanTxn = txnId.trim().take(64)
            val cleanAccount = senderNumber.trim().take(20)
            require(amount in 1L..500000L) { "Amount must be between 1 and 500,000." }
            require(cleanTxn.length >= 4) { "Enter a valid Transaction ID (4+ characters)." }

            val safeName = (profile?.displayName ?: auth.currentUser?.displayName ?: "Survivor")
                .trim().ifBlank { "Survivor" }.take(40)
            val safeFfName = (profile?.ffName ?: "").trim().take(32)

            db.collection("wallet_requests").document(reqId).set(
                mapOf(
                    "userId" to uid,
                    "userName" to safeName,
                    "ffName" to safeFfName,
                    "type" to WalletRequestType.DEPOSIT.name,
                    "method" to method.name,
                    "amount" to amount,
                    "accountNumber" to cleanAccount,
                    "txnId" to cleanTxn,
                    "status" to WalletRequestStatus.PENDING.name,
                    "adminNote" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun submitWithdrawRequest(
        userId: String,
        profile: UserProfile?,
        method: PaymentMethod,
        accountNumber: String,
        amount: Long,
        pendingWithdrawSum: Long
    ): Result<Unit> {
        val reqId = "wd_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val docPath = "wallet_requests/$reqId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val cleanAccount = accountNumber.trim().take(20)
            require(cleanAccount.length >= 6) { "Enter a valid ${method.label} number." }
            require(amount in 1L..500000L) { "Amount must be between 1 and 500,000." }

            val userSnap = db.collection("users").document(uid).get().await()
            val liveBalance = (userSnap.get("walletBalance") as? Number)?.toLong() ?: 0L
            val availableAfterPending = (liveBalance - pendingWithdrawSum).coerceAtLeast(0L)
            require(amount <= liveBalance) { "Insufficient wallet balance (Balance: $liveBalance)." }
            require(amount <= availableAfterPending) {
                "You already have pending withdrawals totaling $pendingWithdrawSum. Available: $availableAfterPending."
            }

            val safeName = (profile?.displayName ?: auth.currentUser?.displayName ?: "Survivor")
                .trim().ifBlank { "Survivor" }.take(40)
            val safeFfName = (profile?.ffName ?: "").trim().take(32)

            db.collection("wallet_requests").document(reqId).set(
                mapOf(
                    "userId" to uid,
                    "userName" to safeName,
                    "ffName" to safeFfName,
                    "type" to WalletRequestType.WITHDRAW.name,
                    "method" to method.name,
                    "amount" to amount,
                    "accountNumber" to cleanAccount,
                    "txnId" to "",
                    "status" to WalletRequestStatus.PENDING.name,
                    "adminNote" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun processWalletRequest(
        request: WalletRequest,
        approve: Boolean,
        adminNote: String
    ): Result<Unit> {
        val docPath = "wallet_requests/${request.id}"
        return try {
            requireUserId()
            val reqRef = db.collection("wallet_requests").document(request.id)
            val liveReqSnap = reqRef.get().await()
            if (!liveReqSnap.exists()) {
                return Result.failure(IllegalStateException("Wallet request not found."))
            }
            val currentStatus = liveReqSnap.getString("status") ?: WalletRequestStatus.PENDING.name
            if (currentStatus != WalletRequestStatus.PENDING.name) {
                return Result.failure(IllegalStateException("This request was already $currentStatus."))
            }

            val userRef = db.collection("users").document(request.userId)
            val userSnap = userRef.get().await()
            val curBalance = (userSnap.get("walletBalance") as? Number)?.toLong() ?: 0L
            val reqType = WalletRequestType.fromRaw(request.type)

            val batch = db.batch()
            if (approve) {
                val nextBalance = when (reqType) {
                    WalletRequestType.DEPOSIT -> curBalance + request.amount
                    WalletRequestType.WITHDRAW -> {
                        if (curBalance < request.amount) {
                            return Result.failure(
                                IllegalStateException("Player wallet balance ($curBalance) is lower than withdraw amount (${request.amount}).")
                            )
                        }
                        curBalance - request.amount
                    }
                }
                if (userSnap.exists()) {
                    val userUpdate = buildMissingUserProfileRepairMap(
                        snap = userSnap,
                        uid = request.userId,
                        fallbackDisplayName = request.userName,
                        fallbackEmail = ""
                    )
                    userUpdate["walletBalance"] = nextBalance.coerceAtLeast(0L)
                    userUpdate["updatedAt"] = FieldValue.serverTimestamp()
                    batch.update(userRef, userUpdate)
                }
            }

            val safeNote = adminNote.trim().take(200).ifBlank {
                if (approve) {
                    if (reqType == WalletRequestType.DEPOSIT) "Approved & Added to Wallet" else "Paid to ${request.accountNumber}"
                } else {
                    "Rejected by Admin"
                }
            }
            batch.update(
                reqRef,
                mapOf(
                    "status" to if (approve) WalletRequestStatus.APPROVED.name else WalletRequestStatus.REJECTED.name,
                    "adminNote" to safeNote,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // --- ADMIN OPERATIONS ---

    suspend fun saveAppConfig(config: AppConfig): Result<Unit> {
        val docPath = "app_config/main"
        return try {
            requireUserId()
            val ref = db.collection("app_config").document("main")
            val snap = ref.get().await()
            if (snap.exists()) {
                ref.update(config.toUpdateMap()).await()
            } else {
                ref.set(config.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveCategory(category: MatchCategory): Result<Unit> {
        val catId = category.id.ifBlank { "cat_${UUID.randomUUID().toString().take(8)}" }
        val docPath = "categories/$catId"
        return try {
            requireUserId()
            require(category.name.trim().length in 2..40) { "Category name must be 2-40 characters." }
            val ref = db.collection("categories").document(catId)
            val snap = ref.get().await()
            if (snap.exists()) {
                ref.update(category.toUpdateMap()).await()
            } else {
                ref.set(category.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(categoryId: String): Result<Unit> {
        val docPath = "categories/$categoryId"
        return try {
            requireUserId()
            db.collection("categories").document(categoryId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveTournament(
        tournament: TournamentMatch,
        roomId: String,
        roomPassword: String
    ): Result<String> {
        val tid = tournament.id.ifBlank { "match_${UUID.randomUUID().toString().take(8)}" }
        val docPath = "tournaments/$tid"
        return try {
            requireUserId()
            require(tournament.title.trim().length in 2..80) { "Match title must be 2-80 characters." }
            val tourneyRef = db.collection("tournaments").document(tid)
            val existing = tourneyRef.get().await()
            val batch = db.batch()
            if (existing.exists()) {
                batch.update(tourneyRef, tournament.toUpdateMap())
            } else {
                batch.set(tourneyRef, tournament.toCreateMap())
            }
            val secretRef = tourneyRef.collection("room_secret").document("credentials")
            val secretExists = secretRef.get().await().exists()
            val secretPayload = mapOf(
                "roomId" to roomId.trim().take(40),
                "roomPassword" to roomPassword.trim().take(40),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (secretExists) {
                batch.update(secretRef, secretPayload)
            } else {
                batch.set(secretRef, secretPayload)
            }
            batch.commit().await()
            Result.success(tid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateRoomCredentials(
        tournamentId: String,
        roomId: String,
        roomPassword: String,
        publishNow: Boolean
    ): Result<Unit> {
        val docPath = "tournaments/$tournamentId"
        return try {
            requireUserId()
            val tourneyRef = db.collection("tournaments").document(tournamentId)
            val secretRef = tourneyRef.collection("room_secret").document("credentials")
            val secretExists = secretRef.get().await().exists()

            val batch = db.batch()
            val secretPayload = mapOf(
                "roomId" to roomId.trim().take(40),
                "roomPassword" to roomPassword.trim().take(40),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (secretExists) {
                batch.update(secretRef, secretPayload)
            } else {
                batch.set(secretRef, secretPayload)
            }
            batch.update(
                tourneyRef,
                mapOf(
                    "roomPublished" to publishNow,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteTournament(tournamentId: String): Result<Unit> {
        val docPath = "tournaments/$tournamentId"
        return try {
            requireUserId()
            val tourneyRef = db.collection("tournaments").document(tournamentId)
            val secretRef = tourneyRef.collection("room_secret").document("credentials")
            if (secretRef.get().await().exists()) {
                secretRef.delete().await()
            }
            tourneyRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateParticipantResult(
        registration: Registration,
        newStatus: RegistrationStatus,
        kills: Long,
        rankPosition: Long,
        prizeWon: Long,
        prizePaid: Boolean
    ): Result<Unit> {
        val regId = "${registration.tournamentId}_${registration.userId}"
        val docPath = "registrations/$regId"
        return try {
            requireUserId()
            val safeKills = kills.coerceIn(0L, 100L)
            val safeRank = rankPosition.coerceIn(0L, 100L)
            val safePrize = prizeWon.coerceIn(0L, 1000000L)

            val killsDelta = safeKills - registration.kills
            val winsDelta = (if (safeRank == 1L) 1L else 0L) - (if (registration.rankPosition == 1L) 1L else 0L)
            val prizeDelta = safePrize - registration.prizeWon

            val regRef = db.collection("registrations").document(regId)
            val rosterRef = db.collection("tournaments").document(registration.tournamentId)
                .collection("roster").document(registration.userId)
            val userRef = db.collection("users").document(registration.userId)
            val leaderRef = db.collection("leaderboard").document(registration.userId)

            val rosterExists = rosterRef.get().await().exists()
            val userSnap = userRef.get().await()
            val leaderSnap = leaderRef.get().await()

            val batch = db.batch()
            batch.update(
                regRef,
                mapOf(
                    "status" to newStatus.name,
                    "slotNumber" to registration.slotNumber.coerceIn(1L, 100L),
                    "kills" to safeKills,
                    "rankPosition" to safeRank,
                    "prizeWon" to safePrize,
                    "prizePaid" to prizePaid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (rosterExists) {
                batch.update(
                    rosterRef,
                    mapOf(
                        "ffName" to registration.ffName,
                        "squadName" to registration.squadName,
                        "slotNumber" to registration.slotNumber.coerceIn(1L, 100L),
                        "kills" to safeKills,
                        "rankPosition" to safeRank,
                        "prizeWon" to safePrize,
                        "status" to newStatus.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            if (userSnap.exists()) {
                val curKills = (userSnap.get("totalKills") as? Number)?.toLong() ?: 0L
                val curWins = (userSnap.get("totalWins") as? Number)?.toLong() ?: 0L
                val curWinnings = (userSnap.get("totalWinnings") as? Number)?.toLong() ?: 0L
                batch.update(
                    userRef,
                    mapOf(
                        "totalKills" to (curKills + killsDelta).coerceAtLeast(0L),
                        "totalWins" to (curWins + winsDelta).coerceAtLeast(0L),
                        "totalWinnings" to (curWinnings + prizeDelta).coerceAtLeast(0L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            if (leaderSnap.exists()) {
                val curKills = (leaderSnap.get("totalKills") as? Number)?.toLong() ?: 0L
                val curWins = (leaderSnap.get("totalWins") as? Number)?.toLong() ?: 0L
                val curWinnings = (leaderSnap.get("totalWinnings") as? Number)?.toLong() ?: 0L
                val curMatches = (leaderSnap.get("matchesPlayed") as? Number)?.toLong() ?: 0L
                val nextMatches = if (registration.kills == 0L && registration.rankPosition == 0L && (safeKills > 0L || safeRank > 0L)) {
                    curMatches + 1L
                } else curMatches
                batch.update(
                    leaderRef,
                    mapOf(
                        "ffName" to registration.ffName.take(32),
                        "totalKills" to (curKills + killsDelta).coerceAtLeast(0L),
                        "totalWins" to (curWins + winsDelta).coerceAtLeast(0L),
                        "totalWinnings" to (curWinnings + prizeDelta).coerceAtLeast(0L),
                        "matchesPlayed" to nextMatches.coerceAtLeast(0L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    leaderRef,
                    mapOf(
                        "userId" to registration.userId,
                        "ffName" to registration.ffName.take(32),
                        "totalKills" to safeKills,
                        "totalWins" to if (safeRank == 1L) 1L else 0L,
                        "totalWinnings" to safePrize,
                        "matchesPlayed" to 1L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun seedStarterPack(): Result<Unit> {
        return try {
            requireUserId()
            val configRef = db.collection("app_config").document("main")
            if (!configRef.get().await().exists()) {
                configRef.set(AppConfig().toCreateMap()).await()
            }
            val starterCategories = listOf(
                MatchCategory(
                    id = "cat_br",
                    name = "BR Full Map",
                    subtitle = "48 Players • Solo / Duo / Squad",
                    badge = "48P",
                    bannerKey = "br",
                    sortOrder = 1L,
                    active = true
                ),
                MatchCategory(
                    id = "cat_cs",
                    name = "Clash Squad",
                    subtitle = "4v4 Tactical Best-of-7 Custom Room",
                    badge = "4v4",
                    bannerKey = "cs",
                    sortOrder = 2L,
                    active = true
                ),
                MatchCategory(
                    id = "cat_lw",
                    name = "Lone Wolf 1v1",
                    subtitle = "1v1 Iron Cage Headshot Duel",
                    badge = "1v1",
                    bannerKey = "lw",
                    sortOrder = 3L,
                    active = true
                )
            )
            starterCategories.forEach { cat ->
                val ref = db.collection("categories").document(cat.id)
                if (!ref.get().await().exists()) {
                    ref.set(cat.toCreateMap()).await()
                }
            }

            val nowMillis = System.currentTimeMillis()
            val match1Time = nowMillis + 42L * 60_000L
            val match2Time = nowMillis + 150L * 60_000L
            val match3Time = nowMillis + 18L * 3600_000L

            val starterMatches = listOf(
                TournamentMatch(
                    id = "match_bermuda_pro",
                    title = "Bermuda Night Rush #101",
                    categoryId = "cat_br",
                    categoryName = "BR Full Map",
                    teamType = TeamType.SQUAD.name,
                    mapName = "Bermuda",
                    perspective = "Mobile Only",
                    entryFee = 0L,
                    prizePool = 1000L,
                    perKillReward = 20L,
                    totalSlots = 48L,
                    filledSlots = 0L,
                    scheduleText = formatScheduleTextFromMillis(match1Time, nowMillis),
                    startTimeMillis = match1Time,
                    status = MatchStatus.UPCOMING.name,
                    rules = "No hackers, scripts, or mod APKs\nNo teaming with opponents\nMobile devices only (no emulator)\nSit in your assigned slot only",
                    roomPublished = false,
                    featured = true,
                    bannerKey = "br"
                ),
                TournamentMatch(
                    id = "match_cs_cup",
                    title = "Clash Squad Arena 4v4",
                    categoryId = "cat_cs",
                    categoryName = "Clash Squad",
                    teamType = TeamType.`4V4`.name,
                    mapName = "Bermuda",
                    perspective = "Limited Ammo",
                    entryFee = 50L,
                    prizePool = 800L,
                    perKillReward = 0L,
                    totalSlots = 8L,
                    filledSlots = 0L,
                    scheduleText = formatScheduleTextFromMillis(match2Time, nowMillis),
                    startTimeMillis = match2Time,
                    status = MatchStatus.UPCOMING.name,
                    rules = "No hackers, scripts, or mod APKs\nNo grenades or double vector\nNo teaming • Roof climb allowed",
                    roomPublished = false,
                    featured = false,
                    bannerKey = "cs"
                ),
                TournamentMatch(
                    id = "match_lone_wolf",
                    title = "Lone Wolf One-Tap Showdown",
                    categoryId = "cat_lw",
                    categoryName = "Lone Wolf 1v1",
                    teamType = TeamType.`1V1`.name,
                    mapName = "Iron Cage",
                    perspective = "Desert Eagle / M1887",
                    entryFee = 30L,
                    prizePool = 400L,
                    perKillReward = 0L,
                    totalSlots = 2L,
                    filledSlots = 0L,
                    scheduleText = formatScheduleTextFromMillis(match3Time, nowMillis),
                    startTimeMillis = match3Time,
                    status = MatchStatus.UPCOMING.name,
                    rules = "No hackers or aim-assist tools\n1v1 fair duel • No teaming\nNo hiding outside zone",
                    roomPublished = false,
                    featured = false,
                    bannerKey = "lw"
                )
            )
            starterMatches.forEach { match ->
                val ref = db.collection("tournaments").document(match.id)
                if (!ref.get().await().exists()) {
                    ref.set(match.toCreateMap()).await()
                    ref.collection("room_secret").document("credentials").set(
                        mapOf(
                            "roomId" to "",
                            "roomPassword" to "",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "seedStarterPack")
            Result.failure(e)
        }
    }

    private companion object {
        const val EXPECTED_ADMIN_SHA256 =
            "87ee7e4486a691a56307c6de6eb151c6ab06baa1d78d712c9cd49d0cd896fd20"
    }
}
