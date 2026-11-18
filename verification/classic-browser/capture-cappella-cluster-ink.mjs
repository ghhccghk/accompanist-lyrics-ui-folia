import fs from 'node:fs';import path from 'node:path';import{createHash}from'node:crypto';import{chromium}from'playwright';
import{root,upstream,revision,checkReference,referenceMotionVersions}from'./reference.mjs';import{startReferenceServer}from'./serve.mjs';
checkReference();const out=path.join(root,'build/verification/cappella-cluster-browser');
const fixtures=path.join(root,'verification/cappella-fixtures/cluster-ink-v1');
if(fs.existsSync(out)||fs.existsSync(fixtures))throw Error('Preserve original cluster capture; refusing overwrite');
const hash=p=>createHash('sha256').update(fs.readFileSync(p)).digest('hex');
fs.mkdirSync(out,{recursive:true});const server=await startReferenceServer(0);let browser;
try{
 browser=await chromium.launch({headless:true,args:['--renderer-process-limit=2']});
 const page=await browser.newPage({viewport:{width:800,height:300},deviceScaleFactor:1});const errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.goto(server.resolvedUrls.local[0]+'cappella-cluster-ink.html');await page.waitForFunction(()=>window.ready);
 const observations=[],cases=await page.evaluate(()=>window.cases);
 for(const{name,count}of cases)for(const owner of [-1,...Array.from({length:count},(_,i)=>i)]){
  const state=await page.evaluate(({name,owner})=>window.draw(name,owner),{name,owner});
  const file=`${name}-${owner<0?'all':owner}.png`;await page.screenshot({path:path.join(out,file),omitBackground:true});
  observations.push({file,...state,sha256:hash(path.join(out,file))});
 }
 if(errors.length)throw Error(errors.join('\n'));
 fs.mkdirSync(fixtures,{recursive:true});
 fs.writeFileSync(path.join(fixtures,'conditions.json'),JSON.stringify({revision,motion:referenceMotionVersions(),browser:browser.version(),
  viewport:[800,300],DPR:1,fontPx:64,lineHeight:92.8,padding:[16,20],border:1,
  source:{file:'src/components/visualizer/cappella/CappellaBubbleParts.tsx',sha256:hash(path.join(upstream,'src/components/visualizer/cappella/CappellaBubbleParts.tsx'))},
  host:{file:'verification/classic-browser/cappella-cluster-ink.tsx',sha256:hash(path.join(root,'verification/classic-browser/cappella-cluster-ink.tsx'))},
  locks:['verification/classic-browser/pnpm-lock.yaml','.work/folia-major/package-lock.json'].map(file=>({file,sha256:hash(path.join(root,file))})),
  fonts:[...new Set(observations.map(x=>x.font))].map(name=>({name,sha256:hash(path.join(root,'verification/assets',name))})),
  observations,scope:'Unmodified original ActiveCappellaText, all EGCs mounted. Only original CSS animation currentTime is paused at opaque/transparent endpoints to isolate each original span paint. This is cluster-ownership evidence, not natural playback or full-stage acceptance.'},null,2)+'\n');
 console.log('CAPPELLA_ORIGINAL_CLUSTER_OWNERSHIP_CAPTURED');
}finally{if(browser)await browser.close();await server.close()}
