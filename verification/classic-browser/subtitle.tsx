import React from 'react';
import {createRoot} from 'react-dom/client';
import {flushSync} from 'react-dom';
import {MotionGlobalConfig,frameData,frameSteps,time as motionTime,flushKeyframeResolvers,visualElementStore} from 'framer-motion';
import Overlay,{resolveVisualizerSubtitleOverlayContent} from '../../.work/folia-major/src/components/visualizer/VisualizerSubtitleOverlay';
import {resolveSubtitleFontSizes} from '../../.work/folia-major/src/components/visualizer/subtitleFontSizes';
import fontUrl from '../assets/FoliaParityLatin-Bold.ttf?url';
import {getRecentCompletedLine,getSubtitleNextLines} from '../../.work/folia-major/src/components/visualizer/runtime';
import {getLineRenderEndTime} from '../../.work/folia-major/src/utils/lyrics/renderHints';
import sceneText from '../classic-fixtures/subtitle-scenes.tsv?raw';
import cjkSceneText from '../classic-fixtures/subtitle-cjk-scenes.tsv?raw';
import cjkFontUrl from '../assets/FoliaParityCJK-Bold.otf?url';
const cjk=new URLSearchParams(location.search).has('cjk');
  const lines=(cjk?cjkSceneText:sceneText).split(/\r?\n/).filter(r=>r&&!r.startsWith('#')).map(row=>{
    const [start,end,fullText,translation,romanization]=row.split('\t');
    return {startTime:Number(start)/1000,endTime:Number(end)/1000,fullText,translation,romanization,
      words:[{text:fullText,startTime:Number(start)/1000,endTime:Number(end)/1000}]};
  });
  const rows=[`# Original subtitle content/runtime, pinned cc6226e464050a456a2913e062374f23d3844cc5`,'# timeMs\tactive\trecent\tlayout\tvisible\trowsJSON\tupcomingJSON'];
  for(const ms of [0,699,700,1900,2140,2141,2500,3200,4400,4401,4800,5000,6400,7000,8300,9000,10300,12000]) {
    const currentLineIndex=lines.findIndex(l=>ms/1000>=l.startTime&&ms/1000<getLineRenderEndTime(l));
    const recent=getRecentCompletedLine({lines,currentLineIndex,currentTime:ms/1000,getLineEndTime:getLineRenderEndTime});
    const nextLines=getSubtitleNextLines(lines,currentLineIndex,recent);
    for(const layout of ['translation','romanization','current+translation+romanization','current+next+next','translation+romanization+next']) for(const showText of [true,false]) {
      const value=resolveVisualizerSubtitleOverlayContent({showText,activeLine:lines[currentLineIndex]??null,recentCompletedLine:recent,nextLines,subtitleOverlayLayout:layout});
      rows.push([ms,currentLineIndex,lines.indexOf(recent),layout,showText,
        JSON.stringify(value.rows.map(r=>[r.source,r.text])),JSON.stringify(value.upcomingLines.map(l=>lines.indexOf(l)))].join('\t'));
    }
  }

(window as any).contentFixtures=rows.join("\n")+"\n";

if(new URLSearchParams(location.search).has('paint')) {
  const font=new FontFace('Subtitle Parity',`url(${cjk?cjkFontUrl:fontUrl})`,{weight:'700'});
  await font.load();document.fonts.add(font);
  MotionGlobalConfig.useManualTiming=true;frameData.timestamp=0;motionTime.set(0);
  document.body.style.background='#162435';document.body.style.fontFamily='Subtitle Parity';
  document.body.style.lineHeight='1.5';
  const root=createRoot(document.getElementById('root')!);
  const params=new URLSearchParams(location.search);
  (window as any).step=(ms:number)=>{
    frameData.delta=ms-frameData.timestamp;frameData.timestamp=ms;motionTime.set(ms);
    const currentLineIndex=lines.findIndex(l=>ms/1000>=l.startTime&&ms/1000<getLineRenderEndTime(l));
    const recent=getRecentCompletedLine({lines,currentLineIndex,currentTime:ms/1000,getLineEndTime:getLineRenderEndTime});
    flushSync(()=>root.render(<Overlay showText={true} activeLine={lines[currentLineIndex]??null}
      recentCompletedLine={recent} nextLines={getSubtitleNextLines(lines,currentLineIndex,recent)}
      theme={{fontFamily:'Subtitle Parity',fontWeight:700,fontStyle:'sans',secondaryColor:'#adb9c8',backgroundColor:params.has('backdrop')||params.has('shadow')?'#48707e':'#162435'} as any}
      {...resolveSubtitleFontSizes(1)} subtitleOverlayLayout={(params.get('layout')??'translation') as any} subtitleOverlayBackground={params.has('backdrop')}/>));
    flushKeyframeResolvers();frameData.isProcessing=true;
    try {Object.values(frameSteps).forEach(step=>step.process(frameData));}finally {frameData.isProcessing=false;}
  };
  (window as any).motionValues=(el:Element)=>({...visualElementStore.get(el)?.latestValues,...Object.fromEntries(Array.from(visualElementStore.get(el)?.values.entries()??[]).map(([key,value])=>[key,value.get()]))});
  (window as any).step(0);(window as any).ready=true;
}
