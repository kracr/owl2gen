const { chromium } = require('playwright');

const SCREENSHOT_DIR = process.argv[2] || '.';
const MAX_TRIES = 5;

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1400, height: 1000 } });

  // Re-run the whole wizard from scratch (fresh page state avoids stale job state).
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

  let status = '';
  for (let attempt = 1; attempt <= MAX_TRIES; attempt++) {
    const generateButton = page.getByRole('button', { name: 'Generate', exact: true });
    if (await generateButton.count()) {
      await generateButton.click();
    } else {
      await page.locator('button:has-text("Regenerate")').click();
    }
    await page.waitForSelector('text=/COMPLETED|FAILED/i', { timeout: 90000 }).catch(() => {});
    // Poll until the RUNNING badge actually clears (async state can lag the selector match).
    for (let i = 0; i < 30; i++) {
      const t = await page.locator('body').innerText();
      if (!/\bRUNNING\b/.test(t)) break;
      await page.waitForTimeout(1000);
    }
    await page.waitForTimeout(500);
    const bodyText = await page.locator('body').innerText();
    const hasNotVerified = bodyText.includes('NOT_VERIFIED_ERROR');
    const hasInconsistent = /\bINCONSISTENT\b/i.test(bodyText);
    console.log(`Attempt ${attempt}: notVerified=${hasNotVerified} inconsistent=${hasInconsistent}`);
    if (!hasNotVerified && !hasInconsistent) {
      status = 'CONSISTENT';
      break;
    }
    status = 'RETRY';
  }

  await page.screenshot({ path: `${SCREENSHOT_DIR}/05-results.png`, fullPage: true });
  const finalText = await page.locator('body').innerText();
  console.log('FINAL_STATUS=' + status);
  console.log('BODY_SNIPPET_START');
  console.log(finalText.slice(0, 3000));
  console.log('BODY_SNIPPET_END');

  await browser.close();
})().catch((err) => {
  console.error('DRIVER_FAILED: ' + err.stack);
  process.exit(1);
});
