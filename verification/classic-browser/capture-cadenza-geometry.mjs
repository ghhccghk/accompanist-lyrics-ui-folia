import fs from 'node:fs';import path from 'node:path';import{createHash}from'node:crypto';
import{chromium}from'playwright';import{checkReference,root,revision}from'./reference.mjs';
checkReference();const folder=path.join(root,'verification/cadenza-fixtures');
const browser=await chromium.launch({headless:true});
try{
 const page=await browser.newPage();await page.goto('http://127.0.0.1:4173/cadenza-geometry.html');await page.waitForFunction(()=>window.ready);
 const input=fs.readFileSync(path.join(root,'verification/fume-fixtures/inputs.tsv'),'utf8').split(/\r?\n/).filter(s=>s&&!s.startsWith('#'));
 const b64=s=>Buffer.from(s).toString('base64'),decode=s=>Buffer.from(s,'base64').toString('utf8');
 input.push([input.length,b64('no-no amber echo echo'),.7,2.8,[['no-no',.7,1],['amber',1,1.5],['echo',1.5,2],['echo',2,2.8]].map(w=>[b64(w[0]),w[1],w[2],''].join('~')).join('|')].join('\t'));
 const rows={geometryInputs:['# id\tlyric\twidth\tfontScale\ttext64\tfontPx\tlineHeight\tmaxWidth'],
  segments:['# id\tsegment\ttext64\twidth\tadvances'],wraps:['# id\trow\tstart\tend\twidth'],
  wordRanges:['# id\tword\tstart\tend\tcolor\ttimings(char64,start,end;...)'],
  realPlacements:['# id\torder\tid64\tword\ttext64\tstartInWord\tendInWord\tcount\tx,y,width,height,scale,passedRotate,driftX,driftY,emphasis'],
  annotations:['# lyric\traw\trenderEnd\ttransition\treveal']};
 let id=0;
 for(const str of input){const f=str.split('\t');const words=f[4]==='-'?[]:f[4].split('|').map(d=>{const w=d.split('~');return{text:decode(w[0]),startTime:+w[1],endTime:+w[2],
  syllables:w[3]?w[3].split(';').map(s=>{const x=s.split('@');return{text:decode(x[0]),startTime:+x[1],endTime:+x[2]}}):[]}});
  const line={fullText:decode(f[1]),startTime:+f[2],endTime:+f[3],words};
  for(const width of [180,380,800])for(const scale of [1.12,.85]){
   const state=await page.evaluate(([l,w,s])=>window.calibrate(l,w,s),[line,width,scale]);
   rows.geometryInputs.push([id,f[0],width,scale,b64(state.text),...state.size].join('\t'));
   state.segments.forEach((s,i)=>rows.segments.push([id,i,b64(s.text),s.width,s.advances?.join(',')||'-'].join('\t')));
   state.lines.forEach((l,i)=>rows.wraps.push([id,i,l.start,l.end,l.width].join('\t')));
   state.ranges.forEach(r=>rows.wordRanges.push([id,r.wordIndex,r.start,r.end,r.color,
    r.graphemeTimings.map(t=>[b64(t.char),t.startTime,t.endTime].join(',')).join(';')||'-'].join('\t')));
   state.placements.forEach((p,i)=>rows.realPlacements.push([id,i,b64(p.id),p.wordIndex,b64(p.text),p.fragmentStartInWord,p.fragmentEndInWord,p.wordGraphemeCount,
    [p.x,p.y,p.width,p.height,p.scale,p.passedRotate,p.passedDriftX,p.passedDriftY,p.emphasis].join(',')].join('\t')));
   if(width===180&&scale===1.12){const h=state.hints;rows.annotations.push([f[0],h.rawDuration,h.renderEndTime,h.lineTransitionMode,h.wordRevealMode].join('\t'));}
   id++;
  }
 }
 fs.writeFileSync(path.join(folder,'fragmentInputs.tsv'),`# Folia ${revision}; synthetic lyric inputs\n`+input.join('\n')+'\n');
 for(const[name,data]of Object.entries(rows))fs.writeFileSync(path.join(folder,name+'.tsv'),data.join('\n')+'\n');
 const conditions={revision,browser:browser.version(),scenarios:id,widths:[180,380,800],fontScales:[1.12,.85],DPR:1,
  fonts:['FoliaParityLatin-Bold.ttf','NotoSansArabic-Bold.ttf','FoliaParityCJK-Bold.otf'].map(name=>({name,sha256:createHash('sha256').update(fs.readFileSync(path.join(root,'verification/assets',name))).digest('hex')})),
  scope:'Actual pretext measured segments/wraps + pinned fragments/placements. Native font measurement still separate.'};
 fs.writeFileSync(path.join(folder,'geometry-conditions.json'),JSON.stringify(conditions,null,2)+'\n');console.log(JSON.stringify(conditions));
}finally{await browser.close();}
