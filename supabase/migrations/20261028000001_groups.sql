-- Plancito: de "pareja" (máx. 2, una por usuario) a grupos (varios por usuario, sin límite).
-- Las parejas existentes pasan a ser grupos y sus perfiles quedan como miembros.

-- ───────────────────────── Grupos ─────────────────────────
alter table public.couples rename to groups;
alter table public.groups
  add column emoji      text not null default '💞',
  add column created_by uuid references public.profiles on delete set null default auth.uid();
alter table public.groups alter column name set default 'Mi grupo';

create table public.group_members (
  group_id  uuid not null references public.groups on delete cascade,
  user_id   uuid not null references public.profiles on delete cascade,
  role      text not null default 'member' check (role in ('owner', 'member')),
  joined_at timestamptz not null default now(),
  primary key (group_id, user_id)
);
create index group_members_user_idx on public.group_members (user_id);

-- Migrar las parejas: cada perfil con couple_id es miembro; el más antiguo queda como dueño.
insert into public.group_members (group_id, user_id, joined_at)
select couple_id, id, created_at from public.profiles where couple_id is not null;

update public.group_members gm set role = 'owner'
from (
  select distinct on (group_id) group_id, user_id
  from public.group_members order by group_id, joined_at
) first
where gm.group_id = first.group_id and gm.user_id = first.user_id;

update public.groups g set created_by = gm.user_id
from public.group_members gm
where gm.group_id = g.id and gm.role = 'owner';

-- ───────────────────────── Sacar lo de la pareja ─────────────────────────
drop policy "ver mi pareja" on public.groups;
drop policy "editar mi pareja" on public.groups;
drop policy "ver perfiles de mi pareja" on public.profiles;
drop policy "ver lugares" on public.places;
drop policy "crear lugares" on public.places;
drop policy "editar lugares" on public.places;
drop policy "borrar lugares" on public.places;
drop policy "ver reseñas" on public.reviews;
drop policy "crear mi reseña" on public.reviews;
drop policy "ver fotos" on public.photos;
drop policy "subir fotos" on public.photos;
drop policy "leer fotos de mi pareja" on storage.objects;
drop policy "subir fotos a mi pareja" on storage.objects;
drop policy "borrar fotos de mi pareja" on storage.objects;

drop function public.create_couple(text);
drop function public.join_couple(text);

-- La app manda el grupo activo al crear un lugar.
alter table public.places rename column couple_id to group_id;
alter table public.places alter column group_id drop default;
alter index public.places_couple_idx rename to places_group_idx;

alter table public.profiles drop column couple_id;
drop function public.my_couple_id();

-- ───────────────────────── Helpers ─────────────────────────
-- Grupos del usuario logueado. security definer para que las policies la usen
-- sin recursión de RLS sobre group_members.
create function public.my_group_ids()
returns setof uuid
language sql
stable
security definer
set search_path = ''
as $$
  select group_id from public.group_members where user_id = auth.uid()
$$;

-- Perfil nuevo: también toma nombre y foto de Google.
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.profiles (id, display_name, avatar_url)
  values (
    new.id,
    coalesce(
      new.raw_user_meta_data ->> 'display_name',
      new.raw_user_meta_data ->> 'full_name',
      new.raw_user_meta_data ->> 'name',
      split_part(new.email, '@', 1)
    ),
    coalesce(new.raw_user_meta_data ->> 'avatar_url', new.raw_user_meta_data ->> 'picture')
  );
  return new;
end;
$$;

-- ───────────────────────── RPCs de grupos ─────────────────────────
create function public.create_group(group_name text, group_emoji text default null)
returns public.groups
language plpgsql
security definer
set search_path = ''
as $$
declare
  g public.groups;
begin
  if auth.uid() is null then
    raise exception 'No autenticado';
  end if;

  insert into public.groups (name, emoji, created_by)
  values (
    coalesce(nullif(trim(group_name), ''), 'Mi grupo'),
    coalesce(nullif(trim(group_emoji), ''), '💞'),
    auth.uid()
  )
  returning * into g;
  insert into public.group_members (group_id, user_id, role) values (g.id, auth.uid(), 'owner');
  return g;
end;
$$;

-- Si ya es miembro devuelve el grupo igual: el link de invitación se puede abrir dos veces.
create function public.join_group(code text)
returns public.groups
language plpgsql
security definer
set search_path = ''
as $$
declare
  g public.groups;
begin
  if auth.uid() is null then
    raise exception 'No autenticado';
  end if;

  select * into g from public.groups where invite_code = upper(trim(code));
  if not found then
    raise exception 'Código inválido';
  end if;

  insert into public.group_members (group_id, user_id) values (g.id, auth.uid())
  on conflict do nothing;
  return g;
end;
$$;

-- Nombre y cantidad de miembros antes de unirse (pantalla "Unirte a …").
create function public.invite_preview(code text)
returns table (id uuid, name text, emoji text, members int, already_member boolean)
language sql
stable
security definer
set search_path = ''
as $$
  select g.id, g.name, g.emoji,
         (select count(*)::int from public.group_members m where m.group_id = g.id),
         exists (select 1 from public.group_members m where m.group_id = g.id and m.user_id = auth.uid())
  from public.groups g
  where g.invite_code = upper(trim(code)) and auth.uid() is not null
$$;

-- Al salir el último miembro se borra el grupo (y en cascada lugares, reseñas y fotos).
-- Si sale el dueño, pasa a serlo el miembro más antiguo.
create function public.leave_group(gid uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  was_owner boolean;
begin
  delete from public.group_members where group_id = gid and user_id = auth.uid()
  returning role = 'owner' into was_owner;
  if not found then
    raise exception 'No sos miembro de ese grupo';
  end if;

  if not exists (select 1 from public.group_members where group_id = gid) then
    delete from public.groups where id = gid;
  elsif was_owner then
    update public.group_members set role = 'owner'
    where (group_id, user_id) = (
      select group_id, user_id from public.group_members where group_id = gid order by joined_at limit 1
    );
  end if;
end;
$$;

-- Invalida el link anterior (por si se compartió de más). Solo el dueño.
create function public.regenerate_invite(gid uuid)
returns public.groups
language plpgsql
security definer
set search_path = ''
as $$
declare
  g public.groups;
begin
  if not exists (select 1 from public.group_members where group_id = gid and user_id = auth.uid() and role = 'owner') then
    raise exception 'Solo quien administra el grupo puede cambiar el código';
  end if;
  update public.groups set invite_code = upper(substr(md5(random()::text), 1, 6))
  where id = gid returning * into g;
  return g;
end;
$$;

revoke execute on function public.my_group_ids(), public.create_group(text, text), public.join_group(text),
  public.invite_preview(text), public.leave_group(uuid), public.regenerate_invite(uuid) from public, anon;
grant execute on function public.my_group_ids(), public.create_group(text, text), public.join_group(text),
  public.invite_preview(text), public.leave_group(uuid), public.regenerate_invite(uuid) to authenticated;

-- ───────────────────────── RLS ─────────────────────────
alter table public.group_members enable row level security;

-- groups: se crean y se dejan vía RPC; cualquier miembro cambia nombre y emoji
create policy "ver mis grupos" on public.groups
  for select to authenticated using (id in (select public.my_group_ids()));
create policy "editar mis grupos" on public.groups
  for update to authenticated using (id in (select public.my_group_ids()));
revoke update on public.groups from authenticated;
grant update (name, emoji) on public.groups to authenticated;

-- group_members: solo lectura (altas y bajas por RPC)
create policy "ver miembros de mis grupos" on public.group_members
  for select to authenticated using (group_id in (select public.my_group_ids()));

-- profiles: me veo a mí y a quienes comparten algún grupo conmigo
create policy "ver perfiles de mis grupos" on public.profiles
  for select to authenticated
  using (id = auth.uid() or id in (
    select m.user_id from public.group_members m where m.group_id in (select public.my_group_ids())));

-- places: todo dentro de los grupos del usuario
create policy "ver lugares" on public.places
  for select to authenticated using (group_id in (select public.my_group_ids()));
create policy "crear lugares" on public.places
  for insert to authenticated
  with check (group_id in (select public.my_group_ids()) and created_by = auth.uid());
create policy "editar lugares" on public.places
  for update to authenticated
  using (group_id in (select public.my_group_ids()))
  with check (group_id in (select public.my_group_ids()));
create policy "borrar lugares" on public.places
  for delete to authenticated using (group_id in (select public.my_group_ids()));

-- reviews y photos: las ve todo el grupo; cada uno maneja las suyas
create policy "ver reseñas" on public.reviews
  for select to authenticated
  using (exists (select 1 from public.places p
                 where p.id = place_id and p.group_id in (select public.my_group_ids())));
create policy "crear mi reseña" on public.reviews
  for insert to authenticated
  with check (user_id = auth.uid() and exists (
    select 1 from public.places p where p.id = place_id and p.group_id in (select public.my_group_ids())));

create policy "ver fotos" on public.photos
  for select to authenticated
  using (exists (select 1 from public.places p
                 where p.id = place_id and p.group_id in (select public.my_group_ids())));
create policy "subir fotos" on public.photos
  for insert to authenticated
  with check (uploaded_by = auth.uid() and exists (
    select 1 from public.places p where p.id = place_id and p.group_id in (select public.my_group_ids())));

-- ───────────────────────── Storage ─────────────────────────
-- Path: {group_id}/{place_id}/{uuid}.jpg
create policy "leer fotos de mis grupos" on storage.objects
  for select to authenticated
  using (bucket_id = 'photos' and (storage.foldername(name))[1] in (select public.my_group_ids()::text));
create policy "subir fotos a mis grupos" on storage.objects
  for insert to authenticated
  with check (bucket_id = 'photos' and (storage.foldername(name))[1] in (select public.my_group_ids()::text));
create policy "borrar fotos de mis grupos" on storage.objects
  for delete to authenticated
  using (bucket_id = 'photos' and (storage.foldername(name))[1] in (select public.my_group_ids()::text));

-- ───────────────────────── Realtime ─────────────────────────
-- La app se refresca cuando otro miembro carga o reseña algo (Realtime respeta RLS).
alter publication supabase_realtime add table public.places, public.reviews, public.photos, public.group_members;
