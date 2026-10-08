import { Checkbox, Group, Slider, Stack, Text, Title } from '@mantine/core'
import { useGenerationDraftStore } from '../state/generationDraftStore'
import { TOPOLOGY_VARIANTS, type TopologyVariant } from '../api/types'

const VARIANT_LABELS: Record<TopologyVariant, string> = {
  CHAIN: 'Chain',
  BALANCED_TREE: 'Balanced tree',
  UNIFORM_RANDOM: 'Uniform random',
  PREFERENTIAL: 'Preferential',
}

const VARIANT_DESCRIPTIONS: Record<TopologyVariant, string> = {
  // Depth claims here are verified against real measured output, not the strategy's name — "Chain" sounds
  // like it should build deep hierarchies, but it concentrates reuse onto one entity at a time, so it
  // measurably produces the shallowest, most tangled hierarchies of the four; "Balanced tree" sounds shallow
  // but spreading reuse evenly means less-used (often newer, deeper) branches keep getting extended instead
  // of already-popular shallow ones, so it measurably produces the deepest hierarchies of the four.
  CHAIN: 'Reuses the most recently created entity — concentrates reuse onto a few entities; measurably the shallowest, most tangled hierarchy of the four, not the deepest.',
  BALANCED_TREE: 'Reuses the least-used entity — spreads reuse evenly, which measurably produces the deepest hierarchy of the four variants.',
  UNIFORM_RANDOM: 'Reuses any entity with equal probability.',
  PREFERENTIAL: 'Biases toward already well-connected entities — hub-like, scale-free-ish structures.',
}

export function StructureControlsPanel() {
  const structure = useGenerationDraftStore((state) => state.structure)
  const setStructure = useGenerationDraftStore((state) => state.setStructure)
  const variants = useGenerationDraftStore((state) => state.variants)
  const setVariants = useGenerationDraftStore((state) => state.setVariants)

  const toggleVariant = (variant: TopologyVariant) => {
    if (variants.includes(variant)) {
      if (variants.length > 1) {
        setVariants(variants.filter((v) => v !== variant))
      }
    } else {
      setVariants([...variants, variant])
    }
  }

  return (
    <Stack gap="xl">
      <div>
        <Title order={4}>Class hierarchy shape</Title>
        <Text c="dimmed" size="sm" mb="sm">
          How deep and how branchy the generated class hierarchy should be. These are targets, not
          guarantees — each SubClassOf axiom can extend the hierarchy by at most one level, so achievable
          depth and branching are capped by how many SubClassOf occurrences you request below. A high target
          depth paired with a low SubClassOf count will fall well short of the target; check the achieved-vs-
          target figures in the results view after generating.
        </Text>
        <Stack gap="lg">
          <div>
            <Text size="sm">Target depth: {structure.hierarchyTargetDepth}</Text>
            <Slider
              min={1}
              max={12}
              value={structure.hierarchyTargetDepth}
              onChange={(value) => setStructure({ hierarchyTargetDepth: value })}
            />
          </div>
          <div>
            <Text size="sm">Branching factor: {structure.hierarchyBranchingFactor}</Text>
            <Slider
              min={1}
              max={10}
              value={structure.hierarchyBranchingFactor}
              onChange={(value) => setStructure({ hierarchyBranchingFactor: value })}
            />
          </div>
        </Stack>
      </div>

      <div>
        <Title order={4}>Axiom nesting</Title>
        <Text c="dimmed" size="sm" mb="sm">
          How often and how deeply class expressions (intersections, unions, restrictions) nest inside one
          another, instead of always being flat. This is structural embellishment — it does not consume your
          selected constructs' counts above.
        </Text>
        <Stack gap="lg">
          <div>
            <Text size="sm">Max nesting depth: {structure.nestingMaxDepth}</Text>
            <Slider
              min={0}
              max={5}
              value={structure.nestingMaxDepth}
              onChange={(value) => setStructure({ nestingMaxDepth: value })}
            />
          </div>
          <div>
            <Text size="sm">Nesting probability: {Math.round(structure.nestingProbability * 100)}%</Text>
            <Slider
              min={0}
              max={1}
              step={0.05}
              value={structure.nestingProbability}
              onChange={(value) => setStructure({ nestingProbability: value })}
              label={(value) => `${Math.round(value * 100)}%`}
            />
          </div>
        </Stack>
      </div>

      <div>
        <Title order={4}>Topology variant(s)</Title>
        <Text c="dimmed" size="sm" mb="sm">
          How entities get reused/wired together. Select more than one to compare structurally different
          ontologies generated from the exact same construct/count selection.
        </Text>
        <Stack gap="xs">
          {TOPOLOGY_VARIANTS.map((variant) => (
            <Checkbox
              key={variant}
              checked={variants.includes(variant)}
              onChange={() => toggleVariant(variant)}
              label={
                <Group gap="xs">
                  <Text size="sm">{VARIANT_LABELS[variant]}</Text>
                  <Text size="xs" c="dimmed">
                    — {VARIANT_DESCRIPTIONS[variant]}
                  </Text>
                </Group>
              }
            />
          ))}
        </Stack>
      </div>
    </Stack>
  )
}
