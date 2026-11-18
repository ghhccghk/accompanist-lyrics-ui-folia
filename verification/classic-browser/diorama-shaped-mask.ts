// Whole-run reference from original rasterDioramaUnit, not isolated source units.
// The original isolated-unit stage is retained separately; this tests shaping
// preservation and never purports to replace its visual acceptance matrix.
import {rasterDioramaUnit} from '../../.work/folia-major/src/components/visualizer/diorama/dioramaTextRaster';
import {splitDioramaLyricUnits} from '../../.work/folia-major/src/components/visualizer/diorama/dioramaSceneLayout';
import {buildLineGraphemeTimeline} from '../../.work/folia-major/src/utils/lyrics/graphemeTiming';
import latin from '../assets/FoliaParityLatin-Bold.ttf?url';
import arabic from '../assets/FoliaParityBidi-Bold.ttf?url';
import cjk from '../assets/FoliaParityChatCJK-Bold.otf?url';
for(const [name,url]of [['Latin',latin],['Bidi',arabic],['Cjk',cjk]]){
 const face=new FontFace('Shaped '+name,`url(${url})`,{weight:'700'});await face.load();document.fonts.add(face);
}
const inputs=[
 {name:'italic',text:'fij Wavy',pieces:['f','i','j',' ','Wavy'],family:'Shaped Latin',italic:true},
 {name:'arabic',text:'مرحبا بالعالم',pieces:['مر','حبا',' ','بال','عالم'],family:'Shaped Bidi'},
 {name:'mixed',text:'Aمرحبا 12B بالعالم',pieces:['Aمر','حبا',' ','12B',' ','بال','عالم'],family:'Shaped Bidi'},
 {name:'cjk',text:'星光回响',pieces:['星光回响'],family:'Shaped Cjk'}];
const captures=inputs.map(input=>{
 const fontSpec=`${input.italic?'italic ':''}700 128px "${input.family}"`;
 const line={fullText:input.text,startTime:.7,endTime:2,words:input.pieces.map((text,i)=>({text,startTime:(700+i*100)/1000,endTime:(800+i*100)/1000}))};
 const units=splitDioramaLyricUnits(line,buildLineGraphemeTimeline(line));
 const raster=rasterDioramaUnit(input.text,fontSpec);
 return {...input,line,units,advance:raster.advancePx,width:raster.canvasWidthPx,height:raster.canvasHeightPx,
  png:raster.baseTexture.image.toDataURL()};
});
Object.assign(window,{shapedMaskReady:true,shapedMasks:captures});
