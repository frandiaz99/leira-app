-- "Me interesa" por persona, precio aproximado y teléfonos para los avisos.

-- ───────────────────────── Precio ─────────────────────────
-- 1 a 4 ($ a $$$$); null = sin dato.
alter table public.places add column price smallint check (price between 1 and 4);

-- ───────────────────────── Me interesa ─────────────────────────
create table public.place_interests (
  place_id   uuid not null references public.places on delete cascade,
  user_id    uuid not null references public.profiles on delete cascade default auth.uid(),
  created_at timestamptz not null default now(),
  primary key (place_id, user_id)
);

alter table public.place_interests enable row level security;

-- Las ve todo el grupo; cada uno marca y desmarca el suyo.
create policy "ver interesados" on public.place_interests
  for select to authenticated
  using (exists (select 1 from public.places p
                 where p.id = place_id and p.group_id in (select public.my_group_ids())));
create policy "marcar me interesa" on public.place_interests
  for insert to authenticated
  with check (user_id = auth.uid() and exists (
    select 1 from public.places p where p.id = place_id and p.group_id in (select public.my_group_ids())));
create policy "desmarcar me interesa" on public.place_interests
  for delete to authenticated using (user_id = auth.uid());

-- ───────────────────────── Avisos (Firebase Cloud Messaging) ─────────────────────────
-- Un token por teléfono. Lo lee la Edge Function "notify" con la service role.
create table public.device_tokens (
  token      text primary key,
  user_id    uuid not null references public.profiles on delete cascade default auth.uid(),
  updated_at timestamptz not null default now()
);

alter table public.device_tokens enable row level security;

create policy "mis teléfonos" on public.device_tokens
  for all to authenticated
  using (user_id = auth.uid())
  with check (user_id = auth.uid());
