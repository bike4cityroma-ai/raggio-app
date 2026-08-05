package org.bike4city.ciclofficinabot.data.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import org.bike4city.ciclofficinabot.BuildConfig
import org.bike4city.ciclofficinabot.domain.model.DiagnosisReport
import org.bike4city.ciclofficinabot.domain.repository.RemoteReportRepository

/** Sincronizza il riepilogo solo dopo la conferma esplicita dell'utente. */
class FirebaseSessionRepository(private val context: Context) : RemoteReportRepository {
    override val isConfigured: Boolean get() = BuildConfig.FIREBASE_CONFIGURED

    private var auth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null

    fun useEmulators(host: String, authPort: Int = 9099, firestorePort: Int = 8080) {
        check(isConfigured) { NOT_CONFIGURED_MESSAGE }
        val services = services()
        services.first.useEmulator(host, authPort)
        services.second.useEmulator(host, firestorePort)
    }

    suspend fun connectAnonymously(): Result<String> = runCatching {
        check(isConfigured) { NOT_CONFIGURED_MESSAGE }
        val firebaseAuth = services().first
        val user = firebaseAuth.currentUser ?: firebaseAuth.signInAnonymously().await().user
        requireNotNull(user) { "Firebase Auth non ha restituito un utente" }
        user.uid
    }

    override suspend fun saveReport(sessionId: String, report: DiagnosisReport): Result<Unit> = runCatching {
        val uid = connectAnonymously().getOrThrow()
        services().second
            .collection("ciclofficinaBotUsers").document(uid)
            .collection("sessions").document(sessionId)
            .set(
                mapOf(
                    "bikeSummary" to report.bikeSummary,
                    "initialProblem" to report.initialProblem,
                    "checksPerformed" to report.checksPerformed,
                    "possibleCauses" to report.possibleCauses,
                    "safetyLevel" to report.safetyLevel.name,
                    "outcome" to report.outcome.name,
                    "usageRecommendation" to report.usageRecommendation,
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            ).await()
    }

    override suspend fun deleteReport(sessionId: String): Result<Unit> = runCatching {
        val uid = connectAnonymously().getOrThrow()
        FirebaseFunctions.getInstance("europe-west1")
            .getHttpsCallable("deleteSessionPhotos")
            .call(mapOf("sessionId" to sessionId))
            .await()
        services().second
            .collection("ciclofficinaBotUsers").document(uid)
            .collection("sessions").document(sessionId)
            .delete()
            .await()
    }

    private fun services(): Pair<FirebaseAuth, FirebaseFirestore> {
        auth?.let { existingAuth ->
            firestore?.let { existingFirestore -> return existingAuth to existingFirestore }
        }
        val app = FirebaseApp.initializeApp(context.applicationContext)
            ?: error("Configurazione Firebase non valida")
        return FirebaseAuth.getInstance(app).also { auth = it } to
            FirebaseFirestore.getInstance(app).also { firestore = it }
    }

    private companion object {
        const val NOT_CONFIGURED_MESSAGE =
            "Firebase non configurato: aggiungere app/google-services.json"
    }
}
