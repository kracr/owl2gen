import { Checkbox, Group, NumberInput, Text, Tooltip } from '@mantine/core'
import { useGenerationDraftStore } from '../state/generationDraftStore'
import type { ConstructDescriptor } from '../api/types'

export function ConstructRow({ construct, disabled = false }: { construct: ConstructDescriptor; disabled?: boolean }) {
  const count = useGenerationDraftStore((state) => state.selectedConstructs[construct.id])
  const toggleConstruct = useGenerationDraftStore((state) => state.toggleConstruct)
  const setConstructCount = useGenerationDraftStore((state) => state.setConstructCount)
  const selected = count !== undefined

  const checkbox = (
    <Checkbox
      checked={selected}
      disabled={disabled}
      onChange={() => toggleConstruct(construct.id)}
      label={
        <div>
          <Text size="sm">{construct.displayName}</Text>
          <Text size="xs" c="dimmed">
            {construct.description}
          </Text>
        </div>
      }
      styles={{ body: { alignItems: 'flex-start' } }}
    />
  )

  return (
    <Group justify="flex-start" wrap="nowrap" gap="md">
      <div style={{ width: 420, flexShrink: 0 }}>{disabled ? <Tooltip label="Not available in OWL 2 EL">{checkbox}</Tooltip> : checkbox}</div>
      {selected && (
        <NumberInput
          value={count}
          onChange={(value) => setConstructCount(construct.id, typeof value === 'number' ? value : 0)}
          min={0}
          max={10000}
          w={90}
          size="xs"
          aria-label={`Count for ${construct.displayName}`}
        />
      )}
    </Group>
  )
}
