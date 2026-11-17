import fs from 'node:fs';import path from 'node:path';import{createHash}from'node:crypto';import{stripTypeScriptTypes}from'node:module';
import{root,upstream,revision,referenceModules}from'./reference.mjs';
const{server,load}=await referenceModules();
try{
 const{buildCappellaMessages}=await load('components/visualizer/cappella/cappellaMessages.ts');
 const{getCappellaIntensityConfig}=await load('components/visualizer/cappella/cappellaIntensity.ts');
 const{getVisibleLineIndexAtTime}=await load('components/visualizer/cappella/cappellaMessageLayout.ts');
 const layoutFile='src/components/visualizer/cappella/cappellaMessageLayout.ts';
 const source=fs.readFileSync(path.join(upstream,layoutFile),'utf8');
 const exact=['getVisibleMessages','getTimedMessageState'].map(name=>source.match(new RegExp(`export const ${name} = [\\s\\S]+?\\n};`))[0]).join('\n');
 const typeSource=fs.readFileSync(path.join(upstream,'src/components/visualizer/cappella/cappellaTypes.ts'),'utf8');
 const isTimed=typeSource.match(/export const isTimedMessage[\s\S]+?;/)[0];
 const code=`const MAX_VISIBLE_MESSAGES=20;let heights;export const setHeights=h=>heights=h;const getEstimatedMessageHeight=(m,a)=>heights[m.id][a?1:0];\n${isTimed}\n${exact}`;
 const window=await import('data:text/javascript;base64,'+Buffer.from(stripTypeScriptTypes(code)).toString('base64'));
 const b=s=>Buffer.from(s).toString('base64'),inputs=['# kind case index text64 start end agent64'],expected=['# M message; Q visible/viewport; H heights'];
 let cases=0,messageCount=0,queries=0;
 for(const intensity of['calm','normal','chaotic'])for(let scenario=0;scenario<8;scenario++){
  const key=`${intensity}${scenario}`,lines=[];
  const count=scenario===0?0:scenario===1?1:scenario===2?9:48;
  for(let i=0;i<count;i++){
   const fullText=[`long synthetic sentence number ${i} ends here.`,`短句${i}`,`e\u0301 👩🏽‍🚀 ${i}`,'......','  ','\u0085'.repeat(13),`\ufeff${i}\u3000`][i%7];
   const startTime=scenario===7?[2,1.001,0,.1,.000001,.0000001,10000000,-0][i%8]+Math.floor(i/8)*3:i*1.137;
   const agentId=scenario===4?[' lead ','guest','third','fourth','fifth','sixth','seventh',undefined][i%8]:
    scenario===5?['solo',' solo ',undefined,'\ufeff'][i%4]:scenario===6?['\ufeffalpha\u3000','beta','\u0085',''][i%4]:undefined;
   const l={fullText,startTime,endTime:startTime+.8,words:[],agentId};lines.push(l);
   inputs.push(['L',key,i,b(fullText),l.startTime,l.endTime,b(agentId??'')||'-'].join('\t'));
  }
  inputs.push(['C',key,intensity,b('Synthetic title')].join('\t'));
  const config=getCappellaIntensityConfig(intensity),messages=buildCappellaMessages(lines,'Synthetic title',config,{showEmoMessages:false},[],false);
  for(const[i,m]of messages.entries()){
   expected.push(['M',key,i,b(m.id),b(m.kind==='title'?m.text:m.line.fullText),m.side,m.avatarIndex,m.lineIndex??-1].join('\t'));messageCount++;
  }
  const heights={};for(const[i,m]of messages.entries()){
   heights[m.id]=[23+(i*31)%111,73+(i*47)%137];expected.push(['H',key,b(m.id),...heights[m.id]].join('\t'));
  }
  window.setHeights(heights);
  for(const time of[-1,0,.1,1.137,3,6,12,30,10000001])for(const h of[180,400,900,10000]){
   const visible=getVisibleLineIndexAtTime(lines,time),current=visible>=0&&time<lines[visible].endTime?visible:-1;
   const result=window.getVisibleMessages(messages,visible,h,current,time,config.motion,{},18,220);
   expected.push(['Q',key,time,h,visible,current,result.map(m=>messages.indexOf(m)).join(',')].join('\t'));queries++;
  }
  cases++;
 }
 const out=path.join(root,'verification/cappella-fixtures');
 fs.writeFileSync(path.join(out,'message-inputs.tsv'),inputs.join('\n')+'\n');fs.writeFileSync(path.join(out,'messages.tsv'),expected.join('\n')+'\n');
 fs.writeFileSync(path.join(out,'messages-conditions.json'),JSON.stringify({revision,cases,messages:messageCount,queries,
  source:'cappellaMessages/cappellaMessageSenders/cappellaIntensity unchanged modules, Vite SSR',
  scope:'Text-only source configuration showEmoMessages=false, empty image pool. Original viewport declaration with controlled estimated-height boundary, NOT browser measurement or geometry acceptance.',
  source_window_sha256:createHash('sha256').update(exact).digest('hex'),layoutFile},null,2)+'\n');
 console.log(JSON.stringify({cases,messages:messageCount,queries}));
}finally{await server.close()}
