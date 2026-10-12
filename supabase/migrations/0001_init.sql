-- Leira: esquema inicial
-- Correr en Supabase → SQL Editor (o `npx supabase db push`).

-- ───────────────────────── Tipos ─────────────────────────
create type public.place_category as enum (
  'restaurante', 'bar', 'cafe', 'viaje', 'paseo', 'experiencia', 'otro'
);
create type public.place_status as enum ('wishlist', 'visited');

-- ───────────────────────── Tablas ─────────────────────────
create table public.couples (
  id          uuid primary key default gen_random_uuid(),
  name        text not null default 'Nosotros',
  invite_code text not null unique default upper(substr(md5(random()::text), 1, 6)),
  created_at  timestamptz not null default now()
);

create table public.profiles (
  id           uuid primary key references auth.users on delete cascade,
  display_name text not null default '',
  avatar_url   text,
  couple_id    uuid references public.couples on delete set null,
  created_at   timestamptz not null default now()
);

-- Devuelve la pareja del usuario logueado. security definer para que las
-- policies puedan usarla sin recursión de RLS sobre profiles.
create function public.my_couple_id()
returns uuid
language sql
stable
security definer
set search_path = ''
as $$
  select couple_id from public.profiles where id = auth.uid()
$$;

create table public.places (
  id           uuid primary key default gen_random_uuid(),
  couple_id    uuid not null default public.my_couple_id() references public.couples on delete cascade,
  created_by   uuid not null default auth.uid() references public.profiles on delete cascade,
  name         text not null check (length(trim(name)) > 0),
  description  text,
  address      text,
  lat          double precision,
  lng          double precision,
  category     public.place_category not null default 'otro',
  tags         text[] not null default '{}',
  planned_date date,
  status       public.place_status not null default 'wishlist',
  visited_at   date,
  link         text,
  created_at   timestamptz not null default now()
);
create index places_couple_idx on public.places (couple_id);

create table public.reviews (
  id           uuid primary key default gen_random_uuid(),
  place_id     uuid not null references public.places on delete cascade,
  user_id      uuid not null default auth.uid() references public.profiles on delete cascade,
  rating       smallint not null check (rating between 1 and 5),
  comment      text,
  would_return boolean,
  created_at   timestamptz not null default now(),
  updated_at   timestamptz not null default now(),
  unique (place_id, user_id)
);
create index reviews_place_idx on public.reviews (place_id);

create table public.photos (
  id           uuid primary key default gen_random_uuid(),
  place_id     uuid not null references public.places on delete cascade,
  uploaded_by  uuid not null default auth.uid() references public.profiles on delete cascade,
  storage_path text not null unique,
  caption      text,
  created_at   timestamptz not null default now()
);
create index photos_place_idx on public.photos (place_id);

-- ───────────────────────── Triggers ─────────────────────────
-- Crea el profile al registrarse.
create function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.profiles (id, display_name)
  values (new.id, coalesce(new.raw_user_meta_data ->> 'display_name', split_part(new.email, '@', 1)));
  return new;
end;
$$;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

create function public.touch_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

create trigger reviews_touch before update on public.reviews
  for each row execute function public.touch_updated_at();

-- ───────────────────────── RPCs de pareja ─────────────────────────
create function public.create_couple(couple_name text default 'Nosotros')
returns public.couples
language plpgsql
security definer
set search_path = ''
as $$
declare
  c public.couples;
begin
  if auth.uid() is null then
    raise exception 'No autenticado';
  end if;
  if public.my_couple_id() is not null then
    raise exception 'Ya pertenecés a una pareja';
  end if;

  insert into public.couples (name) values (coalesce(nullif(trim(couple_name), ''), 'Nosotros'))
  returning * into c;
  update public.profiles set couple_id = c.id where id = auth.uid();
  return c;
end;
$$;

create function public.join_couple(code text)
returns public.couples
language plpgsql
security definer
set search_path = ''
as $$
declare
  c public.couples;
  members int;
begin
  if auth.uid() is null then
    raise exception 'No autenticado';
  end if;
  if public.my_couple_id() is not null then
    raise exception 'Ya pertenecés a una pareja';
  end if;

  select * into c from public.couples where invite_code = upper(trim(code));
  if not found then
    raise exception 'Código inválido';
  end if;

  select count(*) into members from public.profiles where couple_id = c.id;
  if members >= 2 then
    raise exception 'Esa pareja ya está completa';
  end if;

  update public.profiles set couple_id = c.id where id = auth.uid();
  return c;
end;
$$;

-- ───────────────────────── RLS ─────────────────────────
alter table public.couples  enable row level security;
alter table public.profiles enable row level security;
alter table public.places   enable row level security;
alter table public.reviews  enable row level security;
alter table public.photos   enable row level security;

-- couples: solo se crean vía RPC
create policy "ver mi pareja" on public.couples
  for select to authenticated using (id = public.my_couple_id());
create policy "editar mi pareja" on public.couples
  for update to authenticated using (id = public.my_couple_id());
revoke update on public.couples from authenticated;
grant update (name) on public.couples to authenticated;

-- profiles: me veo a mí y a mi pareja; solo edito nombre/avatar propios
create policy "ver perfiles de mi pareja" on public.profiles
  for select to authenticated
  using (id = auth.uid() or couple_id = public.my_couple_id());
create policy "editar mi perfil" on public.profiles
  for update to authenticated using (id = auth.uid());
revoke update on public.profiles from authenticated;
grant update (display_name, avatar_url) on public.profiles to authenticated;

-- places: todo dentro de la pareja
create policy "ver lugares" on public.places
  for select to authenticated using (couple_id = public.my_couple_id());
create policy "crear lugares" on public.places
  for insert to authenticated
  with check (couple_id = public.my_couple_id() and created_by = auth.uid());
create policy "editar lugares" on public.places
  for update to authenticated
  using (couple_id = public.my_couple_id())
  with check (couple_id = public.my_couple_id());
create policy "borrar lugares" on public.places
  for delete to authenticated using (couple_id = public.my_couple_id());

-- reviews: ambos ven todas; cada uno maneja la suya
create policy "ver reseñas" on public.reviews
  for select to authenticated
  using (exists (select 1 from public.places p
                 where p.id = place_id and p.couple_id = public.my_couple_id()));
create policy "crear mi reseña" on public.reviews
  for insert to authenticated
  with check (user_id = auth.uid() and exists (
    select 1 from public.places p where p.id = place_id and p.couple_id = public.my_couple_id()));
create policy "editar mi reseña" on public.reviews
  for update to authenticated
  using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy "borrar mi reseña" on public.reviews
  for delete to authenticated using (user_id = auth.uid());

-- photos: ambos ven y suben; cada uno borra las suyas
create policy "ver fotos" on public.photos
  for select to authenticated
  using (exists (select 1 from public.places p
                 where p.id = place_id and p.couple_id = public.my_couple_id()));
create policy "subir fotos" on public.photos
  for insert to authenticated
  with check (uploaded_by = auth.uid() and exists (
    select 1 from public.places p where p.id = place_id and p.couple_id = public.my_couple_id()));
create policy "borrar mis fotos" on public.photos
  for delete to authenticated using (uploaded_by = auth.uid());

-- ───────────────────────── Storage ─────────────────────────
-- Bucket privado. Path: {couple_id}/{place_id}/{uuid}.jpg
insert into storage.buckets (id, name, public)
values ('photos', 'photos', false)
on conflict (id) do nothing;

create policy "leer fotos de mi pareja" on storage.objects
  for select to authenticated
  using (bucket_id = 'photos' and (storage.foldername(name))[1] = public.my_couple_id()::text);
create policy "subir fotos a mi pareja" on storage.objects
  for insert to authenticated
  with check (bucket_id = 'photos' and (storage.foldername(name))[1] = public.my_couple_id()::text);
create policy "borrar fotos de mi pareja" on storage.objects
  for delete to authenticated
  using (bucket_id = 'photos' and (storage.foldername(name))[1] = public.my_couple_id()::text);
