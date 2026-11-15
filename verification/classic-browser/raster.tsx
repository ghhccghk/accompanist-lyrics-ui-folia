import React from 'react';
import {createRoot} from 'react-dom/client';
import {motionValue} from 'framer-motion';
import {GlowWord} from '../../.work/folia-major/src/components/visualizer/GlowWord';
import fontUrl from '../assets/FoliaParityLatin-Bold.ttf?url';
import originalUrl from '../assets/NotoSans-Bold.ttf?url';
import arabicUrl from '../assets/NotoSansArabic-Bold.ttf?url';
import cjkUrl from '../assets/FoliaParityCJK-Bold.otf?url';

// Actual original body with identity variants; remove timing/glow as independent variables.
const params=new URLSearchParams(location.search);
const font=new FontFace('Raster Noto',`url(${params.has('original')?originalUrl:params.get('font')==='arabic'?arabicUrl:params.get('font')==='cjk'?cjkUrl:fontUrl})`,{weight:'700'});
await font.load();document.fonts.add(font);
const transformed=params.has('transform');
const phase=Number(params.get('phase')??0);
const motionPhase=Number(params.get('motionphase')??0);
if(![phase,motionPhase].every(p=>Number.isFinite(p)&&Math.abs(p)<=1))throw new Error('Invalid raster phase');
createRoot(document.getElementById('root')!).render(<div style={{position:'absolute',left:128+phase,top:128+phase,fontFamily:'Raster Noto'}}>
  <GlowWord word={{text:params.get('text')??'Amber',startTime:1,endTime:2}} config={{id:'raster'}} currentTime={motionValue(0)}
    theme={{fontWeight:700} as any} baseColor="#fff" activeColor="#fff" fontSize={`${params.get('size')??48}px`}
    renderProfile={{wordLookahead:0,wordRevealMode:'normal'} as any}
    layoutStyle={{}} layoutVariants={{waiting:{opacity:1,x:motionPhase,y:motionPhase,scale:transformed?1.6:1,rotate:transformed?-4:0}}}
    bodyVariants={{waiting:{opacity:1,color:'#fff',filter:'blur(0px)'}}}/>
</div>);
