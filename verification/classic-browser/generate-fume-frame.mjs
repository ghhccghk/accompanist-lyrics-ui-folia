// Recording Canvas seam: executes the untouched source drawing code, not rewritten equations.
import fs from 'node:fs';
import path from 'node:path';
import { referenceModules, root, revision } from './reference.mjs';
const { server, load } = await referenceModules();
try {
  const { buildWordRangesFromWords } = await load('components/visualizer/fume/fumeReveal.ts');
  const { splitGraphemes } = await load('components/visualizer/fume/fumeMath.ts');
  const { buildWordRangeIndexByOffset } = await load('components/visualizer/fume/fumeTextMeasure.ts');
  const { resolveFumeBlockTiming, resolveFumeGlowBases, resolveFumeStaticLayers } = await load('components/visualizer/fume/fumeBlockFrame.ts');
  const { drawFumeLiveBlock } = await load('components/visualizer/fume/fumeCanvasText.ts');
  const decode = s => Buffer.from(s, 'base64').toString('utf8');
  const folder = path.join(root, 'verification/fume-fixtures');
  const word = s => { const f = s.split('~'); return { text: decode(f[0]), startTime: +f[1], endTime: +f[2],
    syllables: f[3] ? f[3].split(';').map(s => { const f = s.split('@'); return { text: decode(f[0]), startTime: +f[1], endTime: +f[2] }; }) : [] }; };
  const inputs = fs.readFileSync(path.join(folder, 'inputs.tsv'), 'utf8').split(/\r?\n/).filter(s => s && !s.startsWith('#'))
    .map(s => { const f = s.split('\t'); return { fullText: decode(f[1]), startTime: +f[2], endTime: +f[3], words: f[4] === '-' ? [] : f[4].split('|').map(word) }; });
  const header = `# Folia ${revision}; original Canvas draw recording; synthetic advance=8.5+index%3; no raster claim`;
  const frames = [header, '# id\tlineId\thero\tmode\ttime\ttiming(wait,active,dim,baseline,end,cutoff,complete,cutoffReached,duration,trail,state)\tglow(fillRGBA;blur;shadowRGBA)\tstatic(paint;layerAlpha|...)'];
  const glyphs = [header, '# frameId\tindex\tpaint(fillRGBA;blur;shadowRGBA)\tstamp(centerX,centerY,width,height;paint), or -'];
  function ink(s) {
    if (s === 'transparent') return '0,0,0,0';
    const m = /^rgba\(([^)]+)\)$/.exec(s);
    if (!m) throw Error(`Unexpected source color ${s}`);
    return m[1].split(',').map(Number).join(',');
  }
  const paint = c => [ink(c.fillStyle), c.shadowBlur, ink(c.shadowColor)].join(';');
  let id = 0, stampCount = 0;
  for (const lineId of [0, 1, 3, 4, 5, 6, 7, 9]) for (const hero of [false, true]) for (let mode = 0; mode < 3; mode++) {
    const line = inputs[lineId], gs = splitGraphemes(line.fullText), ranges = buildWordRangesFromWords(line, gs);
    const block = { id: 'fixture', line, variant: hero ? 'hero' : 'body', x: 13, y: 27,
      width: 320, height: 54, fontPx: 32.73, lineHeight: 44.125,
      graphemes: gs, wordRanges: ranges,
      wordRangeIndexByOffset: buildWordRangeIndexByOffset(gs.length, ranges),
      colorRangeIndexByOffset: buildWordRangeIndexByOffset(gs.length, ranges, 'color'),
      renderLines: [{ text: line.fullText, start: 0, end: gs.length, graphemes: gs, glyphOffsets: gs.map((_, i) => i * 11),
        left: 3, top: 0, width: gs.length * 11, segments: gs.map((text, i) => ({ text, start: i, end: i + 1,
          localStart: i, localEnd: i + 1, x: i * 11, measuredGlyphOffsets: [0, 8.5 + i % 3] })) }] };
    const theme = { primaryColor: '#dfe5ec', accentColor: '#f17b4d', fontFamily: 'sans-serif', fontStyle: 'sans', fontWeight: 700,
      animationIntensity: ['normal', 'calm', 'chaotic'][mode], wordColors: [{word:'amber echo',color:'#30caff'}, {word:'远方',color:'#29d475'}] };
    const glowIntensity = [1, .4, 1.7][mode], hold = [1, .3, 0][mode];
    const bases = resolveFumeGlowBases(theme.animationIntensity, glowIntensity);
    const times = [-.1, 0, .01, .05, .12, .4, .7, .8, 1, 1.5, 1.6, 1.8, 2, 2.8, 3.2, 4, 6, 20,
      line.startTime - 1e-9, line.startTime, line.endTime - 1e-9, line.endTime, line.endTime + 1e-9];
    for (const time of times) {
      const timing = resolveFumeBlockTiming(block, time, mode === 2 ? .8 : null, hold);
      const drawn = new Map(); let lineGlow = '-', pendingStamp = new Map(), current = { fillStyle: 'transparent', shadowColor:'transparent', shadowBlur:0, clipped:false };
      const stack = [];
      const context = new Proxy({ save(){stack.push({...current});}, restore(){current=stack.pop();}, beginPath(){}, clip(){current.clipped=true;}, rect(){},
        fillText(text,x,y) { const i = Math.round((x - block.x - 3)/11); if (!current.clipped) lineGlow=paint(current);
          else drawn.set(i,paint(current)); },
        fillRect(x,y,w,h){ const i = Math.round((x+w/2-block.x-3-(8.5+Math.round((x+w/2-block.x-3)/11)%3)/2)/11);
          pendingStamp.set(i, [x+w/2-block.x-3-i*11,y+h/2-block.y-timing.baselineOffset,w,h].join(',')+';'+paint(current)); stampCount++; }
      }, {get(t,k){ return k in t ? t[k] : current[k]; },set(t,k,v){current[k]=v;return true;}});
      drawFumeLiveBlock(context, block, timing, {time,theme,glowIntensity,...bases,showPrintStamp:true});
      const layers = resolveFumeStaticLayers(block,timing,{time,theme,passedGlowBase:bases.passedGlowBase,passedFadeDuration:2.4,
        overviewTextRestoreProgress:mode===2?.35:0,snapshotScale:1},s=>s);
      const staticText = layers ? layers.map(l=>paint({fillStyle:l.snapshot.fill,shadowBlur:l.snapshot.shadowBlur,shadowColor:l.snapshot.shadowColor})+';'+l.alpha).join('|') : '-';
      frames.push([id,lineId,hero,mode,time,[timing.waitingOpacity,timing.activeOpacity,+(timing.effectiveTextHoldStyle==='dimmed'),timing.baselineOffset,
        timing.lineEndTime,timing.linePassCutoffTime,+timing.hasRevealCompleted,+timing.hasPassCutoffReached,timing.lineDuration,timing.colorTrailDuration,timing.staticState??'-'].join(','),lineGlow,staticText].join('\t'));
      for(let i=0;i<gs.length;i++) glyphs.push([id,i,drawn.get(i)??'-',pendingStamp.get(i)??'-'].join('\t'));
      id++;
    }
  }
  fs.writeFileSync(path.join(folder,'frames.tsv'),frames.join('\n')+'\n');
  fs.writeFileSync(path.join(folder,'glyphs.tsv'),glyphs.join('\n')+'\n');
  console.log(JSON.stringify({revision,frames:id,glyphs:glyphs.length-2,stamps:stampCount}));
} finally { await server.close(); }
