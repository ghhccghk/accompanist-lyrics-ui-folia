// Export-only observation seam: exact private progress and target source blocks.
// Does not evaluate/mutate original React/R3F render loop or original checkout.
import {transformWithOxc} from 'vite';
import fs from 'node:fs';import path from 'node:path';import {createHash} from 'node:crypto';
import {root,checkReference} from './reference.mjs';
const relative='src/components/visualizer/diorama/CameraRig.tsx';
const file=path.join(root,'.work/folia-major',relative);const text=fs.readFileSync(file,'utf8');
const fragments=[];
function extract(start,end){const a=text.indexOf(start),b=text.indexOf(end,a);if(a<0||b<0)throw Error('Original source boundary missing');
 const code=text.slice(a,b);fragments.push({file:relative,startLine:text.slice(0,a).split('\n').length,endLine:text.slice(0,b).split('\n').length,
 sha256:createHash('sha256').update(code).digest('hex')});return code;}
const progress=extract('const resolveWordProgress =','const CameraRig:');
const lineProgress=extract('        let progress = line ?','        const camera =');
const visible=extract('        const visibleHalf =','        // Local frame');
const target=extract('        const settle = line ?','        // ── Cinematic transition flight');
const look=extract('        const lookOff =','        const lookGap =');
const prefix='/@fs/'+path.join(root,'.work/folia-major/src').replaceAll('\\','/');
const code=`import {buildLineGraphemeTimeline} from '${prefix}/utils/lyrics/graphemeTiming.ts';
import {getLineRenderEndTime} from '${prefix}/utils/lyrics/renderHints.ts';
import {resolveLyricTextDirection} from '${prefix}/utils/lyrics/textDirection.ts';
import {resolveHoldSettle,resolveReadHeadTruck,resolveShotOffset,resolveCameraDrift,frameHalfWidth,
 DIORAMA_HERO_DISTANCE,DIORAMA_SAFE_FRAME_FRACTION,DIORAMA_CAMERA_LIFT} from '${prefix}/components/visualizer/diorama/cameraPath.ts';
const clamp01 = (value: number): number => Math.min(1, Math.max(0, value));
${progress}
export function originalTarget({line,now,frame,placement,seed,index,shotKind,trackWidth,aspect,fov=55,moveScale=1,driftScale=.62}){
const timeline=buildLineGraphemeTimeline(line),wordProgress=resolveWordProgress(line,timeline,now);
const rtl=resolveLyricTextDirection(line.fullText)==='rtl';
${lineProgress}
${visible}
const R=frame.right,U=frame.up,F=frame.forward;
const resolved={localIndex:index,segment:{seed}},motion={moveScale,driftScale},activeLineWidthRef={current:trackWidth};
${target}
const flight={perp:{x:0,y:0,z:0}},sweepMag=0;
${look}
return {eye:{x:poseX,y:poseY,z:poseZ},look:{x:lookX,y:lookY,z:lookZ},progress,wordProgress,settle,truck,renderEnd:getLineRenderEndTime(line)};
}`;
export const sourceFragments=fragments;
export function cameraTargetPlugin(){checkReference();return{name:'observe-original-camera-target',
 resolveId:id=>id==='virtual:diorama-camera-target'?'\0diorama-camera-target.ts':null,
 load:async id=>id==='\0diorama-camera-target.ts'?await transformWithOxc(code,'camera-target.ts'):null};}
