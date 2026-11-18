import fs from 'node:fs';
import path from 'node:path';
import {createRequire} from 'node:module';
import {createHash} from 'node:crypto';
import {createServer} from 'vite';
import {root,revision,checkReference,referenceMotionVersions} from './reference.mjs';
import {cameraTargetPlugin,sourceFragments} from './diorama-camera-source.mjs';
import {cameraFollowPlugin,followSourceFragments} from './diorama-follow-source.mjs';
checkReference();
const folder=path.join(root,'verification/diorama-fixtures/follow-v1');
if(fs.existsSync(folder))throw Error('Preserve previous original fixture; refusing overwrite');
const require=createRequire(import.meta.url);
const server=await createServer({configFile:false,root:path.join(root,'verification/classic-browser'),
 plugins:[cameraTargetPlugin(),cameraFollowPlugin()],
 resolve:{alias:{three:path.join(path.dirname(require.resolve('three')),'three.module.js')}},
 optimizeDeps:{noDiscovery:true,entries:[]},cacheDir:path.join(root,'.work/diorama-follow-numeric'),
 server:{middlewareMode:true,fs:{allow:[root]}}});
try{
 const {originalTarget}=await server.ssrLoadModule('virtual:diorama-camera-target');
 const {originalFollow}=await server.ssrLoadModule('virtual:diorama-camera-follow');
 const cam=await server.ssrLoadModule(path.join(root,'.work/folia-major/src/components/visualizer/diorama/cameraPath.ts'));
 const seed='DioramaProbe',frames=cam.buildDioramaPath(12,seed),shots=['pushIn','pullBack','orbit','track','crane','hold','swell','spiral','pendulum','flyby','arc','float','glide'];
 const line={fullText:'Synthetic mirror plane',startTime:.7,endTime:3.1,
  words:['Synthetic','mirror','plane'].map((text,i)=>({text,startTime:.7+i*.8,endTime:1.5+i*.8}))};
 const records=[];
 const flat=s=>[...s.position,...s.velocity,...s.look,...s.lookVelocity,s.alignment,
  ...(s.alignmentOrientation??[0,0,0,1]),...(s.orientation??[0,0,0,1]),s.alignmentOrientation?1:0,s.orientation?1:0];
 const initial={position:[0,.6,9],velocity:[0,0,0],look:[0,0,-10],lookVelocity:[0,0,0],alignment:0};
 for(const [si,shotKind]of shots.entries())for(const width of [380,800])for(const schedule of ['60','144','irregular']){
  let previous=initial,now=0;
  const steps=[0,...Array.from({length:18},(_,i)=>schedule==='60'?1/60:schedule==='144'?1/144:[.009,.037,.004,.021,.05][i%5])];
  for(const [i,delta]of steps.entries()){
   now+=delta;
   // Include a true far seek and a different text-plane basis; never compare a
   // Kotlin-derived initial state against itself.
   const index=i<9?3:10,frame=frames[index],placement=cam.getDioramaTextPlacement(index,seed,1.35);
   const time=(i<9?1:3.1)+now,aspect=width/600,fov=55,smoothTime=si%2?.33:.56;
   const target=originalTarget({line,now:time,frame,placement,seed,index,shotKind,trackWidth:12,aspect,moveScale:1.35,driftScale:.85});
   const result=originalFollow({line,now:time,frame,placement,shotKind,delta,aspect,fov,smoothTime,target,previous});
   records.push([shotKind,width,schedule,i,time,delta,index,smoothTime,...flat(previous),
    ...Object.values(target.eye),...Object.values(target.look),...flat(result.state),
    +result.snapped,result.readTarget,result.offAxis,result.maxOffAxis,...result.targetOrientation]);
   previous=result.state;
  }
 }
 fs.mkdirSync(folder,{recursive:true});
 fs.writeFileSync(path.join(folder,'finite.tsv'),['# shot width schedule step now dt index smoothTime; previous23; target6; result23; snapped readTarget offAxis maxOffAxis targetQuaternion4',...records.map(r=>r.join('\t'))].join('\n')+'\n');
 const hash=file=>createHash('sha256').update(fs.readFileSync(path.join(root,file))).digest('hex');
 fs.writeFileSync(path.join(folder,'conditions.json'),JSON.stringify({revision,motion:referenceMotionVersions(),three:'0.185.1',
  lockSha256:hash('verification/classic-browser/pnpm-lock.yaml'),sourceFragments:[...sourceFragments,...followSourceFragments],
  cases:records.length,seed,line,shots,scope:'Original private normal-follow blocks and original target functions. Independent original incoming states; no Kotlin inputs/expected, no flight, no RAF mount. 60/144/irregular are distinct source finite operators, NOT a fixed playback frequency.'},null,2)+'\n');
 console.log(JSON.stringify({cases:records.length}));
}finally{await server.close()}
