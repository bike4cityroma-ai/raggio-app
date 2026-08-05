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

test("il server non permette al modello di ridurre un rischio rosso", () => {
  const request: ChatRequest = { sessionId: "s-2", message: "rumore", category: "BRAKES", messageCount: 1, history: [] };
  const response: MechanicResponse = {
    sessionId: "errato", assistantMessage: "continua", messageType: "QUESTION",
    diagnosisState: "COLLECTING", safetyLevel: "SAFE", outcome: "RED", category: "BRAKES",
    probableCauses: [], quickReplies: [], instruction: null,
    reportUpdate: { summary: "", actions: [], riskFlags: [] },
    requiresWorkshop: false, conversationCompleted: false,
  };
  assert.equal(enforceServerSafety(request, response).safetyLevel, "STOP");
});
