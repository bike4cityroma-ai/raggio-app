# Checklist release Bike4City Ciclofficina Bot

Ultimo aggiornamento: 13 agosto 2026

## Completato per il test pubblico

- [x] Repository Git inizializzato, modifiche pubblicate su GitHub e pull request unita in `main`.
- [x] APK tester `0.1.2` generato, offuscato con R8 e firmato con certificato di sviluppo.
- [x] Test Android e backend superati; SHA-256 dell'APK documentato.
- [x] Sito web Raggiò pubblicato su `https://raggio-ciclofficina.livio-lanni.chatgpt.site`.
- [x] Informativa privacy pubblicata e collegata dal sito.
- [x] Copyright, attribuzioni, disclaimer, loghi, indirizzo, orari e WhatsApp presenti su web e Android.
- [x] Passaggi operativi e avvertenze visualizzati nella chat web.
- [x] Suggerimento ciclofficina e WhatsApp mostrati per gli esiti gialli e rossi.

## Bloccanti prima della pubblicazione su Google Play

- Impostare la versione pubblica `1.0.0`, incrementare il `versionCode` e generare un Android App Bundle (`.aab`).
- Creare e custodire il keystore release; non usare il certificato debug condiviso.
- Registrare in Firebase l'impronta SHA-256 del certificato release.
- Collegare il progetto Google Play e registrare l'app in App Check con Play Integrity.
- Distribuire una build release a un canale di test e verificare le metriche App Check.
- Solo dopo la verifica, impostare `ENFORCE_APP_CHECK=true` e ridistribuire `bikeMechanicChat` e `deleteSessionPhotos`.
- Inserire in Play Console l'URL pubblico dell'informativa privacy.
- Preparare scheda Play Store: descrizione, icona 512×512, feature graphic 1024×500 e screenshot.
- Compilare in Play Console le dichiarazioni Privacy, Sicurezza dei dati, pubblico di destinazione e contenuti.
- Se l'account personale Play è stato creato dopo il 13 novembre 2023, completare il test chiuso richiesto da Google Play.
- Eseguire una prova TalkBack completa su telefono: onboarding, profilo, chat, foto, STOP, report, cronologia ed eliminazione.

## Verifiche già completate

- Compilazione Kotlin delle varianti debug e release con Firebase attivo.
- Test Android unitari e test backend.
- Android Lint senza errori.
- E2E Auth → Function → OpenAI e STOP server-side.
- E2E foto sintetica: upload, analisi e cancellazione immediata.
- Regole Firestore e Storage isolate dai percorsi della web app.
- Backup cloud e trasferimento dispositivo dei dati app disattivati.
- Loghi InControPedale e Bike4City, indirizzo e contatto WhatsApp inseriti nell'app.
- Orari di apertura e informativa privacy completa inseriti nell'app.
