#!/usr/bin/env bun
/**
 * Environment Switching CLI
 *
 * Switch Supabase database and API configurations across apps.
 *
 * Usage:
 *   bun run env:local              # Switch all apps to local
 *   bun run env:prod               # Switch all apps to production
 *   bun run env:status             # Show current environment status
 *
 *   # Granular switching
 *   bun run env -- --app hq --db local
 *   bun run env -- --app api --db prod
 *   bun run env -- --db local                   # All apps
 *   bun run env -- --api prod                   # Frontend apps only
 */

import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT_DIR = join(__dirname, "..");
const ENVS_DIR = join(ROOT_DIR, "envs");

const APPS = ["hq", "auth", "api"] as const;
const FRONTEND_APPS = ["hq", "auth"] as const;
const ENVIRONMENTS = ["local", "production"] as const;

type App = (typeof APPS)[number];
type Environment = (typeof ENVIRONMENTS)[number];

type EnvConfig = {
  supabase: {
    url: string;
    anonKey: string;
    serviceRoleKey: string;
  };
  api: {
    url: string;
    corsOrigins: string;
  };
  apps: {
    hqUrl: string;
    authUrl: string;
  };
  objectStorage: {
    url: string;
    tenantId: string;
    apiKey: string;
  };
  ai: {
    gatewayUrl: string;
    apiKey: string;
    model: string;
  };
  resume: {
    maxFileSizeMb: number;
  };
};

type ParsedArgs = {
  env?: Environment;
  app?: App | "all";
  db?: Environment;
  api?: Environment;
  status?: boolean;
  help?: boolean;
};

// biome-ignore lint/complexity/noExcessiveCognitiveComplexity: CLI script with straightforward control flow
function parseArgs(args: string[]): ParsedArgs {
  const result: ParsedArgs = {};

  for (let i = 0; i < args.length; i += 1) {
    const arg = args[i];

    if (arg === "--help" || arg === "-h") {
      result.help = true;
    } else if (arg === "--status") {
      result.status = true;
    } else if (arg === "--app" && args[i + 1]) {
      i += 1;
      const value = args[i];
      if (value === "all" || APPS.includes(value as App)) {
        result.app = value as App | "all";
      } else {
        console.error(
          `Invalid app: ${value}. Valid options: ${APPS.join(", ")}, all`
        );
        process.exit(1);
      }
    } else if (arg === "--db" && args[i + 1]) {
      i += 1;
      const value = args[i];
      if (ENVIRONMENTS.includes(value as Environment)) {
        result.db = value as Environment;
      } else {
        console.error(
          `Invalid environment: ${value}. Valid options: ${ENVIRONMENTS.join(", ")}`
        );
        process.exit(1);
      }
    } else if (arg === "--api" && args[i + 1]) {
      i += 1;
      const value = args[i];
      if (ENVIRONMENTS.includes(value as Environment)) {
        result.api = value as Environment;
      } else {
        console.error(
          `Invalid environment: ${value}. Valid options: ${ENVIRONMENTS.join(", ")}`
        );
        process.exit(1);
      }
    } else if (!arg.startsWith("-")) {
      // Positional argument - treat as full environment switch
      if (ENVIRONMENTS.includes(arg as Environment)) {
        result.env = arg as Environment;
      } else {
        console.error(
          `Invalid environment: ${arg}. Valid options: ${ENVIRONMENTS.join(", ")}`
        );
        process.exit(1);
      }
    }
  }

  return result;
}

function printHelp(): void {
  console.log(`
Environment Switching CLI

Usage:
  bun run env:local              Switch all apps to local environment
  bun run env:prod               Switch all apps to production environment
  bun run env:status             Show current environment status

  bun run env -- [options]       Granular switching

Options:
  --app <name>    Target specific app (hq, auth, api, all). Default: all
  --db <env>      Switch only database variables (local, production)
  --api <env>     Switch only API URL (local, production) - frontend apps only
  --status        Show current environment status
  --help, -h      Show this help message

Examples:
  bun run env -- --app hq --db local     Switch HQ database to local
  bun run env -- --app api --db prod     Switch API database to production
  bun run env -- --db local              Switch all apps database to local
  bun run env -- --api prod              Switch frontend apps API URL to production
`);
}

function loadEnvConfig(env: Environment): EnvConfig {
  const configPath = join(ENVS_DIR, `${env}.json`);

  if (!existsSync(configPath)) {
    console.error(`Config file not found: ${configPath}`);
    console.error(
      `Copy ${env}.example.json to ${env}.json and fill in your values.`
    );
    process.exit(1);
  }

  const content = readFileSync(configPath, "utf-8");
  return JSON.parse(content) as EnvConfig;
}

function parseEnvFile(content: string): Map<string, string> {
  const result = new Map<string, string>();

  for (const line of content.split("\n")) {
    const trimmed = line.trim();

    // Skip empty lines and comments
    if (!trimmed || trimmed.startsWith("#")) {
      continue;
    }

    const eqIndex = trimmed.indexOf("=");
    if (eqIndex > 0) {
      const key = trimmed.substring(0, eqIndex);
      const value = trimmed.substring(eqIndex + 1);
      result.set(key, value);
    }
  }

  return result;
}

function serializeEnvFile(envMap: Map<string, string>): string {
  const lines: string[] = [];

  for (const [key, value] of envMap) {
    lines.push(`${key}=${value}`);
  }

  return `${lines.join("\n")}\n`;
}

function readAppEnv(app: App): Map<string, string> {
  const envPath = join(ROOT_DIR, "apps", app, ".env");

  if (!existsSync(envPath)) {
    return new Map();
  }

  const content = readFileSync(envPath, "utf-8");
  return parseEnvFile(content);
}

function writeAppEnv(app: App, envMap: Map<string, string>): void {
  const envPath = join(ROOT_DIR, "apps", app, ".env");
  const content = serializeEnvFile(envMap);
  writeFileSync(envPath, content, "utf-8");
}

type Change = {
  app: App;
  key: string;
  oldValue: string;
  newValue: string;
};

// biome-ignore lint/complexity/noExcessiveCognitiveComplexity: CLI script with straightforward control flow
async function switchDatabase(
  apps: App[],
  env: Environment
): Promise<Change[]> {
  const config = await loadEnvConfig(env);
  const changes: Change[] = [];

  for (const app of apps) {
    const envMap = await readAppEnv(app);

    if (app === "api") {
      // API app uses different variable names
      const oldUrl = envMap.get("SUPABASE_URL") ?? "";
      const oldKey = envMap.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
      const oldCors = envMap.get("CORS_ORIGINS") ?? "";

      if (oldUrl !== config.supabase.url) {
        changes.push({
          app,
          key: "SUPABASE_URL",
          oldValue: oldUrl,
          newValue: config.supabase.url,
        });
        envMap.set("SUPABASE_URL", config.supabase.url);
      }

      if (oldKey !== config.supabase.serviceRoleKey) {
        changes.push({
          app,
          key: "SUPABASE_SERVICE_ROLE_KEY",
          oldValue: maskSecret(oldKey),
          newValue: maskSecret(config.supabase.serviceRoleKey),
        });
        envMap.set("SUPABASE_SERVICE_ROLE_KEY", config.supabase.serviceRoleKey);
      }

      if (oldCors !== config.api.corsOrigins) {
        changes.push({
          app,
          key: "CORS_ORIGINS",
          oldValue: oldCors,
          newValue: config.api.corsOrigins,
        });
        envMap.set("CORS_ORIGINS", config.api.corsOrigins);
      }

      // Resume config
      const oldMaxFileSize = envMap.get("RESUME_MAX_FILE_SIZE_MB") ?? "";
      const newMaxFileSize = String(config.resume.maxFileSizeMb);
      if (oldMaxFileSize !== newMaxFileSize) {
        changes.push({
          app,
          key: "RESUME_MAX_FILE_SIZE_MB",
          oldValue: oldMaxFileSize,
          newValue: newMaxFileSize,
        });
        envMap.set("RESUME_MAX_FILE_SIZE_MB", newMaxFileSize);
      }

      // Object Storage config
      const oldOsUrl = envMap.get("OBJECT_STORAGE_URL") ?? "";
      if (oldOsUrl !== config.objectStorage.url) {
        changes.push({
          app,
          key: "OBJECT_STORAGE_URL",
          oldValue: oldOsUrl,
          newValue: config.objectStorage.url,
        });
        envMap.set("OBJECT_STORAGE_URL", config.objectStorage.url);
      }

      const oldOsTenantId = envMap.get("OBJECT_STORAGE_TENANT_ID") ?? "";
      if (oldOsTenantId !== config.objectStorage.tenantId) {
        changes.push({
          app,
          key: "OBJECT_STORAGE_TENANT_ID",
          oldValue: oldOsTenantId,
          newValue: config.objectStorage.tenantId,
        });
        envMap.set("OBJECT_STORAGE_TENANT_ID", config.objectStorage.tenantId);
      }

      const oldOsApiKey = envMap.get("OBJECT_STORAGE_API_KEY") ?? "";
      if (oldOsApiKey !== config.objectStorage.apiKey) {
        changes.push({
          app,
          key: "OBJECT_STORAGE_API_KEY",
          oldValue: maskSecret(oldOsApiKey),
          newValue: maskSecret(config.objectStorage.apiKey),
        });
        envMap.set("OBJECT_STORAGE_API_KEY", config.objectStorage.apiKey);
      }

      // AI config
      const oldAiGatewayUrl = envMap.get("VERCEL_AI_GATEWAY_URL") ?? "";
      if (oldAiGatewayUrl !== config.ai.gatewayUrl) {
        changes.push({
          app,
          key: "VERCEL_AI_GATEWAY_URL",
          oldValue: oldAiGatewayUrl,
          newValue: config.ai.gatewayUrl,
        });
        envMap.set("VERCEL_AI_GATEWAY_URL", config.ai.gatewayUrl);
      }

      const oldAiApiKey = envMap.get("VERCEL_AI_GATEWAY_API_KEY") ?? "";
      if (oldAiApiKey !== config.ai.apiKey) {
        changes.push({
          app,
          key: "VERCEL_AI_GATEWAY_API_KEY",
          oldValue: maskSecret(oldAiApiKey),
          newValue: maskSecret(config.ai.apiKey),
        });
        envMap.set("VERCEL_AI_GATEWAY_API_KEY", config.ai.apiKey);
      }

      const oldAiModel = envMap.get("AI_MODEL") ?? "";
      if (oldAiModel !== config.ai.model) {
        changes.push({
          app,
          key: "AI_MODEL",
          oldValue: oldAiModel,
          newValue: config.ai.model,
        });
        envMap.set("AI_MODEL", config.ai.model);
      }
    } else {
      // Frontend apps (hq, auth)
      const oldUrl = envMap.get("VITE_SUPABASE_URL") ?? "";
      const oldKey = envMap.get("VITE_SUPABASE_KEY") ?? "";
      const oldHqUrl = envMap.get("VITE_HQ_BASE_URL") ?? "";
      const oldAuthUrl = envMap.get("VITE_AUTH_BASE_URL") ?? "";

      if (oldUrl !== config.supabase.url) {
        changes.push({
          app,
          key: "VITE_SUPABASE_URL",
          oldValue: oldUrl,
          newValue: config.supabase.url,
        });
        envMap.set("VITE_SUPABASE_URL", config.supabase.url);
      }

      if (oldKey !== config.supabase.anonKey) {
        changes.push({
          app,
          key: "VITE_SUPABASE_KEY",
          oldValue: maskSecret(oldKey),
          newValue: maskSecret(config.supabase.anonKey),
        });
        envMap.set("VITE_SUPABASE_KEY", config.supabase.anonKey);
      }

      if (oldHqUrl !== config.apps.hqUrl) {
        changes.push({
          app,
          key: "VITE_HQ_BASE_URL",
          oldValue: oldHqUrl,
          newValue: config.apps.hqUrl,
        });
        envMap.set("VITE_HQ_BASE_URL", config.apps.hqUrl);
      }

      if (oldAuthUrl !== config.apps.authUrl) {
        changes.push({
          app,
          key: "VITE_AUTH_BASE_URL",
          oldValue: oldAuthUrl,
          newValue: config.apps.authUrl,
        });
        envMap.set("VITE_AUTH_BASE_URL", config.apps.authUrl);
      }
    }

    await writeAppEnv(app, envMap);
  }

  return changes;
}

async function switchApiUrl(apps: App[], env: Environment): Promise<Change[]> {
  const config = await loadEnvConfig(env);
  const changes: Change[] = [];

  // Only frontend apps have API URL
  const frontendApps = apps.filter((app): app is "hq" | "auth" =>
    FRONTEND_APPS.includes(app as "hq" | "auth")
  );

  for (const app of frontendApps) {
    const envMap = await readAppEnv(app);
    const oldUrl = envMap.get("VITE_API_URL") ?? "";

    if (oldUrl !== config.api.url) {
      changes.push({
        app,
        key: "VITE_API_URL",
        oldValue: oldUrl,
        newValue: config.api.url,
      });
      envMap.set("VITE_API_URL", config.api.url);
      await writeAppEnv(app, envMap);
    }
  }

  return changes;
}

function maskSecret(value: string): string {
  if (value.length <= 8) {
    return "***";
  }
  return `${value.substring(0, 4)}...${value.substring(value.length - 4)}`;
}

function detectEnvironment(url: string): string {
  if (url.includes("127.0.0.1") || url.includes("localhost")) {
    return "local";
  }
  if (url.includes("supabase.co") || url.includes("arcata.co")) {
    return "production";
  }
  return "unknown";
}

async function showStatus(): Promise<void> {
  console.log("\nCurrent Environment Status\n");

  for (const app of APPS) {
    const envMap = await readAppEnv(app);
    console.log(`${app.toUpperCase()}:`);

    if (app === "api") {
      const supabaseUrl = envMap.get("SUPABASE_URL") ?? "(not set)";
      const serviceKey = envMap.get("SUPABASE_SERVICE_ROLE_KEY") ?? "(not set)";
      const corsOrigins = envMap.get("CORS_ORIGINS") ?? "(not set)";
      const resumeMaxSize =
        envMap.get("RESUME_MAX_FILE_SIZE_MB") ?? "(not set)";
      const osUrl = envMap.get("OBJECT_STORAGE_URL") ?? "(not set)";
      const osTenantId = envMap.get("OBJECT_STORAGE_TENANT_ID") ?? "(not set)";
      const osApiKey = envMap.get("OBJECT_STORAGE_API_KEY") ?? "(not set)";
      const aiGatewayUrl = envMap.get("VERCEL_AI_GATEWAY_URL") ?? "(not set)";
      const aiApiKey = envMap.get("VERCEL_AI_GATEWAY_API_KEY") ?? "(not set)";
      const aiModel = envMap.get("AI_MODEL") ?? "(not set)";

      console.log(
        `  Database:     ${detectEnvironment(supabaseUrl)} (${supabaseUrl})`
      );
      console.log(`  Service Key:  ${maskSecret(serviceKey)}`);
      console.log(`  CORS Origins: ${corsOrigins}`);
      console.log(`  Resume Max Size: ${resumeMaxSize}MB`);
      console.log(`  Object Storage URL: ${osUrl}`);
      console.log(`  Object Storage Tenant: ${osTenantId}`);
      console.log(`  Object Storage Key: ${maskSecret(osApiKey)}`);
      console.log(`  AI Gateway URL: ${aiGatewayUrl}`);
      console.log(`  AI Gateway Key: ${maskSecret(aiApiKey)}`);
      console.log(`  AI Model: ${aiModel}`);
    } else {
      const supabaseUrl = envMap.get("VITE_SUPABASE_URL") ?? "(not set)";
      const anonKey = envMap.get("VITE_SUPABASE_KEY") ?? "(not set)";
      const apiUrl = envMap.get("VITE_API_URL") ?? "(not set)";
      const hqUrl = envMap.get("VITE_HQ_BASE_URL") ?? "(not set)";
      const authUrl = envMap.get("VITE_AUTH_BASE_URL") ?? "(not set)";

      console.log(
        `  Database: ${detectEnvironment(supabaseUrl)} (${supabaseUrl})`
      );
      console.log(`  Anon Key: ${maskSecret(anonKey)}`);
      console.log(`  API URL:  ${detectEnvironment(apiUrl)} (${apiUrl})`);
      console.log(`  HQ URL:   ${detectEnvironment(hqUrl)} (${hqUrl})`);
      console.log(`  Auth URL: ${detectEnvironment(authUrl)} (${authUrl})`);
    }
    console.log();
  }
}

function printChanges(changes: Change[]): void {
  if (changes.length === 0) {
    console.log("\nNo changes made (already configured).");
    return;
  }

  console.log("\nChanges applied:\n");

  for (const change of changes) {
    console.log(`  ${change.app.toUpperCase()}: ${change.key}`);
    console.log(`    ${change.oldValue || "(empty)"} -> ${change.newValue}`);
  }

  console.log();
}

async function main(): Promise<void> {
  const args = parseArgs(process.argv.slice(2));

  if (args.help) {
    printHelp();
    return;
  }

  if (args.status) {
    await showStatus();
    return;
  }

  const targetApps: App[] =
    args.app && args.app !== "all" ? [args.app] : [...APPS];

  let allChanges: Change[] = [];

  // Full environment switch (positional arg)
  if (args.env) {
    console.log(`Switching all apps to ${args.env}...`);
    const dbChanges = await switchDatabase(targetApps, args.env);
    const apiChanges = await switchApiUrl(targetApps, args.env);
    allChanges = [...dbChanges, ...apiChanges];
    printChanges(allChanges);
    return;
  }

  // Granular switching
  if (args.db) {
    console.log(
      `Switching database to ${args.db} for: ${targetApps.join(", ")}...`
    );
    const dbChanges = await switchDatabase(targetApps, args.db);
    allChanges = [...allChanges, ...dbChanges];
  }

  if (args.api) {
    const frontendApps = targetApps.filter((app): app is "hq" | "auth" =>
      FRONTEND_APPS.includes(app as "hq" | "auth")
    );
    if (frontendApps.length > 0) {
      console.log(
        `Switching API URL to ${args.api} for: ${frontendApps.join(", ")}...`
      );
      const apiChanges = await switchApiUrl(frontendApps, args.api);
      allChanges = [...allChanges, ...apiChanges];
    } else {
      console.log("Note: --api only affects frontend apps (hq, auth)");
    }
  }

  if (allChanges.length > 0) {
    printChanges(allChanges);
  } else {
    const hasGranularArgs = Boolean(args.db) || Boolean(args.api);
    if (!hasGranularArgs) {
      printHelp();
    }
  }
}

main().catch((err: Error) => {
  console.error("Error:", err.message);
  process.exit(1);
});
