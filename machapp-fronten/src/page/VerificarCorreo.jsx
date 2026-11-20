import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { buildSession, saveSession } from '../auth.js'
import { verifyEmail } from '../services/auth.js'

function VerificarCorreo() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token') ?? ''
  const [status, setStatus] = useState(token ? 'loading' : 'error')
  const [message, setMessage] = useState(
    token ? '' : 'El enlace no incluye un token de verificación.'
  )
  const verificationRef = useRef(null)

  useEffect(() => {
    if (!token) return undefined

    if (verificationRef.current?.token !== token) {
      verificationRef.current = {
        token,
        promise: verifyEmail(token)
          .then((data) => {
            try {
              if (data?.token && data?.user) {
                saveSession(buildSession(data))
              }
              return { ok: true }
            } catch (error) {
              return {
                ok: false,
                message: error?.message || 'No pudimos guardar la sesión.',
              }
            }
          })
          .catch((error) => ({
            ok: false,
            message: error?.message || 'No pudimos verificar tu correo.',
          })),
      }
    }

    let active = true

    verificationRef.current.promise.then((result) => {
      if (!active) return

      if (result.ok) {
        setStatus('success')
      } else {
        setStatus('error')
        setMessage(result.message)
      }
    })

    return () => {
      active = false
    }
  }, [token])

  return (
    <main className="flex min-h-screen items-center justify-center bg-[#d9d9d9] px-4 py-10">
      <div className="w-full max-w-sm rounded-2xl bg-white px-6 py-8 text-center shadow-2xl shadow-black/40">
        <div className="flex items-center justify-center gap-2 text-rose-500">
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

        {status === 'loading' && (
          <>
            <div
              role="status"
              aria-label="Verificando correo"
              className="mx-auto mt-7 h-10 w-10 animate-spin rounded-full border-4 border-rose-100 border-t-rose-500"
            />
            <h1 className="mt-5 text-2xl font-extrabold text-slate-900">
              Verificando tu correo
            </h1>
            <p className="mt-3 text-sm leading-6 text-slate-500">
              Espera un momento mientras activamos tu cuenta.
            </p>
          </>
        )}

        {status === 'success' && (
          <>
            <div className="mx-auto mt-7 flex h-14 w-14 items-center justify-center rounded-full bg-emerald-50 text-emerald-500">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                aria-hidden="true"
                className="h-7 w-7"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="M5 13l4 4L19 7"
                />
              </svg>
            </div>
            <h1 className="mt-5 text-2xl font-extrabold text-slate-900">
              Correo verificado
            </h1>
            <p className="mt-3 text-sm leading-6 text-slate-500">
              Tu cuenta está activa. Ya puedes completar tu perfil y empezar a
              hacer match.
            </p>
            <button
              type="button"
              onClick={() => navigate('/completar-perfil', { replace: true })}
              className="mt-6 w-full rounded-full bg-gradient-to-r from-[#f2455c] to-[#f58a5f] px-4 py-3.5 text-sm font-semibold text-white shadow-lg shadow-rose-500/30 transition hover:brightness-105 focus:outline-none focus:ring-4 focus:ring-rose-200"
            >
              Continuar
            </button>
          </>
        )}

        {status === 'error' && (
          <>
            <div className="mx-auto mt-7 flex h-14 w-14 items-center justify-center rounded-full bg-rose-50 text-rose-500">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                aria-hidden="true"
                className="h-7 w-7"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="M12 8v5m0 3h.01M10.29 3.86L2.82 17a2 2 0 001.71 3h14.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"
                />
              </svg>
            </div>
            <h1 className="mt-5 text-2xl font-extrabold text-slate-900">
              No pudimos verificar tu correo
            </h1>
            <p
              role="alert"
              className="mt-3 text-sm leading-6 text-slate-500"
            >
              {message}
            </p>
            <Link
              to="/registro"
              className="mt-6 block w-full rounded-full bg-gradient-to-r from-[#f2455c] to-[#f58a5f] px-4 py-3.5 text-sm font-semibold text-white shadow-lg shadow-rose-500/30 transition hover:brightness-105"
            >
              Volver al registro
            </Link>
          </>
        )}

        <Link
          to="/login"
          className="mt-4 block text-sm font-semibold text-rose-500 hover:text-rose-600"
        >
          Ir a iniciar sesión
        </Link>
      </div>
    </main>
  )
}

export default VerificarCorreo
