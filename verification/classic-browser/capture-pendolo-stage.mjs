import fs from'node:fs';import path from'node:path';import{chromium}from'playwright';import{root,revision,checkReference}from'./reference.mjs';checkReference();
const browser=await chromium.launch({headless:true}),out=path.join(root,'build/verification/pendolo-stage-browser');fs.mkdirSync(out,{recursive:true});
const states={},inputs={},b=s=>Buffer.from(s).toString('base64')||'-',rows=['# C file time target mode; R file index sourceLineIndex x y angle scale alpha reservedHeight computedContentHeight contentRect subtitles computedWidth'];
try{
 for(const scene of['latin','wrap','arabic','cjk'])for(const width of[380,800]){
  const page=await browser.newPage({viewport:{width,height:600},deviceScaleFactor:1}),errors=[];page.on('pageerror',e=>errors.push(e.message));
  page.on('response',r=>{if(r.status()>=400)errors.push(`${r.status()} ${r.url()}`)});
  await page.goto(`http://127.0.0.1:4173/pendolo-stage.html?scene=${scene}`);await page.waitForFunction(()=>window.ready,null,{polling:100});
  inputs[scene]=await page.evaluate(()=>window.wheelInputs);
  for(const mode of['none','both'])for(const[time,target]of[[0,-1],[1.6,0],[6,1],[7,1.5],[33,8]]){
   const key=`${scene}-${width}-${mode}-${Math.round(time*1000)}`;
   await page.evaluate(({time,target,mode})=>window.mountWheel(time,target,mode),{time,target,mode});
   await page.evaluate(async()=>{await new Promise(requestAnimationFrame);await new Promise(requestAnimationFrame)});
   const state=await page.evaluate(()=>window.readWheel());states[key]={time,target,mode,...state};rows.push(['C',key,time,target,mode].join('\t'));
   state.rows.forEach((r,i)=>{const item=state.items[i];rows.push(['R',key,i,item.index,item.x,item.y,item.angleDeg,item.scale,item.alpha,state.heights[item.index],r.computedHeight,r.content.join(','),b(JSON.stringify(r.subtitles)),r.computedWidth].join('\t'))});
   await page.screenshot({path:path.join(out,key+'.png')});
  }
  if(scene==='arabic'&&width===380){
   const tracking=await page.evaluate(()=>{
    const text=window.wheelInputs.lines[0].fullText;
    return[0,.22].map(spacing=>{
     const e=document.createElement('div');Object.assign(e.style,{position:'absolute',fontFamily:'Pendolo Arabic',fontSize:'22px',fontWeight:'700',letterSpacing:`${spacing}px`,whiteSpace:'pre',width:'max-content'});
     e.textContent=text;document.body.append(e);const box=e.getBoundingClientRect();
     const words=[...text.matchAll(/[^ ]+| /g)].map(m=>{const r=document.createRange();r.setStart(e.firstChild,m.index);r.setEnd(e.firstChild,m.index+m[0].length);const b=r.getBoundingClientRect();return[b.x-box.x,b.width]});
     e.remove();return{spacing,text,width:box.width,words};
    });
   });
   fs.writeFileSync(path.join(root,'verification/pendolo-fixtures/stage-arabic-space-tracking.json'),JSON.stringify(tracking,null,2)+'\n');
  }
  if(errors.length)throw Error(errors.join('\n'));await page.close();
 }
 fs.writeFileSync(path.join(out,'states.json'),JSON.stringify(states,null,2)+'\n');
 fs.writeFileSync(path.join(root,'verification/pendolo-fixtures/stage-inputs.json'),JSON.stringify(inputs,null,2)+'\n');
 fs.writeFileSync(path.join(root,'verification/pendolo-fixtures/stage-targets.tsv'),rows.join('\n')+'\n');
 fs.writeFileSync(path.join(root,'verification/pendolo-fixtures/stage-conditions.json'),JSON.stringify({revision,browser:browser.version(),DPR:1,widths:[380,800],height:600,frames:Object.keys(states).length,
  scope:'Original text-wheel JSX subtree and actual source measurement/rotating-line/sweep/subtitle helpers. Target-only remounts, no audio/gears/chorus. Source declaration hashes in stage-reference.json; no tick/history/manual-scroll acceptance.'},null,2)+'\n');
 console.log(JSON.stringify({frames:Object.keys(states).length,rows:rows.filter(r=>r.startsWith('R\t')).length}));
}finally{await browser.close()}
