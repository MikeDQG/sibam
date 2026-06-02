import { test, expect } from '@playwright/test';

test.describe('Login page UX', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
  });

  test('password toggle shows and hides password', async ({ page }) => {
    const passwordInput = page.locator('input[placeholder="Geslo"]');
    await passwordInput.fill('mypassword');

    await expect(passwordInput).toHaveAttribute('type', 'password');
    await page.getByRole('button', { name: 'Prikaži geslo' }).click();
    await expect(passwordInput).toHaveAttribute('type', 'text');
    await page.getByRole('button', { name: 'Skrij geslo' }).click();
    await expect(passwordInput).toHaveAttribute('type', 'password');
  });

  test('navigates back to landing page', async ({ page }) => {
    await page.getByRole('button', { name: 'Nazaj domov' }).click();
    await expect(page).toHaveURL('/');
  });

  test('navigates to register page', async ({ page }) => {
    await page.getByRole('button', { name: 'Registriraj se' }).click();
    await expect(page).toHaveURL('/register');
  });

  test('shows error on empty form submission', async ({ page }) => {
    await page.getByRole('button', { name: 'Prijavi se' }).click();
    await expect(page.locator('p.text-red-400')).toBeVisible({ timeout: 5000 });
  });

  test('shows error on invalid email format', async ({ page }) => {
    await page.locator('input[placeholder="Email"]').fill('notanemail');
    await page.locator('input[placeholder="Geslo"]').fill('somepassword');
    await page.getByRole('button', { name: 'Prijavi se' }).click();
    await expect(page.locator('p.text-red-400')).toBeVisible({ timeout: 5000 });
  });
});
