/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render } from 'TestRoot/SpecUtil';
import SbomManagerDashboard from 'MainRoot/sbomManager/features/dashboard/SbomManagerDashboard';
import { screen } from '@testing-library/dom';

describe('SbomManagerDashboard page', () => {
  let productFeatureState;
  let renderComponent;
  beforeEach(() => {
    productFeatureState = {
      productFeatures: {
        productFeatures: {
          'sbom-manager': true,
        },
      },
      router: { currentState: { name: 'sbomManager.dashboard' } },
    };
    renderComponent = (preloadedState = productFeatureState) => render(<SbomManagerDashboard />, { preloadedState });
  });

  it('Renders the page', async () => {
    renderComponent();
    expect(await screen.findByText('Content for Dashboard')).toBeVisible();
  });

  it('shows error when the SBOM Manager license is disabled', async () => {
    productFeatureState.productFeatures.productFeatures = {};
    renderComponent();

    const errorMessage = await screen.findByText(
      'An error occurred loading data. The SBOM Manager license feature is not enabled.'
    );
    expect(errorMessage).toBeVisible();
  });
});
