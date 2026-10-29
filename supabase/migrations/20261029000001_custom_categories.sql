-- Categorías propias de cada grupo, además de las fijas (restaurante, bar, …).
-- places.category pasa de enum a texto: guarda el valor de una categoría fija
-- o el id de una categoría propia del grupo.

alter table public.places alter column category drop default;
alter table public.places alter column category type text using category::text;
alter table public.places alter column category set default 'otro';
drop type public.place_category;

create table public.categories (
  id         uuid primary key default gen_random_uuid(),
  group_id   uuid not null references public.groups on delete cascade,
  label      text not null check (char_length(trim(label)) between 1 and 40),
  -- Nombre del ícono que muestra la app (ver ui/common/Icons.kt); no hay emojis.
  icon       text not null default 'place',
  created_by uuid references public.profiles on delete set null default auth.uid(),
  created_at timestamptz not null default now(),
  unique (group_id, label)
);

alter table public.categories enable row level security;

-- Cualquier miembro del grupo las ve, crea, renombra y borra.
create policy "ver categorías" on public.categories
  for select to authenticated using (group_id in (select public.my_group_ids()));
create policy "crear categorías" on public.categories
  for insert to authenticated with check (group_id in (select public.my_group_ids()));
create policy "editar categorías" on public.categories
  for update to authenticated
  using (group_id in (select public.my_group_ids()))
  with check (group_id in (select public.my_group_ids()));
create policy "borrar categorías" on public.categories
  for delete to authenticated using (group_id in (select public.my_group_ids()));

-- Al borrar una categoría propia, sus lugares pasan a "otro".
create function public.places_category_deleted()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  update public.places set category = 'otro'
  where group_id = old.group_id and category = old.id::text;
  return old;
end;
$$;

create trigger categories_reset_places
  after delete on public.categories
  for each row execute function public.places_category_deleted();
