import fs from'node:fs';import path from'node:path';import{fileURLToPath}from'node:url';import{createRequire}from'node:module';import{createHash}from'node:crypto';
import{createServer}from'vite';import{chromium}from'playwright';import{root,revision,checkReference}from'./reference.mjs';
checkReference();const require=createRequire(import.meta.url);const here=path.dirname(fileURLToPath(import.meta.url));
const server=await createServer({configFile:false,root:here,resolve:{alias:{three:path.join(path.dirname(require.resolve('three')),'three.module.js')}},
 optimizeDeps:{noDiscovery:true,entries:[]},cacheDir:path.join(root,'.work/diorama-text-vite'),server:{host:'127.0.0.1',port:4174,strictPort:true,hmr:false,fs:{allow:[root]}}});
let browser;try{
 await server.listen();browser=await chromium.launch({headless:true,args:['--use-angle=swiftshader','--enable-unsafe-swiftshader']});
 const out=path.join(root,'build/verification/diorama-text-plane-browser');fs.mkdirSync(out,{recursive:true});const observations=[];
 for(const scene of['latin','arabic','cjk','long'])for(const width of[380,800,801]){
  const page=await browser.newPage({viewport:{width,height:600},deviceScaleFactor:1}),errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.goto(`http://127.0.0.1:4174/diorama-text-plane.html?scene=${scene}`);await page.waitForFunction(()=>window.ready);
  for(const[right,up]of[[1.7,.8],[-3.6,1.9]]){
   const state=await page.evaluate(([r,u])=>window.drawTextProbe(r,u),[right,up]);const file=`${scene}-${width}-${right}-${up}.png`;
   observations.push({file,scene,width,height:600,right,up,...state});await page.screenshot({path:path.join(out,file)});
  }
  if(errors.length)throw Error(errors.join('\n'));await page.close();
 }
 const vec=v=>[v.x,v.y,v.z];const fixtures=path.join(root,'verification/diorama-fixtures');
 const fonts=['FoliaParityLatin-Bold.ttf','FoliaParityBidi-Bold.ttf','FoliaParityChatCJK-Bold.otf'].map(file=>({file,sha256:createHash('sha256').update(fs.readFileSync(path.join(root,'verification/assets',file))).digest('hex')}));
 fs.writeFileSync(path.join(fixtures,'text-planes.json'),JSON.stringify({revision,browser:browser.version(),fonts,DPR:1,
  scope:'Original rasterDioramaLine, path/placement/frame-fit, real frozen Three WebGL plane. Kotlin must shape its own text. No active units, glow, CameraRig, fog/depth or public theme acceptance.',observations},null,2)+'\n');
 fs.writeFileSync(path.join(fixtures,'text-planes.tsv'),['# file scene width right up text64 advancePx fontPx rasterWidth rasterHeight eye look middleBaseline',...observations.map(o=>
  [o.file,o.scene,o.width,o.right,o.up,Buffer.from(o.text).toString('base64'),o.advancePx,o.fontPx,o.rasterWidth,o.rasterHeight,...vec(o.eye),...vec(o.look),o.baselineMetrics.baselineShift].join('\t'))].join('\n')+'\n');
 console.log(JSON.stringify({frames:observations.length,renderer:observations[0].renderer}));
}finally{await browser?.close();await server.close();}
