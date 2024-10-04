/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import moment from 'moment';

import { axiosMockAdapter, screen, render, waitFor } from 'TestRoot/SpecUtil';

import {
  getApplicationSummaryUrl,
  getAllApplicationSbomVersions,
  getSbomMetadataUrl,
  getSbomSummaryUrl,
  getBillOfMaterialsComponentsUrl,
  getSbomDownloadPdfUrl,
} from 'MainRoot/util/CLMLocation';
import BillOfMaterials from 'MainRoot/sbomManager/features/billOfMaterials/BillOfMaterials';
import { initialState as billOfMaterialsPageInitialState } from 'MainRoot/sbomManager/features/billOfMaterials/billOfMaterialsSlice';
import {
  initialState as billOfMaterialsComponentsTileInitialState,
  COMPONENTS_PER_PAGE,
  SORT_BY_FIELDS,
} from 'MainRoot/sbomManager/features/billOfMaterials/billOfMaterialsComponentsTile/billOfMaterialsComponentsTileSlice';
import {
  cleanUpComponentsFilterDrawerPortalContainer,
  setupComponentsFilterDrawerPortalContainer,
} from './billOfMaterialsComponentsTile/componentsFilterDrawer/ComponentsFilterDrawer.jestspec';
import { fireEvent, queryByText } from '@testing-library/react';

describe('BillOfMaterials Page', () => {
  let axiosMock, renderPage;

  const JEST_TIMER = 1000;
  const APPLICATION_PUBLIC_ID = 'APPLICATION-PUBLIC-ID';
  const APPLICATION_INTERNAL_ID = 'APPLICATION-INTERNAL-ID';
  const SBOM_VERSION = 'SBOM-VERSION';

  const createdAt = moment(new Date('2024-01-12T20:11:22.000+00:00')).format('YYYY-MM-DD HH:mm:ss');

  const getApplicationSummaryResponsePayload = Object.freeze({
    id: APPLICATION_INTERNAL_ID,
    name: 'Alice',
  });

  const getAllApplicationSbomVersionsResponsePayload = Object.freeze([
    SBOM_VERSION,
    'another-sbom-version-1',
    'another-sbom-version-2',
  ]);

  const getSbomMetadataResponsePayload = Object.freeze({
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
  });

  const getSbomSummaryResponsePayload = Object.freeze({
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
    policyViolationSummary: {
      low: 1111,
      moderate: 2222,
      severe: 3333,
      critical: 5555,
    },
    annotatedPercentage: 75,
  });

  const getSbomSummaryNoVulnerabilitiesResponsePayload = Object.freeze({
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
    policyViolationSummary: {
      low: 1111,
      moderate: 2222,
      severe: 3333,
      critical: 5555,
    },
    annotatedPercentage: null,
  });

  const getSbomSummaryEmptyResponsePayload = Object.freeze({
    applicationVersion: '123',
    none: null,
    low: null,
    medium: null,
    high: null,
    critical: null,
    policyViolationSummary: {
      low: null,
      moderate: null,
      severe: null,
      critical: null,
    },
    dependencyType: null,
    annotatedPercentage: null,
  });

  const getBillOfMaterialsComponentsResponsePayload = Object.freeze({
    totalResultsCount: 0,
    results: [],
  });

  const getBillOfMaterialsComponentsParams = Object.freeze([
    APPLICATION_INTERNAL_ID,
    SBOM_VERSION,
    1,
    COMPONENTS_PER_PAGE,
    SORT_BY_FIELDS.vulnerabilities,
    false,
  ]);

  beforeEach(() => {
    setupComponentsFilterDrawerPortalContainer();
    axiosMock = axiosMockAdapter();

    const preloadedState = {
      productFeatures: {
        productFeatures: {
          'sbom-manager': true,
          'sbom-policies': true,
          loading: true,
        },
      },
      router: {
        currentState: { name: 'sbomManager.management.view.bom' },
        currentParams: {
          applicationPublicId: APPLICATION_PUBLIC_ID,
          internalAppId: APPLICATION_INTERNAL_ID,
          versionId: SBOM_VERSION,
        },
      },
      billOfMaterialsPage: {
        ...billOfMaterialsPageInitialState,
      },
      billOfMaterialsComponentsTile: {
        ...billOfMaterialsComponentsTileInitialState,
      },
    };

    renderPage = (additionalPreloadedState = {}) =>
      render(<BillOfMaterials />, { preloadedState: { ...preloadedState, ...additionalPreloadedState } });
  });

  afterEach(() => {
    cleanUpComponentsFilterDrawerPortalContainer();
  });

  it('renders page content', async () => {
    jest.useFakeTimers();

    axiosMock.onGet(getApplicationSummaryUrl(APPLICATION_PUBLIC_ID)).reply(200, getApplicationSummaryResponsePayload);
    axiosMock
      .onGet(getAllApplicationSbomVersions(APPLICATION_INTERNAL_ID))
      .reply(200, getAllApplicationSbomVersionsResponsePayload);
    axiosMock
      .onGet(getSbomMetadataUrl(APPLICATION_INTERNAL_ID, SBOM_VERSION))
      .reply(200, getSbomMetadataResponsePayload);
    axiosMock.onGet(getSbomSummaryUrl(APPLICATION_INTERNAL_ID, SBOM_VERSION)).reply(200, getSbomSummaryResponsePayload);
    axiosMock
      .onGet(getBillOfMaterialsComponentsUrl(...getBillOfMaterialsComponentsParams))
      .reply(200, getBillOfMaterialsComponentsResponsePayload);

    const { container } = renderPage();

    jest.advanceTimersByTime(JEST_TIMER);
    jest.useRealTimers();

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    expect(screen.getByRole('heading', { name: /Alice/ })).toBeVisible();

    const sbomImportedDate = screen.getByTestId('bill-of-materials-page-sbom-imported-date');
    expect(sbomImportedDate).toHaveTextContent(`Imported:${createdAt}`);

    expect(screen.getByRole('button', { name: 'Export SBOM' })).toBeVisible();
    expect(screen.getByText('Components')).toBeVisible();

    const field = await screen.findByRole('button', { name: /Viewing:/i });
    expect(field).toHaveTextContent(`Viewing: ${SBOM_VERSION}`);

    // Check export options
    const exportDropdown = container.querySelector('.nx-segmented-btn__dropdown-btn');
    fireEvent.click(exportDropdown);
    expect(queryByText(container, 'Export PDF')).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Export PDF' }).getAttribute('href')).toBe(
      getSbomDownloadPdfUrl(APPLICATION_PUBLIC_ID, SBOM_VERSION)
    );
  });

  it('renders SummaryTile values correctly', async () => {
    jest.useFakeTimers();

    axiosMock.onGet(getApplicationSummaryUrl(APPLICATION_PUBLIC_ID)).reply(200, getApplicationSummaryResponsePayload);
    axiosMock
      .onGet(getAllApplicationSbomVersions(APPLICATION_INTERNAL_ID))
      .reply(200, getAllApplicationSbomVersionsResponsePayload);
    axiosMock
      .onGet(getSbomMetadataUrl(APPLICATION_INTERNAL_ID, SBOM_VERSION))
      .reply(200, getSbomMetadataResponsePayload);
    axiosMock.onGet(getSbomSummaryUrl(APPLICATION_INTERNAL_ID, SBOM_VERSION)).reply(200, getSbomSummaryResponsePayload);
    axiosMock
      .onGet(getBillOfMaterialsComponentsUrl(...getBillOfMaterialsComponentsParams))
      .reply(200, getBillOfMaterialsComponentsResponsePayload);

    renderPage();

    jest.advanceTimersByTime(JEST_TIMER);
    jest.useRealTimers();

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    const pieChartTotals = screen.getAllByTestId('pie-chart-total');
    expect(pieChartTotals[0]).toHaveTextContent('5,678');
    expect(pieChartTotals[1]).toHaveTextContent('1,234');
    expect(pieChartTotals[2]).toHaveTextContent('12,221');

    expect(screen.getByText(/5,000 Direct/)).toBeVisible();
    expect(screen.getByText(/600 Transitive/)).toBeVisible();
    expect(screen.getByText(/78 Unspecified/)).toBeVisible();

    expect(screen.getByText(/1,000 Low/)).toBeVisible();
    expect(screen.getByText(/200 Medium/)).toBeVisible();
    expect(screen.getByText(/30 High/)).toBeVisible();
    expect(screen.getByText(/4 Critical/)).toBeVisible();

    expect(screen.getByText(/1,111 Low/)).toBeVisible();
    expect(screen.getByText(/2,222 Moderate/)).toBeVisible();
    expect(screen.getByText(/3,333 Severe/)).toBeVisible();
    expect(screen.getByText(/5,555 Critical/)).toBeVisible();

    const description = screen.getByTestId('annotated-vulnerabilities-summary-description');
    expect(description).toHaveTextContent('75% of vulnerabilities annotated with exploitability information');
  });

  it('renders SummaryTile values correctly when there are no vulnerabilities', async () => {
    jest.useFakeTimers();

    axiosMock.onGet(getApplicationSummaryUrl(APPLICATION_PUBLIC_ID)).reply(200, getApplicationSummaryResponsePayload);
    axiosMock
      .onGet(getAllApplicationSbomVersions(APPLICATION_INTERNAL_ID))
      .reply(200, getAllApplicationSbomVersionsResponsePayload);
    axiosMock
      .onGet(getSbomMetadataUrl(APPLICATION_INTERNAL_ID, SBOM_VERSION))
      .reply(200, getSbomMetadataResponsePayload);
    axiosMock
      .onGet(getSbomSummaryUrl(APPLICATION_INTERNAL_ID, SBOM_VERSION))
      .reply(200, getSbomSummaryNoVulnerabilitiesResponsePayload);
    axiosMock
      .onGet(getBillOfMaterialsComponentsUrl(...getBillOfMaterialsComponentsParams))
      .reply(200, getBillOfMaterialsComponentsResponsePayload);

    renderPage();

    jest.advanceTimersByTime(JEST_TIMER);
    jest.useRealTimers();

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

  it('renders SummaryTile values correctly when empty values', async () => {
    jest.useFakeTimers();

    axiosMock.onGet(getApplicationSummaryUrl(APPLICATION_PUBLIC_ID)).reply(200, getApplicationSummaryResponsePayload);
    axiosMock
      .onGet(getAllApplicationSbomVersions(APPLICATION_INTERNAL_ID))
      .reply(200, getAllApplicationSbomVersionsResponsePayload);
    axiosMock
      .onGet(getSbomMetadataUrl(APPLICATION_INTERNAL_ID, SBOM_VERSION))
      .reply(200, getSbomMetadataResponsePayload);
    axiosMock
      .onGet(getSbomSummaryUrl(APPLICATION_INTERNAL_ID, SBOM_VERSION))
      .reply(200, getSbomSummaryEmptyResponsePayload);
    axiosMock
      .onGet(getBillOfMaterialsComponentsUrl(...getBillOfMaterialsComponentsParams))
      .reply(200, getBillOfMaterialsComponentsResponsePayload);

    renderPage();

    jest.advanceTimersByTime(JEST_TIMER);
    jest.useRealTimers();

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    const pieChartTotals = screen.getAllByTestId('pie-chart-total');
    expect(pieChartTotals[0]).toHaveTextContent('0');
    expect(pieChartTotals[1]).toHaveTextContent('0');

    expect(screen.getByText(/0 Direct/)).toBeVisible();
    expect(screen.getByText(/0 Transitive/)).toBeVisible();
    expect(screen.getByText(/0 Unspecified/)).toBeVisible();

    expect(screen.getByText(/0 Medium/)).toBeVisible();
    expect(screen.getByText(/0 High/)).toBeVisible();

    const lows = screen.queryAllByText(/0 Low/);
    expect(lows.length).toBe(2);
    expect(lows[0]).toBeVisible();
    expect(lows[1]).toBeVisible();

    const criticals = screen.queryAllByText(/0 Critical/);
    expect(criticals.length).toBe(2);
    expect(criticals[0]).toBeVisible();
    expect(criticals[1]).toBeVisible();

    expect(screen.getByText(/0 Moderate/)).toBeVisible();
    expect(screen.getByText(/0 Severe/)).toBeVisible();

    const description = screen.getByTestId('annotated-vulnerabilities-summary-description');
    expect(description).toHaveTextContent('No vulnerabilities to annotate');
  });

  it('shows error when the SBOM Manager license is disabled', async () => {
    jest.useFakeTimers();

    renderPage({
      productFeatures: {
        productFeatures: {
          loading: false,
        },
      },
    });

    jest.advanceTimersByTime(JEST_TIMER);
    jest.useRealTimers();

    const errorMessage = await screen.findByText(
      'An error occurred loading data. The SBOM Manager license feature is not enabled.'
    );

    expect(errorMessage).toBeVisible();
  });

  it('shows an error when Application SBOM versions fail to load.', async () => {
    jest.useFakeTimers();

    axiosMock.onGet(getApplicationSummaryUrl(APPLICATION_PUBLIC_ID)).reply(200, getApplicationSummaryResponsePayload);
    axiosMock.onGet(getAllApplicationSbomVersions(APPLICATION_INTERNAL_ID)).reply(() =>
      Promise.reject({
        response: {
          data: 'Error Message From Server',
        },
      })
    );

    renderPage();

    jest.advanceTimersByTime(JEST_TIMER);
    jest.useRealTimers();

    const errorMessage = await screen.findByText('An error occurred loading data. Error Message From Server');
    expect(errorMessage).toBeVisible();
  });

  it('shows an error when SBOM Metadata failed to load.', async () => {
    jest.useFakeTimers();

    axiosMock.onGet(getApplicationSummaryUrl(APPLICATION_PUBLIC_ID)).reply(200, getApplicationSummaryResponsePayload);
    axiosMock
      .onGet(getAllApplicationSbomVersions(APPLICATION_INTERNAL_ID))
      .reply(200, getAllApplicationSbomVersionsResponsePayload);
    axiosMock
      .onGet(getSbomMetadataUrl(APPLICATION_INTERNAL_ID, SBOM_VERSION))
      .reply(200, getSbomMetadataResponsePayload);
    axiosMock.onGet(getSbomSummaryUrl(APPLICATION_INTERNAL_ID, SBOM_VERSION)).reply(200, getSbomSummaryResponsePayload);
    axiosMock.onGet(getSbomMetadataUrl(APPLICATION_INTERNAL_ID, SBOM_VERSION)).reply(() =>
      Promise.reject({
        response: {
          data: 'Error Message From Server',
        },
      })
    );

    renderPage();

    jest.advanceTimersByTime(JEST_TIMER);
    jest.useRealTimers();

    const errorMessage = await screen.findByText('An error occurred loading data. Error Message From Server');
    expect(errorMessage).toBeVisible();
  });
});
