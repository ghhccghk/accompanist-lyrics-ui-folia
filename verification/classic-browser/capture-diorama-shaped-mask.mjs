import fs from 'node:fs';import path from 'node:path';import {createRequire} from 'node:module';
import {createHash} from 'node:crypto';import {createServer} from 'vite';import {chromium} from 'playwright';
import {root,upstream,revision,checkReference,referenceMotionVersions} from './reference.mjs';
checkReference();const require=createRequire(import.meta.url);
const fixture=path.join(root,'verification/diorama-fixtures/shaped-mask-v1');
if(fs.existsSync(fixture))throw Error('Preserve original complete-run fixture; refusing overwrite');
const server=await createServer({configFile:false,root:path.join(root,'verification/classic-browser'),
 resolve:{alias:{three:path.join(path.dirname(require.resolve('three')),'three.module.js')}},
 optimizeDeps:{noDiscovery:true,entries:[]},cacheDir:path.join(root,'.work/diorama-shaped-mask-vite'),
 server:{host:'127.0.0.1',port:4189,strictPort:true,hmr:false,fs:{allow:[root]}}});
let browser;
try {
 await server.listen();browser=await chromium.launch({headless:true});
 const page=await browser.newPage({viewport:{width:1600,height:600},deviceScaleFactor:1});const errors=[];
 page.on('pageerror',e=>errors.push(e.message));await page.goto('http://127.0.0.1:4189/diorama-shaped-mask.html');
 await page.waitForFunction(()=>window.shapedMaskReady);
 const captures=await page.evaluate(()=>window.shapedMasks);if(errors.length)throw Error(errors.join('\n'));
 const out=path.join(root,'build/verification/diorama-shaped-mask-browser');fs.mkdirSync(out,{recursive:true});
 for(const c of captures){fs.writeFileSync(path.join(out,c.name+'.png'),Buffer.from(c.png.split(',')[1],'base64'));delete c.png;}
 const hash=p=>createHash('sha256').update(fs.readFileSync(p)).digest('hex');
 fs.mkdirSync(fixture,{recursive:true});
 fs.writeFileSync(path.join(fixture,'conditions.json'),JSON.stringify({revision,browser:browser.version(),motion:referenceMotionVersions(),DPR:1,
  sourceHashes:['dioramaTextRaster.ts','dioramaSceneLayout.ts'].map(file=>({file,sha256:hash(path.join(upstream,'src/components/visualizer/diorama',file))})),
  fonts:['FoliaParityLatin-Bold.ttf','FoliaParityBidi-Bold.ttf','FoliaParityChatCJK-Bold.otf'].map(file=>({file,sha256:hash(path.join(root,'verification/assets',file))})),
  hostSha256:hash(path.join(root,'verification/classic-browser/diorama-shaped-mask.ts')),
  lockSha256:hash(path.join(root,'verification/classic-browser/pnpm-lock.yaml')),originalLockSha256:hash(path.join(upstream,'package-lock.json')),captures,
  scope:'Unchanged original rasterDioramaUnit(full run) + splitDioramaLyricUnits. Complete Canvas shaping reference, NOT original per-unit stage, CameraRig/Three or new theme equivalence. Synthetic italic/font128/middle/white/transparent/pad90.'},null,2)+'\n');
 fs.writeFileSync(path.join(fixture,'masks.tsv'),['# case width height advance units64',...captures.map(c=>[
  c.name,c.width,c.height,c.advance,Buffer.from(JSON.stringify(c.units)).toString('base64')].join('\t'))].join('\n')+'\n');
 fs.writeFileSync(path.join(fixture,'units.tsv'),['# case unit indexStart indexEnd start end text64',...captures.flatMap(c=>c.units.map((u,i)=>[
  c.name,i,u.charStart,u.charEnd,u.startTime,u.endTime,Buffer.from(u.text).toString('base64')].join('\t')))].join('\n')+'\n');
 console.log(JSON.stringify({cases:captures.length,browser:browser.version()}));
}finally{await browser?.close();await server.close()}
