import { Alert, Badge, Button, Card, Group, List, SimpleGrid, Stack, Text, Title } from '@mantine/core'

const GRADIENT_TEXT_STYLE = {
  backgroundImage: 'linear-gradient(45deg, var(--mantine-color-indigo-6), var(--mantine-color-grape-6))',
  backgroundClip: 'text' as const,
  WebkitBackgroundClip: 'text' as const,
  color: 'transparent',
}

export function AboutPage({ onGetStarted }: { onGetStarted: () => void }) {
  return (
    <Stack gap="xl" p="md" maw={860} mx="auto">
      <div>
        <Group gap="xs" mb={4}>
          <Title order={1} style={GRADIENT_TEXT_STYLE}>
            OWL2Gen
          </Title>
          <Badge color="dl" variant="light" size="lg">
            OWL 2 DL
          </Badge>
          <Badge color="el" variant="light" size="lg">
            OWL 2 EL
          </Badge>
        </Group>
        <Text c="dimmed" size="lg">
          A configurable generator for synthetic OWL 2 ontologies — pick the constructs you want, how the
          class hierarchy should be shaped, and get back a reasoner-verified ontology in the format you need.
        </Text>
      </div>

      <SimpleGrid cols={{ base: 1, sm: 3 }} spacing="md">
        <Card withBorder padding="md">
          <Text fw={600} mb={4}>
            1. Pick constructs
          </Text>
          <Text size="sm" c="dimmed">
            Choose entity counts and which OWL 2 constructs to include, by category, with how many axioms of
            each. Restrict to the EL profile or allow full DL.
          </Text>
        </Card>
        <Card withBorder padding="md">
          <Text fw={600} mb={4}>
            2. Shape the structure
          </Text>
          <Text size="sm" c="dimmed">
            Control hierarchy depth/branching, axiom nesting, and how entities get wired together — compare
            several topology variants from the same selection.
          </Text>
        </Card>
        <Card withBorder padding="md">
          <Text fw={600} mb={4}>
            3. Generate & verify
          </Text>
          <Text size="sm" c="dimmed">
            The result is checked for consistency and profile membership by a real reasoner, with metrics
            and a download in your preferred syntax.
          </Text>
        </Card>
      </SimpleGrid>

      <div>
        <Title order={4} mb={6}>
          How to use it
        </Title>
        <List spacing="xs" size="sm">
          <List.Item>Set entity counts and select constructs on the first step (defaults are fine to start).</List.Item>
          <List.Item>Step through Structure and Reasoning & Output, adjusting only what you care about.</List.Item>
          <List.Item>Review your selections and click Generate.</List.Item>
          <List.Item>On the results page, check the consistency badge and metrics, then download the ontology.</List.Item>
        </List>
      </div>

      <Alert color="yellow" variant="light" title="A note on consistency">
        Some combinations of constructs and counts are logically inconsistent no matter how they're arranged —
        that's a property of the axioms you selected, not a bug in the generator. Turn on{' '}
        <strong>"Guarantee consistency"</strong> on the Reasoning & Output step to have the generator check and
        retry as it builds, and drop only the specific construct types that can't be made consistent after a
        few tries.
      </Alert>

      <Group justify="flex-end">
        <Button size="md" onClick={onGetStarted}>
          Get started →
        </Button>
      </Group>
    </Stack>
  )
}
