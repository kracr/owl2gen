import { useState } from 'react'
import { AppShell, Anchor, Button, Container, Group, Stepper, Title } from '@mantine/core'
import { ConstructPickerPage } from './catalog/ConstructPickerPage'
import { EntityCountsPanel } from './catalog/EntityCountsPanel'
import { StructureControlsPanel } from './features/StructureControlsPanel'
import { ReasoningOutputPanel } from './features/ReasoningOutputPanel'
import { ReviewPanel } from './features/ReviewPanel'
import { ResultsPage } from './features/ResultsPage'
import { AboutPage } from './features/AboutPage'

const GRADIENT_TEXT_STYLE = {
  backgroundImage: 'linear-gradient(45deg, var(--mantine-color-indigo-6), var(--mantine-color-grape-6))',
  backgroundClip: 'text' as const,
  WebkitBackgroundClip: 'text' as const,
  color: 'transparent',
}

const HEADER_TITLE = (
  <Title order={3} fw={700} style={GRADIENT_TEXT_STYLE}>
    OWL2Gen
  </Title>
)

function App() {
  const [showAbout, setShowAbout] = useState(true)
  const [step, setStep] = useState(0)
  const [jobId, setJobId] = useState<string | undefined>(undefined)

  const header = (isAbout: boolean) => (
    <AppShell.Header>
      <Group h="100%" px="md" justify="space-between">
        <Anchor
          component="button"
          type="button"
          underline="never"
          onClick={isAbout ? undefined : () => setShowAbout(true)}
          style={{ cursor: isAbout ? 'default' : 'pointer' }}
        >
          {HEADER_TITLE}
        </Anchor>
        {!isAbout && (
          <Anchor component="button" type="button" size="sm" onClick={() => setShowAbout(true)}>
            About
          </Anchor>
        )}
      </Group>
    </AppShell.Header>
  )

  if (showAbout) {
    return (
      <AppShell header={{ height: 60 }} padding="md">
        {header(true)}
        <AppShell.Main>
          <Container size="lg" px={0}>
            <AboutPage onGetStarted={() => setShowAbout(false)} />
          </Container>
        </AppShell.Main>
      </AppShell>
    )
  }

  if (jobId) {
    return (
      <AppShell header={{ height: 60 }} padding="md">
        {header(false)}
        <AppShell.Main>
          <Container size="md" px={0}>
            <ResultsPage
              jobId={jobId}
              onStartOver={() => {
                setJobId(undefined)
                setStep(0)
              }}
              onRegenerate={setJobId}
            />
          </Container>
        </AppShell.Main>
      </AppShell>
    )
  }

  return (
    <AppShell header={{ height: 60 }} padding="md">
      {header(false)}
      <AppShell.Main>
        <Container size="lg" px={0}>
          <Stepper active={step} onStepClick={setStep} p="md">
            <Stepper.Step label="Entities & Constructs">
              <EntityCountsPanel />
              <ConstructPickerPage />
            </Stepper.Step>
            <Stepper.Step label="Structure">
              <StructureControlsPanel />
            </Stepper.Step>
            <Stepper.Step label="Reasoning & Output">
              <ReasoningOutputPanel />
            </Stepper.Step>
            <Stepper.Step label="Review & Generate">
              <ReviewPanel onSubmitted={setJobId} />
            </Stepper.Step>
          </Stepper>

          <Group justify="space-between" p="md">
            <Button variant="default" onClick={() => setStep((s) => Math.max(0, s - 1))} disabled={step === 0}>
              Back
            </Button>
            {step < 3 && <Button onClick={() => setStep((s) => Math.min(3, s + 1))}>Next</Button>}
          </Group>
        </Container>
      </AppShell.Main>
    </AppShell>
  )
}

export default App
