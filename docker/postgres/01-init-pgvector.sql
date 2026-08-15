-- Enable vector extension for embeddings similarity search
CREATE EXTENSION IF NOT EXISTS vector;

-- Enable UUID generator extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Schema creation
CREATE SCHEMA IF NOT EXISTS logistix;

-- Verification notice
DO $$
BEGIN
    RAISE NOTICE 'LogistiX database initialized with vector and uuid-ossp extensions.';
END $$;
