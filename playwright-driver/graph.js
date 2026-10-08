const { chromium } = require('playwright');

const SCREENSHOT_DIR = process.argv[2] || '.';

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1400, height: 1000 } });

  await page.goto('http://localhost:5173', { waitUntil: 'networkidle' });
  await page.waitForTimeout(300);
  await page.locator('button:has-text("Get started")').click();
  await page.waitForTimeout(300);
  await page.locator('button:has-text("Select all")').first().click();
  await page.waitForTimeout(200);
  await page.locator('button:has-text("Next")').click();
  await page.waitForTimeout(300);
  await page.locator('button:has-text("Next")').click();
  await page.waitForTimeout(300);
  const consistencySwitch = page.locator('text=Guarantee consistency').first();
  if (await consistencySwitch.count()) {
    await consistencySwitch.click();
  }
  await page.waitForTimeout(200);
  await page.locator('button:has-text("Next")').click();
  await page.waitForTimeout(300);

  let bodyText = '';
  for (let attempt = 1; attempt <= 6; attempt++) {
    const btn = page.getByRole('button', { name: 'Generate', exact: true });
    if (await btn.count()) {
      await btn.click();
    } else {
      await page.locator('button:has-text("Regenerate")').click();
    }
    await page.waitForSelector('text=/COMPLETED|FAILED/i', { timeout: 90000 }).catch(() => {});
    for (let i = 0; i < 30; i++) {
      const t = await page.locator('body').innerText();
      if (!/\bRUNNING\b/.test(t)) break;
      await page.waitForTimeout(1000);
    }
    await page.waitForTimeout(500);
    bodyText = await page.locator('body').innerText();
    const bad = bodyText.includes('NOT_VERIFIED_ERROR') || /\bINCONSISTENT\b/i.test(bodyText);
    console.log(`Attempt ${attempt}: bad=${bad}`);
    if (!bad) break;
  }
  if (bodyText.includes('NOT_VERIFIED_ERROR') || /\bINCONSISTENT\b/i.test(bodyText)) {
    console.log('GOT_NON_CONSISTENT_SKIPPING_GRAPH');
    await browser.close();
    return;
  }

  await page.locator('button:has-text("View graph")').first().click();
  await page.waitForSelector('button:has-text("Hierarchical")', { timeout: 15000 });
  await page.waitForTimeout(1000);
  // Force a layout recompute: cytoscape often needs a resize/relayout nudge after mount.
  await page.locator('button:has-text("Hierarchical")').click();
  await page.waitForTimeout(1500);
  await page.locator('button:has-text("Force-directed")').click();
  await page.waitForTimeout(3000);
  await page.screenshot({ path: `${SCREENSHOT_DIR}/06-graph-view.png`, fullPage: true });
  console.log('GRAPH_SCREENSHOT_DONE');

  await browser.close();
})().catch((err) => {
  console.error('DRIVER_FAILED: ' + err.stack);
  process.exit(1);
});
