const SESSION_KEY = 'matchapp.session'

export const getSession = () => {
  try {
    const raw = window.localStorage.getItem(SESSION_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export const saveSession = (session) => {
  window.localStorage.setItem(SESSION_KEY, JSON.stringify(session))
}

export const clearSession = () => {
  window.localStorage.removeItem(SESSION_KEY)
}

export const isAuthenticated = () => {
  const session = getSession()
  return Boolean(session?.token || session?.identifier)
}

export const getToken = () => getSession()?.token ?? null

export const buildSession = ({ user, token }) => ({
  identifier: user?.email ?? '',
  role: user?.role ?? 'user',
  user,
  token,
  loggedAt: new Date().toISOString(),
})
