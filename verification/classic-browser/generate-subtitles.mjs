import fs from 'node:fs';
import path from 'node:path';
import {chromium} from 'playwright';
import {root,checkReference} from './reference.mjs';
checkReference();
const browser=await chromium.launch({headless:true});
try {
 const page=await browser.newPage();
 await page.goto('http://127.0.0.1:4173/subtitle.html');
 await page.waitForFunction(()=>typeof window.contentFixtures==='string');
 fs.writeFileSync(path.join(root,'verification/classic-fixtures/subtitle-content.tsv'),await page.evaluate(()=>window.contentFixtures));
} finally {await browser.close();}
