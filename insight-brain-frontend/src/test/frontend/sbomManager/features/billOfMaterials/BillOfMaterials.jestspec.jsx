/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { axiosMockAdapter, render, waitFor } from 'TestRoot/SpecUtil';
import BillOfMaterials from 'MainRoot/sbomManager/features/billOfMaterials/BillOfMaterials';
import { screen } from '@testing-library/dom';
import { getApplicationSummaryUrl } from 'MainRoot/util/CLMLocation';

describe('BillOfMaterials page', () => {
  let renderPage;
  const applicationPublicId = 'app_123';

  beforeEach(() => {
    const preloadedState = {
      productFeatures: {
        productFeatures: {
          'sbom-manager': true,
          loading: true,
        },
      },
      router: {
        currentState: { name: 'sbomManager.management.view.bom' },
        currentParams: {
          applicationPublicId: applicationPublicId,
          versionId: '1.0-SNAPSHOT_TEST',
        },
      },
      billOfMaterialsPage: {
        loading: false,
        errorInternalAppId: null,
        internalAppId: null,
        publicAppId: null,
      },
    };
    renderPage = (additionalPreloadedState = {}) =>
      render(<BillOfMaterials />, { preloadedState: { ...preloadedState, ...additionalPreloadedState } });
  });

  it('Renders page content', async () => {
    const axiosMock = axiosMockAdapter();
    axiosMock.onGet(getApplicationSummaryUrl(applicationPublicId)).reply(200, {
      id: 'abc123',
    });
    renderPage();
    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());
    expect(await screen.findByText('Bill Of Materials')).toBeVisible();
    expect(screen.getByRole('button', { name: 'Download' })).toBeVisible();
    expect(screen.getByText('Critical')).toBeVisible();
    expect(screen.getByText('High')).toBeVisible();
    expect(screen.getByText('Medium')).toBeVisible();
    expect(screen.getByText('Low')).toBeVisible();
    expect(screen.getByText('None')).toBeVisible();
    expect(screen.getByText('Components')).toBeVisible();
  });

  it('shows error when the SBOM Manager license is disabled', async () => {
    renderPage({
      productFeatures: {
        productFeatures: {
          loading: false,
        },
      },
    });

    const errorMessage = await screen.findByText(
      'An error occurred loading data. The SBOM Manager license feature is not enabled.'
    );
    expect(errorMessage).toBeVisible();
  });
});
