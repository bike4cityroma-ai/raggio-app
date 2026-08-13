import type { ChatRequest, MechanicResponse } from "../schema/chatSchema";

const stopPatterns = [
  /fren[oi].*(non|più).*(funzion|frena|rallenta)/i,
  /leva.*(freno)?.*(manubrio|fondo corsa)/i,
  /ruota.*(non si arresta|non si ferma|non rallenta)/i,
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

type StopContext = Pick<ChatRequest, "sessionId" | "category"> & Partial<Pick<ChatRequest, "message">>;

export function stopResponse(request: StopContext): MechanicResponse {
  return {
    sessionId: request.sessionId,
    assistantMessage: contextualStopMessage(request.message ?? "", request.category ?? null),
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
    return stopResponse({ sessionId: request.sessionId, category: response.category, message: request.message });
  }
  return {
    ...response,
    sessionId: request.sessionId,
    safetyLevel: response.safetyLevel === "SAFE" && response.requiresWorkshop ? "CAUTION" : response.safetyLevel,
  };
}

function contextualStopMessage(message: string, category: ChatRequest["category"]): string {
  const normalized = message.normalize("NFKC").toLocaleLowerCase("it");
  const opening = "Ho rilevato un possibile pericolo. Non utilizzare la bicicletta.";

  // I segnali espliciti del messaggio prevalgono sempre sulla categoria stimata dal modello.
  if (/(batteria|motore|e-?bike|fumo|scintill|odore di bruciato)/i.test(normalized)) {
    return `${opening} Se la batteria è calda, gonfia, fumante o ha un odore anomalo, non ricaricarla e allontanati in sicurezza. Rivolgiti subito alla ciclofficina o ai soccorsi se c'è un rischio immediato.`;
  }
  if (/(fren|leva)|ruota.*(non si arresta|non si ferma|non rallenta)/i.test(normalized)) {
    return `${opening} La frenata non è efficace: non fare altre prove su strada e fai controllare subito freni, leve e cavi in ciclofficina.`;
  }
  if (/ragg(?:io|i).*(rott|spezz|manc)|(?:rott|spezz).*(?:raggio|raggi)/i.test(normalized)) {
    return `${opening} Il raggio rotto può rendere la ruota instabile o deformarla. Non toccare o tendere i raggi e fai controllare la ruota in ciclofficina.`;
  }
  if (/ruota.*(stacc|non fiss|allent|balla|gioco)|mozzo.*(allent|gioco)/i.test(normalized)) {
    return `${opening} La ruota potrebbe non essere fissata correttamente. Non tentare di serrarla durante l'uso e falla controllare in ciclofficina.`;
  }
  if (/(ruota|cerchio).*(deformat|piegat|stort|sfrega molto)/i.test(normalized)) {
    return `${opening} La ruota o il cerchio potrebbero essere deformati. Non proseguire e fai controllare centratura e integrità della ruota in ciclofficina.`;
  }
  if (/(sterzo|manubrio)/i.test(normalized)) {
    return `${opening} Sterzo o manubrio potrebbero non essere sicuri. Non tentare di riallinearli durante l'uso e fai controllare la bicicletta in ciclofficina.`;
  }
  if (/(telaio|forcella|crepa|lesione)/i.test(normalized)) {
    return `${opening} Telaio o forcella potrebbero essere danneggiati. Non sollecitare la zona e fai controllare la bicicletta in ciclofficina.`;
  }

  // La categoria viene usata solo se l'ultimo messaggio non contiene un segnale abbastanza preciso.
  if (category === "EBIKE") return `${opening} Il sistema elettrico richiede un controllo: spegni la e-bike, non ricaricarla e rivolgiti alla ciclofficina.`;
  if (category === "BRAKES") return `${opening} La frenata potrebbe essere compromessa. Non fare altre prove su strada e fai controllare i freni in ciclofficina.`;
  if (category === "WHEELS") return `${opening} La ruota presenta un possibile problema di sicurezza. Non proseguire e falla controllare in ciclofficina.`;
  if (category === "STEERING") return `${opening} Lo sterzo potrebbe non essere sicuro. Non proseguire e fallo controllare in ciclofficina.`;
  return `${opening} Non tentare altre prove o riparazioni e fai controllare la bicicletta in ciclofficina. Se c'è un rischio immediato, contatta i soccorsi.`;
}
