import { execFileSync } from "node:child_process";
import { readFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const projectId = process.argv[2] ?? "bike4city-ciclofficina";
const scriptDir = dirname(fileURLToPath(import.meta.url));
const sourcePath = resolve(scriptDir, "..", "mechanic-procedures.json");
const procedures = JSON.parse(await readFile(sourcePath, "utf8"));

function firestoreValue(value) {
  if (typeof value === "string") return { stringValue: value };
  if (typeof value === "boolean") return { booleanValue: value };
  if (Array.isArray(value)) return { arrayValue: { values: value.map(firestoreValue) } };
  throw new TypeError(`Tipo Firestore non supportato: ${typeof value}`);
}

const accessToken = (process.platform === "win32"
  ? execFileSync(process.env.ComSpec ?? "cmd.exe", ["/d", "/s", "/c", "gcloud.cmd auth print-access-token"], {
      encoding: "utf8",
      windowsHide: true,
    })
  : execFileSync("gcloud", ["auth", "print-access-token"], {
      encoding: "utf8",
    })
).trim();

for (const { id, ...data } of procedures) {
  if (!/^[a-z0-9-]+$/.test(id)) throw new Error(`ID procedura non valido: ${id}`);
  const url = `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents/mechanicProcedures/${id}`;
  const response = await fetch(url, {
    method: "PATCH",
    headers: {
      authorization: `Bearer ${accessToken}`,
      "content-type": "application/json",
    },
    body: JSON.stringify({
      fields: Object.fromEntries(Object.entries(data).map(([key, value]) => [key, firestoreValue(value)])),
    }),
  });
  if (!response.ok) throw new Error(`${id}: Firestore HTTP ${response.status} ${await response.text()}`);
  console.log(`OK ${id}`);
}

console.log(`Caricate ${procedures.length} procedure in ${projectId}.`);
