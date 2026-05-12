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
import { GUIDE_PRODUCTS } from 'GuideRoot/license/licenseProducts';
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

  it('renders children when user has the required license', async () => {
    jest.spyOn(licenseApi, 'fetchLicenseSummary').mockResolvedValue({
      productEdition: 'Lifecycle',
      products: ['Sonatype Guide'],
    });

    renderWithProviders(
      <LicenseGate requires={GUIDE_PRODUCTS}>
        <div>Guide Content</div>
      </LicenseGate>
    );

    await waitFor(() => {
      expect(screen.getByText('Guide Content')).toBeInTheDocument();
    });
  });

  it('renders learn-more page when user does not have the required license', async () => {
    jest.spyOn(licenseApi, 'fetchLicenseSummary').mockResolvedValue({
      productEdition: 'Lifecycle',
      products: ['Sonatype Lifecycle'],
    });

    renderWithProviders(
      <LicenseGate requires={GUIDE_PRODUCTS}>
        <div>Guide Content</div>
      </LicenseGate>
    );

    await waitFor(() => {
      expect(screen.getByText(/not currently enabled/i)).toBeInTheDocument();
    });
    expect(screen.queryByText('Guide Content')).not.toBeInTheDocument();
  });

  it('renders spinner while license is loading', () => {
    jest.spyOn(licenseApi, 'fetchLicenseSummary').mockReturnValue(new Promise(() => {}));

    renderWithProviders(
      <LicenseGate requires={GUIDE_PRODUCTS}>
        <div>Guide Content</div>
      </LicenseGate>
    );

    expect(screen.queryByText('Guide Content')).not.toBeInTheDocument();
    expect(screen.queryByText(/not currently enabled/i)).not.toBeInTheDocument();
  });

  it('renders learn-more page when license fetch fails', async () => {
    jest.spyOn(licenseApi, 'fetchLicenseSummary').mockRejectedValue(new Error('402'));

    renderWithProviders(
      <LicenseGate requires={GUIDE_PRODUCTS}>
        <div>Guide Content</div>
      </LicenseGate>
    );

    await waitFor(() => {
      expect(screen.getByText(/not currently enabled/i)).toBeInTheDocument();
    });
  });
});
