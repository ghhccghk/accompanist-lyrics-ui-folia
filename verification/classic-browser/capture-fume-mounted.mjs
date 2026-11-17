import fs from'node:fs';import path from'node:path';
import{createFumeMountedServer}from'./fume-mounted-server.mjs';import{chromium}from'playwright';import{root,revision,referenceMotionVersions}from'./reference.mjs';
const server=await createFumeMountedServer();
let browser;try{
 await server.listen();browser=await chromium.launch({headless:true});const out=path.join(root,'build/verification/fume-mounted-browser');fs.mkdirSync(out,{recursive:true});
 const times=[0,.69,.7,.717,.75,.8,.9,1,1.15,1.3,2.6,3,3.15,4.2,5.4,5.6,6.3,7.6,8.3,9.5,10,10.7,11.5];
 const observations=[];
 for(const width of[380,800])for(const stepped of[false,true]){
  const page=await browser.newPage({viewport:{width,height:600},deviceScaleFactor:1}),errors=[];page.on('pageerror',e=>{errors.push(e.message);console.error(e.message)});
  await page.goto(`http://127.0.0.1:4175/fume-mounted.html?stepped=${stepped}&glow=1`);await page.waitForFunction(()=>window.ready,null,{timeout:8000});
  await page.screenshot(); // Deliver an actual browser layout/ResizeObserver turn.
  for(let i=0;i<120;i++){
   await page.evaluate(async()=>{window.advanceSourceFrame(0);await new Promise(r=>setTimeout(r,2))});
   if(errors.length)throw Error(errors.join('\n'));
   if(await page.evaluate(()=>!!window.sourceArticleReady))break;
  }
  if(!await page.evaluate(()=>!!window.sourceArticleReady))throw Error(JSON.stringify(await page.evaluate(()=>({html:document.querySelector('#root').innerHTML,rafs:window.testRafs.size,resizes:window.sourceResizes,rects:[...document.querySelectorAll('div')].map(e=>({class:e.className,rect:e.getBoundingClientRect().toJSON(),position:getComputedStyle(e).position}))}))));
  const steps=[...new Set([...times,...Array.from({length:691},(_,i)=>i/60)])].sort((a,b)=>a-b);
  let last=-1;
  for(const t of times){
   for(const step of steps.filter(x=>x>last&&x<=t))await page.evaluate(t=>{window.testNow=t*1000;window.advanceMedia(t);window.advanceSourceFrame(0);window.driveNativeAnimations()},step);
   last=t;const state=await page.evaluate(()=>window.readFume());const file=`${width}-1-${stepped}-${Math.round(t*1000)}.png`;
   observations.push({file,time:t,width,stepped,...state});await page.screenshot({path:path.join(out,file)});
  }
  if(errors.length)throw Error(errors.join('\n'));await page.close();
 }
 const fixtures=path.join(root,'verification/fume-fixtures');fs.writeFileSync(path.join(fixtures,'mounted-observations.json'),JSON.stringify({revision,motion:referenceMotionVersions(),browser:browser.version(),observations,
  scope:'actual unmodified VisualizerFume/useFumeCanvasStage, real source layout/RAF/Motion. BackgroundOpacity0; no-subtitle/shell/setting/loading-icon isolation. Original native animation startTime driven as existing Cappella harness; media origin0. No source formula rewrite.'},null,2)+'\n');
 console.log(JSON.stringify({frames:observations.length,queuedRafsReleased:'browser closed'}));
 fs.writeFileSync(path.join(fixtures,'mounted-frames.tsv'),['# file width stepped millis cameraX cameraY cameraScale parentScale',
  ...observations.map(o=>[o.file,o.width,o.stepped,Math.round(o.time*1000),o.camera.x,o.camera.y,o.camera.scale,o.parentScale].join('\t'))].join('\n')+'\n');
}finally{await browser?.close();await server.close();}
