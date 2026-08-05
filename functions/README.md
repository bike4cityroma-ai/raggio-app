# Bike4City AI Function

`bikeMechanicChat` è una callable Firebase v2 in `europe-west1`. Accetta solo utenti autenticati, valida il payload, applica un limite per utente e interroga OpenAI soltanto dopo i controlli di sicurezza.

## Configurazione locale

1. Eseguire `npm install` nella cartella `functions`.
2. Copiare `.env.example` in `.env.bike4city-ciclofficina` se servono valori diversi dai default.
3. Creare `functions/.secret.local` con `OPENAI_API_KEY=...` solo per l'emulatore. Il file è ignorato da Git.
4. Avviare dalla radice: `firebase emulators:start --only functions,auth,firestore`.

## Produzione

Impostare il segreto senza salvarlo nel progetto:

```text
firebase functions:secrets:set OPENAI_API_KEY
```

Poi distribuire esclusivamente la Function:

```text
firebase deploy --only functions:bikeMechanicChat
```

Le procedure approvate si trovano in `../firebase/mechanic-procedures.json`. Dalla radice del progetto
si possono aggiornare in modo idempotente con:

```text
node firebase/scripts/seed-procedures.mjs bike4city-ciclofficina
```

Per il collaudo reale (Auth anonima, risposta strutturata e STOP server-side), dalla cartella
`functions` eseguire `npm run test:e2e`. Il test legge soltanto la configurazione pubblica Firebase
da `app/google-services.json` e non legge il secret OpenAI.

`ENFORCE_APP_CHECK` resta `false` finché certificato release e Play Integrity non sono registrati e verificati. Prima della pubblicazione va attivato e collaudato su dispositivo. Le richieste OpenAI usano `store: false` e un `safety_identifier` pseudonimo. Il modello predefinito è `gpt-5.6-luna` ed è modificabile con il parametro `OPENAI_MODEL` senza cambiare il codice.

I log non includono testi, cronologia o dati personali; registrano soltanto durata, livello di sicurezza e codici tecnici.
