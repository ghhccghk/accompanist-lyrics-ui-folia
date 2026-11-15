// Use the pinned source lockfile's Tailwind version for its backdrop utilities.
import {compile} from 'tailwindcss';
import fs from 'node:fs';
import {createRequire} from 'node:module';
const require=createRequire(import.meta.url);
const theme=fs.readFileSync(require.resolve('tailwindcss/theme.css'),'utf8');
const compiler=await compile(`${theme}\n@tailwind utilities;`);
const css=compiler.build(['isolate','px-1.5','py-0.5','-inset-x-10','-inset-y-6','z-0','blur-2xl']);
fs.writeFileSync(new URL('./subtitle-utilities.css',import.meta.url),css);
