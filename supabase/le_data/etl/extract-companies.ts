#!/usr/bin/env bun
/**
 * ETL Script: Extract companies from pg_dump backup and transform for Supabase import
 *
 * Usage:
 *   bun run etl/extract-companies.ts [--limit N] [--input path/to/backup.gz]
 *
 * Output:
 *   companies.csv - Ready for psql \copy import
 */

import { createReadStream, createWriteStream } from "node:fs";
import { createInterface } from "node:readline";
import { parseArgs } from "node:util";
import { createGunzip } from "node:zlib";

// ============================================================================
// Configuration
// ============================================================================

const DEFAULT_INPUT = "db_cluster-15-11-2024@15-29-50.backup.gz";
const OUTPUT_FILE = "companies.csv";

// Revenue string -> { min, max } mapping
const REVENUE_MAP: Record<string, { min: number; max: number | null }> = {
  "Under 1 Million": { min: 0, max: 999_999 },
  "1 Million to 5 Million": { min: 1_000_000, max: 5_000_000 },
  "5 Million to 10 Million": { min: 5_000_000, max: 10_000_000 },
  "10 Million to 25 Million": { min: 10_000_000, max: 25_000_000 },
  "25 Million to 50 Miliion": { min: 25_000_000, max: 50_000_000 }, // typo in source data
  "25 Million to 50 Million": { min: 25_000_000, max: 50_000_000 }, // correct spelling fallback
  "50 Million to 100 Million": { min: 50_000_000, max: 100_000_000 },
  "100 Million to 250 Million": { min: 100_000_000, max: 250_000_000 },
  "250 Million to 500 Million": { min: 250_000_000, max: 500_000_000 },
  "500 Million to 1 Billion": { min: 500_000_000, max: 1_000_000_000 },
  "1 Billion and Over": { min: 1_000_000_000, max: null },
};

// Employee count string -> { min, max } mapping
const EMPLOYEE_MAP: Record<string, { min: number; max: number | null }> = {
  "1 to 10": { min: 1, max: 10 },
  "11 to 25": { min: 11, max: 25 },
  "26 to 50": { min: 26, max: 50 },
  "51 to 100": { min: 51, max: 100 },
  "101 to 250": { min: 101, max: 250 },
  "251 to 500": { min: 251, max: 500 },
  "501 to 1000": { min: 501, max: 1000 },
  "1001 to 5000": { min: 1001, max: 5000 },
  "5001 to 10000": { min: 5001, max: 10_000 },
  "10000+": { min: 10_000, max: null },
};

// Source backup column indices (0-based, from COPY statement)
const SRC_COLS = {
  company_id: 0,
  company_name: 1,
  company_domain: 2,
  company_jobs_url: 3,
  company_phone: 4,
  company_sic: 5,
  company_naics: 6,
  company_address: 7,
  company_address_2: 8,
  company_city: 9,
  company_state: 10,
  company_zip: 11,
  company_linkedin_url: 12,
  company_revenue: 13,
  company_employee_count: 14,
  primary_industry: 15,
  cc_id: 16,
  job_board_status: 17,
} as const;

// Target CSV columns (matching Supabase companies table, excluding company_id which is SERIAL)
const TARGET_COLS = [
  "source_company_id",
  "cc_id",
  "company_name",
  "company_domain",
  "company_linkedin_url",
  "company_jobs_url",
  "company_phone",
  "company_address",
  "company_address_2",
  "company_city",
  "company_state",
  "company_zip",
  "employee_count_min",
  "employee_count_max",
  "revenue_min",
  "revenue_max",
  "company_naics",
  "company_sic",
  "primary_industry",
  "job_board_status",
];

// ============================================================================
// Transform Functions
// ============================================================================

function parseNull(value: string): string | null {
  if (value === "\\N" || value === "\\\\N" || value === "") {
    return null;
  }
  return value;
}

function parseRevenue(value: string): {
  min: number | null;
  max: number | null;
} {
  const cleaned = parseNull(value);
  if (cleaned === null) {
    return { min: null, max: null };
  }
  const mapped = REVENUE_MAP[cleaned];
  if (!mapped) {
    console.error(`Unknown revenue value: "${cleaned}"`);
    return { min: null, max: null };
  }
  return mapped;
}

function parseEmployeeCount(value: string): {
  min: number | null;
  max: number | null;
} {
  const cleaned = parseNull(value);
  if (cleaned === null) {
    return { min: null, max: null };
  }
  const mapped = EMPLOYEE_MAP[cleaned];
  if (!mapped) {
    console.error(`Unknown employee count value: "${cleaned}"`);
    return { min: null, max: null };
  }
  return mapped;
}

function escapeCSV(value: string | number | null): string {
  if (value === null) {
    return "";
  }
  const str = String(value);
  if (
    str.includes(",") ||
    str.includes('"') ||
    str.includes("\n") ||
    str.includes("\r")
  ) {
    return `"${str.replace(/"/g, '""')}"`;
  }
  return str;
}

function transformRow(fields: string[]): string[] {
  const revenue = parseRevenue(fields[SRC_COLS.company_revenue]);
  const employees = parseEmployeeCount(fields[SRC_COLS.company_employee_count]);

  return [
    escapeCSV(parseNull(fields[SRC_COLS.company_id])),
    escapeCSV(parseNull(fields[SRC_COLS.cc_id])),
    escapeCSV(parseNull(fields[SRC_COLS.company_name])),
    escapeCSV(parseNull(fields[SRC_COLS.company_domain])),
    escapeCSV(parseNull(fields[SRC_COLS.company_linkedin_url])),
    escapeCSV(parseNull(fields[SRC_COLS.company_jobs_url])),
    escapeCSV(parseNull(fields[SRC_COLS.company_phone])),
    escapeCSV(parseNull(fields[SRC_COLS.company_address])),
    escapeCSV(parseNull(fields[SRC_COLS.company_address_2])),
    escapeCSV(parseNull(fields[SRC_COLS.company_city])),
    escapeCSV(parseNull(fields[SRC_COLS.company_state])),
    escapeCSV(parseNull(fields[SRC_COLS.company_zip])),
    escapeCSV(employees.min),
    escapeCSV(employees.max),
    escapeCSV(revenue.min),
    escapeCSV(revenue.max),
    escapeCSV(parseNull(fields[SRC_COLS.company_naics])),
    escapeCSV(parseNull(fields[SRC_COLS.company_sic])),
    escapeCSV(parseNull(fields[SRC_COLS.primary_industry])),
    escapeCSV(parseNull(fields[SRC_COLS.job_board_status])),
  ];
}

// ============================================================================
// Extraction Logic (separated to reduce main() complexity)
// ============================================================================

type ExtractResult = {
  rowCount: number;
  skippedCount: number;
};

type RowProcessResult = {
  success: boolean;
  transformed?: string[];
};

function processDataRow(line: string, skippedCount: number): RowProcessResult {
  const fields = line.split("\t");

  if (fields.length !== 18) {
    if (skippedCount < 5) {
      console.error(
        `Skipping malformed row (${fields.length} fields): ${line.slice(0, 100)}...`
      );
    }
    return { success: false };
  }

  return { success: true, transformed: transformRow(fields) };
}

function createLineReader(inputFile: string) {
  const gunzip = createGunzip();
  const fileStream = createReadStream(inputFile);
  fileStream.pipe(gunzip);

  return createInterface({
    input: gunzip,
    crlfDelay: Number.POSITIVE_INFINITY,
  });
}

async function extractAndTransform(
  inputFile: string,
  output: ReturnType<typeof createWriteStream>,
  limit: number | undefined,
  onProgress: (count: number, elapsedSecs: number) => void
): Promise<ExtractResult> {
  const startTime = Date.now();
  let inCopyBlock = false;
  let rowCount = 0;
  let skippedCount = 0;

  const rl = createLineReader(inputFile);

  for await (const line of rl) {
    if (line.startsWith("COPY public.companies ")) {
      inCopyBlock = true;
      console.log("Found COPY public.companies block, extracting...");
      continue;
    }

    if (inCopyBlock && line === "\\.") {
      console.log("End of COPY block");
      break;
    }

    if (!inCopyBlock) {
      continue;
    }

    const result = processDataRow(line, skippedCount);
    if (!result.success) {
      skippedCount += 1;
      continue;
    }

    output.write(`${result.transformed?.join(",")}\n`);
    rowCount += 1;

    if (rowCount % 500_000 === 0) {
      onProgress(rowCount, (Date.now() - startTime) / 1000);
    }

    if (limit && rowCount >= limit) {
      console.log(`Reached limit of ${limit} rows`);
      break;
    }
  }

  return { rowCount, skippedCount };
}

// ============================================================================
// Main
// ============================================================================

async function main() {
  const { values } = parseArgs({
    options: {
      limit: { type: "string", short: "l" },
      input: { type: "string", short: "i" },
    },
  });

  const inputFile = values.input ?? DEFAULT_INPUT;
  const limit = values.limit ? Number.parseInt(values.limit, 10) : undefined;

  console.log(`Input:  ${inputFile}`);
  console.log(`Output: ${OUTPUT_FILE}`);
  if (limit) {
    console.log(`Limit:  ${limit} rows`);
  }
  console.log("");

  const startTime = Date.now();
  const output = createWriteStream(OUTPUT_FILE);
  output.write(`${TARGET_COLS.join(",")}\n`);

  const { rowCount, skippedCount } = await extractAndTransform(
    inputFile,
    output,
    limit,
    (count, elapsedSecs) => {
      console.log(
        `  Processed ${count.toLocaleString()} rows (${elapsedSecs.toFixed(1)}s)`
      );
    }
  );

  output.end();

  const totalElapsed = ((Date.now() - startTime) / 1000).toFixed(1);
  console.log("");
  console.log("=".repeat(50));
  console.log(`Extracted: ${rowCount.toLocaleString()} rows`);
  console.log(`Skipped:   ${skippedCount.toLocaleString()} malformed rows`);
  console.log(`Time:      ${totalElapsed}s`);
  console.log(`Output:    ${OUTPUT_FILE}`);
  console.log("=".repeat(50));
  console.log("");
  console.log("Next step: Run the import script");
  console.log("  ./etl/import-companies.sh <DATABASE_URL>");
}

main().catch((err) => {
  console.error("ETL failed:", err);
  process.exit(1);
});
