import React from 'react';import {createRoot} from 'react-dom/client';import{flushSync}from'react-dom';
import{motionValue}from'framer-motion';
import Cadenza from '../../.work/folia-major/src/components/visualizer/cadenza/VisualizerCadenza';
import{annotateLyricLines}from'../../.work/folia-major/src/utils/lyrics/renderHints';
import{buildPreparedState}from'../../.work/folia-major/src/components/visualizer/cadenza/cadenzaPreparedState';
import raw from '../cadenza-fixtures/sceneInputs.tsv?raw';
import latinUrl from '../assets/FoliaParityLatin-Bold.ttf?url';
import arabicUrl from '../assets/NotoSansArabic-Bold.ttf?url';
import cjkUrl from '../assets/FoliaParityCJK-Bold.otf?url';
for(const[name,url]of[['Cadenza Latin',latinUrl],['Cadenza Arabic',arabicUrl],['Cadenza CJK',cjkUrl]]){
 const font=new FontFace(name,`url(${url})`,{weight:'700'});await font.load();document.fonts.add(font);
}
const query=new URLSearchParams(location.search),scene=query.get('scene')??'latin',glow=Number(query.get('glow')??1);
const fields=raw.split(/\r?\n/).find(r=>r.split('\t')[0]===scene)!.split('\t');
const decode=(v:string)=>new TextDecoder().decode(Uint8Array.from(atob(v),c=>c.charCodeAt(0)));
const line={fullText:decode(fields[1]),startTime:+fields[2],endTime:+fields[3],words:fields[4].split('|').map(s=>{const w=s.split('~');return{text:decode(w[0]),startTime:+w[1],endTime:+w[2]}})};
if(query.get('chorus')==='1')(line as any).isChorus=true;
const lines=annotateLyricLines([line]);
const theme={fontStyle:'sans',fontFamily:scene==='arabic'?'Cadenza Arabic':scene==='cjk'?'Cadenza CJK':'Cadenza Latin',
 fontWeight:700,animationIntensity:'normal',primaryColor:'#dfe5ec',accentColor:'#f17b4d',secondaryColor:'#adb9c8',wordColors:[] as {word:string,color:string}[]};
if(query.get('palette')==='alternate')Object.assign(theme,{primaryColor:'#8ab3df',accentColor:'#d6f456',wordColors:[
 {word:'Amber amber',color:'#5bcefa'},{word:'echoes echo',color:'#f5a9b8'},{word:'向远方',color:'#5bcefa'},{word:'星光 回响',color:'#f5a9b8'}]});
const tuning={fontScale:1.12,widthRatio:.72,motionAmount:1,glowIntensity:glow,beamIntensity:0,render:{scale:'global',antialias:'global'}};
const currentTime=motionValue(.7),audioPower=motionValue(0);const root=createRoot(document.querySelector('#root')!);
flushSync(()=>root.render(<Cadenza currentTime={currentTime} currentLineIndex={0} lines={lines} theme={theme} audioPower={audioPower} audioBands={{}} cadenzaTuning={tuning}/>));
(window as any).settleAt=(t:number)=>{
 flushSync(()=>currentTime.set(t));
 for(let i=0;i<210;i++)(window as any).advanceSourceFrame();
 return [...document.querySelectorAll('div[aria-hidden="true"]')].filter(el=>(el as HTMLElement).style.transform.startsWith('translate3d')).map(el=>({
  text:el.querySelector('span')?.textContent,outer:(el as HTMLElement).style.transform,
  inner:(el.firstElementChild as HTMLElement).style.transform,body:(el.querySelector('span')as HTMLElement).style.cssText}));
};
(window as any).prepared=buildPreparedState(lines[0],{showText:true,viewport:{width:innerWidth,height:600},theme,tuning});
(window as any).advanceAt=(t:number,dt:number)=>{
 flushSync(()=>currentTime.set(t));(window as any).advanceSourceFrame(dt);
 return Object.fromEntries((window as any).observedCadenza);
};
(window as any).ready=true;
