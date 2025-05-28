/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import DashboardWaivers from 'MainRoot/dashboard/results/waivers/DashboardWaivers';

describe('DashboardWaivers', function () {
  const renderComponent = () => render(<DashboardWaivers />);

  it('renders Existing and Request Waivers tabs', function () {
    renderComponent();
    const tabs = screen.getAllByRole('tab');
    expect(tabs).toHaveLength(2);
    expect(tabs[0]).toHaveTextContent('Existing Waivers');
    expect(tabs[1]).toHaveTextContent('Requested Waivers');
  });
});
