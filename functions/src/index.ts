import { initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { getStorage } from "firebase-admin/storage";
import { createHash } from "node:crypto";
import OpenAI from "openai";
import * as logger from "firebase-functions/logger";
import { defineBoolean, defineInt, defineSecret, defineString } from "firebase-functions/params";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { ZodError } from "zod";
import { OpenAiProvider } from "./ai/OpenAiProvider";
import { getApprovedProcedures } from "./procedures/procedureRepository";
import { SYSTEM_PROMPT_VERSION } from "./prompts/systemPrompt";
import { consumeRateLimit } from "./rateLimit/rateLimiter";
import { chatRequestSchema } from "./schema/chatSchema";
import { enforceServerSafety, hasCriticalSignal, stopResponse } from "./safety/serverSafety";

initializeApp();
const db = getFirestore();
const bucket = getStorage().bucket("bike4city-ciclofficina.firebasestorage.app");

const openAiApiKey = defineSecret("OPENAI_API_KEY");
const openAiModel = defineString("OPENAI_MODEL", { default: "gpt-5.6-luna" });
const botEnabled = defineBoolean("BOT_ENABLED", { default: true });
const rateLimitPerMinute = defineInt("BOT_RATE_LIMIT_PER_MINUTE", { default: 10 });
const maxMessagesPerSession = defineInt("BOT_MAX_MESSAGES_PER_SESSION", { default: 30 });
const enforceAppCheck = defineBoolean("ENFORCE_APP_CHECK", { default: false });

const analyticsEvents = [
  "app_open",
  "diagnosis_started",
  "diagnosis_completed",
  "photo_used",
  "workshop_recommended",
  "whatsapp_clicked",
] as const;

const analyticsSources = [
  "direct",
  "sito",
  "card",
  "locandina",
  "gazebo",
  "facebook",
  "whatsapp",
  "altro",
] as const;

type AnalyticsEvent = typeof analyticsEvents[number];
type AnalyticsSource = typeof analyticsSources[number];

function isAnalyticsEvent(value: unknown): value is AnalyticsEvent {
  return typeof value === "string" && analyticsEvents.includes(value as AnalyticsEvent);
}

function isAnalyticsSource(value: unknown): value is AnalyticsSource {
  return typeof value === "string" && analyticsSources.includes(value as AnalyticsSource);
}

function romePeriodKeys(now = new Date()): { day: string; month: string } {
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: "Europe/Rome",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).formatToParts(now);
  const values = Object.fromEntries(parts.map((part) => [part.type, part.value]));
  const day = `${values.year}-${values.month}-${values.day}`;
  return { day, month: `${values.year}-${values.month}` };
}

export const recordRaggioAnalytics = onCall({
  region: "europe-west1",
  timeoutSeconds: 15,
  memory: "256MiB",
  minInstances: 0,
  maxInstances: 3,
  concurrency: 40,
  enforceAppCheck,
}, async (call) => {
  if (!call.auth?.uid) {
    throw new HttpsError("unauthenticated", "Autenticazione necessaria.");
  }

  const event = call.data?.event;
  const source = call.data?.source ?? "direct";
  if (!isAnalyticsEvent(event) || !isAnalyticsSource(source)) {
    throw new HttpsError("invalid-argument", "Evento statistico non valido.");
  }

  const { day, month } = romePeriodKeys();
  const eventIncrement = FieldValue.increment(1);
  const sourceIncrement = FieldValue.increment(1);
  const baseData = {
    events: { [event]: eventIncrement },
    updatedAt: FieldValue.serverTimestamp(),
  };
  const data = event === "app_open"
    ? { ...baseData, sources: { [source]: sourceIncrement } }
    : baseData;

  const batch = db.batch();
  batch.set(db.collection("raggioAnalytics").doc(`day_${day}`), {
    periodType: "day",
    period: day,
    ...data,
  }, { merge: true });
  batch.set(db.collection("raggioAnalytics").doc(`month_${month}`), {
    periodType: "month",
    period: month,
    ...data,
  }, { merge: true });
  batch.set(db.collection("raggioAnalytics").doc("total"), {
    periodType: "total",
    period: "all",
    ...data,
  }, { merge: true });
  await batch.commit();

  return { recorded: true };
});

export const bikeMechanicChat = onCall({
  region: "europe-west1",
  timeoutSeconds: 60,
  memory: "512MiB",
  minInstances: 0,
  maxInstances: 5,
  concurrency: 20,
  secrets: [openAiApiKey],
  enforceAppCheck,
}, async (call) => {
  const startedAt = Date.now();
  if (!call.auth?.uid) {
    throw new HttpsError("unauthenticated", "È necessario autenticarsi per usare l'assistente.");
  }
  if (!botEnabled.value()) {
    throw new HttpsError("failed-precondition", "L'assistente è temporaneamente disattivato.");
  }

  const parsed = chatRequestSchema.safeParse(call.data);
  if (!parsed.success) {
    throw new HttpsError("invalid-argument", "Richiesta non valida.", {
      fields: parsed.error.issues.map((issue) => issue.path.join(".")).slice(0, 8),
    });
  }
  const request = parsed.data;

  // Gli avvisi di sicurezza devono prevalere anche sui limiti di sessione e frequenza.
  if (hasCriticalSignal(request.message)) {
    logger.warn("bikeMechanicChat safety stop", { durationMs: Date.now() - startedAt });
    return stopResponse(request);
  }

  if (request.messageCount >= maxMessagesPerSession.value()) {
    throw new HttpsError("resource-exhausted", "Limite messaggi della sessione raggiunto.");
  }

  const decision = await consumeRateLimit(db, call.auth.uid, rateLimitPerMinute.value());
  if (!decision.allowed) {
    throw new HttpsError("resource-exhausted", "Troppe richieste. Riprova tra poco.", {
      retryAfterSeconds: decision.retryAfterSeconds,
    });
  }

  try {
    const procedures = await getApprovedProcedures(db, request.category);
    const imageDataUrl = request.imagePath
      ? await loadOwnedImage(call.auth.uid, request.sessionId, request.imagePath)
      : undefined;
    const provider = new OpenAiProvider(openAiApiKey.value(), openAiModel.value());
    const safetyIdentifier = createHash("sha256").update(call.auth.uid).digest("hex");
    const raw = await provider.generate({ request, procedures, imageDataUrl, safetyIdentifier });
    const result = enforceServerSafety(request, raw);
    logger.info("bikeMechanicChat completed", {
      durationMs: Date.now() - startedAt,
      safetyLevel: result.safetyLevel,
      procedureCount: procedures.length,
      promptVersion: SYSTEM_PROMPT_VERSION,
    });
    return result;
  } catch (error: unknown) {
    if (error instanceof HttpsError) throw error;
    if (error instanceof ZodError) {
      logger.error("bikeMechanicChat invalid provider output", { issueCount: error.issues.length });
      throw new HttpsError("internal", "La risposta dell'assistente non è valida.");
    }
    const diagnostic = error instanceof OpenAI.APIError
      ? { code: error.code ?? "OPENAI_API_ERROR", status: error.status, type: error.type }
      : { code: error instanceof Error ? error.name : "UNKNOWN" };
    logger.error("bikeMechanicChat provider failure", {
      ...diagnostic,
      durationMs: Date.now() - startedAt,
    });
    throw new HttpsError("unavailable", "Assistente temporaneamente non disponibile. Attendi qualche secondo e riprova.");
  }
});

const sessionSchema = chatRequestSchema.pick({ sessionId: true });

export const deleteSessionPhotos = onCall({
  region: "europe-west1",
  timeoutSeconds: 30,
  enforceAppCheck,
}, async (call) => {
  if (!call.auth?.uid) throw new HttpsError("unauthenticated", "È necessario autenticarsi.");
  const parsed = sessionSchema.safeParse(call.data);
  if (!parsed.success) throw new HttpsError("invalid-argument", "Sessione non valida.");
  const prefix = `ciclofficinaBotUsers/${call.auth.uid}/sessions/${parsed.data.sessionId}/photos/`;
  await bucket.deleteFiles({ prefix, force: true });
  return { deleted: true };
});

export const cleanupExpiredPhotos = onSchedule({
  region: "europe-west1",
  schedule: "every day 03:15",
  timeZone: "Europe/Rome",
  timeoutSeconds: 300,
}, async () => {
  const [files] = await bucket.getFiles({ prefix: "ciclofficinaBotUsers/" });
  // Con esecuzione giornaliera, la soglia a 6 giorni garantisce una retention reale <= 7 giorni.
  const cutoff = Date.now() - 6 * 24 * 60 * 60 * 1_000;
  const expired = files.filter((file) => {
    const created = Date.parse(file.metadata.timeCreated ?? "");
    return Number.isFinite(created) && created < cutoff && file.name.includes("/photos/");
  });
  await Promise.all(expired.map((file) => file.delete({ ignoreNotFound: true })));
  logger.info("expired photos cleanup", { deletedCount: expired.length });
});

async function loadOwnedImage(uid: string, sessionId: string, imagePath: string): Promise<string> {
  const expectedPrefix = `ciclofficinaBotUsers/${uid}/sessions/${sessionId}/photos/`;
  if (!imagePath.startsWith(expectedPrefix) || !imagePath.endsWith(".jpg")) {
    throw new HttpsError("permission-denied", "Percorso immagine non autorizzato.");
  }
  const file = bucket.file(imagePath);
  const [metadata] = await file.getMetadata();
  const size = Number(metadata.size ?? 0);
  if (metadata.contentType !== "image/jpeg" || size < 1 || size > 1_250_000) {
    throw new HttpsError("invalid-argument", "Immagine non valida.");
  }
  const [bytes] = await file.download();
  return `data:image/jpeg;base64,${bytes.toString("base64")}`;
}
