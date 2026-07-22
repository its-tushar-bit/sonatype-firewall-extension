/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import userEvent from '@testing-library/user-event';
import { render, screen } from '../test-utils';
import { OnboardingProvider, STORAGE_KEY } from 'GuideRoot/onboarding/OnboardingProvider';
import { OnboardingModal } from 'GuideRoot/onboarding/OnboardingModal';

function renderModal() {
  return render(<OnboardingProvider><OnboardingModal /></OnboardingProvider>);
}

describe('OnboardingModal', () => {
  beforeEach(() => localStorage.clear());

  it('auto-opens at the Welcome step on first run', async () => {
    renderModal();
    expect(await screen.findByRole('dialog')).toBeInTheDocument();
    expect(screen.getByText('Welcome to AI Developer')).toBeInTheDocument();
    expect(screen.getByText('Step 1 of 4')).toBeInTheDocument();
  });

  it('advances with Continue and returns with Back', async () => {
    const user = userEvent.setup();
    renderModal();
    await screen.findByText('Welcome to AI Developer');
    await user.click(screen.getByRole('button', { name: 'Continue' }));
    expect(screen.getByText('Step 2 of 4')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Back' }));
    expect(screen.getByText('Step 1 of 4')).toBeInTheDocument();
  });

  it('the last step dismisses via "Get started" and persists completion', async () => {
    const user = userEvent.setup();
    renderModal();
    await screen.findByText('Welcome to AI Developer');
    await user.click(screen.getByRole('button', { name: 'Continue' })); // 2
    await user.click(screen.getByRole('button', { name: 'Continue' })); // 3
    await user.click(screen.getByRole('button', { name: 'Continue' })); // 4
    await user.click(screen.getByRole('button', { name: 'Get started' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(localStorage.getItem(STORAGE_KEY)).toBe('true');
  });

  it('"Skip tutorial" dismisses immediately', async () => {
    const user = userEvent.setup();
    renderModal();
    await screen.findByText('Welcome to AI Developer');
    await user.click(screen.getByRole('button', { name: 'Skip tutorial' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(localStorage.getItem(STORAGE_KEY)).toBe('true');
  });

  it('pressing Escape dismisses and persists completion', async () => {
    const user = userEvent.setup();
    renderModal();
    await screen.findByText('Welcome to AI Developer');
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(localStorage.getItem(STORAGE_KEY)).toBe('true');
  });
});
