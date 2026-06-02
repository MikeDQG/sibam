import { test as setup, expect } from '@playwright/test';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const EMAIL = process.env.E2E_TEST_EMAIL ?? 'test@test.com';
const PASSWORD = process.env.E2E_TEST_PASSWORD ?? 'TestTestTest!';

export const authFile = path.join(__dirname, '.auth/user.json');

setup('authenticate', async ({ page }) => {
  await page.goto('/login');
  await page.locator('input[placeholder="Email"]').fill(EMAIL);
  await page.locator('input[placeholder="Geslo"]').fill(PASSWORD);
  await page.getByRole('button', { name: 'Prijavi se' }).click();
  await expect(page).toHaveURL('/home', { timeout: 20000 });
  await page.context().storageState({ path: authFile });
});
