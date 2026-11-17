// Original complete component + original canvas hook. Only host props and passive
// observations live here; background opacity0, subtitles/shell outside text scope.
import React from 'react';import{createRoot}from'react-dom/client';import{flushSync}from'react-dom';import{motionValue}from'framer-motion';
import Fume from '../../.work/folia-major/src/components/visualizer/fume/VisualizerFume';
import{annotateLyricLines}from'../../.work/folia-major/src/utils/lyrics/renderHints';
import{FirstFrameContext}from'../../.work/folia-major/src/components/visualizer/styleTransition/firstFrame';
import fontUrl from '../assets/FoliaParityLatin-Bold.ttf?url';
const font=new FontFace('Fume Parity',`url(${fontUrl})`,{weight:'700'});await font.load();document.fonts.add(font);
const word=(text:string,startTime:number,endTime:number)=>({text,startTime,endTime});
const lines=annotateLyricLines([
 {fullText:'Amber echoes beyond',startTime:.7,endTime:2.8,words:[word('Amber ',.7,1.2),word('echoes ',1.2,2),word('beyond',2,2.8)]},
 {fullText:'A second phrase',startTime:3,endTime:5,words:[]},
 {fullText:'Quiet afterglow',startTime:5.4,endTime:7.6,words:[]},
 {fullText:'An invented paragraph whose text wraps naturally. '.repeat(5)+'\nA final echo',startTime:8,endTime:11,words:[]}]);
const q=new URLSearchParams(location.search),stepped=q.get('stepped')==='true',glow=Number(q.get('glow')??1);
const theme={name:'FumeSceneFixture',fontFamily:'Fume Parity',fontWeight:700,fontStyle:'sans',animationIntensity:'normal',
 primaryColor:'#dfe5ec',accentColor:'#f17b4d',secondaryColor:'#adb9c8',backgroundColor:'#162435',wordColors:[{word:'amber echo',color:'#30caff'}]};
const media=motionValue(0),power=motionValue(0);const bands=Object.fromEntries(['bass','lowMid','mid','vocal','treble'].map(k=>[k,motionValue(0)]));
const root=createRoot(document.querySelector('#root')!);let lastCurrent=-1;
const report={claim:()=>{},drawn:()=>{(window as any).sourceArticleReady=true}};
const render=(i:number)=>root.render(<FirstFrameContext.Provider value={report}><Fume currentTime={media} currentLineIndex={i} lines={lines} theme={theme}
 audioPower={power} audioBands={bands} paused={false} isPreviewMode={true} hideTranslationSubtitle={true}
 fumeTuning={{backgroundObjectOpacity:0,disableGeometricBackground:true,textHoldRatio:.3,cameraSpeed:1,cameraTrackingMode:stepped?'stepped':'smooth',glowIntensity:glow,heroScale:1,hidePrintSymbols:false}}/></FirstFrameContext.Provider>);
flushSync(()=>render(-1));
(window as any).advanceMedia=(t:number)=>{
 const current=lines.findLastIndex(l=>t>=l.startTime&&t<=l.renderHints!.renderEndTime);
 flushSync(()=>{media.set(t);if(current!==lastCurrent){render(current);lastCurrent=current}});
};
function originalScene(){
 const canvas=document.querySelector('canvas')!;let fiber=(canvas as any)[Object.keys(canvas).find(k=>k.startsWith('__reactFiber$'))!];
 while(fiber&&fiber.type?.name!=='VisualizerFume')fiber=fiber.return;
 if(!fiber)throw Error('Original VisualizerFume fiber not found');
 let driver:any,scene:any;
 for(let h=fiber.memoizedState;h;h=h.next){const value=Array.isArray(h.memoizedState)?h.memoizedState[0]:h.memoizedState;
  if(value?.cameraState&&value?.getLineIndex)driver=value;if(value?.article&&value?.overviewCamera)scene=value;
 }
 if(!driver||!scene)throw Error('Original scene/driver memo not ready');
 return {canvas,driver,scene};
}
// Passive serialization only; no source camera, layout or clock mutation.
(window as any).exportFumeScene=()=>{const {scene}=originalScene();return JSON.parse(JSON.stringify(scene.article))};
(window as any).readFume=()=>{
 const {canvas,driver,scene}=originalScene();
 const parent=canvas.parentElement!;const rect=parent.getBoundingClientRect();
 return {camera:{...driver.cameraState.camera},retarget:{...driver.cameraState.retarget},parentTransform:parent.style.transform,
  parentScale:rect.width/innerWidth,canvasSize:[canvas.width,canvas.height],blocks:scene.article.blocks.length,firstStart:scene.article.firstRenderableStartTime,
  overviewStart:scene.overviewStartTime,layout:scene.article.blocks.map((b:any)=>({index:b.sourceLineIndex,x:b.x,y:b.y,width:b.width,height:b.height}))};
};
(window as any).ready=true;
