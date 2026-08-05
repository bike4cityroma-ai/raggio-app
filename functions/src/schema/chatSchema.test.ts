import assert from "node:assert/strict";
import test from "node:test";
import { chatRequestSchema, mechanicResponseSchema } from "./chatSchema";

test("accetta una richiesta minima valida", () => {
  const result = chatRequestSchema.safeParse({
    sessionId: "sessione-1",
    message: "La catena è uscita",
    messageCount: 0,
    history: [],
  });
  assert.equal(result.success, true);
});

test("rifiuta campi inattesi e messaggi troppo lunghi", () => {
  const result = chatRequestSchema.safeParse({
    sessionId: "sessione-1",
    message: "x".repeat(1_001),
    messageCount: 0,
    history: [],
    admin: true,
  });
  assert.equal(result.success, false);
});

test("accetta un solo percorso immagine limitato", () => {
  const valid = chatRequestSchema.safeParse({
    sessionId: "sessione-1",
    message: "Controlla questa foto",
    messageCount: 0,
    history: [],
    imagePath: "ciclofficinaBotUsers/u/sessions/sessione-1/photos/p.jpg",
  });
  const tooLong = chatRequestSchema.safeParse({
    sessionId: "sessione-1",
    message: "Controlla questa foto",
    messageCount: 0,
    history: [],
    imagePath: "x".repeat(513),
  });
  assert.equal(valid.success, true);
  assert.equal(tooLong.success, false);
});

test("rifiuta output incompleti del provider", () => {
  const result = mechanicResponseSchema.safeParse({ assistantMessage: "test" });
  assert.equal(result.success, false);
});
