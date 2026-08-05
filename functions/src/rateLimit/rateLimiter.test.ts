import assert from "node:assert/strict";
import test from "node:test";
import { decideRateLimit } from "./rateLimiter";

test("crea una nuova finestra", () => {
  const result = decideRateLimit(null, 1_000, 2);
  assert.equal(result.allowed, true);
  assert.deepEqual(result.next, { windowStartedAtMs: 1_000, count: 1 });
});

test("blocca oltre il limite e indica l'attesa", () => {
  const result = decideRateLimit({ windowStartedAtMs: 1_000, count: 2 }, 31_000, 2);
  assert.equal(result.allowed, false);
  assert.equal(result.retryAfterSeconds, 30);
});

test("riapre dopo sessanta secondi", () => {
  const result = decideRateLimit({ windowStartedAtMs: 1_000, count: 99 }, 61_000, 2);
  assert.equal(result.allowed, true);
  assert.equal(result.next.count, 1);
});
