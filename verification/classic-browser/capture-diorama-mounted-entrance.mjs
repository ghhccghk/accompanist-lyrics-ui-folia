// Four already-failed700ms frames, exact original callback prefixes. Passive
// inspection and clone-only layers; never inject native states into the source.
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
const alphaDiagnostic=process.argv.includes('--alpha');
const fixture=path.join(root,`verification/diorama-fixtures/mounted-entrance${alphaDiagnostic?'-alpha':''}-v1`);
if(fs.existsSync(fixture))throw Error('Preserve original observations; refusing fixture overwrite');
const original=JSON.parse(fs.readFileSync(path.join(root,'verification/diorama-fixtures/mounted-cut-v1/scene.json'),'utf8'));
const report=JSON.parse(fs.readFileSync(path.join(root,'verification/diorama-active-base-sampler/comparison.json'),'utf8'));
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
const out=path.join(root,`build/verification/diorama-mounted-entrance${alphaDiagnostic?'-alpha':''}-browser`);
const sourceSteps=path.join(root,'verification/diorama-fixtures/mounted-cut-v1/steps.tsv');
const allRows=fs.readFileSync(sourceSteps,'utf8').split(/\r?\n/).filter(l=>l&&!l.startsWith('#')).map(l=>l.split('\t'));
const captures=[];let browser;
try{
 await server.listen();browser=await chromium.launch({headless:true,args:['--use-angle=swiftshader','--enable-unsafe-swiftshader']});
 if(browser.version()!==original.browser)throw Error('Browser version changed');
 for(const width of [380,800])for(const fps of [60,144]){
  const file=`mounted-${width}-${fps}-700.png`,expected=report.rows.find(r=>r.file===file)?.hashes.original;
  if(!expected)throw Error('Missing fixed original frame');
  const page=await browser.newPage({viewport:{width,height:600},deviceScaleFactor:1}),errors=[];
  page.on('pageerror',e=>errors.push(e.message));
  try{
   await page.goto('http://127.0.0.1:4188/diorama-mounted.html');await page.waitForFunction(()=>window.mountedReady,null,{timeout:20000});
   await page.screenshot();
   const rows=allRows.filter(r=>+r[1]===width&&+r[2]===fps&&(r[0]==='outgoing'||+r[3]<=.7));
   let cut=false;const observed=[];
   for(const r of rows){
    if(r[0]==='incoming'&&!cut){await page.evaluate(()=>window.beginMountedCut());cut=true;}
    const s=await page.evaluate(([t,w])=>window.stepMounted(t,w),[+r[3],+r[4]]);
    if(s.delta!==+r[5])throw Error('Original callback delta changed');
    observed.push({phase:r[0],time:s.time,wall:s.wall,delta:s.delta});
   }
   const before=await page.screenshot(),state=await page.evaluate(()=>window.readMounted());
   if(hash(before)!==expected)throw Error(`${file} original changed ${hash(before)} != ${expected}`);
   const planes=await page.evaluate(()=>window.readMountedActivePlanes());
   if(hash(await page.screenshot())!==expected||JSON.stringify(state)!==JSON.stringify(await page.evaluate(()=>window.readMounted())))throw Error('Passive read changed original');
   const prefix=`${width}-${fps}`,layers=[];
   fs.mkdirSync(out,{recursive:true});fs.writeFileSync(path.join(out,`${prefix}-original.png`),before);
   for(const kind of ['all','base','glow','soul','neighbors']){
    await page.evaluate(k=>window.renderMountedActiveLayer(k),kind);
    const data=await page.screenshot(),name=`${prefix}-${kind}.png`;
    fs.writeFileSync(path.join(out,name),data);
    let framebuffer;
    if(alphaDiagnostic){
     const {png,...attributes}=await page.evaluate(()=>{
      const canvas=document.querySelector('canvas'),gl=canvas.getContext('webgl2');
      return{png:canvas.toDataURL('image/png'),context:gl.getContextAttributes(),
       clearColor:Array.from(gl.getParameter(gl.COLOR_CLEAR_VALUE)),
       cssBackground:getComputedStyle(document.body).backgroundColor};
     });
     const bytes=Buffer.from(png.split(',')[1],'base64'),file=`${prefix}-${kind}-framebuffer.png`;
     fs.writeFileSync(path.join(out,file),bytes);framebuffer={...attributes,file,sha256:hash(bytes)};
    }
    layers.push({kind,file:name,sha256:hash(data),...(framebuffer?{framebuffer}:{})});
   }
   await page.evaluate(()=>window.renderMountedActiveLayer(null));
   const restored=await page.screenshot(),restoredState=await page.evaluate(()=>window.readMounted());
   if(hash(restored)!==expected||JSON.stringify(state)!==JSON.stringify(restoredState)||
      JSON.stringify(planes)!==JSON.stringify(await page.evaluate(()=>window.readMountedActivePlanes())))throw Error('Clone render changed original state/pixels/input versions');
   fs.writeFileSync(path.join(out,`${prefix}-restored.png`),restored);
   const inputs=planes.map(({png,...p})=>{
    const data=Buffer.from(png.split(',')[1],'base64'),name=`${prefix}-unit-${p.unit}-order-${p.order}.png`;
    fs.writeFileSync(path.join(out,name),data);return{...p,file:name,sha256:hash(data)};
   });
   const teardown=await page.evaluate(()=>window.unmountMounted());
   if(teardown.roots||teardown.queuedRafs||errors.length)throw Error(JSON.stringify({teardown,errors}));
   captures.push({file,width,height:600,fps,time:.7,wall:observed.at(-1).wall,callbacks:observed.length,
    callbackHistorySha256:hash(Buffer.from(JSON.stringify(observed))),expectedOriginalSha256:expected,state,inputs,layers,teardown});
   console.log(JSON.stringify({file,callbacks:observed.length,expected,teardown}));
  }finally{await page.close();}
 }
 const sourceFiles=['DioramaActiveUnitPlanes.tsx','DioramaScene.tsx','dioramaSceneFrame.ts','dioramaSceneLayout.ts','dioramaTextRaster.ts'];
 const record={revision,browser:browser.version(),motion:referenceMotionVersions(),rendering,DPR:1,
  fontSha256:hash(fs.readFileSync(path.join(root,'verification/assets/FoliaParityLatin-Bold.ttf'))),
  originalStepsSha256:hash(fs.readFileSync(sourceSteps)),captures,
  sourceFiles:sourceFiles.map(file=>({file,sha256:hash(fs.readFileSync(path.join(upstream,'src/components/visualizer/diorama',file)))})),
  hostFiles:['diorama-mounted.tsx','capture-diorama-mounted-entrance.mjs','pnpm-lock.yaml'].map(file=>({file,sha256:hash(fs.readFileSync(path.join(root,'verification/classic-browser',file)))})),
  originalFullFramesStateMatricesVersionsExact:true,
  scope:'EXISTING four700ms failed frames with exact callback prefixes, actual original CameraRig/DioramaScene. Cloned active/base/glow/soul/neighbors are diagnostics only. No ref/clock/logic/texture edits; originals remain acceptance frames.'};
 fs.mkdirSync(fixture,{recursive:true});fs.writeFileSync(path.join(fixture,'scene.json'),JSON.stringify(record,null,2)+'\n');
}finally{await browser?.close();await server.close();}
