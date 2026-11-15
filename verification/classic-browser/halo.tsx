import React from 'react';
import {createRoot} from 'react-dom/client';
import {motionValue} from 'framer-motion';
import {GlowWord} from '../../.work/folia-major/src/components/visualizer/GlowWord';
import {setGlowBlurQuantized} from '../../.work/folia-major/src/utils/glowBlurQuantize';
import fontUrl from '../assets/FoliaParityLatin-Bold.ttf?url';

// Paint isolation: actual GlowWord/shadow definitions, identity pose, hidden body.
// capture-halo forces only glyph opacity, to measure the fixed raster independently of timing.
setGlowBlurQuantized(false);
const params=new URLSearchParams(location.search);
const ink=params.get('ink')??'#fff';
const reveal=params.get('reveal')??'normal';
if(!/^#[0-9a-f]{3}([0-9a-f]{3}([0-9a-f]{2})?)?$/i.test(ink))throw new Error('Invalid halo ink');
if(!['normal','fast','instant'].includes(reveal))throw new Error('Invalid halo reveal');
const font = new FontFace('Halo Noto',`url(${fontUrl})`,{weight:'700'});
await font.load(); document.fonts.add(font);
const currentTime=motionValue(0);
createRoot(document.getElementById('root')!).render(<div style={{position:'absolute',left:128,top:128,fontFamily:'Halo Noto'}}>
  <GlowWord word={{text:'M',startTime:1,endTime:2}} config={{id:'halo'}} currentTime={currentTime}
    theme={{fontWeight:700} as any} baseColor={ink} activeColor={ink} fontSize="64px"
    renderProfile={{wordLookahead:0,wordRevealMode:reveal} as any}
    layoutStyle={{}} layoutVariants={{waiting:{opacity:1}}} bodyVariants={{waiting:{opacity:0}}}/>
</div>);
(window as any).ready=true;
