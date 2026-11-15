import React from 'react';
import {createRoot} from 'react-dom/client';
import {motionValue} from 'framer-motion';
import {GlowWord} from '../../.work/folia-major/src/components/visualizer/GlowWord';
import fontUrl from '../assets/FoliaParityLatin-Bold.ttf?url';
import originalUrl from '../assets/NotoSans-Bold.ttf?url';

// Actual original body with identity variants; remove timing/glow as independent variables.
const params=new URLSearchParams(location.search);
const font=new FontFace('Raster Noto',`url(${params.has('original')?originalUrl:fontUrl})`,{weight:'700'});
await font.load();document.fonts.add(font);
const transformed=params.has('transform');
createRoot(document.getElementById('root')!).render(<div style={{position:'absolute',left:128,top:128,fontFamily:'Raster Noto'}}>
  <GlowWord word={{text:'Amber',startTime:1,endTime:2}} config={{id:'raster'}} currentTime={motionValue(0)}
    theme={{fontWeight:700} as any} baseColor="#fff" activeColor="#fff" fontSize="48px"
    renderProfile={{wordLookahead:0,wordRevealMode:'normal'} as any}
    layoutStyle={{}} layoutVariants={{waiting:{opacity:1,scale:transformed?1.6:1,rotate:transformed?-4:0}}}
    bodyVariants={{waiting:{opacity:1,color:'#fff',filter:'blur(0px)'}}}/>
</div>);
