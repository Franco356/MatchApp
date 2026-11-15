# AGENTS.md

Vite + React 19 SPA (JavaScript, not TypeScript) with React Router. Auth is backed by a Spring Boot API in `../MacthAppBackend`; the rest of the app is still localStorage mock.

## Commands

```bash
npm run dev      # Vite dev server with HMR
npm run build    # production build to dist/
npm run preview  # serve the built dist/
npm run lint     # eslint . (flat config, ignores dist/)
```

- There is no test script and no test files — do not invent one unless asked.
- Run `npm run lint` and `npm run build` after code changes; they are the only verification steps.
- Run `npm ci` first in a fresh checkout. It prints unrelated EBADENGINE/deprecation warnings from the stray `tailwind`/`css` deps; ignore them.

## Tailwind

- Tailwind v4 is wired through `@tailwindcss/vite` in `vite.config.js`.
- The only stylesheet entry is `src/index.css`, which is just `@import 'tailwindcss';`.
- No `tailwind.config.js` — v4 config is CSS-based. Don't add the old config file.
- Use utilities from `tailwindcss`. The `tailwind` and `css` packages in `package.json` are stray installs, not the source of Tailwind; don't import from them.

## Layout

- Entry chain: `index.html` -> `src/main.jsx` -> `src/App.jsx`.
- `src/App.css` holds hand-written component styles; Tailwind utilities are used inline in JSX.
- Static assets in `public/` are served from `/` (e.g. `/icons.svg`); imported assets live in `src/assets/`.
- `src/page/` holds screen-level components; `src/App.jsx` mounts the routes (`/login`, `/registro`, `/verificar-correo`, `/recuperar-contrasena`, protected screens).
- Auth is wired to the Spring backend through `src/services/api.js` and `src/services/auth.js` (`/api/auth/login`, `/api/auth/register`, `/api/auth/verify-email`, `/api/auth/resend-verification`, `/api/auth/google`, `/api/auth/dni/{dni}`, `/api/auth/forgot-password`, `/api/auth/reset-password`). Account endpoints `/api/users/me` and `/api/users/me/password` require `Authorization: Bearer <token>`. Configure `VITE_API_URL` and `VITE_DEMO_MODE=false` in `.env.local` and in the Railway frontend service; `VITE_GOOGLE_CLIENT_ID` is optional and only enables the Google button. The rest of the app is still localStorage mock.
- Registration creates an unverified account and sends an email link to `/verificar-correo?token=...`; it no longer logs in immediately. `Login` blocks unverified accounts (403) and offers resend. `VerificarCorreo` saves the session and continues to `/completar-perfil`. Sessions store `{ identifier, role, user, token, loggedAt }` via `src/auth.js`.

## Conventions

- ES modules (`"type": "module"`), function components, hooks, single quotes, no semicolons.
- UI copy is Spanish (labels, validation errors, messages); keep new screens consistent.
- Keep JSX lint-clean: `eslint-plugin-react-hooks` and `eslint-plugin-react-refresh` are enforced.

## Git

The git root is `/home/francojc/Documentos/MatchApp`. The repo also contains `MacthAppBackend/`; stage only the paths you changed when committing.
