import fs from 'node:fs';import path from 'node:path';import{createHash}from'node:crypto';
import{chromium}from'playwright';import{checkReference,root,revision}from'./reference.mjs';
checkReference();const browser=await chromium.launch({headless:true});
const out=path.join(root,'build/verification/cadenza-motion-browser'),fixtures=path.join(root,'verification/cadenza-fixtures');
fs.mkdirSync(out,{recursive:true});
const inputs=['# case\tscene\twidth\tcadence'],placements=['# case\tid64\twordIndex\ttext64\tstartInWord\tendInWord\tgraphemeCount\tx,y,width,height,rotation,scale,passedRotation,passedDriftX,passedDriftY,entryX,entryY,emphasis'],
 states=['# case\ttime\tid64\tx,y,rotation,scale,bodyAlpha,blur,activeMix,glowAlpha'],frames=['# file\tscene\twidth\ttime'];
const fields=['x','y','rotation','scale','bodyAlpha','blur','activeMix','glowAlpha'];
const b64=s=>Buffer.from(s).toString('base64');
try{
 for(const[scene,width]of[['latin',380],['wrap',380],['arabic',800],['cjk',800]])for(const cadence of['30','60','144','irregular']){
  const key=`${scene}-${width}-${cadence}`,page=await browser.newPage({viewport:{width,height:600},deviceScaleFactor:1});
  const errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.goto(`http://127.0.0.1:4173/cadenza-mounted.html?scene=${scene}&glow=1`);
  await page.waitForFunction(()=>window.ready&&window.observedCadenza.size>0,null,{polling:100});
  const initial=await page.evaluate(()=>({prepared:window.prepared,observations:window.cadenzaObservations,state:Object.fromEntries(window.observedCadenza)}));
  if(initial.observations.length!==initial.prepared.placements.length||initial.observations.some(o=>o.now!==0))throw Error(`Unexpected multiple mount draws: ${key}`);
  inputs.push([key,scene,width,cadence].join('\t'));
  for(const p of initial.prepared.placements)placements.push([key,b64(p.id),p.wordIndex,b64(p.text),p.fragmentStartInWord,p.fragmentEndInWord,p.wordGraphemeCount,
   [p.x,p.y,p.width,p.height,p.rotate,p.scale,p.passedRotate,p.passedDriftX,p.passedDriftY,p.entryOffsetX,p.entryOffsetY,p.emphasis].join(',')].join('\t'));
  let time=.7,index=0,probe=0;const probes=[.7,.75,.85,1.3,1.6,2.2,2.8,3.05];
  async function record(state){
   for(const[id,s]of Object.entries(state))states.push([key,time,b64(id),fields.map(f=>s[f]).join(',')].join('\t'));
   if(cadence==='60'&&probe<probes.length&&time>=probes[probe]-1e-9){
    const file=`${key}-${probe}.png`;await page.screenshot({path:path.join(out,file)});frames.push([file,scene,width,time].join('\t'));probe++;
   }
  }
  await record(initial.state);
  while(time<3.15){const dt=cadence==='irregular'?[1/90,1/35,1/57,1/120,1/28][index++%5]:1/Number(cadence);time=Math.min(time+dt,3.15);
   await record(await page.evaluate(({t,dt})=>window.advanceAt(t,dt*1000),{t:time,dt}));}
  if(errors.length)throw Error(errors.join('\n'));await page.close();console.log(key);
 }
 for(const[name,rows]of Object.entries({motionInputs:inputs,motionPlacements:placements,motionStates:states,motionFrames:frames}))fs.writeFileSync(path.join(fixtures,`${name}.tsv`),rows.join('\n')+'\n');
 const conditions={revision,browser:browser.version(),height:600,DPR:1,energy:0,glow:1,start:.7,end:3.15,cadences:['30','60','144','irregular'],
  observer:'Map.set copies original animated state after untouched RAF update; no target convergence, phase fit or source-state mutation.',
  fonts:['FoliaParityLatin-Bold.ttf','NotoSansArabic-Bold.ttf','FoliaParityCJK-Bold.otf'].map(name=>({name,sha256:createHash('sha256').update(fs.readFileSync(path.join(root,'verification/assets',name))).digest('hex')}))};
 fs.writeFileSync(path.join(fixtures,'motion-conditions.json'),JSON.stringify(conditions,null,2)+'\n');
 console.log(JSON.stringify({observations:states.length-1,screenshots:frames.length-1,out}));
}finally{await browser.close()}
