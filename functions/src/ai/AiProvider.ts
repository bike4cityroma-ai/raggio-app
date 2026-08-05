import type { ChatRequest, MechanicResponse } from "../schema/chatSchema";
import type { ApprovedProcedure } from "../procedures/procedureRepository";

export interface AiRequestContext {
  request: ChatRequest;
  procedures: ApprovedProcedure[];
  imageDataUrl?: string;
  safetyIdentifier: string;
}

export interface AiProvider {
  generate(context: AiRequestContext): Promise<MechanicResponse>;
}
