/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { axiosMockAdapter, render, waitFor } from 'TestRoot/SpecUtil';
import BillOfMaterials from 'MainRoot/sbomManager/features/billOfMaterials/BillOfMaterials';
import { screen } from '@testing-library/dom';
import { getApplicationSummaryUrl, getAllApplicationSbomVersions } from 'MainRoot/util/CLMLocation';

describe('BillOfMaterials page', () => {
  let renderPage;
  const applicationPublicId = 'app_123';
  const internalAppId = 'abc123';
  const axiosMock = axiosMockAdapter();

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
          versionId: '1.0-SNAPSHOT',
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
    axiosMock.onGet(getApplicationSummaryUrl(applicationPublicId)).reply(200, {
      id: internalAppId,
    });
    axiosMock
      .onGet(getAllApplicationSbomVersions(internalAppId))
      .reply(200, ['1.0-SNAPSHOT', '1.1-SNAPSHOT', '1.2-SNAPSHOT']);
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
    const field = await screen.findByRole('button', { name: /Viewing:/i });
    expect(field).toHaveTextContent('Viewing: 1.0-SNAPSHOT');
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

  it('shows error when Application SBOM versions fail to load.', async () => {
    axiosMock.onGet(getApplicationSummaryUrl(applicationPublicId)).reply(200, {
      id: internalAppId,
    });
    axiosMock.onGet(getAllApplicationSbomVersions(internalAppId)).reply(() =>
      Promise.reject({
        response: {
          data: 'Error',
        },
      })
    );
    renderPage();

    const errorMessage = await screen.findByText('An error occurred loading data. Error');
    expect(errorMessage).toBeVisible();
  });
});
