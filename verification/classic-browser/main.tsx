import React from 'react';
import { createRoot } from 'react-dom/client';
import { flushSync } from 'react-dom';
import { motionValue, MotionGlobalConfig, frameData, frameSteps, time as motionTime, flushKeyframeResolvers, visualElementStore } from 'framer-motion';
import Classic from 'pinned-classic';
import { getLineRenderEndTime } from '../../.work/folia-major/src/utils/lyrics/renderHints';
import { setGlowBlurQuantized } from '../../.work/folia-major/src/utils/glowBlurQuantize';
import {resolveGlowWordRenderProfile,getGlowWordActiveEndTime} from '../../.work/folia-major/src/components/visualizer/glowWordTiming';
import {buildPostLyricLayoutUnits,buildDisplayWordsFromLayoutUnits} from '../../.work/folia-major/src/utils/lyrics/cjkSemanticLayout';
import fontUrl from '../assets/FoliaParityLatin-Bold.ttf?url';
import arabicUrl from '../assets/NotoSansArabic-Bold.ttf?url';
import cjkUrl from '../assets/FoliaParityCJK-Bold.otf?url';
import scenes from '../classic-fixtures/scenes.tsv?raw';

// Windows text-shadow branch is the baseline. No source patches and no remote media.
setGlowBlurQuantized(false);
const face = new FontFace('Parity Noto', `url(${fontUrl})`, { weight: '700' });
await face.load(); document.fonts.add(face);
const arabicFace = new FontFace('Parity Arabic', `url(${arabicUrl})`, {weight:'700'});
await arabicFace.load();document.fonts.add(arabicFace);
const cjkFace = new FontFace('Parity CJK', `url(${cjkUrl})`, {weight:'700'});
await cjkFace.load();document.fonts.add(cjkFace);
const params = new URLSearchParams(location.search);
const manual = params.has('manual');
if(manual) {MotionGlobalConfig.useManualTiming=true;frameData.timestamp=0;motionTime.set(0);}
const currentTime = motionValue(0);
const audioPower = motionValue(0);
const themes = { animationIntensity: 'normal', fontStyle: 'sans', fontFamily: 'Parity Noto', fontWeight: 700,
  primaryColor: '#dfe5ec', accentColor: '#ffffff', secondaryColor: '#adb9c8', wordColors: [] };
let lines = [
  { startTime: .7, endTime: 1.9, fullText: 'Amber echo', words: [
    { text: 'Amber ', startTime: .7, endTime: 1.3 }, { text: 'echo', startTime: 1.3, endTime: 1.9 }] },
  { startTime: 3.2, endTime: 4.1, fullText: 'Silver light', words: [
    { text: 'Silver ', startTime: 3.2, endTime: 3.65 }, { text: 'light', startTime: 3.65, endTime: 4.1 }] },
];
if(params.has('scene')) {
  const rows=scenes.split(/\r?\n/).filter(r=>!r.startsWith('#')&&r.split('\t')[0]===params.get('scene')).map(r=>r.split('\t'));
  if(!rows.length) throw new Error('Unknown scene');
  const words=rows.map(r=>({text:r[5],startTime:Number(r[3])/1000,endTime:Number(r[4])/1000}));
  lines=[{startTime:Number(rows[0][1])/1000,endTime:Number(rows[0][2])/1000,fullText:words.map(w=>w.text).join(''),words}];
  if(params.get('scene')==='arabic') themes.fontFamily='Parity Arabic';
  if(params.get('scene')==='cjk') themes.fontFamily='Parity CJK';
}
const root = createRoot(document.getElementById('root')!);
(window as any).mountTime=performance.now();
(window as any).motionValues=(element:Element)=>{
  const visual=visualElementStore.get(element);
  return {...visual?.latestValues,...Object.fromEntries(
    Array.from(visual?.values.entries()??[]).map(([key,value])=>[key,value.get()]))};
};
(window as any).motionDebug=(element:Element)=>Object.fromEntries(
  Array.from(visualElementStore.get(element)?.values.entries()??[]).map(([key,value])=>
    [key,{value:value.get(),startTime:(value as any).animation?.startTime,keyframes:(value as any).animation?.options?.keyframes}]));
(window as any).eventTimes=lines.flatMap(line=>{
  const profile=resolveGlowWordRenderProfile(line)!;
  const words=buildDisplayWordsFromLayoutUnits(buildPostLyricLayoutUnits(line,{semantic:true,sticky:true}));
  return [line.startTime*1000,getLineRenderEndTime(line)*1000,...words.flatMap(word=>[
    Math.max(line.startTime,word.startTime-profile.wordLookahead)*1000,
    getGlowWordActiveEndTime(word,profile)*1000+.001])];
});
(window as any).step = (ms: number) => {
  if(manual) {frameData.delta=ms-frameData.timestamp;frameData.timestamp=ms;motionTime.set(ms);}
  const seconds = ms / 1000;
  // Set the selected line before emitting time, matching host→runtime→GlowWord ownership.
  const index = lines.findIndex((l, i) => seconds >= l.startTime && seconds < getLineRenderEndTime(l) && (!lines[i + 1] || seconds < lines[i + 1].startTime));
  flushSync(() => root.render(<Classic currentTime={currentTime} currentLineIndex={index} lines={lines}
    theme={themes} audioPower={audioPower} audioBands={{}} classicTuning={{ enableWordRotation: true,
      breathingFloatMultiplier: params.has('breathe') ? 1 : 0, wordSpacing: .7, avoidOverlap: params.has('avoid'), useLegacyLayout: false }} />));
  flushSync(() => currentTime.set(seconds));
  if(manual) {
    flushKeyframeResolvers();
    frameData.isProcessing=true;
    try {Object.values(frameSteps).forEach(step=>step.process(frameData));}
    finally {frameData.isProcessing=false;}
  }
};
(window as any).step(0);
(window as any).ready = true;
