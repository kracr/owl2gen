import { Accordion, Alert, Badge, Button, Group, Loader, SegmentedControl, SimpleGrid, Stack, Text, Title } from '@mantine/core'
import { useCatalog } from './useCatalog'
import { useGenerationDraftStore } from '../state/generationDraftStore'
import { ConstructRow } from './ConstructRow'
import { extractErrorMessage } from '../api/errors'
import type { Category, TargetProfile } from '../api/types'

const PROFILE_OPTIONS: { value: TargetProfile; label: string }[] = [
  { value: 'DL', label: 'DL (no restriction)' },
  { value: 'EL', label: 'EL' },
]

const isEligible = (profile: TargetProfile, elEligible: boolean) => profile === 'DL' || elEligible

/**
 * Renders every OWL2 construct grouped by category, entirely from GET /api/catalog — there is no
 * hardcoded construct list here. Selection state lives in the shared generation draft store so the
 * other wizard steps (structure, reasoning, review) all see the same picks.
 */
export function ConstructPickerPage() {
  const { data, isLoading, isError, error } = useCatalog()
  const selectedConstructs = useGenerationDraftStore((state) => state.selectedConstructs)
  const setConstructCount = useGenerationDraftStore((state) => state.setConstructCount)
  const toggleConstruct = useGenerationDraftStore((state) => state.toggleConstruct)
  const targetProfile = useGenerationDraftStore((state) => state.targetProfile)
  const setTargetProfile = useGenerationDraftStore((state) => state.setTargetProfile)

  if (isLoading) {
    return (
      <Group justify="center" p="xl">
        <Loader />
      </Group>
    )
  }

  if (isError) {
    return (
      <Alert color="red" title="Could not load construct catalog" m="md">
        {extractErrorMessage(error)}
      </Alert>
    )
  }

  const selectAllInCategory = (category: Category) => {
    category.constructs.forEach((construct) => {
      if (!(construct.id in selectedConstructs) && isEligible(targetProfile, construct.elEligible)) {
        setConstructCount(construct.id, 5)
      }
    })
  }

  const clearAllInCategory = (category: Category) => {
    category.constructs.forEach((construct) => {
      if (construct.id in selectedConstructs) {
        toggleConstruct(construct.id)
      }
    })
  }

  const selectAllOverall = () => {
    data?.categories.forEach((category) => selectAllInCategory(category))
  }

  const clearAllOverall = () => {
    data?.categories
      .flatMap((category) => category.constructs)
      .forEach((construct) => {
        if (construct.id in selectedConstructs) {
          toggleConstruct(construct.id)
        }
      })
  }

  const selectedCountInCategory = (category: Category) =>
    category.constructs.filter((construct) => construct.id in selectedConstructs).length

  const handleProfileChange = (value: string) => {
    const profile = value as TargetProfile
    const deselectIds =
      profile === 'EL'
        ? (data?.categories.flatMap((category) => category.constructs) ?? [])
            .filter((construct) => construct.id in selectedConstructs && !construct.elEligible)
            .map((construct) => construct.id)
        : []
    setTargetProfile(profile, deselectIds)
  }

  const totalConstructs = data?.categories.flatMap((category) => category.constructs).length ?? 0
  const totalSelected = Object.keys(selectedConstructs).length

  const categories = data?.categories ?? []
  const midpoint = Math.ceil(categories.length / 2)
  const columns = [categories.slice(0, midpoint), categories.slice(midpoint)]

  const renderCategoryColumn = (columnCategories: Category[]) => (
    <Accordion multiple defaultValue={[]}>
      {columnCategories.map((category) => (
        <Accordion.Item key={category.id} value={category.id}>
          <Accordion.Control>
            <Group justify="space-between" pr="md">
              <Text fw={500}>{category.displayName}</Text>
              <Badge variant="light" color={selectedCountInCategory(category) > 0 ? 'blue' : 'gray'}>
                {selectedCountInCategory(category)} / {category.constructs.length}
              </Badge>
            </Group>
          </Accordion.Control>
          <Accordion.Panel>
            <Group justify="flex-end" gap="xs" mb="xs">
              <Button variant="subtle" size="compact-xs" onClick={() => selectAllInCategory(category)}>
                Select all
              </Button>
              <Button variant="subtle" color="gray" size="compact-xs" onClick={() => clearAllInCategory(category)}>
                Clear all
              </Button>
            </Group>
            <Stack gap="xs">
              {category.constructs.map((construct) => (
                <ConstructRow
                  key={construct.id}
                  construct={construct}
                  disabled={!isEligible(targetProfile, construct.elEligible)}
                />
              ))}
            </Stack>
          </Accordion.Panel>
        </Accordion.Item>
      ))}
    </Accordion>
  )

  return (
    <Stack p="md" gap="md">
      <Group justify="space-between" align="flex-start" wrap="nowrap">
        <div style={{ flex: 1, minWidth: 0 }}>
          <Title order={2}>Constructs</Title>
          <Text c="dimmed" size="sm">
            Pick the OWL2 constructs you want in the generated ontology, organized by category, and how many
            axioms of each.
          </Text>
        </div>
        <Group gap="xs" wrap="nowrap" style={{ flexShrink: 0 }}>
          <Badge variant="light" color={totalSelected > 0 ? 'blue' : 'gray'}>
            {totalSelected} / {totalConstructs} selected
          </Badge>
          <Button variant="light" size="compact-sm" onClick={selectAllOverall}>
            Select all
          </Button>
          <Button variant="subtle" color="gray" size="compact-sm" onClick={clearAllOverall}>
            Clear all
          </Button>
        </Group>
      </Group>

      <div>
        <Text size="sm" fw={500} mb={4}>
          Target profile
        </Text>
        <SegmentedControl
          value={targetProfile}
          onChange={handleProfileChange}
          data={PROFILE_OPTIONS}
          color={targetProfile === 'DL' ? 'dl' : 'el'}
        />
        {targetProfile === 'EL' && (
          <Text c="dimmed" size="xs" mt={4}>
            Constructs not available in OWL 2 EL are grayed out below. The generated ontology is verified
            against the real OWL 2 EL profile checker after generation, not just filtered here.
          </Text>
        )}
      </div>

      <SimpleGrid cols={{ base: 1, md: 2 }} spacing="md">
        {columns.map((columnCategories, index) => (
          <div key={index}>{renderCategoryColumn(columnCategories)}</div>
        ))}
      </SimpleGrid>
    </Stack>
  )
}
