import fs from'node:fs';import path from'node:path';import{chromium}from'playwright';
import{checkReference,root,revision,referenceModules}from'./reference.mjs';
checkReference();const browser=await chromium.launch({headless:true});
const out=path.join(root,'build/verification/cadenza-accents-browser'),fixtures=path.join(root,'verification/cadenza-fixtures');fs.mkdirSync(out,{recursive:true});
const probes=['# scene\twidth\ttime\tindex\tx,y,radius,lineWidth\trawStroke64'];
const {server,load}=await referenceModules();
try{
 const {resolveWordColor}=await load('components/visualizer/wordColoring.ts');
 const colors=[{word:'Amber amber',color:'#5bcefa'},{word:'echoes echo',color:'#f5a9b8'},{word:'向远方',color:'#5bcefa'},{word:'星光 回响',color:'#f5a9b8'}];
 const colorCases=['Amber ','AMBER!','echoes ','echo','beyond','向远方 ','星','光','星光 ','回响','مرحبا','écho','AM_BER','\ufeffAmber\ufeff','\u0085Amber','!!!',''];
 fs.writeFileSync(path.join(fixtures,'accentColors.tsv'),['# text64\texpected',...colorCases.map(t=>[Buffer.from(t).toString('base64'),resolveWordColor(t,colors,'#d6f456')].join('\t'))].join('\n')+'\n');
 for(const[scene,width]of[['latin',380],['wrap',380],['arabic',800],['cjk',800]]){
  const page=await browser.newPage({viewport:{width,height:600},deviceScaleFactor:1});const errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.addInitScript(()=>{
   window.cadenzaRings=[];const p=CanvasRenderingContext2D.prototype,clear=p.clearRect,arc=p.arc;
   const descriptor=Object.getOwnPropertyDescriptor(p,'strokeStyle');
   Object.defineProperty(p,'strokeStyle',{...descriptor,set(value){this.rawStroke=value;descriptor.set.call(this,value)}});
   p.clearRect=function(...args){if(this.canvas.width===innerWidth&&this.canvas.height===600)window.cadenzaRings=[];return clear.apply(this,args)};
   p.arc=function(...args){if(this.canvas.width===innerWidth&&this.canvas.height===600)window.cadenzaRings.push({x:args[0],y:args[1],radius:args[2],lineWidth:this.lineWidth,rawStroke:this.rawStroke});return arc.apply(this,args)};
  });
  await page.goto(`http://127.0.0.1:4173/cadenza-mounted.html?scene=${scene}&glow=1&chorus=1&palette=alternate`);
  await page.waitForFunction(()=>window.ready&&window.observedCadenza.size>0,null,{polling:100});
  for(const time of[.7,.75,.85,1.3,1.6,2.2,2.8,3.05]){
   const rings=await page.evaluate(t=>{window.settleAt(t);return window.cadenzaRings},time);
   rings.forEach((r,i)=>probes.push([scene,width,time,i,[r.x,r.y,r.radius,r.lineWidth].join(','),Buffer.from(r.rawStroke).toString('base64')].join('\t')));
   await page.screenshot({path:path.join(out,`${scene}-${width}-${Math.round(time*1000)}.png`)});
  }
  if(errors.length)throw Error(errors.join('\n'));await page.close();
 }
 fs.writeFileSync(path.join(fixtures,'accentRings.tsv'),probes.join('\n')+'\n');
 fs.writeFileSync(path.join(fixtures,'accent-conditions.json'),JSON.stringify({revision,browser:browser.version(),widths:[380,800],height:600,DPR:1,chorus:true,energy:0,glow:1,
  palette:{primary:'#8ab3df',accent:'#d6f456',colors},driver:'Actual original mounted component; settled targets. Observe original Canvas.arc arguments and raw strokeStyle assignment. Not transition acceptance.'},null,2)+'\n');
 console.log(JSON.stringify({frames:32,rings:probes.length-1,colorCases:colorCases.length}));
}finally{await browser.close();await server.close()}
