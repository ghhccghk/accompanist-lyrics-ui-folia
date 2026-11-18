// Supplement the FROZEN actual mounted capture. Never recapture/rewrite frames,
// or compute expected material values from a port/formula copied into this host.
import fs from'node:fs';import path from'node:path';import{createHash}from'node:crypto';
import{root,upstream,revision,referenceModules,referenceMotionVersions}from'./reference.mjs';
const folder=path.join(root,'verification/diorama-fixtures/mounted-material-v1');
if(fs.existsSync(folder))throw Error('Preserve original material observations; refusing overwrite');
const mounted=path.join(root,'verification/diorama-fixtures/mounted-cut-v1/scene.json');
const raw=path.join(root,'build/verification/diorama-mounted-browser/steps-full.json');
const baseline=JSON.parse(fs.readFileSync(mounted,'utf8')),steps=JSON.parse(fs.readFileSync(raw,'utf8'));
if(steps.length!==4686)throw Error('Missing original callback records');
const {server,load}=await referenceModules();
try {
 const {buildLineGraphemeTimeline}=await load('utils/lyrics/graphemeTiming.ts');
 const {splitDioramaLyricUnits}=await load('components/visualizer/diorama/dioramaSceneLayout.ts');
 const unitRows=[];
 for(const[phase,lines]of[['outgoing',baseline.inputs.oldLines],['incoming',baseline.inputs.lines]])
  lines.forEach((line,local)=>splitDioramaLyricUnits(line,buildLineGraphemeTimeline(line)).forEach((u,i)=>
   unitRows.push([phase,local,i,u.charStart,u.charEnd,u.startTime,u.endTime,u.text].join('\t'))));
 const materialRows=[];
 for(const c of baseline.captures){
  const r=steps.find(s=>s.phase==='incoming'&&s.width===c.width&&s.fps===c.fps&&s.time===c.time);
  if(!r)throw Error('Missing frozen original step');
  for(const key of['light','soul','unitMaterials'])
   if(JSON.stringify(c[key])!==JSON.stringify(r[key]))throw Error('Original raw/mounted manifest mismatch: '+key);
  if(r.activeUnits!==r.light.length||r.activeUnits!==r.soul.length||r.activeUnits!==r.unitMaterials.length)
   throw Error('Incomplete original unit observation');
  r.unitMaterials.forEach((m,i)=>materialRows.push([c.width,c.fps,c.time,i,r.light[i],r.soul[i],...m.color,m.opacity,m.glowOpacity,m.soulOpacity].join('\t')));
 }
 const hash=p=>createHash('sha256').update(fs.readFileSync(p)).digest('hex');
 fs.mkdirSync(folder,{recursive:true});
 fs.writeFileSync(path.join(folder,'units.tsv'),['# phase localIndex unitIndex charStart charEnd start end text',...unitRows].join('\n')+'\n');
 fs.writeFileSync(path.join(folder,'materials.tsv'),['# width fps time unitIndex light soul linearRGB3 baseOpacity glowOpacity soulOpacity',...materialRows].join('\n')+'\n');
 fs.writeFileSync(path.join(folder,'conditions.json'),JSON.stringify({revision,motion:referenceMotionVersions(),browser:baseline.browser,rendering:baseline.rendering,
  sourceFrames:baseline.captures.length,originalIntervals:steps.length,unitRows:unitRows.length,materialRows:materialRows.length,
  mountedManifestSha256:hash(mounted),originalRawSha256:hash(raw),exporterSha256:hash(path.join(root,'verification/classic-browser/export-diorama-mounted-materials.mjs')),
  sources:['DioramaScene.tsx','dioramaSceneFrame.ts','dioramaSceneLayout.ts'].map(file=>({file,sha256:hash(path.join(upstream,'src/components/visualizer/diorama',file))})),
  scope:'Exact existing 175dabe original CameraRig+Scene capture; 48 material/light/soul snapshots are exported from original mounted observations, also checked against retained raw callbacks. Units/timings are evaluated by actual pinned graphemeTiming/splitDioramaLyricUnits on original synthetic inputs, not native. No browser rerun/expected formula/native-state substitution. Independent native finite history will replay all4686 intervals and assert these192 observed unit states at1e-9.'},null,2)+'\n');
 console.log(JSON.stringify({units:unitRows.length,originalMaterialStates:materialRows.length,sourceFrames:baseline.captures.length}));
}finally{await server.close()}
