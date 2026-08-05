import type { Firestore, QueryDocumentSnapshot } from "firebase-admin/firestore";
import { diagnosisCategories } from "../schema/chatSchema";

export interface ApprovedProcedure {
  id: string;
  title: string;
  category: string;
  steps: string[];
  warnings: string[];
}

const allowedCategories = new Set<string>(diagnosisCategories);

function text(value: unknown, max: number): string {
  return typeof value === "string" ? value.trim().slice(0, max) : "";
}

function textList(value: unknown, maxItems: number, maxLength: number): string[] {
  if (!Array.isArray(value)) return [];
  return value.map((item) => text(item, maxLength)).filter(Boolean).slice(0, maxItems);
}

function mapProcedure(document: QueryDocumentSnapshot): ApprovedProcedure | null {
  const data = document.data();
  const category = text(data.category, 32).toUpperCase();
  const title = text(data.title, 120);
  if (!title || !allowedCategories.has(category)) return null;
  return {
    id: document.id,
    title,
    category,
    steps: textList(data.steps, 10, 500),
    warnings: textList(data.warnings, 8, 240),
  };
}

export async function getApprovedProcedures(
  db: Firestore,
  category?: string | null,
): Promise<ApprovedProcedure[]> {
  let query = db.collection("mechanicProcedures").where("active", "==", true).limit(5);
  if (category && allowedCategories.has(category)) {
    query = query.where("category", "==", category);
  }
  const snapshot = await query.get();
  return snapshot.docs.map(mapProcedure).filter((item): item is ApprovedProcedure => item !== null);
}
