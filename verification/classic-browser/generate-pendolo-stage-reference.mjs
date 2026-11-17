// Isolate the original text wheel, not a rewritten JSX oracle. Do not include
// clockwork canvas, audio analysis/lenses, or the original application's stores.
import fs from'node:fs';import path from'node:path';import{createHash}from'node:crypto';import{root,upstream,revision,checkReference}from'./reference.mjs';checkReference();
const original=fs.readFileSync(path.join(upstream,'src/components/visualizer/pendolo/VisualizerPendolo.tsx'),'utf8');
const geometry=original.slice(original.indexOf('    const centerX = viewportSize.width'),original.indexOf('    // Escapement angular shift calculation'));
const measure=original.slice(original.indexOf('    const lineBlockHeights = useMemo'),original.indexOf('    const primaryTextColor ='));
const tree=original.match(/                    <motion.div\r?\n                        ref=\{wheelRailRef\}[\s\S]+?\r?\n                    <\/motion.div>/)?.[0];
if(!geometry||!measure||!tree||!tree.includes('subtitleTracks.map'))throw Error('Pinned text-wheel source seam changed');
const generated=`// Copyright (c) 2026 chthollyphile and contributors. AGPL-3.0-only.
// Generated from pinned ${revision}; original text preparation/JSX below unchanged.
import React,{useMemo,useRef,useLayoutEffect}from'react';import{motion,motionValue,useTransform}from'framer-motion';
import{calculatePendoloWheelLayout}from'./folia-major/src/components/visualizer/pendolo/pendoloGeometry';
import{buildPendoloTextLayout}from'./folia-major/src/components/visualizer/pendolo/pendoloTextLayout';
import{buildPendoloSubtitleFontSpec,resolvePendoloSubtitleTracks,sumPendoloSubtitleTrackHeights}from'./folia-major/src/components/visualizer/pendolo/pendoloSubtitleTracks';
import{resolvePendoloChorusPresentation,resolvePendoloMotionProfile}from'./folia-major/src/components/visualizer/pendolo/pendoloMotionProfile';
import{resolveThemeFontWeight}from'./folia-major/src/utils/fontStacks';
import{colorWithAlpha}from'./folia-major/src/components/visualizer/colorMix';
import PendoloRotatingLine from'./folia-major/src/components/visualizer/pendolo/PendoloRotatingLine';
import PendoloActiveLyricSweep from'./folia-major/src/components/visualizer/pendolo/PendoloActiveLyricSweep';
export default function Reference(p:any){
 const{lines,targetLineIndex,currentLineIndex,currentTime,theme,viewportSize,pendoloTuning,resolvedMode}=p;
 const fontFamily=theme.fontFamily,fontWeight=theme.fontWeight,subtitleFontFamily=fontFamily,subtitleTheme=theme;
 const subtitleFontScale=1,lyricsFontScale=1,hideTranslationSubtitle=false,manualScrollAnchorIndex=null;
 const primaryTextColor=theme.primaryColor,secondaryTextColor=theme.secondaryColor,accentTextColor=theme.accentColor;
 const motionProfile=resolvePendoloMotionProfile(theme.animationIntensity),wheelRailRef=useRef(null);
 const wheelRotationDeg=useMemo(()=>motionValue(0),[]),textRotationCorrectionDeg=useTransform(wheelRotationDeg,value=>-value*.65);
 const zero=useMemo(()=>motionValue(0),[]);const beatFx={waveRadius:zero,waveBand:zero,waveStrength:zero,waveX:zero,waveY:zero};
 const onLyricLineSeek=(time:number)=>{(window as any).stageSeek=time};const handleLineSeek=(_i:number,t:number)=>onLyricLineSeek(t);
${geometry}
${measure}
 useLayoutEffect(()=>{(window as any).stageModel={items:lineItems,heights:lineBlockHeights,availableTextWidth,centerX,centerY};});
 return <div id="pendolo-reference">${tree}</div>;
}
`;
fs.writeFileSync(path.join(root,'.work/pendolo-stage-reference.tsx'),generated);
fs.writeFileSync(path.join(root,'verification/pendolo-fixtures/stage-reference.json'),JSON.stringify({revision,
 source:'src/components/visualizer/pendolo/VisualizerPendolo.tsx',
 extracts:[geometry,measure,tree].map(s=>({sha256:createHash('sha256').update(s).digest('hex'),characters:s.length})),
 scope:'Unchanged original geometry/measurement declarations and complete lyric-wheel JSX subtree; original rotating-line/sweep/subtitle/color functions. Target-only host: rotation=0, smart-beat values=0, no clockwork/background/store/manual-scroll behavior. Not complete original app or animated tick acceptance.'},null,2)+'\n');
console.log('Generated pinned original Pendolo text-wheel reference');
