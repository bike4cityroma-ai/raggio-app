# Piano di sviluppo — BIKE4CITY Ciclofficina Bot

Ultimo aggiornamento: 13 agosto 2026

## Stato

- [x] Analisi repository: repository Git vuoto, nessun componente preesistente.
- [x] Fase 1: scaffold Android, tema Material 3, navigazione e schermate MVP; build debug verificata con JDK 21 di Android Studio e installata su moto g85 5G.
- [x] Fase 2: motore sicurezza, `ChatViewModel`/StateFlow, persistenza Room di sessioni, messaggi e report, DataStore per consenso/profilo bici, cronologia ed eliminazione.
- [x] Fase 3: app Android registrata nel progetto Firebase esistente, Auth anonima abilitata, adapter Auth/Firestore e regole isolate dalla web app; sincronizzazione facoltativa dalla UI, stato persistente e cancellazione remota coordinata. Regole e flusso completo verificati sul progetto reale; 12 test unitari e 2 test strumentali superati sul moto g85 5G.
- [x] Fase 4: Cloud Function TypeScript e provider AI distribuiti; 7 procedure approvate caricate, test locali completati e collaudo E2E superato con risposta strutturata e STOP server-side.
- [x] Fase 5: Photo Picker, consenso esplicito, sanitizzazione JPEG, Storage isolato, analisi OpenAI, cancellazione per sessione e retention massima di 7 giorni; flusso E2E collaudato con immagine sintetica.
- [x] Fase 6: hardening tecnico, gestione esplicita degli errori online, privacy backend, backup disattivato, App Check client, test Android, compilazione debug/release, audit Lint e installazione su moto g85 5G completati. Rimossi il motore e i contenuti fissi della prima versione; titolo, corpo e avvisi dipendono esclusivamente dalla diagnosi corrente. Gli adempimenti manuali pre-pubblicazione restano tracciati in `RELEASE_CHECKLIST.md`.
- [x] Fase 7: versione web pubblicata con chat, passaggi numerati, foto facoltative, contatti, orari, copyright, attribuzioni e privacy; app Android allineata con footer legale e suggerimento WhatsApp per esiti gialli o rossi.
- [x] APK tester `0.1.2` generato con R8, test Android e backend superati, SHA-256 documentato e sorgenti pubblicati su GitHub tramite pull request.

## Architettura proposta

Single-activity, UI Jetpack Compose, flusso dati unidirezionale e livelli `presentation → domain ← data`.
Il dominio non dipende da Android né Firebase. I repository sono interfacce di dominio e `FirebaseChatRepository` è l'adapter dell'assistente online. `BikeSafetyEngine` è locale,
deterministico e prevale sempre sulla risposta remota.

## Struttura

```text
app/src/main/java/org/bike4city/ciclofficinabot/
  core/designsystem/       tema, colori e componenti condivisi
  domain/model/            modelli ed enum di sicurezza
  domain/repository/       contratti repository
  data/repository/         persistenza locale
  presentation/            schermate e ViewModel per funzionalità
  navigation/              grafo e destinazioni
functions/                 backend TypeScript (fase 4)
```

## Dipendenze

Fase 1: AndroidX Core, Lifecycle, Activity Compose, Compose Material 3, Navigation Compose.
Fase 2: Coroutines, kotlinx.serialization, Room, DataStore, Hilt, Coil.
Fase 3: Firebase BoM (Auth, Firestore, Functions, Storage).

Le versioni sono allineate e verificate: AGP 9.2.1, Gradle 9.4.1, Kotlin/Compose compiler 2.3.10,
KSP 2.3.10, Compose BOM 2026.06.00 e Navigation 2.9.8.

## Modello dati

Entità principali: `BikeProfile`, `ChatSession`, `ChatMessage`, `DiagnosticStep`, `DiagnosisReport`,
`QuickReply`. Enum fondamentali: `SafetyLevel`, `DiagnosisOutcome`, `DiagnosisCategory`.
Room conserva sessioni, messaggi, report e stato della copia remota; DataStore conserva consenso di onboarding e profilo bici attivo.

## Fasi verificabili

1. Navigazione Splash → Onboarding → Home → Profilo bici → Chat → Report; build debug.
2. Safety engine, persistenza locale e test unitari/UI.
3. Adapter Firebase opzionale, emulatori e regole proprietario/admin.
4. Callable Function `bikeMechanicChat`, schema validato, rate limit e safety server-side.
5. Photo Picker, consenso esplicito, compressione e retention configurabile.
6. Audit TalkBack/contrasto/errori/offline e documentazione finale.

## Rischi tecnici

- Risposte AI non affidabili: schema chiuso, validazione e safety engine locale/server.
- Dati sensibili nelle foto: consenso, rimozione metadati, TTL e log minimali.
- Costi backend: limiti per messaggio/sessione/immagine e kill switch remoto.
- Dipendenza da Firebase: secondo tentativo automatico ed errore esplicito quando il servizio non risponde.
- API 36: Android Studio e SDK 36 devono essere installati; API 37 sarà valutata quando la coppia AGP/SDK sarà stabile nell'ambiente.

## Configurazioni manuali future

- Installare Android SDK 36 e usare il JDK 21 incorporato in Android Studio per Gradle.
- Aggiungere `google-services.json` solo quando si abilita Firebase (non versionato).
- Mantenere il secret del provider AI soltanto lato Cloud Functions e ruotarlo tramite Secret Manager quando necessario.
- Mantenere sincronizzati tra Android e web logo, contatti, orari, copyright e informativa privacy.
- Configurare il certificato di firma release e registrare Play Integrity/App Check prima di impostare `ENFORCE_APP_CHECK=true`.
- Preparare la versione `1.0.0`, l'Android App Bundle firmato e la scheda Google Play prima della pubblicazione sullo store.
