// Tipos de la base. Se pueden regenerar con:
//   npx supabase gen types typescript --project-id <id> > src/lib/database.types.ts

export type Json = string | number | boolean | null | { [key: string]: Json | undefined } | Json[]

type CoupleRow = {
  id: string
  name: string
  invite_code: string
  created_at: string
}

export type Database = {
  __InternalSupabase: {
    PostgrestVersion: '12'
  }
  public: {
    Tables: {
      couples: {
        Row: CoupleRow
        Insert: {
          id?: string
          name?: string
          invite_code?: string
          created_at?: string
        }
        Update: {
          name?: string
        }
        Relationships: []
      }
      profiles: {
        Row: {
          id: string
          display_name: string
          avatar_url: string | null
          couple_id: string | null
          created_at: string
        }
        Insert: {
          id: string
          display_name?: string
          avatar_url?: string | null
          couple_id?: string | null
          created_at?: string
        }
        Update: {
          display_name?: string
          avatar_url?: string | null
        }
        Relationships: [
          {
            foreignKeyName: 'profiles_couple_id_fkey'
            columns: ['couple_id']
            isOneToOne: false
            referencedRelation: 'couples'
            referencedColumns: ['id']
          },
        ]
      }
      places: {
        Row: {
          id: string
          couple_id: string
          created_by: string
          name: string
          description: string | null
          address: string | null
          lat: number | null
          lng: number | null
          category: Database['public']['Enums']['place_category']
          tags: string[]
          planned_date: string | null
          status: Database['public']['Enums']['place_status']
          visited_at: string | null
          link: string | null
          created_at: string
        }
        Insert: {
          id?: string
          couple_id?: string
          created_by?: string
          name: string
          description?: string | null
          address?: string | null
          lat?: number | null
          lng?: number | null
          category?: Database['public']['Enums']['place_category']
          tags?: string[]
          planned_date?: string | null
          status?: Database['public']['Enums']['place_status']
          visited_at?: string | null
          link?: string | null
          created_at?: string
        }
        Update: {
          name?: string
          description?: string | null
          address?: string | null
          lat?: number | null
          lng?: number | null
          category?: Database['public']['Enums']['place_category']
          tags?: string[]
          planned_date?: string | null
          status?: Database['public']['Enums']['place_status']
          visited_at?: string | null
          link?: string | null
        }
        Relationships: [
          {
            foreignKeyName: 'places_couple_id_fkey'
            columns: ['couple_id']
            isOneToOne: false
            referencedRelation: 'couples'
            referencedColumns: ['id']
          },
          {
            foreignKeyName: 'places_created_by_fkey'
            columns: ['created_by']
            isOneToOne: false
            referencedRelation: 'profiles'
            referencedColumns: ['id']
          },
        ]
      }
      reviews: {
        Row: {
          id: string
          place_id: string
          user_id: string
          rating: number
          comment: string | null
          would_return: boolean | null
          created_at: string
          updated_at: string
        }
        Insert: {
          id?: string
          place_id: string
          user_id?: string
          rating: number
          comment?: string | null
          would_return?: boolean | null
          created_at?: string
          updated_at?: string
        }
        Update: {
          rating?: number
          comment?: string | null
          would_return?: boolean | null
        }
        Relationships: [
          {
            foreignKeyName: 'reviews_place_id_fkey'
            columns: ['place_id']
            isOneToOne: false
            referencedRelation: 'places'
            referencedColumns: ['id']
          },
          {
            foreignKeyName: 'reviews_user_id_fkey'
            columns: ['user_id']
            isOneToOne: false
            referencedRelation: 'profiles'
            referencedColumns: ['id']
          },
        ]
      }
      photos: {
        Row: {
          id: string
          place_id: string
          uploaded_by: string
          storage_path: string
          caption: string | null
          created_at: string
        }
        Insert: {
          id?: string
          place_id: string
          uploaded_by?: string
          storage_path: string
          caption?: string | null
          created_at?: string
        }
        Update: {
          caption?: string | null
        }
        Relationships: [
          {
            foreignKeyName: 'photos_place_id_fkey'
            columns: ['place_id']
            isOneToOne: false
            referencedRelation: 'places'
            referencedColumns: ['id']
          },
          {
            foreignKeyName: 'photos_uploaded_by_fkey'
            columns: ['uploaded_by']
            isOneToOne: false
            referencedRelation: 'profiles'
            referencedColumns: ['id']
          },
        ]
      }
    }
    Views: { [_ in never]: never }
    Functions: {
      my_couple_id: { Args: never; Returns: string }
      create_couple: { Args: { couple_name?: string }; Returns: CoupleRow }
      join_couple: { Args: { code: string }; Returns: CoupleRow }
    }
    Enums: {
      place_category: 'restaurante' | 'bar' | 'cafe' | 'viaje' | 'paseo' | 'experiencia' | 'otro'
      place_status: 'wishlist' | 'visited'
    }
    CompositeTypes: { [_ in never]: never }
  }
}

type PublicSchema = Database['public']
export type Tables<T extends keyof PublicSchema['Tables']> = PublicSchema['Tables'][T]['Row']
export type TablesInsert<T extends keyof PublicSchema['Tables']> = PublicSchema['Tables'][T]['Insert']
export type TablesUpdate<T extends keyof PublicSchema['Tables']> = PublicSchema['Tables'][T]['Update']
export type Enums<T extends keyof PublicSchema['Enums']> = PublicSchema['Enums'][T]
