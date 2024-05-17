/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import moment from 'moment';
import { axiosMockAdapter, render, waitFor } from 'TestRoot/SpecUtil';
import BillOfMaterials from 'MainRoot/sbomManager/features/billOfMaterials/BillOfMaterials';
import { screen } from '@testing-library/dom';
import {
  getApplicationSummaryUrl,
  getAllApplicationSbomVersions,
  getSbomMetadataUrl,
  getSbomSummaryUrl,
} from 'MainRoot/util/CLMLocation';

describe('BillOfMaterials page', () => {
  let renderPage;
  const applicationPublicId = 'app_123';
  const internalAppId = 'abc123';
  const axiosMock = axiosMockAdapter();

  const sbomMetadataInitialState = Object.freeze({
    author: [],
    manufacturer: [],
    supplier: [],
    person: [],
    organization: [],
    specification: null,
    specVersion: null,
    fileFormat: null,
    createdAt: null,
  });

  const vulnerabilitiesSummaryInitialState = Object.freeze({
    critical: null,
    high: null,
    medium: null,
    low: null,
  });

  const componentSummaryInitialState = Object.freeze({
    direct: null,
    transitive: null,
    unspecified: null,
  });

  const createdAt = moment(new Date('2024-01-12T20:11:22.000+00:00')).format('YYYY-MM-DD HH:mm:ss');

  const sbomMetadataResponsePayload = {
    author: ['Alice', 'Bob'],
    manufacturer: ['Orange'],
    supplier: ['Apple'],
    person: ['John', 'Jane'],
    organization: ['Sonatype'],
    specification: 'SPDX',
    specVersion: '2.3',
    fileFormat: 'json',
    createdAt: createdAt,
    scanId: 'scan-id',
  };

  const sbomSummaryResponsePayload = Object.freeze({
    applicationVersion: '123',
    none: 123,
    low: 1000,
    medium: 200,
    high: 30,
    critical: 4,
    dependencyType: {
      direct: 5000,
      transitive: 600,
      unspecified: 78,
    },
    annotatedPercentage: 75,
  });

  const sbomSummaryNoVulnerabilitiesResponsePayload = Object.freeze({
    applicationVersion: '123',
    none: 0,
    low: 0,
    medium: 0,
    high: 0,
    critical: 0,
    dependencyType: {
      direct: 5000,
      transitive: 600,
      unspecified: 78,
    },
    annotatedPercentage: null,
  });

  const sbomSummaryNoComponentsResponsePayload = Object.freeze({
    applicationVersion: '123',
    none: null,
    low: null,
    medium: null,
    high: null,
    critical: null,
    dependencyType: null,
    annotatedPercentage: null,
  });

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
        publicAppId: null,

        // internal-application-id
        loadingInternalAppId: true,
        errorInternalAppId: null,
        internalAppId: null,

        // sbom-versions
        loadingSbomVersions: true,
        errorSbomVersions: null,
        sbomVersions: null,

        // sbom-metadata
        loadingSbomMetadata: true,
        errorSbomMetadata: null,
        sbomMetadata: { ...sbomMetadataInitialState },
        scanId: null,

        // sbom-summary
        loadingSbomSummary: true,
        errorSbomSummary: null,
        componentSummary: { ...componentSummaryInitialState },
        vulnerabilitiesSummary: { ...vulnerabilitiesSummaryInitialState },
        annotatedVulnerabilitesPercentage: null,
      },
    };
    renderPage = (additionalPreloadedState = {}) =>
      render(<BillOfMaterials />, { preloadedState: { ...preloadedState, ...additionalPreloadedState } });
  });

  it('renders page content', async () => {
    axiosMock.onGet(getApplicationSummaryUrl(applicationPublicId)).reply(200, {
      id: internalAppId,
      name: 'Alice',
    });
    axiosMock
      .onGet(getAllApplicationSbomVersions(internalAppId))
      .reply(200, ['1.0-SNAPSHOT', '1.1-SNAPSHOT', '1.2-SNAPSHOT']);
    axiosMock.onGet(getSbomMetadataUrl(internalAppId, '1.0-SNAPSHOT')).reply(200, sbomMetadataResponsePayload);
    axiosMock.onGet(getSbomSummaryUrl(internalAppId, '1.0-SNAPSHOT')).reply(200, sbomSummaryResponsePayload);
    renderPage();

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    expect(screen.getByRole('heading', { name: /Alice/ })).toBeVisible();

    const sbomImportedDate = screen.getByTestId('bill-of-materials-page-sbom-imported-date');
    expect(sbomImportedDate).toHaveTextContent(`Imported:${createdAt}`);

    expect(screen.getByRole('button', { name: 'Download' })).toBeVisible();
    expect(screen.getByText('Components')).toBeVisible();

    const field = await screen.findByRole('button', { name: /Viewing:/i });
    expect(field).toHaveTextContent('Viewing: 1.0-SNAPSHOT');
  });

  it('renders Bill of Material Summary Tile values correctly', async () => {
    axiosMock.onGet(getApplicationSummaryUrl(applicationPublicId)).reply(200, {
      id: internalAppId,
      name: 'Alice',
    });
    axiosMock
      .onGet(getAllApplicationSbomVersions(internalAppId))
      .reply(200, ['1.0-SNAPSHOT', '1.1-SNAPSHOT', '1.2-SNAPSHOT']);
    axiosMock.onGet(getSbomMetadataUrl(internalAppId, '1.0-SNAPSHOT')).reply(200, sbomMetadataResponsePayload);
    axiosMock.onGet(getSbomSummaryUrl(internalAppId, '1.0-SNAPSHOT')).reply(200, sbomSummaryResponsePayload);

    renderPage();

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    const pieChartTotals = screen.getAllByTestId('pie-chart-total');
    expect(pieChartTotals[0]).toHaveTextContent('5,678');
    expect(pieChartTotals[1]).toHaveTextContent('1,234');

    expect(screen.getByText(/5,000 Direct/)).toBeVisible();
    expect(screen.getByText(/600 Transitive/)).toBeVisible();
    expect(screen.getByText(/78 Unspecified/)).toBeVisible();

    expect(screen.getByText(/1,000 Low/)).toBeVisible();
    expect(screen.getByText(/200 Medium/)).toBeVisible();
    expect(screen.getByText(/30 High/)).toBeVisible();
    expect(screen.getByText(/4 Critical/)).toBeVisible();

    const description = screen.getByTestId('annotated-vulnerabilities-summary-description');
    expect(description).toHaveTextContent('75% of vulnerabilities annotated with exploitability information');
  });

  it('renders Bill of Material Summary Tile values correctly when there are no vulnerabilities', async () => {
    axiosMock.onGet(getApplicationSummaryUrl(applicationPublicId)).reply(200, {
      id: internalAppId,
      name: 'Alice',
    });
    axiosMock
      .onGet(getAllApplicationSbomVersions(internalAppId))
      .reply(200, ['1.0-SNAPSHOT', '1.1-SNAPSHOT', '1.2-SNAPSHOT']);
    axiosMock.onGet(getSbomMetadataUrl(internalAppId, '1.0-SNAPSHOT')).reply(200, sbomMetadataResponsePayload);
    axiosMock
      .onGet(getSbomSummaryUrl(internalAppId, '1.0-SNAPSHOT'))
      .reply(200, sbomSummaryNoVulnerabilitiesResponsePayload);

    renderPage();

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    const pieChartTotals = screen.getAllByTestId('pie-chart-total');
    expect(pieChartTotals[0]).toHaveTextContent('5,678');
    expect(pieChartTotals[1]).toHaveTextContent('0');

    expect(screen.getByText(/5,000 Direct/)).toBeVisible();
    expect(screen.getByText(/600 Transitive/)).toBeVisible();
    expect(screen.getByText(/78 Unspecified/)).toBeVisible();

    expect(screen.getByText(/0 Low/)).toBeVisible();
    expect(screen.getByText(/0 Medium/)).toBeVisible();
    expect(screen.getByText(/0 High/)).toBeVisible();
    expect(screen.getByText(/0 Critical/)).toBeVisible();

    const description = screen.getByTestId('annotated-vulnerabilities-summary-description');
    expect(description).toHaveTextContent('No vulnerabilities to annotate');
  });

  it('renders Bill of Material Summary Tile values correctly when there are no components', async () => {
    axiosMock.onGet(getApplicationSummaryUrl(applicationPublicId)).reply(200, {
      id: internalAppId,
      name: 'Alice',
    });
    axiosMock
      .onGet(getAllApplicationSbomVersions(internalAppId))
      .reply(200, ['1.0-SNAPSHOT', '1.1-SNAPSHOT', '1.2-SNAPSHOT']);
    axiosMock.onGet(getSbomMetadataUrl(internalAppId, '1.0-SNAPSHOT')).reply(200, sbomMetadataResponsePayload);
    axiosMock
      .onGet(getSbomSummaryUrl(internalAppId, '1.0-SNAPSHOT'))
      .reply(200, sbomSummaryNoComponentsResponsePayload);

    renderPage();

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    const pieChartTotals = screen.getAllByTestId('pie-chart-total');
    expect(pieChartTotals[0]).toHaveTextContent('0');
    expect(pieChartTotals[1]).toHaveTextContent('0');

    expect(screen.getByText(/0 Direct/)).toBeVisible();
    expect(screen.getByText(/0 Transitive/)).toBeVisible();
    expect(screen.getByText(/0 Unspecified/)).toBeVisible();

    expect(screen.getByText(/0 Low/)).toBeVisible();
    expect(screen.getByText(/0 Medium/)).toBeVisible();
    expect(screen.getByText(/0 High/)).toBeVisible();
    expect(screen.getByText(/0 Critical/)).toBeVisible();

    const description = screen.getByTestId('annotated-vulnerabilities-summary-description');
    expect(description).toHaveTextContent('No vulnerabilities to annotate');
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
