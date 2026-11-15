import {chromium} from 'playwright';
const browser=await chromium.launch({headless:true});
try {
  const page=await browser.newPage({viewport:{width:800,height:600}});
  await page.goto('http://127.0.0.1:4173/?scene=whitespace');
  await page.waitForFunction(()=>window.ready===true);
  await page.evaluate(()=>window.step(1500));
  console.log(JSON.stringify(await page.locator('.inline-block').evaluateAll(els=>els.map(el=>{
    const style=getComputedStyle(el),body=el.querySelector('.z-10');
    const ctx=document.createElement('canvas').getContext('2d');
    ctx.font=`${style.fontWeight} ${style.fontSize} ${style.fontFamily}`;
    return {text:body.textContent,cssWidth:getComputedStyle(body).width,
      canvas:ctx.measureText(body.textContent.trim().replace(/ +/g,' ')).width,
      pieces:body.textContent.trim().split(/ +/).map(s=>({text:s,width:ctx.measureText(s).width})),space:ctx.measureText(' ').width};
  })),null,2));
} finally {await browser.close();}
