import fs from'node:fs';import path from'node:path';import{chromium}from'playwright';
import{checkReference,root,revision}from'./reference.mjs';checkReference();
const browser=await chromium.launch({headless:true}),out=path.join(root,'build/verification/cappella-resize-browser');fs.mkdirSync(out,{recursive:true});
const rows=['# scene textWidth side time frameWidth frameHeight bubbleWidth bubbleHeight'],states={};
try{
 for(const[scene,width]of[['latin',160],['wrap',160]])for(const side of['left','right']){
  const page=await browser.newPage({viewport:{width:480,height:300},deviceScaleFactor:1}),errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.goto(`http://127.0.0.1:4173/cappella-resize.html?scene=${scene}&textWidth=${width}&side=${side}&motion=true`);await page.waitForFunction(()=>window.ready);
  const initial=await page.evaluate(()=>window.drawBubble(.7));
  for(let i=0;i<3;i++)await page.evaluate(()=>{for(let i=0;i<30;i++)window.advanceSourceFrame()});
  const base=await page.evaluate(()=>window.testNow);
  // Exact original reveal events plus dense independent read points. Wall-time
  // is set before updates, then the actual original RAF queue processes once.
  const probes=[.7,.71,.75,.8,.85,.9,1,1.1,1.2,1.3,1.4,1.5,1.6,1.75,1.9,2,2.1,2.2,2.3,2.5,2.8,3,3.5];
  const events=initial.reveal.map(t=>t-.2).filter(t=>t>.7&&t<=3.5);
  const frames=Array.from({length:169},(_,i)=>.7+i/60).filter(t=>t<=3.5);
  for(const t of[...new Set([...events,...probes,...frames])].sort((a,b)=>a-b)){
   await page.evaluate(({t,base})=>{window.testNow=base+(t-.7)*1000;window.drawBubble(t);window.advanceSourceFrame(0)},{t,base});
   if(!probes.includes(t))continue;
   const state=await page.evaluate(()=>window.readBubble()),key=`${scene}-${width}-${side}-${Math.round(t*1000)}`;
   rows.push([scene,width,side,t,state.frame[2],state.frame[3],state.bubble[2],state.bubble[3]].join('\t'));states[key]=state;
   await page.screenshot({path:path.join(out,key+'.png')});
  }
  if(errors.length)throw Error(errors.join('\n'));await page.close();
 }
 fs.writeFileSync(path.join(root,'verification/cappella-fixtures/resize-observations.tsv'),rows.join('\n')+'\n');
 fs.writeFileSync(path.join(out,'states.json'),JSON.stringify(states,null,2)+'\n');
 fs.writeFileSync(path.join(out,'conditions.json'),JSON.stringify({revision,browser:browser.version(),viewport:[480,300],DPR:1,frames:rows.length-1,
  scope:'Original stable-key AnimatedBubbleFrame/ActiveCappellaText. Source .2s lookahead+actual width/height animation; fixed font24.12/bold Latin. Exact target events plus60Hz read steps, original RAF queue. CSS glyph fades explicitly media-anchored. Excludes row springs,group scale,shadow,avatar/timestamp.'},null,2)+'\n');
 console.log(JSON.stringify({frames:rows.length-1}));
}finally{await browser.close()}
