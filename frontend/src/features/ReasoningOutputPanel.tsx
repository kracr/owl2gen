import { Alert, NumberInput, Select, Stack, Switch, Text, Title } from '@mantine/core'
import { useGenerationDraftStore } from '../state/generationDraftStore'
import { OUTPUT_FORMATS, type OutputFormat } from '../api/types'

const FORMAT_LABELS: Record<OutputFormat, string> = {
  RDFXML: 'RDF/XML',
  TURTLE: 'Turtle',
  OWLXML: 'OWL/XML',
  MANCHESTER: 'Manchester Syntax',
  FUNCTIONAL: 'Functional Syntax',
}

export function ReasoningOutputPanel() {
  const timeoutSeconds = useGenerationDraftStore((state) => state.timeoutSeconds)
  const setTimeoutSeconds = useGenerationDraftStore((state) => state.setTimeoutSeconds)
  const outputFormat = useGenerationDraftStore((state) => state.outputFormat)
  const setOutputFormat = useGenerationDraftStore((state) => state.setOutputFormat)
  const seed = useGenerationDraftStore((state) => state.seed)
  const setSeed = useGenerationDraftStore((state) => state.setSeed)
  const strictConsistency = useGenerationDraftStore((state) => state.strictConsistency)
  const setStrictConsistency = useGenerationDraftStore((state) => state.setStrictConsistency)

  return (
    <Stack gap="xl">
      <div>
        <Title order={4}>Consistency verification</Title>
        <Text c="dimmed" size="sm" mb="sm">
          Full DL consistency checking has no guaranteed time bound at scale. Verification runs under this
          timeout and reports "not verified within budget" rather than blocking if it's exceeded — that's
          honest information about the request's difficulty, not a failure of the tool.
        </Text>
        <NumberInput
          label="Timeout (seconds)"
          value={timeoutSeconds}
          onChange={(value) => setTimeoutSeconds(typeof value === 'number' ? value : 30)}
          min={1}
          max={300}
          w={160}
          mb="md"
        />
        <Switch
          label="Guarantee consistency (slower)"
          description="Checks consistency after each selected construct type during generation, not just once at
            the end, and drops any that can't be made consistent after a few tries. Large or varied selections
            take noticeably longer to generate, but are far more likely to come back consistent on the first try."
          checked={strictConsistency}
          onChange={(event) => setStrictConsistency(event.currentTarget.checked)}
          mb="md"
        />
        <Alert color="yellow" variant="light" title="Note">
          Some combinations of constructs and counts are logically inconsistent no matter how "Guarantee
          consistency" rearranges them — that's a property of the axioms you selected, not a tool issue. In
          that case the affected construct types are dropped rather than forced.
        </Alert>
      </div>

      <div>
        <Title order={4}>Output format</Title>
        <Select
          data={OUTPUT_FORMATS.map((format) => ({ value: format, label: FORMAT_LABELS[format] }))}
          value={outputFormat}
          onChange={(value) => value && setOutputFormat(value as OutputFormat)}
          w={220}
          allowDeselect={false}
        />
      </div>

      <div>
        <Title order={4}>Random seed</Title>
        <Text c="dimmed" size="sm" mb="sm">
          Optional — set one to reproduce the exact same generated ontology later.
        </Text>
        <NumberInput
          placeholder="Random"
          value={seed}
          onChange={(value) => setSeed(typeof value === 'number' ? value : undefined)}
          w={160}
        />
      </div>
    </Stack>
  )
}
