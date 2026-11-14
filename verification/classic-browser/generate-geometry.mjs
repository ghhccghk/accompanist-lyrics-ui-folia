import fs from 'node:fs';
import path from 'node:path';
import { referenceModules, revision, root } from './reference.mjs';
const {server,load} = await referenceModules();
try {
  const {getGlowWordReach, getGlowWordOverflow, getGlowWordPairGap} = await load('components/visualizer/glowWordOverlap.ts');
  const rows = [`# pinned Folia ${revision}; original reach/overflow/pairGap`,
    '# width\tnextWidth\tfont\tx\ty\tscale\trotation\tpassedRotation\tnextX\tnextY\tnextScale\tnextRotation\tnextPassed\trtl\thalfWidth\thalfHeight\tleft\tright\ttop\tbottom\tgap'];
  for(let i=0;i<160;i++) {
    const width=10+i*3.75,nextWidth=300-i*1.25,font=24+(i%9)*6;
    const pose={scale:1.1+(i%7)*.025,rotate:(i%11-5)*3,passedRotate:(i%19-9)*4};
    const next={scale:1.2+(i%3)*.05,rotate:(i%7-3)*5,passedRotate:(i%13-6)*5};
    const offset={x:i%31-15,y:i%29-14}, nextOffset={x:i%23-11,y:0};
    const reach=getGlowWordReach(width,font,pose),overflow=getGlowWordOverflow(width,font,pose,offset);
    const rtl=i%2===1;
    rows.push([width,nextWidth,font,offset.x,offset.y,pose.scale,pose.rotate,pose.passedRotate,nextOffset.x,nextOffset.y,
      next.scale,next.rotate,next.passedRotate,rtl,...Object.values(reach),...Object.values(overflow),
      getGlowWordPairGap({width,pose,offset},{width:nextWidth,pose:next,offset:nextOffset},font,rtl?'rtl':'ltr')].join('\t'));
  }
  fs.writeFileSync(path.join(root,'verification/classic-fixtures/geometry.tsv'),rows.join('\n')+'\n');
} finally {await server.close();}
