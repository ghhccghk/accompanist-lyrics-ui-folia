import React from 'react';
import {createRoot} from 'react-dom/client';
import {flushSync} from 'react-dom';
import {motionValue} from 'framer-motion';
import {GlowWord} from '../../.work/folia-major/src/components/visualizer/GlowWord';
import fontUrl from '../assets/FoliaParityLatin-Bold.ttf?url';
import originalUrl from '../assets/NotoSans-Bold.ttf?url';
import arabicUrl from '../assets/NotoSansArabic-Bold.ttf?url';
import cjkUrl from '../assets/FoliaParityCJK-Bold.otf?url';

// Actual original body with identity variants; remove timing/glow as independent variables.
const params=new URLSearchParams(location.search);
const ink=params.get('ink')??'#fff';
if(!/^#[0-9a-f]{3}([0-9a-f]{3})?$/i.test(ink))throw new Error('Invalid raster ink');
const font=new FontFace('Raster Noto',`url(${params.has('original')?originalUrl:params.get('font')==='arabic'?arabicUrl:params.get('font')==='cjk'?cjkUrl:fontUrl})`,{weight:'700'});
await font.load();document.fonts.add(font);
const transformed=params.has('transform');
const phase=Number(params.get('phase')??0);
const motionPhase=Number(params.get('motionphase')??0);
const historicalScale=params.has('history')?Number(params.get('history')):null;
if(historicalScale!==null&&(!Number.isFinite(historicalScale)||historicalScale<=0||historicalScale>2))throw new Error('Invalid raster history');
const clock=motionValue(0);
(window as any).rasterStep=()=>flushSync(()=>clock.set(1));
const target={opacity:1,x:motionPhase,y:motionPhase,scale:transformed?1.6:1,rotate:transformed?-4:0};
if(![phase,motionPhase].every(p=>Number.isFinite(p)&&Math.abs(p)<=1))throw new Error('Invalid raster phase');
createRoot(document.getElementById('root')!).render(<div style={{position:'absolute',left:128+phase,top:128+phase,fontFamily:'Raster Noto'}}>
  <GlowWord word={{text:params.get('text')??'Amber',startTime:1,endTime:2}} config={{id:'raster'}} currentTime={clock}
    theme={{fontWeight:700} as any} baseColor={ink} activeColor={ink} fontSize={`${params.get('size')??48}px`}
    renderProfile={{wordLookahead:0,wordRevealMode:'normal'} as any}
    layoutStyle={{}} layoutVariants={{waiting:historicalScale===null?target:{...target,scale:historicalScale,rotate:20},active:{...target,transition:{duration:.3}}}}
    bodyVariants={{waiting:{opacity:1,color:ink,filter:'blur(0px)'},active:{opacity:1,color:ink,filter:'blur(0px)'}}}/>
</div>);
