const { chromium } = require('playwright');

const SCREENSHOT_DIR = process.argv[2] || '.';

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1400, height: 1000 } });
  const consoleErrors = [];
  page.on('console', (msg) => {
    if (msg.type() === 'error') consoleErrors.push(msg.text());
  });
  page.on('pageerror', (err) => consoleErrors.push('pageerror: ' + err.message));

  // 0. Landing / about page
  await page.goto('http://localhost:5173', { waitUntil: 'networkidle' });
  await page.waitForTimeout(500);
  await page.screenshot({ path: `${SCREENSHOT_DIR}/00-landing.png`, fullPage: true });

  await page.locator('button:has-text("Get started")').click();
  await page.waitForTimeout(500);

  // 1. Entities & Constructs
  await page.locator('input').first().fill('15');
  const inputs = page.locator('input[inputmode="numeric"], input[type="text"]');
  const labels = ['Classes', 'Object properties', 'Data properties', 'Individuals'];
  for (const label of labels) {
    const group = page.locator(`text=${label}`).first();
    if (await group.count()) {
      const input = group.locator('xpath=following::input[1]');
      if (await input.count()) {
        await input.fill('15');
      }
    }
  }

  await page.locator('button:has-text("Select all")').first().click();
  await page.waitForTimeout(300);
  // Expand one category to show what selection looks like
  await page.locator('text=Class Axioms').first().click();
  await page.waitForTimeout(300);
  await page.screenshot({ path: `${SCREENSHOT_DIR}/01-construct-picker.png`, fullPage: true });

  await page.locator('button:has-text("Next")').click();
  await page.waitForTimeout(500);
  await page.screenshot({ path: `${SCREENSHOT_DIR}/02-structure-step.png`, fullPage: true });

  await page.locator('button:has-text("Next")').click();
  await page.waitForTimeout(500);
  // Turn on "Guarantee consistency" if present (select-all is dense, needs it)
  const consistencySwitch = page.locator('text=Guarantee consistency').first();
  if (await consistencySwitch.count()) {
    await consistencySwitch.click();
    await page.waitForTimeout(200);
  }
  await page.screenshot({ path: `${SCREENSHOT_DIR}/03-reasoning-step.png`, fullPage: true });

  await page.locator('button:has-text("Next")').click();
  await page.waitForTimeout(500);
  await page.screenshot({ path: `${SCREENSHOT_DIR}/04-review-step.png`, fullPage: true });

  const generateButton = page.getByRole('button', { name: 'Generate', exact: true });
  await generateButton.click();

  await page.waitForSelector('text=/COMPLETED|FAILED/i', { timeout: 90000 }).catch(() => {});
  await page.waitForTimeout(1500);
  await page.screenshot({ path: `${SCREENSHOT_DIR}/05-results.png`, fullPage: true });

  const bodyText = await page.locator('body').innerText();

  console.log('CONSOLE_ERRORS_JSON=' + JSON.stringify(consoleErrors));
  console.log('BODY_SNIPPET_START');
  console.log(bodyText.slice(0, 4000));
  console.log('BODY_SNIPPET_END');

  await browser.close();
})().catch((err) => {
  console.error('DRIVER_FAILED: ' + err.stack);
  process.exit(1);
});
