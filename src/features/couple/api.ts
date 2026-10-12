import type { Tables } from '@/lib/database.types'
import { supabase, unwrap } from '@/lib/supabase'

export type Profile = Tables<'profiles'>
export type Couple = Tables<'couples'>

export async function fetchMyProfile(userId: string): Promise<Profile> {
  return unwrap(await supabase.from('profiles').select('*').eq('id', userId).single())
}

export async function fetchCouple(coupleId: string): Promise<Couple> {
  return unwrap(await supabase.from('couples').select('*').eq('id', coupleId).single())
}

export async function fetchMembers(coupleId: string): Promise<Profile[]> {
  return unwrap(await supabase.from('profiles').select('*').eq('couple_id', coupleId).order('created_at'))
}

export async function createCouple(name: string): Promise<Couple> {
  return unwrap(await supabase.rpc('create_couple', { couple_name: name }))
}

export async function joinCouple(code: string): Promise<Couple> {
  return unwrap(await supabase.rpc('join_couple', { code }))
}

export async function updateProfile(userId: string, patch: { display_name?: string }) {
  unwrap(await supabase.from('profiles').update(patch).eq('id', userId))
}

export async function updateCoupleName(coupleId: string, name: string) {
  unwrap(await supabase.from('couples').update({ name }).eq('id', coupleId))
}
