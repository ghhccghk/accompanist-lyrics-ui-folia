// Browser measurement and fragmentation run the actual pinned source.
import {buildPreparedState} from '../../.work/folia-major/src/components/visualizer/cadenza/cadenzaPreparedState';
import {findWordRanges,buildSegmentMetas} from '../../.work/folia-major/src/components/visualizer/cadenza/cadenzaFragments';
import {buildLineRenderHints} from '../../.work/folia-major/src/utils/lyrics/renderHints';
import latinUrl from '../assets/FoliaParityLatin-Bold.ttf?url';
import arabicUrl from '../assets/NotoSansArabic-Bold.ttf?url';
import cjkUrl from '../assets/FoliaParityCJK-Bold.otf?url';
for(const[name,url]of [['Cadenza Latin',latinUrl],['Cadenza Arabic',arabicUrl],['Cadenza CJK',cjkUrl]]) {
 const font=new FontFace(name,`url(${url})`,{weight:'700'});await font.load();document.fonts.add(font);
}
(window as any).calibrate=(line:any,width:number,fontScale:number)=>{
 const theme={fontStyle:'sans',fontFamily:/[\u0600-\u06ff]/.test(line.fullText)?'Cadenza Arabic':/[\u4e00-\u9fa5]/.test(line.fullText)?'Cadenza CJK':'Cadenza Latin',
  fontWeight:700,primaryColor:'#dfe5ec',accentColor:'#f17b4d',animationIntensity:'normal',wordColors:[]};
 const state=buildPreparedState(line,{showText:true,viewport:{width,height:600},theme,tuning:{fontScale,widthRatio:.72}})!;
 const{graphemes,segmentMetas}=buildSegmentMetas(state.prepared);
 const offset=(c:any)=>{const s=segmentMetas[c.segmentIndex];return s?Math.max(s.graphemeStart,Math.min(s.graphemeEnd,s.graphemeStart+c.graphemeIndex)):graphemes.length};
 return{size:[state.fontPx,state.lineHeight,state.maxWidth],text:state.text,
  segments:state.prepared.segments.map((text,i)=>({text,width:state.prepared.widths[i],advances:state.prepared.breakableFitAdvances[i]})),
  lines:state.layout.lines.map(l=>({start:offset(l.start),end:offset(l.end),width:l.width})),
  ranges:findWordRanges(line,graphemes,theme),placements:state.placements,hints:buildLineRenderHints(line)};
};
(window as any).ready=true;
