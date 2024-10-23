/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import { screen, render } from 'TestRoot/SpecUtil';

import SbomApplicationsPage from 'MainRoot/sbomManager/features/sbomApplicationsPage/SbomApplicationsPage';

describe('SbomApplicationsPage', () => {
  let renderPage;

  beforeEach(() => {
    const preloadedState = {
      productFeatures: {
        productFeatures: {
          'sbom-manager': true,
          'sbom-policies': true,
          loading: true,
        },
      },
      router: {
        currentState: { name: 'sbomManager.applications' },
      },
    };

    renderPage = (additionalPreloadedState = {}) =>
      render(<SbomApplicationsPage />, { preloadedState: { ...preloadedState, ...additionalPreloadedState } });
  });

  it('renders page content', async () => {
    renderPage();
    expect(screen.getByRole('heading', { name: /Applications/ })).toBeVisible();
  });
});
