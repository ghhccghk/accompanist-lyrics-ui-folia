import fs from'node:fs';import path from'node:path';import{chromium}from'playwright';
import{checkReference,root,revision}from'./reference.mjs';checkReference();
const browser=await chromium.launch({headless:true}),out=path.join(root,'build/verification/cappella-presence-browser');fs.mkdirSync(out,{recursive:true});
const states={},frames=['# file timeline intensity time'],rows=['# file row id sourceCurrent bubbleRect avatarRect opacity'],inputs=['# timeline index text64 start end words64~start~end'];
try{
 for(const timeline of['rapid','overlap'])for(const intensity of['calm','normal','chaotic']){
  const page=await browser.newPage({viewport:{width:380,height:480},deviceScaleFactor:1}),errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.goto(`${process.env.FOLIA_REFERENCE_URL??'http://127.0.0.1:4173'}/cappella-playback.html?scene=latin&timeline=${timeline}&intensity=${intensity}`);await page.waitForFunction(()=>window.ready);
  const lines=await page.evaluate(()=>window.stageInputs),encode=s=>Buffer.from(s).toString('base64');
  if(intensity==='calm')for(const[i,l]of lines.entries())inputs.push([timeline,i,encode(l.fullText),l.startTime,l.endTime,l.words.map(w=>[encode(w.text),w.startTime,w.endTime].join('~')).join('|')].join('\t'));
  const probes=[...new Set([0,.7,.72,.78,.8,.847,.86,.9,.94,1,1.02,1.1,1.2,1.3,1.5,1.8,2,2.2,2.5,3,3.5,4,
   ...lines.flatMap(l=>[l.startTime,l.renderHints.renderEndTime,l.renderHints.renderEndTime+.01])])].sort((a,b)=>a-b);
  await page.evaluate(()=>window.mountStage(0));await page.evaluate(()=>{for(let i=0;i<90;i++)window.advanceSourceFrame()});
  const base=await page.evaluate(()=>window.testNow),ticks=[...new Set([...probes,...Array.from({length:241},(_,i)=>i/60)])].sort((a,b)=>a-b);let last=-1;
  for(const[probeIndex,t]of probes.entries()){
   for(const step of ticks.filter(x=>x>last&&x<=t))await page.evaluate(({t,base})=>{window.testNow=base+t*1000;window.advanceStage(t);window.advanceSourceFrame(0);window.driveNativeAnimations()},{t:step,base});last=t;
   const phase=await page.evaluate(t=>window.anchorStage(t),t),geometry=await page.evaluate(()=>window.readStage());
   const file=`${timeline}-${intensity}-${probeIndex}-${Math.round(t*1e6)}.png`;states[file]={time:t,phase,geometry};frames.push([file,timeline,intensity,t].join('\t'));
   for(const[i,r]of geometry.rows.entries())rows.push([file,i,r.id,r.current,r.bubble.join(','),r.avatar.join(','),+r.rowOpacity*+r.opacity].join('\t'));
   await page.screenshot({path:path.join(out,file)});
  }
  if(errors.length)throw Error(errors.join('\n'));await page.close();
 }
 const fixtures=path.join(root,'verification/cappella-fixtures');for(const[name,data]of[['presence-frames',frames],['presence-observations',rows],['presence-inputs',inputs]])fs.writeFileSync(path.join(fixtures,name+'.tsv'),data.join('\n')+'\n');
 fs.writeFileSync(path.join(out,'states.json'),JSON.stringify(states,null,2)+'\n');fs.writeFileSync(path.join(out,'conditions.json'),JSON.stringify({revision,browser:browser.version(),frames:Object.keys(states).length,DPR:1,font:'FoliaParityLatin-Bold.ttf',viewport:[380,480],scope:'Same controlled original mounted-stage clocks as cappella-playback; rapid micro-lines and overlapping lines, exact starts/inclusive ends and post-end probes.'},null,2)+'\n');
 console.log(JSON.stringify({frames:Object.keys(states).length,rows:rows.length-1}));
}finally{await browser.close()}
