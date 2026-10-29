// Avisa a los otros miembros del grupo cuando alguien agrega un lugar o deja una reseña.
// La llama la app después de guardar: { kind: "place" | "review", place_id }.
// Manda mensajes de datos por Firebase Cloud Messaging (API HTTP v1) a los teléfonos
// guardados en device_tokens. Secret necesario: FCM_SERVICE_ACCOUNT (el JSON de la
// cuenta de servicio de Firebase). Sin él responde sin mandar nada.

import { createClient } from 'npm:@supabase/supabase-js@2.45.4';

type ServiceAccount = { project_id: string; client_email: string; private_key: string };

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });

Deno.serve(async (req) => {
  const raw = Deno.env.get('FCM_SERVICE_ACCOUNT');
  if (!raw) return json({ sent: 0, reason: 'Falta el secret FCM_SERVICE_ACCOUNT' });
  const account: ServiceAccount = JSON.parse(raw);

  const { kind, place_id } = await req.json().catch(() => ({}));
  if ((kind !== 'place' && kind !== 'review') || typeof place_id !== 'string') {
    return json({ error: 'Pedido inválido' }, 400);
  }

  const url = Deno.env.get('SUPABASE_URL')!;
  // Como el usuario que llama: RLS garantiza que el lugar es de uno de sus grupos.
  const asUser = createClient(url, Deno.env.get('SUPABASE_ANON_KEY')!, {
    global: { headers: { Authorization: req.headers.get('Authorization') ?? '' } },
  });
  const { data: { user } } = await asUser.auth.getUser();
  if (!user) return json({ error: 'Sin sesión' }, 401);
  const { data: place } = await asUser
    .from('places')
    .select('id, name, group_id, groups(name)')
    .eq('id', place_id)
    .maybeSingle();
  if (!place) return json({ error: 'Lugar no encontrado' }, 404);

  // Con la service role: los tokens de los demás miembros no los ve el usuario.
  const admin = createClient(url, Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!);
  const { data: me } = await admin.from('profiles').select('display_name').eq('id', user.id).maybeSingle();
  const who = me?.display_name || 'Alguien';
  const groupName = (place.groups as { name?: string } | null)?.name ?? '';

  let title: string;
  let body: string;
  if (kind === 'place') {
    title = `${who} agregó ${place.name}`;
    body = groupName ? `Nuevo lugar en ${groupName}` : 'Nuevo lugar';
  } else {
    const { data: review } = await admin
      .from('reviews')
      .select('rating, comment')
      .eq('place_id', place.id)
      .eq('user_id', user.id)
      .maybeSingle();
    title = `${who} reseñó ${place.name}`;
    body = review ? `${review.rating}/5` + (review.comment ? ` · ${review.comment}` : '') : 'Nueva reseña';
  }

  const { data: members } = await admin
    .from('group_members')
    .select('user_id')
    .eq('group_id', place.group_id)
    .neq('user_id', user.id);
  const ids = (members ?? []).map((m) => m.user_id);
  if (ids.length === 0) return json({ sent: 0 });
  const { data: tokens } = await admin.from('device_tokens').select('token').in('user_id', ids);
  if (!tokens?.length) return json({ sent: 0 });

  const accessToken = await googleAccessToken(account);
  let sent = 0;
  for (const { token } of tokens) {
    const res = await fetch(`https://fcm.googleapis.com/v1/projects/${account.project_id}/messages:send`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${accessToken}`, 'Content-Type': 'application/json' },
      body: JSON.stringify({
        message: {
          token,
          android: { priority: 'high' },
          // Solo datos: la app arma la notificación (canal, ícono y qué abrir al tocarla).
          data: { title, body, place_id: place.id, group_id: place.group_id },
        },
      }),
    });
    if (res.ok) {
      sent++;
    } else if (res.status === 404 || res.status === 400) {
      // Token vencido o de una app desinstalada: se borra.
      await admin.from('device_tokens').delete().eq('token', token);
    }
  }
  return json({ sent });
});

/** Token OAuth de la cuenta de servicio (JWT firmado con RS256), para la API de FCM. */
async function googleAccessToken(account: ServiceAccount): Promise<string> {
  const now = Math.floor(Date.now() / 1000);
  const encode = (obj: unknown) => base64url(new TextEncoder().encode(JSON.stringify(obj)));
  const unsigned = `${encode({ alg: 'RS256', typ: 'JWT' })}.${encode({
    iss: account.client_email,
    scope: 'https://www.googleapis.com/auth/firebase.messaging',
    aud: 'https://oauth2.googleapis.com/token',
    iat: now,
    exp: now + 3600,
  })}`;
  const pem = account.private_key.replace(/-----[^-]+-----/g, '').replace(/\s/g, '');
  const der = Uint8Array.from(atob(pem), (c) => c.charCodeAt(0));
  const key = await crypto.subtle.importKey(
    'pkcs8',
    der,
    { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' },
    false,
    ['sign'],
  );
  const signature = new Uint8Array(
    await crypto.subtle.sign('RSASSA-PKCS1-v1_5', key, new TextEncoder().encode(unsigned)),
  );
  const res = await fetch('https://oauth2.googleapis.com/token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer',
      assertion: `${unsigned}.${base64url(signature)}`,
    }),
  });
  const data = await res.json();
  if (!data.access_token) throw new Error(`No se pudo obtener el token de Google: ${JSON.stringify(data)}`);
  return data.access_token;
}

function base64url(bytes: Uint8Array): string {
  let s = '';
  for (const b of bytes) s += String.fromCharCode(b);
  return btoa(s).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}
