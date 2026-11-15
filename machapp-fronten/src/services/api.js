const normalizeApiUrl = (value) => {
  const url = (value ?? '').trim().replace(/\/+$/, '')

  if (!url || url.startsWith('/')) return url
  if (/^https?:\/\//i.test(url)) return url
  if (/^(localhost|127\.0\.0\.1)(:\d+)?$/i.test(url)) return `http://${url}`

  return `https://${url}`
}

const API_URL = normalizeApiUrl(import.meta.env.VITE_API_URL)
const REQUEST_TIMEOUT_MS = 30000

export class ApiError extends Error {
  constructor(message, status, data) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.data = data
  }
}

export const getApiUrl = () => API_URL

const readBody = async (response) => {
  const text = await response.text()
  if (!text) return null

  try {
    return JSON.parse(text)
  } catch {
    return text
  }
}

const getErrorMessage = (data, fallback) => {
  if (typeof data === 'string' && data.trim()) return data
  if (data && typeof data === 'object') {
    return data.message || data.error || fallback
  }
  return fallback
}

export const apiRequest = async (
  path,
  { method = 'GET', body, token, headers } = {},
) => {
  if (!API_URL) {
    throw new ApiError('Falta configurar VITE_API_URL en .env.local.', 0)
  }

  const controller = new AbortController()
  const timeoutId = window.setTimeout(
    () => controller.abort(),
    REQUEST_TIMEOUT_MS,
  )

  let response
  try {
    response = await fetch(`${API_URL}${path}`, {
      method,
      headers: {
        ...(body ? { 'Content-Type': 'application/json' } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...headers,
      },
      body: body ? JSON.stringify(body) : undefined,
      signal: controller.signal,
    })
  } catch (error) {
    if (error?.name === 'AbortError') {
      throw new ApiError(
        'El servidor tardó demasiado en responder. Intenta de nuevo.',
        0,
      )
    }

    throw new ApiError(
      'No pudimos conectar con el servidor. Revisa tu conexión e intenta de nuevo.',
      0,
    )
  } finally {
    window.clearTimeout(timeoutId)
  }

  const data = await readBody(response)

  if (!response.ok) {
    throw new ApiError(
      getErrorMessage(data, 'Ocurrió un error. Intenta de nuevo.'),
      response.status,
      data,
    )
  }

  return data
}
