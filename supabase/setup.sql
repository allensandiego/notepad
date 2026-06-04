-- =============================================================================
-- Supabase Database & Storage Initialization Script
-- File: supabase/setup.sql
-- Description: Consolidated setup script to initialize all profiles, metadata
--              tables, storage buckets, and row-level security (RLS) policies
--              for the Android Notepad application.
-- Instructions: Copy and paste the entire contents of this file into the
--               Supabase SQL Editor and click "Run".
-- =============================================================================

-- -----------------------------------------------------------------------------
-- SECTION 1: profiles table (Auth Integration)
-- Mirrors users created in auth.users to provide a public profile record.
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS public.profiles (
    id           UUID PRIMARY KEY REFERENCES auth.users (id) ON DELETE CASCADE,
    display_name TEXT,
    avatar_url   TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index for fast lookups by display name.
CREATE INDEX IF NOT EXISTS profiles_display_name_idx ON public.profiles (display_name);

-- Automatically update updated_at column on modifications.
CREATE OR REPLACE FUNCTION public.handle_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS on_profiles_updated ON public.profiles;
CREATE TRIGGER on_profiles_updated
    BEFORE UPDATE ON public.profiles
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_updated_at();

-- Automatically create a profile row when a new user signs up.
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    INSERT INTO public.profiles (id, display_name, avatar_url)
    VALUES (
        NEW.id,
        NEW.raw_user_meta_data ->> 'display_name',
        NEW.raw_user_meta_data ->> 'avatar_url'
    )
    ON CONFLICT (id) DO NOTHING;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_new_user();


-- -----------------------------------------------------------------------------
-- SECTION 2: Row-Level Security on public.profiles
-- -----------------------------------------------------------------------------

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

-- Allow public read access to all profiles.
DROP POLICY IF EXISTS "profiles: public read" ON public.profiles;
CREATE POLICY "profiles: public read"
    ON public.profiles
    FOR SELECT
    TO public
    USING (true);

-- Allow authenticated users to insert their own profile.
DROP POLICY IF EXISTS "profiles: owner insert" ON public.profiles;
CREATE POLICY "profiles: owner insert"
    ON public.profiles
    FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = id);

-- Allow authenticated users to update their own profile.
DROP POLICY IF EXISTS "profiles: owner update" ON public.profiles;
CREATE POLICY "profiles: owner update"
    ON public.profiles
    FOR UPDATE
    TO authenticated
    USING      (auth.uid() = id)
    WITH CHECK (auth.uid() = id);

-- Allow authenticated users to delete their own profile.
DROP POLICY IF EXISTS "profiles: owner delete" ON public.profiles;
CREATE POLICY "profiles: owner delete"
    ON public.profiles
    FOR DELETE
    TO authenticated
    USING (auth.uid() = id);


-- -----------------------------------------------------------------------------
-- SECTION 3: Metadata Tables (App Schemas)
-- Core tables used by the Notepad client.
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS public.custom_tables (
    id              TEXT PRIMARY KEY,
    name            TEXT NOT NULL UNIQUE,
    parent_table_id TEXT REFERENCES public.custom_tables(id) ON DELETE CASCADE,
    created_at      BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS public.custom_fields (
    id           TEXT PRIMARY KEY,
    table_id     TEXT NOT NULL REFERENCES public.custom_tables(id) ON DELETE CASCADE,
    name         TEXT NOT NULL,
    type         TEXT NOT NULL,
    required     BOOLEAN NOT NULL,
    default_value TEXT,
    default_type TEXT,
    is_system    BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS public.custom_records (
    id         TEXT PRIMARY KEY,
    table_id   TEXT NOT NULL REFERENCES public.custom_tables(id) ON DELETE CASCADE,
    created_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS public.custom_values (
    id         TEXT PRIMARY KEY,
    record_id  TEXT NOT NULL REFERENCES public.custom_records(id) ON DELETE CASCADE,
    field_id   TEXT NOT NULL REFERENCES public.custom_fields(id) ON DELETE CASCADE,
    value_text TEXT,
    UNIQUE(record_id, field_id)
);

-- Disable Row Level Security on data tables to allow client-side PostgREST synchronization.
ALTER TABLE public.custom_tables DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.custom_fields DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.custom_records DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.custom_values DISABLE ROW LEVEL SECURITY;


-- -----------------------------------------------------------------------------
-- SECTION 4: Storage Buckets Provisioning
-- -----------------------------------------------------------------------------

-- 1. Create 'user-uploads' bucket (used for namespaced user attachments)
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'user-uploads',
    'user-uploads',
    true,
    52428800,   -- 50 MB file limit
    ARRAY[
        'image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/svg+xml',
        'application/pdf', 'text/plain', 'application/msword',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
    ]
)
ON CONFLICT (id) DO NOTHING;

-- 2. Create 'attachments' bucket (fallback/default app attachments bucket)
INSERT INTO storage.buckets (id, name, public)
VALUES ('attachments', 'attachments', true)
ON CONFLICT (id) DO NOTHING;


-- -----------------------------------------------------------------------------
-- SECTION 5: Row-Level Security on storage.objects
-- -----------------------------------------------------------------------------

-- Enable RLS on storage.objects to apply policies.
ALTER TABLE storage.objects ENABLE ROW LEVEL SECURITY;

-- --- Policies for 'user-uploads' bucket ---

-- Anyone can download/view files in user-uploads.
DROP POLICY IF EXISTS "user-uploads: public read" ON storage.objects;
CREATE POLICY "user-uploads: public read"
    ON storage.objects FOR SELECT TO public
    USING (bucket_id = 'user-uploads');

-- Authenticated users can upload only to their own UID-prefixed folder.
DROP POLICY IF EXISTS "user-uploads: authenticated insert" ON storage.objects;
CREATE POLICY "user-uploads: authenticated insert"
    ON storage.objects FOR INSERT TO authenticated
    WITH CHECK (
        bucket_id = 'user-uploads'
        AND (storage.foldername(name))[1] = (auth.uid())::TEXT
    );

-- Authenticated users can modify files in their own folder.
DROP POLICY IF EXISTS "user-uploads: owner update" ON storage.objects;
CREATE POLICY "user-uploads: owner update"
    ON storage.objects FOR UPDATE TO authenticated
    USING (bucket_id = 'user-uploads' AND owner_id = (auth.uid())::TEXT);

-- Authenticated users can delete files in their own folder.
DROP POLICY IF EXISTS "user-uploads: owner delete" ON storage.objects;
CREATE POLICY "user-uploads: owner delete"
    ON storage.objects FOR DELETE TO authenticated
    USING (bucket_id = 'user-uploads' AND owner_id = (auth.uid())::TEXT);


-- --- Policies for 'attachments' bucket ---

-- Anyone can select/read from attachments.
DROP POLICY IF EXISTS "Allow public select from attachments" ON storage.objects;
CREATE POLICY "Allow public select from attachments"
    ON storage.objects FOR SELECT TO public
    USING (bucket_id = 'attachments');

-- Anyone can upload to attachments (public uploads).
DROP POLICY IF EXISTS "Allow public upload to attachments" ON storage.objects;
CREATE POLICY "Allow public upload to attachments"
    ON storage.objects FOR INSERT TO public
    WITH CHECK (bucket_id = 'attachments');
