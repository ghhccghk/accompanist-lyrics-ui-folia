import fs from'node:fs';import path from'node:path';import{execFileSync}from'node:child_process';import{chromium}from'playwright';
import{checkReference,root,revision}from'./reference.mjs';checkReference();
const coverage=JSON.parse(execFileSync('python',[path.join(root,'verification/check-cappella-fonts.py')],{encoding:'utf8'}));
const referenceUrl=process.env.FOLIA_REFERENCE_URL??'http://127.0.0.1:4173';
const browser=await chromium.launch({headless:true}),out=path.join(root,'build/verification/cappella-playback-browser');fs.mkdirSync(out,{recursive:true});
const states={},frames=['# file scene width height intensity time current'],rows=['# file row id sourceCurrent bubbleRect avatarRect opacity'];
const probes=[0,.7,.72,.8,.86,1,1.6,2.8,3,3.18,3.2,3.4,4.7,4.72,4.8,4.9,5.1,6,6.8,7.18,7.3,8.7,8.72,8.8,9,9.1,9.5,10.8,11.18,11.3,12.7,12.75,12.9,13.1,14];
try{
 for(const[scene,width,height,intensity]of[['latin',380,480,'calm'],['latin',380,480,'normal'],['latin',380,480,'chaotic'],['wrap',800,600,'normal'],['arabic',800,600,'normal'],['cjk',1200,600,'normal']]){
  const page=await browser.newPage({viewport:{width,height},deviceScaleFactor:1}),errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.goto(`${referenceUrl}/cappella-playback.html?scene=${scene}&intensity=${intensity}`);await page.waitForFunction(()=>window.ready);
  await page.evaluate(()=>window.mountStage(0));
  // Initialize the initially visible title. Later children mount normally.
  await page.evaluate(()=>{for(let i=0;i<90;i++)window.advanceSourceFrame()});
  const base=await page.evaluate(()=>window.testNow),times=[...new Set([...probes,...Array.from({length:841},(_,i)=>i/60)])].sort((a,b)=>a-b);
  let last=0;
  for(const t of probes){
   const steps=times.filter(x=>x>=last&&x<=t&&(x>last||t===0));
   // A real task boundary per original RAF is required: Motion's time.now cache
   // is cleared in a microtask. Synchronous multi-frame batches otherwise give
   // newly mounted rows an older start timestamp and invalidate the oracle.
   for(const step of steps)await page.evaluate(({t,base})=>{window.testNow=base+t*1000;window.advanceStage(t);window.advanceSourceFrame(0);window.driveNativeAnimations()},{t:step,base});last=t;
   const phase=await page.evaluate(t=>window.anchorStage(t),t),geometry=await page.evaluate(()=>window.readStage());
   const file=`${scene}-${width}-${height}-${intensity}-${Math.round(t*1000)}.png`,current=geometry.rows.find(r=>r.lineIndex!==null)?.current??-1;
   states[file]={time:t,phase,geometry};frames.push([file,scene,width,height,intensity,t,current].join('\t'));
   for(const[i,r]of geometry.rows.entries())rows.push([file,i,r.id,r.current,r.bubble.join(','),r.avatar.join(','),+r.rowOpacity*+r.opacity].join('\t'));
   await page.screenshot({path:path.join(out,file)});
  }
  if(errors.length)throw Error(errors.join('\n'));await page.close();
 }
 const fixtures=path.join(root,'verification/cappella-fixtures');
 fs.writeFileSync(path.join(fixtures,'playback-frames.tsv'),frames.join('\n')+'\n');fs.writeFileSync(path.join(fixtures,'playback-observations.tsv'),rows.join('\n')+'\n');
 fs.writeFileSync(path.join(out,'states.json'),JSON.stringify(states,null,2)+'\n');
 fs.writeFileSync(path.join(out,'conditions.json'),JSON.stringify({revision,browser:browser.version(),frames:Object.keys(states).length,coverage,DPR:1,
  scope:'Actual original complete JSX stays mounted from media0 through14s. Original RAF queue at60Hz plus exact probes,one task per tick. Media MotionValue updates each tick; React host props update only on changed selection,matching source usePlaybackStore separation. Original row/size/spring/layout/timestamp Motion runs. Native WAAPI uses original startTime on media clock,original onfinish delivered once;CSS transition phases use creation time. Glyph fade/sweep explicitly media-anchored. No original assets. Passive React fiber props identify popLayout exits without modifying source. Forced parent-every-RAF diagnostics preserved separately.'},null,2)+'\n');
 console.log(JSON.stringify({frames:Object.keys(states).length,rows:rows.length-1}));
}finally{await browser.close()}
