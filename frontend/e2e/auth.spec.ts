import { test, expect } from '@playwright/test';

test.describe('Login page', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
  });

  test('renders login form', async ({ page }) => {
    await expect(page.getByRole('heading', { name: 'Prijava' })).toBeVisible();
    await expect(page.locator('input[placeholder="Email"]')).toBeVisible();
    await expect(page.locator('input[placeholder="Geslo"]')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Prijavi se' })).toBeVisible();
  });

  test('shows error on wrong credentials', async ({ page }) => {
    await page.locator('input[placeholder="Email"]').fill('wrong@wrong.com');
    await page.locator('input[placeholder="Geslo"]').fill('wrongpassword');
    await page.getByRole('button', { name: 'Prijavi se' }).click();
    await expect(page.locator('p.text-red-400')).toBeVisible({ timeout: 10000 });
  });
});

test.describe('Logout', () => {
  test('logs out and redirects to login page', async ({ page }) => {
    await page.goto('/home');
    await page.getByRole('button', { name: 'Odjava' }).click();
    await expect(page).toHaveURL('/login', { timeout: 10000 });
  });
});
