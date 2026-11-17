// Actual pinned source functions; no image assets or fake native oracle.
import fs from'node:fs';import path from'node:path';import{root,revision,referenceModules}from'./reference.mjs';
const{server,load}=await referenceModules();
try{
 const{buildCappellaMessages}=await load('components/visualizer/cappella/cappellaMessages.ts');
 const{getCappellaIntensityConfig}=await load('components/visualizer/cappella/cappellaIntensity.ts');
 const{buildLineGraphemeTimeline}=await load('utils/lyrics/graphemeTiming.ts');
 const{buildLineRenderHints}=await load('utils/lyrics/renderHints.ts');
 const b=s=>Buffer.from(s).toString('base64');
 const input=['# case index text64 startMs endMs agent64'],out=['# M case intensity line side avatar; G case line index text64 start end; R case line renderEnd'];
 const cases=[[],['solo'],['lead','guest'],[' lead ','guest','third','']];
 for(let c=0;c<cases.length;c++){
  const lines=Array.from({length:8},(_,i)=>({fullText:`Synthetic line ${i}`,startTime:(700+i*1100)/1000,endTime:(1500+i*1100)/1000,words:[],agentId:cases[c][i%cases[c].length]}));
  lines.forEach((l,i)=>{input.push([c,i,b(l.fullText),Math.round(l.startTime*1000),Math.round(l.endTime*1000),b(l.agentId??'')||'-'].join('\t'));
   buildLineGraphemeTimeline(l).forEach((g,j)=>out.push(['G',c,i,j,b(g.char),g.startTime,g.endTime].join('\t')));
   out.push(['R',c,i,buildLineRenderHints(l).renderEndTime].join('\t'));
  });
  for(const intensity of['calm','normal','chaotic']){
   const messages=buildCappellaMessages(lines,'Synthetic title',getCappellaIntensityConfig(intensity),{showEmoMessages:false},[],false);
   for(const m of messages)if(m.lineIndex!==undefined)out.push(['M',c,intensity,m.lineIndex,m.side,m.avatarIndex].join('\t'));
  }
 }
 const dir=path.join(root,'verification/cappella-fixtures');
 fs.writeFileSync(path.join(dir,'core-inputs.tsv'),input.join('\n')+'\n');
 fs.writeFileSync(path.join(dir,'core.tsv'),out.join('\n')+'\n');
 fs.writeFileSync(path.join(dir,'core-conditions.json'),JSON.stringify({revision,cases:4,lines:32,senderChecks:96,scope:'Real core-equivalent line-only input, source even-grapheme timing/renderHints and explicit/missing/single agent fallback. Text-only, no emoji assets. Unsafe parser splits checked separately as documented shaping adaptation.'},null,2)+'\n');
 console.log('32 source lines / 96 sender checks');
}finally{await server.close()}
