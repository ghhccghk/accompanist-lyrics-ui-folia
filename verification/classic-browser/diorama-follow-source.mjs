// Export exact normal-follow CameraRig blocks, without rewriting their formulas.
import fs from 'node:fs';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {transformWithOxc} from 'vite';
import {root,checkReference} from './reference.mjs';
const relative='src/components/visualizer/diorama/CameraRig.tsx';
const source=fs.readFileSync(path.join(root,'.work/folia-major',relative),'utf8');
const fragments=[];
function extract(start,end){
 const a=source.indexOf(start),b=source.indexOf(end,a);
 if(a<0||b<0)throw Error('Missing pinned CameraRig block');
 const code=source.slice(a,b);
 fragments.push({file:relative,startLine:source.slice(0,a).split('\n').length,
  endLine:source.slice(0,b).split('\n').length,sha256:createHash('sha256').update(code).digest('hex')});
 return code;
}
const constants=extract('const UP =','// Grapheme-reveal fraction');
const position=extract('        const posGap =','        // Aim beside the read-head');
const look=extract('        const lookGap =','        // Reset up before every lookAt');
const orientation=extract('        // Reset up before every lookAt','    });\n\n    return null;');
const prefix='/@fs/'+path.join(root,'.work/folia-major/src').replaceAll('\\','/');
const code=`import * as THREE from 'three';
import {smoothDamp,DIORAMA_SNAP_DISTANCE} from '${prefix}/components/visualizer/diorama/cameraPath.ts';
import {getLineRenderEndTime} from '${prefix}/utils/lyrics/renderHints.ts';
${constants}
export function originalFollow(input){
 const {line,now,frame,placement,shotKind,delta,aspect,fov=55,smoothTime=.42,target,previous={}}=input;
 const vec=(name,fallback)=>({current:new THREE.Vector3(...(previous[name]??fallback))});
 const posRef=vec('position',[0,.6,9]),posVelRef=vec('velocity',[0,0,0]);
 const lookRef=vec('look',[0,0,-10]),lookVelRef=vec('lookVelocity',[0,0,0]);
 const quat=name=>({current:new THREE.Quaternion(...(previous[name]??[0,0,0,1]))});
 const alignRef={current:previous.alignment??0},alignQuatRef=quat('alignmentOrientation'),orientRef=quat('orientation');
 const alignQuatInitRef={current:!!previous.alignmentOrientation},orientInitRef={current:!!previous.orientation};
 const R=frame.right,U=frame.up,F=frame.forward;
 const camera=new THREE.PerspectiveCamera(fov,aspect,.1,1000);
 const posX=target.eye.x,posY=target.eye.y,posZ=target.eye.z;
 const poseX=posX,poseY=posY,poseZ=posZ,lookX=target.look.x,lookY=target.look.y,lookZ=target.look.z;
 const flight={perp:new THREE.Vector3()},flying=false,followSmooth=smoothTime,arcMag=0,bank=0;
 ${position}
 ${look}
 ${orientation}
 return {state:{position:posRef.current.toArray(),velocity:posVelRef.current.toArray(),look:lookRef.current.toArray(),
  lookVelocity:lookVelRef.current.toArray(),alignment:alignRef.current,
  alignmentOrientation:alignQuatRef.current.toArray(),orientation:orientRef.current.toArray()},
  snapped,readTarget,offAxis:offAxisAngle,maxOffAxis,targetOrientation:_targetQuat.toArray()};
}`;
export const followSourceFragments=fragments;
export function cameraFollowPlugin(){
 checkReference();
 return {name:'observe-original-camera-follow',
  resolveId:id=>id==='virtual:diorama-camera-follow'?'\0diorama-camera-follow.ts':null,
  load:async id=>id==='\0diorama-camera-follow.ts'?await transformWithOxc(code,'camera-follow.ts'):null};
}
