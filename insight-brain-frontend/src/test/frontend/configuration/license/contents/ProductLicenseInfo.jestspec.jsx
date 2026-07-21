/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import ProductLicenseInfo, { EXPIRATION_DATE_FORMAT } from 'MainRoot/configuration/license/contents/ProductLicenseInfo';
import { formatDate } from 'MainRoot/util/dateUtils';

describe('ProductLicenseInfo', () => {
  const mockLicense = {
    contactCompany: 'Acme Corp',
    contactName: 'John Doe',
    contactEmail: 'john.doe@acme.com',
    expiryTimestamp: 1672531200000,
    daysToExpiration: 30,
    fingerprint: 'abc123xyz456',
    productEdition: 'Enterprise',
    products: ['Sonatype Lifecycle', 'Sonatype Firewall'],
    licensedUsersToDisplay: 100,
    firewallUsersToDisplay: 50,
    applicationLimitToDisplay: 200,
    applicationCountToDisplay: 150,
    sbomLimitToDisplay: 300,
    sbomCountToDisplay: 250,
  };

  it('renders basic license information correctly', () => {
    render(<ProductLicenseInfo license={mockLicense} />);

    expect(screen.getByText('Acme Corp')).toBeInTheDocument();
    expect(screen.getByText('John Doe')).toBeInTheDocument();
    expect(screen.getByText('john.doe@acme.com')).toBeInTheDocument();
    const formattedDate = formatDate(mockLicense.expiryTimestamp, EXPIRATION_DATE_FORMAT);
    expect(screen.getByText(formattedDate)).toBeInTheDocument();
    expect(screen.getByText('30')).toBeInTheDocument();
    expect(screen.getByText('abc123xyz456')).toBeInTheDocument();
  });

  it('renders license types correctly', () => {
    render(<ProductLicenseInfo license={mockLicense} />);

    mockLicense.products.forEach((product) => {
      expect(screen.getByText(product)).toBeInTheDocument();
    });
  });

  it('renders "Sonatype Guide" license type as "Sonatype AI Developer"', () => {
    const licenseWithGuide = {
      ...mockLicense,
      products: [...mockLicense.products, 'Sonatype Guide'],
    };

    render(<ProductLicenseInfo license={licenseWithGuide} />);

    expect(screen.getByText('Sonatype AI Developer')).toBeInTheDocument();
    expect(screen.queryByText('Sonatype Guide')).not.toBeInTheDocument();
  });

  it('renders user limits correctly', () => {
    render(<ProductLicenseInfo license={mockLicense} />);

    expect(screen.getByText('Licensed Developers')).toBeInTheDocument();
    expect(screen.getByText('Lifecycle — 100')).toBeInTheDocument();
    expect(screen.getByText('Firewall — 50')).toBeInTheDocument();
  });

  it('renders application limits correctly', () => {
    render(<ProductLicenseInfo license={mockLicense} />);

    expect(screen.getByText('Licensed Applications')).toBeInTheDocument();
    expect(screen.getByText('200 (150 in use)')).toBeInTheDocument();
  });

  it('renders SBOM limits correctly', () => {
    render(<ProductLicenseInfo license={mockLicense} />);

    expect(screen.getByText('Licensed SBOMs')).toBeInTheDocument();
    expect(screen.getByText('300 (250 in use)')).toBeInTheDocument();
  });

  it('does not render user limits when not provided', () => {
    const licenseWithoutUserLimits = {
      ...mockLicense,
      licensedUsersToDisplay: null,
      firewallUsersToDisplay: null,
    };

    render(<ProductLicenseInfo license={licenseWithoutUserLimits} />);

    expect(screen.queryByText('Licensed Developers')).not.toBeInTheDocument();
  });

  it('does not render application limits when not provided', () => {
    const licenseWithoutAppLimits = {
      ...mockLicense,
      applicationLimitToDisplay: null,
    };

    render(<ProductLicenseInfo license={licenseWithoutAppLimits} />);

    expect(screen.queryByText('Licensed Applications')).not.toBeInTheDocument();
  });

  it('does not render SBOM limits when not provided', () => {
    const licenseWithoutSbomLimits = {
      ...mockLicense,
      sbomLimitToDisplay: null,
    };

    render(<ProductLicenseInfo license={licenseWithoutSbomLimits} />);

    expect(screen.queryByText('Licensed SBOMs')).not.toBeInTheDocument();
  });

  it('renders Lifecycle Cloud user limit when product is included', () => {
    const licenseWithLifecycleCloud = {
      ...mockLicense,
      products: [...mockLicense.products, 'Sonatype Lifecycle Cloud'],
    };

    render(<ProductLicenseInfo license={licenseWithLifecycleCloud} />);

    expect(screen.getByText('Lifecycle Cloud — 100')).toBeInTheDocument();
  });

  it('does not render Lifecycle Cloud user limit when product is not included', () => {
    render(<ProductLicenseInfo license={mockLicense} />);

    expect(screen.queryByText('Lifecycle Cloud — 100')).not.toBeInTheDocument();
  });

  it('renders SBOM limit without count when sbomCountToDisplay is not provided', () => {
    const licenseWithoutSbomCount = {
      ...mockLicense,
      sbomCountToDisplay: null,
    };

    render(<ProductLicenseInfo license={licenseWithoutSbomCount} />);

    expect(screen.getByText('300')).toBeInTheDocument();
    expect(screen.queryByText('300 (null in use)')).not.toBeInTheDocument();
  });

  it('renders a single user limit when only one type is provided', () => {
    const licenseWithSingleUserLimit = {
      ...mockLicense,
      firewallUsersToDisplay: null,
    };

    render(<ProductLicenseInfo license={licenseWithSingleUserLimit} />);

    expect(screen.getByText('Licensed Developers')).toBeInTheDocument();
    expect(screen.getByText('100')).toBeInTheDocument();
    expect(screen.queryByText('Lifecycle —')).not.toBeInTheDocument();
  });

  it('renders credit amount when provided', () => {
    const licenseWithCredits = {
      ...mockLicense,
      creditAmountToDisplay: '5000',
    };

    render(<ProductLicenseInfo license={licenseWithCredits} />);

    expect(screen.getByText('Licensed Credits')).toBeInTheDocument();
    expect(screen.getByText('5000')).toBeInTheDocument();
  });

  it('does not render credit amount when not provided', () => {
    const licenseWithoutCredits = {
      ...mockLicense,
      creditAmountToDisplay: null,
    };

    render(<ProductLicenseInfo license={licenseWithoutCredits} />);

    expect(screen.queryByText('Licensed Credits')).not.toBeInTheDocument();
  });
});
