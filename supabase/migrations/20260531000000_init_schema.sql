-- 1. Create metadata tables
CREATE TABLE IF NOT EXISTS custom_tables (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    parent_table_id TEXT REFERENCES custom_tables(id) ON DELETE CASCADE,
    created_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS custom_fields (
    id TEXT PRIMARY KEY,
    table_id TEXT NOT NULL REFERENCES custom_tables(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    type TEXT NOT NULL,
    required BOOLEAN NOT NULL,
    default_value TEXT,
    default_type TEXT,
    is_system BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS custom_records (
    id TEXT PRIMARY KEY,
    table_id TEXT NOT NULL REFERENCES custom_tables(id) ON DELETE CASCADE,
    created_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS custom_values (
    id TEXT PRIMARY KEY,
    record_id TEXT NOT NULL REFERENCES custom_records(id) ON DELETE CASCADE,
    field_id TEXT NOT NULL REFERENCES custom_fields(id) ON DELETE CASCADE,
    value_text TEXT,
    UNIQUE(record_id, field_id)
);

-- Disable RLS on data tables to allow client-side PostgREST synchronization
ALTER TABLE custom_tables DISABLE ROW LEVEL SECURITY;
ALTER TABLE custom_fields DISABLE ROW LEVEL SECURITY;
ALTER TABLE custom_records DISABLE ROW LEVEL SECURITY;
ALTER TABLE custom_values DISABLE ROW LEVEL SECURITY;

-- 2. Setup Storage Bucket for file attachments
INSERT INTO storage.buckets (id, name, public)
VALUES ('attachments', 'attachments', true)
ON CONFLICT (id) DO NOTHING;

-- Enable RLS on storage.objects to define explicit policies
ALTER TABLE storage.objects ENABLE ROW LEVEL SECURITY;

-- Allow public upload access to the attachments bucket
DROP POLICY IF EXISTS "Allow public upload to attachments" ON storage.objects;
CREATE POLICY "Allow public upload to attachments"
ON storage.objects FOR INSERT
TO public
WITH CHECK (bucket_id = 'attachments');

-- Allow public read access to the attachments bucket
DROP POLICY IF EXISTS "Allow public select from attachments" ON storage.objects;
CREATE POLICY "Allow public select from attachments"
ON storage.objects FOR SELECT
TO public
USING (bucket_id = 'attachments');
