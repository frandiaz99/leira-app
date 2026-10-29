# Puesta en marcha

Ningún secreto va en el código: la app solo usa la URL y la anon key públicas de Supabase y el client ID público de Google.

## 0. Herramientas

| Herramienta | Para qué |
|---|---|
| [Android Studio](https://developer.android.com/studio) (incluye JDK 17 y SDK) | compilar e instalar la app |
| [Supabase CLI](https://supabase.com/docs/guides/cli) (o el MCP de Supabase en Claude Code) | migraciones y Edge Functions |

## 1. Supabase (proyecto `pyxlvumbetsoxrqjaxec`)

1. Aplicá las migraciones de `supabase/migrations/` en orden. `0001_init.sql` es el esquema original de la PWA (si ya se corrió en el SQL Editor, saltealo); `20261028000001_groups.sql` pasa de "pareja" a grupos y migra los datos existentes; `20261029000001_custom_categories.sql` agrega las categorías propias de cada grupo; `20261030000001_interests_price_push.sql` agrega "Me interesa", el precio y los teléfonos para los avisos.
   ```bash
   supabase link --project-ref pyxlvumbetsoxrqjaxec
   supabase db push
   ```
2. Deploy del link de invitación:
   ```bash
   supabase functions deploy invite --no-verify-jwt
   ```
   Opcional: `supabase secrets set APK_URL=<link de descarga>` si el repo de Releases no es público.
3. **Authentication → Sign In / Providers → Email**: dejá habilitado el registro y **desactivá "Confirm email"**. Las cuentas se crean con mail y contraseña desde la app y no se manda ningún mail.

### MCP de Supabase en Claude Code

Ya está en `.mcp.json`. Al abrir Claude Code en esta carpeta, aprobá el servidor y autenticá con `/mcp`.

## 2. Google Sign-In

1. En [Google Cloud Console](https://console.cloud.google.com/) → *APIs y servicios → Credenciales*:
   - **OAuth client ID tipo "Web application"**: copiá el *Client ID* y el *Client secret*.
   - **OAuth client ID tipo "Android"** con package `com.fdm.plancito` y el SHA-1 del keystore de release (y otro con el de debug para probar desde Android Studio):
     ```bash
     keytool -list -v -keystore android/signing/plancito.jks -alias plancito
     keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android
     ```
2. Supabase → **Authentication → Providers → Google**: habilitalo y pegá el Client ID y el secret del cliente *Web*.
3. El Client ID *Web* va en `google.webClientId` (paso 5). Si queda vacío, la app oculta "Continuar con Google".

**Si "Continuar con Google" no entra**, la app muestra la causa debajo del formulario (y en `adb logcat -s Login`):

| Mensaje / síntoma | Qué falta |
|---|---|
| "Google rechazó la app" o "no ofreció ninguna cuenta" | El OAuth client **Android** con el SHA-1 de la firma de *ese* APK. El APK de Releases se firma con `plancito.jks`, el de Android Studio con el debug keystore: cada uno necesita su client. |
| "Google no está habilitado en Supabase" | Habilitar Google en Supabase → Authentication → Providers. |
| "Supabase no acepta este Client ID" | En ese provider, el campo *Client IDs* tiene que tener el Client ID **Web** (el mismo de `google.webClientId`). |

## 3. Google Maps

1. En [Google Cloud Console](https://console.cloud.google.com/), en el mismo proyecto: *APIs y servicios → Biblioteca* → **Maps SDK for Android** → *Habilitar*. Google Maps pide tener una cuenta de facturación asociada al proyecto, aunque los mapas en la app de Android no tienen costo.
2. *APIs y servicios → Credenciales → Crear credenciales → Clave de API*. Editala:
   - *Restricciones de aplicación*: **Apps de Android** → agregar `com.fdm.plancito` con el SHA-1 del keystore de release (el mismo del client OAuth Android; y el de debug si probás desde Android Studio).
   - *Restricciones de API*: **Maps SDK for Android**.
3. La clave va en `google.mapsApiKey` (paso 5) y en el secret `GOOGLE_MAPS_API_KEY` de GitHub. Si falta, la app muestra un aviso en lugar del mapa.

## 4. Avisos del grupo (Firebase, opcional)

Sin esto la app funciona igual; solo no llegan avisos cuando otro miembro agrega un lugar o deja una reseña. Los recordatorios de fechas no lo necesitan.

1. En [Firebase](https://console.firebase.google.com/) → *Agregar proyecto* → elegí el mismo proyecto de Google Cloud.
2. *Agregar app → Android* con el paquete `com.fdm.plancito`. Descargá `google-services.json`:
   - para compilar en tu compu, va en `android/app/google-services.json` (no se versiona);
   - para GitHub, pegá el contenido entero en el secret `GOOGLE_SERVICES_JSON`.
3. *Configuración del proyecto → Cuentas de servicio → Generar nueva clave privada* (un JSON) y cargalo en Supabase con la función:
   ```bash
   supabase secrets set FCM_SERVICE_ACCOUNT="$(cat cuenta-de-servicio.json)"
   supabase functions deploy notify
   ```

## 5. App

`android/local.properties` (no se versiona):

```properties
supabase.url=https://pyxlvumbetsoxrqjaxec.supabase.co
supabase.anonKey=eyJ...
google.webClientId=xxxx.apps.googleusercontent.com
google.mapsApiKey=AIza...
```

Abrí `android/` en Android Studio y corré la app.

### Clave de firma y APK en GitHub

Generá la clave una sola vez y guardala aparte (si se pierde, no se puede actualizar la app instalada):

```bash
mkdir -p android/signing
keytool -genkeypair -v -keystore android/signing/plancito.jks -alias plancito -keyalg RSA -keysize 2048 -validity 10000
printf 'storeFile=plancito.jks\npassword=<la clave>\nalias=plancito\n' > android/signing/signing.properties
```

Secrets del repo en GitHub (*Settings → Secrets → Actions*): `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `GOOGLE_WEB_CLIENT_ID`, `GOOGLE_MAPS_API_KEY`, `SIGNING_PASSWORD` y `SIGNING_KEYSTORE_B64` (`base64 -w0 android/signing/plancito.jks`). Opcional: `GOOGLE_SERVICES_JSON` (avisos).

Cada push a `main` que toque `android/` corre los tests y publica el APK en *Releases*.
