import { z } from "zod";

export const diagnosisCategories = [
  "TIRES",
  "BRAKES",
  "TRANSMISSION",
  "STEERING",
  "WHEELS",
  "EBIKE",
  "OTHER",
] as const;

export const chatRequestSchema = z.object({
  sessionId: z.string().trim().min(1).max(128),
  message: z.string().trim().min(1).max(1_000),
  category: z.enum(diagnosisCategories).nullable().optional(),
  messageCount: z.number().int().min(0).max(100),
  imagePath: z.string().trim().max(512).nullable().optional(),
  history: z.array(z.object({
    role: z.enum(["USER", "ASSISTANT"]),
    text: z.string().trim().min(1).max(1_000),
  }).strict()).max(20).default([]),
}).strict();

export const instructionSchema = z.object({
  title: z.string().max(120),
  body: z.string().max(1_200),
  warnings: z.array(z.string().max(240)).max(5),
}).strict();

export const reportUpdateSchema = z.object({
  summary: z.string().max(800),
  actions: z.array(z.string().max(240)).max(8),
  riskFlags: z.array(z.string().max(120)).max(8),
}).strict();

export const mechanicResponseSchema = z.object({
  sessionId: z.string().max(128),
  assistantMessage: z.string().min(1).max(1_500),
  messageType: z.enum(["QUESTION", "INSTRUCTION", "WARNING", "SUMMARY"]),
  diagnosisState: z.enum(["COLLECTING", "INSTRUCTION", "COMPLETED"]),
  safetyLevel: z.enum(["SAFE", "CAUTION", "STOP"]),
  outcome: z.enum(["UNDETERMINED", "GREEN", "YELLOW", "RED"]),
  category: z.enum(diagnosisCategories),
  probableCauses: z.array(z.string().max(180)).max(5),
  quickReplies: z.array(z.string().max(100)).max(5),
  instruction: instructionSchema.nullable(),
  reportUpdate: reportUpdateSchema,
  requiresWorkshop: z.boolean(),
  conversationCompleted: z.boolean(),
}).strict();

export type ChatRequest = z.infer<typeof chatRequestSchema>;
export type MechanicResponse = z.infer<typeof mechanicResponseSchema>;
