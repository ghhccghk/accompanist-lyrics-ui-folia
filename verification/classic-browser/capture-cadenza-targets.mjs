import fs from'node:fs';import path from'node:path';import{createHash}from'node:crypto';
import{chromium}from'playwright';import{checkReference,root,revision}from'./reference.mjs';
checkReference();const browser=await chromium.launch({headless:true});
try{
 const out=path.join(root,'build/verification/cadenza-target-browser');fs.mkdirSync(out,{recursive:true});
 const times=[.7,.75,.85,1.3,1.6,2.2,2.8,3.05],metadata={};
 for(const scene of['latin','wrap','arabic','cjk'])for(const width of[380,800])for(const glow of[0,1]){
  const page=await browser.newPage({viewport:{width,height:600},deviceScaleFactor:1});const errors=[];page.on('pageerror',e=>{errors.push(e.message);console.error(e.message)});
  page.on('response',r=>{if(r.status()>=400)console.error(`HTTP ${r.status()}: ${r.url()}`)});
  await page.goto(`http://127.0.0.1:4173/cadenza-mounted.html?scene=${scene}&glow=${glow}`);await page.waitForFunction(()=>window.ready,null,{polling:100});
  // RAF is explicitly test-controlled, so readiness checks must use timers.
  await page.waitForFunction(()=>[...document.querySelectorAll('canvas')].some(c=>c.width===innerWidth),null,{polling:100});
  const key=`${scene}-${width}-${glow}`;
  metadata[key]={prepared:await page.evaluate(()=>{const p=window.prepared;return{font:p.font,fontPx:p.fontPx,lineHeight:p.lineHeight,maxWidth:p.maxWidth,placements:p.placements}}),frames:[]};
  for(const time of times){const state=await page.evaluate(t=>window.settleAt(t),time);metadata[key].frames.push({time,state});
   await page.screenshot({path:path.join(out,`${key}-${Math.round(time*1000)}.png`)});}
  if(errors.length)throw Error(errors.join('\n'));await page.close();
 }
 fs.writeFileSync(path.join(out,'source-states.json'),JSON.stringify(metadata,null,2)+'\n');
 const geometry=[`# Folia ${revision}; scene\twidth\tid64\tx,y,width,height,scale; actual independent browser preparation`];
 for(const scene of['latin','wrap','arabic','cjk'])for(const width of[380,800])
  for(const p of metadata[`${scene}-${width}-0`].prepared.placements)
   geometry.push([scene,width,Buffer.from(p.id).toString('base64'),[p.x,p.y,p.width,p.height,p.scale].join(',')].join('\t'));
 fs.writeFileSync(path.join(root,'verification/cadenza-fixtures/targetGeometry.tsv'),geometry.join('\n')+'\n');
 const conditions={revision,browser:browser.version(),scenes:['latin','wrap','arabic','cjk'],widths:[380,800],height:600,DPR:1,glow:[0,1],times,
  fonts:['FoliaParityLatin-Bold.ttf','NotoSansArabic-Bold.ttf','FoliaParityCJK-Bold.otf'].map(name=>({name,sha256:createHash('sha256').update(fs.readFileSync(path.join(root,'verification/assets',name))).digest('hex')})),
  driver:'Actual untouched mounted Cadenza. Fixed media time;210 original RAF iterations converge to target. NOT transition/pause acceptance.',scope:'Original DOM text and Canvas; shell/background/subtitle omitted; energy0.'};
 fs.writeFileSync(path.join(out,'conditions.json'),JSON.stringify(conditions,null,2)+'\n');console.log(JSON.stringify({frames:128,out}));
}finally{await browser.close()}
