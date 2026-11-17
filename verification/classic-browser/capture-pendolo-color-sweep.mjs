import fs from'node:fs';import path from'node:path';import{chromium}from'playwright';import{root,revision,checkReference}from'./reference.mjs';checkReference();
const browser=await chromium.launch({headless:true}),out=path.join(root,'build/verification/pendolo-color-sweep-browser');fs.mkdirSync(out,{recursive:true});
const fixture=path.join(root,'verification/pendolo-fixtures'),states={},rows=['# file scene width time'];
try{
 for(const scene of['latin','wrap','arabic','cjk'])for(const width of[150,300]){
  const page=await browser.newPage({viewport:{width:380,height:480},deviceScaleFactor:1}),errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.goto(`http://127.0.0.1:4173/pendolo-sweep.html?scene=${scene}&width=${width}&colors=1`);await page.waitForFunction(()=>window.ready,null,{polling:100});
  states[`${scene}-${width}`]=await page.evaluate(()=>window.sweepInputs);
  if(!states[`${scene}-${width}`].wordColors?.length)throw Error('Keyword host not loaded; restart the local no-HMR fixture server');
  for(const t of[0,.7,1,1.3,1.6,2,2.4,2.8,3.2,3.6,4]){
   await page.evaluate(t=>window.setSweepTime(t),t);await page.evaluate(async()=>{await new Promise(requestAnimationFrame);await new Promise(requestAnimationFrame)});
   const name=`${scene}-${width}-${Math.round(t*1000)}`;rows.push([name,scene,width,t].join('\t'));await page.screenshot({path:path.join(out,name+'.png')});
  }if(errors.length)throw Error(errors.join('\n'));await page.close();
 }
 fs.writeFileSync(path.join(fixture,'color-sweep-inputs.json'),JSON.stringify(states,null,2)+'\n');fs.writeFileSync(path.join(fixture,'color-sweep-frames.tsv'),rows.join('\n')+'\n');
 fs.writeFileSync(path.join(fixture,'color-sweep-conditions.json'),JSON.stringify({revision,browser:browser.version(),DPR:1,viewport:[380,480],frames:rows.length-1,
  scope:'Actual unchanged PendoloActiveLyricSweep with keyword ranges. Same existing covered fonts/700/28px/synthetic lyrics/150 and300px row widths as sweep baseline. Arabic keyword intentionally does not match source ASCII keyword matcher. No chorus/whole-wheel/device acceptance.'},null,2)+'\n');console.log(JSON.stringify({frames:rows.length-1}));
}finally{await browser.close()}
