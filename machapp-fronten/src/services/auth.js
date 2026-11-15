import { apiRequest } from './api.js'

export const login = ({ email, password }) =>
  apiRequest('/api/auth/login', {
    method: 'POST',
    body: { email: email.trim().toLowerCase(), password },
  })

export const register = (account) =>
  apiRequest('/api/auth/register', {
    method: 'POST',
    body: account,
  })

export const loginWithGoogle = (credential) =>
  apiRequest('/api/auth/google', {
    method: 'POST',
    body: { credential },
  })

export const verifyDni = (dni) =>
  apiRequest(`/api/auth/dni/${encodeURIComponent(dni.trim())}`)

export const forgotPassword = (identifier) =>
  apiRequest('/api/auth/forgot-password', {
    method: 'POST',
    body: { identifier: identifier.trim() },
  })

export const resetPassword = ({ token, newPassword }) =>
  apiRequest('/api/auth/reset-password', {
    method: 'POST',
    body: { token, newPassword },
  })

export const verifyEmail = (token) =>
  apiRequest('/api/auth/verify-email', {
    method: 'POST',
    body: { token },
  })

export const resendVerification = (email) =>
  apiRequest('/api/auth/resend-verification', {
    method: 'POST',
    body: { email: email.trim().toLowerCase() },
  })

export const getMe = (token) =>
  apiRequest('/api/users/me', { token })

export const updateMe = ({ nombres, apellidos }, token) =>
  apiRequest('/api/users/me', {
    method: 'PATCH',
    body: { nombres, apellidos },
    token,
  })

export const changePassword = ({ currentPassword, newPassword }, token) =>
  apiRequest('/api/users/me/password', {
    method: 'PATCH',
    body: { currentPassword, newPassword },
    token,
  })
