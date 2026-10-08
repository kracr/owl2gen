const { chromium } = require('playwright');
(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1400, height: 1100 } });
  await page.goto('http://localhost:5173', { waitUntil: 'networkidle' });
  const getStarted = page.locator('text=Get started').first();
  if (await getStarted.count()) {
    await getStarted.click();
    await page.waitForTimeout(800);
  }
  const bodyText = await page.locator('body').innerText();
  console.log(bodyText.slice(0, 6000));
  await browser.close();
})();
