import { isAxiosError } from 'axios'

interface ErrorResponseBody {
  message?: string
  details?: string[]
}

/** Extracts the backend's {message, details} error body when present, falling back to a generic message. */
export function extractErrorMessage(error: unknown): string {
  if (isAxiosError<ErrorResponseBody>(error)) {
    const body = error.response?.data
    if (body?.message) {
      return body.details && body.details.length > 0 ? `${body.message} (${body.details.join(', ')})` : body.message
    }
    if (error.response) {
      return `Request failed with status ${error.response.status}`
    }
    return 'Could not reach the backend — is it running on :8080?'
  }
  if (error instanceof Error) {
    return error.message
  }
  return 'Unknown error'
}

/** True when the backend rejected the request because an explicitly-set entity count was too low for the
 *  selected constructs (see RequestValidator.validateExplicitEntityCounts on the backend). */
export function isInsufficientEntitiesError(error: unknown): boolean {
  return extractErrorMessage(error).startsWith('Not enough entities for the selected constructs')
}
