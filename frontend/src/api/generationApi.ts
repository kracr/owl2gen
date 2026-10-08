import { apiClient } from './client'
import type { GenerationJob, GenerationRequest, OntologyGraph, OutputFormat } from './types'

export async function submitGeneration(request: GenerationRequest): Promise<GenerationJob> {
  const { data } = await apiClient.post<GenerationJob>('/generations', request)
  return data
}

export async function getVariantGraph(jobId: string, variantId: string): Promise<OntologyGraph> {
  const { data } = await apiClient.get<OntologyGraph>(`/generations/${jobId}/variants/${variantId}/graph`)
  return data
}

export async function getGenerationJob(jobId: string): Promise<GenerationJob> {
  const { data } = await apiClient.get<GenerationJob>(`/generations/${jobId}`)
  return data
}

export function ontologyDownloadUrl(jobId: string, variantId: string, format: OutputFormat): string {
  return `/api/generations/${jobId}/variants/${variantId}/ontology?format=${format}`
}
