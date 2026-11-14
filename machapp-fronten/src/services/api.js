const API_URL = (import.meta.env.VITE_API_URL ?? '').replace(/\/+$/, '')

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
    })
  } catch {
    throw new ApiError(
      'No pudimos conectar con el servidor. Revisa tu conexión e intenta de nuevo.',
      0,
    )
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
