import { test, expect } from '@playwright/test';

const EMAIL = process.env.E2E_TEST_EMAIL ?? 'test@test.com';

test.describe('Account page', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/account');
  });

  test('shows user greeting', async ({ page }) => {
    await expect(page.getByText(/Zdravo/)).toBeVisible({ timeout: 10000 });
  });

  test('shows logged in email', async ({ page }) => {
    await expect(page.getByText('Prijavljen si z emailom')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText(EMAIL)).toBeVisible({ timeout: 10000 });
  });

  test('shows saved locations section', async ({ page }) => {
    await expect(page.getByRole('heading', { name: 'Shranjene lokacije' })).toBeVisible({ timeout: 10000 });
  });

  test('shows recent routes section', async ({ page }) => {
    await expect(page.getByRole('heading', { name: 'Zadnje poti' })).toBeVisible({ timeout: 10000 });
  });

  test('navigates back to home', async ({ page }) => {
    await page.getByRole('button', { name: 'Domov' }).click();
    await expect(page).toHaveURL('/home');
  });

  test('logs out from account page', async ({ page }) => {
    await page.getByRole('button', { name: 'Odjava' }).click();
    await expect(page).toHaveURL('/login', { timeout: 10000 });
  });
});

test.describe('Account page - unauthenticated', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test('redirects to login when not authenticated', async ({ page }) => {
    await page.goto('/account');
    await expect(page).toHaveURL('/login', { timeout: 10000 });
  });
});
