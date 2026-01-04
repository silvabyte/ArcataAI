#!/usr/bin/env bun
/**
 * Docker Build/Push/Run CLI
 *
 * Build, push, and run Docker images for apps.
 *
 * Usage:
 *   bun run scripts/docker.ts build --app=api
 *   bun run scripts/docker.ts push --app=api
 *   bun run scripts/docker.ts build-push --app=api
 *   bun run scripts/docker.ts run --app=api
 *   bun run scripts/docker.ts stop --app=api
 *
 * Or via npm scripts:
 *   bun run docker:build:api
 *   bun run docker:push:api
 *   bun run docker:build-push:api
 *   bun run docker:run:api
 *   bun run docker:stop:api
 */

import { existsSync, readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { $ } from "bun";

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT_DIR = join(__dirname, "..");

// App configurations
const APPS: Record<string, { port: number; imageName: string }> = {
  api: { port: 4203, imageName: "arcata-api" },
  // Future apps can be added here
};

const COMMANDS = ["build", "push", "build-push", "run", "stop"] as const;
type Command = (typeof COMMANDS)[number];

type DockerEnv = {
  CR_NAMESPACE: string;
  CR_USER: string;
  CR_PAT: string;
};

type ParsedArgs = {
  command?: Command;
  app?: string;
  help?: boolean;
};

function parseArgs(args: string[]): ParsedArgs {
  const result: ParsedArgs = {};

  for (let i = 0; i < args.length; i += 1) {
    const arg = args[i];

    if (arg === "--help" || arg === "-h") {
      result.help = true;
    } else if (arg.startsWith("--app=")) {
      result.app = arg.substring("--app=".length);
    } else if (arg === "--app" && args[i + 1] && !args[i + 1].startsWith("-")) {
      i += 1;
      result.app = args[i];
    } else if (!arg.startsWith("-") && COMMANDS.includes(arg as Command)) {
      result.command = arg as Command;
    }
  }

  return result;
}

function printHelp(): void {
  console.log(`
Docker Build/Push/Run CLI

Usage:
  bun run scripts/docker.ts <command> --app=<app>

Commands:
  build       Build Docker image locally (tagged as :local)
  push        Push image to ghcr.io (requires prior build-push or manual tag)
  build-push  Build and push image to ghcr.io
  run         Run local image in a container
  stop        Stop and remove local container

Apps: ${Object.keys(APPS).join(", ")}

Examples:
  bun run scripts/docker.ts build --app=api
  bun run scripts/docker.ts build-push --app=api
  bun run scripts/docker.ts run --app=api
  bun run scripts/docker.ts stop --app=api

NPM Scripts:
  bun run docker:build:api
  bun run docker:push:api
  bun run docker:build-push:api
  bun run docker:run:api
  bun run docker:stop:api
`);
}

function loadDockerEnv(appDir: string): DockerEnv {
  const envPath = join(appDir, "docker", "docker.env");

  if (!existsSync(envPath)) {
    console.error(`[ERROR] ${envPath} not found.`);
    console.error(
      "Copy docker.env.example to docker.env and configure credentials."
    );
    process.exit(1);
  }

  const content = readFileSync(envPath, "utf-8");
  const env: Record<string, string> = {};

  for (const line of content.split("\n")) {
    const trimmed = line.trim();
    if (trimmed && !trimmed.startsWith("#")) {
      const eqIndex = trimmed.indexOf("=");
      if (eqIndex > 0) {
        const key = trimmed.substring(0, eqIndex);
        const value = trimmed.substring(eqIndex + 1);
        env[key] = value;
      }
    }
  }

  for (const key of ["CR_NAMESPACE", "CR_USER", "CR_PAT"]) {
    if (!env[key]) {
      console.error(`[ERROR] ${key} is not set in ${envPath}`);
      process.exit(1);
    }
  }

  return env as unknown as DockerEnv;
}

async function getGitSha(): Promise<string> {
  const result = await $`git rev-parse --short HEAD`.text();
  return result.trim();
}

async function dockerLogin(env: DockerEnv): Promise<void> {
  console.log(`[INFO] Logging into ghcr.io as ${env.CR_USER}`);
  await $`echo ${env.CR_PAT} | docker login ghcr.io -u ${env.CR_USER} --password-stdin`.quiet();
}

async function buildAssembly(appDir: string): Promise<void> {
  console.log("[INFO] Building assembly JAR...");
  await $`./mill api.assembly`.cwd(appDir);
}

async function buildImage(appDir: string, tags: string[]): Promise<void> {
  console.log("[INFO] Building Docker image...");
  const tagArgs = tags.flatMap((t) => ["-t", t]);
  await $`docker build --platform linux/amd64 -f docker/Dockerfile ${tagArgs} .`.cwd(
    appDir
  );
}

async function pushImage(tags: string[]): Promise<void> {
  for (const tag of tags) {
    console.log(`[INFO] Pushing ${tag}...`);
    await $`docker push ${tag}`;
  }
}

async function runLocal(
  appDir: string,
  config: { port: number; imageName: string }
): Promise<void> {
  const containerName = `${config.imageName}-local`;

  // Stop existing container if running
  await $`docker rm -f ${containerName}`.quiet().nothrow();

  console.log(`[INFO] Starting ${containerName} on port ${config.port}...`);

  const envFile = join(appDir, ".env");
  if (existsSync(envFile)) {
    await $`docker run -d --name ${containerName} -p ${config.port}:${config.port} --env-file ${envFile} ${config.imageName}:local`;
  } else {
    await $`docker run -d --name ${containerName} -p ${config.port}:${config.port} ${config.imageName}:local`;
  }

  console.log(`[SUCCESS] Running at http://localhost:${config.port}`);
}

async function stopLocal(config: {
  port: number;
  imageName: string;
}): Promise<void> {
  const containerName = `${config.imageName}-local`;
  await $`docker rm -f ${containerName}`.quiet().nothrow();
  console.log(`[INFO] Stopped ${containerName}`);
}

async function cmdBuild(
  appDir: string,
  config: { port: number; imageName: string }
): Promise<void> {
  await buildAssembly(appDir);
  await buildImage(appDir, [`${config.imageName}:local`]);
  console.log(`[SUCCESS] Built ${config.imageName}:local`);
}

async function cmdPush(
  appDir: string,
  config: { port: number; imageName: string }
): Promise<void> {
  const env = loadDockerEnv(appDir);
  const sha = await getGitSha();
  const namespace = env.CR_NAMESPACE.toLowerCase();
  const tags = [
    `ghcr.io/${namespace}/${config.imageName}:latest`,
    `ghcr.io/${namespace}/${config.imageName}:${sha}`,
  ];
  await dockerLogin(env);
  await pushImage(tags);
  console.log("[SUCCESS] Pushed:");
  for (const t of tags) {
    console.log(`  - ${t}`);
  }
}

async function cmdBuildPush(
  appDir: string,
  config: { port: number; imageName: string }
): Promise<void> {
  const env = loadDockerEnv(appDir);
  const sha = await getGitSha();
  const namespace = env.CR_NAMESPACE.toLowerCase();
  const tags = [
    `ghcr.io/${namespace}/${config.imageName}:latest`,
    `ghcr.io/${namespace}/${config.imageName}:${sha}`,
  ];
  await buildAssembly(appDir);
  await dockerLogin(env);
  await buildImage(appDir, tags);
  await pushImage(tags);
  console.log("[SUCCESS] Built and pushed:");
  for (const t of tags) {
    console.log(`  - ${t}`);
  }
}

const COMMAND_HANDLERS: Record<
  Command,
  (appDir: string, config: { port: number; imageName: string }) => Promise<void>
> = {
  build: cmdBuild,
  push: cmdPush,
  "build-push": cmdBuildPush,
  run: runLocal,
  stop: (_appDir, config) => stopLocal(config),
};

async function main(): Promise<void> {
  const args = parseArgs(process.argv.slice(2));

  if (args.help) {
    printHelp();
    return;
  }

  if (!args.command) {
    console.error("[ERROR] No command specified.");
    printHelp();
    process.exit(1);
  }

  if (!args.app) {
    console.error("[ERROR] No --app specified.");
    printHelp();
    process.exit(1);
  }

  if (!APPS[args.app]) {
    console.error(
      `[ERROR] Unknown app: ${args.app}. Available: ${Object.keys(APPS).join(", ")}`
    );
    process.exit(1);
  }

  const config = APPS[args.app];
  const appDir = join(ROOT_DIR, "apps", args.app);
  const handler = COMMAND_HANDLERS[args.command];

  await handler(appDir, config);
}

main().catch((err: Error) => {
  console.error("Error:", err.message);
  process.exit(1);
});
