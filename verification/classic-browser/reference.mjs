import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import fs from 'node:fs';
import { createServer } from 'vite';
import { createRequire } from 'node:module';

export const root = path.resolve(fileURLToPath(new URL('../..', import.meta.url)));
export const upstream = path.join(root, '.work/folia-major');
export const revision = 'cc6226e464050a456a2913e062374f23d3844cc5';
export function referenceMotionVersions() {
  const require = createRequire(import.meta.url);
  const motionRequire = createRequire(require.resolve('framer-motion'));
  const packages = ['framer-motion', 'motion-dom', 'motion-utils'];
  const locked = JSON.parse(fs.readFileSync(path.join(upstream, 'package-lock.json'), 'utf8'));
  return Object.fromEntries(packages.map(name => {
    let directory = path.dirname((name === 'framer-motion' ? require : motionRequire).resolve(name));
    let metadata;
    while (true) {
      const candidate = path.join(directory, 'package.json');
      if (fs.existsSync(candidate)) {
        const value = JSON.parse(fs.readFileSync(candidate, 'utf8'));
        if (value.name === name) { metadata = value; break; }
      }
      const parent = path.dirname(directory);
      if (parent === directory) throw new Error(`Cannot find package metadata: ${name}`);
      directory = parent;
    }
    const version = metadata.version;
    const expected = locked.packages['node_modules/' + name].version;
    if (version !== expected) throw new Error(`${name}: installed ${version}, original lock ${expected}`);
    return [name, version];
  }));
}
export function checkReference() {
  if (execFileSync('git', ['-C', upstream, 'rev-parse', 'HEAD'], { encoding: 'utf8' }).trim() !== revision)
    throw new Error('Reference checkout must match pinned revision');
  if (execFileSync('git', ['-C', upstream, 'status', '--porcelain'], { encoding: 'utf8' }).trim())
    throw new Error('Reference checkout has modifications; do not use as oracle');
  referenceMotionVersions();
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
