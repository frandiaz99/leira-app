# Plancito

App Android para anotar con tu pareja o tus amigos los lugares a los que quieren ir y, cuando van, dejar cada uno su reseña, puntaje y fotos.

- **Grupos**: cada usuario puede estar en varios (la pareja, los amigos del club, la familia) y elige cuál ver.
- **Invitaciones** con un link por WhatsApp (o cualquier app), o con un código.
- **Login** con Google o con mail y contraseña.
- Lugares pendientes/visitados, categorías (fijas y propias de cada grupo), etiquetas, fecha, mapa (Google Maps), reseñas por persona y fotos.
- Se actualiza en vivo cuando otro miembro carga algo (Supabase Realtime).

**Stack** (el mismo que `gastos`): Kotlin · Jetpack Compose + Material 3 · Navigation Compose · supabase-kt (Auth, Postgrest, Realtime, Storage) · Ktor OkHttp · kotlinx.serialization · Coil 3 · Google Maps (maps-compose) · Credential Manager. Backend en Supabase (Postgres + RLS, Storage, Edge Functions). El APK lo compila y publica GitHub Actions.

Puesta en marcha: [docs/setup.md](docs/setup.md).

## Estructura

```
android/app/src/main/java/com/fdm/plancito/
  PlancitoApp.kt        Application: Repository, Prefs, GroupStore y cambios en vivo (Realtime)
  data/                 Repository (Supabase), modelos, grupo activo, geocoding (Nominatim),
                        invitaciones, compresión de fotos, Google Sign-In
  ui/
    MainActivity.kt     sesión → invitación pendiente → bienvenida → navegación con barra inferior
    login/              Google o mail + contraseña
    groups/             lista, crear/unirse, detalle (miembros, invitar), unirse por link
    places/             lista (pendientes/visitados), detalle (¡Fuimos!, reseñas, fotos), formulario
    map/                mapa general y componente PlaceMap de Google Maps (también selector de ubicación)
    reviews/ photos/ profile/
    common/ theme/      componentes y tokens de color (claro/oscuro)
supabase/
  migrations/           esquema, RLS y storage
  functions/invite/     link de invitación que abre la app (o la descarga del APK)
```

### Modelo de datos

- **groups**: nombre, `invite_code` (la columna `emoji` quedó sin uso: la app muestra las iniciales del nombre). **group_members**: (grupo, usuario, `owner`/`member`). Altas y bajas por RPC: `create_group`, `join_group`, `leave_group`, `regenerate_invite`, `invite_preview`.
- **profiles**: uno por usuario (lo crea un trigger; con Google toma nombre y foto).
- **places**: del grupo (`group_id`), `wishlist`/`visited`, categoría (una fija o el id de una propia), etiquetas, fecha planeada, ubicación.
- **categories**: categorías propias de cada grupo (nombre e ícono); al borrar una, sus lugares pasan a "otro".
- **reviews**: una por persona por lugar. **photos**: el archivo vive en el bucket privado `photos` en `{group_id}/{place_id}/{uuid}.jpg`.

La seguridad está en la base (RLS con `my_group_ids()`): cada uno ve solo los grupos donde es miembro, edita solo su reseña y borra solo sus fotos.

## Tests

```bash
cd android && gradle testDebugUnitTest
```
