/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import userEvent from '@testing-library/user-event';
import { render, screen } from '../test-utils';
import { OnboardingProvider, STORAGE_KEY } from 'GuideRoot/onboarding/OnboardingProvider';
import { OnboardingModal } from 'GuideRoot/onboarding/OnboardingModal';
import { PolicyContextPicker } from 'GuideRoot/components/navigation/context-picker/PolicyContextPicker';

describe('Onboarding replay from the policy-context picker', () => {
  beforeEach(() => {
    localStorage.clear();
    // Mark the tour completed so it starts closed — we assert the "Need help?" click reopens it.
    localStorage.setItem(STORAGE_KEY, 'true');
  });

  it('"Need help?" closes the picker and restarts the tour at Welcome', async () => {
    const user = userEvent.setup();
    // Both live under one OnboardingProvider so open() from the picker drives the tour modal.
    // The picker owns the Dialog.Root that hosts PolicyContextModal.
    render(
      <OnboardingProvider>
        <OnboardingModal />
        <PolicyContextPicker />
      </OnboardingProvider>
    );

    // Tour starts closed (flag set).
    expect(screen.queryByText('Welcome to AI Developer')).not.toBeInTheDocument();

    // Open the picker modal, then click "Need help?".
    await user.click(screen.getByRole('button', { name: 'Policy context — open picker' }));
    await user.click(await screen.findByRole('button', { name: 'Need help?' }));

    // The picker modal closed (its listbox is gone) and the tour reopened at Welcome.
    expect(screen.queryByRole('listbox', { name: 'Policy context options' })).not.toBeInTheDocument();
    expect(await screen.findByText('Welcome to AI Developer')).toBeInTheDocument();
    expect(screen.getByText('Step 1 of 4')).toBeInTheDocument();
  });
});
