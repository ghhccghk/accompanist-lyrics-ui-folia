import fs from'node:fs';import path from'node:path';import{chromium}from'playwright';import{root,revision,checkReference}from'./reference.mjs';checkReference();
const browser=await chromium.launch({headless:true}),out=path.join(root,'build/verification/pendolo-chorus-stage-browser');fs.mkdirSync(out,{recursive:true});
const haloOut=path.join(root,'build/verification/pendolo-chorus-halo-browser');fs.mkdirSync(haloOut,{recursive:true});
const frames=['# file scene width intensity dense time target wheel'],rows=['# file index scale haloOpacity haloScale contentRect'],states={},inputs={};
const probes=[0,.7,.72,.75,.8,.9,1.1,2.8,2.8001,2.82,2.9,3.1,3.5,4.7,4.72,4.8,4.9,5.1,6,7,7.3,8.7,8.75,9];
try{
 for(const[scene,width,intensity,dense]of[['latin',380,'calm',false],['latin',380,'normal',false],['latin',380,'chaotic',false],['wrap',380,'normal',false],['arabic',800,'normal',false],['cjk',800,'normal',false],['latin',380,'normal',true]]){
  const page=await browser.newPage({viewport:{width,height:600},deviceScaleFactor:1}),errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.goto(`${process.env.FOLIA_REFERENCE_URL??'http://127.0.0.1:4173'}/pendolo-chorus-stage.html?scene=${scene}&intensity=${intensity}&dense=${dense?1:0}`);await page.waitForFunction(()=>window.ready,null,{polling:100});
  inputs[`${scene}-${width}-${intensity}-${dense}`]=await page.evaluate(()=>window.chorusInputs);
  for(let i=0;i<10;i++)await page.evaluate(()=>window.advanceSourceFrame());
  const base=await page.evaluate(()=>window.testNow),times=[...new Set([...probes,...Array.from({length:541},(_,i)=>i/60)])].sort((a,b)=>a-b);let last=-1;
  for(const t of probes){
   for(const time of times.filter(x=>x>last&&x<=t))await page.evaluate(({time,base})=>{window.testNow=base+time*1000;window.advanceWheel(time);window.advanceSourceFrame(0);window.driveNativeAnimations()},{time,base});last=t;
   const s=await page.evaluate(()=>window.readChorusWheel()),target=await page.evaluate(t=>window.advanceWheel(t).target,t);
   if(s.rows.some(r=>typeof r.scale!=='number'))throw Error('Chorus observer host not loaded');
   const file=`${scene}-${width}-${intensity}-${dense?'dense':'spaced'}-${Math.round(t*10000)}`;states[file]={t,target,...s};
   frames.push([file,scene,width,intensity,dense,t,target,s.rotation].join('\t'));
   for(const r of s.rows)rows.push([file,r.index,r.scale,r.halo?.opacity??0,r.halo?.scale??1,r.content.join(',')].join('\t'));
   await page.screenshot({path:path.join(out,file+'.png')});
   await page.evaluate(()=>{const wheel=document.querySelector('#pendolo-reference').firstElementChild;for(const row of wheel.children)row.firstElementChild.firstElementChild.firstElementChild.lastElementChild.style.opacity='0'});
   await page.screenshot({path:path.join(haloOut,file+'.png')});
   await page.evaluate(()=>{const wheel=document.querySelector('#pendolo-reference').firstElementChild;for(const row of wheel.children)row.firstElementChild.firstElementChild.firstElementChild.lastElementChild.style.opacity='1'});
  }
  if(errors.length)throw Error(errors.join('\n'));await page.close();
 }
 const dir=path.join(root,'verification/pendolo-fixtures');fs.writeFileSync(path.join(dir,'chorus-stage-frames.tsv'),frames.join('\n')+'\n');
 fs.writeFileSync(path.join(dir,'chorus-stage-rows.tsv'),rows.join('\n')+'\n');fs.writeFileSync(path.join(dir,'chorus-stage-inputs.json'),JSON.stringify(inputs,null,2)+'\n');fs.writeFileSync(path.join(out,'states.json'),JSON.stringify(states,null,2)+'\n');
 fs.writeFileSync(path.join(dir,'chorus-stage-conditions.json'),JSON.stringify({revision,browser:browser.version(),DPR:1,frames:frames.length-1,rows:rows.length-1,height:600,
  scope:'Actual original useSpring/angle declarations and text-wheel JSX remain mounted. Controlled performance/RAF60Hz plus exact probes,one browser task per frame; original WAAPI/CSS transitions media-anchored at their start/creation times. Source target selection only rerenders on changes. Smart-beat zero; explicit alternating chorus and keyword palette; no audio/gears. Native uses semantic media history and exact derivative; differences are reported, not accepted as parity.'},null,2)+'\n');
 console.log(JSON.stringify({frames:frames.length-1,rows:rows.length-1}));
}finally{await browser.close()}
