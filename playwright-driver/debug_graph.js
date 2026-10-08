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
    await page.waitForSelector('text=/COMPLETED|FAILED/i', { timeout: 90000 }).catch(() => {});
    for (let i = 0; i < 30; i++) {
      const t = await page.locator('body').innerText();
      if (!/\bRUNNING\b/.test(t)) break;
      await page.waitForTimeout(1000);
    }
    await page.waitForTimeout(500);
    const bodyText = await page.locator('body').innerText();
    const bad = bodyText.includes('NOT_VERIFIED_ERROR') || /\bINCONSISTENT\b/i.test(bodyText);
    console.log(`Attempt ${attempt}: bad=${bad}`);
    if (!bad) break;
  }

  await page.evaluate(() => { document.body.style.zoom = '1.35'; });
  await page.waitForTimeout(500);
  console.log('--- before clicking View graph (zoomed) ---');
  console.log(await page.locator('body').innerText());
  await page.screenshot({ path: `${SCREENSHOT_DIR}/debug-before-viewgraph-zoomed.png`, fullPage: true });
  const vgBtn = page.locator('button:has-text("View graph")').first();
  console.log('viewGraphBtn count=', await vgBtn.count(), 'visible=', await vgBtn.isVisible().catch(e => 'ERR:' + e.message));
  await vgBtn.click();
  await page.waitForTimeout(1500);
  console.log('--- after clicking View graph ---');
  console.log(await page.locator('body').innerText());
  await page.screenshot({ path: `${SCREENSHOT_DIR}/debug-after-viewgraph.png`, fullPage: true });

  await browser.close();
})().catch((err) => {
  console.error('DRIVER_FAILED: ' + err.stack);
  process.exit(1);
});
