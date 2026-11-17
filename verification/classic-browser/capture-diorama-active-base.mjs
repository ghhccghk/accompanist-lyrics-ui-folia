import fs from'node:fs';import path from'node:path';import{fileURLToPath}from'node:url';import{createRequire}from'node:module';import{createHash}from'node:crypto';
import{createServer}from'vite';import{chromium}from'playwright';import{root,revision,checkReference}from'./reference.mjs';
checkReference();const require=createRequire(import.meta.url);const here=path.dirname(fileURLToPath(import.meta.url));
const server=await createServer({configFile:false,root:here,resolve:{alias:{three:path.join(path.dirname(require.resolve('three')),'three.module.js')}},
 optimizeDeps:{noDiscovery:true,entries:[]},cacheDir:path.join(root,'.work/diorama-active-vite'),server:{host:'127.0.0.1',port:4174,strictPort:true,hmr:false,fs:{allow:[root]}}});
let browser;try{
 await server.listen();browser=await chromium.launch({headless:true,args:['--use-angle=swiftshader','--enable-unsafe-swiftshader']});
 const out=path.join(root,'build/verification/diorama-active-base-browser');fs.mkdirSync(out,{recursive:true});const observations=[];
 for(const scene of['latin','arabic','cjk'])for(const width of[380,800,801]){
  const page=await browser.newPage({viewport:{width,height:600},deviceScaleFactor:1}),errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.goto(`http://127.0.0.1:4174/diorama-active-base.html?scene=${scene}`);await page.waitForFunction(()=>window.ready);
  for(const time of[0,.7,1.1,1.5,1.9,2.3,3.1,3.45,4.0,5.25]){
   const state=await page.evaluate(t=>window.drawBase(t),time);const file=`${scene}-${width}-${Math.round(time*1000)}.png`;
   observations.push({file,scene,width,height:600,time,...state});await page.screenshot({path:path.join(out,file)});
  }
  if(errors.length)throw Error(errors.join('\n'));await page.close();
 }
 const vec=v=>[v.x,v.y,v.z];const fixtures=path.join(root,'verification/diorama-fixtures');
 const fonts=['FoliaParityLatin-Bold.ttf','FoliaParityBidi-Bold.ttf','FoliaParityChatCJK-Bold.otf'].map(file=>({file,sha256:createHash('sha256').update(fs.readFileSync(path.join(root,'verification/assets',file))).digest('hex')}));
 fs.writeFileSync(path.join(fixtures,'active-base.json'),JSON.stringify({revision,browser:browser.version(),fonts,DPR:1,
  scope:'Actual original active layout/grapheme timing/material update with glow and soul disabled, gradient1. Frozen Three perspective; no RAF envelopes/CameraRig/fog/depth. Kotlin complete-shaped slices, not source textures.',observations},null,2)+'\n');
 fs.writeFileSync(path.join(fixtures,'active-base.tsv'),['# file scene width time text64 words64StartEnd lineWidth primary accent eye look',...observations.map(o=>
  [o.file,o.scene,o.width,o.time,Buffer.from(o.text).toString('base64'),o.words.map(w=>[Buffer.from(w.text).toString('base64'),w.startTime,w.endTime].join(',')).join(';'),o.lineWidth,o.primary.join(','),o.accent.join(','),...vec(o.eye),...vec(o.look)].join('\t'))].join('\n')+'\n');
 console.log(JSON.stringify({frames:observations.length,renderer:observations[0].renderer}));
}finally{await browser?.close();await server.close();}
