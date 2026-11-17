// Actual pinned source modules are the oracle. No reimplementation of expected curves.
import fs from 'node:fs';
import path from 'node:path';
import {referenceModules,root,revision} from './reference.mjs';
const {server,load}=await referenceModules();
try {
 const e=await load('components/visualizer/cadenza/cadenzaEnvelopes.ts');
 const {buildLineRenderHints}=await load('utils/lyrics/renderHints.ts');
 const folder=path.join(root,'verification/cadenza-fixtures');fs.mkdirSync(folder,{recursive:true});
 const inputs=[`# Folia ${revision}; line timing and explicit/absent hints; seconds`,
 '# id\tstart\tend\trawHint\trenderEndHint\ttransition\treveal\twords(base64,start,end)'];
 const frames=[`# Folia ${revision}; unmodified cadenzaEnvelopes.ts outputs`,
 '# id\tword\ttime\tstatus\tprogress\tglow\tbody\tdrift\tlineEnvelope\tcharGlow(no timings)\tcharGlow(explicit)'];
 const resolved=['# id\tend\treveal\tlastWordEnd\tenter\texit\thold'];
 const b64=s=>Buffer.from(s).toString('base64');let id=0;
 for(const duration of [-.02,0,.05,.099,.1,.15,.179,.18,.7,3]) {
  for(const hints of ['absent','annotated','custom']) {
   const line={startTime:2,endTime:2+duration,fullText:'Amber echo',words:[
    {text:'Amber ',startTime:2,endTime:2+duration*.4},
    {text:'echo',startTime:2+duration*.4,endTime:2+duration}]};
   if(hints==='annotated')line.renderHints=buildLineRenderHints(line);
   if(hints==='custom')line.renderHints={rawDuration:.13,renderEndTime:line.endTime+.5,
    timingClass:'short',lineTransitionMode:duration<.1?'none':'fast',wordRevealMode:duration<.18?'instant':'fast'};
   const h=line.renderHints;
   inputs.push([id,line.startTime,line.endTime,h?.rawDuration??'-',h?.renderEndTime??'-',h?.lineTransitionMode??'-',h?.wordRevealMode??'-',
    line.words.map(w=>[b64(w.text),w.startTime,w.endTime].join(',')).join('|')].join('\t'));
   const timing=e.resolveLineRenderTiming(line),tr=timing.transitionTiming;
   resolved.push([id,timing.lineRenderEndTime,timing.wordRevealMode,timing.lastWordEndTime,tr.enterDuration,tr.exitDuration,tr.linePassHold].join('\t'));
   const times=new Set([0,1.7,1.82,1.955,2,2.000001,2.01,2.02,2.045,2.067,2.1,2.3,3,4,6,8,10]);
   for(const w of line.words)for(const delta of [-.180001,-.18,-.179999,-.045001,-.045,0,.000001,.01,.067,.12,.14,.8,.9,5]){
    times.add(w.startTime+delta);times.add(w.endTime+delta);
   }
   const explicit=[{char:'Am',startTime:2,endTime:2.001},{char:'b',startTime:2.001,endTime:2.4},
    {char:'e',startTime:2.2,endTime:2.1},{char:'r',startTime:2.4,endTime:2.4}];
   for(const time of [...times].sort((a,b)=>a-b))for(const[wi,w]of line.words.entries()){
    const env=e.getClassicLineEnvelope(time,line,timing);
    frames.push([id,wi,time,e.getWordStatus(time,timing,w),e.getWordProgress(time,timing.wordRevealMode,w),
     e.getClassicGlowEnvelope(time,timing,w),e.getClassicBodyMix(time,timing,w),e.getClassicPassedDrift(time,w),
     [env.opacity,env.scale,env.blur].join(','),[0,1,2,3,4,5].map(i=>e.getClassicCharGlow(time,w,i,6)).join(','),
     [0,1,2,3,4,5].map(i=>e.getClassicCharGlow(time,w,i,6,explicit)).join(',')].join('\t'));
   }
   id++;
  }
 }
 for(const[name,rows]of Object.entries({envelopeInputs:inputs,envelopes:frames,timings:resolved}))
  fs.writeFileSync(path.join(folder,`${name}.tsv`),rows.join('\n')+'\n');
 const shadows=[`# Folia ${revision}; intensity\tblurScale\tactual CSS shadow`];
 for(const value of [-1,0,.009,.01,.010001,.2,.8,1,1.6,2])for(const scale of [1,1.22])
  shadows.push([value,scale,e.buildDomTextShadow('#f17b4d',value,scale)].join('\t'));
 fs.writeFileSync(path.join(folder,'shadows.tsv'),shadows.join('\n')+'\n');
 console.log(JSON.stringify({revision,scenarios:id,frames:frames.length-2,shadows:shadows.length-1}));
} finally {await server.close();}
