// Actual unmodified original Three materials/meshes and frame updater.
import fs from'node:fs';import path from'node:path';import * as THREE from'three';import{referenceModules,root,revision}from'./reference.mjs';
const{server,load}=await referenceModules();try{
 const{updateDioramaActiveUnits}=await load('components/visualizer/diorama/dioramaSceneFrame.ts');
 const{resolveGradientEnergy}=await load('components/visualizer/diorama/dioramaSceneUnits.ts');
 const cases=[],expected=[];const emit=(start,end,now,delta,previous,p,a,t,keyword,life,breath,power)=>{
  const unit={text:'Synthetic',charStart:0,charEnd:9,startTime:start,endTime:end};
  const planes={baseMats:[new THREE.MeshBasicMaterial()],glowMats:[new THREE.MeshBasicMaterial()],soulMats:[new THREE.MeshBasicMaterial()],
   glowMeshes:[new THREE.Mesh()],soulMeshes:[new THREE.Mesh()],unitGroups:[new THREE.Group()]};
  const light=new Float32Array([previous[0]]),soul=new Float32Array([previous[1]]);const color=v=>new THREE.Color(...v);
  updateDioramaActiveUnits({activeLineUnits:[unit],planes,lightVals:light,soulVals:soul,now,delta,life,breath,powerEnv:power,
   damped:{primary:color(p),accent:color(a),secondary:color(p),background:color(p)},keywordUnitColors:new Map(keyword?[[0,color(keyword)]]:[]),
   glowIntensity:t[0],soulIntensity:t[1],soulActiveEnabled:t[2],gradientIntensity:t[3]});
  const fill=planes.baseMats[0].color;
  cases.push({start,end,now,delta,previous,primary:p,accent:a,tuning:t,keyword,life,breath,power});
  expected.push([light[0],soul[0],fill.r,fill.g,fill.b,planes.baseMats[0].opacity,planes.glowMats[0].opacity,+planes.glowMeshes[0].visible,
   planes.soulMats[0].opacity,planes.soulMeshes[0].position.y,planes.soulMeshes[0].scale.x,+planes.soulMeshes[0].visible,resolveGradientEnergy(now,unit)]);
  [...planes.baseMats,...planes.glowMats,...planes.soulMats].forEach(m=>m.dispose());
 };
 const palettes=[[[.8,.7,.6],[.9,.15,.02]],[[.9,.9,.9],[.9,.9,.9]],[[.05,.05,.05],[.05,.05,.05]],[[.4,.41,.42],[.43,.42,.41]]];
 for(const[start,end]of[[.7,2.8],[1,1],[3,1],[.7,.705]])for(const now of[-1,start-.000001,start,start+.000001,(start+end)/2,end-.000001,end,end+.000001,end+.35,end+.350001,end+.6,end+2.15,end+3])
  for(let pi=0;pi<palettes.length;pi++)for(const t of[[0,0,false,0],[1,1,true,1],[1.5,1.5,false,1.5],[2,2,true,0]]){
   const[p,a]=palettes[pi];emit(start,end,now,[0,1/144,1/60,.05][pi],[.17,.43],p,a,t,pi%2?[.1,.8,.3]:null,pi===3?.35:1,pi===2?.87:1,0);
  }
 const folder=path.join(root,'verification/diorama-fixtures');
 const encode=v=>Array.isArray(v)?v.join(','):v===null?'-':v;
 fs.writeFileSync(path.join(folder,'unit-frame-inputs.tsv'),['# start end now delta previous primary accent tuning keyword life breath power',...cases.map(c=>Object.values(c).map(encode).join('\t'))].join('\n')+'\n');
 fs.writeFileSync(path.join(folder,'unit-frames.tsv'),['# light soul linearRGB baseAlpha glowAlpha glowVisible soulAlpha soulLift soulScale soulVisible gradient',...expected.map(row=>row.join('\t'))].join('\n')+'\n');
 fs.writeFileSync(path.join(folder,'unit-frame-conditions.json'),JSON.stringify({revision,cases:cases.length,three:'0.185.1',scope:'Unmodified original updateDioramaActiveUnits writes real Three material/mesh instances; explicit Float32 initial envelopes, linear color channels, power0. Finite operator only; NOT CameraRig/frame-rate or visual acceptance.'},null,2)+'\n');
 console.log(JSON.stringify({cases:cases.length}));
}finally{await server.close();}
