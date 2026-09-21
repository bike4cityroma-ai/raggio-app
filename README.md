# BIKE4CITY Ciclofficina Bot

Prima interfaccia Android per guidare utenti non esperti nell'identificazione prudente di piccoli problemi
della bicicletta. Non sostituisce un meccanico e interrompe il flusso quando rileva segnali critici.

## Stato attuale

L'app fornisce il tema Bike4City, onboarding, home, profilo bici, diagnosi guidata online,
regole di sicurezza, schermata STOP, riepilogo e cronologia locale. Sessioni, messaggi e report sono
persistiti con Room; consenso e profilo bici sono salvati con DataStore. Build debug/release, KSP,
I test Android e backend, la compilazione e Lint sono stati verificati. L'APK tester aggiornato
`0.1.2` è disponibile in `dist/`, offuscato con R8 e firmato con il certificato di sviluppo.

Dal riepilogo l'utente può scegliere esplicitamente di salvare una copia su Firebase, verificarne
lo stato ed eliminarla. Room resta la sorgente offline principale e la cronologia coordina la
cancellazione locale e remota.

Firebase Auth/Firestore e collegato al progetto esistente con dati Android isolati dalla web app.
L'adattatore AI remoto e l'analisi facoltativa delle foto sono implementati. Function, regole Storage
e pulizia automatica sono pubblicate e il collaudo E2E è stato superato.

## Prerequisiti

- Android Studio 2025.3.4 o compatibile con AGP 9.2
- JDK 21 incorporato in Android Studio e configurato come Gradle JDK
- Android SDK 36 / Build Tools 36+

Aprire questa cartella in Android Studio, attendere il Gradle Sync e avviare la configurazione `app` su
un emulatore API 23 o superiore.

## Comandi PowerShell

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```

L'APK debug viene generato in `app\build\outputs\apk\debug\app-debug.apk`.

## Prova su telefono Android

1. Installa e apri Android Studio, scegli **Open** e seleziona questa cartella.
2. In **Settings → Build, Execution, Deployment → Build Tools → Gradle**, seleziona il JDK incorporato di Android Studio (17 o 21), non il JDK 25 di sistema.
3. Sul telefono apri **Impostazioni → Informazioni sul telefono** e tocca sette volte **Numero build**.
4. In **Opzioni sviluppatore** abilita **Debug USB**.
5. Collega il telefono con un cavo dati, sbloccalo e accetta l'impronta RSA del computer.
6. Seleziona il dispositivo nella barra superiore di Android Studio e premi **Run ▶**.

Da Android 11 è disponibile anche **Pair Devices Using Wi-Fi** nel Device Manager, con computer e
telefono sulla stessa rete. Per il primo test il cavo USB è la scelta più semplice.

## Firebase (sincronizzazione facoltativa attiva)

L'app Android `org.bike4city.ciclofficinabot` è registrata nel progetto esistente
`bike4city-ciclofficina`. Authentication anonima è abilitata, `google-services.json` è presente
soltanto in locale e il database Firestore condiviso si trova in `europe-west1`.

La web app conserva le proprie raccolte e regole. Android scrive soltanto nel percorso isolato
`ciclofficinaBotUsers/{uid}/sessions/{sessionId}`. Le regole combinate sono state validate con
l'emulatore e pubblicate sul progetto reale il 3 agosto 2026.

Per i test locali avviare `firebase.cmd emulators:start --only auth,firestore` e consultare
`firebase/README.md` per host e porte su emulatore o telefono fisico.

Senza `google-services.json` la versione definitiva non può essere avviata. Quando il file è presente, Gradle applica automaticamente Google Services e include
gli SDK Firebase.

L'invio non è automatico: richiede una conferma che descrive i campi trasmessi. Lo stato
`LOCAL_ONLY / SYNCED / FAILED` è persistito in Room; eliminando una conversazione sincronizzata,
la copia Firebase viene rimossa prima dei dati locali.

Il secret AI viene configurato soltanto lato server con Secret Manager, mai nell'app o in BuildConfig.
Le build debug inizializzano il provider App Check di sviluppo; le build release usano Play Integrity.
L'enforcement server resta disattivato finché certificato release e provider non sono registrati e verificati.

## Assistente AI remoto (distribuito e collaudato)

La callable v2 `bikeMechanicChat` si trova in `functions/` e usa Node.js 22, TypeScript, OpenAI
Responses API e output strutturato. Prima del provider verifica autenticazione, schema, limite per utente
e segnali critici. I controlli STOP sono ripetuti anche sul server e nell'app Android.

Per compilare e testare il backend:

```powershell
cd functions
npm install
npm test
npm run test:e2e
```

Impostando `E2E_IMAGE_PATH` con il percorso di un JPEG sintetico, il test verifica anche upload,
analisi visiva e cancellazione immediata. Non usare fotografie personali nei test automatici.

Le sette procedure meccaniche approvate sono versionate in `firebase/mechanic-procedures.json` e
caricate nella raccolta server-only `mechanicProcedures` con:

```powershell
node firebase/scripts/seed-procedures.mjs bike4city-ciclofficina
```

Lo script è idempotente e usa l'account attivo di Google Cloud; la raccolta non è accessibile
direttamente ai client Android. Il modello predefinito è `gpt-5.6-luna`, scelto per contenere i costi
del carico interattivo e modificabile come parametro Firebase senza cambiare il codice.

Per generare una build debug, dopo aver configurato e distribuito la Function:

```powershell
.\gradlew.bat assembleDebug
```

Se la chiamata remota fallisce, l'app effettua un secondo tentativo e poi mostra un errore esplicito senza produrre una diagnosi sostitutiva. Le istruzioni per Secret Manager, emulatore e deploy selettivo sono in `functions/README.md`.

Il deploy della sola AI userà `firebase deploy --only functions:bikeMechanicChat`; non modifica le regole
Firestore della web app.

## Versione web pubblica

La versione web di Raggiò è pubblica all'indirizzo:

<https://raggio-ciclofficina.livio-lanni.chatgpt.site>

La chat web usa lo stesso servizio Firebase dell'app Android e mostra messaggi, risposte rapide,
istruzioni operative numerate e avvertenze. Include caricamento facoltativo delle foto con consenso,
contatto WhatsApp per gli esiti che richiedono la ciclofficina, indirizzo, orari, copyright,
attribuzioni tecnologiche e informativa privacy pubblica. Il codice del sito vive in `web/`, che
mantiene un repository e un ciclo di pubblicazione Sites separati dal repository Android/backend.

## Pacchetto tester

- APK: `dist/RAGGIO-tester-v0.1.2.apk`
- Versione: `0.1.2` (`versionCode 3`)
- SHA-256: `833C45403B41561E3861AAAA5FB91306F21878E8C023FF935C3CAC8B6C429591`
- Istruzioni: `dist/ISTRUZIONI_TESTER.md`

L'APK tester non è una build destinata a Google Play: usa il certificato di sviluppo e sarà
sostituito da un Android App Bundle firmato con il keystore release.

## Struttura

Vedere [PLAN.md](PLAN.md) per architettura, fasi, modello dati e rischi. Il package segue i livelli
`core`, `domain`, `data`, `presentation`, `navigation`.

## Sicurezza e privacy

Non usare la bici in presenza di problemi a freni, ruote, sterzo, telaio, forcella o batteria.
Nessuna posizione, pubblicità o tracciamento è previsto. Backup e trasferimento dei dati diagnostici
sono disattivati. Le richieste OpenAI usano `store: false` e un identificatore di sicurezza pseudonimo.
Logo, contatti, indirizzo, orari, copyright e informativa privacy sono presenti nell'app e nel sito.
Il titolare indicato è Ciclofficina InControPedale e Bike4City; Firebase di Google e OpenAI sono
attribuiti come fornitori tecnologici. Raggiò resta un primo orientamento e non sostituisce il
controllo di un meccanico qualificato.

## Problemi noti / prossimi passi

- Sessioni, messaggi e report sono persistenti in Room e consultabili/eliminabili dalla cronologia.
- Consenso di onboarding e profilo bici attivo sono persistenti in DataStore.
- Foto collegate con consenso, sanitizzazione, cancellazione immediata e retention massima di 7 giorni.
- Sito pubblico, contatti, orari, loghi, copyright, attribuzioni e informativa privacy sono disponibili.
- APK tester `0.1.2` generato, verificato e versionato con istruzioni e SHA-256.
- Il contatto WhatsApp viene proposto al termine delle diagnosi gialle o rosse.
- Cloud Function e adapter AI sono distribuiti; sette procedure meccaniche approvate sono presenti in Firestore.
- Il percorso E2E Auth anonima → Function → OpenAI è verificato, inclusi output strutturato e STOP server-side.
- Photo Picker, consenso, rimozione metadati, limite 1,25 MB, analisi visiva e retention massima di 7 giorni sono attivi.
- Hardening tecnico e audit Lint completati; restano i punti manuali elencati in `RELEASE_CHECKLIST.md`.
