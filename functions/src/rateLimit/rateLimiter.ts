import { Timestamp, type Firestore } from "firebase-admin/firestore";

export interface RateState {
  windowStartedAtMs: number;
  count: number;
}

export interface RateDecision {
  allowed: boolean;
  next: RateState;
  retryAfterSeconds: number;
}

export function decideRateLimit(
  state: RateState | null,
  nowMs: number,
  limit: number,
  windowMs = 60_000,
): RateDecision {
  if (!state || nowMs - state.windowStartedAtMs >= windowMs || nowMs < state.windowStartedAtMs) {
    return { allowed: true, next: { windowStartedAtMs: nowMs, count: 1 }, retryAfterSeconds: 0 };
  }
  if (state.count >= limit) {
    return {
      allowed: false,
      next: state,
      retryAfterSeconds: Math.max(1, Math.ceil((state.windowStartedAtMs + windowMs - nowMs) / 1_000)),
    };
  }
  return {
    allowed: true,
    next: { windowStartedAtMs: state.windowStartedAtMs, count: state.count + 1 },
    retryAfterSeconds: 0,
  };
}

export async function consumeRateLimit(db: Firestore, uid: string, limit: number): Promise<RateDecision> {
  const reference = db.collection("ciclofficinaBotRateLimits").doc(uid);
  return db.runTransaction(async (transaction) => {
    const snapshot = await transaction.get(reference);
    const raw = snapshot.data();
    const started = raw?.windowStartedAt;
    const state = started instanceof Timestamp && typeof raw?.count === "number" ? {
      windowStartedAtMs: started.toMillis(),
      count: Math.max(0, Math.trunc(raw.count)),
    } : null;
    const nowMs = Date.now();
    const decision = decideRateLimit(state, nowMs, limit);
    if (decision.allowed) {
      transaction.set(reference, {
        windowStartedAt: Timestamp.fromMillis(decision.next.windowStartedAtMs),
        count: decision.next.count,
        updatedAt: Timestamp.fromMillis(nowMs),
      });
    }
    return decision;
  });
}
