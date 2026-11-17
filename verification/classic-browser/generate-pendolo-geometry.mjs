// Evaluate unchanged pinned source exports; inputs and outputs remain separate.
import fs from 'node:fs';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {referenceModules, root, upstream, revision} from './reference.mjs';

const {server, load} = await referenceModules();
try {
  const {calculatePendoloWheelLayout} = await load('components/visualizer/pendolo/pendoloGeometry.ts');
  const {resolvePendoloFallbackAnchorIndex: anchor, resolvePendoloRotatingLineOpacity: opacity} =
    await load('components/visualizer/pendolo/pendoloTimeline.ts');
  const hostSource=fs.readFileSync(path.join(upstream,'src/components/visualizer/pendolo/VisualizerPendolo.tsx'),'utf8');
  const widthDeclaration=hostSource.match(/    const availableTextWidth = Math.max\([\s\S]+?\n    \);/)[0];
  const sourceWidth=new Function('viewportSize','maxItemX',`${widthDeclaration}\nreturn availableTextWidth`);
  // Loading types.ts would pull unrelated browser assets; these exact values are
  // cross-checked against the pinned declaration, and recorded as explicit inputs.
  const defaults = {arcRadius:.42,arcAngleDeg:100,wheelCenterX:0,wheelCenterY:.5,activeScale:1.25};
  const typeSource=fs.readFileSync(path.join(upstream,'src/types.ts'),'utf8');
  const originalDefault=typeSource.match(/export const DEFAULT_PENDOLO_TUNING: PendoloTuning = \{([\s\S]*?)\n};/)[1];
  for(const[k,v]of Object.entries(defaults))if(Number(originalDefault.match(new RegExp(`${k}: ([\\d.]+),`))?.[1])!==v)
    throw Error(`default mismatch ${k}`);
  const input=['# key count target offset width height radiusOffset arcRadius arcAngleDeg wheelCenterX wheelCenterY activeScale blockHeights'],
    expected=['# key count; item key index angleRad angleDeg x y active distance alpha scale'],
    timelines=['# L case index start end renderEnd; Q case time current lastValid observed anchor'],
    fades=['# baseAngle rotation baseOpacity originalOpacity'];
  let cases=0,items=0,queries=0;
  for(const n of [0,1,6,25])for(const target of [-1,0,.5,2,5.5,12,n])for(const size of [[380,480],[800,600],[200,180]])for(let variant=0;variant<3;variant++){
    const key=`g${cases++}`, [width,height]=size;
    const tuning=variant===2?{...defaults,arcRadius:.31,arcAngleDeg:160,wheelCenterX:-.12,wheelCenterY:.42,activeScale:1.4}:defaults;
    const heights=variant===0?undefined:Array.from({length:n},(_,i)=>[33.6,104,211.5,0][i%4]);
    const offset=variant===2?.091:0, radiusOffset=variant===0?0:Math.min(width,height)*.06;
    const lines=Array.from({length:n},(_,i)=>({id:String(i),fullText:`Synthetic line ${i}`,startTime:i,endTime:i+.5,words:[]}));
    input.push([key,n,target,offset,width,height,radiusOffset,...Object.values(tuning),heights?.join(',')||'-'].join('\t'));
    const result=calculatePendoloWheelLayout(lines,target,offset,width,height,tuning,radiusOffset,heights);
    expected.push(['C',key,result.length,sourceWidth({width,height},width*tuning.wheelCenterX+Math.min(width,height)*tuning.arcRadius)].join('\t'));
    for(const r of result){items++;expected.push(['I',key,r.index,r.angleRad,r.angleDeg,r.x,r.y,+r.isActive,r.distanceFromActive,r.alpha,r.scale].join('\t'))}
  }
  const scenes={empty:[],regular:[[1,2,2.2],[4,5,5.2],[7,8,8.2]],overlap:[[1,6,6.3],[2,2.1,2.167],[3,3.4,3.6],[7,8,8.2]],
    unsorted:[[5,6,6.2],[1,4,4.2],[2,2,2.067],[8,9,9.2]],edges:[[1,1,1],[1,1.1,1.167],[1.167,1.3,1.5]]};
  for(const[name,spans]of Object.entries(scenes)){
    const lines=spans.map(([startTime,endTime,renderEndTime],i)=>({id:String(i),fullText:`Synthetic line ${i}`,startTime,endTime,renderHints:{renderEndTime}}));
    lines.forEach((l,i)=>timelines.push(['L',name,i,l.startTime,l.endTime,l.renderHints.renderEndTime].join('\t')));
    // Explicit exact and right-limit probes; no assertion depends on one RAF grid.
    const times=[-1,0,1,10,...Array.from({length:481},(_,i)=>i/48),...spans.flatMap(([s,e,r])=>[s-1e-7,s,s+1e-7,e,r-1e-7,r,r+1e-7])].sort((a,b)=>a-b);
    let last=0,observed=false;
    for(const t of [...new Set(times)]){
      const current=lines.findLastIndex(l=>t>=l.startTime&&t<=l.renderHints.renderEndTime);
      if(current>=0){last=current;observed=true}
      const value=anchor(lines,current,last,observed,t);
      timelines.push(['Q',name,t,current,last,+observed,value].join('\t'));queries++;
    }
  }
  for(const angle of[-360,-110,-109.999,-90,-82,-45,0,45,82,90,109.999,110,360])for(const rotation of[-720,-90,-.01,0,.01,90,720])for(const alpha of[.12,.5,1])
    fades.push([angle,rotation,alpha,opacity(angle,rotation,alpha)].join('\t'));
  const out=path.join(root,'verification/pendolo-fixtures');fs.mkdirSync(out,{recursive:true});
  for(const[name,rows]of Object.entries({'geometry-inputs':input,'geometry':expected,'timeline':timelines,'opacity':fades}))fs.writeFileSync(path.join(out,name+'.tsv'),rows.join('\n')+'\n');
  const sources=['pendoloGeometry.ts','pendoloTimeline.ts'].map(name=>({path:`src/components/visualizer/pendolo/${name}`,sha256:createHash('sha256').update(fs.readFileSync(path.join(upstream,`src/components/visualizer/pendolo/${name}`))).digest('hex')}));
  const conditions={revision,cases,items,timelineQueries:queries,opacityQueries:fades.length-1,defaults,sources,
    scope:'Unchanged pinned pure exports via locked Vite/pretext. Controlled measured block-height inputs, not real text wrapping or full browser/Compose rendering. Latest-source-array-order inclusive host selector; deterministic native history reconstructed from semantic boundaries.'};
  fs.writeFileSync(path.join(out,'geometry-conditions.json'),JSON.stringify(conditions,null,2)+'\n');console.log(JSON.stringify(conditions));
} finally { await server.close() }
