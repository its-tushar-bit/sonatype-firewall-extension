/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { LicenseProvider, useLicense } from 'GuideRoot/license/LicenseProvider';
import * as licenseApi from 'GuideRoot/license/licenseApi';
import { GUIDE_PRODUCTS } from 'GuideRoot/license/licenseProducts';

jest.mock('GuideRoot/license/licenseApi');

function LicenseConsumer() {
  const { products, isLoading, hasLicenseFor } = useLicense();
  return (
    <div>
      <span data-testid="loading">{String(isLoading)}</span>
      <span data-testid="products">{products.join(',')}</span>
      <span data-testid="has-guide">{String(hasLicenseFor(GUIDE_PRODUCTS))}</span>
    </div>
  );
}

describe('LicenseProvider', () => {
  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('starts in loading state then exposes fetched products', async () => {
    jest.spyOn(licenseApi, 'fetchLicenseSummary').mockResolvedValue({
      productEdition: 'Lifecycle',
      products: ['Sonatype Lifecycle', 'Sonatype Guide'],
    });

    render(
      <LicenseProvider>
        <LicenseConsumer />
      </LicenseProvider>
    );

    expect(screen.getByTestId('loading')).toHaveTextContent('true');

    await waitFor(() => {
      expect(screen.getByTestId('loading')).toHaveTextContent('false');
    });
    expect(screen.getByTestId('products')).toHaveTextContent('Sonatype Lifecycle,Sonatype Guide');
    expect(screen.getByTestId('has-guide')).toHaveTextContent('true');
  });

  it('hasLicenseFor returns false when product is not in the list', async () => {
    jest.spyOn(licenseApi, 'fetchLicenseSummary').mockResolvedValue({
      productEdition: 'Lifecycle',
      products: ['Sonatype Lifecycle'],
    });

    render(
      <LicenseProvider>
        <LicenseConsumer />
      </LicenseProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId('loading')).toHaveTextContent('false');
    });
    expect(screen.getByTestId('has-guide')).toHaveTextContent('false');
  });

  it('sets empty products when fetch fails', async () => {
    jest.spyOn(licenseApi, 'fetchLicenseSummary').mockRejectedValue(new Error('402 Payment Required'));

    render(
      <LicenseProvider>
        <LicenseConsumer />
      </LicenseProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId('loading')).toHaveTextContent('false');
    });
    expect(screen.getByTestId('products')).toHaveTextContent('');
    expect(screen.getByTestId('has-guide')).toHaveTextContent('false');
  });

  it('throws when useLicense is used outside LicenseProvider', () => {
    const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});

    expect(() => render(<LicenseConsumer />)).toThrow(
      'useLicense must be used within a LicenseProvider'
    );

    consoleSpy.mockRestore();
  });
});
