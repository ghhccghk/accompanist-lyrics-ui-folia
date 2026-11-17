// Execute the exact isolated source selector, with only TypeScript erased.
// Importing the entire app helper would initialize unrelated i18n/player stores.
import fs from'node:fs';import path from'node:path';import{createHash}from'node:crypto';import{stripTypeScriptTypes}from'node:module';
import{checkReference,root,upstream,revision,referenceModules}from'./reference.mjs';
checkReference();
const content=fs.readFileSync(path.join(upstream,'src/utils/appPlaybackHelpers.ts'),'utf8');
const declaration=content.match(/export const findLatestActiveLineIndex = [\s\S]+?\n};/)[0];
const{findLatestActiveLineIndex:select}=await import('data:text/javascript;base64,'+Buffer.from(stripTypeScriptTypes(declaration)).toString('base64'));
const{server,load}=await referenceModules();
try{
 const{buildLineRenderHints}=await load('utils/lyrics/renderHints.ts');
 const input=['# case\tline\ttext64\tstart\tend\trenderEnd'],expected=['# case\ttime\tactive\trecent\tupcoming'];let cases=0;
 for(const source of[
  [],[[.7,1.4],[1.4,2.1],[2.8,3.5]],[[.7,2.8],[1.2,1.4],[1.2,1.6]],
  [[.7,.7],[.701,.751],[.8,.9],[1,1.1],[1.2,1.379],[1.4,1.58]],
  [[2,3],[.7,1.5],[.7,1.2],[1,1.1]],[[.7,9],[1,2],[3,4],[5,6]],
 ])for(const annotated of[false,true]){
  const key=`case${cases++}`,lines=source.map(([startTime,endTime],i)=>({fullText:`synthetic ${i}`,startTime,endTime,words:[]}));
  if(annotated)for(const line of lines)line.renderHints=buildLineRenderHints(line);
  for(const[i,l]of lines.entries())input.push([key,i,Buffer.from(l.fullText).toString('base64'),l.startTime,l.endTime,l.renderHints?.renderEndTime??l.endTime].join('\t'));
  const times=new Set([-1,0,10,99]);for(const l of lines)for(const t of[l.startTime,l.endTime,l.renderHints?.renderEndTime??l.endTime])for(const d of[-1e-6,0,1e-6])times.add(t+d);
  for(let i=0;i<80;i++)times.add(i*.1234567);
  for(const t of[...times].sort((a,b)=>a-b)){
   const active=select(lines,t);let recent=-1;if(active===-1)for(let i=lines.length-1;i>=0;i--)if(t>(lines[i].renderHints?.renderEndTime??lines[i].endTime)){recent=i;break}
   const upcoming=active>=0?(active+1<lines.length?active+1:-1):lines.findIndex(l=>l.startTime>t);
   expected.push([key,t,active,recent,upcoming].join('\t'));
  }
 }
 const out=path.join(root,'verification/cadenza-fixtures');fs.writeFileSync(path.join(out,'timelineInputs.tsv'),input.join('\n')+'\n');fs.writeFileSync(path.join(out,'timeline.tsv'),expected.join('\n')+'\n');
 fs.writeFileSync(path.join(out,'timeline-conditions.json'),JSON.stringify({revision,cases,observations:expected.length-1,source:'src/utils/appPlaybackHelpers.ts:findLatestActiveLineIndex',
  exact_declaration_sha256:createHash('sha256').update(declaration).digest('hex'),method:'Exact original declaration extracted, annotations erased by Node stripTypeScriptTypes, imported as ESM. Source array order retained; no rewritten selector.'},null,2)+'\n');
 console.log(JSON.stringify({cases,observations:expected.length-1}));
}finally{await server.close()}
