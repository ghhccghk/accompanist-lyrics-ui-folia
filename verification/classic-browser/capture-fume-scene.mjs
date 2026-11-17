import fs from 'node:fs';import path from 'node:path';import {createHash}from'node:crypto';
import {chromium}from'playwright';import {checkReference,root,revision}from'./reference.mjs';
checkReference();const browser=await chromium.launch({headless:true});
try{
 const out=path.join(root,'build/verification/fume-scene-browser');fs.mkdirSync(out,{recursive:true});
 const samples=[`# Folia ${revision}; actual article/camera/text; original60Hz; no background engine or subtitle`, '# width\tglow\tstepped\ttime\tx\ty\tscale\trestore'];
 const times=[0,.7,.75,1,1.3,2.6,3,3.15,4.2,5.4,5.6,6.3,7.6,8.3,9.5,10,10.7,11.5];
 for(const width of[380,800,801])for(const glow of[0,1])for(const stepped of[false,true]){
  const page=await browser.newPage({viewport:{width,height:600},deviceScaleFactor:1});
  await page.goto(`http://127.0.0.1:4173/fume-scene.html?glow=${glow}&stepped=${stepped}`);await page.waitForFunction(()=>window.ready);
  fs.writeFileSync(path.join(out,`${width}-${glow}-${stepped}.json`),JSON.stringify(await page.evaluate(()=>window.fumeScene),null,2));
  for(const t of times){const p=await page.evaluate(t=>window.drawAt(t),t);samples.push([width,glow,stepped,t,p.camera.x,p.camera.y,p.camera.scale,p.restore].join('\t'));await page.screenshot({path:path.join(out,`${width}-${glow}-${stepped}-${Math.round(t*1000)}.png`)});}
  await page.close();
 }
 fs.writeFileSync(path.join(root,'verification/fume-fixtures/scene-camera.tsv'),samples.join('\n')+'\n');
 fs.writeFileSync(path.join(out,'conditions.json'),JSON.stringify({reference:revision,browser:browser.version(),times,widths:[380,800,801],glow:[0,1],stepped:[false,true],DPR:1,fontSha256:createHash('sha256').update(fs.readFileSync(path.join(root,'verification/assets/FoliaParityLatin-Bold.ttf'))).digest('hex'),camera:'actual original operator60Hz, fixed media-index epochs, float origin9.25',scope:'actual article and text; fixed plain background; no background engine or subtitle'},null,2));
 console.log(JSON.stringify({frames:216,out}));
}finally{await browser.close();}
