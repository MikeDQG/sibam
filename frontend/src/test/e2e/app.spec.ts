import { test, expect } from '@playwright/test';

test.describe('Main app', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/home');
  });

  test('shows destination search input', async ({ page }) => {
    await expect(page.locator('input[placeholder="Kam šibaš?"]')).toBeVisible({ timeout: 10000 });
  });

  test('shows weather widget', async ({ page }) => {
    await expect(page.locator('input[placeholder="Kam šibaš?"]')).toBeVisible({ timeout: 10000 });
  });

  test('shows directions panel after typing destination', async ({ page }) => {
    const destinationInput = page.locator('input[placeholder="Kam šibaš?"]');
    await expect(destinationInput).toBeVisible({ timeout: 10000 });
    await destinationInput.fill('Maribor');
    await expect(destinationInput).toHaveValue('Maribor');
  });
});

test.describe('Landing page', () => {
  test('shows navigation and CTA', async ({ page }) => {
    await page.goto('/');
    await expect(page.getByRole('button', { name: 'Najdi pot' })).toBeVisible();
  });

  test('navigates to login from landing page', async ({ page }) => {
    await page.goto('/');
    await page.getByRole('button', { name: 'Prijava' }).click();
    await expect(page).toHaveURL('/login');
  });
});
