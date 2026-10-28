// Link de invitación que se comparte por WhatsApp/Telegram/etc:
//   https://<ref>.supabase.co/functions/v1/invite?c=ABC123
// Redirige a la app (plancito://join/ABC123). Supabase no sirve HTML desde
// *.supabase.co, por eso es un redirect y no una página: Chrome (y la pestaña
// que abre WhatsApp) abre el intent en la app o, si no está instalada, va a
// browser_fallback_url (la descarga del APK).

const PACKAGE = 'com.fdm.plancito';
const DEFAULT_APK_URL = 'https://github.com/frandiaz99/leira-app/releases/latest';

Deno.serve((req) => {
  const code = (new URL(req.url).searchParams.get('c') ?? '').trim().toUpperCase();
  const fallback = Deno.env.get('APK_URL') ?? DEFAULT_APK_URL;
  if (!/^[A-Z0-9]{4,12}$/.test(code)) {
    return Response.redirect(fallback, 302);
  }
  const intent =
    `intent://join/${code}#Intent;scheme=plancito;package=${PACKAGE};` +
    `S.browser_fallback_url=${encodeURIComponent(fallback)};end`;
  return new Response(null, { status: 302, headers: { Location: intent } });
});
