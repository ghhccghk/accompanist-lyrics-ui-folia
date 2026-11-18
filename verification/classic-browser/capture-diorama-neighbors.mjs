import fs from 'node:fs';import path from 'node:path';import {createRequire} from 'node:module';
import {createHash} from 'node:crypto';import {createServer} from 'vite';import {chromium} from 'playwright';
import {root,upstream,revision,checkReference,referenceMotionVersions} from './reference.mjs';
checkReference();const require=createRequire(import.meta.url);
const fixture=path.join(root,'verification/diorama-fixtures/neighbors-v1');
if(fs.existsSync(fixture))throw Error('Preserve original neighbor fixture; refusing overwrite');
const server=await createServer({configFile:false,root:path.join(root,'verification/classic-browser'),
 resolve:{alias:{three:path.join(path.dirname(require.resolve('three')),'three.module.js')}},
 optimizeDeps:{noDiscovery:true,entries:[]},cacheDir:path.join(root,'.work/diorama-neighbor-vite'),
 server:{host:'127.0.0.1',port:4188,strictPort:true,hmr:false,fs:{allow:[root]}}});
let browser;
try {
 await server.listen();browser=await chromium.launch({headless:true,args:['--use-angle=swiftshader','--enable-unsafe-swiftshader']});
 const out=path.join(root,'build/verification/diorama-neighbors-browser');fs.mkdirSync(out,{recursive:true});
 const captures=[],inputs={};
 for(const scene of ['latin','arabic','cjk'])for(const width of [380,800]){
  const page=await browser.newPage({viewport:{width,height:600},deviceScaleFactor:1});const errors=[];
  page.on('pageerror',e=>errors.push(e.message));await page.goto(`http://127.0.0.1:4188/diorama-neighbors.html?scene=${scene}`);
  await page.waitForFunction(()=>window.neighborsReady);inputs[scene]=await page.evaluate(()=>window.neighborInputs);
  for(const current of [0,3,7])for(const distance of [5.2,14,36]){
   // Exact integer-ms media boundary +450ms; original words need not have integer ends.
   const time=(700+current*2400+450)/1000;
   const state=await page.evaluate(([i,d,t])=>window.drawNeighbors(i,d,t),[current,distance,time]);
   const file=`${scene}-${width}-${current}-${distance}.png`;await page.screenshot({path:path.join(out,file)});
   captures.push({file,scene,width,...state});
  }
  if(errors.length)throw Error(errors.join('\n'));await page.close();
 }
 const hash=p=>createHash('sha256').update(fs.readFileSync(p)).digest('hex');
 const sources=['dioramaSceneLayout.ts','dioramaSceneFrame.ts','dioramaSceneConstants.ts','dioramaSceneUnits.ts','dioramaTextRaster.ts','cameraPath.ts','dioramaSequencer.ts']
  .map(file=>({file,sha256:hash(path.join(upstream,'src/components/visualizer/diorama',file))}));
 fs.mkdirSync(fixture,{recursive:true});
 fs.writeFileSync(path.join(fixture,'scene.json'),JSON.stringify({revision,motion:referenceMotionVersions(),browser:browser.version(),three:'0.185.1',DPR:1,
  sourceHashes:sources,lockSha256:hash(path.join(root,'verification/classic-browser/pnpm-lock.yaml')),
  originalLockSha256:hash(path.join(upstream,'package-lock.json')),
  hostSha256:hash(path.join(root,'verification/classic-browser/diorama-neighbors.ts')),
  fonts:['FoliaParityLatin-Bold.ttf','FoliaParityBidi-Bold.ttf','FoliaParityChatCJK-Bold.otf'].map(file=>({file,sha256:hash(path.join(root,'verification/assets',file))})),
  inputs,captures,scope:'Actual original single-segment window/visible lines/raster/material functions and Three text composition. Frozen source camera; analytic source-unit charges injected at delta0, life original, breath1/audio0. NO React scene/CameraRig, fog, background, opaque-depth or cross-song transition acceptance.'},null,2)+'\n');
 fs.writeFileSync(path.join(fixture,'frames.tsv'),['# file scene width current distance time eye3 look3 activeLife',...captures.map(c=>[
  c.file,c.scene,c.width,c.current,c.distance,c.time,...c.eye,...c.look,c.activeLife].join('\t'))].join('\n')+'\n');
 fs.writeFileSync(path.join(fixture,'neighbors.tsv'),['# file index advance fontPx fit opacity linearColor3 center3 depth',...captures.flatMap(c=>c.neighbors.map(n=>[
  c.file,n.index,n.advance,n.fontPx,n.fit,n.opacity,...n.color,...n.center,n.depth].join('\t')))].join('\n')+'\n');
 fs.writeFileSync(path.join(fixture,'windows.tsv'),['# total current indices; - means empty',...inputs.latin.windows.map(w=>[w.total,w.current,w.indices.join(',')||'-'].join('\t'))].join('\n')+'\n');
 console.log(JSON.stringify({frames:captures.length,neighborRows:captures.reduce((n,c)=>n+c.neighbors.length,0),browser:browser.version()}));
}finally{await browser?.close();await server.close()}
