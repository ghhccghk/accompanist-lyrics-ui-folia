// Actual original passed-text node, isolated from row/stack/Motion history.
import React from 'react';
import {createRoot} from 'react-dom/client';
import {flushSync} from 'react-dom';
import {CappellaText} from '../../.work/folia-major/src/components/visualizer/cappella/CappellaBubbleParts';
import latinUrl from '../assets/FoliaParityLatin-Bold.ttf?url';
import bidiUrl from '../assets/FoliaParityBidi-Bold.ttf?url';
import cjkUrl from '../assets/FoliaParityChatCJK-Bold.otf?url';
const fontUrls={'FoliaParityLatin-Bold.ttf':latinUrl,'FoliaParityBidi-Bold.ttf':bidiUrl,'FoliaParityChatCJK-Bold.otf':cjkUrl};
const cases=[
  {name:'italic',text:'fij Wavy',font:'FoliaParityLatin-Bold.ttf',italic:true},
  {name:'arabic',text:'مرحبا بالعالم',font:'FoliaParityBidi-Bold.ttf'},
  {name:'mixed',text:'Aمرحبا 12B بالعالم',font:'FoliaParityBidi-Bold.ttf'},
  {name:'latin',text:'office affine',font:'FoliaParityLatin-Bold.ttf'},
  {name:'cjk',text:'星光回响',font:'FoliaParityChatCJK-Bold.otf'}
];
const root=createRoot(document.querySelector('#root')!);
(window as any).draw=async(name:string)=>{
  const c=cases.find(c=>c.name===name)!;
  const font=new FontFace('Parity Complete',`url(${fontUrls[c.font]})`,{weight:'700'});
  await font.load();document.fonts.clear();document.fonts.add(font);
  flushSync(()=>root.render(<div style={{width:780,height:160,padding:'16px 20px',border:'1px solid transparent',
    borderRadius:'24px 24px 24px 6px',overflow:'hidden',whiteSpace:'pre-wrap',overflowWrap:'anywhere',
    fontFamily:'Parity Complete',fontSize:64,fontWeight:700,fontStyle:c.italic?'italic':'normal',
    lineHeight:1.45,color:'white',direction:'ltr',textAlign:'left'}}>
    <CappellaText message={{kind:'lyric',id:'owned-complete',line:{fullText:c.text,startTime:0,endTime:1,words:[]},lineIndex:0,side:'left',avatarIndex:0} as any}/>
  </div>));
  return {name,text:c.text,font:c.font,italic:!!c.italic,viewport:[800,300],fontPx:64,lineHeight:92.8,
    scope:'Actual original CappellaText in fixed host frame; passed whole text only, not full message-stage or active fade parity.'};
};
(window as any).ready=true;
