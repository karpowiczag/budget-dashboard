import { cp, mkdir, rm, writeFile } from "node:fs/promises";
import { existsSync } from "node:fs";
import { resolve } from "node:path";

const root = resolve(import.meta.dirname, "..");
const dist = resolve(root, "dist");
const target = resolve(root, "src/main/resources/static");

if (!existsSync(dist)) {
  throw new Error("dist/ does not exist. Run npm run build first.");
}

await mkdir(target, { recursive: true });
await rm(target, { recursive: true, force: true });
await mkdir(target, { recursive: true });
await cp(dist, target, { recursive: true });
await writeFile(resolve(target, ".gitkeep"), "");
