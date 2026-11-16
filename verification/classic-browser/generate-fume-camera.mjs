// Executes untouched original camera target functions against explicit measured geometry.
import fs from 'node:fs';import path from 'node:path';
import {referenceModules,root,revision}from'./reference.mjs';
const{server,load}=await referenceModules();
try{
 const c=await load('components/visualizer/fume/fumeCamera.ts');
 const {isFumeBlockOnScreen}=await load('components/visualizer/fume/fumeBlockFrame.ts');
 const {buildWordRangesFromWords}=await load('components/visualizer/fume/fumeReveal.ts');
 const {getLineRenderEndTime}=await load('utils/lyrics/renderHints.ts');
 const header=`# original Folia ${revision}; synthetic measured advances, not raster or camera-trajectory evidence`;
 const defs=[header,'# id\ttextB64\tstart\tend\tx\ty\twidth\theight\tlineHeight'];
 const rowDefs=[header,'# blockId\tstart\tend\tleft\ttop\twidth\toffsets'];
 const blocks=[
  {sourceLineIndex:0,line:{fullText:'ABCD EFGH',startTime:2,endTime:5.7,words:[]},x:47.3,y:92.8,width:188,height:75.4,lineHeight:37.7,rows:[[0,4,6.2,0],[4,9,0,37.7]]},
  {sourceLineIndex:2,line:{fullText:'XYZ',startTime:0,endTime:.12,words:[]},x:1800,y:4200,width:155,height:45,lineHeight:45,rows:[[0,3,0,0]]},
  {sourceLineIndex:4,line:{fullText:'',startTime:6,endTime:8,words:[]},x:-25,y:-19,width:80,height:22,lineHeight:22,rows:[]},
 ].map(b=>{
  const graphemes=[...b.line.fullText],segments=[],segmentMetas=[],widths=[],breakableFitAdvances=[];
  b.renderLines=b.rows.map(([start,end,left,top],i)=>{
   const advances=Array.from({length:end-start},(_,j)=>8.25+(j*3+i)%7);
   const offsets=[0];for(const a of advances)offsets.push(offsets.at(-1)+a);
   const text=graphemes.slice(start,end).join('');segments.push(text);segmentMetas.push({graphemeStart:start,graphemeEnd:end,graphemeCount:end-start});widths.push(offsets.at(-1));breakableFitAdvances.push(advances);
   rowDefs.push([b.sourceLineIndex,start,end,left,top,offsets.at(-1),offsets.join(',')].join('\t'));
   return{start,end,left,top,width:offsets.at(-1),glyphOffsets:offsets.slice(0,-1),graphemes:graphemes.slice(start,end)};
  });
  b.graphemes=graphemes;b.wordRanges=buildWordRangesFromWords(b.line,graphemes);b.prepared={segments,widths,breakableFitAdvances};b.segmentMetas=segmentMetas;
  defs.push([b.sourceLineIndex,Buffer.from(b.line.fullText).toString('base64'),b.line.startTime,b.line.endTime,b.x,b.y,b.width,b.height,b.lineHeight].join('\t'));
  return b;
 });
 const out=[header,'# kind\tblockId\tviewportWidth\tviewportHeight\targuments\texpected'];
 const emit=(kind,b,v,args,result)=>out.push([kind,b,v.width,v.height,args.join(','),result===null?'-':result.join(',')].join('\t'));
 const articles=[[],blocks.slice(0,2),blocks];
 for(const v of[{width:380,height:600},{width:1440,height:900},{width:50,height:20},{width:0,height:0}]){
  for(const b of blocks){
   emit('entry',b.sourceLineIndex,v,[],Object.values(c.resolveBlockEntryFocusPoint(b)));
   emit('scale',b.sourceLineIndex,v,[],[c.resolveCameraScaleForBlock(b,v)]);
   for(const p of[-2,0,.1,.7,1,3.29,3.3,3.5,3.9,4-1e-8,4,4+1e-8,4.000001,4.1,4.69,4.7,5,8.5,9,15]){
    emit('smooth',b.sourceLineIndex,v,[p],Object.values(c.resolveSmoothBlockFocusPoint(b,p)));
    emit('stepped',b.sourceLineIndex,v,[Math.floor(p)],Object.values(c.resolveSteppedBlockFocusPoint(b,Math.floor(p))))
   }
   for(const camera of[{x:0,y:0,scale:1},{x:100,y:200,scale:.22},{x:1900,y:4300,scale:2.24}])for(const overscan of[0,180])
    emit('screen',b.sourceLineIndex,v,[camera.x,camera.y,camera.scale,overscan],[+isFumeBlockOnScreen(b,camera,v,overscan)]);
  }
  emit('overviewDuration',-1,v,[],[c.resolveOverviewRetargetDuration(v)]);
  articles.forEach((bs,i)=>{
   const article={width:2500,height:4600,blocks:bs,chronologicalBlocks:bs,blockBySourceLineIndex:new Map(bs.map(b=>[b.sourceLineIndex,b])),lastChronologicalRenderEndTime:bs.length?getLineRenderEndTime(bs.at(-1).line):-Infinity};
   emit('overview',i,v,[],Object.values(c.resolveArticleOverviewCamera(article,v)));
   for(const index of[-1,0,2,4,99])for(const t of[-.1,0,.05,.12,1,2,2.1,5.7,8,20])emit('focus',i,v,[index,t],[c.resolveFocusBlock(article,index,t)?.sourceLineIndex??-1]);
  });
  for(const from of[{x:0,y:0,scale:.22},{x:-100,y:200,scale:1.18},{x:3000,y:5000,scale:2.2}])for(const target of[{x:0,y:0,scale:1},{x:1800,y:4200,scale:2.2}])for(const overview of[null,{x:1000,y:2000,scale:.4}]){
   const b=c.resolveOverviewFlightBridge({fromX:from.x,fromY:from.y,fromScale:from.scale,targetX:target.x,targetY:target.y,targetScale:target.scale,overviewCamera:overview,viewport:v});
   emit('bridge',-1,v,[...Object.values(from),...Object.values(target),...(overview?Object.values(overview):[0,0,-1])],b?Object.values(b):null);
  }
 }
 for(const duration of[-1,0,.01,.099999,.1,.100001,.12,.179999,.18,.3,.7,1.6,20])emit('retarget',-1,{width:0,height:0},[duration],[c.resolveCameraRetargetDuration({startTime:.7,endTime:.7+duration,words:[]})]);
 const dir=path.join(root,'verification/fume-fixtures');for(const[name,rows]of Object.entries({'camera-blocks':defs,'camera-rows':rowDefs,'camera-targets':out}))fs.writeFileSync(path.join(dir,name+'.tsv'),rows.join('\n')+'\n');
 console.log(JSON.stringify({revision,samples:out.length-2}));
}finally{await server.close();}
