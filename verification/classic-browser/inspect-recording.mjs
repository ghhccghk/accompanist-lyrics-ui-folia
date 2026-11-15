// Decode the actual Android screenrecord with the existing verification browser.
import {chromium} from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import {pathToFileURL} from 'node:url';
import {root} from './reference.mjs';
const api=process.argv[2];
if(!['30','34'].includes(api))throw new Error('Pass verified API 30 or 34');
const output=path.join(root,`build/verification/classic-android-api${api}-recording`);
const html=path.join(output,'inspect.html');
fs.writeFileSync(html,'<!doctype html><style>body{margin:0;background:#162435}video{display:block;width:800px;height:600px;object-fit:contain;object-position:top left}</style><video preload="auto" src="classic-play-pause-seek.mp4"></video>');
const browser=await chromium.launch({headless:true});
try {
 const page=await browser.newPage({viewport:{width:800,height:600}});
 await page.goto(pathToFileURL(html).href);
 await page.waitForFunction(()=>document.querySelector('video').readyState>=2,null,{timeout:15000});
 const metadata=await page.locator('video').evaluate(v=>({duration:v.duration,width:v.videoWidth,height:v.videoHeight}));
 await page.locator('video').evaluate(v=>{v.style.width=`${v.videoWidth}px`;v.style.height=`${v.videoHeight}px`;});
 const samples=[];
 for(const seconds of [.5,1.2,2,2.8,3.6,4.5,5.8,6.5,7.5,Math.max(0,metadata.duration-.1)]) {
  if(seconds>=metadata.duration)continue;
  await page.locator('video').evaluate(async(v,t)=>{
   await new Promise((resolve,reject)=>{
    const timeout=setTimeout(()=>reject(new Error('No decoded video frame after seek')),3000);
    v.requestVideoFrameCallback(()=>{clearTimeout(timeout);resolve();});v.currentTime=t;
   });
  },seconds);
  const file=`video-${seconds.toFixed(1)}s.png`;
  await page.screenshot({path:path.join(output,file)});samples.push({videoSeconds:seconds,file});
 }
 fs.writeFileSync(path.join(output,'video-metadata.json'),JSON.stringify({...metadata,browser:browser.version(),
  note:'Encoded video timestamps are not exact media-time golden checkpoints',samples},null,2));
 console.log(output,metadata);
}finally {await browser.close();}
