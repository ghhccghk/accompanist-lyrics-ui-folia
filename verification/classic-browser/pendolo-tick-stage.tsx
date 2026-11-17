import React from'react';import{createRoot}from'react-dom/client';import{flushSync}from'react-dom';import{motionValue}from'framer-motion';
import Reference from'../../.work/pendolo-tick-stage-reference';
import{resolvePendoloFallbackAnchorIndex}from'../../.work/folia-major/src/components/visualizer/pendolo/pendoloTimeline';
import raw from'../cadenza-fixtures/sceneInputs.tsv?raw';
import latinUrl from'../assets/FoliaParityLatin-Bold.ttf?url';import arabicUrl from'../assets/FoliaParityBidi-Bold.ttf?url';import cjkUrl from'../assets/FoliaParityChatCJK-Bold.otf?url';
for(const[name,url]of[['Pendolo Latin',latinUrl],['Pendolo Arabic',arabicUrl],['Pendolo CJK',cjkUrl]]){const f=new FontFace(name,`url(${url})`,{weight:'700'});await f.load();document.fonts.add(f)}
const q=new URLSearchParams(location.search),scene=q.get('scene')??'latin',intensity=q.get('intensity')??'normal';
const decode=(s:string)=>new TextDecoder().decode(Uint8Array.from(atob(s),c=>c.charCodeAt(0)));
const f=raw.split(/\r?\n/).find(r=>r.split('\t')[0]===scene)!.split('\t');
const lines=Array.from({length:8},(_,i)=>({id:String(i),fullText:decode(f[1]),startTime:+f[2]+i*4,endTime:+f[3]+i*4,
 words:f[4].split('|').map(s=>{const w=s.split('~');return{text:decode(w[0]),startTime:+w[1]+i*4,endTime:+w[2]+i*4}}),
 translation:scene==='arabic'?'بالعالم':scene==='cjk'?'星光':'beyond',romanization:scene==='arabic'?'مرحبا':scene==='cjk'?'向远方':'Amber echoes'}));
const family=scene==='arabic'?'Pendolo Arabic':scene==='cjk'?'Pendolo CJK':'Pendolo Latin';
const theme={fontFamily:family,fontWeight:700,primaryColor:'#dfe5ec',secondaryColor:'#adb9c8',accentColor:'#f17b4d',animationIntensity:intensity};
const tuning={arcRadius:.42,arcAngleDeg:100,wheelCenterX:0,wheelCenterY:.5,activeScale:1.25,tickSnappiness:2};
const root=createRoot(document.querySelector('#root')!),media=motionValue(0);let lastValid=0,observed=false,lastTarget=-1;
const render=(target:number,current:number)=>root.render(<Reference lines={lines} currentTime={media} currentLineIndex={current} targetLineIndex={target} theme={theme}
 viewportSize={{width:innerWidth,height:innerHeight}} pendoloTuning={tuning} resolvedMode="both"/>);
(window as any).advanceWheel=(time:number)=>{const current=lines.findLastIndex(l=>time>=l.startTime&&time<=l.endTime);
 if(current>=0){lastValid=current;observed=true}const target=resolvePendoloFallbackAnchorIndex(lines,current,lastValid,observed,time);
 flushSync(()=>{media.set(time);if(target!==lastTarget){render(target,current);lastTarget=target}});return{current,target};
};
const rect=(e:Element)=>{const r=e.getBoundingClientRect();return[r.x,r.y,r.width,r.height]};
(window as any).readTickWheel=()=>{const wheel=document.querySelector('#pendolo-reference')!.firstElementChild!;const tick=(window as any).tickSource;
 return{...window.stageModel,value:tick.tickSpring.get(),velocity:tick.tickSpring.getVelocity(),rotation:tick.wheelRotationDeg.get(),correction:tick.textRotationCorrectionDeg.get(),
  rows:[...wheel.children].map((row,i)=>{const content=row.firstElementChild!.firstElementChild!.firstElementChild!.firstElementChild!,main=content.firstElementChild!.firstElementChild!;
   return{index:window.stageModel.items[i].index,opacity:+getComputedStyle(row).opacity,content:rect(content),mainMax:parseFloat(getComputedStyle(main).maxWidth),
    mainWidth:parseFloat(getComputedStyle(main).width),mainHeight:parseFloat(getComputedStyle(main).height),contentWidth:parseFloat(getComputedStyle(content).width),contentHeight:parseFloat(getComputedStyle(content).height)};})};};
flushSync(()=>render(-1,-1));(window as any).tickInputs={lines,tuning,scene,intensity};(window as any).ready=true;
