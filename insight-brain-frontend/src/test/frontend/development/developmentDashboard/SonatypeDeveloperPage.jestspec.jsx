/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import SonatypeDeveloperPage from 'MainRoot/development/developmentDashboard/SonatypeDeveloperPage';
import { SECTIONS } from 'MainRoot/development/developmentDashboard/sections';

import * as ProductFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';
import { DEVELOPER_FEATURE_DISABLED_MESSAGE } from 'MainRoot/development/developmentDashboard/LicenseLockScreen';

describe('SonatypeDeveloperPage', () => {
  let renderComponent;
  let selectIsDeveloperDashboardEnabled;

  const defaultPreloadedState = {
    router: {
      currentState: {
        name: `developer.dashboard.${SECTIONS.OVERVIEW}`,
      },
    },
  };

  beforeEach(() => {
    selectIsDeveloperDashboardEnabled = jest
      .spyOn(ProductFeaturesSelectors, 'selectIsDeveloperDashboardEnabled')
      .mockReturnValue(true);

    renderComponent = (preloadedState) =>
      render(<SonatypeDeveloperPage />, { preloadedState: preloadedState || defaultPreloadedState });
  });

  it('renders a heading "Dashboard"', () => {
    renderComponent();
    expect(screen.getByRole('heading', { name: 'Dashboard' })).toBeInTheDocument();
  });

  it('renders an alert in place of content given the feature is not enabled for the license', async () => {
    selectIsDeveloperDashboardEnabled.mockReturnValue(false);

    renderComponent();

    const alert = screen.getByRole('alert');
    expect(alert).toBeInTheDocument();
    expect(alert).toHaveTextContent(DEVELOPER_FEATURE_DISABLED_MESSAGE);

    expect(screen.queryByRole('tablist')).not.toBeInTheDocument();
  });

  it('does not render the Try AI Developer banner when not entitled', () => {
    renderComponent();
    expect(screen.queryByText(/Try AI Developer/)).not.toBeInTheDocument();
  });

  it('renders the Try AI Developer banner when entitled', () => {
    renderComponent({
      ...defaultPreloadedState,
      solutionSwitcher: {
        licensedSolutions: [{ id: 'guide', url: 'https://ai-developer.example.com' }],
        loading: false,
        loadError: null,
      },
    });

    expect(
      screen.getByText('Get AI-powered remediation guidance and faster fixes right in your workflow.')
    ).toBeInTheDocument();
    const link = screen.getByRole('link', { name: /Try AI Developer/ });
    expect(link).toHaveAttribute('href', 'https://ai-developer.example.com');
  });

  it('does not render the Try AI Developer banner when entitled but the url is missing', () => {
    renderComponent({
      ...defaultPreloadedState,
      solutionSwitcher: {
        licensedSolutions: [{ id: 'guide' }],
        loading: false,
        loadError: null,
      },
    });

    expect(
      screen.queryByText('Get AI-powered remediation guidance and faster fixes right in your workflow.')
    ).not.toBeInTheDocument();
  });
});
