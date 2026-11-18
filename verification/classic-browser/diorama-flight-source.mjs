// Export pinned CameraRig flight/follow blocks verbatim; no rewritten oracle.
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
 if(a<0||b<0)throw Error('Missing pinned flight block');
 const code=source.slice(a,b);
 fragments.push({file:relative,startLine:source.slice(0,a).split('\n').length,
  endLine:source.slice(0,b).split('\n').length,sha256:createHash('sha256').update(code).digest('hex')});
 return code;
}
const constants=extract('const UP =','// Grapheme-reveal fraction');
const flightBlock=extract('        const flight = flightRef.current;','        // SmoothDamp the camera');
const position=extract('        const posGap =','        // Aim beside the read-head');
const look=extract('        const lookGap =','        // Reset up before every lookAt');
const orientation=extract('        // Reset up before every lookAt','    });\n\n    return null;');
const prefix='/@fs/'+path.join(root,'.work/folia-major/src').replaceAll('\\','/');
const code=`import * as THREE from 'three';
import {smoothDamp,DIORAMA_SNAP_DISTANCE} from '${prefix}/components/visualizer/diorama/cameraPath.ts';
import {getLineRenderEndTime} from '${prefix}/utils/lyrics/renderHints.ts';
import {flightPerp,flightSide,TRANSITION_AIM_SWEEP,TRANSITION_BANK,TRANSITION_DURATION,transitionEase}
 from '${prefix}/components/visualizer/diorama/dioramaTransition.ts';
${constants}
export function originalFlight(input){
 const {line,now,frame,placement,shotKind,delta,aspect,fov=55,smoothTime=.42,target,previous={},
  transitionEpoch,shotSeed,wallTime=now,flightState}=input;
 const vec=(name,fallback)=>({current:new THREE.Vector3(...(previous[name]??fallback))});
 const posRef=vec('position',[0,.6,9]),posVelRef=vec('velocity',[0,0,0]);
 const lookRef=vec('look',[0,0,-10]),lookVelRef=vec('lookVelocity',[0,0,0]);
 const quat=name=>({current:new THREE.Quaternion(...(previous[name]??[0,0,0,1]))});
 const alignRef={current:previous.alignment??0},alignQuatRef=quat('alignmentOrientation'),orientRef=quat('orientation');
 const alignQuatInitRef={current:!!previous.alignmentOrientation},orientInitRef={current:!!previous.orientation};
 const R=frame.right,U=frame.up,F=frame.forward;
 const camera=new THREE.PerspectiveCamera(fov,aspect,.1,1000),motion={smoothTime};
 const state={clock:{elapsedTime:wallTime}};
 const flightRef={current:{flying:flightState?.flying??false,epoch:flightState?.epoch??0,
  start:flightState?.start??0,perp:new THREE.Vector3(...(flightState?.perp??[0,0,0])),
  side:flightState?.side??1,flightLen:flightState?.flightLen??1}};
 const poseX=target.eye.x,poseY=target.eye.y,poseZ=target.eye.z;
 ${flightBlock}
 ${position}
 // The original look target BEFORE its flight sweep is supplied by originalTarget.
 const lookX=target.look.x+flight.perp.x*sweepMag,lookY=target.look.y+flight.perp.y*sweepMag,
  lookZ=target.look.z+flight.perp.z*sweepMag;
 ${look}
 ${orientation}
 return {state:{position:posRef.current.toArray(),velocity:posVelRef.current.toArray(),look:lookRef.current.toArray(),
  lookVelocity:lookVelRef.current.toArray(),alignment:alignRef.current,
  alignmentOrientation:alignQuatRef.current.toArray(),orientation:orientRef.current.toArray()},
  renderedPosition:camera.position.toArray(),renderedOrientation:camera.quaternion.toArray(),
  flight:{flying:flight.flying,epoch:flight.epoch,start:flight.start,perp:flight.perp.toArray(),side:flight.side,flightLen:flight.flightLen},
  flying,te,followSmooth,arcMag,sweepMag,bank,snapped,readTarget,offAxis:offAxisAngle,maxOffAxis,
  targetOrientation:_targetQuat.toArray()};
}`;
export const flightSourceFragments=fragments;
export function cameraFlightPlugin(){
 checkReference();return {name:'observe-original-camera-flight',
  resolveId:id=>id==='virtual:diorama-camera-flight'?'\0diorama-camera-flight.ts':null,
  load:async id=>id==='\0diorama-camera-flight.ts'?await transformWithOxc(code,'camera-flight.ts'):null};
}
