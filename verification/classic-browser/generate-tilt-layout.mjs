// Pure Tilt helpers are private in the upstream TSX. Extract declarations verbatim
// at top-level declaration boundaries; measurement/font lookup is a labelled test seam.
import fs from 'node:fs';
import path from 'node:path';
import {referenceModules,root,upstream,revision} from './reference.mjs';
const {server,load}=await referenceModules();
try {
 const source=fs.readFileSync(path.join(upstream,'src/components/visualizer/tilt/VisualizerTilt.tsx'),'utf8');
 const names=['CHAR_REF_LENGTH','LOG_OFFSET','LINE_THRESHOLDS','determineLineCount','REM_PX',
   'RESPLIT_THRESHOLD','SCALE_FLOOR_NORMAL','SCALE_FLOOR_TILT','getAvailableWidth','seededRandom',
   'buildTiltLayout','getCharPulseIntensity','GRAPHEME_SEGMENTER','findSegmentWordRange','buildCharTimings'];
 const declarations=names.map(name=>{
   const start=source.search(new RegExp(`^const ${name} = `,'m'));
   if(start<0)throw Error(`Missing original declaration ${name}`);
   const tail=source.slice(start),lines=tail.split(/\r?\n/);
   const end=lines[0].endsWith(';')?0:lines.findIndex(line=>line==='};');
   if(end<0)throw Error(`Missing end of ${name}`);
   return lines.slice(0,end+1).join('\n');
 }).join('\n');
 const helperFile=path.join(root,'.work/tilt-extracted.ts');
 fs.writeFileSync(helperFile,`import {SentenceLayout} from './folia-major/src/utils/lyrics/sentenceLayout';
 import {buildWordGraphemeTimings} from './folia-major/src/utils/lyrics/graphemeTiming';
 let window={innerWidth:800};
 const resolveThemeFontStack=()=> 'Fixture';
 const resolveThemeFontWeight=(_,fallback)=>fallback;
 const measureAtSize=(text,size,fontSpec)=>text.length*size*(fontSpec.endsWith(' italic')?.61:.57);
 ${declarations}
 export {buildTiltLayout,getCharPulseIntensity,determineLineCount,findSegmentWordRange,buildCharTimings};
 export const setWidth=(width)=>{window.innerWidth=width};`);
 const helpers=await server.ssrLoadModule(helperFile);
 const cases=['Amber echoes cross the quiet synthetic sky','One, two! Three? Four.',
   '  open (inside) close 「引用」 tail','让我们 沿着星光 向远方',
   '星光 Hello quiet sky 风声 and the rain 结束','a very long synthetic sentence to force resplitting into several independently measured sections',
   '...','Stay','مرحبا بالعالم نحن هنا','甲　乙 丙丁 戊己'];
 const b64=s=>Buffer.from(s).toString('base64');
 const layouts=[`# original ${revision}; synthetic measure width=text.length*size*(italic?.61:.57), not visual evidence`,
   '# textB64\tseed\tviewport\tfontScale\tsplitProbability\ttiltProbability\tscale\tsegments(textB64,isTilt,shortLast,offset;...)'];
 for(const text of cases)for(const seed of [0,.7,12.345])for(const width of [280,800,1600])
   for(const fontScale of [.75,1,1.5])for(const p of [0,.75,1])for(const tilt of [0,.35,1]) {
     helpers.setWidth(width);
     const result=helpers.buildTiltLayout(text,seed,{splitProbability:p,tiltStyleProbability:tilt},{},fontScale);
     layouts.push([b64(text),seed,width,fontScale,p,tilt,result.scaleMultiplier,
       result.segments.map(s=>[b64(s.text),s.isTilt,s.isShortLastLine,s.charOffset].join(',')).join(';')].join('\t'));
   }
 const pulses=[`# original ${revision}; currentTime/startTime/endTime/intensity`];
 for(const start of [0,.7])for(const duration of [0,.05,.2,.5,.9,2])for(const elapsed of [-.1,0,.05,.1,.2,.5,.9,1,1.5,3]) {
   const time=start+elapsed,end=start+duration;
   pulses.push([time,start,end,helpers.getCharPulseIntensity(time,{startTime:start,endTime:end})].join('\t'));
 }
 const folder=path.join(root,'verification/tilt-fixtures');fs.mkdirSync(folder,{recursive:true});
 fs.writeFileSync(path.join(folder,'layout.tsv'),layouts.join('\n')+'\n');
 fs.writeFileSync(path.join(folder,'pulse.tsv'),pulses.join('\n')+'\n');
 const timingOut=[`# original ${revision}; source helpers, word descriptor=textB64~start~end~syllables(textB64@start@end+...)`,
   '# fullTextB64\twords(|)\toffset\tsegmentB64\tstart\tend\trangeFirst\trangeEndExclusive\ttimings(index,start,end;...)'];
 const timingCases=[
   [{text:'Amber ',startTime:.7,endTime:1.3},{text:'echo',startTime:1.3,endTime:1.9}],
   [{text:'Á ',startTime:0,endTime:.05},{text:'👩‍👩‍👧‍👦',startTime:.05,endTime:.1}],
   [{text:'مرحبا ',startTime:2,endTime:3},{text:'بالعالم',startTime:3,endTime:4}],
   [{text:'star light',startTime:.7,endTime:2,syllables:[{text:'star ',startTime:.7,endTime:1},{text:'light',startTime:1.2,endTime:2}]}],
 ];
 for(const words of timingCases) {
   const fullText=words.map(w=>w.text).join('');
   const edges=[...new Intl.Segmenter(undefined,{granularity:'grapheme'}).segment(fullText)].map(g=>g.index);
   for(const offset of edges)for(const length of [1,2,fullText.length]) {
     const segment=fullText.slice(offset,edges.find(e=>e>=offset+length)??fullText.length);
     const start=words[0].startTime,end=words.at(-1).endTime;
     const range=helpers.findSegmentWordRange(offset,segment,fullText,words);
     const timings=helpers.buildCharTimings(offset,segment,start,end,{fullText,words});
     const descriptor=words.map(w=>[b64(w.text),w.startTime,w.endTime,
       (w.syllables??[]).map(s=>[b64(s.text),s.startTime,s.endTime].join('@')).join('+')].join('~')).join('|');
     timingOut.push([b64(fullText),descriptor,offset,b64(segment),start,end,range.startWordIndex,range.endWordIndex,
       timings.map(t=>[t.charIndex,t.startTime,t.endTime].join(',')).join(';')||'-'].join('\t'));
   }
 }
 fs.writeFileSync(path.join(folder,'timings.tsv'),timingOut.join('\n')+'\n');
 console.log(`Saved ${layouts.length-2} original layouts, ${pulses.length-1} pulse samples and ${timingOut.length-2} timing cases`);
}finally{await server.close()}
