// One fresh dedicated instance; never overwrite a source oracle silently.
import fs from 'node:fs';import path from 'node:path';import {createHash} from 'node:crypto';
import {chromium} from 'playwright';
import {root,upstream,revision,checkReference,referenceMotionVersions} from './reference.mjs';
import {startReferenceServer} from './serve.mjs';
checkReference();
const output=path.join(root,'build/verification/cappella-complete-browser');
if(fs.existsSync(output))throw Error('Preserve the previous source capture before a deliberate audit');
fs.mkdirSync(output,{recursive:true});
const server=await startReferenceServer(0);let browser;
try {
 browser=await chromium.launch({headless:true,args:['--renderer-process-limit=2']});
 const page=await browser.newPage({viewport:{width:800,height:300},deviceScaleFactor:1});
 const errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.goto(server.resolvedUrls.local[0]+'cappella-complete-text.html');
 await page.waitForFunction(()=>window.ready);
 const observations=[];
 for(const name of ['italic','arabic','mixed','latin','cjk']) {
  observations.push(await page.evaluate(name=>window.draw(name),name));
  await page.screenshot({path:path.join(output,name+'.png'),omitBackground:true});
 }
 if(errors.length)throw Error(errors.join('\n'));
 const hash=p=>createHash('sha256').update(fs.readFileSync(p)).digest('hex');
 fs.writeFileSync(path.join(output,'conditions.json'),JSON.stringify({revision,motion:referenceMotionVersions(),
  browser:browser.version(),viewport:[800,300],DPR:1,
  originalSha256:hash(path.join(upstream,'src/components/visualizer/cappella/CappellaBubbleParts.tsx')),
  hostSha256:hash(path.join(root,'verification/classic-browser/cappella-complete-text.tsx')),
  lockSha256:hash(path.join(root,'verification/classic-browser/pnpm-lock.yaml')),
  fonts:[...new Set(observations.map(x=>x.font))].map(name=>({name,sha256:hash(path.join(root,'verification/assets',name))})),
  observations,scope:'Actual original passed whole-text component, fixed transparent host frame. No original algorithm replacements, per-character opacity edits, time control, or source screenshot pixel registration. Not whole-theme visual acceptance.'},null,2)+'\n');
 console.log('CAPPELLA_COMPLETE_ORIGINAL_CAPTURED');
} finally {if(browser)await browser.close();await server.close()}
