const { chromium } = require('playwright');

const SCREENSHOT_DIR = process.argv[2] || '.';

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1400, height: 1200 } });

  await page.goto('http://localhost:5173', { waitUntil: 'networkidle' });
  const getStarted = page.locator('text=Get started').first();
  if (await getStarted.count()) {
    await getStarted.click();
    await page.waitForTimeout(800);
  }
  await page.waitForSelector('text=Constructs', { timeout: 15000 });

  // A denser, deliberately illustrative entity pool - enough classes for a real hierarchy to
  // form and enough object/data properties and individuals for a rich, well-connected graph.
  await page.getByRole('textbox', { name: 'Classes' }).fill('35');
  await page.getByRole('textbox', { name: 'Object properties' }).fill('8');
  await page.getByRole('textbox', { name: 'Data properties' }).fill('4');
  await page.getByRole('textbox', { name: 'Individuals' }).fill('15');

  async function selectAllInCategory(name) {
    await page.locator(`text=${name}`).first().click();
    await page.waitForTimeout(300);
    const panel = page.locator(`text=${name}`).first().locator('xpath=ancestor::div[contains(@class,"mantine-Accordion-item")][1]');
    await panel.locator('button:has-text("Select all")').click();
    await page.waitForTimeout(200);
  }

  await selectAllInCategory('Class Axioms');
  await selectAllInCategory('Class Expressions & Enumerations');
  await selectAllInCategory('Object Property Restrictions');
  await selectAllInCategory('Object Property Axioms');
  await selectAllInCategory('Assertions & Keys');

  // Give Sub Class Of enough of a budget that a real hierarchy (not depth 0) actually forms -
  // chooseSuperclass is only invoked once per requested SubClassOf axiom.
  const subClassOfCount = page.getByRole('textbox', { name: 'Count for Sub Class Of' });
  if (await subClassOfCount.count()) {
    await subClassOfCount.fill('35');
  }

  await page.waitForTimeout(300);

  // Step 2: Structure - reachable hierarchy target given the widened SubClassOf budget above.
  await page.locator('button:has-text("Next")').click();
  await page.waitForTimeout(500);

  // Step 3: Reasoning & Output - turn on Guarantee consistency for a clean, real CONSISTENT result.
  await page.locator('button:has-text("Next")').click();
  await page.waitForTimeout(500);
  const guaranteeSwitch = page.locator('text=Guarantee consistency').first();
  if (await guaranteeSwitch.count()) {
    await guaranteeSwitch.click();
  }

  // Step 4: Review & Generate
  await page.locator('button:has-text("Next")').click();
  await page.waitForTimeout(500);

  const generateButton = page.getByRole('button', { name: 'Generate', exact: true });
  await generateButton.click();

  await page.waitForSelector('text=/COMPLETED|FAILED/i', { timeout: 90000 }).catch(() => {});
  await page.waitForTimeout(1500);

  // Retry until we get a clean CONSISTENT result with actual hierarchy depth, not a flat one.
  for (let attempt = 0; attempt < 6; attempt++) {
    const bodyText = await page.locator('body').innerText();
    const isConsistent = /\bCONSISTENT\b/.test(bodyText) && !/NOT_VERIFIED|INCONSISTENT/.test(bodyText);
    const depthMatch = bodyText.match(/Hierarchy depth\s*\n\s*(\d+)\s*\/\s*(\d+) target/);
    const depthOk = depthMatch && parseInt(depthMatch[1], 10) >= 2;
    if (isConsistent && depthOk) break;
    const regenButton = page.getByRole('button', { name: 'Regenerate' });
    if (!(await regenButton.count())) break;
    await regenButton.click();
    await page.waitForSelector('text=/COMPLETED|FAILED/i', { timeout: 90000 }).catch(() => {});
    await page.waitForTimeout(1500);
  }

  const viewGraphButton = page.getByRole('button', { name: 'View graph' });
  if (await viewGraphButton.count()) {
    await viewGraphButton.click();
    await page.waitForTimeout(3000);
    await page.locator('text=Hierarchical').first().click();
    await page.waitForTimeout(2000);
    await page.locator('text=Force-directed').first().click();
    await page.waitForTimeout(3500);
  }

  await page.waitForTimeout(500);
  await page.screenshot({ path: `${SCREENSHOT_DIR}/nice-figure-result.png`, fullPage: true });

  const bodyText = await page.locator('body').innerText();
  console.log('BODY_SNIPPET_START');
  console.log(bodyText.slice(0, 3000));
  console.log('BODY_SNIPPET_END');

  await browser.close();
})().catch((err) => {
  console.error('DRIVER_FAILED: ' + err.stack);
  process.exit(1);
});
