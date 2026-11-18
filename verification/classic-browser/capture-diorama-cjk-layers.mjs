// One frozen isolated source composition, not another whole matrix capture.
import fs from 'node:fs';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {createRequire} from 'node:module';
import {createServer} from 'vite';
import {chromium} from 'playwright';
import {root,upstream,revision,checkReference,referenceMotionVersions} from './reference.mjs';
checkReference();
const require=createRequire(import.meta.url),hash=data=>createHash('sha256').update(data).digest('hex');
const fixture=path.join(root,'verification/diorama-fixtures/active-cjk-layers-v1');
if(fs.existsSync(fixture))throw Error('Preserve source observations; refusing overwrite');
const existing=JSON.parse(fs.readFileSync(path.join(root,'verification/diorama-active-layers/comparison.json'),'utf8'));
const file='cjk-800-active-1100.png',original=existing.frames.find(r=>r.file===file);
const expected=Object.entries(original.png_sha256).find(([k])=>k.includes('browser'))[1];
const server=await createServer({configFile:false,root:path.join(root,'verification/classic-browser'),
 resolve:{alias:{three:path.join(path.dirname(require.resolve('three')),'three.module.js')}},
 optimizeDeps:{noDiscovery:true,entries:[]},cacheDir:path.join(root,'.work/diorama-cjk-layers-vite'),
 server:{host:'127.0.0.1',port:4189,strictPort:true,hmr:false,fs:{allow:[root]}}});
const out=path.join(root,'build/verification/diorama-cjk-layers-browser');let browser;
try{
 await server.listen();browser=await chromium.launch({headless:true,args:['--use-angle=swiftshader','--enable-unsafe-swiftshader']});
 const page=await browser.newPage({viewport:{width:800,height:600},deviceScaleFactor:1}),errors=[];
 page.on('pageerror',e=>errors.push(e.message));
 await page.goto('http://127.0.0.1:4189/diorama-active-layers.html?scene=cjk');await page.waitForFunction(()=>window.ready);
 const observed=await page.evaluate(()=>window.drawLayers(1.1,true));
 const before=await page.screenshot();if(hash(before)!==expected)throw Error('Original full frame changed: '+hash(before));
 const inputs=await page.evaluate(()=>window.readActiveLayerInputs());
 const after=await page.screenshot();if(hash(after)!==expected)throw Error('Passive input read changed frame');
 fs.mkdirSync(out,{recursive:true});const layers=[];
 for(const kind of ['all','base','glow','soul']){
  await page.evaluate(k=>window.renderActiveLayer(k),kind);
  const data=await page.screenshot(),name=kind+'.png';fs.writeFileSync(path.join(out,name),data);layers.push({kind,file:name,sha256:hash(data)});
 }
 await page.evaluate(()=>window.renderActiveLayer(null));
 const restored=await page.screenshot(),restoredInputs=await page.evaluate(()=>window.readActiveLayerInputs());
 if(hash(restored)!==expected||JSON.stringify(inputs)!==JSON.stringify(restoredInputs))throw Error('Clone layer render changed original inputs/full frame');
 if(layers.find(x=>x.kind==='all').sha256!==expected)throw Error('Clone full scene differs');
 fs.writeFileSync(path.join(out,'original.png'),before);
 const release=await page.evaluate(()=>window.releaseActiveLayerStage());
 if(release.geometries||release.textures||errors.length)throw Error(JSON.stringify({release,errors}));
 const sourceFiles=['dioramaSceneLayout.ts','dioramaTextRaster.ts','dioramaSceneFrame.ts','DioramaActiveUnitPlanes.tsx'];
 const data={revision,browser:browser.version(),motion:referenceMotionVersions(),three:JSON.parse(fs.readFileSync(path.resolve(path.dirname(require.resolve('three')),'../package.json'),'utf8')).version,
  file,width:800,height:600,DPR:1,time:1.1,soulActive:true,expectedOriginalSha256:expected,observed,inputs,layers,release,
  sourceFiles:sourceFiles.map(file=>({file,sha256:hash(fs.readFileSync(path.join(upstream,'src/components/visualizer/diorama',file)))})),
  hostFiles:['diorama-active-layers.ts','capture-diorama-cjk-layers.mjs','pnpm-lock.yaml'].map(file=>({file,sha256:hash(fs.readFileSync(path.join(root,'verification/classic-browser',file)))})),
  fontSha256:hash(fs.readFileSync(path.join(root,'verification/assets/FoliaParityChatCJK-Bold.otf'))),
  originalFullFrameInputsAndVersionsUnchanged:true,
  scope:'Existing isolated original layout/raster/material stage; original analytic-envelope Float32 injection/delta0 retained, NOT mounted CameraRig/RAF. Only passive read and labelled cloned layers. No source edits, new timing, source-ref changes or replacement original expected image.'};
 fs.mkdirSync(fixture,{recursive:true});fs.writeFileSync(path.join(fixture,'scene.json'),JSON.stringify(data,null,2)+'\n');
 console.log(JSON.stringify({file,expected,layers,release}));
}finally{await browser?.close();await server.close();}
