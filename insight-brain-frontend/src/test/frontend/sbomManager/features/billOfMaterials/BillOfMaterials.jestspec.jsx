/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render } from 'TestRoot/SpecUtil';
import BillOfMaterials from 'MainRoot/sbomManager/features/billOfMaterials/BillOfMaterials';
import { screen } from '@testing-library/dom';

describe('BillOfMaterials page', () => {
  let renderPage;

  beforeEach(() => {
    const preloadedState = {
      productFeatures: {
        productFeatures: {
          'sbom-manager': true,
        },
      },
      router: {
        currentState: { name: 'sbomManager.management.view.bom' },
      },
    };
    renderPage = (additionalPreloadedState = {}) =>
      render(<BillOfMaterials />, { preloadedState: { ...preloadedState, ...additionalPreloadedState } });
  });

  it('Renders page content', async () => {
    renderPage();
    expect(await screen.findByText('Bill Of Materials')).toBeVisible();
    expect(screen.getByText('Summary')).toBeVisible();
    expect(screen.getByText('Components')).toBeVisible();
  });

  it('shows error when the SBOM Manager license is disabled', async () => {
    renderPage({
      productFeatures: {
        productFeatures: {},
      },
    });

    const errorMessage = await screen.findByText(
      'An error occurred loading data. The SBOM Manager license feature is not enabled.'
    );
    expect(errorMessage).toBeVisible();
  });
});
