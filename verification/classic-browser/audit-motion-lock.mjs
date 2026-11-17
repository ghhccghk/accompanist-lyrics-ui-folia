// Dedicated exact-lock recapture; preserve every previous source image/fixture.
import fs from'node:fs';import path from'node:path';import{createHash}from'node:crypto';
import{startReferenceServer}from'./serve.mjs';import{root,revision,referenceMotionVersions}from'./reference.mjs';
const run=process.argv[2]??'motion-lock-audit';if(!/^[a-z0-9-]+$/.test(run))throw Error('Use a plain run directory name');
const baseline=path.join(root,'.work',run);
if(fs.existsSync(baseline))throw Error('Audit baseline already exists; preserve prior evidence and choose a fresh run directory');
fs.mkdirSync(baseline,{recursive:true});
const groups=[{name:'pendolo',dir:'build/verification/pendolo-public-stage-browser',files:['verification/pendolo-fixtures/public-stage-frames.tsv','verification/pendolo-fixtures/public-stage-rows.tsv','verification/pendolo-fixtures/public-stage-inputs.json','verification/pendolo-fixtures/public-stage-conditions.json'],capture:'./capture-pendolo-public-stage.mjs'},
{name:'cappella',dir:'build/verification/cappella-playback-browser',files:['verification/cappella-fixtures/playback-frames.tsv','verification/cappella-fixtures/playback-observations.tsv'],capture:'./capture-cappella-playback.mjs'}];
const sha=p=>createHash('sha256').update(fs.readFileSync(p)).digest('hex');
const snapshots={};
for(const g of groups){
 const source=path.join(root,g.dir),save=path.join(baseline,g.name);
 fs.cpSync(source,save,{recursive:true});
 for(const file of g.files){const to=path.join(baseline,file);fs.mkdirSync(path.dirname(to),{recursive:true});fs.copyFileSync(path.join(root,file),to);}
 snapshots[g.name]=Object.fromEntries(fs.readdirSync(source).filter(f=>f.endsWith('.png')).map(f=>[f,sha(path.join(source,f))]));
}
const oldUrl=process.env.FOLIA_REFERENCE_URL;process.env.FOLIA_REFERENCE_URL='http://127.0.0.1:4177';
let server;const reports=[];
try{
 server=await startReferenceServer(4177);
 for(const g of groups){
  await import(g.capture);
  const source=path.join(root,g.dir),old=snapshots[g.name],names=fs.readdirSync(source).filter(f=>f.endsWith('.png'));
  const frames=names.map(file=>({file,before:old[file]??null,exactLock:sha(path.join(source,file))}));
  reports.push({theme:g.name,frames:frames.length,changedFrames:frames.filter(f=>f.before!==f.exactLock).length,
   oldMissing:names.filter(f=>!old[f]),newMissing:Object.keys(old).filter(f=>!names.includes(f)),
   fixtureFiles:g.files.map(file=>({file,before:sha(path.join(baseline,file)),exactLock:sha(path.join(root,file))})),pngs:frames});
 }
 const out=path.join(root,'verification/motion-lock-audit');fs.mkdirSync(out,{recursive:true});
 fs.writeFileSync(path.join(out,'comparison.json'),JSON.stringify({revision,run,motion:referenceMotionVersions(),scope:'Original public-stage Pendolo and continuous regular Cappella captures, fresh dedicated server/cache, unchanged source host/probes/fonts/clocks. Previous baseline preserved. No Kotlin recertification or final visual acceptance implied.',reports},null,2)+'\n');
 console.log(JSON.stringify(reports.map(({theme,frames,changedFrames})=>({theme,frames,changedFrames}))));
}finally{await server?.close();if(oldUrl===undefined)delete process.env.FOLIA_REFERENCE_URL;else process.env.FOLIA_REFERENCE_URL=oldUrl;}
