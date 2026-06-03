import { test, expect } from '@playwright/test';

test.describe('Protected routes', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test('redirects /account to login when not authenticated', async ({ page }) => {
    await page.goto('/account');
    await expect(page).toHaveURL('/login', { timeout: 10000 });
  });

  test('/home is accessible without authentication', async ({ page }) => {
    await page.goto('/home');
    await expect(page).toHaveURL('/home');
    await expect(page.locator('input[placeholder="Kam šibaš?"]')).toBeVisible({ timeout: 10000 });
  });
});

test.describe('Landing page - unauthenticated', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('shows login button when not authenticated', async ({ page }) => {
    await expect(page.getByRole('button', { name: 'Prijava' })).toBeVisible();
  });

  test('does not show account button when not authenticated', async ({ page }) => {
    await expect(page.getByRole('button', { name: 'Moj račun' })).not.toBeVisible();
  });

  test('navigates to /home when clicking Najdi pot', async ({ page }) => {
    await page.getByRole('button', { name: 'Najdi pot' }).first().click();
    await expect(page).toHaveURL('/home');
  });
});

test.describe('Landing page - authenticated', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('shows logout button when authenticated', async ({ page }) => {
    await expect(page.getByRole('button', { name: 'Odjava' })).toBeVisible();
  });

  test('shows account button when authenticated', async ({ page }) => {
    await expect(page.getByRole('button', { name: 'Moj račun' })).toBeVisible();
  });

  test('navigates to account page', async ({ page }) => {
    await page.getByRole('button', { name: 'Moj račun' }).click();
    await expect(page).toHaveURL('/account');
  });
});
