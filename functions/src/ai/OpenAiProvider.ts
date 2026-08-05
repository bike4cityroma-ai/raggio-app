import OpenAI from "openai";
import { zodTextFormat } from "openai/helpers/zod";
import type { AiProvider, AiRequestContext } from "./AiProvider";
import { mechanicResponseSchema, type MechanicResponse } from "../schema/chatSchema";
import { SYSTEM_PROMPT, SYSTEM_PROMPT_VERSION } from "../prompts/systemPrompt";

export class OpenAiProvider implements AiProvider {
  private readonly client: OpenAI;

  constructor(apiKey: string, private readonly model: string) {
    this.client = new OpenAI({ apiKey, timeout: 20_000, maxRetries: 1 });
  }

  async generate(context: AiRequestContext): Promise<MechanicResponse> {
    const { request, procedures, imageDataUrl, safetyIdentifier } = context;
    const response = await this.client.responses.parse({
      model: this.model,
      store: false,
      safety_identifier: safetyIdentifier,
      max_output_tokens: 1_200,
      input: [
        { role: "system", content: SYSTEM_PROMPT },
        {
          role: "system",
          content: JSON.stringify({
            promptVersion: SYSTEM_PROMPT_VERSION,
            approvedProcedures: procedures,
          }),
        },
        {
          role: "user",
          content: [
            { type: "input_text", text: JSON.stringify({
            sessionId: request.sessionId,
            message: request.message,
            category: request.category ?? null,
            messageCount: request.messageCount,
            recentHistory: request.history,
              hasImage: Boolean(imageDataUrl),
            }) },
            ...(imageDataUrl ? [{ type: "input_image" as const, image_url: imageDataUrl, detail: "low" as const }] : []),
          ],
        },
      ],
      text: {
        format: zodTextFormat(mechanicResponseSchema, "bike_mechanic_response"),
      },
    });

    if (!response.output_parsed) {
      throw new Error("OPENAI_STRUCTURED_OUTPUT_MISSING");
    }
    return mechanicResponseSchema.parse(response.output_parsed);
  }
}
