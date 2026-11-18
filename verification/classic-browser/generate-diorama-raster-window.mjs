// Untouched original unit/grapheme clocks for the long shaped-raster controls.
import fs from 'node:fs';import path from 'node:path';import{createHash}from'node:crypto';
import{referenceModules,root,upstream,revision,referenceMotionVersions}from'./reference.mjs';
const out=path.join(root,'verification/diorama-fixtures/raster-window-v1');
if(fs.existsSync(out))throw Error('Preserve original long-line oracle; refusing overwrite');
const{server,load}=await referenceModules();
try {
 const{buildLineGraphemeTimeline}=await load('utils/lyrics/graphemeTiming.ts');
 const{splitDioramaLyricUnits}=await load('components/visualizer/diorama/dioramaSceneLayout.ts');
 const records=['# kind case index startOffset endOffset startTime endTime text64'];const inputs=[];
 for(const name of ['latin','mixed']) {
  const words=Array.from({length:32},(_,i)=>(name==='mixed'?['Aمرحبا','12B','بالعالم','fiji']:['Wavy','fiji'])[i%(name==='mixed'?4:2)]);
  const line={fullText:words.join(' '),startTime:.7,endTime:3.9,
   words:words.map((word,i)=>({text:word+(i<31?' ':''),startTime:(700+i*100)/1000,endTime:(800+i*100)/1000}))};
  inputs.push({name,line});let offset=0;
  const glyphs=buildLineGraphemeTimeline(line);
  glyphs.forEach((g,i)=>{const start=offset;offset+=g.char.length;
   records.push(['G',name,i,start,offset,g.startTime,g.endTime,Buffer.from(g.char).toString('base64')].join('\t'))});
  splitDioramaLyricUnits(line,glyphs).forEach((u,i)=>records.push(['U',name,i,u.charStart,u.charEnd,
   u.startTime,u.endTime,Buffer.from(u.text).toString('base64')].join('\t')));
 }
 const hash=p=>createHash('sha256').update(fs.readFileSync(p)).digest('hex');
 fs.mkdirSync(out,{recursive:true});
 fs.writeFileSync(path.join(out,'source.tsv'),records.join('\n')+'\n');
 fs.writeFileSync(path.join(out,'conditions.json'),JSON.stringify({revision,motion:referenceMotionVersions(),
  scope:'Actual unmodified original splitDioramaLyricUnits/buildLineGraphemeTimeline via Vite SSR. Unit/timing oracle only; not pixels, camera or device performance.',
  node:process.version,inputs,sources:['src/utils/lyrics/graphemeTiming.ts','src/components/visualizer/diorama/dioramaSceneLayout.ts'].map(file=>({file,sha256:hash(path.join(upstream,file))})),
  lockSha256:hash(path.join(root,'verification/classic-browser/pnpm-lock.yaml')),
  originalLockSha256:hash(path.join(upstream,'package-lock.json')),
  generatorSha256:hash(path.join(root,'verification/classic-browser/generate-diorama-raster-window.mjs'))},null,2)+'\n');
 console.log(JSON.stringify({records:records.length-1,cases:inputs.length}));
}finally{await server.close()}
