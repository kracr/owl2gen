import { create } from 'zustand'
import type { EntityCounts, OutputFormat, StructureConfig, TargetProfile, TopologyVariant } from '../api/types'

interface GenerationDraftState {
  entityCounts: EntityCounts
  setEntityCounts: (counts: Partial<EntityCounts>) => void

  selectedConstructs: Record<string, number>
  setConstructCount: (constructId: string, count: number) => void
  toggleConstruct: (constructId: string, defaultCount?: number) => void
  clearConstructs: () => void

  targetProfile: TargetProfile
  /** deselectIds: construct ids to drop from the current selection because they became ineligible under
   *  the new profile — computed by the caller (which has catalog eligibility data), not the store. */
  setTargetProfile: (profile: TargetProfile, deselectIds?: string[]) => void

  structure: StructureConfig
  setStructure: (structure: Partial<StructureConfig>) => void

  variants: TopologyVariant[]
  setVariants: (variants: TopologyVariant[]) => void

  timeoutSeconds: number
  setTimeoutSeconds: (seconds: number) => void

  outputFormat: OutputFormat
  setOutputFormat: (format: OutputFormat) => void

  seed: number | undefined
  setSeed: (seed: number | undefined) => void

  strictConsistency: boolean
  setStrictConsistency: (strictConsistency: boolean) => void
}

export const useGenerationDraftStore = create<GenerationDraftState>((set) => ({
  // Empty by default — every field is left for the backend to size automatically from the selected
  // constructs (see EntityCountsResolver). Filling in a field pins it exactly; clearing it back out (passing
  // `undefined`) hands sizing back to the backend for that field.
  entityCounts: {},
  setEntityCounts: (counts) => set((state) => ({ entityCounts: { ...state.entityCounts, ...counts } })),

  selectedConstructs: {},
  setConstructCount: (constructId, count) =>
    set((state) => ({ selectedConstructs: { ...state.selectedConstructs, [constructId]: count } })),
  toggleConstruct: (constructId, defaultCount = 5) =>
    set((state) => {
      const next = { ...state.selectedConstructs }
      if (constructId in next) {
        delete next[constructId]
      } else {
        next[constructId] = defaultCount
      }
      return { selectedConstructs: next }
    }),
  clearConstructs: () => set({ selectedConstructs: {} }),

  targetProfile: 'DL',
  setTargetProfile: (targetProfile, deselectIds = []) =>
    set((state) => {
      if (deselectIds.length === 0) {
        return { targetProfile }
      }
      const next = { ...state.selectedConstructs }
      deselectIds.forEach((id) => delete next[id])
      return { targetProfile, selectedConstructs: next }
    }),

  structure: { hierarchyTargetDepth: 4, hierarchyBranchingFactor: 3, nestingMaxDepth: 2, nestingProbability: 0.3 },
  setStructure: (structure) => set((state) => ({ structure: { ...state.structure, ...structure } })),

  variants: ['UNIFORM_RANDOM'],
  setVariants: (variants) => set({ variants }),

  timeoutSeconds: 30,
  setTimeoutSeconds: (timeoutSeconds) => set({ timeoutSeconds }),

  outputFormat: 'RDFXML',
  setOutputFormat: (outputFormat) => set({ outputFormat }),

  seed: undefined,
  setSeed: (seed) => set({ seed }),

  strictConsistency: false,
  setStrictConsistency: (strictConsistency) => set({ strictConsistency }),
}))
