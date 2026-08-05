import type { ChatRequest, MechanicResponse } from "../schema/chatSchema";

const stopPatterns = [
  /fren[oi].*(non|pi[uù]).*(funzion|frena|rallenta)/i,
  /leva.*(freno)?.*(manubrio|fondo corsa)/i,
  /(ruota|sterzo|manubrio).*(stacc|allent|instabil|balla|gioco forte)/i,
  /(telaio|forcella).*(crep|lesion|rott|piegat)/i,
  /batteria.*(gonfi|cald|fum|fumo|bruc|perdit|odore)/i,
  /(fumo|scintill|odore di bruciato).*(batteria|motore|e-?bike)/i,
  /(incendio|fiamma).*(bici|batteria|e-?bike)/i,
];

export function hasCriticalSignal(message: string): boolean {
  const normalized = message.normalize("NFKC").replace(/\s+/g, " ").trim();
  return stopPatterns.some((pattern) => pattern.test(normalized));
}

export function stopResponse(request: Pick<ChatRequest, "sessionId" | "category">): MechanicResponse {
  return {
    sessionId: request.sessionId,
    assistantMessage: "Ho rilevato un possibile pericolo. Non utilizzare la bicicletta. Se riguarda una batteria calda, gonfia, fumante o con odore anomalo, non ricaricarla e allontanati in sicurezza. Rivolgiti subito alla ciclofficina o ai soccorsi se c'è un rischio immediato.",
    messageType: "WARNING",
    diagnosisState: "COMPLETED",
    safetyLevel: "STOP",
    outcome: "RED",
    category: request.category ?? "OTHER",
    probableCauses: [],
    quickReplies: [],
    instruction: null,
    reportUpdate: {
      summary: "Segnale critico rilevato: utilizzo della bicicletta interrotto.",
      actions: ["Non utilizzare la bicicletta", "Richiedere una verifica in ciclofficina"],
      riskFlags: ["STOP_DI_SICUREZZA"],
    },
    requiresWorkshop: true,
    conversationCompleted: true,
  };
}

export function enforceServerSafety(request: ChatRequest, response: MechanicResponse): MechanicResponse {
  if (hasCriticalSignal(request.message) || response.safetyLevel === "STOP" || response.outcome === "RED") {
    return stopResponse({ sessionId: request.sessionId, category: response.category });
  }
  return {
    ...response,
    sessionId: request.sessionId,
    safetyLevel: response.safetyLevel === "SAFE" && response.requiresWorkshop ? "CAUTION" : response.safetyLevel,
  };
}
