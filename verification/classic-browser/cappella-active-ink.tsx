import React from 'react';import {createRoot} from 'react-dom/client';import {flushSync} from 'react-dom';
import {ActiveCappellaText} from '../../.work/folia-major/src/components/visualizer/cappella/CappellaBubbleParts';
import {getCharacterRevealPlan,getCharacterCountAtTime,buildCharacterRevealTimes} from '../../.work/folia-major/src/components/visualizer/cappella/cappellaReveal';
import latinUrl from '../assets/FoliaParityLatin-Bold.ttf?url';
import bidiUrl from '../assets/FoliaParityBidi-Bold.ttf?url';
import cjkUrl from '../assets/FoliaParityChatCJK-Bold.otf?url';
const cases=[
 {name:'italic',text:'fij Wavy',font:'FoliaParityLatin-Bold.ttf',url:latinUrl,italic:true},
 {name:'arabic',text:'مرحبا بالعالم',font:'FoliaParityBidi-Bold.ttf',url:bidiUrl},
 {name:'mixed',text:'Aمرحبا 12B بالعالم',font:'FoliaParityBidi-Bold.ttf',url:bidiUrl},
 {name:'latin',text:'office affine',font:'FoliaParityLatin-Bold.ttf',url:latinUrl},
 {name:'cjk',text:'星光回响',font:'FoliaParityChatCJK-Bold.otf',url:cjkUrl}
];
const root=createRoot(document.querySelector('#root')!);
(window as any).draw=async(name:string,time:number,uniform:boolean)=>{
 const c=cases.find(c=>c.name===name)!;
 const font=new FontFace('Parity Active',`url(${c.url})`,{weight:'700'});await font.load();document.fonts.clear();document.fonts.add(font);
 // All fixture strings consist of single-codepoint EGCs. Use Segmenter anyway;
 // production and source reveal logic remain responsible for their own EGCs.
 const chars=[...new Intl.Segmenter('en',{granularity:'grapheme'}).segment(c.text)].map(x=>x.segment);
 const line={fullText:c.text,startTime:0,endTime:1,words:chars.map((text,i)=>({text,
  startTime:uniform?0:i/chars.length*.8,endTime:uniform?1:(i+1)/chars.length*.8}))};
 const plan=getCharacterRevealPlan(line),revealTimes=buildCharacterRevealTimes(line,plan.characters),count=getCharacterCountAtTime(revealTimes,time);
 flushSync(()=>root.render(<div key={`${name}/${time}/${uniform}`} style={{width:780,height:160,padding:'16px 20px',border:'1px solid transparent',
  borderRadius:'24px 24px 24px 6px',overflow:'hidden',whiteSpace:'pre-wrap',overflowWrap:'anywhere',fontFamily:'Parity Active',
  fontSize:64,fontWeight:700,fontStyle:c.italic?'italic':'normal',lineHeight:1.45,color:'white',direction:'ltr',textAlign:'left'}}>
  <span data-content="true"><ActiveCappellaText line={line} visibleCharacterCount={count}/></span>
 </div>));
 for(const[i,span] of [...document.querySelectorAll('[data-content] > span > span')].entries())for(const a of span.getAnimations()){
  a.pause();a.currentTime=Math.max(0,(time-revealTimes[i])*1000);
 }
 return {name,text:c.text,font:c.font,italic:!!c.italic,time,uniform,count,characters:plan.characters,revealTimes,
  opacities:[...document.querySelectorAll('[data-content] > span > span')].map(x=>+getComputedStyle(x).opacity)};
};
(window as any).ready=true;
