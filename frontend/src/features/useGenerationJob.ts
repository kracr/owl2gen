import { useQuery } from '@tanstack/react-query'
import { getGenerationJob } from '../api/generationApi'

export function useGenerationJob(jobId: string | undefined) {
  return useQuery({
    queryKey: ['generation-job', jobId],
    queryFn: () => getGenerationJob(jobId as string),
    enabled: jobId !== undefined,
    refetchInterval: (query) => {
      const status = query.state.data?.status
      return status === 'COMPLETED' || status === 'FAILED' ? false : 1500
    },
  })
}
