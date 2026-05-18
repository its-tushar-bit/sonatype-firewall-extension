/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import ProductLicenceSummary from 'MainRoot/configuration/gettingStarted/components/ProductLicenseSummary';

describe('ProductLicenseSummary', () => {
  const mockLicense = {
    applicationCountToDisplay: 10,
    applicationLimitToDisplay: 100,
    expiryTimestamp: 1672531200000,
    fingerprint: 'abc123xyz456',
    products: ['Sonatype Lifecycle', 'Sonatype Guide'],
    sbomCountToDisplay: 5,
    sbomLimitToDisplay: 50,
    creditAmountToDisplay: null,
  };

  it('renders credit amount when provided', () => {
    const licenseWithCredits = {
      ...mockLicense,
      creditAmountToDisplay: '5000',
    };

    render(<ProductLicenceSummary license={licenseWithCredits} tenantMode="single-tenant" />);

    expect(screen.getByText('Licensed Credits')).toBeInTheDocument();
    expect(screen.getByText('5000')).toBeInTheDocument();
  });

  it('does not render credit amount when not provided', () => {
    render(<ProductLicenceSummary license={mockLicense} tenantMode="single-tenant" />);

    expect(screen.queryByText('Licensed Credits')).not.toBeInTheDocument();
  });
});
