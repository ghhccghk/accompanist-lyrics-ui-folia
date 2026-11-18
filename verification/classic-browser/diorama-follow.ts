import './diorama-active-layers';
import {originalTarget} from 'virtual:diorama-camera-target';
import {originalFollow} from 'virtual:diorama-camera-follow';
const stage=(window as any).targetStage;
let previous:any;
Object.assign(window,{followReady:true,stepFollow:(time:number,delta:number)=>{
 const target=originalTarget({line:stage.line,now:time,frame:stage.frame,placement:stage.placement,
  seed:'DioramaProbe',index:3,shotKind:'orbit',trackWidth:stage.layout.lineWidth*stage.group.scale.x,aspect:innerWidth/600});
 const result=originalFollow({line:stage.line,now:time,frame:stage.frame,placement:stage.placement,
  shotKind:'orbit',delta,aspect:innerWidth/600,target,previous});
 previous=result.state;
 stage.camera.position.fromArray(result.state.position);
 stage.camera.quaternion.fromArray(result.state.orientation);
 stage.camera.updateMatrixWorld();
 return {...result,target};
},drawFollow:(time:number)=>({...((window as any).drawLayers(time,true)),follow:previous})});
