import { createServer } from 'vite';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import { checkReference, root, upstream } from './reference.mjs';
checkReference();
const require = createRequire(import.meta.url);
const server = await createServer({ configFile: false, root: path.dirname(fileURLToPath(import.meta.url)),
  resolve: { alias: [
    { find: /^react$/, replacement: require.resolve('react') },
    { find: /^react-dom\/client$/, replacement: require.resolve('react-dom/client') },
    { find: /^react-dom$/, replacement: require.resolve('react-dom') },
    { find: /^framer-motion$/, replacement: require.resolve('framer-motion') },
    { find: 'react-i18next', replacement: path.join(root, 'verification/classic-browser/surfaces.tsx') },
    { find: '../VisualizerShell', replacement: path.join(root, 'verification/classic-browser/surfaces.tsx') },
    { find: '../VisualizerSubtitleOverlay', replacement: path.join(root, 'verification/classic-browser/no-subtitle.tsx') },
    { find: 'pinned-classic', replacement: path.join(upstream, 'src/components/visualizer/classic/Visualizer.tsx') },
  ] },
  server: { host: '127.0.0.1', port: 4173, strictPort: true, fs: { allow: [root] } },
});
await server.listen();
console.log('Fixed upstream classic comparison at http://127.0.0.1:4173');
