import React from'react';import{createRoot}from'react-dom/client';import{flushSync}from'react-dom';import{motionValue}from'framer-motion';
import Cappella from'../../.work/folia-major/src/components/visualizer/cappella/VisualizerCappella';
import{annotateLyricLines}from'../../.work/folia-major/src/utils/lyrics/renderHints';
import raw from'../cadenza-fixtures/sceneInputs.tsv?raw';
import latinUrl from'../assets/FoliaParityLatin-Bold.ttf?url';import arabicUrl from'../assets/FoliaParityBidi-Bold.ttf?url';import cjkUrl from'../assets/FoliaParityChatCJK-Bold.otf?url';
for(const[name,url]of[['Cappella Latin',latinUrl],['Cappella Arabic',arabicUrl],['Cappella CJK',cjkUrl]]){const f=new FontFace(name,`url(${url})`,{weight:'700'});await f.load();document.fonts.add(f)}
const q=new URLSearchParams(location.search),scene=q.get('scene')??'latin',intensity=q.get('intensity')??'normal';
const decode=(s:string)=>new TextDecoder().decode(Uint8Array.from(atob(s),c=>c.charCodeAt(0)));
const f=raw.split(/\r?\n/).find(r=>r.split('\t')[0]===scene)!.split('\t');
const lines=annotateLyricLines(Array.from({length:8},(_,i)=>({fullText:decode(f[1]),startTime:+f[2]+i*4,endTime:+f[3]+i*4,
 words:f[4].split('|').map(s=>{const w=s.split('~');return{text:decode(w[0]),startTime:+w[1]+i*4,endTime:+w[2]+i*4}})})));
const theme={fontFamily:scene==='arabic'?'Cappella Arabic':scene==='cjk'?'Cappella CJK':'Cappella Latin',fontWeight:700,fontStyle:'sans',animationIntensity:intensity,
 primaryColor:'#dfe5ec',accentColor:'#f17b4d',secondaryColor:'#adb9c8',backgroundColor:'#162435'};
const root=createRoot(document.querySelector('#root')!);let media:any;
(window as any).mountStage=(t:number)=>{
 media=motionValue(t);const current=lines.findLastIndex(l=>t>=l.startTime&&t<=l.renderHints!.renderEndTime);
 flushSync(()=>root.render(<Cappella key={t} currentTime={media} currentLineIndex={current} lines={lines} theme={theme} songTitle="Synthetic title" audioPower={motionValue(0)} audioBands={{}}
  cappellaTuning={{showEmoMessages:false,avatarSource:'color',emojiPackSource:'builtin'}}/>));return current;
};
(window as any).readStage=()=>{
 const panel=document.querySelector('.max-w-4xl')!;const rect=(e:Element)=>{const r=e.getBoundingClientRect();return[r.x,r.y,r.width,r.height]};
 return {panel:rect(panel),rows:[...panel.children].map(row=>{
  const group=row.firstElementChild!,avatar=group.firstElementChild!,frame=group.children[1],bubble=frame.firstElementChild!;
  return{row:rect(row),group:rect(group),avatar:rect(avatar),bubble:rect(bubble),text:bubble.textContent,scale:(group as HTMLElement).style.transform,opacity:getComputedStyle(group).opacity};
 })};
};
(window as any).ready=true;
