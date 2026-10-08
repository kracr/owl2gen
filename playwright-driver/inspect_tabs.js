const { chromium } = require('playwright');
const SCREENSHOT_DIR = process.argv[2] || '.';

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1400, height: 1000 } });
  await page.goto('http://localhost:5173', { waitUntil: 'networkidle' });
  await page.locator('button:has-text("Get started")').click();
  await page.waitForTimeout(300);
  await page.locator('button:has-text("Select all")').first().click();
  await page.waitForTimeout(200);
  await page.locator('button:has-text("Next")').click();
  await page.waitForTimeout(300);
  await page.locator('button:has-text("Next")').click();
  await page.waitForTimeout(300);
  const sw = page.locator('text=Guarantee consistency').first();
  if (await sw.count()) await sw.click();
  await page.waitForTimeout(200);
  await page.locator('button:has-text("Next")').click();
  await page.waitForTimeout(300);

  for (let attempt = 1; attempt <= 6; attempt++) {
    const btn = page.getByRole('button', { name: 'Generate', exact: true });
    if (await btn.count()) { await btn.click(); } else { await page.locator('button:has-text("Regenerate")').click(); }
    await page.waitForFunction(
      () => /COMPLETED|FAILED/.test(document.body.innerText) && !/\bRUNNING\b/.test(document.body.innerText),
      null, { timeout: 90000 }
    ).catch(() => {});
    await page.waitForTimeout(800);
    const bodyText = await page.locator('body').innerText();
    const bad = bodyText.includes('NOT_VERIFIED_ERROR') || /\bINCONSISTENT\b/i.test(bodyText);
    if (!bad) break;
  }

  await page.locator('button:has-text("View graph")').first().click();
  await page.waitForFunction(() => document.body.innerText.includes('Hierarchical'), null, { timeout: 20000 });
  await page.waitForTimeout(1000);

  const html = await page.evaluate(() => {
    const all = Array.from(document.querySelectorAll('*'));
    const el = all.find(e => e.textContent.trim() === 'Hierarchical' && e.children.length === 0);
    if (!el) return 'NOT_FOUND';
    let cur = el;
    let path = [];
    for (let i = 0; i < 4 && cur; i++) {
      path.push(cur.outerHTML.slice(0, 300));
      cur = cur.parentElement;
    }
    return path.join('\n---PARENT---\n');
  });
  console.log(html);

  await browser.close();
})().catch((err) => {
  console.error('DRIVER_FAILED: ' + err.stack);
  process.exit(1);
});
