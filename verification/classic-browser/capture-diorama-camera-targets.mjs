import fs from'node:fs';import path from'node:path';import{fileURLToPath}from'node:url';import{createRequire}from'node:module';import{createHash}from'node:crypto';
import{createServer}from'vite';import{cameraTargetPlugin,sourceFragments}from'./diorama-camera-source.mjs';import{chromium}from'playwright';import{root,revision,checkReference}from'./reference.mjs';
checkReference();const require=createRequire(import.meta.url);const here=path.dirname(fileURLToPath(import.meta.url));
const server=await createServer({configFile:false,root:here,plugins:[cameraTargetPlugin()],resolve:{alias:{three:path.join(path.dirname(require.resolve('three')),'three.module.js')}},
 optimizeDeps:{noDiscovery:true,entries:[]},cacheDir:path.join(root,'.work/diorama-camera-vite'),server:{host:'127.0.0.1',port:4176,strictPort:true,hmr:false,fs:{allow:[root]}}});
let browser;try{
 await server.listen();browser=await chromium.launch({headless:true,args:['--use-angle=swiftshader','--enable-unsafe-swiftshader']});
 const out=path.join(root,'build/verification/diorama-camera-targets-browser');fs.mkdirSync(out,{recursive:true});const observations=[];
 for(const scene of['latin','arabic','cjk'])for(const width of[380,800]){
  const page=await browser.newPage({viewport:{width,height:600},deviceScaleFactor:1}),errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.goto(`http://127.0.0.1:4176/diorama-camera-targets.html?scene=${scene}`);await page.waitForFunction(()=>window.targetReady);
  const shots=scene==='latin'&&width===800?['pushIn','pullBack','orbit','track','crane','hold','swell','spiral','pendulum','flyby','arc','float','glide']:['orbit','track','hold','glide'];
  for(const shot of shots)for(const time of[.7,1.5,3.45,8.92]){
   const state=await page.evaluate(([t,k])=>window.drawTarget(t,k),[time,shot]);const file=`${scene}-${width}-${shot}-${Math.round(time*1000)}.png`;
   observations.push({file,scene,width,height:600,time,shot,...state});await page.screenshot({path:path.join(out,file)});
  }
  if(errors.length)throw Error(errors.join('\n'));await page.close();
 }
 const vec=v=>[v.x,v.y,v.z];const fixtures=path.join(root,'verification/diorama-fixtures');
 const fonts=['FoliaParityLatin-Bold.ttf','FoliaParityBidi-Bold.ttf','FoliaParityChatCJK-Bold.otf'].map(file=>({file,sha256:createHash('sha256').update(fs.readFileSync(path.join(root,'verification/assets',file))).digest('hex')}));
 fs.writeFileSync(path.join(fixtures,'camera-targets.json'),JSON.stringify({revision,browser:browser.version(),fonts,DPR:1,sourceFragments,
  scope:'Exact extracted original CameraRig progress/desired pose/look before damped follow. Real Three three-layer text; analytic media charges/delta0, no original RAF/orientation/alignment/fog/depth.',observations},null,2)+'\n');
 fs.writeFileSync(path.join(fixtures,'camera-targets.tsv'),['# file scene width time text64 words64StartEnd lineWidth primary accent eye look',...observations.map(o=>
  [o.file,o.scene,o.width,o.time,Buffer.from(o.text).toString('base64'),o.words.map(w=>[Buffer.from(w.text).toString('base64'),w.startTime,w.endTime].join(',')).join(';'),o.lineWidth,o.primary.join(','),o.accent.join(','),...vec(o.eye),...vec(o.look),o.shot,o.trackWidth].join('\t'))].join('\n')+'\n');
 console.log(JSON.stringify({frames:observations.length,renderer:observations[0].renderer}));
}finally{await browser?.close();await server.close();}
