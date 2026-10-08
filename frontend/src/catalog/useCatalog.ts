import { useQuery } from '@tanstack/react-query'
import { apiClient } from '../api/client'
import type { Catalog } from '../api/types'

export function useCatalog() {
  return useQuery({
    queryKey: ['catalog'],
    queryFn: async () => {
      const { data } = await apiClient.get<Catalog>('/catalog')
      return data
    },
    staleTime: Infinity, // the catalog is static for the lifetime of the app
  })
}
