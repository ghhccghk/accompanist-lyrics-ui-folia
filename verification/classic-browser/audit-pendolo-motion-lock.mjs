// Preserve each old dense original/native matrix before a single exact-lock capture.
import fs from 'node:fs';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {startReferenceServer} from './serve.mjs';
import {root, revision, referenceMotionVersions, checkReference} from './reference.mjs';
checkReference();
const run=process.argv[2]??'pendolo-motion-lock-v1';
if(!/^[a-z0-9-]+$/.test(run)) throw Error('Invalid run directory');
const baseline=path.join(root,'.work',run);
if(fs.existsSync(baseline)) throw Error('Baseline exists; preserve it, do not overwrite');
const groups=[
 {name:'tick-stage',source:['pendolo-tick-stage-browser'],native:['pendolo-tick-stage-kotlin','pendolo-tick-stage-source-rotation-kotlin'],report:'pendolo-tick-stage',files:['tick-stage-frames.tsv','tick-stage-rows.tsv','tick-stage-widths.tsv','tick-stage-conditions.json'],capture:'./capture-pendolo-tick-stage.mjs'},
 {name:'chorus-stage',source:['pendolo-chorus-stage-browser','pendolo-chorus-halo-browser'],native:['pendolo-chorus-stage-kotlin','pendolo-chorus-stage-source-rotation-kotlin','pendolo-chorus-halo-kotlin'],report:'pendolo-chorus-stage',files:['chorus-stage-frames.tsv','chorus-stage-rows.tsv','chorus-stage-inputs.json','chorus-stage-conditions.json'],capture:'./capture-pendolo-chorus-stage.mjs'},
 {name:'width-response',source:[],native:['pendolo-width'],report:'pendolo-width',files:['width-response.tsv','width-response-reference.json'],capture:'./capture-pendolo-width-response.mjs'},
];
const sha=p=>createHash('sha256').update(fs.readFileSync(p)).digest('hex');
const copy=relative=>{const dest=path.join(baseline,relative);fs.mkdirSync(path.dirname(dest),{recursive:true});fs.cpSync(path.join(root,relative),dest,{recursive:true});};
const images=dir=>Object.fromEntries(fs.readdirSync(dir).filter(f=>f.endsWith('.png')).sort().map(f=>[f,sha(path.join(dir,f))]));
fs.mkdirSync(baseline,{recursive:true});
for(const g of groups){
 for(const dir of [...g.source,...g.native]) copy('build/verification/'+dir);
 copy('verification/'+g.report);
 for(const f of g.files) copy('verification/pendolo-fixtures/'+f);
}
for(const f of ['stage-reference.json','tick-stage-reference.json']) copy('verification/pendolo-fixtures/'+f);
const oldUrl=process.env.FOLIA_REFERENCE_URL;
let server;
const reports=[];
try {
 await import('./generate-pendolo-tick-stage-reference.mjs');
 server=await startReferenceServer(4184);
 process.env.FOLIA_REFERENCE_URL='http://127.0.0.1:4184';
 for(const g of groups){
  await import(g.capture); // Complete capture before reading/comparing any outputs.
  reports.push({name:g.name,images:g.source.map(dir=>{
   const relative='build/verification/'+dir;
   const old=images(path.join(baseline,relative)),exact=images(path.join(root,relative));
   return {dir,missingAfter:Object.keys(old).filter(f=>!(f in exact)),frames:Object.keys(exact).map(file=>({file,before:old[file]??null,exact:exact[file]}))};
  }),fixtures:g.files.map(f=>{const file='verification/pendolo-fixtures/'+f;return {file,before:sha(path.join(baseline,file)),exact:sha(path.join(root,file))};})});
 }
 const report={revision,run,motion:referenceMotionVersions(),
  lockSha256:sha(path.join(root,'verification/classic-browser/pnpm-lock.yaml')),
  upstreamLockSha256:sha(path.join(root,'.work/folia-major/package-lock.json')),
  fontSha256:['FoliaParityLatin-Bold.ttf','FoliaParityBidi-Bold.ttf','FoliaParityChatCJK-Bold.otf'].map(file=>({file,sha256:sha(path.join(root,'verification/assets',file))})),
  generatedSourceSha256:sha(path.join(root,'.work/pendolo-tick-stage-reference.tsx')),
  scope:'Same mounted tick144/chorus168+halo168 matrices and isolated CSS width probes. Original formulas, conditions, fonts and timing unchanged; fresh dedicated server/cache4184. All old frames/native/reports preserved before capture. Width control tests Blink CSS, not Motion physical integration. No visual acceptance implied.',reports};
 fs.writeFileSync(path.join(root,'verification/motion-lock-audit/pendolo-dense.json'),JSON.stringify(report,null,2)+'\n');
 console.log(JSON.stringify(reports.map(r=>({name:r.name,images:r.images.map(i=>({dir:i.dir,count:i.frames.length,changed:i.frames.filter(f=>f.before!==f.exact).length})),fixtures:r.fixtures}))));
}finally{
 await server?.close();
 if(oldUrl===undefined) delete process.env.FOLIA_REFERENCE_URL;else process.env.FOLIA_REFERENCE_URL=oldUrl;
}
