import { test, expect } from '@playwright/test';

test.describe('Register page', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/register');
  });

  test('renders registration form', async ({ page }) => {
    await expect(page.locator('input[placeholder="Ime in priimek"]')).toBeVisible();
    await expect(page.locator('input[placeholder="Email"]')).toBeVisible();
    await expect(page.locator('input[placeholder="Geslo"]')).toBeVisible();
    await expect(page.locator('input[placeholder="Ponovi geslo"]')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Registriraj se' })).toBeVisible();
  });

  test('shows password requirements', async ({ page }) => {
    await expect(page.getByText('Vsaj 12 znakov')).toBeVisible();
    await expect(page.getByText('Velike črke')).toBeVisible();
    await expect(page.getByText('Male črke')).toBeVisible();
    await expect(page.getByText('Posebni znaki')).toBeVisible();
  });

  test('password requirements update as user types', async ({ page }) => {
    const passwordInput = page.locator('input[placeholder="Geslo"]');
    await passwordInput.fill('Short1!');
    await expect(page.getByText('Vsaj 12 znakov')).toBeVisible();

    await passwordInput.fill('LongEnoughPassword1!');
    await expect(page.getByText('Vsaj 12 znakov')).toBeVisible();
  });

  test('shows error when passwords do not match', async ({ page }) => {
    await page.locator('input[placeholder="Ime in priimek"]').fill('Test User');
    await page.locator('input[placeholder="Email"]').fill('newuser@test.com');
    await page.locator('input[placeholder="Geslo"]').fill('ValidPassword1!');
    await page.locator('input[placeholder="Ponovi geslo"]').fill('DifferentPassword1!');
    await page.getByRole('button', { name: 'Registriraj se' }).click();
    await expect(page.locator('p.text-red-400')).toBeVisible({ timeout: 5000 });
  });

  test('navigates back to landing page', async ({ page }) => {
    await page.getByRole('button', { name: 'Nazaj domov' }).click();
    await expect(page).toHaveURL('/');
  });

  test('navigates to login page', async ({ page }) => {
    await page.getByRole('button', { name: 'Prijavi se' }).click();
    await expect(page).toHaveURL('/login');
  });
});
