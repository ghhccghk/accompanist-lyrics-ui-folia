import fs from'node:fs';import path from'node:path';import{execFileSync}from'node:child_process';import{chromium}from'playwright';
import{checkReference,root,revision}from'./reference.mjs';checkReference();
const coverage=JSON.parse(execFileSync('python',[path.join(root,'verification/check-cappella-fonts.py')],{encoding:'utf8'}));
const browser=await chromium.launch({headless:true}),out=path.join(root,'build/verification/cappella-stage-visual-browser');fs.mkdirSync(out,{recursive:true});
const states={};
try{
 for(const[scene,width]of[['latin',380],['wrap',800],['arabic',800],['cjk',1200]])for(const intensity of['calm','normal','chaotic']){
  const page=await browser.newPage({viewport:{width,height:900},deviceScaleFactor:1}),errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.goto(`http://127.0.0.1:4173/cappella-stage.html?scene=${scene}&intensity=${intensity}`);await page.waitForFunction(()=>window.ready);
  for(const t of[0,1.6,6,14,27.5]){
   await page.evaluate(t=>window.mountStage(t),t);
   for(let batch=0;batch<7;batch++)await page.evaluate(()=>{for(let i=0;i<30;i++)window.advanceSourceFrame()});
   const phase=await page.evaluate(t=>window.anchorStage(t),t),key=`${scene}-${width}-${intensity}-${Math.round(t*1000)}`;
   states[key]={time:t,phase,geometry:await page.evaluate(()=>window.readStage())};
   await page.screenshot({path:path.join(out,key+'.png')});
  }
  if(errors.length)throw Error(errors.join('\n'));await page.close();
 }
 fs.writeFileSync(path.join(out,'states.json'),JSON.stringify(states,null,2)+'\n');
 fs.writeFileSync(path.join(out,'conditions.json'),JSON.stringify({revision,browser:browser.version(),coverage,frames:Object.keys(states).length,
  viewportHeight:900,DPR:1,scope:'Actual unchanged original whole Cappella JSX, text-only/color avatars. Fixed media,210 RAF convergence for geometry, CSS character fades/sweep explicitly media-anchored via WAAPI. Not dynamic row/size/spring/timestamp response acceptance.'},null,2)+'\n');
 console.log(JSON.stringify({frames:Object.keys(states).length}));
}finally{await browser.close()}
