/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { axiosMockAdapter, render, waitFor } from 'TestRoot/SpecUtil';
import BillOfMaterials from 'MainRoot/sbomManager/features/billOfMaterials/BillOfMaterials';
import { screen } from '@testing-library/dom';
import { getApplicationSummaryUrl, getAllApplicationSbomVersions, getSbomMetadataUrl } from 'MainRoot/util/CLMLocation';

describe('BillOfMaterials page', () => {
  let renderPage;
  const applicationPublicId = 'app_123';
  const internalAppId = 'abc123';
  const axiosMock = axiosMockAdapter();

  const sbomMetadataInitialState = {
    author: [],
    manufacturer: [],
    supplier: [],
    person: [],
    organization: [],
    specification: null,
    specVersion: null,
    fileFormat: null,
    createdAt: null,
  };

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
        // internal-application-id
        loadingInternalAppId: true,
        errorInternalAppId: null,
        internalAppId: null,
        publicAppId: null,

        // sbom-versions
        loadingSbomVersions: true,
        errorSbomVersions: null,
        sbomVersions: null,

        // sbom-metadata
        loadingSbomMetadata: true,
        errorSbomMetadata: null,
        sbomMetadata: { ...sbomMetadataInitialState },
        scanId: null,
      },
    };
    renderPage = (additionalPreloadedState = {}) =>
      render(<BillOfMaterials />, { preloadedState: { ...preloadedState, ...additionalPreloadedState } });
  });

  it('Renders page content', async () => {
    axiosMock.onGet(getApplicationSummaryUrl(applicationPublicId)).reply(200, {
      id: internalAppId,
      name: 'Alice',
    });
    axiosMock
      .onGet(getAllApplicationSbomVersions(internalAppId))
      .reply(200, ['1.0-SNAPSHOT', '1.1-SNAPSHOT', '1.2-SNAPSHOT']);
    axiosMock.onGet(getSbomMetadataUrl(internalAppId, '1.0-SNAPSHOT')).reply(200, {
      author: ['Alice', 'Bob'],
      manufacturer: ['Orange'],
      supplier: ['Apple'],
      person: ['John', 'Jane'],
      organization: ['Sonatype'],
      specification: 'SPDX',
      specVersion: '2.3',
      fileFormat: 'json',
      createdAt: '2024-01-12T20:11:22Z',
      scanId: 'scan-id',
    });

    renderPage();

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    expect(screen.getByRole('heading', { name: /Alice/ })).toBeVisible();

    const sbomImportedDate = screen.getByTestId('bill-of-materials-page-sbom-imported-date');
    expect(sbomImportedDate).toHaveTextContent('Imported:2024-01-12 20:11:22 UTC+00:00');

    expect(screen.getByRole('button', { name: 'Download' })).toBeVisible();
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

  it('shows error when SBOM Metadata fail to load.', async () => {
    axiosMock.onGet(getApplicationSummaryUrl(applicationPublicId)).reply(200, {
      id: internalAppId,
      name: 'Alice',
    });
    axiosMock
      .onGet(getAllApplicationSbomVersions(internalAppId))
      .reply(200, ['1.0-SNAPSHOT', '1.1-SNAPSHOT', '1.2-SNAPSHOT']);
    axiosMock.onGet(getSbomMetadataUrl(internalAppId, '1.0-SNAPSHOT')).reply(() =>
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
