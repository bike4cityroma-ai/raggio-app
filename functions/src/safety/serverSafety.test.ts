import assert from "node:assert/strict";
import test from "node:test";
import { enforceServerSafety, hasCriticalSignal, stopResponse } from "./serverSafety";
import type { ChatRequest, MechanicResponse } from "../schema/chatSchema";

test("rileva i segnali critici principali", () => {
  assert.equal(hasCriticalSignal("La batteria è gonfia e molto calda"), true);
  assert.equal(hasCriticalSignal("Il freno non rallenta più la bici"), true);
  assert.equal(hasCriticalSignal("La gomma è un po' morbida"), false);
});

test("lo STOP locale produce un esito rosso conclusivo", () => {
  const result = stopResponse({ sessionId: "s-1", category: "EBIKE" });
  assert.equal(result.sessionId, "s-1");
  assert.equal(result.safetyLevel, "STOP");
  assert.equal(result.outcome, "RED");
  assert.equal(result.conversationCompleted, true);
  assert.equal(result.instruction, null);
});

test("lo STOP per una ruota non mostra avvisi relativi alla batteria", () => {
  const result = stopResponse({ sessionId: "s-wheel", category: "WHEELS", message: "Vedo un raggio rotto" });
  assert.match(result.assistantMessage, /raggio rotto/i);
  assert.doesNotMatch(result.assistantMessage, /batteria/i);
});

test("il significato del messaggio prevale su una categoria ruote errata", () => {
  const result = stopResponse({ sessionId: "s-brake", category: "WHEELS", message: "No, la ruota non si arresta" });
  assert.match(result.assistantMessage, /frenata non è efficace/i);
  assert.doesNotMatch(result.assistantMessage, /raggio|raggi|cerchio/i);
});

test("distingue raggio rotto, ruota non fissata e cerchio deformato", () => {
  const spoke = stopResponse({ sessionId: "s-spoke", category: "WHEELS", message: "Vedo un raggio rotto" });
  const loose = stopResponse({ sessionId: "s-loose", category: "WHEELS", message: "La ruota balla e non è fissata" });
  const rim = stopResponse({ sessionId: "s-rim", category: "WHEELS", message: "Il cerchio è deformato" });
  assert.match(spoke.assistantMessage, /raggio rotto/i);
  assert.match(loose.assistantMessage, /non essere fissata correttamente/i);
  assert.match(rim.assistantMessage, /cerchio potrebbero essere deformati/i);
});

test("lo STOP per una e-bike mantiene l'avviso specifico sulla batteria", () => {
  const result = stopResponse({ sessionId: "s-ebike", category: "EBIKE", message: "La batteria è gonfia" });
  assert.match(result.assistantMessage, /batteria/i);
  assert.match(result.assistantMessage, /non ricaricarla/i);
});

test("il server non permette al modello di ridurre un rischio rosso", () => {
  const request: ChatRequest = { sessionId: "s-2", message: "rumore", category: "BRAKES", messageCount: 1, history: [] };
  const response: MechanicResponse = {
    sessionId: "errato", assistantMessage: "continua", messageType: "QUESTION",
    diagnosisState: "COLLECTING", safetyLevel: "SAFE", outcome: "RED", category: "BRAKES",
    probableCauses: [], quickReplies: [], instruction: null,
    reportUpdate: { summary: "", actions: [], riskFlags: [] },
    requiresWorkshop: false, conversationCompleted: false,
  };
  const result = enforceServerSafety(request, response);
  assert.equal(result.safetyLevel, "STOP");
  assert.doesNotMatch(result.assistantMessage, /batteria/i);
});
