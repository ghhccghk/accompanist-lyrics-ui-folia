import React from'react';import{createRoot}from'react-dom/client';import{flushSync}from'react-dom';import{motionValue}from'framer-motion';
import Reference from'../../.work/pendolo-tick-stage-reference';
import{resolvePendoloFallbackAnchorIndex}from'../../.work/folia-major/src/components/visualizer/pendolo/pendoloTimeline';
import raw from'../cadenza-fixtures/sceneInputs.tsv?raw';
import latinUrl from'../assets/FoliaParityLatin-Bold.ttf?url';import arabicUrl from'../assets/FoliaParityBidi-Bold.ttf?url';import cjkUrl from'../assets/FoliaParityChatCJK-Bold.otf?url';
for(const[name,url]of[['Pendolo Latin',latinUrl],['Pendolo Arabic',arabicUrl],['Pendolo CJK',cjkUrl]]){const f=new FontFace(name,`url(${url})`,{weight:'700'});await f.load();document.fonts.add(f)}
const q=new URLSearchParams(location.search),scene=q.get('scene')??'latin',intensity=q.get('intensity')??'normal',dense=q.get('dense')==='1',step=dense?.16:4;
const decode=(s:string)=>new TextDecoder().decode(Uint8Array.from(atob(s),c=>c.charCodeAt(0)));
const f=raw.split(/\r?\n/).find(r=>r.split('\t')[0]===scene)!.split('\t');
const lines=Array.from({length:8},(_,i)=>({id:String(i),isChorus:i%2===0,fullText:decode(f[1]),startTime:+f[2]+i*step,endTime:+f[3]+i*step,
 words:f[4].split('|').map(s=>{const w=s.split('~');return{text:decode(w[0]),startTime:+w[1]+i*step,endTime:+w[2]+i*step}}),
 translation:scene==='arabic'?'بالعالم':scene==='cjk'?'星光':'beyond',romanization:scene==='arabic'?'مرحبا':scene==='cjk'?'向远方':'Amber echoes'}));
const lyricsFontScale=+(q.get('lyricsScale')??1),subtitleFontScale=+(q.get('subtitleScale')??1);
const family=scene==='arabic'?'Pendolo Arabic':scene==='cjk'?'Pendolo CJK':'Pendolo Latin';
const theme={fontFamily:family,fontWeight:700,primaryColor:'#71b8ed',secondaryColor:'#e2b66f',accentColor:'#93f1a1',animationIntensity:intensity,wordColors:scene==='cjk'?[{word:'向远方',color:'#8ab4f8'},{word:'星光',color:'#ef55ae'}]:[{word:'Amber',color:'#ed71b8'},{word:'echoes',color:'#73dea1'},{word:'beyond',color:'#8ab4f8'}]};
const tuning={arcRadius:.42,arcAngleDeg:100,wheelCenterX:0,wheelCenterY:.5,activeScale:1.25,tickSnappiness:2};
const root=createRoot(document.querySelector('#root')!),media=motionValue(0);let lastValid=0,observed=false,lastTarget=-1;
const render=(target:number,current:number)=>root.render(<Reference lines={lines} currentTime={media} currentLineIndex={current} targetLineIndex={target} theme={theme}
 viewportSize={{width:innerWidth,height:innerHeight}} pendoloTuning={tuning} lyricsFontScale={lyricsFontScale} subtitleFontScale={subtitleFontScale} resolvedMode="both"/>);
(window as any).advanceWheel=(time:number)=>{const current=lines.findLastIndex(l=>time>=l.startTime&&time<=l.endTime);
 if(current>=0){lastValid=current;observed=true}const target=resolvePendoloFallbackAnchorIndex(lines,current,lastValid,observed,time);
 flushSync(()=>{media.set(time);if(target!==lastTarget){render(target,current);lastTarget=target}});return{current,target};
};
const rect=(e:Element)=>{const r=e.getBoundingClientRect();return[r.x,r.y,r.width,r.height]};
(window as any).readChorusWheel=()=>{const wheel=document.querySelector('#pendolo-reference')!.firstElementChild!,tick=(window as any).tickSource;
 return{...window.stageModel,rotation:tick.wheelRotationDeg.get(),rows:[...wheel.children].map((row,i)=>{
  const parent=row.firstElementChild!.firstElementChild!.firstElementChild!,content=parent.lastElementChild!,halo=parent.children.length===2?parent.firstElementChild:null;
  const scale=(e:Element)=>new DOMMatrix(getComputedStyle(e).transform).a;
  const main=content.firstElementChild!.lastElementChild!,subtitles=[...content.querySelectorAll('[data-pendolo-subtitle-track]')];
  return{index:window.stageModel.items[i].index,content:rect(content),scale:scale(parent),font:parseFloat(getComputedStyle(main).fontSize),activeRows:window.stageModel.items[i].isActive?[...main.children].map(e=>e.firstElementChild!.textContent):null,subtitles:subtitles.map(e=>({font:parseFloat(getComputedStyle(e).fontSize),height:e.getBoundingClientRect().height})),
   halo:halo?{opacity:+getComputedStyle(halo).opacity,scale:scale(halo),rect:rect(halo),left:parseFloat(getComputedStyle(halo).left),top:parseFloat(getComputedStyle(halo).top),width:parseFloat(getComputedStyle(halo).width),height:parseFloat(getComputedStyle(halo).height),filter:getComputedStyle(halo).filter,background:getComputedStyle(halo).backgroundImage}:null};})};};
flushSync(()=>render(-1,-1));(window as any).chorusInputs={lines,tuning,scene,intensity,dense,theme,lyricsFontScale,subtitleFontScale};(window as any).ready=true;
