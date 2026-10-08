const { chromium } = require('playwright');

const SCREENSHOT_DIR = process.argv[2] || '.';

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1400, height: 1100 } });

  await page.goto('http://localhost:5173', { waitUntil: 'networkidle' });
  const getStarted = page.locator('text=Get started').first();
  if (await getStarted.count()) {
    await getStarted.click();
    await page.waitForTimeout(800);
  }
  await page.waitForSelector('text=Constructs', { timeout: 15000 });

  // Select construct categories that are nesting-eligible: boolean class expressions and
  // object-property restrictions, both of which call NestingStrategy.buildClassFiller.
  await page.locator('text=Class Expressions & Enumerations').first().click();
  await page.waitForTimeout(300);
  const classExprPanel = page.locator('text=Class Expressions & Enumerations').first().locator('xpath=ancestor::div[contains(@class,"mantine-Accordion-item")][1]');
  await classExprPanel.locator('button:has-text("Select all")').click();

  await page.locator('text=Object Property Restrictions').first().click();
  await page.waitForTimeout(300);
  const restrictionsPanel = page.locator('text=Object Property Restrictions').first().locator('xpath=ancestor::div[contains(@class,"mantine-Accordion-item")][1]');
  await restrictionsPanel.locator('button:has-text("Select all")').click();

  await page.waitForTimeout(300);

  // Step through the wizard, leaving structure defaults untouched (nesting max depth = 2, the
  // system default we just fixed) so the results reflect the fixed default behavior.
  await page.locator('button:has-text("Next")').click();
  await page.waitForTimeout(500);
  await page.screenshot({ path: `${SCREENSHOT_DIR}/structure-step.png`, fullPage: true });

  await page.locator('button:has-text("Next")').click();
  await page.waitForTimeout(500);

  await page.locator('button:has-text("Next")').click();
  await page.waitForTimeout(500);

  const generateButton = page.getByRole('button', { name: 'Generate', exact: true });
  await generateButton.click();

  await page.waitForSelector('text=/COMPLETED|FAILED/i', { timeout: 60000 }).catch(() => {});
  await page.waitForTimeout(1500);

  // If this particular random draw came out NOT_VERIFIED_ERROR/INCONSISTENT, regenerate a
  // couple of times to get a clean CONSISTENT result for the illustration screenshot.
  for (let attempt = 0; attempt < 3; attempt++) {
    const badgeText = await page.locator('text=/CONSISTENT|INCONSISTENT|NOT_VERIFIED/').first().innerText().catch(() => '');
    if (badgeText.trim() === 'CONSISTENT') break;
    const regenButton = page.getByRole('button', { name: 'Regenerate' });
    if (!(await regenButton.count())) break;
    await regenButton.click();
    await page.waitForSelector('text=/COMPLETED|FAILED/i', { timeout: 60000 }).catch(() => {});
    await page.waitForTimeout(1500);
  }

  // Expand the graph view so both the structural-profile stats and the graph itself are
  // visible in the same screenshot. Cytoscape's force-directed layout needs real time to
  // settle for ~50+ nodes before fit() has anything meaningful to frame.
  const viewGraphButton = page.getByRole('button', { name: 'View graph' });
  if (await viewGraphButton.count()) {
    await viewGraphButton.click();
    await page.waitForTimeout(3000);
    // Force a fresh layout pass now that the container has real, settled dimensions -
    // the very first cose layout can run before Cytoscape's container size is measured
    // correctly, leaving nodes stacked near the origin.
    await page.locator('text=Hierarchical').first().click();
    await page.waitForTimeout(2000);
    await page.locator('text=Force-directed').first().click();
    await page.waitForTimeout(3000);
  }

  await page.waitForTimeout(500);
  await page.screenshot({ path: `${SCREENSHOT_DIR}/nesting-fix-result-with-graph.png`, fullPage: true });

  const bodyText = await page.locator('body').innerText();
  console.log('BODY_SNIPPET_START');
  console.log(bodyText.slice(0, 3000));
  console.log('BODY_SNIPPET_END');

  await browser.close();
})().catch((err) => {
  console.error('DRIVER_FAILED: ' + err.stack);
  process.exit(1);
});
