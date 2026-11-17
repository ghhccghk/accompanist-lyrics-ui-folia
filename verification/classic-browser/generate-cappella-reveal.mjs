// Execute the pinned source modules unchanged. No screenshots or synthetic native oracle.
import fs from 'node:fs';import path from 'node:path';import {createHash} from 'node:crypto';
import {root,upstream,revision,referenceModules} from './reference.mjs';
const {server,load}=await referenceModules();
try {
 const reveal=await load('components/visualizer/cappella/cappellaReveal.ts');
 const {buildLineGraphemeTimeline}=await load('utils/lyrics/graphemeTiming.ts');
 const word=(text,startTime,endTime,syllables)=>({text,startTime,endTime,...(syllables?{syllables}: {})});
 const line=(fullText,startTime,endTime,words)=>({fullText,startTime,endTime,words});
 const cases=[
  line('',1,3,[]),line('Only a line.',1,3,[]),line('  hello, world! ',1,5,[word('hello',1.2,2),word('world',3,4)]),
  line('go go go',1,4,[word('go',1,1.5),word('go',2,2.5),word('go',3,3.5)]),
  line('a?',1,4,[word('missing',1.2,2)]),line('tail!?',1,4,[word('tail',1,2)]),
  line('瞬时',1,1,[word('瞬时',1,1)]),line('逆序',3,1,[word('逆序',3,1)]),
  line(' syllable ',1,5,[word('syllable',1.2,4,[word('syll',1.2,2),word('able',2.8,4)])]),
  line('e\u0301 👩🏽‍🚀 🇨🇳!',1,4,[word('e\u0301',1,1.5),word('👩🏽‍🚀',2,3),word('🇨🇳',3,3.5)]),
  line('مرحبا بالعالم',1,4,[word('مرحبا',1,2),word('بالعالم',2.5,4)]),
  line('क्यों नमस्ते',1,4,[word('क्यों',1,2),word('नमस्ते',2.5,4)]),
  line('空 白\n行。',.1,4,[word('空',.1,.6),word('白',1,2),word('行',2.5,3)]),
  line('abcd',1,5,[word('ab',3,4),word('cd',1,2)]),
  line('abc',1,4,[word('',1,1),word('a',1,2),word('bc',2,3)]),
 ];
 for(let i=0;i<40;i++)cases.push(line(`  seed${i} 中文!`,i*.13,i*.13+3,
  [word(`seed${i}`,i*.13+.2,i*.13+1.3),word('中文',i*.13+1.7,i*.13+2.4)]));
 const inputs=['# kind case index [word] text64 start end'],expected=['# kind case index/time fields'];
 const b64=s=>Buffer.from(s).toString('base64');let queries=0,glyphs=0;
 const unicode=[];
 for(const [ci,l]of cases.entries()){
  const key=`case${ci}`;inputs.push(['L',key,0,b64(l.fullText),l.startTime,l.endTime].join('\t'));
  for(const[wi,w]of l.words.entries()){
   inputs.push(['W',key,wi,b64(w.text),w.startTime,w.endTime].join('\t'));
   for(const[si,s]of(w.syllables??[]).entries())inputs.push(['S',key,si,wi,b64(s.text),s.startTime,s.endTime].join('\t'));
  }
  const characters=reveal.getLineCharacters(l),times=reveal.buildCharacterRevealTimes(l,characters),fade=reveal.buildCharacterFadeDurationsMs(l,characters);
  const timeline=buildLineGraphemeTimeline(l);
  for(const[i,c]of characters.entries()){
   expected.push(['G',key,i,b64(c),times[i],timeline[i].endTime,fade[i]].join('\t'));glyphs++;
  }
  expected.push(['R',key,reveal.getTimestampReadyTimeFromMetrics(l,times,fade)].join('\t'));
  const ts=new Set([-1,0,99,l.startTime,l.endTime]);
  for(const t of times)for(const d of[-.200001,-.2,-.000001,0,.000001,.04,.22])ts.add(t+d);
  for(const t of ts){expected.push(['Q',key,t,reveal.getCharacterCountAtTime(times,t),reveal.getCharacterCountAtTime(times.map(t=>t-.2),t)].join('\t'));queries++;}
  const plan=reveal.getCharacterRevealPlan(l);
  if(plan.characters.length!==characters.length)unicode.push({case:key,text:l.fullText,metrics_graphemes:characters,dom_codepoints:plan.characters});
 }
 const out=path.join(root,'verification/cappella-fixtures');fs.mkdirSync(out,{recursive:true});
 fs.writeFileSync(path.join(out,'reveal-inputs.tsv'),inputs.join('\n')+'\n');
 fs.writeFileSync(path.join(out,'reveal.tsv'),expected.join('\n')+'\n');
 const sources=['src/utils/lyrics/graphemeTiming.ts','src/components/visualizer/cappella/cappellaReveal.ts'];
 fs.writeFileSync(path.join(out,'reveal-conditions.json'),JSON.stringify({revision,cases:cases.length,glyphs,queries,
  method:'Unchanged source modules loaded by Vite SSR; Intl.Segmenter enabled. Native uses EGC display and media-time fade; source DOM codepoint mismatch recorded, not emulated.',
  sources:sources.map(file=>({file,sha256:createHash('sha256').update(fs.readFileSync(path.join(upstream,file))).digest('hex')})),unicode_display_discrepancies:unicode},null,2)+'\n');
 console.log(JSON.stringify({cases:cases.length,glyphs,queries,sourceDOMDiscrepancies:unicode.length}));
} finally {await server.close();}
