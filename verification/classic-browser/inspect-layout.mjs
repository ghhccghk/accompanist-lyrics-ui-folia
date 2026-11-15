import {chromium} from 'playwright';
const browser=await chromium.launch({headless:true});
try {
  const page=await browser.newPage({viewport:{width:380,height:600}});
  await page.addInitScript(()=>{
    window.widthTrace=[];const original=CanvasRenderingContext2D.prototype.measureText;
    CanvasRenderingContext2D.prototype.measureText=function(text){const result=original.call(this,text);window.widthTrace.push({font:this.font,text,width:result.width});return result;};
  });
  await page.goto('http://127.0.0.1:4173/?scene=wrap&avoid');await page.waitForFunction(()=>window.ready===true);
  await page.evaluate(()=>window.step(750));
  console.log(await page.evaluate(()=>({fonts:[...new Set(window.widthTrace.map(t=>t.font))],
    samples:window.widthTrace.filter(t=>t.text==='Synthetic '),
    font:getComputedStyle(document.querySelector('.inline-block')).fontSize,
    height:document.querySelector('[data-overlap-fit-line]').offsetHeight,
    probes:[19.73,19.74,19.744,19.7479,19.75].map(size=>{const c=document.createElement('canvas').getContext('2d');c.font=`700 ${size}px "Parity Noto"`;return [size,c.measureText('Synthetic ').width];})})));
}finally{await browser.close();}
