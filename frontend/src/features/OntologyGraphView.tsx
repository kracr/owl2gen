import { useEffect, useMemo, useRef, useState } from 'react'
import type { Core, ElementDefinition, LayoutOptions, StylesheetJsonBlock } from 'cytoscape'
import CytoscapeComponent from 'react-cytoscapejs'
import { Alert, Badge, Checkbox, Group, Loader, Paper, SegmentedControl, Stack, Text } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import { getVariantGraph } from '../api/generationApi'
import { GRAPH_NODE_TYPES, type GraphNodeType } from '../api/types'

const TYPE_COLORS: Record<GraphNodeType, string> = {
  CLASS: '#4c6ef5',
  OBJECT_PROPERTY: '#12b886',
  DATA_PROPERTY: '#f08c00',
  INDIVIDUAL: '#9c36b5',
  ANNOTATION_PROPERTY: '#868e96',
  DATATYPE: '#adb5bd',
  OTHER: '#ced4da',
}

const TYPE_LABELS: Record<GraphNodeType, string> = {
  CLASS: 'Class',
  OBJECT_PROPERTY: 'Object property',
  DATA_PROPERTY: 'Data property',
  INDIVIDUAL: 'Individual',
  ANNOTATION_PROPERTY: 'Annotation property',
  DATATYPE: 'Datatype',
  OTHER: 'Other',
}

type LayoutName = 'cose' | 'breadthfirst' | 'concentric'

const LAYOUTS: { value: LayoutName; label: string }[] = [
  { value: 'cose', label: 'Force-directed' },
  { value: 'breadthfirst', label: 'Hierarchical' },
  { value: 'concentric', label: 'Concentric' },
]

const STYLESHEET: StylesheetJsonBlock[] = [
  {
    selector: 'node',
    style: {
      'background-color': (el) => TYPE_COLORS[el.data('type') as GraphNodeType] ?? '#868e96',
      label: 'data(label)',
      'font-size': 9,
      color: '#495057',
      'text-valign': 'bottom',
      'text-halign': 'center',
      'text-margin-y': 4,
      width: 20,
      height: 20,
    },
  },
  {
    selector: 'edge',
    style: {
      width: 1.5,
      'line-color': '#ced4da',
      'target-arrow-color': '#ced4da',
      'target-arrow-shape': 'triangle',
      'curve-style': 'bezier',
      'arrow-scale': 0.7,
    },
  },
  { selector: 'node:selected', style: { 'border-width': 2, 'border-color': '#1971c2' } },
  { selector: 'edge:selected', style: { 'line-color': '#1971c2', 'target-arrow-color': '#1971c2', width: 2.5 } },
]

export function OntologyGraphView({ jobId, variantId }: { jobId: string; variantId: string }) {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['variant-graph', jobId, variantId],
    queryFn: () => getVariantGraph(jobId, variantId),
  })

  const [layout, setLayout] = useState<LayoutName>('cose')
  const [hiddenTypes, setHiddenTypes] = useState<Set<GraphNodeType>>(new Set(['ANNOTATION_PROPERTY', 'DATATYPE']))
  const [selected, setSelected] = useState<{ label: string; detail: string } | null>(null)
  const cyRef = useRef<Core | null>(null)

  const toggleType = (type: GraphNodeType) => {
    setHiddenTypes((prev) => {
      const next = new Set(prev)
      if (next.has(type)) {
        next.delete(type)
      } else {
        next.add(type)
      }
      return next
    })
  }

  const elements = useMemo<ElementDefinition[]>(() => {
    if (!data) {
      return []
    }
    const visibleIds = new Set(data.nodes.filter((n) => !hiddenTypes.has(n.type)).map((n) => n.id))
    const nodeEls: ElementDefinition[] = data.nodes
      .filter((n) => visibleIds.has(n.id))
      .map((n) => ({ data: { id: n.id, label: n.label, type: n.type } }))
    const edgeEls: ElementDefinition[] = data.edges
      .filter((e) => visibleIds.has(e.source) && visibleIds.has(e.target))
      .map((e) => ({ data: { id: e.id, source: e.source, target: e.target, label: e.label } }))
    return [...nodeEls, ...edgeEls]
  }, [data, hiddenTypes])

  useEffect(() => {
    const cy = cyRef.current
    if (!cy) {
      return
    }
    const runningLayout = cy.layout({ name: layout, animate: false } as LayoutOptions)
    runningLayout.one('layoutstop', () => cy.fit(undefined, 30))
    runningLayout.run()
  }, [layout, elements])

  if (isLoading) {
    return (
      <Group justify="center" p="md">
        <Loader size="sm" />
      </Group>
    )
  }

  if (isError || !data) {
    return (
      <Alert color="red" title="Could not load graph">
        Try again once the variant has finished generating.
      </Alert>
    )
  }

  if (data.nodes.length === 0) {
    return (
      <Text c="dimmed" size="sm">
        No entities to visualize.
      </Text>
    )
  }

  const availableTypes = GRAPH_NODE_TYPES.filter((type) => data.nodes.some((n) => n.type === type))
  const shownNodeCount = elements.filter((el) => !el.data.source).length
  const shownEdgeCount = elements.filter((el) => el.data.source).length

  return (
    <Stack gap="xs">
      {data.truncated && (
        <Alert color="yellow" variant="light" title="Large ontology">
          This is a big ontology — showing a partial view capped at the first {data.edges.length} relationships
          found, not the full graph.
        </Alert>
      )}

      <Group justify="space-between" wrap="wrap" gap="sm">
        <SegmentedControl size="xs" value={layout} onChange={(v) => setLayout(v as LayoutName)} data={LAYOUTS} />
        <Group gap="sm" wrap="wrap">
          {availableTypes.map((type) => (
            <Checkbox
              key={type}
              size="xs"
              checked={!hiddenTypes.has(type)}
              onChange={() => toggleType(type)}
              label={
                <Group gap={4}>
                  <span
                    style={{
                      width: 8,
                      height: 8,
                      borderRadius: 8,
                      background: TYPE_COLORS[type],
                      display: 'inline-block',
                    }}
                  />
                  <Text size="xs">{TYPE_LABELS[type]}</Text>
                </Group>
              }
            />
          ))}
        </Group>
      </Group>

      <Paper withBorder style={{ position: 'relative' }}>
        <CytoscapeComponent
          elements={elements}
          style={{ width: '100%', height: 420 }}
          stylesheet={STYLESHEET}
          cy={(cy) => {
            if (cyRef.current === cy) {
              return
            }
            cyRef.current = cy
            cy.on('tap', 'node', (evt) => {
              const n = evt.target
              setSelected({
                label: n.data('label'),
                detail: `${TYPE_LABELS[n.data('type') as GraphNodeType]} · ${n.degree(false)} connection(s)`,
              })
            })
            cy.on('tap', 'edge', (evt) => {
              const e = evt.target
              setSelected({ label: e.data('label'), detail: `${e.source().data('label')} → ${e.target().data('label')}` })
            })
            cy.on('tap', (evt) => {
              if (evt.target === cy) {
                setSelected(null)
              }
            })
          }}
        />
        {selected && (
          <Paper
            withBorder
            p="xs"
            style={{ position: 'absolute', bottom: 8, left: 8, background: 'var(--mantine-color-body)', maxWidth: 260 }}
          >
            <Text size="sm" fw={600}>
              {selected.label}
            </Text>
            <Text size="xs" c="dimmed">
              {selected.detail}
            </Text>
          </Paper>
        )}
      </Paper>

      <Group gap="xs">
        <Badge variant="light" color="gray">
          {shownNodeCount} nodes shown
        </Badge>
        <Badge variant="light" color="gray">
          {shownEdgeCount} edges shown
        </Badge>
      </Group>
    </Stack>
  )
}
