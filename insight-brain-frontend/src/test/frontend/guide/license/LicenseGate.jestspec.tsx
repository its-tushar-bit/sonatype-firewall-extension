/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import { LicenseProvider } from 'GuideRoot/license/LicenseProvider';
import { LicenseGate } from 'GuideRoot/license/LicenseGate';
import * as licenseApi from 'GuideRoot/license/licenseApi';

jest.mock('GuideRoot/license/licenseApi');

function renderWithProviders(ui: React.ReactElement) {
  return render(
    <Theme>
      <LicenseProvider>
        {ui}
      </LicenseProvider>
    </Theme>
  );
}

describe('LicenseGate', () => {
  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('renders children when guide is in the licensed solutions list', async () => {
    jest.spyOn(licenseApi, 'fetchLicensedSolutions').mockResolvedValue([
      { id: 'lifecycle', url: '/lifecycle' },
      { id: 'guide', url: '/guide' },
    ]);

    renderWithProviders(
      <LicenseGate>
        <div>Guide Content</div>
      </LicenseGate>
    );

    await waitFor(() => {
      expect(screen.getByText('Guide Content')).toBeInTheDocument();
    });
  });

  // The "guide" solution is absent from the response in two backend cases that are
  // indistinguishable to the frontend: a Lifecycle-only license (no Guide product), and a
  // Guide-product license where HDS dropped the GUIDE feature (SolutionResolver omits it).
  // Both must surface the learn-more page.
  it('renders learn-more page when "guide" is absent from the licensed solutions', async () => {
    jest.spyOn(licenseApi, 'fetchLicensedSolutions').mockResolvedValue([
      { id: 'lifecycle', url: '/lifecycle' },
    ]);

    renderWithProviders(
      <LicenseGate>
        <div>Guide Content</div>
      </LicenseGate>
    );

    await waitFor(() => {
      expect(screen.getByText(/not currently enabled/i)).toBeInTheDocument();
    });
    expect(screen.queryByText('Guide Content')).not.toBeInTheDocument();
  });

  it('renders spinner while licensed solutions are loading', () => {
    jest.spyOn(licenseApi, 'fetchLicensedSolutions').mockReturnValue(new Promise(() => {}));

    renderWithProviders(
      <LicenseGate>
        <div>Guide Content</div>
      </LicenseGate>
    );

    expect(screen.queryByText('Guide Content')).not.toBeInTheDocument();
    expect(screen.queryByText(/not currently enabled/i)).not.toBeInTheDocument();
  });

  it('renders learn-more page when licensed solutions fetch fails', async () => {
    jest.spyOn(licenseApi, 'fetchLicensedSolutions').mockRejectedValue(new Error('402'));

    renderWithProviders(
      <LicenseGate>
        <div>Guide Content</div>
      </LicenseGate>
    );

    await waitFor(() => {
      expect(screen.getByText(/not currently enabled/i)).toBeInTheDocument();
    });
  });
});
