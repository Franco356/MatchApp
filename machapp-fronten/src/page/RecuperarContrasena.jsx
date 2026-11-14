import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { forgotPassword, resetPassword } from '../services/auth.js'

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const DNI_REGEX = /^\d{8}$/

const INPUT_CLASS =
  'w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-rose-400 focus:ring-4 focus:ring-rose-100'

const FieldError = ({ message }) =>
  message ? (
    <p className="mt-1.5 text-xs font-medium text-rose-600">{message}</p>
  ) : null

const titles = {
  request: 'Recuperar contraseña',
  sent: 'Revisa tu correo',
  reset: 'Nueva contraseña',
  done: 'Contraseña actualizada',
}

const subtitles = {
  request:
    'Ingresa tu correo o DNI registrado y te enviaremos un enlace para crear una nueva contraseña.',
  sent: 'Si existe una cuenta con ese dato, enviamos un enlace de recuperación. Revisa también tu carpeta de spam. El enlace expira en 30 minutos.',
  reset: 'Elige una contraseña nueva para tu cuenta.',
  done: 'Ya puedes iniciar sesión con tu nueva contraseña.',
}

function RecuperarContrasena() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const token = searchParams.get('token')?.trim() ?? ''

  const [step, setStep] = useState(token ? 'reset' : 'request')
  const [identifier, setIdentifier] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [errors, setErrors] = useState({})
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleRequest = async (event) => {
    event.preventDefault()

    if (isSubmitting) return

    const value = identifier.trim()
    const nextErrors = {}

    if (!value) {
      nextErrors.identifier = 'Ingresa tu correo o DNI.'
    } else if (!EMAIL_REGEX.test(value) && !DNI_REGEX.test(value)) {
      nextErrors.identifier = 'Ingresa un correo válido o un DNI de 8 dígitos.'
    }

    setErrors(nextErrors)

    if (Object.keys(nextErrors).length > 0) return

    setIsSubmitting(true)

    try {
      await forgotPassword(value)
      setStep('sent')
    } catch (error) {
      setErrors({
        general: error?.message || 'No pudimos procesar la solicitud.',
      })
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleReset = async (event) => {
    event.preventDefault()

    if (isSubmitting) return

    const nextErrors = {}

    if (!password) {
      nextErrors.password = 'Ingresa tu nueva contraseña.'
    } else if (password.length < 8) {
      nextErrors.password = 'La contraseña debe tener al menos 8 caracteres.'
    }

    if (!confirmPassword) {
      nextErrors.confirmPassword = 'Confirma tu nueva contraseña.'
    } else if (confirmPassword !== password) {
      nextErrors.confirmPassword = 'Las contraseñas no coinciden.'
    }

    setErrors(nextErrors)

    if (Object.keys(nextErrors).length > 0) return

    if (!token) {
      setErrors({ general: 'El enlace no es válido. Solicita uno nuevo.' })
      return
    }

    setIsSubmitting(true)

    try {
      await resetPassword({ token, newPassword: password })
      setStep('done')
    } catch (error) {
      setErrors({
        general: error?.message || 'No pudimos actualizar la contraseña.',
      })
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleRestart = () => {
    setIdentifier('')
    setPassword('')
    setConfirmPassword('')
    setErrors({})
    setShowPassword(false)
    setStep('request')
    navigate('/recuperar-contrasena', { replace: true })
  }

  const updateIdentifier = (event) => {
    setIdentifier(event.target.value)
    if (errors.identifier || errors.general) {
      setErrors((current) => ({
        ...current,
        identifier: '',
        general: '',
      }))
    }
  }

  const updatePassword = (event) => {
    setPassword(event.target.value)
    if (errors.password || errors.general) {
      setErrors((current) => ({
        ...current,
        password: '',
        general: '',
      }))
    }
  }

  const updateConfirmPassword = (event) => {
    setConfirmPassword(event.target.value)
    if (errors.confirmPassword || errors.general) {
      setErrors((current) => ({
        ...current,
        confirmPassword: '',
        general: '',
      }))
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-[#d9d9d9] px-4 py-10">
      <div className="w-full max-w-md rounded-3xl bg-white p-7 shadow-2xl shadow-black/40 sm:p-9">
        <div className="flex items-center gap-2 text-rose-500">
          <svg
            viewBox="0 0 24 24"
            fill="currentColor"
            aria-hidden="true"
            className="h-5 w-5"
          >
            <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z" />
          </svg>
          <span className="text-lg font-extrabold italic tracking-tight">
            MatchApp
          </span>
        </div>

        <h1 className="mt-6 text-2xl font-extrabold text-slate-900">
          {titles[step]}
        </h1>
        <p className="mt-2 text-sm leading-6 text-slate-500">
          {subtitles[step]}
        </p>

        {step === 'request' && (
          <form noValidate onSubmit={handleRequest} className="mt-6 space-y-5">
            <div>
              <label
                htmlFor="identifier"
                className="mb-1.5 block text-sm text-slate-600"
              >
                Correo electrónico o DNI
              </label>
              <input
                id="identifier"
                name="identifier"
                type="text"
                autoComplete="username"
                placeholder="correo@ejemplo.com / 8 dígitos"
                value={identifier}
                onChange={updateIdentifier}
                aria-invalid={Boolean(errors.identifier)}
                className={INPUT_CLASS}
              />
              <FieldError message={errors.identifier} />
            </div>

            {errors.general && (
              <p
                role="alert"
                className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-xs font-medium text-rose-700"
              >
                {errors.general}
              </p>
            )}

            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full rounded-full bg-gradient-to-r from-[#f2455c] to-[#f58a5f] px-4 py-3.5 text-sm font-semibold text-white shadow-lg shadow-rose-500/30 transition hover:brightness-105 focus:outline-none focus:ring-4 focus:ring-rose-200 disabled:cursor-not-allowed disabled:from-slate-300 disabled:to-slate-300 disabled:shadow-none"
            >
              {isSubmitting ? 'Enviando...' : 'Enviar enlace'}
            </button>
          </form>
        )}

        {step === 'sent' && (
          <div className="mt-6">
            <div className="flex justify-center">
              <div className="flex h-16 w-16 items-center justify-center rounded-full bg-rose-100 text-rose-500">
                <svg
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  aria-hidden="true"
                  className="h-8 w-8"
                >
                  <rect x="3" y="5" width="18" height="14" rx="2" />
                  <path d="m3 7 9 6 9-6" />
                </svg>
              </div>
            </div>

            <button
              type="button"
              onClick={handleRestart}
              className="mt-5 flex w-full items-center justify-center rounded-full border border-slate-300 px-4 py-3.5 text-sm font-semibold text-slate-600 transition hover:border-rose-400 hover:text-rose-500"
            >
              Usar otro correo o DNI
            </button>
          </div>
        )}

        {step === 'reset' && (
          <form noValidate onSubmit={handleReset} className="mt-6 space-y-5">
            <div>
              <div className="mb-1.5 flex items-center justify-between">
                <label htmlFor="password" className="text-sm text-slate-600">
                  Nueva contraseña
                </label>
                <button
                  type="button"
                  onClick={() => setShowPassword((current) => !current)}
                  className="text-xs font-semibold text-rose-500 hover:text-rose-600"
                >
                  {showPassword ? 'Ocultar' : 'Mostrar'}
                </button>
              </div>
              <input
                id="password"
                name="password"
                type={showPassword ? 'text' : 'password'}
                autoComplete="new-password"
                placeholder="Mínimo 8 caracteres"
                value={password}
                onChange={updatePassword}
                aria-invalid={Boolean(errors.password)}
                className={INPUT_CLASS}
              />
              <FieldError message={errors.password} />
            </div>

            <div>
              <label
                htmlFor="confirmPassword"
                className="mb-1.5 block text-sm text-slate-600"
              >
                Confirmar contraseña
              </label>
              <input
                id="confirmPassword"
                name="confirmPassword"
                type={showPassword ? 'text' : 'password'}
                autoComplete="new-password"
                placeholder="Repite tu contraseña"
                value={confirmPassword}
                onChange={updateConfirmPassword}
                aria-invalid={Boolean(errors.confirmPassword)}
                className={INPUT_CLASS}
              />
              <FieldError message={errors.confirmPassword} />
            </div>

            {errors.general && (
              <p
                role="alert"
                className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-xs font-medium text-rose-700"
              >
                {errors.general}
              </p>
            )}

            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full rounded-full bg-gradient-to-r from-[#f2455c] to-[#f58a5f] px-4 py-3.5 text-sm font-semibold text-white shadow-lg shadow-rose-500/30 transition hover:brightness-105 focus:outline-none focus:ring-4 focus:ring-rose-200 disabled:cursor-not-allowed disabled:from-slate-300 disabled:to-slate-300 disabled:shadow-none"
            >
              {isSubmitting ? 'Guardando...' : 'Guardar contraseña'}
            </button>

            <button
              type="button"
              onClick={handleRestart}
              className="w-full text-center text-xs font-semibold text-slate-500 transition hover:text-rose-500"
            >
              Solicitar un enlace nuevo
            </button>
          </form>
        )}

        {step === 'done' && (
          <div className="mt-6">
            <div className="flex justify-center">
              <div className="flex h-16 w-16 items-center justify-center rounded-full bg-emerald-100 text-emerald-600">
                <svg
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2.5"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  aria-hidden="true"
                  className="h-8 w-8"
                >
                  <path d="M20 6L9 17l-5-5" />
                </svg>
              </div>
            </div>

            <p className="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-center text-sm font-medium text-emerald-700">
              Tu contraseña se actualizó correctamente.
            </p>

            <Link
              to="/login"
              className="mt-5 flex w-full items-center justify-center rounded-full bg-gradient-to-r from-[#f2455c] to-[#f58a5f] px-4 py-3.5 text-sm font-semibold text-white shadow-lg shadow-rose-500/30 transition hover:brightness-105"
            >
              Iniciar sesión
            </Link>
          </div>
        )}

        {step !== 'done' && (
          <p className="mt-6 text-center text-xs text-slate-500">
            ¿Recordaste tu contraseña?{' '}
            <Link
              to="/login"
              className="font-semibold text-rose-500 hover:text-rose-600"
            >
              Inicia sesión
            </Link>
          </p>
        )}
      </div>
    </main>
  )
}

export default RecuperarContrasena
