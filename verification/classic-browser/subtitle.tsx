import {resolveVisualizerSubtitleOverlayContent} from '../../.work/folia-major/src/components/visualizer/VisualizerSubtitleOverlay';
import {getRecentCompletedLine,getSubtitleNextLines} from '../../.work/folia-major/src/components/visualizer/runtime';
import {getLineRenderEndTime} from '../../.work/folia-major/src/utils/lyrics/renderHints';
import sceneText from '../classic-fixtures/subtitle-scenes.tsv?raw';
  const lines=sceneText.split(/\r?\n/).filter(r=>r&&!r.startsWith('#')).map(row=>{
    const [start,end,fullText,translation,romanization]=row.split('\t');
    return {startTime:Number(start)/1000,endTime:Number(end)/1000,fullText,translation,romanization,
      words:[{text:fullText,startTime:Number(start)/1000,endTime:Number(end)/1000}]};
  });
  const rows=[`# Original subtitle content/runtime, pinned cc6226e464050a456a2913e062374f23d3844cc5`,'# timeMs\tactive\trecent\tlayout\tvisible\trowsJSON\tupcomingJSON'];
  for(const ms of [0,699,700,1900,2140,2141,2500,3200,4400,4401,4800,5000,6400,7000,8300,9000,10300,12000]) {
    const currentLineIndex=lines.findIndex(l=>ms/1000>=l.startTime&&ms/1000<getLineRenderEndTime(l));
    const recent=getRecentCompletedLine({lines,currentLineIndex,currentTime:ms/1000,getLineEndTime:getLineRenderEndTime});
    const nextLines=getSubtitleNextLines(lines,currentLineIndex,recent);
    for(const layout of ['translation','romanization','current+translation+romanization','current+next+next','translation+romanization+next']) for(const showText of [true,false]) {
      const value=resolveVisualizerSubtitleOverlayContent({showText,activeLine:lines[currentLineIndex]??null,recentCompletedLine:recent,nextLines,subtitleOverlayLayout:layout});
      rows.push([ms,currentLineIndex,lines.indexOf(recent),layout,showText,
        JSON.stringify(value.rows.map(r=>[r.source,r.text])),JSON.stringify(value.upcomingLines.map(l=>lines.indexOf(l)))].join('\t'));
    }
  }

(window as any).contentFixtures=rows.join("\n")+"\n";
