import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import { createServer } from 'vite';
import { createRequire } from 'node:module';

export const root = path.resolve(fileURLToPath(new URL('../..', import.meta.url)));
export const upstream = path.join(root, '.work/folia-major');
export const revision = 'cc6226e464050a456a2913e062374f23d3844cc5';
export function checkReference() {
  if (execFileSync('git', ['-C', upstream, 'rev-parse', 'HEAD'], { encoding: 'utf8' }).trim() !== revision)
    throw new Error('Reference checkout must match pinned revision');
  if (execFileSync('git', ['-C', upstream, 'status', '--porcelain'], { encoding: 'utf8' }).trim())
    throw new Error('Reference checkout has modifications; do not use as oracle');
}
export async function referenceModules() {
  checkReference();
  const server = await createServer({ configFile: false, root: path.dirname(fileURLToPath(import.meta.url)),
    resolve: { alias: { '@chenglou/pretext': createRequire(import.meta.url).resolve('@chenglou/pretext'),
      'three': path.join(path.dirname(createRequire(import.meta.url).resolve('three')),'three.module.js') } },
    cacheDir: path.join(root,'.work/vite-reference-cache'), optimizeDeps: { noDiscovery: true, entries: [] },
    server: { middlewareMode: true, fs: { allow: [root] } } });
  const load = async p => server.ssrLoadModule(path.join(upstream, 'src', p));
  return { server, load };
}
