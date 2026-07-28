/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import moment from 'moment';

import { axiosMockAdapter, screen, render, waitFor, fireEvent, within } from 'TestRoot/SpecUtil';
import userEvent from '@testing-library/user-event';

import {
  getApplicationSummaryUrl,
  getAllApplicationSbomVersions,
  getSbomMetadataUrl,
  getSbomSummaryUrl,
  getBillOfMaterialsComponentsUrl,
  getSbomDownloadPdfUrl,
  getDownloadSbomFileUrl,
} from 'MainRoot/util/CLMLocation';
import BillOfMaterials from 'MainRoot/sbomManager/features/billOfMaterials/BillOfMaterials';
import { initialState as billOfMaterialsPageInitialState } from 'MainRoot/sbomManager/features/billOfMaterials/billOfMaterialsSlice';
import {
  initialState as billOfMaterialsComponentsTileInitialState,
  COMPONENTS_PER_PAGE,
  SORT_BY_FIELDS,
} from 'MainRoot/sbomManager/features/billOfMaterials/billOfMaterialsComponentsTile/billOfMaterialsComponentsTileSlice';

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
    isValid: true,
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
    releaseStatusPercentage: 75,
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
    releaseStatusPercentage: null,
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
    releaseStatusPercentage: null,
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

    renderPage();

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
  });

  it('renders correct export options for a valid SBOM', async () => {
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

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    // Check export options
    const exportButton = screen.getByRole('button', { name: 'Export SBOM' });
    expect(exportButton).toBeVisible();
    expect(exportButton).not.toBeDisabled();
    const exportDropdown = screen.getByRole('button', { name: 'more options' });
    fireEvent.click(exportDropdown);
    const exportOriginalButton = screen.getByRole('button', { name: 'Export Original SBOM' });
    expect(exportOriginalButton).toBeVisible();
    expect(exportOriginalButton).not.toBeDisabled();
    const additionalExportOptionsButton = screen.getByRole('button', { name: 'Additional Export Options' });
    expect(additionalExportOptionsButton).toBeVisible();
    expect(additionalExportOptionsButton).not.toBeDisabled();
    const exportPdfLink = screen.getByRole('link', { name: 'Export PDF' });
    expect(exportPdfLink).toBeVisible();
    expect(exportPdfLink).not.toHaveClass('disabled');
    expect(exportPdfLink.getAttribute('href')).toBe(getSbomDownloadPdfUrl(APPLICATION_PUBLIC_ID, SBOM_VERSION));
  });

  it('renders correct export options for an invalid SBOM', async () => {
    const user = userEvent.setup();
    axiosMock.onGet(getApplicationSummaryUrl(APPLICATION_PUBLIC_ID)).reply(200, getApplicationSummaryResponsePayload);
    axiosMock
      .onGet(getAllApplicationSbomVersions(APPLICATION_INTERNAL_ID))
      .reply(200, getAllApplicationSbomVersionsResponsePayload);
    axiosMock
      .onGet(getSbomMetadataUrl(APPLICATION_INTERNAL_ID, SBOM_VERSION))
      .reply(200, { ...getSbomMetadataResponsePayload, isValid: false });
    axiosMock.onGet(getSbomSummaryUrl(APPLICATION_INTERNAL_ID, SBOM_VERSION)).reply(200, getSbomSummaryResponsePayload);
    axiosMock
      .onGet(getBillOfMaterialsComponentsUrl(...getBillOfMaterialsComponentsParams))
      .reply(200, getBillOfMaterialsComponentsResponsePayload);

    renderPage();

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    // Check export options
    const exportOriginalButton = screen.getByRole('button', { name: 'Export Original SBOM' });
    expect(exportOriginalButton).toBeVisible();
    expect(exportOriginalButton).not.toBeDisabled();
    const exportDropdown = screen.getByRole('button', { name: 'more options' });
    await user.click(exportDropdown);

    const exportButton = screen.getByRole('button', { name: 'Export SBOM' });
    expect(exportButton).toBeVisible();
    expect(exportButton).toBeDisabled();

    const additionalExportOptionsButton = screen.getByRole('button', { name: 'Additional Export Options' });
    expect(additionalExportOptionsButton).toBeVisible();
    expect(additionalExportOptionsButton).toBeDisabled();

    const exportPdfLink = screen.getByRole('link', { name: 'Export PDF' });
    expect(exportPdfLink).toBeVisible();
    expect(exportPdfLink).toHaveClass('disabled');
    expect(exportPdfLink.getAttribute('href')).toBeNull();

    await user.hover(additionalExportOptionsButton);
    let tooltip = await screen.findByRole('tooltip');
    expect(tooltip).toHaveTextContent('Additional Export Options disabled due to validation errors.');
    await user.unhover(additionalExportOptionsButton);

    await user.hover(exportPdfLink);
    tooltip = await screen.findByRole('tooltip');
    expect(tooltip).toHaveTextContent('Export PDF is disabled due to validation errors.');
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
    expect(pieChartTotals[2]).toHaveTextContent('75%');
    expect(pieChartTotals[3]).toHaveTextContent('12,221');

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

    const description = screen.getByTestId('summary-tile-release-status-description');
    expect(description).toHaveTextContent(
      '75% of critical and high vulnerabilities have been annotated with exploitability information'
    );
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

    const description = screen.getByTestId('summary-tile-release-status-description');
    expect(description).toHaveTextContent(
      '0% of critical and high vulnerabilities have been annotated with exploitability information'
    );
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

    const description = screen.getByTestId('summary-tile-release-status-description');
    expect(description).toHaveTextContent(
      '0% of critical and high vulnerabilities have been annotated with exploitability information'
    );
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

  describe('Invalid SBOM alert', function () {
    it('does not appear when the SBOM is valid', async () => {
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
        .reply(200, getSbomSummaryResponsePayload);
      axiosMock
        .onGet(getBillOfMaterialsComponentsUrl(...getBillOfMaterialsComponentsParams))
        .reply(200, getBillOfMaterialsComponentsResponsePayload);

      renderPage();

      jest.advanceTimersByTime(JEST_TIMER);
      jest.useRealTimers();

      await waitFor(() => expect(screen.queryByRole('status', { name: 'Loading…' })).not.toBeInTheDocument());

      expect(screen.queryByRole('status', { name: 'Invalid SBOM Detected' })).not.toBeInTheDocument();
    });

    it('is visible on load when the SBOM is invalid', async () => {
      jest.useFakeTimers();

      axiosMock.onGet(getApplicationSummaryUrl(APPLICATION_PUBLIC_ID)).reply(200, getApplicationSummaryResponsePayload);
      axiosMock
        .onGet(getAllApplicationSbomVersions(APPLICATION_INTERNAL_ID))
        .reply(200, getAllApplicationSbomVersionsResponsePayload);
      axiosMock
        .onGet(getSbomSummaryUrl(APPLICATION_INTERNAL_ID, SBOM_VERSION))
        .reply(200, getSbomSummaryResponsePayload);
      axiosMock
        .onGet(getBillOfMaterialsComponentsUrl(...getBillOfMaterialsComponentsParams))
        .reply(200, getBillOfMaterialsComponentsResponsePayload);

      axiosMock
        .onGet(getSbomMetadataUrl(APPLICATION_INTERNAL_ID, SBOM_VERSION))
        .reply(200, { ...getSbomMetadataResponsePayload, isValid: false });

      renderPage();

      jest.advanceTimersByTime(JEST_TIMER);
      jest.useRealTimers();

      await waitFor(() => expect(screen.queryByRole('status', { name: 'Loading…' })).not.toBeInTheDocument());

      const alert = screen.getByRole('status', { name: 'Invalid SBOM Detected' });
      expect(alert).toBeInTheDocument();
      expect(alert).toHaveTextContent('Invalid SBOM Detected');
    });

    it('contains a close button which makes it disappear', async () => {
      const user = userEvent.setup();
      jest.useFakeTimers();

      axiosMock.onGet(getApplicationSummaryUrl(APPLICATION_PUBLIC_ID)).reply(200, getApplicationSummaryResponsePayload);
      axiosMock
        .onGet(getAllApplicationSbomVersions(APPLICATION_INTERNAL_ID))
        .reply(200, getAllApplicationSbomVersionsResponsePayload);
      axiosMock
        .onGet(getSbomSummaryUrl(APPLICATION_INTERNAL_ID, SBOM_VERSION))
        .reply(200, getSbomSummaryResponsePayload);
      axiosMock
        .onGet(getBillOfMaterialsComponentsUrl(...getBillOfMaterialsComponentsParams))
        .reply(200, getBillOfMaterialsComponentsResponsePayload);

      axiosMock
        .onGet(getSbomMetadataUrl(APPLICATION_INTERNAL_ID, SBOM_VERSION))
        .reply(200, { ...getSbomMetadataResponsePayload, isValid: false });

      renderPage();

      jest.advanceTimersByTime(JEST_TIMER);
      jest.useRealTimers();

      await waitFor(() => expect(screen.queryByRole('status', { name: 'Loading…' })).not.toBeInTheDocument());

      const alert = screen.getByRole('status', { name: 'Invalid SBOM Detected' }),
        closeAlert = within(alert).getByRole('button', { name: 'Close' });

      expect(closeAlert).toBeInTheDocument();

      await user.click(closeAlert);

      expect(closeAlert).not.toBeInTheDocument();
    });
  });

  describe('Invalid SBOM indicator', function () {
    // TODO fill in when a11y name for indicator is determined
    it('does not appear when the SBOM is valid', async () => {
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
        .reply(200, getSbomSummaryResponsePayload);
      axiosMock
        .onGet(getBillOfMaterialsComponentsUrl(...getBillOfMaterialsComponentsParams))
        .reply(200, getBillOfMaterialsComponentsResponsePayload);

      renderPage();

      jest.advanceTimersByTime(JEST_TIMER);
      jest.useRealTimers();

      await waitFor(() => expect(screen.queryByRole('status', { name: 'Loading…' })).not.toBeInTheDocument());

      expect(
        screen.queryByTitle('This SBOM has validation errors which may result in partial or incorrect information.')
      ).not.toBeInTheDocument();
    });

    it('is not visible on load when the SBOM is invalid', async () => {
      jest.useFakeTimers();

      axiosMock.onGet(getApplicationSummaryUrl(APPLICATION_PUBLIC_ID)).reply(200, getApplicationSummaryResponsePayload);
      axiosMock
        .onGet(getAllApplicationSbomVersions(APPLICATION_INTERNAL_ID))
        .reply(200, getAllApplicationSbomVersionsResponsePayload);
      axiosMock
        .onGet(getSbomSummaryUrl(APPLICATION_INTERNAL_ID, SBOM_VERSION))
        .reply(200, getSbomSummaryResponsePayload);
      axiosMock
        .onGet(getBillOfMaterialsComponentsUrl(...getBillOfMaterialsComponentsParams))
        .reply(200, getBillOfMaterialsComponentsResponsePayload);

      axiosMock
        .onGet(getSbomMetadataUrl(APPLICATION_INTERNAL_ID, SBOM_VERSION))
        .reply(200, { ...getSbomMetadataResponsePayload, isValid: false });

      renderPage();

      jest.advanceTimersByTime(JEST_TIMER);
      jest.useRealTimers();

      await waitFor(() => expect(screen.queryByRole('status', { name: 'Loading…' })).not.toBeInTheDocument());

      expect(
        screen.queryByTitle('This SBOM has validation errors which may result in partial or incorrect information.')
      ).not.toBeInTheDocument();
    });

    it('appears when the Invalid SBOM alert is closed', async () => {
      const user = userEvent.setup();
      jest.useFakeTimers();

      axiosMock.onGet(getApplicationSummaryUrl(APPLICATION_PUBLIC_ID)).reply(200, getApplicationSummaryResponsePayload);
      axiosMock
        .onGet(getAllApplicationSbomVersions(APPLICATION_INTERNAL_ID))
        .reply(200, getAllApplicationSbomVersionsResponsePayload);
      axiosMock
        .onGet(getSbomSummaryUrl(APPLICATION_INTERNAL_ID, SBOM_VERSION))
        .reply(200, getSbomSummaryResponsePayload);
      axiosMock
        .onGet(getBillOfMaterialsComponentsUrl(...getBillOfMaterialsComponentsParams))
        .reply(200, getBillOfMaterialsComponentsResponsePayload);

      axiosMock
        .onGet(getSbomMetadataUrl(APPLICATION_INTERNAL_ID, SBOM_VERSION))
        .reply(200, { ...getSbomMetadataResponsePayload, isValid: false });

      renderPage();

      jest.advanceTimersByTime(JEST_TIMER);
      jest.useRealTimers();

      await waitFor(() => expect(screen.queryByRole('status', { name: 'Loading…' })).not.toBeInTheDocument());

      const alert = screen.getByRole('status', { name: 'Invalid SBOM Detected' });

      await user.click(within(alert).getByRole('button', { name: 'Close' }));

      expect(
        screen.getByRole('img', {
          name: 'This SBOM has validation errors which may result in partial or incorrect information.',
        })
      ).toBeInTheDocument();
    });
  });

  it('exports SPDX 3.0 source SBOM with spdx3.0 specification instead of spdx2.3', async () => {
    const spdx30Metadata = {
      ...getSbomMetadataResponsePayload,
      specification: 'SPDX',
      specVersion: '3.0',
      fileFormat: 'json',
    };

    axiosMock.onGet(getApplicationSummaryUrl(APPLICATION_PUBLIC_ID)).reply(200, getApplicationSummaryResponsePayload);
    axiosMock
      .onGet(getAllApplicationSbomVersions(APPLICATION_INTERNAL_ID))
      .reply(200, getAllApplicationSbomVersionsResponsePayload);
    axiosMock.onGet(getSbomMetadataUrl(APPLICATION_INTERNAL_ID, SBOM_VERSION)).reply(200, spdx30Metadata);
    axiosMock.onGet(getSbomSummaryUrl(APPLICATION_INTERNAL_ID, SBOM_VERSION)).reply(200, getSbomSummaryResponsePayload);
    axiosMock
      .onGet(getBillOfMaterialsComponentsUrl(...getBillOfMaterialsComponentsParams))
      .reply(200, getBillOfMaterialsComponentsResponsePayload);

    const expectedUrl = getDownloadSbomFileUrl(APPLICATION_INTERNAL_ID, SBOM_VERSION, 'current', 'spdx3.0');
    axiosMock.onGet(expectedUrl).reply(200, new Blob(['test']));

    const user = userEvent.setup();
    renderPage();

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    const exportButton = screen.getByRole('button', { name: 'Export SBOM' });
    await user.click(exportButton);

    await waitFor(() => {
      const getRequests = axiosMock.history.get;
      const exportRequest = getRequests.find((req) => req.url === expectedUrl);
      expect(exportRequest).toBeDefined();
    });
  });

  it('exports a CycloneDX source SBOM with the cyclonedx1.7 specification', async () => {
    const cycloneDxMetadata = {
      ...getSbomMetadataResponsePayload,
      specification: 'CycloneDx',
      specVersion: '1.6',
      fileFormat: 'json',
    };

    axiosMock.onGet(getApplicationSummaryUrl(APPLICATION_PUBLIC_ID)).reply(200, getApplicationSummaryResponsePayload);
    axiosMock
      .onGet(getAllApplicationSbomVersions(APPLICATION_INTERNAL_ID))
      .reply(200, getAllApplicationSbomVersionsResponsePayload);
    axiosMock.onGet(getSbomMetadataUrl(APPLICATION_INTERNAL_ID, SBOM_VERSION)).reply(200, cycloneDxMetadata);
    axiosMock.onGet(getSbomSummaryUrl(APPLICATION_INTERNAL_ID, SBOM_VERSION)).reply(200, getSbomSummaryResponsePayload);
    axiosMock
      .onGet(getBillOfMaterialsComponentsUrl(...getBillOfMaterialsComponentsParams))
      .reply(200, getBillOfMaterialsComponentsResponsePayload);

    const expectedUrl = getDownloadSbomFileUrl(APPLICATION_INTERNAL_ID, SBOM_VERSION, 'current', 'cyclonedx1.7');
    axiosMock.onGet(expectedUrl).reply(200, new Blob(['test']));

    const user = userEvent.setup();
    renderPage();

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    const exportButton = screen.getByRole('button', { name: 'Export SBOM' });
    await user.click(exportButton);

    await waitFor(() => {
      const getRequests = axiosMock.history.get;
      const exportRequest = getRequests.find((req) => req.url === expectedUrl);
      expect(exportRequest).toBeDefined();
    });
  });

  it('exports an SPDX 2.x source SBOM with the spdx2.3 specification', async () => {
    const spdx23Metadata = {
      ...getSbomMetadataResponsePayload,
      specification: 'SPDX',
      specVersion: '2.3',
      fileFormat: 'json',
    };

    axiosMock.onGet(getApplicationSummaryUrl(APPLICATION_PUBLIC_ID)).reply(200, getApplicationSummaryResponsePayload);
    axiosMock
      .onGet(getAllApplicationSbomVersions(APPLICATION_INTERNAL_ID))
      .reply(200, getAllApplicationSbomVersionsResponsePayload);
    axiosMock.onGet(getSbomMetadataUrl(APPLICATION_INTERNAL_ID, SBOM_VERSION)).reply(200, spdx23Metadata);
    axiosMock.onGet(getSbomSummaryUrl(APPLICATION_INTERNAL_ID, SBOM_VERSION)).reply(200, getSbomSummaryResponsePayload);
    axiosMock
      .onGet(getBillOfMaterialsComponentsUrl(...getBillOfMaterialsComponentsParams))
      .reply(200, getBillOfMaterialsComponentsResponsePayload);

    const expectedUrl = getDownloadSbomFileUrl(APPLICATION_INTERNAL_ID, SBOM_VERSION, 'current', 'spdx2.3');
    axiosMock.onGet(expectedUrl).reply(200, new Blob(['test']));

    const user = userEvent.setup();
    renderPage();

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    const exportButton = screen.getByRole('button', { name: 'Export SBOM' });
    await user.click(exportButton);

    await waitFor(() => {
      const getRequests = axiosMock.history.get;
      const exportRequest = getRequests.find((req) => req.url === expectedUrl);
      expect(exportRequest).toBeDefined();
    });
  });
});
