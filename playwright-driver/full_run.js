const { chromium } = require('playwright');

const SCREENSHOT_DIR = process.argv[2] || '.';

async function shot(page, name) {
  await page.screenshot({ path: `${SCREENSHOT_DIR}/${name}.png`, fullPage: true });
}

(async () => {
  const browser = await chromium.launch();
  // deviceScaleFactor renders at 2x pixel density (crisp text) without touching CSS
  // layout at all -- CSS `zoom` was tried instead and corrupts fullPage screenshot
  // stitching in Chromium (duplicated/overlapping seams), so avoid it entirely.
  const page = await browser.newPage({ viewport: { width: 1400, height: 1000 }, deviceScaleFactor: 2 });
  const consoleErrors = [];
  page.on('console', (msg) => { if (msg.type() === 'error') consoleErrors.push(msg.text()); });
  page.on('pageerror', (err) => consoleErrors.push('pageerror: ' + err.message));

  await page.goto('http://localhost:5173', { waitUntil: 'networkidle' });
  await page.waitForTimeout(400);
  await shot(page, 'raw-00-landing');

  await page.locator('button:has-text("Get started")').click();
  await page.waitForTimeout(400);

  await page.locator('button:has-text("Select all")').first().click();
  await page.waitForTimeout(200);
  await page.locator('text=Class Axioms').first().click();
  await page.waitForTimeout(300);
  await shot(page, 'raw-01-construct-picker');

  await page.locator('button:has-text("Next")').click();
  await page.waitForTimeout(400);
  await shot(page, 'raw-02-structure');

  await page.locator('button:has-text("Next")').click();
  await page.waitForTimeout(400);
  const consistencySwitch = page.locator('text=Guarantee consistency').first();
  if (await consistencySwitch.count()) {
    await consistencySwitch.click();
  }
  await page.waitForTimeout(200);
  await shot(page, 'raw-03-reasoning');

  await page.locator('button:has-text("Next")').click();
  await page.waitForTimeout(400);
  await shot(page, 'raw-04-review');

  // Generate, retrying until a clean CONSISTENT result.
  let bodyText = '';
  for (let attempt = 1; attempt <= 6; attempt++) {
    const btn = page.getByRole('button', { name: 'Generate', exact: true });
    if (await btn.count()) {
      await btn.click();
    } else {
      await page.locator('button:has-text("Regenerate")').click();
    }
    await page.waitForFunction(
      () => /COMPLETED|FAILED/.test(document.body.innerText) && !/\bRUNNING\b/.test(document.body.innerText),
      null,
      { timeout: 90000 }
    ).catch(() => {});
    await page.waitForTimeout(800);
    bodyText = await page.locator('body').innerText();
    const bad = bodyText.includes('NOT_VERIFIED_ERROR') || /\bINCONSISTENT\b/i.test(bodyText);
    console.log(`Generate attempt ${attempt}: bad=${bad}`);
    if (!bad) break;
  }
  await page.waitForTimeout(400);
  await shot(page, 'raw-05-results');

  const viewGraphBtn = page.locator('button:has-text("View graph")').first();
  if (await viewGraphBtn.count()) {
    await viewGraphBtn.click().catch(() => {});
  }
  const graphOpen = await page.waitForFunction(
    () => document.body.innerText.includes('Hierarchical'),
    null,
    { timeout: 20000 }
  ).then(() => true).catch(() => false);
  if (!graphOpen) {
    console.log('CONSOLE_ERRORS=' + JSON.stringify(consoleErrors));
    await page.screenshot({ path: `${SCREENSHOT_DIR}/debug-graph-fail.png`, fullPage: true });
    throw new Error('Could not open graph panel after retries');
  }
  // The layout is not auto-run on mount; nudging the SegmentedControl (a <label>/radio
  // pair in Mantine, not a <button>) re-triggers cytoscape's layout algorithm.
  await page.waitForTimeout(500);
  await page.locator('label:has-text("Hierarchical")').click();
  await page.waitForTimeout(1500);
  await page.locator('label:has-text("Force-directed")').click();
  await page.waitForTimeout(3000);
  await page.waitForTimeout(400);
  await shot(page, 'raw-06-graph');

  console.log('ALL_SHOTS_DONE');
  await browser.close();
})().catch((err) => {
  console.error('DRIVER_FAILED: ' + err.stack);
  process.exit(1);
});
