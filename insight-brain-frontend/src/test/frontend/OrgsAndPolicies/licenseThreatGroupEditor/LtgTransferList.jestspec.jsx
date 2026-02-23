/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import LtgTransferList from 'MainRoot/OrgsAndPolicies/licenseThreatGroupEditor/LtgTransferList';

import 'TestRoot/SpecUtil';

describe('LtgTransferList', () => {
  let renderComponent, setSelectedLicensesSpy;
  const allLicenses = [
    { id: 'first', displayName: 'TestLicenseFirst' },
    { id: 'second', displayName: 'TestLicenseSecond' },
  ];
  const licenseIds = [];

  beforeEach(() => {
    setSelectedLicensesSpy = jest.fn().mockName('setSelectedLicenses');
    renderComponent = () =>
      render(
        <LtgTransferList
          allLicenses={allLicenses}
          licenseIds={licenseIds}
          setSelectedLicenses={setSelectedLicensesSpy}
        />
      );
  });

  it('renders NxTransferList', () => {
    renderComponent();

    expect(screen.getByText('Available Licenses')).toBeVisible();
    expect(screen.getByText('Included Licenses')).toBeVisible();
  });
});
