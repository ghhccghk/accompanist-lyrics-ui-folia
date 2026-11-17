// Same ORIGINAL incoming nine-state, article and retarget on both sides.
// Source module receives passive trace reads only; no source expression is replaced.
import fs from 'node:fs';import path from 'node:path';import{createHash}from'node:crypto';
import{createRequire}from'node:module';import{createServer}from'vite';import{chromium}from'playwright';
import{createFumeMountedServer}from'./fume-mounted-server.mjs';
import{root,upstream,revision,checkReference,referenceMotionVersions}from'./reference.mjs';
checkReference();const require=createRequire(import.meta.url),folder=path.join(root,'verification/fume-fixtures/handoff-v1');
if(fs.existsSync(folder)&&!process.env.FOLIA_RECAPTURE_HANDOFF)throw Error('Preserve existing source evidence; set FOLIA_RECAPTURE_HANDOFF for an intentional audit');
const sourceFile=path.join(upstream,'src/components/visualizer/fume/fumeCameraStep.ts');
const injections=[
 ['    if (shouldUseBridge) {',`    globalThis.__fumeTrace?.push({kind:'intent',time,dt,phase:retargetPhase,branch:shouldUseBridge?'bridge':'spring',target:[targetCameraX,targetCameraY,targetCameraScale],retarget:{...state.retarget},before:{...state.camera}});\n`],
 ['        state.camera.focusX = bridgedCameraX;',`        globalThis.__fumeTrace?.push({kind:'bridge',forcing:[bridgedCameraX,bridgedCameraY,bridgedCameraScale],catchUp:bridgeCatchUp});\n`],
 ['        state.camera.velocityX = clamp(state.camera.velocityX, -maxVelocity, maxVelocity);',`        globalThis.__fumeTrace?.push({kind:'springXY',strength:springStrength,damping,catchUp:targetCatchUp,limit:maxVelocity,unclamped:[state.camera.velocityX,state.camera.velocityY],focus:[state.camera.focusX,state.camera.focusY,state.camera.focusScale]});\n`],
 ['        state.camera.velocityScale = clamp(state.camera.velocityScale, -1.6, 1.6);',`        globalThis.__fumeTrace?.push({kind:'springScale',strength:scaleSpringStrength,damping:scaleDamping,unclamped:state.camera.velocityScale});\n`]
];
const moduleServer=await createServer({configFile:false,root:path.dirname(sourceFile),
 resolve:{alias:{'@chenglou/pretext':require.resolve('@chenglou/pretext')}},
 plugins:[{name:'passive-fume-step-trace',enforce:'pre',transform(code,id){if(id.split('?')[0].replaceAll('\\','/')!==sourceFile.replaceAll('\\','/'))return;
  for(const[anchor,addition]of injections){if(code.split(anchor).length!==2)throw Error('Source trace anchor changed');code=code.replace(anchor,addition+anchor)}return{code,map:null};}}],
 optimizeDeps:{noDiscovery:true,entries:[]},cacheDir:path.join(root,'.work/fume-handoff-module'),server:{middlewareMode:true,fs:{allow:[root]}}});
const mounted=await createFumeMountedServer(4181);let browser;
try{
 const {stepFumeCamera}=await moduleServer.ssrLoadModule(sourceFile);
 const {resolveArticleOverviewCamera}=await moduleServer.ssrLoadModule(path.join(upstream,'src/components/visualizer/fume/fumeCamera.ts'));
 await mounted.listen();browser=await chromium.launch({headless:true});
 const original=JSON.parse(fs.readFileSync(path.join(root,'verification/fume-fixtures/mounted-observations.json'),'utf8'));
 const cases=[],articleRows=['# case width height viewportHeight columns gap bounds'],blocks=['# case index text64 start end hero x y width height innerWidth fontPx lineHeight words'],geometry=['# case index start end left top width offsets'];
 const steps=['# case rate from time dt before9 after9 phase branch target3 bridge3 limit unclampedXY unclampedScale'];
 const seeds=['# case width stepped time pose9 source started duration from3 bridge waypoint3 waypointPhase overviewStart'];
 const pose=p=>[p.x,p.y,p.velocityX,p.velocityY,p.focusX,p.focusY,p.scale,p.velocityScale,p.focusScale];
 for(const width of[380,800])for(const stepped of[false,true]){
  const page=await browser.newPage({viewport:{width,height:600},deviceScaleFactor:1}),errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.goto(`http://127.0.0.1:4181/fume-mounted.html?stepped=${stepped}&glow=1`);await page.waitForFunction(()=>window.ready);await page.screenshot();
  for(let n=0;n<120;n++){await page.evaluate(async()=>{window.advanceSourceFrame(0);await new Promise(r=>setTimeout(r,2))});if(errors.length)throw Error(errors.join('\n'));if(await page.evaluate(()=>!!window.sourceArticleReady))break;}
  if(!await page.evaluate(()=>!!window.sourceArticleReady))throw Error('Original article not ready');
  const a=await page.evaluate(()=>window.exportFumeScene());await page.close();
  a.blockBySourceLineIndex=new Map(a.blocks.map(b=>[b.sourceLineIndex,b]));a.chronologicalBlocks=a.chronologicalBlocks.map(b=>a.blockBySourceLineIndex.get(b.sourceLineIndex));
  for(const start of[3,5.4]){
   const id=`${width}-${stepped}-${start}`,obs=original.observations.find(o=>o.width===width&&o.stepped===stepped&&o.time===start);
   if(JSON.stringify(obs.layout)!==JSON.stringify(a.blocks.map(b=>({index:b.sourceLineIndex,x:b.x,y:b.y,width:b.width,height:b.height}))))throw Error('Original mounted geometry changed');
   const seed={initialized:true,camera:structuredClone(obs.camera),retarget:structuredClone(obs.retarget)};
   const r=seed.retarget;seeds.push([id,width,stepped,start,pose(seed.camera).join(','),r.sourceLineIndex,r.startedAt,r.duration,[r.fromX,r.fromY,r.fromScale].join(','),r.bridgeMode,[r.bridgeWaypointX,r.bridgeWaypointY,r.bridgeWaypointScale].join(','),r.bridgeWaypointPhase,obs.overviewStart].join('\t'));
   articleRows.push([id,a.width,a.height,a.viewportHeight,a.columns,a.gap,Object.values(a.paperBounds).join(',')].join('\t'));
   for(const b of a.blocks){const l=b.line,encode=s=>Buffer.from(s).toString('base64');
    blocks.push([id,b.sourceLineIndex,encode(l.fullText),l.startTime,l.endTime,b.variant==='hero',b.x,b.y,b.width,b.height,b.innerWidth,b.fontPx,b.lineHeight,(l.words??[]).map(w=>[encode(w.text),w.startTime,w.endTime].join('~')).join('|')||'-'].join('\t'));
    for(const row of b.renderLines)geometry.push([id,b.sourceLineIndex,row.start,row.end,row.left,row.top,row.width,[...row.glyphOffsets,row.width].join(',')].join('\t'));
   }
   const runs=[];
   for(const rate of['mounted60','144','irregular']){
    let state=structuredClone(seed),last=start,n=0;const records=[];
    while(last<start+.25-1e-12){
     const time=Math.min(start+.25,rate==='mounted60'?(Math.round(start*60)+n+1)/60:rate==='144'?start+(n+1)/144:last+[.007,.029,.011,.019,.033][n%5]);
     const dt=Math.max(1/240,Math.min(.05,time-last));globalThis.__fumeTrace=[];const before=structuredClone(state);
     stepFumeCamera(state,{article:a,time,now:time*1000,dt,lineIndex:r.sourceLineIndex,viewport:{width,height:600},overviewCamera:resolveArticleOverviewCamera(a,{width,height:600}),overviewStartTime:obs.overviewStart,cameraSpeed:1,cameraTrackingMode:stepped?'stepped':'smooth',staticMode:false,animationIntensity:'normal'});
     const trace=globalThis.__fumeTrace;if(!trace.length)throw Error('Passive trace missing');
     const i=trace.find(x=>x.kind==='intent'),b=trace.find(x=>x.kind==='bridge'),s=trace.find(x=>x.kind==='springXY'),sc=trace.find(x=>x.kind==='springScale');
     steps.push([id,rate,last,time,dt,pose(before.camera).join(','),pose(state.camera).join(','),i.phase,i.branch,i.target.join(','),b?.forcing.join(',')??'-',s?.limit??'-',s?.unclamped.join(',')??'-',sc?.unclamped??'-'].join('\t'));
     records.push({from:last,time,dt,trace:trace.map(({before,retarget,...values})=>values)});last=time;n++;
    }
    runs.push({rate,records});
   }
   cases.push({id,geometryOrigin:'passive actual mounted original article; matches saved mounted-observations layout exactly',seedOrigin:`mounted-observations.json ${obs.file}, AFTER original frame at ${start}s`,seed,runs});
  }
 }
 fs.mkdirSync(folder,{recursive:true});for(const[name,rows]of[['articles',articleRows],['blocks',blocks],['rows',geometry],['seeds',seeds],['steps',steps]])fs.writeFileSync(path.join(folder,name+'.tsv'),rows.join('\n')+'\n');
 const sha=file=>createHash('sha256').update(fs.readFileSync(file)).digest('hex');
 fs.writeFileSync(path.join(folder,'source-trace.json'),JSON.stringify({version:1,revision,motion:referenceMotionVersions(),browser:browser.version(),sourceSha256:sha(sourceFile),mountedSourceSha256:sha(path.join(root,'verification/fume-fixtures/mounted-observations.json')),fontSha256:sha(path.join(root,'verification/assets/FoliaParityLatin-Bold.ttf')),lockSha256:sha(path.join(root,'verification/classic-browser/pnpm-lock.yaml')),injections,scope:'No source formulas replaced. Passive trace reads injected in an isolated SSR copy. Actual article from untouched mounted component. Seeds from preserved original 92-frame source capture. Only mounted60 times match original capture schedule; other rates are controlled original operator experiments, not a mounted capture.',cases},null,2)+'\n');
 console.log(JSON.stringify({cases:cases.length,steps:steps.length-1,browser:browser.version(),motion:referenceMotionVersions()}));
}finally{delete globalThis.__fumeTrace;await browser?.close();await mounted.close();await moduleServer.close();}
