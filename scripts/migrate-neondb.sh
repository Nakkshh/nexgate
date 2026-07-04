#!/bin/sh
# Applies the schema migration to NeonDB
# Usage: ./scripts/migrate-neondb.sh
#
# Before running:
#   1. Create account at neon.tech
#   2. Create a project called "nexgate"
#   3. Copy the connection string from Dashboard → Connection Details
#   4. Export it: export NEONDB_URL="postgresql://user:pass@host/nexgate?sslmode=require"

set -e

if [ -z "$NEONDB_URL" ]; then
  echo "Error: NEONDB_URL is not set."
  echo "Export your NeonDB connection string first:"
  echo '  export NEONDB_URL="postgresql://user:pass@host/nexgate?sslmode=require"'
  exit 1
fi

echo "Applying migration to NeonDB..."
psql "$NEONDB_URL" -f "$(dirname "$0")/../db/migrations/001_init.sql"
echo "Migration complete."

echo "\nVerifying tables..."
psql "$NEONDB_URL" -c "\dt"
