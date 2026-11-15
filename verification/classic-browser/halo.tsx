import React from 'react';
import {createRoot} from 'react-dom/client';
import {motionValue} from 'framer-motion';
import {GlowWord} from '../../.work/folia-major/src/components/visualizer/GlowWord';
import {setGlowBlurQuantized} from '../../.work/folia-major/src/utils/glowBlurQuantize';
import fontUrl from '../assets/NotoSans-Bold.ttf?url';

// Paint isolation: actual GlowWord/shadow definitions, identity pose, hidden body.
// capture-halo forces only glyph opacity, to measure the fixed raster independently of timing.
setGlowBlurQuantized(false);
const font = new FontFace('Halo Noto',`url(${fontUrl})`,{weight:'700'});
await font.load(); document.fonts.add(font);
const currentTime=motionValue(0);
createRoot(document.getElementById('root')!).render(<div style={{position:'absolute',left:128,top:128,fontFamily:'Halo Noto'}}>
  <GlowWord word={{text:'M',startTime:1,endTime:2}} config={{id:'halo'}} currentTime={currentTime}
    theme={{fontWeight:700} as any} baseColor="#fff" activeColor="#fff" fontSize="64px"
    renderProfile={{wordLookahead:0,wordRevealMode:'normal'} as any}
    layoutStyle={{}} layoutVariants={{waiting:{opacity:1}}} bodyVariants={{waiting:{opacity:0}}}/>
</div>);
(window as any).ready=true;
