import fs from 'node:fs';import path from 'node:path';
import {referenceModules,root,revision}from'./reference.mjs';
const{server,load}=await referenceModules();
try{
 const{buildWordPlacements}=await load('components/visualizer/cadenza/cadenzaPlacements.ts');
 const{splitGraphemes}=await load('components/visualizer/cadenza/cadenzaEnvelopes.ts');
 const b64=s=>Buffer.from(s).toString('base64');
 // Fragment geometry is an explicit seam, not claimed browser font layout.
 const f=(wi,word,text,startX,endX,start=0,end=splitGraphemes(text).length)=>({wordIndex:wi,
  word:{text:word,startTime:1+wi*.1,endTime:1.5+wi*.1},text,color:'#f17b4d',startX,endX,
  fragmentStartInWord:start,fragmentEndInWord:end,wordGraphemeCount:splitGraphemes(word).length,
  wordGraphemeTimings:[{char:word,startTime:1+wi*.1,endTime:1.5+wi*.1}]});
 const cases=[
  [],[{width:80,fragments:[f(0,'Amber','Amber',0,80)]}],
  [{width:300,fragments:[f(0,'Amber','Amber',0,90),f(1,'echoes','echoes',98,190),f(2,'beyond','beyond',200,300)]}],
  [{width:160,fragments:[f(0,'no-no','no-',0,100),f(1,'light','light',106,160)]},{width:60,fragments:[f(0,'no-no','no',0,60,3,5)]}],
  [{width:120,fragments:[f(0,'no-no','no-',0,120)]},{width:80,fragments:[f(0,'no-no','no',0,80,3,5)]}],
  [{width:210,fragments:[f(0,'远方','远方',0,60),f(1,'かなた','かなた',65,140),f(2,'사랑','사랑',145,210)]}],
  [{width:140,fragments:[f(0,'e\u0301','e\u0301',0,24),f(1,'👩‍🔬','👩‍🔬',30,70),f(2,'echo','echo',75,140)]}],
  [{width:140,fragments:[f(0,'\ufeff','\ufeff',0,0),f(1,' ',' ',4,9),f(2,'\u0085','\u0085',12,20),f(3,'x','x',22,38)]}],
  Array.from({length:4},(_,i)=>({width:240,fragments:Array.from({length:6},(_,j)=>f(i*6+j,'echo','echo',j*40,j*40+38))})),
  [{width:1600,fragments:[f(0,'An unusually wide hero','An unusually wide hero',0,1500),f(1,'x','x',1501,1600)]}],
  [{width:20,fragments:[f(0,'','',0,0),f(1,'...','...',0,20)]}],
  [{width:300,fragments:[f(0,'same','same',0,60),f(1,'same','same',0,60),f(2,'same','same',0,60)]}],
 ];
 const inputs=[`# Folia ${revision}; id\tfontPx\tlineHeight\tmaxWidth\tintensity\tseed\tinterlude\tlineCount`];
 const fragments=['# id\trow\tlineWidth\twordIndex\tword64\ttext64\tx0\tx1\tstartInWord\tendInWord\tcount\tfragmentIndex\tfragmentCount\tprimary\tsplit'];
 const expected=['# id\torder\tplacementId64\twordIndex\tx,y,width,height,rotate,scale,passedRotate,driftX,driftY,entryX,entryY,emphasis\tinterlude'];
 let id=0;
 for(const original of cases)for(const intensity of ['normal','calm','chaotic'])for(const seed of [0,1327.4,-733.5]){
  const lines=structuredClone(original);const counts=new Map(),seen=new Map();
  for(const l of lines)for(const p of l.fragments)counts.set(p.wordIndex,(counts.get(p.wordIndex)??0)+1);
  lines.forEach((l,li)=>{l.line={width:l.width};l.lineStart=0;l.lineEnd=100;
   l.fragments.forEach(p=>{p.lineIndex=li;p.fragmentIndexInWord=seen.get(p.wordIndex)??0;
    seen.set(p.wordIndex,p.fragmentIndexInWord+1);p.fragmentCountInWord=counts.get(p.wordIndex);
    p.isPrimaryFragment=p.fragmentIndexInWord===0;p.isSplitAcrossLines=p.fragmentCountInWord>1;
    fragments.push([id,li,l.width,p.wordIndex,b64(p.word.text),b64(p.text),p.startX,p.endX,p.fragmentStartInWord,p.fragmentEndInWord,
     p.wordGraphemeCount,p.fragmentIndexInWord,p.fragmentCountInWord,p.isPrimaryFragment,p.isSplitAcrossLines].join('\t'));
   });
  });
  const font=seed<0?45.5:32,lineHeight=Math.round(font*1.1),maxWidth=seed===0?320:seed>0?180:620,interlude=seed<0;
  inputs.push([id,font,lineHeight,maxWidth,intensity,seed,interlude,lines.length].join('\t'));
  const ps=buildWordPlacements(lines,font,lineHeight,maxWidth,intensity,seed,interlude);
  ps.forEach((p,i)=>expected.push([id,i,b64(p.id),p.wordIndex,[p.x,p.y,p.width,p.height,p.rotate,p.scale,p.passedRotate,
   p.passedDriftX,p.passedDriftY,p.entryOffsetX,p.entryOffsetY,p.emphasis].join(','),p.isInterlude].join('\t')));
  id++;
 }
 const folder=path.join(root,'verification/cadenza-fixtures');fs.mkdirSync(folder,{recursive:true});
 for(const[name,rows]of Object.entries({placementInputs:inputs,placementFragments:fragments,placements:expected}))
  fs.writeFileSync(path.join(folder,name+'.tsv'),rows.join('\n')+'\n');
 console.log(JSON.stringify({revision,scenarios:id,placements:expected.length-1}));
}finally{await server.close();}
