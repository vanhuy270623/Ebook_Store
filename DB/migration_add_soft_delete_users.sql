-- Migration script to add soft deletion support to users table
-- Add deleted_at column for soft deletion functionality
-- Date: December 4, 2025

USE ebook_store;

-- Add deleted_at column to users table
ALTER TABLE users
ADD COLUMN deleted_at DATETIME DEFAULT NULL;

-- Add index on deleted_at for better query performance
CREATE INDEX idx_users_deleted_at ON users(deleted_at);

-- Add composite index for active user queries
CREATE INDEX idx_users_active ON users(deleted_at, is_active);

-- Optional: Update any existing test data if needed
-- UPDATE users SET deleted_at = NULL WHERE deleted_at IS NULL;

-- Verify the changes
DESCRIBE users;
