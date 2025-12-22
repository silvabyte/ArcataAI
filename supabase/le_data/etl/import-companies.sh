#!/bin/bash
# Import companies.csv into Supabase
#
# Usage:
#   ./etl/import-companies.sh <DATABASE_URL>
#
# Example:
#   ./etl/import-companies.sh "postgresql://postgres:password@db.xxx.supabase.co:5432/postgres"

set -e

DATABASE_URL="$1"
CSV_FILE="companies.csv"

if [ -z "$DATABASE_URL" ]; then
  echo "Usage: $0 <DATABASE_URL>"
  echo ""
  echo "Example:"
  echo "  $0 \"postgresql://postgres:password@db.xxx.supabase.co:5432/postgres\""
  exit 1
fi

if [ ! -f "$CSV_FILE" ]; then
  echo "Error: $CSV_FILE not found"
  echo "Run the ETL script first: bun run etl/extract-companies.ts"
  exit 1
fi

ROW_COUNT=$(wc -l < "$CSV_FILE")
ROW_COUNT=$((ROW_COUNT - 1))  # Subtract header row

echo "=========================================="
echo "Importing companies to Supabase"
echo "=========================================="
echo "CSV File:  $CSV_FILE"
echo "Rows:      $ROW_COUNT"
echo ""

# Columns to import (must match CSV header order)
COLUMNS="source_company_id,cc_id,company_name,company_domain,company_linkedin_url,company_jobs_url,company_phone,company_address,company_address_2,company_city,company_state,company_zip,employee_count_min,employee_count_max,revenue_min,revenue_max,company_naics,company_sic,primary_industry,job_board_status"

echo "Starting import..."
START_TIME=$(date +%s)

psql "$DATABASE_URL" -c "\copy companies($COLUMNS) FROM '$CSV_FILE' WITH (FORMAT csv, HEADER true, NULL '')"

END_TIME=$(date +%s)
ELAPSED=$((END_TIME - START_TIME))

echo ""
echo "=========================================="
echo "Import complete!"
echo "Time: ${ELAPSED}s"
echo "=========================================="

# Verify count
echo ""
echo "Verifying row count..."
psql "$DATABASE_URL" -c "SELECT COUNT(*) as imported_rows FROM companies;"
