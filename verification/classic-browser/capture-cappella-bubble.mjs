import fs from'node:fs';import path from'node:path';import{createHash}from'node:crypto';import{chromium}from'playwright';
import{checkReference,root,revision}from'./reference.mjs';checkReference();
const browser=await chromium.launch({headless:true}),out=path.join(root,'build/verification/cappella-bubble-browser');fs.mkdirSync(out,{recursive:true});
const rows=['# scene textWidth index width height'],frames=['# file scene textWidth side time'],opacity=['# scene time index actualCSSOpacity'],states={};
try{
 for(const[scene,textWidth]of[['latin',160],['wrap',160],['arabic',300],['cjk',160]])for(const side of['left','right']){
  const page=await browser.newPage({viewport:{width:480,height:300},deviceScaleFactor:1});const errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.goto(`http://127.0.0.1:4173/cappella-bubble.html?scene=${scene}&textWidth=${textWidth}&side=${side}`);await page.waitForFunction(()=>window.ready);
  for(const t of[.7,.85,1.3,1.6,2.8,3.5]){
   const s=await page.evaluate(t=>window.drawBubble(t),t),file=`${scene}-${textWidth}-${side}-${Math.round(t*1000)}.png`;
   await page.screenshot({path:path.join(out,file)});frames.push([file,scene,textWidth,side,t].join('\t'));states[file]=s;
   if(side==='left')for(const[i,a]of s.opacity.entries())opacity.push([scene,t,i,a].join('\t'));
   if(side==='left'&&t===.7)for(const[i,size]of s.sizes.entries())rows.push([scene,textWidth,i,size.width,size.height].join('\t'));
  }
  if(errors.length)throw Error(errors.join('\n'));await page.close();
 }
 const fixture=path.join(root,'verification/cappella-fixtures');fs.writeFileSync(path.join(fixture,'bubble-sizes.tsv'),rows.join('\n')+'\n');fs.writeFileSync(path.join(fixture,'bubble-frames.tsv'),frames.join('\n')+'\n');
 fs.writeFileSync(path.join(fixture,'bubble-opacity.tsv'),opacity.join('\n')+'\n');
 fs.writeFileSync(path.join(out,'states.json'),JSON.stringify(states,null,2)+'\n');
 fs.writeFileSync(path.join(fixture,'bubble-conditions.json'),JSON.stringify({revision,browser:browser.version(),frames:frames.length-1,viewport:[480,300],DPR:1,fontPx:24.12,
  scope:'Original AnimatedBubbleFrame/ActiveCappellaText + original pretext metrics/colors. Exact target frame, CSS fades explicitly re-anchored to media for comparison. Excludes stack, row scale/springs, resize transition, timestamp, shadows, glow and avatar. Not complete theme acceptance.',
  fonts:['FoliaParityLatin-Bold.ttf','NotoSansArabic-Bold.ttf','FoliaParityCJK-Bold.otf'].map(name=>({name,sha256:createHash('sha256').update(fs.readFileSync(path.join(root,'verification/assets',name))).digest('hex')}))},null,2)+'\n');
 console.log(JSON.stringify({frames:frames.length-1,sizes:rows.length-1}));
}finally{await browser.close()}
