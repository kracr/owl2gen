import { Card, SimpleGrid, Text } from '@mantine/core'
import { useGenerationDraftStore } from '../state/generationDraftStore'

/**
 * The "what was requested" summary tiles — shown on the Review step before submitting, and again on the
 * Results page (reading the same draft store, which isn't cleared on submit) so users can see what they
 * asked for right next to what they got, without navigating back.
 */
export function RequestSummary() {
  const draft = useGenerationDraftStore()
  const constructEntries = Object.entries(draft.selectedConstructs)
  const totalRequestedAxioms = constructEntries.reduce((sum, [, count]) => sum + count, 0)
  const { classes, objectProperties, dataProperties, individuals } = draft.entityCounts
  const allEntityCountsSet = [classes, objectProperties, dataProperties, individuals].every((v) => v !== undefined)
  const entitiesDisplay = allEntityCountsSet
    ? String((classes ?? 0) + (objectProperties ?? 0) + (dataProperties ?? 0) + (individuals ?? 0))
    : 'Auto'

  return (
    <SimpleGrid cols={{ base: 2, sm: 5 }}>
      <Card withBorder padding="sm">
        <Text size="xs" c="dimmed">
          Profile
        </Text>
        <Text size="lg" fw={600}>
          {draft.targetProfile}
        </Text>
      </Card>
      <Card withBorder padding="sm">
        <Text size="xs" c="dimmed">
          Entities
        </Text>
        <Text size="lg" fw={600}>
          {entitiesDisplay}
        </Text>
      </Card>
      <Card withBorder padding="sm">
        <Text size="xs" c="dimmed">
          Selected constructs
        </Text>
        <Text size="lg" fw={600}>
          {constructEntries.length}
        </Text>
      </Card>
      <Card withBorder padding="sm">
        <Text size="xs" c="dimmed">
          Requested axioms
        </Text>
        <Text size="lg" fw={600}>
          {totalRequestedAxioms}
        </Text>
      </Card>
      <Card withBorder padding="sm">
        <Text size="xs" c="dimmed">
          Variants
        </Text>
        <Text size="lg" fw={600}>
          {draft.variants.length}
        </Text>
      </Card>
    </SimpleGrid>
  )
}
