-- Migration: Remove 'source' column from candidates
ALTER TABLE candidates DROP COLUMN IF EXISTS source; 