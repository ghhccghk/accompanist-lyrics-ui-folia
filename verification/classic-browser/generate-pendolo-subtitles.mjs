import fs from'node:fs';import path from'node:path';import{referenceModules,root,revision}from'./reference.mjs';
const{server,load}=await referenceModules();
try{
 const{resolvePendoloSubtitleTracks:tracks,sumPendoloSubtitleTrackHeights:height}=await load('components/visualizer/pendolo/pendoloSubtitleTracks.ts');
 const cases=[{}, {translation:'A synthetic translation',romanization:'Amber echoes'}, {translation:' same ',romanization:'same'},
  {translation:'//',romanization:'●●●'}, {translation:'１２３',romanization:'Ⅷ ²'}, {translation:'星光 回响',romanization:'e\u0301'},
  {translation:'𠀀',romanization:'𝟠'}, {translation:'\ufeff words \u3000',romanization:'\u0085'},
  {translation:' ',alternateTexts:[{role:'translation',text:' '},{role:'translation',text:'first valid'},{role:'translation',text:'second'}]},
  {translation:'//',romanization:'romaji',alternateTexts:[{role:'translation',text:'must not replace direct placeholder'}]},
  {romanization:' ',alternateTexts:[{role:'romanization',text:'same'},{role:'translation',text:'same'}]}];
 const b=s=>Buffer.from(s).toString('base64')||'-';
 const input=['# C key mode hidden focal scale translation64 romanization64; A key role text64'],out=['# C key trackCount height; T key index role text64 fontPx lineHeight fallbackWeight gapEm opacity'];let count=0;
 for(const source of cases)for(const mode of['none','translation','romanization','both'])for(const focal of[false,true])for(const scale of[.8,1,1.33])for(const hidden of[false,true]){
  const key=`s${count++}`;input.push(['C',key,mode,+hidden,+focal,scale,b(source.translation??''),b(source.romanization??'')].join('\t'));
  for(const a of source.alternateTexts??[])input.push(['A',key,a.role,b(a.text)].join('\t'));
  const result=tracks(source,{mode,hidden,focal,subtitleFontScale:scale});
  // Controlled wrap heights are independent inputs for sum validation, not
  // source text measurements disguised as native geometry.
  const measured=t=>t.text.length*1.137+t.lineHeightPx*2;
  out.push(['C',key,result.length,height(result,measured)].join('\t'));
  result.forEach((t,i)=>out.push(['T',key,i,t.role,b(t.text),t.fontPx,t.lineHeightPx,t.fontWeightFallback,t.gapEm,t.opacityFactor].join('\t')));
 }
 const dest=path.join(root,'verification/pendolo-fixtures');
 fs.writeFileSync(path.join(dest,'subtitle-inputs.tsv'),input.join('\n')+'\n');fs.writeFileSync(path.join(dest,'subtitles.tsv'),out.join('\n')+'\n');
 fs.writeFileSync(path.join(dest,'subtitle-conditions.json'),JSON.stringify({revision,cases:count,scope:'Unchanged source subtitle/alternate helpers. Controlled text-only height callback; NOT actual typography measurement or rendered subtitle acceptance.'},null,2)+'\n');
 console.log(JSON.stringify({cases:count,tracks:out.filter(x=>x.startsWith('T\t')).length}));
}finally{await server.close()}
