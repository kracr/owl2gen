import type { GenerationRequest } from '../api/types'
import type { useGenerationDraftStore } from './generationDraftStore'

type GenerationDraftState = ReturnType<typeof useGenerationDraftStore.getState>

/** Builds the POST /api/generations payload from the current draft store — shared by the initial submit
 *  (ReviewPanel) and by Regenerate on the Results page, so both stay in sync with the request shape. */
export function buildGenerationRequest(draft: GenerationDraftState): GenerationRequest {
  return {
    entityCounts: draft.entityCounts,
    constructs: draft.selectedConstructs,
    seed: draft.seed,
    structure: draft.structure,
    variants: draft.variants,
    reasoning: { timeoutSeconds: draft.timeoutSeconds },
    targetProfile: draft.targetProfile,
    strictConsistency: draft.strictConsistency,
  }
}
