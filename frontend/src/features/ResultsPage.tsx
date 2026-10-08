import { Alert, Badge, Button, Divider, Group, Loader, SimpleGrid, Stack, Text, Title } from '@mantine/core'
import { useMutation } from '@tanstack/react-query'
import { useGenerationJob } from './useGenerationJob'
import { VariantResultCard } from './VariantResultCard'
import { extractErrorMessage, isInsufficientEntitiesError } from '../api/errors'
import { RequestSummary } from './RequestSummary'
import { useGenerationDraftStore } from '../state/generationDraftStore'
import { buildGenerationRequest } from '../state/buildGenerationRequest'
import { submitGeneration } from '../api/generationApi'

export function ResultsPage({
  jobId,
  onStartOver,
  onRegenerate,
}: {
  jobId: string
  onStartOver: () => void
  onRegenerate: (jobId: string) => void
}) {
  const { data: job, isLoading, isError, error } = useGenerationJob(jobId)
  const draft = useGenerationDraftStore()
  const setEntityCounts = useGenerationDraftStore((state) => state.setEntityCounts)

  const regenerateMutation = useMutation({
    mutationFn: (useAutomaticSizing: boolean) =>
      submitGeneration(useAutomaticSizing ? buildGenerationRequest({ ...draft, entityCounts: {} }) : buildGenerationRequest(draft)),
    onSuccess: (newJob) => onRegenerate(newJob.jobId),
  })

  const handleUseAutomaticSizing = () => {
    // Also clear the persisted draft so the store stays consistent with what was actually submitted —
    // the mutation itself doesn't depend on this having propagated, since it builds the override inline.
    setEntityCounts({ classes: undefined, objectProperties: undefined, dataProperties: undefined, individuals: undefined })
    regenerateMutation.mutate(true)
  }

  if (isLoading) {
    return (
      <Group justify="center" p="xl">
        <Loader />
      </Group>
    )
  }

  if (isError || !job) {
    return (
      <Alert color="red" title="Could not load job status" m="md">
        {extractErrorMessage(error)}
      </Alert>
    )
  }

  const hasInconsistentVariant = job.variants.some((result) => result.verification?.status === 'INCONSISTENT')

  return (
    <Stack p="md" gap="md">
      <Group justify="space-between">
        <Group gap="sm">
          <Title order={3}>Results</Title>
          <Badge color={job.status === 'COMPLETED' ? 'green' : job.status === 'FAILED' ? 'red' : 'blue'}>
            {job.status}
          </Badge>
        </Group>
        <Group gap="xs">
          <Button
            variant={hasInconsistentVariant ? 'filled' : 'default'}
            color={hasInconsistentVariant ? 'orange' : undefined}
            onClick={() => regenerateMutation.mutate(false)}
            loading={regenerateMutation.isPending}
          >
            Regenerate
          </Button>
          <Button variant="subtle" onClick={onStartOver}>
            Start a new generation
          </Button>
        </Group>
      </Group>

      <div>
        <Text size="sm" fw={500} mb={4}>
          Requested
        </Text>
        <RequestSummary />
      </div>

      {hasInconsistentVariant && (
        <Alert color="orange" title="One or more variants came back inconsistent">
          The generator retries automatically a few times before giving up, but some construct/count/seed
          combinations can still land on an inconsistent draw. Click Regenerate to try again with the exact
          same request{draft.seed === undefined ? ' — with no seed pinned, it draws a fresh random one each time' : '; note you have a seed pinned, so this will reproduce the same draw until you change or clear it'}.
        </Alert>
      )}

      {regenerateMutation.isError && isInsufficientEntitiesError(regenerateMutation.error) && (
        <Alert color="red" title="Not enough entities for what you selected">
          <Text size="sm" mb="xs">
            {extractErrorMessage(regenerateMutation.error)}
          </Text>
          <Button size="xs" variant="light" onClick={handleUseAutomaticSizing} loading={regenerateMutation.isPending}>
            Use automatic sizing instead
          </Button>
        </Alert>
      )}

      {regenerateMutation.isError && !isInsufficientEntitiesError(regenerateMutation.error) && (
        <Alert color="red" title="Failed to regenerate">
          {extractErrorMessage(regenerateMutation.error)}
        </Alert>
      )}

      {job.error && (
        <Alert color="red" title="Job failed">
          {job.error}
        </Alert>
      )}

      <Divider label="Got" labelPosition="left" />

      <SimpleGrid cols={{ base: 1, md: job.variants.length > 1 ? 2 : 1 }}>
        {job.variants.map((result) => (
          <VariantResultCard key={result.variantId} jobId={jobId} result={result} />
        ))}
      </SimpleGrid>
    </Stack>
  )
}
