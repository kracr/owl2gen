import { NumberInput, SimpleGrid, Stack, Text, Title } from '@mantine/core'
import { useGenerationDraftStore } from '../state/generationDraftStore'

export function EntityCountsPanel() {
  const entityCounts = useGenerationDraftStore((state) => state.entityCounts)
  const setEntityCounts = useGenerationDraftStore((state) => state.setEntityCounts)

  const field = (key: keyof typeof entityCounts, label: string) => (
    <NumberInput
      label={label}
      placeholder="Auto"
      value={entityCounts[key] ?? ''}
      onChange={(value) => setEntityCounts({ [key]: typeof value === 'number' ? value : undefined })}
      min={0}
    />
  )

  return (
    <Stack gap="xs">
      <div>
        <Title order={4}>Entity pool</Title>
        <Text c="dimmed" size="sm">
          How many classes, object properties, data properties, and individuals to create. Leave a field
          blank to let the generator pick a sensible number automatically, based on what you select below. If
          a number you enter turns out too small for your selection, a few extra entities are created
          automatically so generation still succeeds.
        </Text>
      </div>
      <SimpleGrid cols={{ base: 2, sm: 4 }}>
        {field('classes', 'Classes')}
        {field('objectProperties', 'Object properties')}
        {field('dataProperties', 'Data properties')}
        {field('individuals', 'Individuals')}
      </SimpleGrid>
    </Stack>
  )
}
