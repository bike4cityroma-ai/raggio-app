import { readFile } from "node:fs/promises";
import { resolve } from "node:path";

const projectId = "bike4city-ciclofficina";
const configPath = resolve(process.cwd(), "..", "app", "google-services.json");
const config = JSON.parse(await readFile(configPath, "utf8"));
const apiKey = config.client?.[0]?.api_key?.[0]?.current_key;
if (!apiKey) throw new Error("API key Firebase non trovata in app/google-services.json");

const authResponse = await fetch(`https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=${apiKey}`, {
  method: "POST",
  headers: { "content-type": "application/json" },
  body: JSON.stringify({ returnSecureToken: true }),
});
if (!authResponse.ok) throw new Error(`Auth anonima fallita: HTTP ${authResponse.status}`);
const { idToken, localId } = await authResponse.json();

async function callFunction(data, functionName = "bikeMechanicChat") {
  const response = await fetch(`https://europe-west1-${projectId}.cloudfunctions.net/${functionName}`, {
    method: "POST",
    headers: {
      authorization: `Bearer ${idToken}`,
      "content-type": "application/json",
    },
    body: JSON.stringify({ data }),
  });
  const body = await response.json();
  if (!response.ok || body.error) {
    throw new Error(`Callable fallita: HTTP ${response.status} ${JSON.stringify(body.error ?? body)}`);
  }
  return body.result;
}

const sessionId = `e2e-${Date.now()}`;
const safe = await callFunction({
  sessionId,
  message: "La catena è asciutta e fa un leggero rumore, ma resta in sede. Cosa posso controllare?",
  category: "TRANSMISSION",
  messageCount: 0,
  history: [],
});
if (safe.sessionId !== sessionId || safe.safetyLevel === "STOP" || !safe.assistantMessage) {
  throw new Error(`Risposta ordinaria non valida: ${JSON.stringify(safe)}`);
}
console.log(`OK risposta AI strutturata: ${safe.safetyLevel}/${safe.messageType}`);

const stop = await callFunction({
  sessionId: `${sessionId}-stop`,
  message: "La batteria è gonfia, molto calda e sento odore di bruciato.",
  category: "EBIKE",
  messageCount: 0,
  history: [],
});
if (stop.safetyLevel !== "STOP" || stop.outcome !== "RED" || !stop.requiresWorkshop) {
  throw new Error(`STOP server non valido: ${JSON.stringify(stop)}`);
}
console.log("OK arresto di sicurezza server: STOP/RED");

if (process.env.E2E_IMAGE_PATH || process.env.E2E_SYNTHETIC_IMAGE === "1") {
  const imageSessionId = `${sessionId}-image`;
  const imagePath = `ciclofficinaBotUsers/${localId}/sessions/${imageSessionId}/photos/test.jpg`;
  const imageBytes = process.env.E2E_IMAGE_PATH
    ? await readFile(process.env.E2E_IMAGE_PATH)
    : Buffer.from("/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////2wBDAf//////////////////////////////////////////////////////////////////////////////////////wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAX/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIQAxAAAAF//8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABBQJ//8QAFBEBAAAAAAAAAAAAAAAAAAAAAP/aAAgBAwEBPwF//8QAFBEBAAAAAAAAAAAAAAAAAAAAAP/aAAgBAgEBPwF//8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQAGPwJ//8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPyF//9oADAMBAAIAAwAAAB//xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oACAEDAQE/EB//xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oACAECAQE/EB//xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oACAEBAAE/EB//2Q==", "base64");
  const upload = await fetch(
    `https://firebasestorage.googleapis.com/v0/b/${projectId}.firebasestorage.app/o?uploadType=media&name=${encodeURIComponent(imagePath)}`,
    { method: "POST", headers: { authorization: `Bearer ${idToken}`, "content-type": "image/jpeg" }, body: imageBytes },
  );
  if (!upload.ok) throw new Error(`Upload immagine fallito: HTTP ${upload.status} ${await upload.text()}`);
  try {
    const vision = await callFunction({
      sessionId: imageSessionId,
      message: "Osserva questa immagine sintetica e dimmi se è sufficiente per una valutazione prudente.",
      category: "OTHER",
      messageCount: 0,
      history: [],
      imagePath,
    });
    if (!vision.assistantMessage || vision.sessionId !== imageSessionId) throw new Error("Risposta immagine non valida");
    console.log(`OK immagine: upload/analisi (${vision.safetyLevel})`);
  } finally {
    await callFunction({ sessionId: imageSessionId }, "deleteSessionPhotos");
    console.log("OK cancellazione immediata immagine");
  }
}
