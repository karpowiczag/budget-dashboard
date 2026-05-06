import { cp, mkdir, rm, writeFile } from "node:fs/promises";
import { existsSync } from "node:fs";
import { resolve } from "node:path";

const frontendRoot = resolve(import.meta.dirname, "..");
const repoRoot = resolve(frontendRoot, "..");
const dist = resolve(frontendRoot, "dist");
const target = resolve(repoRoot, "src/main/resources/static");

if (!existsSync(dist)) {
  throw new Error("frontend/dist/ does not exist. Run npm run build from frontend/ first.");
}

await mkdir(target, { recursive: true });
await rm(target, { recursive: true, force: true });
await mkdir(target, { recursive: true });
await cp(dist, target, { recursive: true });
await writeFile(resolve(target, ".gitkeep"), "");
