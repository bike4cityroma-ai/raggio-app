# Firebase Android e convivenza con la web app

La versione Android definitiva richiede la configurazione Firebase. Quando e presente
`app/google-services.json`, la build include anche Firebase Auth e Firestore.

## Configurazione reale

L'app Android e registrata nel progetto Firebase esistente `bike4city-ciclofficina`
con package `org.bike4city.ciclofficinabot`. Authentication anonima e abilitata e il
database Firestore esistente si trova in `europe-west1`.

`google-services.json` e `.firebaserc` sono locali e ignorati da Git. Non inserire
mai API key private aggiuntive, credenziali server o secret AI nell'APK.

## Emulatori

Per i test locali eseguire:

```powershell
firebase.cmd emulators:start --only auth,firestore
```

Sul simulatore Android l'host e `10.0.2.2`. Su un telefono fisico usare l'indirizzo
LAN del PC e consentire le porte 9099 e 8080 soltanto sulla rete privata.

## Separazione e sicurezza

La web app mantiene le proprie raccolte e tutte le regole basate su utenti attivi e
ruoli. L'app Android usa esclusivamente:

```text
ciclofficinaBotUsers/{uid}/sessions/{sessionId}
```

Un utente autenticato puo accedere soltanto ai propri report. Il report remoto accetta
solo campi previsti, enum chiusi, timestamp e dimensioni limitate. Il `default deny`
continua a proteggere ogni altro percorso.

Il 3 agosto 2026 un test end-to-end sul progetto reale ha verificato scrittura e lettura
del proprietario, rifiuto dell'accesso da un secondo utente anonimo e isolamento dalla
raccolta `users` della web app. Documento e account temporanei sono stati eliminati.

## Foto diagnostiche

Le fotografie usano il percorso Storage isolato
`ciclofficinaBotUsers/{uid}/sessions/{sessionId}/photos/{photoId}.jpg`. Il client ricodifica
la foto selezionata in JPEG, rimuove i metadati e applica i limiti di 1280 px e 1,25 MB prima
dell'upload autorizzato dall'utente. `deleteSessionPhotos` elimina subito gli oggetti quando
viene eliminata la conversazione; `cleanupExpiredPhotos` usa una soglia di 6 giorni durante
l'esecuzione giornaliera, garantendo una permanenza massima inferiore o uguale a 7 giorni.
Le regole Storage preesistenti della web app sono state conservate.
