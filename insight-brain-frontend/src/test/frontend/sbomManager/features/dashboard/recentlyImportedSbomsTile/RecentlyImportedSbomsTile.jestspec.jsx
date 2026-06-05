/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import { axiosMockAdapter, render, screen, waitFor, within } from 'TestRoot/SpecUtil';
import { SORT_DIRECTION } from 'MainRoot/sbomManager/features/dashboard/recentlyImportedSbomsTile/recentlyImportedSbomsTileSlice';
import RecentlyImportedSbomsTile from 'MainRoot/sbomManager/features/dashboard/recentlyImportedSbomsTile/RecentlyImportedSbomsTile';
import { getRecentlyImportedSbomsUrl } from 'MainRoot/util/CLMLocation';

describe('RecentlyImportedSbomsTile', () => {
  let renderTile;

  const initialState = Object.freeze({
    sbomManagerDashboard: {
      recentlyImportedSbomsTile: {
        loading: true,
        loadingErrorMessage: null,
        sboms: null,
        sortDirection: SORT_DIRECTION.UNSORTED,
      },
    },
  });

  const response = Object.freeze([
    {
      applicationName: 'alice',
      publicApplicationId: 'alice-id',
      sbomVersion: '1.2.3',
      specification: 'SPDX',
      importDate: '2024-01-01T00:00:00.000+00:00',
      criticalCount: 1,
      highCount: 2,
      mediumCount: 3,
      lowCount: 4,
    },
    {
      applicationName: 'bob',
      publicApplicationId: 'bob-id',
      sbomVersion: '2.3.4',
      specification: 'Cyclone DX',
      importDate: '2024-01-02T00:00:00.000+00:00',
      criticalCount: 2,
      highCount: 3,
      mediumCount: 4,
      lowCount: 5,
    },
    {
      applicationName: 'chesire',
      publicApplicationId: 'chesire-id',
      sbomVersion: '3.4.5',
      specification: 'SPDX',
      importDate: '2024-01-03T00:00:00.000+00:00',
      criticalCount: 3,
      highCount: 4,
      mediumCount: 5,
      lowCount: 6,
    },
  ]);

  beforeEach(() => {
    renderTile = (additionalPreloadedState = {}) =>
      render(<RecentlyImportedSbomsTile />, { preloadedState: { ...initialState, ...additionalPreloadedState } });
  });

  it('renders correct tile header', async () => {
    const axiosMock = axiosMockAdapter();
    axiosMock.onGet(getRecentlyImportedSbomsUrl()).reply(200, response);

    renderTile();

    expect(screen.getByRole('heading', { name: /Recently Imported SBOMs/ })).toBeVisible();
  });

  describe('Recently Imported SBOMs Table', () => {
    it('renders the loading indicator initially', () => {
      renderTile();
      expect(screen.getByText(/Loading…/)).toBeVisible();
    });

    it('renders the correct empty message', async () => {
      const axiosMock = axiosMockAdapter();
      axiosMock.onGet(getRecentlyImportedSbomsUrl()).reply(200, []);

      renderTile();

      expect(await screen.findByText(/No recently imported SBOMs./)).toBeVisible();
    });

    it('renders an error message if an error occurs', async () => {
      const axiosMock = axiosMockAdapter();
      axiosMock
        .onGet(getRecentlyImportedSbomsUrl())
        .reply(() => Promise.reject({ response: { data: 'ERROR-MESSAGE' } }));

      renderTile();

      const errorAlert = await screen.findByRole('alert');
      expect(errorAlert).toBeVisible();
      expect(errorAlert).toHaveTextContent(/An error occurred loading data. ERROR-MESSAGE/);
    });

    it('renders the correct table rows', async () => {
      const axiosMock = axiosMockAdapter();
      axiosMock.onGet(getRecentlyImportedSbomsUrl()).reply(200, response);

      renderTile();

      await waitFor(() => expect(screen.queryByText(/Loading…/)).toBeNull());

      const tableRows = await screen.findAllByRole('row');

      // +1 including the header
      expect(tableRows.length).toBe(4);

      const firstRow = tableRows[1];
      const firstRowCells = within(firstRow).getAllByRole('cell');
      expect(firstRowCells[0]).toHaveTextContent(/alice/);
      expect(firstRowCells[1]).toHaveTextContent(/1.2.3/);
      expect(firstRowCells[2]).toHaveTextContent(/SPDX/);
      expect(firstRowCells[3].textContent).toMatch(/\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}/);
      // The text content for each severity pill is contained in 3 divs:
      // • One div with the severity text
      // • One div for the actual severity
      // • One div with the overflow text in this case is set at 100 so the overflow text is 99+
      // The expected text of the vulnerabilities cell is the combinations of all of these.
      expect(firstRowCells[4]).toHaveTextContent('Critical1999+Severe2999+Moderate3999+Low4999+');

      const secondRow = tableRows[2];
      const secondRowCells = within(secondRow).getAllByRole('cell');
      expect(secondRowCells[0]).toHaveTextContent(/bob/);
      expect(secondRowCells[1]).toHaveTextContent(/2.3.4/);
      expect(secondRowCells[2]).toHaveTextContent(/Cyclone DX/);
      expect(secondRowCells[3].textContent).toMatch(/\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}/);
      expect(secondRowCells[4]).toHaveTextContent('Critical2999+Severe3999+Moderate4999+Low5999+');

      const thirdRow = tableRows[3];
      const thirdRowCells = within(thirdRow).getAllByRole('cell');
      expect(thirdRowCells[0]).toHaveTextContent(/chesire/);
      expect(thirdRowCells[1]).toHaveTextContent(/3.4.5/);
      expect(thirdRowCells[2]).toHaveTextContent(/SPDX/);
      expect(thirdRowCells[3].textContent).toMatch(/\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}/);
      expect(thirdRowCells[4]).toHaveTextContent('Critical3999+Severe4999+Moderate5999+Low6999+');
    });
  });
});
