// All fixed-font original subtitle overflow scenarios. Does not modify upstream.
import {execFileSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
const script=fileURLToPath(new URL('./capture-subtitles.mjs',import.meta.url));
for(const flags of [[],['--repeat2'],['--next'],['--repeat2','--next'],
  ['--next','--backdrop'],['--repeat2','--next','--backdrop']]) {
  execFileSync(process.execPath,[script,'--cjk',...flags],{stdio:'inherit'});
}
