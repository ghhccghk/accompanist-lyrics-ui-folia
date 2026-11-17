import {compile} from 'tailwindcss';import fs from 'node:fs';import {createRequire} from 'node:module';import{upstream,checkReference}from'./reference.mjs';
checkReference();const require=createRequire(import.meta.url);
const css=fs.readFileSync(require.resolve('tailwindcss/theme.css'),'utf8');
const source=fs.readFileSync(`${upstream}/src/components/visualizer/fume/VisualizerFume.tsx`,'utf8');
const classes=[...new Set([...source.matchAll(/className="([^"]+)"/g)].flatMap(m=>m[1].split(/\s+/)))];
const compiler=await compile(`${css}\n@tailwind utilities;`);
fs.writeFileSync(new URL('./fume-utilities.css',import.meta.url),compiler.build(classes));
