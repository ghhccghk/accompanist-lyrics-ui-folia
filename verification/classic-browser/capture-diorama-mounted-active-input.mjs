// Frozen worst finite region: original380/144/3500, same complete callback list.
// Passive input read and explicitly labelled clone-only layer diagnostics.
import fs from 'node:fs';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {createRequire} from 'node:module';
import {createServer} from 'vite';
import {chromium} from 'playwright';
import {root,upstream,revision,checkReference,referenceMotionVersions} from './reference.mjs';
import {cameraTargetPlugin} from './diorama-camera-source.mjs';
import {cameraFlightPlugin} from './diorama-flight-source.mjs';

checkReference();
const require=createRequire(import.meta.url),hash=data=>createHash('sha256').update(data).digest('hex');
const fixture=path.join(root,'verification/diorama-fixtures/mounted-active-input-v1');
if(fs.existsSync(fixture))throw Error('Preserve original observations; refusing fixture overwrite');
const original=JSON.parse(fs.readFileSync(path.join(root,'verification/diorama-fixtures/mounted-cut-v1/scene.json'),'utf8'));
const report=JSON.parse(fs.readFileSync(path.join(root,'verification/diorama-plain-sampler/comparison.json'),'utf8'));
const file='mounted-380-144-3500.png',expected=report.rows.find(r=>r.file===file)?.hashes.original;
if(!expected)throw Error('Missing frozen original full frame');
const directory=path.resolve(path.dirname(fs.realpathSync(require.resolve('@react-three/fiber'))),'..');
const runtime=JSON.parse(fs.readFileSync(path.join(directory,'package.json'),'utf8'));
const rendering=Object.fromEntries([['@react-three/fiber',runtime.version],...Object.keys(runtime.dependencies).map(name=>
 [name,JSON.parse(fs.readFileSync(path.resolve(directory,'../..',name,'package.json'),'utf8')).version])]);
if(JSON.stringify(rendering)!==JSON.stringify(original.rendering))throw Error('Rendering versions changed');
if(JSON.stringify(referenceMotionVersions())!==JSON.stringify(original.motion))throw Error('Motion versions changed');
const aliases=['react','react/jsx-runtime','react/jsx-dev-runtime','react-dom','react-dom/client','@react-three/fiber','framer-motion']
 .map(n=>({find:new RegExp('^'+n+'$'),replacement:require.resolve(n)}));
aliases.push({find:/^three$/,replacement:path.join(path.dirname(require.resolve('three')),'three.module.js')});
const server=await createServer({configFile:false,root:path.join(root,'verification/classic-browser'),
 plugins:[cameraTargetPlugin(),cameraFlightPlugin()],resolve:{alias:aliases,dedupe:['react','react-dom','three','@react-three/fiber']},
 optimizeDeps:{noDiscovery:true,entries:[],include:['react','react/jsx-runtime','react/jsx-dev-runtime','react-dom','react-dom/client','@react-three/fiber','framer-motion','three']},
 cacheDir:path.join(root,'.work/diorama-mounted-vite-v2'),server:{host:'127.0.0.1',port:4188,strictPort:true,hmr:false,fs:{allow:[root]}}});
const out=path.join(root,'build/verification/diorama-mounted-active-input-browser');
let browser;
try{
 await server.listen();browser=await chromium.launch({headless:true,args:['--use-angle=swiftshader','--enable-unsafe-swiftshader']});
 if(browser.version()!==original.browser)throw Error('Browser version changed');
 const page=await browser.newPage({viewport:{width:380,height:600},deviceScaleFactor:1}),errors=[];
 page.on('pageerror',e=>errors.push(e.message));
 await page.goto('http://127.0.0.1:4188/diorama-mounted.html');await page.waitForFunction(()=>window.mountedReady,null,{timeout:20000});
 await page.screenshot();
 const steps=fs.readFileSync(path.join(root,'verification/diorama-fixtures/mounted-cut-v1/steps.tsv'),'utf8')
  .split(/\r?\n/).filter(l=>l&&!l.startsWith('#')).map(l=>l.split('\t'))
  .filter(r=>r[1]==='380'&&r[2]==='144'&&(r[0]==='outgoing'||Number(r[3])<=3.5));
 let cut=false;const observed=[];
 for(const r of steps){
  if(r[0]==='incoming'&&!cut){await page.evaluate(()=>window.beginMountedCut());cut=true;}
  const s=await page.evaluate(([t,w])=>window.stepMounted(t,w),[Number(r[3]),Number(r[4])]);
  if(s.delta!==Number(r[5]))throw Error('Original callback delta changed');
  observed.push({phase:r[0],time:s.time,wall:s.wall,delta:s.delta});
 }
 const before=await page.screenshot(),stateBefore=await page.evaluate(()=>window.readMounted());
 if(hash(before)!==expected)throw Error(`Original BEFORE mismatch ${hash(before)} != ${expected}`);
 const planes=await page.evaluate(()=>window.readMountedActivePlanes());
 const after=await page.screenshot(),stateAfter=await page.evaluate(()=>window.readMounted());
 if(hash(after)!==expected||JSON.stringify(stateBefore)!==JSON.stringify(stateAfter))throw Error('Passive input read changed original');
 fs.mkdirSync(out,{recursive:true});const layers=[];
 for(const kind of ['all','base','glow','soul']){
  await page.evaluate(k=>window.renderMountedActiveLayer(k),kind);
  const data=await page.screenshot(),name=`active-${kind}.png`;
  fs.writeFileSync(path.join(out,name),data);layers.push({kind,file:name,sha256:hash(data)});
 }
 await page.evaluate(()=>window.renderMountedActiveLayer(null));
 const restored=await page.screenshot(),stateRestored=await page.evaluate(()=>window.readMounted());
 const planesRestored=await page.evaluate(()=>window.readMountedActivePlanes());
 if(hash(restored)!==expected||JSON.stringify(stateBefore)!==JSON.stringify(stateRestored)||JSON.stringify(planes)!==JSON.stringify(planesRestored))throw Error('Cloned layer render changed original state/texture/matrix/pixels');
 const teardown=await page.evaluate(()=>window.unmountMounted());
 if(teardown.roots||teardown.queuedRafs||errors.length)throw Error(JSON.stringify({teardown,errors}));
 fs.writeFileSync(path.join(out,'original-before.png'),before);fs.writeFileSync(path.join(out,'original-restored.png'),restored);
 const inputs=planes.map(({png,...p})=>{
  const data=Buffer.from(png.split(',')[1],'base64'),name=`unit-${p.unit}-order-${p.order}.png`;
  fs.writeFileSync(path.join(out,name),data);return{...p,file:name,sha256:hash(data)};
 });
 const sourceFiles=['DioramaActiveUnitPlanes.tsx','DioramaScene.tsx','dioramaSceneFrame.ts','dioramaSceneLayout.ts','dioramaTextRaster.ts'];
 const record={revision,motion:referenceMotionVersions(),rendering,browser:browser.version(),file,expectedPngSha256:expected,
  width:380,height:600,DPR:1,fps:144,time:3.5,wall:10.3,callbacks:observed.length,
  callbackHistorySha256:hash(Buffer.from(JSON.stringify(observed))),state:stateBefore,inputs,layers,teardown,
  sourceFiles:sourceFiles.map(file=>({file,sha256:hash(fs.readFileSync(path.join(upstream,'src/components/visualizer/diorama',file)))})),
  hostFiles:['diorama-mounted.tsx','capture-diorama-mounted-active-input.mjs','pnpm-lock.yaml'].map(file=>({file,sha256:hash(fs.readFileSync(path.join(root,'verification/classic-browser',file)))})),
  fontSha256:hash(fs.readFileSync(path.join(root,'verification/assets/FoliaParityLatin-Bold.ttf'))),
  originalBeforeAfterRestoredExact:true,originalStateTextureVersionsMatricesExact:true,
  scope:'Actual original active input read, unchanged frozen whole frame and exact full source callback list. Layer images are labelled diagnostic clones of the actual existing meshes/materials/textures, never replacement original acceptance frames. No source ref writes, new RAF or original resource disposal.'};
 fs.mkdirSync(fixture,{recursive:true});fs.writeFileSync(path.join(fixture,'scene.json'),JSON.stringify(record,null,2)+'\n');
 console.log(JSON.stringify({file,inputs:inputs.length,callbacks:observed.length,teardown,expected}));
}finally{await browser?.close();await server.close();}
