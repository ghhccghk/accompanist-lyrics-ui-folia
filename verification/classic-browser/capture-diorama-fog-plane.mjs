import fs from 'node:fs';import path from 'node:path';import {createRequire} from 'node:module';
import {createHash} from 'node:crypto';import {createServer} from 'vite';import {chromium} from 'playwright';
import {root,upstream,revision,checkReference,referenceMotionVersions} from './reference.mjs';
checkReference();const require=createRequire(import.meta.url);
const fixture=path.join(root,'verification/diorama-fixtures/fog-plane-v1');
if(fs.existsSync(fixture))throw Error('Preserve original fog-plane capture; refusing overwrite');
const server=await createServer({configFile:false,root:path.join(root,'verification/classic-browser'),
 resolve:{alias:{three:path.join(path.dirname(require.resolve('three')),'three.module.js')}},
 optimizeDeps:{noDiscovery:true,entries:[]},cacheDir:path.join(root,'.work/diorama-fog-plane-vite'),
 server:{host:'127.0.0.1',port:4190,strictPort:true,hmr:false,fs:{allow:[root]}}});
let browser;
try {
 await server.listen();browser=await chromium.launch({headless:true,args:['--use-angle=swiftshader','--enable-unsafe-swiftshader']});
 const out=path.join(root,'build/verification/diorama-fog-plane-browser');fs.mkdirSync(out,{recursive:true});const captures=[];
 for(const viewportWidth of [380,800]){
  const page=await browser.newPage({viewport:{width:viewportWidth,height:600},deviceScaleFactor:1});const errors=[];
  page.on('pageerror',e=>errors.push(e.message));await page.goto('http://127.0.0.1:4190/diorama-fog-plane.html');
  await page.waitForFunction(()=>window.fogPlaneReady);const cases=await page.evaluate(()=>window.fogPlaneCases);
  for(const c of cases)for(const mask of ['glyph','solid'])for(const additive of [false,true]){
   const state=await page.evaluate(([n,m,a])=>window.drawFogPlane(n,m,a),[c.name,mask,additive]);
   const file=`${c.name}-${mask}-${additive?'add':'normal'}-${viewportWidth}.png`;
   await page.screenshot({path:path.join(out,file)});captures.push({file,viewportWidth,...state});
  }
  if(errors.length)throw Error(errors.join('\n'));await page.close();
 }
 const hash=p=>createHash('sha256').update(fs.readFileSync(p)).digest('hex');
 fs.mkdirSync(fixture,{recursive:true});
 fs.writeFileSync(path.join(fixture,'conditions.json'),JSON.stringify({revision,motion:referenceMotionVersions(),browser:browser.version(),three:'0.185.1',DPR:1,
  lockSha256:hash(path.join(root,'verification/classic-browser/pnpm-lock.yaml')),
  originalLockSha256:hash(path.join(upstream,'package-lock.json')),
  rasterSha256:hash(path.join(root,'verification/diorama-fixtures/plane-raster.png')),
  hostSha256:hash(path.join(root,'verification/classic-browser/diorama-fog-plane.ts')),
  captureSha256:hash(path.join(root,'verification/classic-browser/capture-diorama-fog-plane.mjs')),
  shaderHashes:['ShaderLib/meshbasic.glsl.js','ShaderChunk/fog_vertex.glsl.js','ShaderChunk/fog_fragment.glsl.js','UniformsUtils.js'].map(file=>({file,
   sha256:hash(path.join(path.dirname(require.resolve('three')),'../src/renderers/shaders',file))})),
  sourceConstantsSha256:hash(path.join(upstream,'src/components/visualizer/diorama/dioramaSceneConstants.ts')),
  scope:'Actual locked Three fog fragment shader and original Folia 12/30 constants; original shared synthetic glyph mask plus opaque-white calibration mask. Same perspective pose, .63 opacity, normal/additive. Frozen geometry isolates RGB/view-depth/perspective, NOT original mounted CameraRig or full-scene/Android acceptance.',captures},null,2)+'\n');
 fs.writeFileSync(path.join(fixture,'planes.tsv'),['# file viewportWidth mask additive alpha origin3 x3 y3 textureWidth textureHeight',...captures.map(c=>[
  c.file,c.viewportWidth,c.mask,c.additive?1:0,c.alpha,...c.origin,...c.x,...c.y,c.width,c.height].join('\t'))].join('\n')+'\n');
 console.log(JSON.stringify({frames:captures.length,browser:browser.version()}));
}finally{await browser?.close();await server.close()}
