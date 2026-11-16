import { buildArticleLayout } from '../../.work/folia-major/src/components/visualizer/fume/fumeArticleLayout';
import { resolveFumeBlockTiming, resolveFumeStaticLayers, resolveFumeGlowBases } from '../../.work/folia-major/src/components/visualizer/fume/fumeBlockFrame';
import { drawFumeLiveBlock, createStaticBlockSnapshot } from '../../.work/folia-major/src/components/visualizer/fume/fumeCanvasText';
import fontUrl from '../assets/FoliaParityLatin-Bold.ttf?url';
const font = new FontFace('Fume Parity', `url(${fontUrl})`, { weight: '700' });
await font.load(); document.fonts.add(font);
const query = new URLSearchParams(location.search), scene=Number(query.get('scene')??0), glow=Number(query.get('glow')??1);
const w=(text:string,startTime:number,endTime:number,syllables:any[]=[])=>({text,startTime,endTime,syllables});
const lines=[
  {fullText:'Amber  echo',startTime:.7,endTime:2.8,words:[w('Amber',.7,1.6),w('echo',1.8,2.8)]},
  {fullText:'AB CD',startTime:.7,endTime:2.8,words:[w('AB ',.7,1.6,[w('A',.7,.8),w('B ',1.5,1.6)]),w('CD',1.8,2.8)]},
  {fullText:'An invented paragraph whose text wraps naturally. '.repeat(5)+'\nA final echo',startTime:.7,endTime:2.8,words:[]},
];
const line=lines[scene];
const theme={name:'FumeLiveFixture',fontStyle:'sans',fontFamily:'Fume Parity',fontWeight:700,primaryColor:'#dfe5ec',accentColor:'#f17b4d',animationIntensity:'normal',wordColors:[{word:'amber echo',color:'#30caff'}]};
const steps=buildArticleLayout([line],{width:innerWidth,height:600},theme,1,{heroScale:1});let s;do{s=steps.next()}while(!s.done);
const block=s.value!.blocks[0];
// Fixed translation, 1 world px per device px: isolate text from camera/CTM blur semantics.
block.x=32;block.y=150;
const canvas=document.querySelector('canvas')!;canvas.width=innerWidth;canvas.height=600;
const ctx=canvas.getContext('2d')!;
const bases=resolveFumeGlowBases(theme.animationIntensity,glow);
(window as any).fumeLive={scene,hero:block.variant==='hero',width:block.width,height:block.height,innerWidth:block.innerWidth,fontPx:block.fontPx,lineHeight:block.lineHeight,lineCount:block.renderLines.length,
  rows:block.renderLines.map(r=>({text:r.text,left:r.left,top:r.top,width:r.width,segments:r.segments.map(s=>({text:s.text,start:s.start,end:s.end,x:s.x,offsets:s.measuredGlyphOffsets}))}))};
(window as any).drawAt=(time:number)=>{
  ctx.fillStyle='#162435';ctx.fillRect(0,0,innerWidth,600);
  const timing=resolveFumeBlockTiming(block,time,null,.3);
  const layers=resolveFumeStaticLayers(block,timing,{time,theme,passedGlowBase:bases.passedGlowBase,passedFadeDuration:2.4,overviewTextRestoreProgress:0,snapshotScale:1},spec=>createStaticBlockSnapshot(block,theme,spec.fill,spec.shadowBlur,spec.shadowColor,1)??undefined);
  if(layers)for(const {snapshot,alpha} of layers){ctx.save();ctx.globalAlpha=alpha;ctx.drawImage(snapshot.canvas,block.x-snapshot.padding,block.y-snapshot.padding);ctx.restore();}
  else drawFumeLiveBlock(ctx,block,timing,{time,theme,glowIntensity:glow,...bases,showPrintStamp:true});
};
(window as any).drawAt(0);(window as any).ready=true;
