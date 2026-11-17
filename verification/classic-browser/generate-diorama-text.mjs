// Execute untouched original timing and unit splitting; no geometry/background.
import fs from 'node:fs';import path from 'node:path';import {createHash}from'node:crypto';
import {referenceModules,root,upstream,revision}from'./reference.mjs';
const{server,load}=await referenceModules();
try{
 const{buildLineGraphemeTimeline}=await load('utils/lyrics/graphemeTiming.ts');
 const{splitDioramaLyricUnits}=await load('components/visualizer/diorama/dioramaSceneLayout.ts');
 const rows=fs.readFileSync(path.join(root,'verification/cappella-fixtures/reveal-inputs.tsv'),'utf8').split(/\r?\n/).filter(s=>s&&!s.startsWith('#')).map(s=>s.split('\t'));
 const decode=s=>Buffer.from(s,'base64').toString('utf8'),encode=s=>Buffer.from(s).toString('base64');
 const cases=new Map();
 for(const r of rows){if(r[0]==='L')cases.set(r[1],{fullText:decode(r[3]),startTime:+r[4],endTime:+r[5],words:[]});
  else if(r[0]==='W')cases.get(r[1]).words[+r[2]]={text:decode(r[3]),startTime:+r[4],endTime:+r[5],syllables:[]};
  else if(r[0]==='S')cases.get(r[1]).words[+r[3]].syllables[+r[2]]={text:decode(r[4]),startTime:+r[5],endTime:+r[6]};}
 const extra=['●●●','•·■◿','ｦｶﾞかなカナ漢字','𠀀𠀁A','one\u00a0two\ufeffthree','a—b (c)!','a\u0301👩🏽‍🚀🇨🇳','مرحبا بالعالم','क्यों नमस्ते','  \n\t','A😀B','中文Latin中文'];
 extra.forEach((fullText,i)=>cases.set('extra'+i,{fullText,startTime:.7,endTime:4.3,words:[]}));
 const inputs=['# kind case index [word] text64 start end'];const expected=['# G case index text64 start end wordIndex; U case index text64 charStart charEnd start end'];
 let glyphs=0,units=0;
 for(const[key,l]of cases){
  inputs.push(['L',key,0,encode(l.fullText),l.startTime,l.endTime].join('\t'));
  l.words.forEach((w,i)=>{inputs.push(['W',key,i,encode(w.text),w.startTime,w.endTime].join('\t'));(w.syllables??[]).forEach((s,j)=>inputs.push(['S',key,j,i,encode(s.text),s.startTime,s.endTime].join('\t')))});
  const timeline=buildLineGraphemeTimeline(l),groups=splitDioramaLyricUnits(l,timeline);
  timeline.forEach((g,i)=>expected.push(['G',key,i,encode(g.char),g.startTime,g.endTime,g.wordIndex??'-'].join('\t')));
  groups.forEach((u,i)=>expected.push(['U',key,i,encode(u.text),u.charStart,u.charEnd,u.startTime,u.endTime].join('\t')));
  glyphs+=timeline.length;units+=groups.length;
 }
 const out=path.join(root,'verification/diorama-fixtures');
 fs.writeFileSync(path.join(out,'text-inputs.tsv'),inputs.join('\n')+'\n');fs.writeFileSync(path.join(out,'text.tsv'),expected.join('\n')+'\n');
 const sources=['src/utils/lyrics/graphemeTiming.ts','src/components/visualizer/diorama/dioramaSceneLayout.ts','src/components/visualizer/diorama/dioramaSceneTypes.ts'];
 const conditions={revision,cases:cases.size,glyphs,units,scope:'Actual unmodified original shared timing + splitDioramaLyricUnits via Vite SSR/Intl.Segmenter. Reused synthetic Cappella cases plus explicit Diorama category boundaries. No rendering/camera/visual acceptance.',sources:sources.map(file=>({file,sha256:createHash('sha256').update(fs.readFileSync(path.join(upstream,file))).digest('hex')}))};
 fs.writeFileSync(path.join(out,'text-conditions.json'),JSON.stringify(conditions,null,2)+'\n');console.log(JSON.stringify(conditions));
}finally{await server.close();}
