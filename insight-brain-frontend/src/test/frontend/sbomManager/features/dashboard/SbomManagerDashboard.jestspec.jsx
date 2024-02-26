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

  beforeEach(() => {
    productFeatureState = {
      productFeatures: {
        productFeatures: {
          'sbom-manager': true,
        },
      },
    };
  });

  it('Renders the page', async () => {
    render(<SbomManagerDashboard />, { preloadedState: productFeatureState });

    expect(await screen.findByText('Content for Dashboard')).toBeInTheDocument();
  });

  it('shows error when the SBOM Manager license is disabled', async () => {
    productFeatureState.productFeatures.productFeatures = {};
    render(<SbomManagerDashboard />, { preloadedState: productFeatureState });

    const errorMessage = await screen.findByText(
      'An error occurred loading data. The SBOM Manager license feature is not enabled.'
    );
    expect(errorMessage).toBeInTheDocument();
  });
});
