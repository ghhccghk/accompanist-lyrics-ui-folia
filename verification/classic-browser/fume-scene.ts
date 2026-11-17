// Test shell only; actual article, camera and Canvas text functions are unmodified upstream.
import { buildArticleLayout } from '../../.work/folia-major/src/components/visualizer/fume/fumeArticleLayout';
import { resolveArticleOverviewCamera } from '../../.work/folia-major/src/components/visualizer/fume/fumeCamera';
import { createFumeCameraState, syncFumeCameraToArticle, stepFumeCamera } from '../../.work/folia-major/src/components/visualizer/fume/fumeCameraStep';
import { resolveFumeBlockTiming, resolveFumeStaticLayers, resolveFumeGlowBases, isFumeBlockOnScreen } from '../../.work/folia-major/src/components/visualizer/fume/fumeBlockFrame';
import { drawFumeLiveBlock, createStaticBlockSnapshot } from '../../.work/folia-major/src/components/visualizer/fume/fumeCanvasText';
import { getLineRenderEndTime } from '../../.work/folia-major/src/utils/lyrics/renderHints';
import { resolveFumePassedFadeDuration } from '../../.work/folia-major/src/components/visualizer/fume/fumeTextStyle';
import fontUrl from '../assets/FoliaParityLatin-Bold.ttf?url';
const font = new FontFace('Fume Parity', `url(${fontUrl})`, { weight: '700' });
await font.load(); document.fonts.add(font);
const query = new URLSearchParams(location.search), glow=Number(query.get('glow')??1), stepped=query.get('stepped')==='true';
const word=(text:string,startTime:number,endTime:number)=>({text,startTime,endTime});
const lines=[
 {fullText:'Amber echoes beyond',startTime:.7,endTime:2.8,words:[word('Amber ',.7,1.2),word('echoes ',1.2,2),word('beyond',2,2.8)]},
 {fullText:'A second phrase',startTime:3,endTime:5,words:[]},
 {fullText:'Quiet afterglow',startTime:5.4,endTime:7.6,words:[]},
 {fullText:'An invented paragraph whose text wraps naturally. '.repeat(5)+'\nA final echo',startTime:8,endTime:11,words:[]},
];
const theme={name:'FumeSceneFixture',fontStyle:'sans',fontFamily:'Fume Parity',fontWeight:700,primaryColor:'#dfe5ec',accentColor:'#f17b4d',animationIntensity:'normal',wordColors:[{word:'amber echo',color:'#30caff'}]};
const viewport={width:innerWidth,height:600};
const steps=buildArticleLayout(lines,viewport,theme,1,{heroScale:1});let s;do{s=steps.next()}while(!s.done);
const article=s.value!, overview=resolveArticleOverviewCamera(article,viewport), overviewStart=8+(getLineRenderEndTime(lines[3])-8)*.5;
const focusAt=(time:number)=>lines.findLastIndex(l=>l.startTime<=time&&time<l.endTime);
const epochs=[...new Set([0,...lines.flatMap(l=>[l.startTime,l.endTime])])].sort((a,b)=>a-b).map(time=>({time,lineIndex:focusAt(time)}));
let camera=createFumeCameraState(), frame=-1, lastRestore=0;
const canvas=document.querySelector('canvas')!;canvas.width=innerWidth;canvas.height=600;const ctx=canvas.getContext('2d')!;
const bases=resolveFumeGlowBases(theme.animationIntensity,glow);
(window as any).fumeScene={overviewStart,epochs,article:{width:article.width,height:article.height},
 blocks:article.blocks.map(b=>({index:b.sourceLineIndex,x:b.x,y:b.y,width:b.width,height:b.height,fontPx:b.fontPx,
  rows:b.renderLines.map(r=>({start:r.start,end:r.end,left:r.left,top:r.top,width:r.width,offsets:r.glyphOffsets}))}))};
(window as any).drawAt=(time:number)=>{
 const target=Math.round(time*60);if(Math.abs(target/60-time)>1e-9)throw Error('Capture must coincide with an original source frame');
 if(target<frame){camera=createFumeCameraState();frame=-1;lastRestore=0;}
 let restore=lastRestore;
 if(frame<0)syncFumeCameraToArticle(camera,article);
 for(let f=frame+1;f<=target;f++){
  const time=f/60;
  restore=stepFumeCamera(camera,{article,time,now:(time+9.25)*1000,dt:f===0?0:1/60,lineIndex:focusAt(time),viewport,
   overviewCamera:overview,overviewStartTime:overviewStart,cameraSpeed:1,cameraTrackingMode:stepped?'stepped':'smooth',staticMode:false,animationIntensity:'normal'}).overviewTextRestoreProgress;
 }
 // Repeated time is a frozen observation, never another source step.
 frame=target;lastRestore=restore;
 ctx.setTransform(1,0,0,1,0,0);ctx.fillStyle='#162435';ctx.fillRect(0,0,innerWidth,600);
 ctx.save();ctx.translate(innerWidth/2,300);ctx.scale(camera.camera.scale,camera.camera.scale);ctx.translate(-camera.camera.x,-camera.camera.y);
 for(const block of article.blocks){
  if(!isFumeBlockOnScreen(block,camera.camera,viewport))continue;
  const timing=resolveFumeBlockTiming(block,time,lines[block.sourceLineIndex+1]?.startTime??null,.3);
  const layers=resolveFumeStaticLayers(block,timing,{time,theme,passedGlowBase:bases.passedGlowBase,passedFadeDuration:resolveFumePassedFadeDuration(lines,.3),overviewTextRestoreProgress:restore,snapshotScale:1},
   spec=>createStaticBlockSnapshot(block,theme,spec.fill,spec.shadowBlur,spec.shadowColor,1)??undefined);
  if(layers)for(const {snapshot,alpha}of layers){ctx.save();ctx.globalAlpha=alpha;ctx.drawImage(snapshot.canvas,block.x-snapshot.padding,block.y-snapshot.padding,block.width+snapshot.padding*2,block.height+snapshot.padding*2);ctx.restore();}
  else drawFumeLiveBlock(ctx,block,timing,{time,theme,glowIntensity:glow,...bases,showPrintStamp:true});
 }
 ctx.restore();return{camera:camera.camera,restore};
};
(window as any).ready=true;
