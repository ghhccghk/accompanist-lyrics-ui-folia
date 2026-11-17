// Reuse the unchanged wheel JSX, replacing only its explicitly static host seam
// with the pinned original useSpring/angle/counter-rotation declarations.
import'./generate-pendolo-stage-reference.mjs';
import fs from'node:fs';import path from'node:path';import{createHash}from'node:crypto';import{root,upstream,revision}from'./reference.mjs';
const original=fs.readFileSync(path.join(upstream,'src/components/visualizer/pendolo/VisualizerPendolo.tsx'),'utf8');
const spring=original.slice(original.indexOf('    const springSnappiness ='),original.indexOf('    // Font stack & weight setup'));
const step=original.slice(original.indexOf('    const totalArcRad ='),original.indexOf('    // 智能动感模式 (usePendoloBeatFx)'));
const rotate=original.slice(original.indexOf('    const wheelRotationDeg ='),original.indexOf('    // Pendolo lays subtitles out'));
if(!spring.includes('tickSpring.set')||!step.includes('angleStepRad')||!rotate.includes('textRotationCorrectionDeg'))throw Error('Pinned tick declarations changed');
let text=fs.readFileSync(path.join(root,'.work/pendolo-stage-reference.tsx'),'utf8');
text=text.replace("{useMemo,useRef,useLayoutEffect}","{useMemo,useRef,useLayoutEffect,useEffect}")
 .replace('{motion,motionValue,useTransform}','{motion,motionValue,useTransform,useSpring}')
 .replace('const motionProfile=resolvePendoloMotionProfile(theme.animationIntensity),wheelRailRef=useRef(null);','const wheelRailRef=useRef(null);')
 .replace('const wheelRotationDeg=useMemo(()=>motionValue(0),[]),textRotationCorrectionDeg=useTransform(wheelRotationDeg,value=>-value*.65);',
 `${spring}\n${step}\n${rotate}\nuseLayoutEffect(()=>{window.tickSource={tickSpring,wheelRotationDeg,textRotationCorrectionDeg};});`);
if(!text.includes('const tickSpring = useSpring'))throw Error('Static seam replacement failed');
fs.writeFileSync(path.join(root,'.work/pendolo-tick-stage-reference.tsx'),text);
fs.writeFileSync(path.join(root,'verification/pendolo-fixtures/tick-stage-reference.json'),JSON.stringify({revision,
 source:'src/components/visualizer/pendolo/VisualizerPendolo.tsx',extracts:[spring,step,rotate].map(s=>({sha256:createHash('sha256').update(s).digest('hex'),characters:s.length})),
 scope:'Original hook declarations and original complete text-wheel subtree. Controlled RAF/performance clock; smart-beat zero, no audio/gears/manual-scroll/chorus. Mounted useSpring velocity is observed, not overridden.'},null,2)+'\n');
