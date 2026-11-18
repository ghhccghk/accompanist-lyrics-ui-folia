import { createServer } from 'vite';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import { checkReference, root, upstream } from './reference.mjs';
const require = createRequire(import.meta.url);
export async function startReferenceServer(port = 4173, { realSubtitles = false } = {}) {
checkReference();
const server = await createServer({ configFile: false, root: path.dirname(fileURLToPath(import.meta.url)),
  resolve: { alias: [
    { find: /^react$/, replacement: require.resolve('react') },
    { find: /^react-dom\/client$/, replacement: require.resolve('react-dom/client') },
    { find: /^react-dom$/, replacement: require.resolve('react-dom') },
    { find: /^framer-motion$/, replacement: require.resolve('framer-motion') },
    { find: 'react-i18next', replacement: path.join(root, 'verification/classic-browser/surfaces.tsx') },
    { find: '../VisualizerShell', replacement: path.join(root, 'verification/classic-browser/surfaces.tsx') },
    { find: '../VisualizerSubtitleOverlay', replacement: realSubtitles
      ? path.join(upstream, 'src/components/visualizer/VisualizerSubtitleOverlay.tsx')
      : path.join(root, 'verification/classic-browser/no-subtitle.tsx') },
    { find: '../../../stores/useVisualizerSettingsStore', replacement: path.join(root, 'verification/classic-browser/reference-settings.ts') },
    { find: 'pinned-classic', replacement: path.join(upstream, 'src/components/visualizer/classic/Visualizer.tsx') },
    { find: 'pinned-partita', replacement: path.join(upstream, 'src/components/visualizer/partita/VisualizerPartita.tsx') },
    { find: 'pinned-tilt', replacement: path.join(upstream, 'src/components/visualizer/tilt/VisualizerTilt.tsx') },
    { find: /^@chenglou\/pretext$/, replacement: require.resolve('@chenglou/pretext') },
    { find: '@chenglou/pretext/rich-inline', replacement: require.resolve('@chenglou/pretext/rich-inline') },
    { find: '../../hooks/usePlayerBottomBarBottomPx', replacement: path.join(root, 'verification/classic-browser/subtitle-host.ts') },
  ] },
  optimizeDeps: { noDiscovery: true, entries: [], include: ['react', 'react-dom', 'react-dom/client', 'react/jsx-runtime', 'react/jsx-dev-runtime', 'framer-motion'] },
  cacheDir: path.join(root, '.work', `reference-vite-${port}`),
  server: { host: '127.0.0.1', port, strictPort: true, hmr: false, fs: { allow: [root] } },
});
await server.listen();
return server;
}
if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const port = Number(process.env.FOLIA_REFERENCE_PORT ?? 4173);
  await startReferenceServer(port);
  console.log(`Fixed upstream reference at http://127.0.0.1:${port}`);
}
