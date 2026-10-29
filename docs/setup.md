# Puesta en marcha

Ningún secreto va en el código: la app solo usa la URL y la anon key públicas de Supabase y el client ID público de Google.

## 0. Herramientas

| Herramienta | Para qué |
|---|---|
| [Android Studio](https://developer.android.com/studio) (incluye JDK 17 y SDK) | compilar e instalar la app |
| [Supabase CLI](https://supabase.com/docs/guides/cli) (o el MCP de Supabase en Claude Code) | migraciones y Edge Functions |

## 1. Supabase (proyecto `pyxlvumbetsoxrqjaxec`)

1. Aplicá las migraciones de `supabase/migrations/` en orden. `0001_init.sql` es el esquema original de la PWA (si ya se corrió en el SQL Editor, saltealo); `20261028000001_groups.sql` pasa de "pareja" a grupos y migra los datos existentes.
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
3. El Client ID *Web* va en `google.webClientId` (paso 3). Si queda vacío, la app oculta "Continuar con Google".

**Si "Continuar con Google" no entra**, la app muestra la causa debajo del formulario (y en `adb logcat -s Login`):

| Mensaje / síntoma | Qué falta |
|---|---|
| "Google rechazó la app" o "no ofreció ninguna cuenta" | El OAuth client **Android** con el SHA-1 de la firma de *ese* APK. El APK de Releases se firma con `plancito.jks`, el de Android Studio con el debug keystore: cada uno necesita su client. |
| "Google no está habilitado en Supabase" | Habilitar Google en Supabase → Authentication → Providers. |
| "Supabase no acepta este Client ID" | En ese provider, el campo *Client IDs* tiene que tener el Client ID **Web** (el mismo de `google.webClientId`). |

## 3. App

`android/local.properties` (no se versiona):

```properties
supabase.url=https://pyxlvumbetsoxrqjaxec.supabase.co
supabase.anonKey=eyJ...
google.webClientId=xxxx.apps.googleusercontent.com
```

Abrí `android/` en Android Studio y corré la app.

### Clave de firma y APK en GitHub

Generá la clave una sola vez y guardala aparte (si se pierde, no se puede actualizar la app instalada):

```bash
mkdir -p android/signing
keytool -genkeypair -v -keystore android/signing/plancito.jks -alias plancito -keyalg RSA -keysize 2048 -validity 10000
printf 'storeFile=plancito.jks\npassword=<la clave>\nalias=plancito\n' > android/signing/signing.properties
```

Secrets del repo en GitHub (*Settings → Secrets → Actions*): `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `GOOGLE_WEB_CLIENT_ID`, `SIGNING_PASSWORD` y `SIGNING_KEYSTORE_B64` (`base64 -w0 android/signing/plancito.jks`).

Cada push a `main` que toque `android/` corre los tests y publica el APK en *Releases*.
