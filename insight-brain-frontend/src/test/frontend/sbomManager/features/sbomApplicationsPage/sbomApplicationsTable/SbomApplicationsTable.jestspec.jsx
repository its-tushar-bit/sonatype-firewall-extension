/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { update } from 'ramda';
import { axiosMockAdapter, fireEvent, render, screen, waitFor, within } from 'TestRoot/SpecUtil';

import SbomApplicationsTable from 'MainRoot/sbomManager/features/sbomApplicationsPage/sbomApplicationsTable/SbomApplicationsTable';
import { getSbomApplicationsUrl } from 'MainRoot/util/CLMLocation';

import {
  APPLICATIONS_PER_PAGE,
  SORT_BY_FIELDS,
  SORT_DIRECTION,
} from 'MainRoot/sbomManager/features/sbomApplicationsPage/sbomApplicationsTable/sbomApplicationsTableSlice';

describe('SbomApplicationsTable', () => {
  let axiosMock;

  const baseUrlParams = [1, APPLICATIONS_PER_PAGE, SORT_BY_FIELDS.importDate, false, null];

  const mockApplication = (count) => ({
    applicationInternalId: `app-internal-id-${count}`,
    applicationPublicId: `app-public-id-${count}`,
    sbomVersion: `sbom-version-${count}`,
    applicationName: `app-name-${count}`,
    importDate: '2024-01-01T00:00:00+0000',
    vulnerabilitySummary: {
      none: 1 + count,
      low: 2 + count,
      medium: 3 + count,
      high: 4 + count,
      critical: 5 + count,
    },
    policyViolationSummary: {
      low: 111 + count,
      moderate: 222 + count,
      severe: 333 + count,
      critical: 444 + count,
    },
    releaseStatusPercentage: count,
  });

  const generateResponse = (totalCount = 2) => ({
    applications: [mockApplication(0), mockApplication(1)],
    totalCount,
  });

  const initialState = Object.freeze({
    sbomApplicationsPage: {
      sbomApplicationsTable: {
        loading: false,
        errorMessage: null,
        applications: [],
        applicationsTotalCount: 0,
        sortConfiguration: { sortBy: SORT_BY_FIELDS.importDate, sortDirection: SORT_DIRECTION.DESC },
        pagination: { pageCount: 1, currentPage: 0 },
        applicationNameRawFilterTerm: '',
      },
    },
  });

  const renderComponent = (preloadedState) => render(<SbomApplicationsTable />, { preloadedState });

  beforeEach(() => {
    axiosMock = axiosMockAdapter();
  });

  it('renders a table', async () => {
    axiosMock.onGet(getSbomApplicationsUrl(...baseUrlParams)).reply(200, generateResponse());

    renderComponent(initialState);

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    expect(screen.getByTestId('sbom-manager-applications-table')).toBeVisible();
  });

  it('renders the correct number of applications and content', async () => {
    axiosMock.onGet(getSbomApplicationsUrl(...baseUrlParams)).reply(200, generateResponse());

    renderComponent(initialState);

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    const tableRows = await screen.findAllByRole('row');

    // +1 including the header and filter
    expect(tableRows.length).toBe(4);

    const firstRow = tableRows[2];
    const firstRowCells = within(firstRow).getAllByRole('cell');
    expect(firstRowCells[0]).toHaveTextContent('app-name-0');
    expect(firstRowCells[1]).toHaveTextContent('sbom-version-0');
    expect(firstRowCells[2]).toHaveTextContent(/0%/);
    // The text content for each severity pill is contained in 3 divs:
    // • One div with the severity text
    // • One div for the actual severity
    // • One div with the overflow text in this case is set at 100 so the overflow text is 99+
    // The expected text of the vulnerabilities cell is the combinations of all of these.
    expect(firstRowCells[4]).toHaveTextContent('Critical5999+High4999+Medium3999+');
    expect(firstRowCells[5]).toHaveTextContent('Critical444999+Severe333999+Moderate222999+');

    const secondRow = tableRows[3];
    const secondRowCells = within(secondRow).getAllByRole('cell');
    expect(secondRowCells[0]).toHaveTextContent('app-name-1');
    expect(secondRowCells[1]).toHaveTextContent('sbom-version-1');
    expect(secondRowCells[2]).toHaveTextContent(/1%/);
    expect(firstRowCells[4]).toHaveTextContent('Critical5999+High4999+Medium3999+');
    expect(secondRowCells[5]).toHaveTextContent('Critical445999+Severe334999+Moderate223999+');
  });

  describe('Pagination Status', () => {
    it('should show the correct pagination status text on first page', async () => {
      axiosMock.onGet(getSbomApplicationsUrl(...baseUrlParams)).reply(200, generateResponse(2500));

      renderComponent(initialState);

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      const paginationStatus = screen.getByTestId('sbom-applications-table-pagination-status');
      expect(paginationStatus).toBeVisible();
      expect(paginationStatus).toHaveTextContent(`Showing 50 of 2,500 applications`);
    });

    it('should show the correct pagination status text when total < maximum applications per page', async () => {
      axiosMock.onGet(getSbomApplicationsUrl(...baseUrlParams)).reply(200, generateResponse(25));

      renderComponent(initialState);

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      const paginationStatus = screen.getByTestId('sbom-applications-table-pagination-status');
      expect(paginationStatus).toBeVisible();
      expect(paginationStatus).toHaveTextContent(`Showing 25 of 25 applications`);
    });

    it('should show the correct pagination status for not first or last page', async () => {
      axiosMock.onGet(getSbomApplicationsUrl(...baseUrlParams)).reply(200, generateResponse(150));

      renderComponent(initialState);

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      axiosMock.onGet(getSbomApplicationsUrl(...update(0, 2, baseUrlParams))).reply(200, generateResponse(150));

      const nextButton = screen.getByRole('button', { name: 'goto next page' });
      expect(nextButton).toBeVisible();
      fireEvent.click(nextButton);

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      const paginationStatus = screen.getByTestId('sbom-applications-table-pagination-status');
      expect(paginationStatus).toBeVisible();

      expect(paginationStatus).toHaveTextContent(`Showing 51\u2014100 of 150 applications`);
    });

    it('should show the correct pagination status for the last page', async () => {
      axiosMock.onGet(getSbomApplicationsUrl(...baseUrlParams)).reply(200, generateResponse(100));

      renderComponent(initialState);

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      axiosMock.onGet(getSbomApplicationsUrl(...update(0, 2, baseUrlParams))).reply(200, generateResponse(100));

      const nextButton = screen.getByRole('button', { name: 'goto next page' });
      expect(nextButton).toBeVisible();
      fireEvent.click(nextButton);

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      const paginationStatus = screen.getByTestId('sbom-applications-table-pagination-status');
      expect(paginationStatus).toBeVisible();

      expect(paginationStatus).toHaveTextContent(`Showing 100 of 100 applications`);
    });
  });

  describe('Application Name Filter', () => {
    it('should render application name filter text field', async () => {
      axiosMock.onGet(getSbomApplicationsUrl(...baseUrlParams)).reply(200, generateResponse(100));

      renderComponent(initialState);

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      expect(screen.getByTestId('application-name-filter')).toBeVisible();
    });
  });
});
