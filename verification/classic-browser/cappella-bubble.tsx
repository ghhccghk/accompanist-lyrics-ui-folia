// Isolate ORIGINAL bubble/text components, not a reimplementation of their drawing.
// Parent message-stack/springs/shadow/glow are outside this calibration seam.
import React from'react';import{createRoot}from'react-dom/client';import{flushSync}from'react-dom';
import{AnimatedBubbleFrame,ActiveCappellaText}from'../../.work/folia-major/src/components/visualizer/cappella/CappellaBubbleParts';
import{getOrBuildBubbleMetrics}from'../../.work/folia-major/src/components/visualizer/cappella/cappellaBubbleMetrics';
import{getCharacterCountAtTime,buildCharacterFadeDurationsMs,getLineCharacters}from'../../.work/folia-major/src/components/visualizer/cappella/cappellaReveal';
import{getBubbleColors}from'../../.work/folia-major/src/components/visualizer/cappella/cappellaMessageLayout';
import raw from'../cadenza-fixtures/sceneInputs.tsv?raw';
import latinUrl from'../assets/FoliaParityLatin-Bold.ttf?url';import arabicUrl from'../assets/NotoSansArabic-Bold.ttf?url';import cjkUrl from'../assets/FoliaParityCJK-Bold.otf?url';
for(const[name,url]of[['Cappella Latin',latinUrl],['Cappella Arabic',arabicUrl],['Cappella CJK',cjkUrl]]){const f=new FontFace(name,`url(${url})`,{weight:'700'});await f.load();document.fonts.add(f)}
const q=new URLSearchParams(location.search),scene=q.get('scene')??'latin',maxTextWidth=+(q.get('textWidth')??160),side=q.get('side')??'left';
const decode=(s:string)=>new TextDecoder().decode(Uint8Array.from(atob(s),c=>c.charCodeAt(0)));
const f=raw.split(/\r?\n/).find(r=>r.split('\t')[0]===scene)!.split('\t');
const line={fullText:decode(f[1]),startTime:+f[2],endTime:+f[3],words:f[4].split('|').map(s=>{const w=s.split('~');return{text:decode(w[0]),startTime:+w[1],endTime:+w[2]}})};
const theme={fontFamily:scene==='arabic'?'Cappella Arabic':scene==='cjk'?'Cappella CJK':'Cappella Latin',fontWeight:700,fontStyle:'sans',
 primaryColor:'#dfe5ec',accentColor:'#f17b4d',secondaryColor:'#adb9c8',backgroundColor:'#162435'};
const message={kind:'lyric',id:'test',line,lineIndex:0,side,avatarIndex:side==='right'?8:2}as any;
const colors=getBubbleColors(message,theme),fontSize=24.12,lineHeightPx=fontSize*1.45,paddingX=20,paddingY=16;
const metrics=getOrBuildBubbleMetrics(new Map(),{line,theme,fontSize,lineHeightPx,maxTextWidth,paddingX,paddingY});
const root=createRoot(document.querySelector('#root')!);
(window as any).drawBubble=(t:number)=>{
 const count=getCharacterCountAtTime(metrics.revealTimes,t),target=getCharacterCountAtTime(metrics.bubbleTargetTimes,t);
 // Mount at the exact target size to isolate text/frame from the source .2s resize transition.
 flushSync(()=>root.render(<div key={t} style={{position:'absolute',left:16,top:32,fontFamily:theme.fontFamily}}>
  <AnimatedBubbleFrame className={`relative rounded-3xl ${side==='right'?'rounded-br-md':'rounded-bl-md'}`} targetSize={metrics.sizes[target]}
   style={{backgroundColor:colors.backgroundColor,border:`1px solid ${colors.borderColor}`,color:colors.textColor,fontSize,fontWeight:700,lineHeight:1.45,
    maxWidth:maxTextWidth+paddingX*2+2,minHeight:Math.max(64,lineHeightPx+paddingY*2),minWidth:72,padding:`${paddingY}px ${paddingX}px`,whiteSpace:'pre-wrap',overflowWrap:'anywhere'}}>
   <span data-content="true"><ActiveCappellaText line={line} visibleCharacterCount={count}/></span>
  </AnimatedBubbleFrame></div>));
 // Native freezes/reconstructs CSS fades on media time. Apply the same media
 // phase to ORIGINAL CSS animations for this isolated comparison, not to pretend
 // original wall-clock pause/seek behavior is media driven.
 const fades=buildCharacterFadeDurationsMs(line,getLineCharacters(line));
 for(const[i,span]of[...document.querySelectorAll('[data-content] > span > span')].entries())for(const a of span.getAnimations()){
  a.pause();a.currentTime=Math.max(0,(t-metrics.revealTimes[i])*1000);
 }
 return {count,target,sizes:metrics.sizes,reveal:metrics.revealTimes,fade:fades,colors,
  opacity:[...document.querySelectorAll('[data-content] > span > span')].map(s=>+getComputedStyle(s).opacity),
  rect:(()=>{const r=document.querySelector('[data-content]')!.parentElement!.getBoundingClientRect();return{x:r.x,y:r.y,width:r.width,height:r.height}})()};
};
(window as any).ready=true;
