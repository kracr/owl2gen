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

  await page.goto('http://localhost:5173', { waitUntil: 'networkidle' });
  await page.waitForSelector('text=Constructs', { timeout: 15000 });

  // Entity counts
  const classesInput = page.locator('input[aria-label="Class count"], input').first();
  // Fall back to a generic approach: find NumberInputs near "Classes" label text.
  const entityLabels = ['Classes', 'Object Properties', 'Data Properties', 'Individuals'];
  for (const label of entityLabels) {
    const group = page.locator(`text=${label}`).first();
    if (await group.count()) {
      const input = group.locator('xpath=following::input[1]');
      if (await input.count()) {
        await input.fill('20');
      }
    }
  }

  // Expand "Class Axioms" category and select all, then set Sub Class Of count explicitly
  await page.locator('text=Class Axioms').first().click();
  await page.waitForTimeout(300);
  const classAxiomsPanel = page.locator('text=Class Axioms').first().locator('xpath=ancestor::div[contains(@class,"mantine-Accordion-item")][1]');
  await classAxiomsPanel.locator('button:has-text("Select all")').click();

  // Expand "Object Property Restrictions" and select all (covers ObjectSomeValuesFrom)
  await page.locator('text=Object Property Restrictions').first().click();
  await page.waitForTimeout(300);
  const restrictionsPanel = page.locator('text=Object Property Restrictions').first().locator('xpath=ancestor::div[contains(@class,"mantine-Accordion-item")][1]');
  await restrictionsPanel.locator('button:has-text("Select all")').click();

  // Expand "Class Expressions & Enumerations" and check "Class Declaration" specifically (OwlClass)
  await page.locator('text=Class Expressions & Enumerations').first().click();
  await page.waitForTimeout(300);
  await page.locator('label:has-text("Class Declaration")').first().click();

  await page.waitForTimeout(300);
  await page.screenshot({ path: `${SCREENSHOT_DIR}/01-construct-picker.png`, fullPage: true });

  // Step through the wizard
  await page.locator('button:has-text("Next")').click();
  await page.waitForTimeout(500);
  await page.screenshot({ path: `${SCREENSHOT_DIR}/02-structure-step.png`, fullPage: true });

  await page.locator('button:has-text("Next")').click();
  await page.waitForTimeout(500);
  await page.screenshot({ path: `${SCREENSHOT_DIR}/03-reasoning-step.png`, fullPage: true });

  await page.locator('button:has-text("Next")').click();
  await page.waitForTimeout(500);
  await page.screenshot({ path: `${SCREENSHOT_DIR}/04-review-step.png`, fullPage: true });

  const generateButton = page.getByRole('button', { name: 'Generate', exact: true });
  await generateButton.click();

  // Poll for the results page to reach a terminal state
  await page.waitForSelector('text=/COMPLETED|FAILED/i', { timeout: 60000 }).catch(() => {});
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
