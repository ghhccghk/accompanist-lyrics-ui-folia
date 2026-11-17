import './diorama-active-layers';
import {originalTarget} from 'virtual:diorama-camera-target';
// Isolate desired original camera target, before damping/orientation followers.
const stage=(window as any).targetStage;
Object.assign(window,{targetReady:true,drawTarget:(time:number,shotKind:string)=>{
 const target=originalTarget({line:stage.line,now:time,frame:stage.frame,placement:stage.placement,
 seed:'DioramaProbe',index:3,shotKind,trackWidth:stage.layout.lineWidth*stage.group.scale.x,aspect:innerWidth/600});
 stage.camera.position.set(target.eye.x,target.eye.y,target.eye.z);
 stage.camera.up.set(0,1,0);stage.camera.lookAt(target.look.x,target.look.y,target.look.z);stage.camera.updateMatrixWorld();
 const state=(window as any).drawLayers(time,true);
 return {...state,...target,trackWidth:stage.layout.lineWidth*stage.group.scale.x};
}});
