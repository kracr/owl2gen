import { Alert, Button, Group, Stack, Text, Title } from '@mantine/core'
import { useMutation } from '@tanstack/react-query'
import { useGenerationDraftStore } from '../state/generationDraftStore'
import { submitGeneration } from '../api/generationApi'
import { extractErrorMessage, isInsufficientEntitiesError } from '../api/errors'
import { MAX_TOTAL_REQUESTED_AXIOMS, type GenerationRequest } from '../api/types'
import { RequestSummary } from './RequestSummary'
import { buildGenerationRequest } from '../state/buildGenerationRequest'

export function ReviewPanel({ onSubmitted }: { onSubmitted: (jobId: string) => void }) {
  const draft = useGenerationDraftStore()
  const setEntityCounts = useGenerationDraftStore((state) => state.setEntityCounts)
  const constructEntries = Object.entries(draft.selectedConstructs)
  const totalRequestedAxioms = constructEntries.reduce((sum, [, count]) => sum + count, 0)
  const overLimit = totalRequestedAxioms > MAX_TOTAL_REQUESTED_AXIOMS
  const noConstructsSelected = constructEntries.length === 0

  const mutation = useMutation({
    mutationFn: (request: GenerationRequest) => submitGeneration(request),
    onSuccess: (job) => onSubmitted(job.jobId),
  })

  const handleGenerate = () => {
    mutation.mutate(buildGenerationRequest(draft) satisfies GenerationRequest)
  }

  const handleUseAutomaticSizing = () => {
    setEntityCounts({ classes: undefined, objectProperties: undefined, dataProperties: undefined, individuals: undefined })
    mutation.mutate(
      buildGenerationRequest({ ...draft, entityCounts: {} }) satisfies GenerationRequest,
    )
  }

  return (
    <Stack gap="md">
      <Title order={4}>Review</Title>

      <RequestSummary />

      {noConstructsSelected && (
        <Alert color="yellow" title="No constructs selected">
          Go back to the Constructs step and pick at least one construct with a count above zero.
        </Alert>
      )}

      {overLimit && (
        <Alert color="red" title="Too many requested axioms">
          {totalRequestedAxioms} requested axioms exceeds the maximum of {MAX_TOTAL_REQUESTED_AXIOMS} per request.
          Reduce some construct counts on the Constructs step.
        </Alert>
      )}

      {mutation.isError && isInsufficientEntitiesError(mutation.error) && (
        <Alert color="red" title="Not enough entities for what you selected">
          <Text size="sm" mb="xs">
            {extractErrorMessage(mutation.error)}
          </Text>
          <Button size="xs" variant="light" onClick={handleUseAutomaticSizing} loading={mutation.isPending}>
            Use automatic sizing instead
          </Button>
        </Alert>
      )}

      {mutation.isError && !isInsufficientEntitiesError(mutation.error) && (
        <Alert color="red" title="Failed to submit generation">
          {extractErrorMessage(mutation.error)}
        </Alert>
      )}

      <Group>
        <Button
          size="md"
          onClick={handleGenerate}
          loading={mutation.isPending}
          disabled={noConstructsSelected || overLimit}
        >
          Generate
        </Button>
      </Group>
    </Stack>
  )
}
