const { chromium } = require('playwright');

const SCREENSHOT_DIR = process.argv[2] || '.';

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1400, height: 1200 } });
  const consoleErrors = [];
  page.on('console', (msg) => {
    if (msg.type() === 'error') consoleErrors.push(msg.text());
  });
  page.on('pageerror', (err) => consoleErrors.push('pageerror: ' + err.message));

  await page.goto('http://localhost:5173', { waitUntil: 'networkidle' });
  await page.waitForSelector('text=Target profile', { timeout: 15000 });

  await page.screenshot({ path: `${SCREENSHOT_DIR}/01-before-el.png`, fullPage: true });

  // Switch to EL and confirm the picker reacts
  await page.locator('label:has-text("EL")').first().click();
  await page.waitForTimeout(400);

  // Expand "Object Property Restrictions" (contains both eligible ObjectSomeValuesFrom and
  // ineligible ObjectAllValuesFrom) and "Class Expressions & Enumerations" (has ObjectUnionOf, ineligible).
  await page.locator('text=Object Property Restrictions').first().click();
  await page.waitForTimeout(300);
  await page.locator('text=Class Expressions & Enumerations').first().click();
  await page.waitForTimeout(300);

  await page.screenshot({ path: `${SCREENSHOT_DIR}/02-el-picker-grayed.png`, fullPage: true });

  // Select an EL-eligible construct: Sub Class Of, Object Some Values From
  await page.locator('text=Class Axioms').first().click();
  await page.waitForTimeout(300);
  await page.locator('label:has-text("Sub Class Of")').first().click();
  await page.waitForTimeout(200);
  await page.locator('label:has-text("Object Some Values From")').first().click();
  await page.waitForTimeout(200);
  await page.locator('label:has-text("Class Declaration")').first().click();

  await page.screenshot({ path: `${SCREENSHOT_DIR}/03-el-selected.png`, fullPage: true });

  await page.locator('button:has-text("Next")').click();
  await page.waitForTimeout(400);
  await page.locator('button:has-text("Next")').click();
  await page.waitForTimeout(400);
  await page.locator('button:has-text("Next")').click();
  await page.waitForTimeout(400);

  await page.screenshot({ path: `${SCREENSHOT_DIR}/04-review-el.png`, fullPage: true });

  const generateButton = page.getByRole('button', { name: 'Generate', exact: true });
  await generateButton.click();

  await page.waitForSelector('text=/COMPLETED|FAILED/i', { timeout: 60000 }).catch(() => {});
  await page.waitForTimeout(1500);
  await page.screenshot({ path: `${SCREENSHOT_DIR}/05-results-el.png`, fullPage: true });

  const bodyText = await page.locator('body').innerText();

  console.log('CONSOLE_ERRORS_JSON=' + JSON.stringify(consoleErrors));
  console.log('BODY_SNIPPET_START');
  console.log(bodyText.slice(0, 3000));
  console.log('BODY_SNIPPET_END');

  await browser.close();
})().catch((err) => {
  console.error('DRIVER_FAILED: ' + err.stack);
  process.exit(1);
});
