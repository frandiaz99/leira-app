# Plancito 💞

App para anotar en pareja los lugares a los que quieren ir y, cuando van, dejar cada uno su reseña, puntaje y fotos.

**Stack:** Vite + React 19 + TypeScript · Tailwind v4 · TanStack Query · React Router · Supabase (auth, Postgres, storage) · Leaflet + OpenStreetMap · PWA instalable.

## Puesta en marcha

### 1. Crear el proyecto en Supabase (gratis)

1. Entrá a [supabase.com](https://supabase.com) → **New project**.
2. **SQL Editor** → pegá todo el contenido de [`supabase/migrations/0001_init.sql`](supabase/migrations/0001_init.sql) → **Run**.
   Crea las tablas, la seguridad (RLS) y el bucket privado `photos`.
3. **Authentication → Email Templates → Magic Link**: agregá el código al mail para poder
   entrar desde la app instalada en el celu (en iPhone el link abre Safari, no la app):

   ```html
   <h2>Entrar a Plancito</h2>
   <p>Tu código: <strong>{{ .Token }}</strong></p>
   <p>O tocá este link: <a href="{{ .ConfirmationURL }}">Entrar</a></p>
   ```

4. **Authentication → URL Configuration**: en *Site URL* poné `http://localhost:5173` (después la URL de producción) y agregá ambas en *Redirect URLs*.

### 2. Variables de entorno

```bash
cp .env.example .env.local
```

Completá `VITE_SUPABASE_URL` y `VITE_SUPABASE_ANON_KEY` (Supabase → Project Settings → API).

### 3. Correr

```bash
npm install
npm run dev
```

Abrí http://localhost:5173, entrá con tu mail, creá el espacio y pasale el **código de invitación** (en Perfil) a tu pareja.

## Deploy (Vercel)

1. Subí el repo a GitHub e importalo en [vercel.com](https://vercel.com) (detecta Vite solo).
2. Cargá las dos variables `VITE_SUPABASE_*` en *Environment Variables*.
3. Agregá la URL de Vercel en Supabase → Authentication → URL Configuration.
4. En el celu: abrí la URL → *Compartir → Agregar a pantalla de inicio* (iPhone) o *Instalar app* (Android).

## Estructura

```
supabase/migrations/   esquema SQL + RLS + storage
src/
  lib/                 cliente Supabase, tipos de la DB, geocoding, fechas, categorías
  components/          layout, guards de rutas, bottom nav y UI base
  features/
    auth/              sesión y login por mail (link o código)
    couple/            perfil, crear/unirse a la pareja
    places/            lista, detalle, formulario, "¡Fuimos!"
    map/               mapa general, selector de ubicación, búsqueda de direcciones
    reviews/           reseña por persona y promedio
    photos/            subida comprimida, galería, lightbox
    profile/           nombre, código de invitación, cerrar sesión
```

Cada feature sigue el mismo patrón: `api.ts` (llamadas a Supabase) → `hooks.ts` (TanStack Query) → componentes.

### Modelo de datos

- **couples**: el espacio compartido, con `invite_code`. Máximo 2 miembros.
- **profiles**: uno por usuario (lo crea un trigger), con su `couple_id`.
- **places**: `status` `wishlist`/`visited`, categoría, etiquetas, fecha planeada, ubicación.
- **reviews**: una por persona por lugar (`unique(place_id, user_id)`).
- **photos**: metadata; el archivo vive en el bucket `photos` en `{couple_id}/{place_id}/{uuid}.jpg`.

La seguridad está en la base (RLS): cada pareja solo ve lo suyo, cada uno edita solo su reseña y borra solo sus fotos.

### Regenerar los tipos

`src/lib/database.types.ts` está escrito a mano con el formato de Supabase. Si cambiás el esquema:

```bash
npx supabase gen types typescript --project-id <tu-project-id> > src/lib/database.types.ts
```

## Scripts

| | |
|---|---|
| `npm run dev` | servidor de desarrollo |
| `npm run build` | typecheck + build de producción (con service worker) |
| `npm run preview` | sirve el build |
| `npm run lint` | oxlint |
