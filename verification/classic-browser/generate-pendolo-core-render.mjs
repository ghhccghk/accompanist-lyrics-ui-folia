import fs from'node:fs';import path from'node:path';import{referenceModules,root}from'./reference.mjs';
const{server,load}=await referenceModules();
try{
 const{buildLineRenderHints}=await load('utils/lyrics/renderHints.ts');
 const cases=[[0,0],[0,80],[1000,1120],[1000,1180],[700,2800],[100,500],
  ...[0,100,700,1000,12345].flatMap(start=>[1,67,99,100,101,119,179,180,181,1000].map(duration=>[start,start+duration]))];
 const rows=['# declaredStartMillis declaredEndMillis originalRenderEndSeconds'];
 for(const[start,end]of cases)rows.push([start,end,buildLineRenderHints({startTime:start/1000,endTime:end/1000,words:[]}).renderEndTime].join('\t'));
 fs.writeFileSync(path.join(root,'verification/pendolo-fixtures/core-render.tsv'),rows.join('\n')+'\n');
}finally{await server.close()}
