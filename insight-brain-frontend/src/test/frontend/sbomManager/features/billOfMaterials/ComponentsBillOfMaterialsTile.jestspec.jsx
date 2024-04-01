/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { axiosMockAdapter, render, screen, waitFor, fireEvent, within } from 'TestRoot/SpecUtil';
import ComponentsBillOfMaterialsTile from 'MainRoot/sbomManager/features/componentsTile/ComponentsBillOfMaterialsTile.jsx';
// import * as routerContext from 'MainRoot/react/RouterStateContext';
import { getBillsOfMaterialsComponents } from 'MainRoot/util/CLMLocation';

describe('ComponentsBillofMaterialsTile', () => {
  let axiosMock, initialProps, initialState;
  const internalAppId = 'abc123';
  const sbomVersion = '1.0-SNAPSHOT_TEST';
  const renderComponent = (props, preloadedState) =>
    render(<ComponentsBillOfMaterialsTile {...props} />, { preloadedState });

  beforeEach(() => {
    axiosMock = axiosMockAdapter();

    initialProps = {
      internalAppId: internalAppId,
      sbomVersion: sbomVersion,
      isInternalAppIdLoading: false,
    };

    initialState = {
      componentsBillOfMaterialsTile: {
        results: null,
        loading: false,
        error: null,
        sortDir: 'asc',
      },
    };

    axiosMock.onGet(getBillsOfMaterialsComponents(internalAppId, sbomVersion)).reply(200, [
      {
        hash: '12345',
        packageUrl: 'pkg:a',
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            artifactId: 'foo',
            extension: 'foo',
            groupId: 'foo',
            version: '1',
          },
        },
        displayName: 'a1',
        licenses: [
          {
            licenseId: 'Apache-2.0',
            licenseName: 'Apache-2.0',
          },
        ],
        vulnerabilitySeverityNoneCount: 0,
        vulnerabilitySeverityLowCount: 1,
        vulnerabilitySeverityMediumCount: 2,
        vulnerabilitySeverityHighCount: 3,
        vulnerabilitySeverityCriticalCount: 4,
      },
      {
        hash: '67890',
        packageUrl: 'pkg:z',
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            artifactId: 'bar',
            extension: 'bar',
            groupId: 'bar',
            version: '2',
          },
        },
        displayName: 'z2',
        licenses: [
          {
            licenseId: 'Apache-2.0',
            licenseName: 'Apache-2.0',
          },
        ],
        vulnerabilitySeverityNoneCount: 0,
        vulnerabilitySeverityLowCount: 5,
        vulnerabilitySeverityMediumCount: 6,
        vulnerabilitySeverityHighCount: 7,
        vulnerabilitySeverityCriticalCount: 8,
      },
    ]);
  });

  it("renders it's title", async () => {
    renderComponent(initialProps, initialState);
    await waitFor(() => expect(screen.queryByText('Loading')).toBeNull());
    expect(screen.getByText('Components')).toBeVisible();
  });

  it('renders the loading error if an error happens', async () => {
    axiosMock
      .onGet(getBillsOfMaterialsComponents(internalAppId, sbomVersion))
      .reply(() => Promise.reject({ response: { data: 'Error' } }));
    renderComponent(initialProps, initialState);
    const errorAlert = await screen.findByRole('alert');
    expect(errorAlert).toBeVisible();
    expect(errorAlert).toHaveTextContent('An error occurred loading data.');
  });

  describe('has a table that', () => {
    it('renders the loading spinner when loading', () => {
      renderComponent(initialProps, initialState);
      expect(screen.getByText('Loading…')).toBeVisible();
    });

    it('renders a component row correctly', async () => {
      renderComponent(initialProps, initialState);
      await waitFor(() => expect(screen.queryByText('Loading')).toBeNull());
      const tableRows = await screen.findAllByRole('row');
      const firstRow = tableRows[1];
      const rowCells = within(firstRow).getAllByRole('cell');

      expect(rowCells[0]).toHaveTextContent('a1');
      /* The text content for each severity pill is contained in 3 divs:
      One div with the severity text
      One div for the actual severity
      One div with the overflow text in this case is sett at 100 so the overflow text is 99+
      The expected text of the vulnerabilities cell is the combinations pf all of this */
      expect(rowCells[1]).toHaveTextContent('Critical499+Severe399+Moderate299+Low199+');
      expect(rowCells[2]).toHaveTextContent('Apache-2.0');
    });

    it('renders the correct number of rows', async () => {
      renderComponent(initialProps, initialState);
      await waitFor(() => expect(screen.queryByText('Loading')).toBeNull());
      const tableRows = await screen.findAllByRole('row');
      expect(tableRows.length).toBe(3); // Including the header
    });

    it('sorts the component rows correctly', async () => {
      renderComponent(initialProps, initialState);
      await waitFor(() => expect(screen.queryByText('Loading')).toBeNull());
      const nameHeader = await screen.findByRole('columnheader', { name: /Name/i });
      expect(nameHeader).toBeInTheDocument();

      //Descending
      fireEvent.click(nameHeader);
      const tableRows = await screen.findAllByRole('row');
      const firstRow = tableRows[1];
      const rowCells = within(firstRow).getAllByRole('cell');
      expect(rowCells[0]).toHaveTextContent('z2');

      //Ascending
      fireEvent.click(nameHeader);
      const tableRowsAfterSecondClick = await screen.findAllByRole('row');
      const firstRowAfterSecondClick = tableRowsAfterSecondClick[1];
      const rowCellsAfterSecondClick = within(firstRowAfterSecondClick).getAllByRole('cell');
      expect(rowCellsAfterSecondClick[0]).toHaveTextContent('a1');
    });
  });
});
