const { chromium } = require('playwright');

const SCREENSHOT_DIR = process.argv[2] || '.';

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1400, height: 1000 } });
  await page.goto('http://localhost:5173', { waitUntil: 'networkidle' });
  await page.waitForTimeout(500);
  await page.locator('button:has-text("Get started")').click();
  await page.waitForTimeout(800);
  await page.screenshot({ path: `${SCREENSHOT_DIR}/peek-01-picker.png`, fullPage: true });
  const bodyText = await page.locator('body').innerText();
  console.log('BODY_SNIPPET_START');
  console.log(bodyText.slice(0, 4000));
  console.log('BODY_SNIPPET_END');
  await browser.close();
})().catch((err) => {
  console.error('PEEK_FAILED: ' + err.stack);
  process.exit(1);
});
