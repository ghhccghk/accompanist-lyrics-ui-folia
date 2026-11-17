import path from'node:path';import{createRequire}from'node:module';import{fileURLToPath}from'node:url';
import{createServer}from'vite';import{checkReference,root}from'./reference.mjs';
export async function createFumeMountedServer(port=4175){
checkReference();const require=createRequire(import.meta.url),here=path.dirname(fileURLToPath(import.meta.url));
const server=await createServer({configFile:false,root:here,resolve:{alias:[
 {find:/^react$/,replacement:require.resolve('react')},{find:/^react-dom\/client$/,replacement:require.resolve('react-dom/client')},
 {find:/^react\/jsx-dev-runtime$/,replacement:require.resolve('react/jsx-dev-runtime')},
 {find:/^react\/jsx-runtime$/,replacement:require.resolve('react/jsx-runtime')},
 {find:/^react-dom$/,replacement:require.resolve('react-dom')},{find:/^framer-motion$/,replacement:require.resolve('framer-motion')},
 {find:/^@chenglou\/pretext$/,replacement:require.resolve('@chenglou/pretext')},
 {find:'@chenglou/pretext/rich-inline',replacement:require.resolve('@chenglou/pretext/rich-inline')},
 {find:'lucide-react',replacement:path.join(here,'reference-icons.tsx')},
 {find:'../VisualizerShell',replacement:path.join(here,'surfaces.tsx')},{find:'../VisualizerSubtitleOverlay',replacement:path.join(here,'no-subtitle.tsx')},
 {find:'../../../stores/useVisualizerSettingsStore',replacement:path.join(here,'reference-settings.ts')}]},
 optimizeDeps:{noDiscovery:true,entries:[],include:['react','react-dom','react-dom/client','react/jsx-runtime','react/jsx-dev-runtime','framer-motion']},cacheDir:path.join(root,`.work/fume-mounted-vite-${port}`),server:{host:'127.0.0.1',port,strictPort:true,hmr:false,fs:{allow:[root]}}});
return server;
}
