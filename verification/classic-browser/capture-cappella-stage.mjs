import fs from'node:fs';import path from'node:path';import{createHash}from'node:crypto';import{execFileSync}from'node:child_process';import{chromium}from'playwright';import{checkReference,root,revision}from'./reference.mjs';checkReference();
const coverage=JSON.parse(execFileSync('python',[path.join(root,'verification/check-cappella-fonts.py')],{encoding:'utf8'}));
const browser=await chromium.launch({headless:true}),out=path.join(root,'build/verification/cappella-stage-browser');fs.mkdirSync(out,{recursive:true});
const states={},rows=['# scene width intensity time current rowIndex rowRect avatarRect bubbleRect opacity scale'],advances=['# scene fontPx text64 originalCanvasAdvance'];
try{
 for(const[scene,width]of[['latin',380],['wrap',800],['arabic',800],['cjk',1200]])for(const intensity of['calm','normal','chaotic']){
  const page=await browser.newPage({viewport:{width,height:900},deviceScaleFactor:1}),errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.goto(`http://127.0.0.1:4173/cappella-stage.html?scene=${scene}&intensity=${intensity}`);await page.waitForFunction(()=>window.ready,null,{polling:100});
  if(intensity==='normal'){
   const texts=scene==='arabic'?['مرحبا','بالعالم','مرحبا بالعالم','لالا','للم']:scene==='cjk'?['向远方','星光 回响','流光','中英混排']:
    ['no-','no','amber','echo','no-no-no-no-no-no-no amber echo','Synthetic title','AVATAR','office ffi','To AV','WAVE','a\u0301'];
   const probes=await page.evaluate(({texts,scene})=>{const c=document.createElement('canvas').getContext('2d'),result=[];for(const size of[15,16.56,16.92,18,21.96,24.12,25.2,32])for(const text of texts){c.font=`700 ${size}px "Cappella ${scene==='arabic'?'Arabic':scene==='cjk'?'CJK':'Latin'}"`;result.push([size,text,c.measureText(text).width])}return result},{texts,scene});
   for(const[size,text,width]of probes)advances.push([scene,size,Buffer.from(text).toString('base64'),width].join('\t'));
  }
  for(const t of[0,1.6,6,14,27.5]){
   const current=await page.evaluate(t=>window.mountStage(t),t);
   for(let batch=0;batch<7;batch++)await page.evaluate(()=>{for(let i=0;i<30;i++)window.advanceSourceFrame()});
   const state=await page.evaluate(()=>window.readStage()),key=`${scene}-${width}-${intensity}-${Math.round(t*1000)}`;
   states[key]={time:t,current,...state};await page.screenshot({path:path.join(out,key+'.png')});
   for(const[i,r]of state.rows.entries())rows.push([scene,width,intensity,t,current,i,r.row.join(','),r.avatar.join(','),r.bubble.join(','),r.opacity,r.scale.replaceAll('\t',' ')||'none'].join('\t'));
  }
  if(errors.length)throw Error(errors.join('\n'));await page.close();
 }
 fs.writeFileSync(path.join(root,'verification/cappella-fixtures/stage-targets.tsv'),rows.join('\n')+'\n');
 fs.writeFileSync(path.join(root,'verification/cappella-fixtures/canvas-advances.tsv'),advances.join('\n')+'\n');
 fs.writeFileSync(path.join(root,'verification/cappella-fixtures/stage-conditions.json'),JSON.stringify({revision,browser:browser.version(),frames:Object.keys(states).length,height:900,DPR:1,
  scope:'Actual original complete Cappella component. Media fixed,210 original RAF steps converge row target geometry. Source color avatars, emoji disabled. No original assets copied. This is NOT dynamic row transition/resize/spring acceptance.',
  coverage,fonts:[...new Set(Object.values(coverage).map(v=>v.font))].map(name=>({name,sha256:createHash('sha256').update(fs.readFileSync(path.join(root,'verification/assets',name))).digest('hex')})),canvasAdvanceProbes:advances.length-1},null,2)+'\n');
 fs.writeFileSync(path.join(out,'states.json'),JSON.stringify(states,null,2)+'\n');console.log(JSON.stringify({frames:Object.keys(states).length,rows:rows.length-1}));
}finally{await browser.close()}
