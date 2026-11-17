import fs from 'node:fs';import path from 'node:path';import {fileURLToPath} from 'node:url';import {createRequire} from 'node:module';
import {createServer} from 'vite';import {chromium} from 'playwright';import {root,revision,checkReference} from './reference.mjs';
checkReference();const require=createRequire(import.meta.url);
const server=await createServer({configFile:false,root:path.dirname(fileURLToPath(import.meta.url)),
 resolve:{alias:{three:path.join(path.dirname(require.resolve('three')),'three.module.js')}},
 optimizeDeps:{noDiscovery:true,entries:[]},
 cacheDir:path.join(root,'.work/diorama-vite-cache'),server:{host:'127.0.0.1',port:4174,strictPort:true,hmr:false,fs:{allow:[root]}}});
let browser;
try{
 await server.listen();browser=await chromium.launch({headless:true,args:['--use-angle=swiftshader','--enable-unsafe-swiftshader']});
 const out=path.join(root,'build/verification/diorama-projection-browser');fs.mkdirSync(out,{recursive:true});
 const fixtures=path.join(root,'verification/diorama-fixtures');fs.mkdirSync(fixtures,{recursive:true});
 const observations=[];let renderer;
 for(const width of[380,800,801]){
  const page=await browser.newPage({viewport:{width,height:600},deviceScaleFactor:1});
  page.on('pageerror',e=>console.error(e));await page.goto('http://127.0.0.1:4174/diorama-projection.html');await page.waitForFunction(()=>window.ready);
  if(width===380)fs.writeFileSync(path.join(fixtures,'plane-raster.png'),Buffer.from((await page.evaluate(()=>window.rasterPng)).split(',')[1],'base64'));
  for(const [right,up]of[[1.7,.8],[-3.6,1.9],[4.4,-1.2]]){
   const o=await page.evaluate(([r,u])=>window.drawProbe(r,u),[right,up]);const file=`${width}-${right}-${up}.png`;
   renderer=o.renderer;observations.push({file,width,height:600,right,up,...o});await page.screenshot({path:path.join(out,file)});
  }
  await page.close();
 }
 fs.writeFileSync(path.join(fixtures,'planes.json'),JSON.stringify({revision,browser:browser.version(),renderer,
  scope:'frozen actual Three.js plane + original raster/path/placement; no CameraRig, fog, depth occlusion, text animation or React scene',observations},null,2)+'\n');
 // Kotlin test reads only explicit geometry, no JSON runtime dependency.
 const vec=v=>[v.x,v.y,v.z];
 fs.writeFileSync(path.join(fixtures,'planes.tsv'),['# file width height eye look origin axisX axisY rasterWidth rasterHeight',...observations.map(o=>
  [o.file,o.width,o.height,...vec(o.eye),...vec(o.look),...vec(o.origin),...vec(o.x),...vec(o.y),o.rasterWidth,o.rasterHeight].join('\t'))].join('\n')+'\n');
 console.log(JSON.stringify({frames:observations.length,renderer}));
}finally{await browser?.close();await server.close();}
