import fs from 'node:fs';import path from 'node:path';import {createRequire} from 'node:module';
import {createHash} from 'node:crypto';import {createServer} from 'vite';import {chromium} from 'playwright';
import {root,revision,checkReference,referenceMotionVersions} from './reference.mjs';
checkReference();const require=createRequire(import.meta.url);
const fixture=path.join(root,'verification/diorama-fixtures/clipping-v1');
if(fs.existsSync(fixture))throw Error('Preserve original clipping capture; refusing overwrite');
const server=await createServer({configFile:false,root:path.join(root,'verification/classic-browser'),
 resolve:{alias:{three:path.join(path.dirname(require.resolve('three')),'three.module.js')}},
 optimizeDeps:{noDiscovery:true,entries:[]},cacheDir:path.join(root,'.work/diorama-clipping-vite'),
 server:{host:'127.0.0.1',port:4187,strictPort:true,hmr:false,fs:{allow:[root]}}});
let browser;
try {
 await server.listen();browser=await chromium.launch({headless:true,args:['--use-angle=swiftshader','--enable-unsafe-swiftshader']});
 const out=path.join(root,'build/verification/diorama-clipping-browser');fs.mkdirSync(out,{recursive:true});
 const captures=[];
 const cases=[['front',1.2,0],['near-cross',.4,1.1],['behind-cross',-.3,1.1],['before-near',.2,0],
  ['beyond-far',2.2,0],['backface',1.2,Math.PI],['far-cross',1.9,.6],['eye-cross',0,.8],['near-and-far',1.2,1.4]];
 for(const width of [380,800]){
  const page=await browser.newPage({viewport:{width,height:600},deviceScaleFactor:1});const errors=[];
  page.on('pageerror',e=>errors.push(e.message));await page.goto('http://127.0.0.1:4187/diorama-clipping.html');
  await page.waitForFunction(()=>window.clipReady);
  for(const [name,distance,angle] of cases){
   const state=await page.evaluate(([d,a])=>window.drawClip(d,a),[distance,angle]);const file=`${name}-${width}.png`;
   await page.screenshot({path:path.join(out,file)});captures.push({file,name,viewportWidth:width,distance,angle,...state});
  }
  if(errors.length)throw Error(errors.join('\n'));await page.close();
 }
 fs.mkdirSync(fixture,{recursive:true});
 fs.writeFileSync(path.join(fixture,'conditions.json'),JSON.stringify({revision,motion:referenceMotionVersions(),browser:browser.version(),three:'0.185.1',DPR:1,
  rasterSha256:createHash('sha256').update(fs.readFileSync(path.join(root,'verification/diorama-fixtures/plane-raster.png'))).digest('hex'),
  scope:'Locked actual Three MeshBasicMaterial default FrontSide and GPU homogeneous clipping, shared original synthetic raster, frozen front/near/far/behind/backface geometry. NOT a text shaping/depth buffer/full scene or original CameraRig test.',captures},null,2)+'\n');
 fs.writeFileSync(path.join(fixture,'planes.tsv'),['# file viewportWidth originXYZ xXYZ yDownXYZ width height near far',...captures.map(c=>[
  c.file,c.viewportWidth,...c.origin,...c.x,...c.y,c.width,c.height,c.near,c.far].join('\t'))].join('\n')+'\n');
 console.log(JSON.stringify({frames:captures.length}));
}finally{await browser?.close();await server.close()}
