// Hand-written, mirroring backend/src/main/java/com/owl2gendl/{api/dto,reasoning,metrics}/*.
// TODO: generate these from the backend's live OpenAPI spec (/v3/api-docs) via openapi-typescript
// instead of hand-maintaining a second copy, once the contract stabilizes.

export interface ConstructDescriptor {
  id: string
  displayName: string
  description: string
  supportsNesting: boolean
  requiresIndividuals: boolean
  elEligible: boolean
}

export interface Category {
  id: string
  displayName: string
  constructs: ConstructDescriptor[]
}

export interface Catalog {
  categories: Category[]
}

/** Mirrors backend/src/main/java/com/owl2gendl/api/RequestValidator.MAX_TOTAL_REQUESTED_AXIOMS. */
export const MAX_TOTAL_REQUESTED_AXIOMS = 5000

export const TOPOLOGY_VARIANTS = ['CHAIN', 'BALANCED_TREE', 'UNIFORM_RANDOM', 'PREFERENTIAL'] as const
export type TopologyVariant = (typeof TOPOLOGY_VARIANTS)[number]

export const OUTPUT_FORMATS = ['RDFXML', 'TURTLE', 'OWLXML', 'MANCHESTER', 'FUNCTIONAL'] as const
export type OutputFormat = (typeof OUTPUT_FORMATS)[number]

// QL and RL are deliberately not offered yet — see backend ConstructId.elEligible doc comment.
export const TARGET_PROFILES = ['DL', 'EL'] as const
export type TargetProfile = (typeof TARGET_PROFILES)[number]

/** Each field is optional — leave any of them unset to let the backend size that pool automatically from
 *  whichever constructs are selected (see EntityCountsResolver on the backend). */
export interface EntityCounts {
  classes?: number
  objectProperties?: number
  dataProperties?: number
  individuals?: number
}

export interface StructureConfig {
  hierarchyTargetDepth: number
  hierarchyBranchingFactor: number
  nestingMaxDepth: number
  nestingProbability: number
}

export interface ReasoningConfig {
  timeoutSeconds: number
}

export interface GenerationRequest {
  entityCounts: EntityCounts
  constructs: Record<string, number>
  seed?: number
  structure: StructureConfig
  variants: TopologyVariant[]
  reasoning: ReasoningConfig
  targetProfile?: TargetProfile
  /** Opt-in: checks consistency after each selected construct type during generation (not just once at the
   *  end), rolling back and dropping ones that can't be made consistent after a few retries. Slower, but
   *  catches multi-hop logical inconsistencies no local rule can. Defaults to false server-side if omitted. */
  strictConsistency?: boolean
}

// --- Response shapes ---

export type RunStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED'
export type VerificationStatus = 'CONSISTENT' | 'INCONSISTENT' | 'NOT_VERIFIED_TIMEOUT' | 'NOT_VERIFIED_ERROR'

export interface ProfileCheckResult {
  el: boolean
  ql: boolean
  rl: boolean
  dl: boolean
}

export interface VerificationResult {
  status: VerificationStatus
  tier: 'OPENLLET'
  profile: ProfileCheckResult
  elapsedMillis: number
  /** null when no profile was requested (target DL) — the question doesn't apply. */
  requestedProfile: TargetProfile | null
  /** null when no profile was requested; otherwise whether `profile` actually satisfies `requestedProfile`. */
  profileGuaranteeSatisfied: boolean | null
}

export interface NestingMetrics {
  maxDepth: number
  avgDepth: number
}

export interface HierarchyMetrics {
  maxDepth: number
  avgBranchingFactor: number
  tangledness: number
}

export interface GraphMetrics {
  nodeCount: number
  edgeCount: number
  avgDegree: number
  clusteringCoefficient: number
}

export interface ReasoningMetrics {
  computed: boolean
  assertedSubClassOfCount: number
  inferredNewSubClassOfCount: number
  inferredToAssertedRatio: number
}

export interface MetricsReport {
  nesting: NestingMetrics
  hierarchy: HierarchyMetrics
  graph: GraphMetrics
  reasoning: ReasoningMetrics
}

export interface VariantResult {
  variantId: TopologyVariant
  status: RunStatus
  axiomCount: number | null
  errorMessage: string | null
  verification: VerificationResult | null
  metrics: MetricsReport | null
}

export interface GenerationJob {
  jobId: string
  status: RunStatus
  createdAt: string
  variants: VariantResult[]
  error: string | null
}

export const GRAPH_NODE_TYPES = ['CLASS', 'OBJECT_PROPERTY', 'DATA_PROPERTY', 'INDIVIDUAL', 'ANNOTATION_PROPERTY', 'DATATYPE', 'OTHER'] as const
export type GraphNodeType = (typeof GRAPH_NODE_TYPES)[number]

export interface GraphNode {
  id: string
  label: string
  type: GraphNodeType
}

export interface GraphEdge {
  id: string
  source: string
  target: string
  label: string
}

export interface OntologyGraph {
  nodes: GraphNode[]
  edges: GraphEdge[]
  truncated: boolean
}
