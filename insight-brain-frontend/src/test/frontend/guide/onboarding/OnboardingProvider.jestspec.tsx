/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import userEvent from '@testing-library/user-event';
import { render, screen } from '../test-utils';
import { OnboardingProvider, useOnboarding, STORAGE_KEY } from 'GuideRoot/onboarding/OnboardingProvider';

function Harness() {
  const { isOpen, open, dismiss } = useOnboarding();
  return (
    <div>
      <span>open: {String(isOpen)}</span>
      <button onClick={open}>open-tour</button>
      <button onClick={dismiss}>dismiss-tour</button>
    </div>
  );
}

describe('OnboardingProvider', () => {
  beforeEach(() => localStorage.clear());

  it('auto-opens on first run when the completion flag is unset', async () => {
    render(<OnboardingProvider><Harness /></OnboardingProvider>);
    expect(await screen.findByText('open: true')).toBeInTheDocument();
  });

  it('stays closed when the completion flag is set', async () => {
    localStorage.setItem(STORAGE_KEY, 'true');
    render(<OnboardingProvider><Harness /></OnboardingProvider>);
    expect(await screen.findByText('open: false')).toBeInTheDocument();
  });

  it('dismiss() persists the flag and closes; open() reopens', async () => {
    const user = userEvent.setup();
    render(<OnboardingProvider><Harness /></OnboardingProvider>);
    await user.click(screen.getByRole('button', { name: 'dismiss-tour' }));
    expect(screen.getByText('open: false')).toBeInTheDocument();
    expect(localStorage.getItem(STORAGE_KEY)).toBe('true');
    await user.click(screen.getByRole('button', { name: 'open-tour' }));
    expect(screen.getByText('open: true')).toBeInTheDocument();
  });

  it('useOnboarding() outside a provider returns safe defaults (no throw)', () => {
    render(<Harness />);
    expect(screen.getByText('open: false')).toBeInTheDocument();
  });
});
