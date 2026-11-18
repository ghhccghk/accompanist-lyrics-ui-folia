import React from 'react';import {createRoot} from 'react-dom/client';import {flushSync} from 'react-dom';
import {ActiveCappellaText} from '../../.work/folia-major/src/components/visualizer/cappella/CappellaBubbleParts';
import latin from '../assets/FoliaParityLatin-Bold.ttf?url';import bidi from '../assets/FoliaParityBidi-Bold.ttf?url';
const cases=[
 {name:'fi',text:'fi',url:latin,font:'FoliaParityLatin-Bold.ttf'},
 {name:'fi-italic',text:'fi',url:latin,font:'FoliaParityLatin-Bold.ttf',italic:true},
 {name:'ffi',text:'ffi',url:latin,font:'FoliaParityLatin-Bold.ttf'},
 {name:'ffl',text:'ffl',url:latin,font:'FoliaParityLatin-Bold.ttf'},
 {name:'lam-alef',text:'لا',url:bidi,font:'FoliaParityBidi-Bold.ttf'},
 {name:'latin-separate',text:'fj',url:latin,font:'FoliaParityLatin-Bold.ttf'},
 {name:'arabic-separate',text:'مرح',url:bidi,font:'FoliaParityBidi-Bold.ttf'},
];
const root=createRoot(document.querySelector('#root')!);
(window as any).draw=async(name:string,owner:number)=>{
 const c=cases.find(c=>c.name===name)!;
 const font=new FontFace('Cluster Parity',`url(${c.url})`,{weight:'700'});await font.load();document.fonts.clear();document.fonts.add(font);
 const chars=[...new Intl.Segmenter('en',{granularity:'grapheme'}).segment(c.text)].map(x=>x.segment);
 const line={fullText:c.text,startTime:0,endTime:1,words:chars.map(text=>({text,startTime:0,endTime:1}))};
 flushSync(()=>root.render(<div key={`${name}/${owner}`} style={{width:780,height:160,padding:'16px 20px',border:'1px solid transparent',
  borderRadius:'24px 24px 24px 6px',overflow:'hidden',whiteSpace:'pre-wrap',overflowWrap:'anywhere',fontFamily:'Cluster Parity',
  fontSize:64,fontWeight:700,fontStyle:c.italic?'italic':'normal',lineHeight:1.45,color:'white',direction:'ltr',textAlign:'left'}}>
  <span data-content="true"><ActiveCappellaText line={line} visibleCharacterCount={chars.length}/></span>
 </div>));
 const spans=[...document.querySelectorAll<HTMLElement>('[data-content] > span > span')];
 for(const[i,span]of spans.entries())for(const a of span.getAnimations()) {
  a.pause();a.currentTime=owner<0||owner===i?1000:0;
 }
 return {name,text:c.text,font:c.font,italic:!!c.italic,owner,characters:chars,
  opacities:spans.map(s=>+getComputedStyle(s).opacity),rects:spans.map(s=>{const r=s.getBoundingClientRect();return [r.x,r.y,r.width,r.height]})};
};
(window as any).cases=cases.map(({name,text})=>({name,count:[...new Intl.Segmenter('en',{granularity:'grapheme'}).segment(text)].length}));
(window as any).ready=true;
