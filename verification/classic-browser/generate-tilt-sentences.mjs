// Execute the pinned original sentence module; no rewritten oracle or modified checkout.
import fs from 'node:fs';
import path from 'node:path';
import { referenceModules, root, revision } from './reference.mjs';
const {server,load}=await referenceModules();
try {
 const {SentenceLayout}=await load('utils/lyrics/sentenceLayout.ts');
 const cases=['Amber echoes cross the quiet synthetic sky','One, two! Three? Four.',
   '  open (inside) close 「引用」 tail','“echo” \'note\' [bracket] end',
   '让我们 沿着星光 向远方','星光 Hello quiet sky 风声 and the rain 结束',
   '路径/a b/方向/c d','一～二：三/四|五','...','Stay','Á B́ Ć D́',
   'مرحبا بالعالم نحن هنا','first\tsecond\nthird','甲　乙 丙丁 戊己'];
 const out=[`# original ${revision}; UTF8 base64 input/output, ~ delimited outputs`,
   '# inputB64\ttarget\tseed\tsegmentsB64'];
 const b64=s=>Buffer.from(s).toString('base64');
 for(const text of cases)for(const target of [1,2,3,4,-1,-2,-3,-4,-5])for(const seed of [0,.7,12.345]) {
   const segments=SentenceLayout.splitIntoSentences(text,target,seed).map(u=>u.text);
   out.push([b64(text),target,seed,segments.map(b64).join('~')].join('\t'));
 }
 const folder=path.join(root,'verification/tilt-fixtures');fs.mkdirSync(folder,{recursive:true});
 fs.writeFileSync(path.join(folder,'sentences.tsv'),out.join('\n')+'\n');
 console.log(`Saved ${out.length-2} original sentence partitions`);
}finally{await server.close()}
