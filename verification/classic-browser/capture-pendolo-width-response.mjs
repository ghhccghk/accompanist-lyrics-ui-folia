// Isolated browser CSS behavior used by the original Pendolo idle main DIV.
// This complements (does not replace) the actual original JSX image matrix.
import fs from'node:fs';import path from'node:path';import{chromium}from'playwright';import{root,revision,checkReference}from'./reference.mjs';checkReference();
const browser=await chromium.launch({headless:true}),rows=['# E scenario time target; Q scenario time maxWidth durationSeconds'];
const inputs={settled:[[0,190.123456],[.3,211.9],[.7,140.23]],interrupt:[[0,200],[.3,100],[.35,170],[.39,110],[.43,190]],
 reverse:[[0,200],[.3,100],[.35,200],[.38,100],[.41,200]],nochange:[[0,200],[.3,100],[.35,100],[.38,100],[.41,100]]};
try{
 for(const[name,events]of Object.entries(inputs)){
  const page=await browser.newPage();await page.goto(`${process.env.FOLIA_REFERENCE_URL??'http://127.0.0.1:4173'}/pendolo-tick-stage.html`);await page.waitForFunction(()=>window.ready,null,{polling:100});
  await page.evaluate(initial=>{window.testNow=0;const e=document.createElement('div');e.id='width-probe';e.className='transition-all duration-200';e.style.maxWidth=`${initial}px`;document.body.append(e);getComputedStyle(e).maxWidth},events[0][1]);
  for(const[t,target]of events)rows.push(['E',name,t,target].join('\t'));
  const times=[...new Set([0,.01,.2,.25,.3,.31,.34,.35,.36,.37,.38,.39,.4,.41,.42,.43,.44,.45,.46,.47,.49,.5,.55,.6,.65,.7,.72,.75,.8,1,2,...events.map(e=>e[0])])].sort((a,b)=>a-b);
  for(const time of times){const event=events.find(e=>e[0]===time);
   const s=await page.evaluate(({time,target})=>{window.testNow=time*1000;window.driveNativeAnimations();const e=document.querySelector('#width-probe');if(target!==null)e.style.maxWidth=`${target}px`;window.driveNativeAnimations();
    const a=document.getAnimations().find(a=>a.effect.target===e);return{width:parseFloat(getComputedStyle(e).maxWidth),duration:a?a.effect.getTiming().duration/1000:0};},{time,target:event?.[1]??null});
   rows.push(['Q',name,time,s.width,s.duration].join('\t'));
  }await page.close();
 }
 fs.writeFileSync(path.join(root,'verification/pendolo-fixtures/width-response.tsv'),rows.join('\n')+'\n');
 fs.writeFileSync(path.join(root,'verification/pendolo-fixtures/width-response-reference.json'),JSON.stringify({revision,browser:browser.version(),inputs,
  style:'Original idle DIV transition-all duration-200 + locked Tailwind4 default cubic-bezier(.4,0,.2,1)',
  scope:'Isolated actual Blink CSS max-width interpolation/interruption/reversal. Original complete subtree remains separately tested in tick-stage. Animations driven at controlled media style-change times, not source frames.'},null,2)+'\n');
 console.log(JSON.stringify({rows:rows.length-1}));
}finally{await browser.close()}
