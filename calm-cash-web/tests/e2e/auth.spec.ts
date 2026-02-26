import { expect, test } from '@playwright/test'

test('auth flow basics: tab titles, isolated fields, and validation', async ({ page }) => {
  await page.goto('/')
  const activeForm = () => page.locator('.auth-form-panel').last()

  await expect(page).toHaveTitle('Calm Cash | Sign In')

  await activeForm().getByPlaceholder('Enter your email').fill('sign-in@example.com')
  await activeForm().getByPlaceholder('Enter your password').fill('Password123!')

  await page.getByRole('tab', { name: 'Create Account' }).click()
  await expect(page).toHaveTitle('Calm Cash | Create Account')
  await page.waitForTimeout(300)

  await expect(activeForm().getByPlaceholder('you@example.com')).toHaveValue('')
  await expect(activeForm().getByPlaceholder('Choose a display name')).toHaveValue('')

  await activeForm().getByPlaceholder('you@example.com').fill('new-user@example.com')
  await activeForm().getByPlaceholder('Choose a display name').fill('abc')
  await activeForm().getByPlaceholder('Create a strong password').fill('ValidPass123!')

  await expect(activeForm().getByRole('status')).toContainText('Display name must be at least 6 characters.')

  await page.getByRole('tab', { name: 'Sign In' }).click()
  await page.waitForTimeout(300)
  await expect(activeForm().getByPlaceholder('Enter your email')).toHaveValue('sign-in@example.com')
  await expect(activeForm().getByPlaceholder('Enter your password')).toHaveValue('Password123!')

  await activeForm().getByPlaceholder('Enter your email').fill('invalid-email')
  await expect(activeForm().getByRole('status')).toContainText('Please enter a valid email address.')
})
