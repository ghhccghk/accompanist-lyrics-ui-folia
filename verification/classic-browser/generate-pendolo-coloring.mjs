import fs from'node:fs';import path from'node:path';import{referenceModules,root,revision}from'./reference.mjs';
const{server,load}=await referenceModules();
try{
 const{prepareWordColorMatchers,buildWordColorRangesFromMatchers,resolveTokenColorMap}=await load('components/visualizer/wordColoring.ts');
 const{buildPendoloColorRuns}=await load('components/visualizer/pendolo/pendoloColorRuns.ts');
 const{splitLyricGraphemes}=await load('utils/lyrics/graphemeTiming.ts');
 const cases=[
  ['Amber echoes beyond',[['Amber','#ed71b8'],['beyond','#73dea1']]],
  ['Echo echo echoes ECHO_echo echo!',[['echo','#f00'],['echo longword','#0f0'],['echo','#00f'],['echoes','#777']]],
  ['星光向远方 星光向远方',[['星光','#f00'],['星光向远方','#0f0'],['向远方','#00f']]],
  ['e\u0301 e e\u0301',[['e','#de89ff']]],
  ['👩🏽‍💻 family 👨‍👩‍👧‍👦 👩🏽‍💻',[['family','#abc'],['👩🏽‍💻','#def']]],
  ['مرحبا بالعالم',[['مرحبا','#f00'],['بالعالم','#0f0']]],
  ['向远方 星光 回响',[['向远方','#8ab4f8'],['星光 回响','#ef55ae']]],
  ['가나다 カタカナ 㐀 漢字',[['가나다','#abc'],['カタカナ','#def'],['㐀','#f00'],['漢字','#0f0']]],
  ['I İ i ı _3',[['İ','#123'],['_3','#456'],['   ','#fff']]],
  ['echo\u0085echo echo\ufeffecho',[['echo\u0085echo','#abc'],['echo\ufeffecho','#def']]],
 ];
 const rows=['# C key text64 fallback; E key word64 color; R key start end color priority; G key index color; U key globalStart row64; P key row start text64 color'],b=s=>Buffer.from(s).toString('base64')||'-';
 for(const[index,[text,entries]]of cases.entries()){
  const key=String(index),fallback='#aabbcc',words=entries.map(([word,color])=>({word,color}));rows.push(['C',key,b(text),fallback].join('\t'));
  for(const w of words)rows.push(['E',key,b(w.word),w.color].join('\t'));
  const ranges=buildWordColorRangesFromMatchers(text,prepareWordColorMatchers(words,true));for(const r of ranges)rows.push(['R',key,r.startOffset,r.endOffset,r.color,r.priority].join('\t'));
  let cursor=0;const gs=splitLyricGraphemes(text),tokens=gs.map((g,i)=>{const startOffset=cursor;cursor+=g.length;return{key:String(i),timed:true,startOffset,endOffset:cursor}}),colors=resolveTokenColorMap(tokens,ranges);
  for(const[i,c]of colors)rows.push(['G',key,i,c].join('\t'));
  for(const start of[0,1,Math.floor(gs.length/2)]){const row=gs.slice(start).join('');rows.push(['U',key,start,b(row)].join('\t'));for(const run of buildPendoloColorRuns(row,start,colors,fallback))rows.push(['P',key,start,run.key,b(run.text),run.color].join('\t'));}
 }
 fs.writeFileSync(path.join(root,'verification/pendolo-fixtures/coloring.tsv'),rows.join('\n')+'\n');
 fs.writeFileSync(path.join(root,'verification/pendolo-fixtures/coloring-reference.json'),JSON.stringify({revision,cases:cases.length,scope:'Unchanged source range matcher, timed grapheme token map and Pendolo adjacent-color runs. No browser text/raster/chorus acceptance.'},null,2)+'\n');console.log(JSON.stringify({rows:rows.length-1}));
}finally{await server.close()}
