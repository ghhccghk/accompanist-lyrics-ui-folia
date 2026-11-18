import fs from 'node:fs';import path from 'node:path';import {createHash} from 'node:crypto';import {chromium} from 'playwright';
import {root,upstream,revision,checkReference,referenceMotionVersions} from './reference.mjs';import {startReferenceServer} from './serve.mjs';
checkReference();const output=path.join(root,'build/verification/cappella-active-browser');
const fixture=path.join(root,'verification/cappella-fixtures/active-ink-v1');
if(fs.existsSync(output)||fs.existsSync(fixture))throw Error('Preserve previous original active-ink capture; refusing overwrite');
fs.mkdirSync(output,{recursive:true});
const server=await startReferenceServer(0);let browser;
try {
 browser=await chromium.launch({headless:true,args:['--renderer-process-limit=2']});
 const page=await browser.newPage({viewport:{width:800,height:300},deviceScaleFactor:1});const errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.goto(server.resolvedUrls.local[0]+'cappella-active-ink.html');await page.waitForFunction(()=>window.ready);
 const observations=[],frames=['# file case uniform time count base64Text cssOpacity'];
 for(const name of ['italic','arabic','mixed','latin','cjk'])for(const [time,uniform]of[[.1,false],[.35,false],[.65,false],[1.2,false],[.5,true],[2,true]]){
  const s=await page.evaluate(({name,time,uniform})=>window.draw(name,time,uniform),{name,time,uniform});
  const file=`${name}-${uniform?'uniform':'stagger'}-${Math.round(time*1000)}.png`;
  await page.screenshot({path:path.join(output,file),omitBackground:true});observations.push({file,...s});
  frames.push([file,name,uniform,time,s.count,Buffer.from(s.text).toString('base64'),s.opacities.join(',')].join('\t'));
 }
 if(errors.length)throw Error(errors.join('\n'));
 const hash=p=>createHash('sha256').update(fs.readFileSync(p)).digest('hex');
 fs.mkdirSync(fixture,{recursive:true});fs.writeFileSync(path.join(fixture,'frames.tsv'),frames.join('\n')+'\n');
 fs.writeFileSync(path.join(fixture,'conditions.json'),JSON.stringify({revision,motion:referenceMotionVersions(),browser:browser.version(),
  viewport:[800,300],DPR:1,fontPx:64,lineHeight:92.8,padding:[16,20],border:1,
  originalSha256:hash(path.join(upstream,'src/components/visualizer/cappella/CappellaBubbleParts.tsx')),
  hostSha256:hash(path.join(root,'verification/classic-browser/cappella-active-ink.tsx')),
  locks:['verification/classic-browser/pnpm-lock.yaml','.work/folia-major/package-lock.json'].map(file=>({file,sha256:hash(path.join(root,file))})),
  fonts:[...new Set(observations.map(x=>x.font))].map(name=>({name,sha256:hash(path.join(root,'verification/assets',name))})),
  observations,scope:'Unmodified original ActiveCappellaText/character-plan, fixed transparent host frame, per-source-character CSS animations paused/re-anchored to their media progress for deterministic comparison. No shaping, geometry, original opacity formula or event logic rewritten. Not full message-stage or every source RAF history.'},null,2)+'\n');
 console.log('CAPPELLA_ACTIVE_ORIGINAL_CAPTURED');
} finally {if(browser)await browser.close();await server.close()}
