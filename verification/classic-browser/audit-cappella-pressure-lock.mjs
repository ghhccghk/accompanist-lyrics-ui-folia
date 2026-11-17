// One bounded exact-lock audit. Preserve old source/native frames and reports first.
import fs from'node:fs';import path from'node:path';import{createHash}from'node:crypto';
import{startReferenceServer}from'./serve.mjs';import{root,revision,referenceMotionVersions}from'./reference.mjs';
const run=process.argv[2]??'cappella-pressure-lock-v1';if(!/^[a-z0-9-]+$/.test(run))throw Error('Invalid run name');
const baseline=path.join(root,'.work',run);if(fs.existsSync(baseline))throw Error('Preserve existing baseline; choose a new audit name');
const source='build/verification/cappella-presence-browser',native='build/verification/cappella-presence-kotlin';
const fixture=['presence-frames.tsv','presence-inputs.tsv','presence-observations.tsv'].map(f=>'verification/cappella-fixtures/'+f);
fs.mkdirSync(baseline,{recursive:true});
for(const name of[source,native,'verification/cappella-presence',...fixture]){const to=path.join(baseline,name);fs.mkdirSync(path.dirname(to),{recursive:true});fs.cpSync(path.join(root,name),to,{recursive:true});}
const sha=p=>createHash('sha256').update(fs.readFileSync(p)).digest('hex');
const snapshot=folder=>Object.fromEntries(fs.readdirSync(folder).filter(f=>f.endsWith('.png')).map(f=>[f,sha(path.join(folder,f))]));
const before=snapshot(path.join(root,source));let server;const old=process.env.FOLIA_REFERENCE_URL;
try{
 server=await startReferenceServer(4182);process.env.FOLIA_REFERENCE_URL='http://127.0.0.1:4182';
 await import('./capture-cappella-presence.mjs');
 const after=snapshot(path.join(root,source)),frames=Object.keys(after).map(file=>({file,before:before[file]??null,exact:after[file]}));
 const report={revision,run,motion:referenceMotionVersions(),
  fontSha256:sha(path.join(root,'verification/assets/FoliaParityLatin-Bold.ttf')),
  verificationLockSha256:sha(path.join(root,'verification/classic-browser/pnpm-lock.yaml')),
  originalLockSha256:sha(path.join(root,'.work/folia-major/package-lock.json')),
  conditions:JSON.parse(fs.readFileSync(path.join(root,source,'conditions.json'),'utf8')),
  scope:'Same original dense240 pressure capture code/inputs/clocks/font/viewport. New dedicated server/cache, no source formula changes. Previous complete source/native/report preserved. No final theme or device acceptance.',
  changedFrames:frames.filter(f=>f.before!==f.exact).length,missingAfter:Object.keys(before).filter(f=>!after[f]),frames,
  fixtures:fixture.map(file=>({file,before:sha(path.join(baseline,file)),exact:sha(path.join(root,file))}))};
 fs.writeFileSync(path.join(root,'verification/motion-lock-audit/cappella-pressure.json'),JSON.stringify(report,null,2)+'\n');
 console.log(JSON.stringify({frames:frames.length,changedFrames:report.changedFrames,fixtures:report.fixtures}));
}finally{await server?.close();if(old===undefined)delete process.env.FOLIA_REFERENCE_URL;else process.env.FOLIA_REFERENCE_URL=old;}
