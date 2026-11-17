import fs from'node:fs';import path from'node:path';import{createHash}from'node:crypto';import{chromium}from'playwright';import{checkReference,root,revision}from'./reference.mjs';
checkReference();const browser=await chromium.launch({headless:true});
const out=path.join(root,'build/verification/cadenza-public-browser');fs.mkdirSync(out,{recursive:true});
const frames=['# file\tscene\twidth\ttime'],sourceStates={};
try{
 for(const[scene,width]of[['latin',380],['wrap',380],['arabic',800],['cjk',800]]){
  const page=await browser.newPage({viewport:{width,height:600},deviceScaleFactor:1});const errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.goto(`http://127.0.0.1:4173/cadenza-mounted.html?scene=${scene}&glow=1`);
  await page.waitForFunction(()=>window.ready&&window.observedCadenza.size>0,null,{polling:100});
  const mount=await page.evaluate(()=>({count:window.cadenzaObservations.length,placements:window.prepared.placements.length}));
  if(mount.count!==mount.placements)throw Error(`Multiple original mount updates: ${scene}`);
  let probe=0;const probes=[700,750,850,1300,1600,2200,2800,3050];
  for(let index=0;probe<probes.length;index++){
   const ms=Math.round(700+index*1000/60);
   if(index>0)await page.evaluate(ms=>window.advanceAt(ms/1000,1000/60),ms);
   if(ms>=probes[probe]){const file=`${scene}-${width}-60-${probe++}.png`;
    sourceStates[file]={ms,states:await page.evaluate(()=>Object.fromEntries(window.observedCadenza))};
    await page.screenshot({path:path.join(out,file)});frames.push([file,scene,width,ms/1000].join('\t'));}
  }
  if(errors.length)throw Error(errors.join('\n'));await page.close();
 }
 fs.writeFileSync(path.join(root,'verification/cadenza-fixtures/publicMotionFrames.tsv'),frames.join('\n')+'\n');
 fs.writeFileSync(path.join(out,'source-frames.json'),JSON.stringify(sourceStates,null,2)+'\n');
 fs.writeFileSync(path.join(root,'verification/cadenza-fixtures/publicMotion-conditions.json'),JSON.stringify({revision,browser:browser.version(),height:600,DPR:1,energy:0,glow:1,
  mediaClock:'Same integer-millisecond snapshots on both sides; original wall RAF60Hz. No settled target convergence, geometry registration, phase fit or time offset.',
  fonts:['FoliaParityLatin-Bold.ttf','NotoSansArabic-Bold.ttf','FoliaParityCJK-Bold.otf'].map(name=>({name,sha256:createHash('sha256').update(fs.readFileSync(path.join(root,'verification/assets',name))).digest('hex')})),
  scope:'Actual original mounted Cadenza versus public FoliaLyricsView/Cadenza using published core model; synthetic same-font sceneInputs.tsv.'},null,2)+'\n');
 console.log(JSON.stringify({frames:frames.length-1,out}));
}finally{await browser.close()}
