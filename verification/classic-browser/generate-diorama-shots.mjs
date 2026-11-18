import fs from 'node:fs';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {createServer,transformWithOxc} from 'vite';
import {root,revision,checkReference,referenceMotionVersions} from './reference.mjs';
checkReference();
const folder=path.join(root,'verification/diorama-fixtures/shots-v1');
if(fs.existsSync(folder))throw Error('Preserve existing source shot fixture; refusing overwrite');
const file=path.join(root,'.work/folia-major/src/components/visualizer/diorama/cameraPath.ts');
const source=fs.readFileSync(file,'utf8');
const a=source.indexOf('const SHOT_KINDS:'),b=source.indexOf('export interface DioramaShotOffset',a);
if(a<0||b<0)throw Error('Pinned original shot source boundary missing');
const fragment=source.slice(a,b);
const spec='/@fs/'+file.replaceAll('\\','/');
const code=`import {seededUnit,hashSeed} from '${spec}';
const clamp01=(value:number):number=>Math.min(1,Math.max(0,value));
${fragment}
export const originalWeights=computeShotWeights;`;
const server=await createServer({configFile:false,root:path.join(root,'verification/classic-browser'),
 plugins:[{name:'observe-private-shot-weights',resolveId:id=>id==='virtual:shot-weights'?'\0shot-weights.ts':null,
  load:async id=>id==='\0shot-weights.ts'?await transformWithOxc(code,'shot-weights.ts'):null}],
 optimizeDeps:{noDiscovery:true,entries:[]},cacheDir:path.join(root,'.work/diorama-shot-weights'),
 server:{middlewareMode:true,fs:{allow:[root]}}});
try{
 const cam=await server.ssrLoadModule(file);
 const observed=await server.ssrLoadModule('virtual:shot-weights');
 const inputs=Array.from({length:36},(_,i)=>{
  const count=[0,3,4,7,8,12][i%6];
  const duration=[.34,.340000001,1.3,1.299999999,-.1,0][Math.floor(i/6)];
  return {fullText:`Synthetic passage ${i}`,startTime:i*1.1,endTime:i*1.1+1,
   words:Array.from({length:count},(_,j)=>({text:`word${j}`,startTime:i*1.1+j*.1,endTime:i*1.1+j*.1+duration})),
   ...(i%7===1?{isChorus:true}:{}),...(i%7===2?{songPart:'HOOK repeat'}:i%7===3?{songPart:'drop'}:i%7===4?{songPart:'verse'}:{}),
   ...(i%4!==0?{blockIndex:Math.floor(i/5)}:{})};
 });
 const seedCases=[undefined,'','DioramaProbe','Mirror👩‍🚀',7,12345];
 const rows=[],motions=[];
 for(const mode of ['calm','normal','chaotic']){
  const m=cam.resolveDioramaMotionParams(undefined,mode);motions.push([mode,m.moveScale,m.driftScale,m.smoothTime,m.weaveScale]);
  for(const profile of ['plain','metadata']){
   const lines=inputs.map(({isChorus,songPart,blockIndex,...l})=>profile==='plain'?l:{...l,isChorus,songPart,blockIndex});
   for(const [si,seed]of seedCases.entries())for(const i of [-1,...inputs.keys(),inputs.length,inputs.length+1]){
    const shot=cam.getDioramaShot(i,lines,seed,mode);
    if(shot!==observed.getDioramaShot(i,lines,seed,mode))throw Error('Observed source picker differs');
    rows.push([profile,mode,si,cam.hashSeed(seed),i,shot,...Object.values(observed.originalWeights(i,lines,mode))]);
   }
  }
 }
 fs.mkdirSync(folder,{recursive:true});
 const b64=s=>Buffer.from(s).toString('base64');
 fs.writeFileSync(path.join(folder,'inputs.tsv'),['# index text64 start end words64startEnd isChorus songPart64 blockIndex',...inputs.map((l,i)=>[
  i,b64(l.fullText),l.startTime,l.endTime,l.words.map(w=>[b64(w.text),w.startTime,w.endTime].join(',')).join(';'),
  l.isChorus?1:0,b64(l.songPart??''),l.blockIndex??'null'].join('\t'))].join('\n')+'\n');
 fs.writeFileSync(path.join(folder,'expected.tsv'),['# profile mode seedCase seedHash lineIndex picked then13 weights in SHOT_KINDS order',...rows.map(r=>r.join('\t'))].join('\n')+'\n');
 fs.writeFileSync(path.join(folder,'motion.tsv'),['# mode move drift smooth weave',...motions.map(r=>r.join('\t'))].join('\n')+'\n');
 fs.writeFileSync(path.join(folder,'conditions.json'),JSON.stringify({revision,motion:referenceMotionVersions(),
  sourceFile:'src/components/visualizer/diorama/cameraPath.ts',sourceSha256:createHash('sha256').update(source).digest('hex'),
  sourceFragment:{startLine:source.slice(0,a).split('\n').length,endLine:source.slice(0,b).split('\n').length,sha256:createHash('sha256').update(fragment).digest('hex')},
  seedCases:seedCases.map((s,i)=>({case:i,type:typeof s,value:s??null,hash:cam.hashSeed(s)})),cases:rows.length,inputs,
  scope:'Original exported getDioramaShot and resolveDioramaMotionParams. Exact private computeShotWeights observed via unchanged source extraction; copy export checked against actual source picker. No rendered scene or invented core chorus metadata.'},null,2)+'\n');
 console.log(JSON.stringify({cases:rows.length}));
}finally{await server.close()}
