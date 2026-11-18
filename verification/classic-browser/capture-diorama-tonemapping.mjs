import fs from'node:fs';import path from'node:path';import{createRequire}from'node:module';
import{createHash}from'node:crypto';import{createServer}from'vite';import{chromium}from'playwright';
import{root,upstream,revision,checkReference,referenceMotionVersions}from'./reference.mjs';
checkReference();const require=createRequire(import.meta.url),folder=path.join(root,'verification/diorama-fixtures/tonemapping-v1');
if(fs.existsSync(folder))throw Error('Preserve original shader oracle; refusing overwrite');
const threeRoot=path.resolve(path.dirname(fs.realpathSync(require.resolve('three'))),'..');
const metadata=JSON.parse(fs.readFileSync(path.join(threeRoot,'package.json'),'utf8'));
const originalLock=JSON.parse(fs.readFileSync(path.join(upstream,'package-lock.json'),'utf8'));
if(metadata.version!==originalLock.packages['node_modules/three'].version)throw Error('Three differs from pinned original lock');
const server=await createServer({configFile:false,root:path.join(root,'verification/classic-browser'),
 resolve:{alias:{three:path.join(threeRoot,'build/three.module.js')}},optimizeDeps:{noDiscovery:true,entries:[]},
 cacheDir:path.join(root,'.work/diorama-tonemapping-vite'),
 server:{host:'127.0.0.1',port:4189,strictPort:true,hmr:false,fs:{allow:[root]}}});
let browser;
try {
 await server.listen();browser=await chromium.launch({headless:true,args:['--use-angle=swiftshader','--enable-unsafe-swiftshader']});
 const page=await browser.newPage({viewport:{width:64,height:64},deviceScaleFactor:1}),errors=[];
 page.on('pageerror',e=>errors.push(e.message));
 await page.goto('http://127.0.0.1:4189/diorama-tonemapping.html');
 await page.waitForFunction(()=>window.toneMappingOracle,null,{timeout:20000});
 const data=await page.evaluate(()=>window.toneMappingOracle);
 if(errors.length)throw Error(errors.join('\n'));
 if(data.rows.length!==960)throw Error('Incomplete shader matrix');
 const hash=p=>createHash('sha256').update(fs.readFileSync(p)).digest('hex');
 const shaders=['ShaderLib/meshbasic.glsl.js','ShaderChunk/tonemapping_pars_fragment.glsl.js',
  'ShaderChunk/tonemapping_fragment.glsl.js','ShaderChunk/colorspace_pars_fragment.glsl.js','ShaderChunk/fog_fragment.glsl.js','UniformsUtils.js'];
 fs.mkdirSync(folder,{recursive:true});
 fs.writeFileSync(path.join(folder,'conditions.json'),JSON.stringify({revision,three:metadata.version,motion:referenceMotionVersions(),browser:browser.version(),DPR:1,
  outputColorSpace:data.outputColorSpace,toneMappingExposure:data.toneMappingExposure,fog:data.fog,context:data.context,
  originalLockSha256:hash(path.join(upstream,'package-lock.json')),lockSha256:hash(path.join(root,'verification/classic-browser/pnpm-lock.yaml')),
  hostFiles:['diorama-tonemapping.ts','diorama-tonemapping.html','capture-diorama-tonemapping.mjs'].map(file=>({file,sha256:hash(path.join(root,'verification/classic-browser',file))})),
  shaderFiles:shaders.map(file=>({file,sha256:hash(path.join(threeRoot,'src/renderers/shaders',file))})),
  cases:data.rows.length,scope:'Actual original-lock Three MeshBasicMaterial default-framebuffer readPixels, no JS ACES expected formula.16 linear RGB including source palette/HDR/black, alpha1/.63/.2, normal/additive, fog6/12/21/30/36, None/ACES. Solid center pixel; no text/camera/antialias oracle. Fixed initial gates: opaque color <=1 byte; full Compose blend/fog <=2 bytes. Source unchanged.'},null,2)+'\n');
 fs.writeFileSync(path.join(folder,'colors.tsv'),['# mapping linearRGB3 alpha additive depth framebufferRGBA4',
  ...data.rows.map(r=>[r.mapping,...r.color,r.alpha,r.additive?1:0,r.depth,...r.rgba].join('\t'))].join('\n')+'\n');
 console.log(JSON.stringify({cases:data.rows.length,browser:browser.version(),three:metadata.version}));
}finally{await browser?.close();await server.close()}
