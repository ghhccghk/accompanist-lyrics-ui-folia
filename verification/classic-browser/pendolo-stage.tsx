import React from'react';import{createRoot}from'react-dom/client';import{flushSync}from'react-dom';import{motionValue}from'framer-motion';
import Reference from'../../.work/pendolo-stage-reference';
import raw from'../cadenza-fixtures/sceneInputs.tsv?raw';
import latinUrl from'../assets/FoliaParityLatin-Bold.ttf?url';import arabicUrl from'../assets/FoliaParityBidi-Bold.ttf?url';import cjkUrl from'../assets/FoliaParityChatCJK-Bold.otf?url';
for(const[name,url]of[['Pendolo Latin',latinUrl],['Pendolo Arabic',arabicUrl],['Pendolo CJK',cjkUrl]]){const f=new FontFace(name,`url(${url})`,{weight:'700'});await f.load();document.fonts.add(f)}
const q=new URLSearchParams(location.search),scene=q.get('scene')??'latin';
const decode=(s:string)=>new TextDecoder().decode(Uint8Array.from(atob(s),c=>c.charCodeAt(0)));
const f=raw.split(/\r?\n/).find(r=>r.split('\t')[0]===scene)!.split('\t');
const lines=Array.from({length:8},(_,i)=>({id:String(i),fullText:decode(f[1]),startTime:+f[2]+i*4,endTime:+f[3]+i*4,
 words:f[4].split('|').map(s=>{const w=s.split('~');return{text:decode(w[0]),startTime:+w[1]+i*4,endTime:+w[2]+i*4}}),
 translation:scene==='arabic'?'بالعالم':scene==='cjk'?'星光':'beyond',romanization:scene==='arabic'?'مرحبا':scene==='cjk'?'向远方':'Amber echoes'}));
const family=scene==='arabic'?'Pendolo Arabic':scene==='cjk'?'Pendolo CJK':'Pendolo Latin';
const theme={fontFamily:family,fontWeight:700,primaryColor:'#dfe5ec',secondaryColor:'#adb9c8',accentColor:'#f17b4d',animationIntensity:'normal'};
const tuning={arcRadius:.42,arcAngleDeg:100,wheelCenterX:0,wheelCenterY:.5,activeScale:1.25};
const root=createRoot(document.querySelector('#root')!);let serial=0;
(window as any).mountWheel=(time:number,target:number,mode:string)=>flushSync(()=>root.render(<Reference key={serial++} lines={lines} currentTime={motionValue(time)} currentLineIndex={Number.isInteger(target)&&target>=0&&target<lines.length?target:-1}
 targetLineIndex={target} theme={theme} viewportSize={{width:innerWidth,height:innerHeight}} pendoloTuning={tuning} resolvedMode={mode}/>));
const rect=(e:Element)=>{const r=e.getBoundingClientRect();return[r.x,r.y,r.width,r.height]};
(window as any).readWheel=()=>{const wheel=document.querySelector('#pendolo-reference')!.firstElementChild!;return{...window.stageModel,rows:[...wheel.children].map(row=>{
 const corrected=row.firstElementChild!,inner=corrected.firstElementChild!,content=inner.firstElementChild!.firstElementChild!;
 return{outer:rect(row),inner:rect(inner),content:rect(content),untransformedHeight:(inner as HTMLElement).offsetHeight,
  contentHeight:(content as HTMLElement).offsetHeight,computedHeight:parseFloat(getComputedStyle(content).height),
  computedWidth:parseFloat(getComputedStyle(inner).width),opacity:getComputedStyle(row).opacity,
  subtitles:[...row.querySelectorAll('[data-pendolo-subtitle-track]')].map(s=>({role:s.getAttribute('data-pendolo-subtitle-track'),rect:rect(s),height:(s as HTMLElement).offsetHeight,text:s.textContent}))};})}};
(window as any).wheelInputs={lines,tuning,scene};(window as any).ready=true;
