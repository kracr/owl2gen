import { Badge, Button, Card, Divider, Group, Select, SimpleGrid, Text, Title } from '@mantine/core'
import { useState } from 'react'
import { ontologyDownloadUrl } from '../api/generationApi'
import { OUTPUT_FORMATS, type OutputFormat, type VariantResult } from '../api/types'
import { useGenerationDraftStore } from '../state/generationDraftStore'
import { OntologyGraphView } from './OntologyGraphView'

const STATUS_COLOR: Record<string, string> = {
  CONSISTENT: 'green',
  INCONSISTENT: 'red',
  NOT_VERIFIED_TIMEOUT: 'yellow',
  NOT_VERIFIED_ERROR: 'gray',
}

function Stat({ label, value }: { label: string; value: string | number }) {
  return (
    <div>
      <Text size="xs" c="dimmed">
        {label}
      </Text>
      <Text size="sm" fw={600}>
        {value}
      </Text>
    </div>
  )
}

export function VariantResultCard({ jobId, result }: { jobId: string; result: VariantResult }) {
  const structure = useGenerationDraftStore((state) => state.structure)
  const [format, setFormat] = useState<OutputFormat>('RDFXML')
  const [showGraph, setShowGraph] = useState(false)

  if (result.status === 'PENDING' || result.status === 'RUNNING') {
    return (
      <Card withBorder padding="md">
        <Group justify="space-between">
          <Title order={5}>{result.variantId}</Title>
          <Badge color="blue">{result.status}</Badge>
        </Group>
      </Card>
    )
  }

  if (result.status === 'FAILED') {
    return (
      <Card withBorder padding="md">
        <Group justify="space-between" mb="xs">
          <Title order={5}>{result.variantId}</Title>
          <Badge color="red">FAILED</Badge>
        </Group>
        <Text size="sm" c="dimmed">
          {result.errorMessage ?? 'Unknown error'}
        </Text>
      </Card>
    )
  }

  const { verification, metrics } = result

  return (
    <Card withBorder padding="md">
      <Group justify="space-between" mb="xs">
        <Title order={5}>{result.variantId}</Title>
        <Group gap="xs">
          {verification && <Badge color={STATUS_COLOR[verification.status] ?? 'gray'}>{verification.status}</Badge>}
          <Badge variant="light">{result.axiomCount} axioms</Badge>
        </Group>
      </Group>

      {verification && (
        <Text size="xs" c="dimmed" mb="sm">
          Verified via {verification.tier} in {verification.elapsedMillis}ms — profile:{' '}
          {(['el', 'ql', 'rl', 'dl'] as const).filter((p) => verification.profile[p]).join(', ').toUpperCase() || 'none'}
        </Text>
      )}

      {verification?.profileGuaranteeSatisfied !== null && verification?.profileGuaranteeSatisfied !== undefined && (
        <Badge
          variant="light"
          color={verification.profileGuaranteeSatisfied ? 'green' : 'red'}
          mb="sm"
        >
          Requested {verification.requestedProfile}: {verification.profileGuaranteeSatisfied ? 'satisfied' : 'NOT satisfied'}
        </Badge>
      )}

      {metrics && (
        <>
          <Divider label="Structural profile" labelPosition="left" mb="xs" />
          <SimpleGrid cols={3} spacing="sm" mb="sm">
            <Stat label="Hierarchy depth" value={`${metrics.hierarchy.maxDepth} / ${structure.hierarchyTargetDepth} target`} />
            <Stat label="Avg branching" value={metrics.hierarchy.avgBranchingFactor.toFixed(2)} />
            <Stat label="Tangledness" value={metrics.hierarchy.tangledness.toFixed(2)} />
            <Stat label="Nesting depth" value={`${metrics.nesting.maxDepth} / ${structure.nestingMaxDepth} target`} />
            <Stat label="Avg node degree" value={metrics.graph.avgDegree.toFixed(2)} />
            <Stat label="Clustering coeff." value={metrics.graph.clusteringCoefficient.toFixed(3)} />
            <Stat label="Entities / edges" value={`${metrics.graph.nodeCount} / ${metrics.graph.edgeCount}`} />
            <Stat label="Inferred (new)" value={metrics.reasoning.computed ? metrics.reasoning.inferredNewSubClassOfCount : 'N/A'} />
            <Stat label="Inferred:asserted" value={metrics.reasoning.computed ? metrics.reasoning.inferredToAssertedRatio.toFixed(2) : 'N/A (inconsistent)'} />
          </SimpleGrid>
        </>
      )}

      <Group gap="xs" mb={showGraph ? 'sm' : 0}>
        <Select
          data={OUTPUT_FORMATS.map((f) => ({ value: f, label: f }))}
          value={format}
          onChange={(value) => value && setFormat(value as OutputFormat)}
          w={140}
          size="xs"
          allowDeselect={false}
        />
        <Button
          component="a"
          href={ontologyDownloadUrl(jobId, result.variantId, format)}
          size="xs"
          variant="light"
        >
          Download
        </Button>
        <Button size="xs" variant={showGraph ? 'filled' : 'default'} onClick={() => setShowGraph((v) => !v)}>
          {showGraph ? 'Hide graph' : 'View graph'}
        </Button>
      </Group>

      {showGraph && <OntologyGraphView jobId={jobId} variantId={result.variantId} />}
    </Card>
  )
}
