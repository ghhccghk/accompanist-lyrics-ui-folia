import fs from'node:fs';import path from'node:path';import{createHash}from'node:crypto';import{chromium}from'playwright';import{root,revision,checkReference}from'./reference.mjs';checkReference();
const browser=await chromium.launch({headless:true}),out=path.join(root,'build/verification/pendolo-sweep-browser');fs.mkdirSync(out,{recursive:true});
const fixture=path.join(root,'verification/pendolo-fixtures'),b=s=>Buffer.from(s).toString('base64')||'-';
const inputs=['# C case scene width fontPx text64 start end; W case index text64 start end'],rows=['# L case row text64 width graphemeStart graphemeEnd offsetsCSV; T case glyph text64 start end; Q case time row opacity mask64'];
const states={};
try{
 for(const scene of['latin','wrap','arabic','cjk'])for(const width of[150,300]){
  const key=`${scene}-${width}`,page=await browser.newPage({viewport:{width:380,height:480},deviceScaleFactor:1}),errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.goto(`http://127.0.0.1:4173/pendolo-sweep.html?scene=${scene}&width=${width}`);await page.waitForFunction(()=>window.ready,null,{polling:100});
  const data=await page.evaluate(()=>({input:window.sweepInputs,layout:window.sweepLayout}));states[key]=data;
  const l=data.input.line;inputs.push(['C',key,scene,width,data.input.fontPx,b(l.fullText),l.startTime,l.endTime].join('\t'));
  l.words.forEach((w,i)=>inputs.push(['W',key,i,b(w.text),w.startTime,w.endTime].join('\t')));
  data.layout.lines.forEach((r,i)=>rows.push(['L',key,i,b(r.text),r.width,r.graphemeStart,r.graphemeEnd,r.offsets.join(',')].join('\t')));
  data.layout.timings.forEach((g,i)=>rows.push(['T',key,i,b(g.char),g.startTime,g.endTime].join('\t')));
  for(const t of[0,.7,1,1.3,1.6,2,2.4,2.8,3.2,3.6,4]){
   await page.evaluate(t=>window.setSweepTime(t),t);
   await page.evaluate(async()=>{await new Promise(requestAnimationFrame);await new Promise(requestAnimationFrame)});
   const values=await page.evaluate(()=>window.readSweep());values.forEach((v,i)=>rows.push(['Q',key,t,i,v.opacity,b(v.mask)].join('\t')));
   await page.screenshot({path:path.join(out,`${key}-${Math.round(t*1000)}.png`)});
  }
  if(errors.length)throw Error(errors.join('\n'));await page.close();
 }
 fs.writeFileSync(path.join(fixture,'sweep-inputs.tsv'),inputs.join('\n')+'\n');fs.writeFileSync(path.join(fixture,'sweep.tsv'),rows.join('\n')+'\n');
 fs.writeFileSync(path.join(out,'states.json'),JSON.stringify(states,null,2)+'\n');
 const fonts=['FoliaParityLatin-Bold.ttf','FoliaParityBidi-Bold.ttf','FoliaParityChatCJK-Bold.otf'].map(name=>({name,sha256:createHash('sha256').update(fs.readFileSync(path.join(root,'verification/assets',name))).digest('hex')}));
 const conditions={revision,browser:browser.version(),frames:Object.keys(states).length*11,viewport:[380,480],DPR:1,fontPx:28,fonts,
  scope:'Actual unchanged PendoloActiveLyricSweep component, no chorus/keyword override. Original layout, prefix offsets, grapheme timeline and actual MotionValue DOM mask/opacity at same media times; not complete rotating wheel or tick transitions.'};
 fs.writeFileSync(path.join(fixture,'sweep-conditions.json'),JSON.stringify(conditions,null,2)+'\n');console.log(JSON.stringify(conditions));
}finally{await browser.close()}
