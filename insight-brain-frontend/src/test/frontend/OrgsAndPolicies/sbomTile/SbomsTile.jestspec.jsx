/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { axiosMockAdapter, render, screen, waitFor, fireEvent, within } from 'TestRoot/SpecUtil';
import SbomsTile from 'MainRoot/OrgsAndPolicies/ownerSummary/sbomsTile/SbomsTile.jsx';
import * as routerContext from 'MainRoot/react/RouterStateContext';
import { getDownloadSbomFileUrl, getSbomsByApplicationUrl } from 'MainRoot/util/CLMLocation';
import moment from 'moment';
import {
  SORT_BY_FIELDS,
  defaultSortConfiguration,
} from 'MainRoot/OrgsAndPolicies/ownerSummary/sbomsTile/sbomsTileSlice';

describe('SbomsTile', () => {
  let axiosMock, initialState;
  const applicationId = 'abc123';
  const publicApplicationId = 'publicID';
  const renderComponent = (preloadedState) => render(<SbomsTile />, { preloadedState });

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
    initialState = {
      orgsAndPolicies: {
        root: {
          selectedOwner: {
            id: applicationId,
            publicId: publicApplicationId,
          },
        },
        sbomsTile: {
          sboms: null,
          sbomsTotalCount: null,
          loading: false,
          error: null,
          currentPage: 0,
          pageCount: 0,
          selectedVersionForActions: null,
          applicationId: null,
          sortConfiguration: { ...defaultSortConfiguration },
        },
      },
    };
  });

  it("renders it's title", () => {
    renderComponent(initialState);
    expect(screen.getByText('SBOMs')).toBeVisible();
  });

  it("renders it's import button", () => {
    renderComponent(initialState);
    expect(screen.getByRole('button', { name: 'Import' })).toBeVisible();
  });

  describe('has a table that', () => {
    it('renders the loading spinner when loading', () => {
      renderComponent(initialState);
      expect(screen.getByText('Loading…')).toBeVisible();
    });

    it('renders the loading error if an error happens', async () => {
      axiosMock
        .onGet(getSbomsByApplicationUrl(applicationId, 1, 10, SORT_BY_FIELDS.importDate, false))
        .reply(() => Promise.reject({ response: { data: 'Error' } }));
      renderComponent(initialState);
      const errorAlert = await screen.findByRole('alert');
      expect(errorAlert).toBeVisible();
      expect(errorAlert).toHaveTextContent('An error occurred loading data.');
    });

    it('renders the empty message when an application has no SBOMs', async () => {
      axiosMock.onGet(getSbomsByApplicationUrl(applicationId, 1, 10, SORT_BY_FIELDS.importDate, false)).reply(200, {
        applicationId: applicationId,
        results: [],
        totalResultsCount: 0,
      });
      renderComponent(initialState);
      expect(await screen.findByText('No SBOMs found')).toBeVisible();
    });

    it('renders SBOM row correctly', async () => {
      jest.spyOn(routerContext, 'useRouterState').mockReturnValue({
        href: jest.fn((stateName, stateParams) => {
          if (stateName === 'sbomManager.management.view.bom') {
            return `/application/${stateParams.applicationPublicId}/bom/${stateParams.versionId}`;
          }
          return 'otherHref';
        }),
      });

      const importDate = moment(new Date('2020-01-01T12:00:00.000+00:00')).format('YYYY-MM-DD HH:mm:ss');

      axiosMock.onGet(getSbomsByApplicationUrl(applicationId, 1, 10, SORT_BY_FIELDS.importDate, false)).reply(200, {
        applicationId: applicationId,
        results: [
          {
            applicationVersion: 'app123',
            spec: 'SPDX',
            specVersion: '2.1',
            importDate: importDate,
            none: 0,
            low: 1,
            medium: 2,
            high: 3,
            critical: 4,
            releaseStatusPercentage: 31.0,
          },
        ],
        totalResultsCount: 0,
      });
      renderComponent(initialState);
      // The table has its own loader spinner so we have to assert that it is gone before we can start querying the
      // table's content
      await waitFor(() => expect(screen.queryByText('Loading')).toBeNull());
      const tableRows = await screen.findAllByRole('row');
      const firstRow = tableRows[1];
      const rowCells = within(firstRow).getAllByRole('cell');

      expect(rowCells[0]).toHaveTextContent('app123');
      expect(rowCells[0].querySelector('a')).toHaveAttribute('href', '/application/publicID/bom/app123');
      /* The text content for each severity pill is contained in 3 divs:
      One div with the severity text
      One div for the actual severity
      One div with the overflow text in this case is sett at 100 so the overflow text is 99+
      The expected text of the vulnerabilities cell is the combinations pf all of this */
      expect(rowCells[1]).toHaveTextContent('Critical499+Severe399+Moderate299+Low199+');
      expect(rowCells[2]).toHaveTextContent(31.0);
      expect(rowCells[3]).toHaveTextContent('SPDX 2.1');
      expect(rowCells[4]).toHaveTextContent(importDate);
    });

    it('only renders the SBOM InvalidSbomIndicator on an invalid row', async () => {
      axiosMock.onGet(getSbomsByApplicationUrl(applicationId, 1, 10, SORT_BY_FIELDS.importDate, false)).reply(200, {
        applicationId: applicationId,
        results: [
          {
            applicationVersion: 'app123',
            spec: 'SPDX',
            specVersion: '2.1',
            importDate: '2020-02-01T12:00:00.000+00:00',
            none: 0,
            low: 1,
            medium: 2,
            high: 3,
            critical: 4,
            isValid: true,
            releaseStatusPercentage: 0.0,
          },
          {
            applicationVersion: 'app456',
            spec: 'SPDX',
            specVersion: '2.1',
            importDate: '2020-01-01T12:00:00.000+00:00',
            none: 0,
            low: 1,
            medium: 2,
            high: 3,
            critical: 4,
            isValid: false,
            releaseStatusPercentage: 0.0,
          },
        ],
        totalResultsCount: 0,
      });
      renderComponent(initialState);
      // The table has its own loader spinner so we have to assert that it is gone before we can start querying the
      // table's content
      await waitFor(() => expect(screen.queryByText('Loading')).toBeNull());
      const tableRows = await screen.findAllByRole('row');

      const firstRowCells = within(tableRows[1]).getAllByRole('cell');
      expect(firstRowCells[0]).toHaveTextContent('app123');
      expect(
        within(firstRowCells[0]).queryByRole('img', {
          name: 'This SBOM has validation errors which may result in partial or incorrect information.',
        })
      ).not.toBeInTheDocument();

      const secondRowCells = within(tableRows[2]).getAllByRole('cell');
      expect(secondRowCells[0]).toHaveTextContent('app456');
      expect(
        within(secondRowCells[0]).getByRole('img', {
          name: 'This SBOM has validation errors which may result in partial or incorrect information.',
        })
      ).toBeInTheDocument();
    });

    it("renders SBOM row's dropdown correctly for a valid SBOM", async () => {
      axiosMock.onGet(getSbomsByApplicationUrl(applicationId, 1, 10, SORT_BY_FIELDS.importDate, false)).reply(200, {
        applicationId: applicationId,
        results: [
          {
            applicationVersion: 'app123',
            spec: 'SPDX',
            specVersion: '2.1',
            importDate: '2020-01-01T12:00:00.000+00:00',
            none: 0,
            low: 1,
            medium: 2,
            high: 3,
            critical: 4,
            isValid: true,
            releaseStatusPercentage: 0.0,
          },
        ],
        totalResultsCount: 1,
      });
      renderComponent(initialState);
      await waitFor(() => expect(screen.queryByText('Loading')).toBeNull());
      const tableRows = await screen.findAllByRole('row');
      const firstRow = tableRows[1];
      const dropdown = within(firstRow).getByRole('button');
      fireEvent.click(dropdown);
      expect(screen.getByRole('button', { name: 'Export Original SBOM' })).toBeVisible();
      const additionalExportOptionsBtn = screen.getByRole('button', { name: 'Additional Export Options' });
      expect(additionalExportOptionsBtn).toBeVisible();
      expect(additionalExportOptionsBtn).toBeEnabled();
      const exportPdfLink = screen.getByRole('link', { name: 'Export PDF' });
      expect(exportPdfLink).toBeVisible();
      expect(exportPdfLink).toBeEnabled();
      expect(exportPdfLink.getAttribute('href')).toBe(`/rest/report/abc123/sbom/app123/printReport`);
      expect(screen.getByRole('button', { name: 'Delete SBOM' })).toBeVisible();
    });

    it("renders SBOM row's dropdown correctly for a invalid SBOM", async () => {
      axiosMock.onGet(getSbomsByApplicationUrl(applicationId, 1, 10, SORT_BY_FIELDS.importDate, false)).reply(200, {
        applicationId: applicationId,
        results: [
          {
            applicationVersion: 'app123',
            spec: 'SPDX',
            specVersion: '2.1',
            importDate: '2020-01-01T12:00:00.000+00:00',
            none: 0,
            low: 1,
            medium: 2,
            high: 3,
            critical: 4,
            isValid: false,
          },
        ],
        totalResultsCount: 1,
      });
      renderComponent(initialState);
      await waitFor(() => expect(screen.queryByText('Loading')).toBeNull());
      const tableRows = await screen.findAllByRole('row');
      const firstRow = tableRows[1];
      let dropdown = within(firstRow).getByRole('button');
      fireEvent.click(dropdown);

      expect(screen.getByRole('button', { name: 'Export Original SBOM' })).toBeVisible();
      const additionalExportOptionsBtn = screen.getByRole('button', { name: 'Additional Export Options' });
      expect(additionalExportOptionsBtn).toBeVisible();
      expect(additionalExportOptionsBtn).toHaveClass('disabled');

      const exportPdfLink = screen.getByRole('link', { name: 'Export PDF' });
      expect(exportPdfLink).toBeVisible();
      expect(exportPdfLink).toHaveClass('disabled');
      expect(exportPdfLink.getAttribute('href')).toBe(null);

      fireEvent.mouseOver(additionalExportOptionsBtn);
      const tooltip = await screen.findByRole('tooltip');
      expect(tooltip).toHaveTextContent('Additional Export Options disabled due to validation errors.');

      fireEvent.mouseOver(exportPdfLink);
      await waitFor(() => expect(screen.queryByText('Export PDF is disabled due to validation errors.')).toBeVisible());

      expect(screen.getByRole('button', { name: 'Delete SBOM' })).toBeVisible();
    });

    describe('when the additional export options modal is used with a valid SBOM', () => {
      beforeEach(() => {
        global.URL.createObjectURL = jest.fn(() => 'fakeResponseUrl');
        global.URL.revokeObjectURL = jest.fn();
      });

      it('downloads an SBOM', async () => {
        const downloadSbomFileUrl = getDownloadSbomFileUrl(applicationId, 'app123', 'current', 'cyclonedx1.7');
        let resolveFn = null;
        axiosMock.onGet(downloadSbomFileUrl).reply(
          () =>
            new Promise((resolve) => {
              resolveFn = resolve;
            })
        );
        axiosMock.onGet(getSbomsByApplicationUrl(applicationId, 1, 10, SORT_BY_FIELDS.importDate, false)).reply(200, {
          applicationId: applicationId,
          results: [
            {
              applicationVersion: 'app123',
              spec: 'SPDX',
              specVersion: '2.1',
              importDate: '2020-01-01T12:00:00.000+00:00',
              none: 0,
              low: 1,
              medium: 2,
              high: 3,
              critical: 4,
              isValid: true,
            },
          ],
          totalResultsCount: 1,
        });

        renderComponent(initialState);
        await waitFor(() => expect(screen.queryByText('Loading')).toBeNull());
        const tableRows = await screen.findAllByRole('row');
        const dropdown = within(tableRows[1]).getByRole('button');
        fireEvent.click(dropdown);
        fireEvent.click(screen.getByRole('button', { name: 'Additional Export Options' }));

        expect(screen.getByText(/Additional Export Options/)).toBeVisible();

        expect(screen.getByText(/SBOM Specification/)).toBeVisible();
        expect(screen.getByLabelText(/CycloneDX 1\.7/)).toBeVisible();
        expect(screen.getByLabelText(/CycloneDX 1\.7/)).toBeChecked();
        expect(screen.getByLabelText(/CycloneDX 1\.6/)).toBeVisible();
        expect(screen.getByLabelText(/SPDX 2\.3/)).toBeVisible();
        expect(screen.getByLabelText(/SPDX 3\.0/)).toBeVisible();

        expect(screen.getByText(/SBOM Format/)).toBeVisible();
        expect(screen.getByLabelText(/JSON/)).toBeVisible();
        expect(screen.getByLabelText(/JSON/)).toBeChecked();
        expect(screen.getByLabelText(/XML/)).toBeVisible();

        expect(screen.getByRole('button', { name: /Cancel/ })).toBeVisible();

        fireEvent.click(screen.getByRole('button', { name: /Export SBOM/ }));
        expect(screen.getByText(/SBOM export in progress…/)).toBeVisible();

        resolveFn([200, { data: {} }]);
        await waitFor(() => expect(screen.queryByText('SBOM export completed successfully!')).toBeVisible());

        expect(axiosMock.history.get[1].url).toBe(downloadSbomFileUrl);
        expect(axiosMock.history.get[0].headers).toHaveProperty('Accept', 'application/json, text/plain, */*');
        expect(global.URL.createObjectURL).toHaveBeenCalledTimes(1);
        expect(global.URL.revokeObjectURL).toHaveBeenCalledTimes(1);
      });

      afterEach(() => {
        delete global.URL.createObjectURL;
        delete global.URL.revokeObjectURL;
      });
    });

    it('renders the correct amount of rows', async () => {
      axiosMock.onGet(getSbomsByApplicationUrl(applicationId, 1, 10, SORT_BY_FIELDS.importDate, false)).reply(200, {
        applicationId: applicationId,
        results: [
          {
            applicationVersion: 'app1231',
            spec: 'SPDX',
            specVersion: '2.1',
            importDate: '2020-01-01T12:00:00.000+00:00',
            none: 0,
            low: 1,
            medium: 2,
            high: 3,
            critical: 4,
          },
          {
            applicationVersion: 'app1232',
            spec: 'SPDX',
            specVersion: '2.1',
            importDate: '2020-01-01T12:00:00.000+00:00',
            none: 0,
            low: 1,
            medium: 2,
            high: 3,
            critical: 4,
          },
          {
            applicationVersion: 'app1233',
            spec: 'SPDX',
            specVersion: '2.1',
            importDate: '2020-01-01T12:00:00.000+00:00',
            none: 0,
            low: 1,
            medium: 2,
            high: 3,
            critical: 4,
          },
          {
            applicationVersion: 'app1234',
            spec: 'SPDX',
            specVersion: '2.1',
            importDate: '2020-01-01T12:00:00.000+00:00',
            none: 0,
            low: 1,
            medium: 2,
            high: 3,
            critical: 4,
          },
          {
            applicationVersion: 'app1235',
            spec: 'SPDX',
            specVersion: '2.1',
            importDate: '2020-01-01T12:00:00.000+00:00',
            none: 0,
            low: 1,
            medium: 2,
            high: 3,
            critical: 4,
          },
          {
            applicationVersion: 'app1236',
            spec: 'SPDX',
            specVersion: '2.1',
            importDate: '2020-01-01T12:00:00.000+00:00',
            none: 0,
            low: 1,
            medium: 2,
            high: 3,
            critical: 4,
          },
          {
            applicationVersion: 'app1237',
            spec: 'SPDX',
            specVersion: '2.1',
            importDate: '2020-01-01T12:00:00.000+00:00',
            none: 0,
            low: 1,
            medium: 2,
            high: 3,
            critical: 4,
          },
          {
            applicationVersion: 'app1238',
            spec: 'SPDX',
            specVersion: '2.1',
            importDate: '2020-01-01T12:00:00.000+00:00',
            none: 0,
            low: 1,
            medium: 2,
            high: 3,
            critical: 4,
          },
          {
            applicationVersion: 'app1239',
            spec: 'SPDX',
            specVersion: '2.1',
            importDate: '2020-01-01T12:00:00.000+00:00',
            none: 0,
            low: 1,
            medium: 2,
            high: 3,
            critical: 4,
          },
          {
            applicationVersion: 'app12310',
            spec: 'SPDX',
            specVersion: '2.1',
            importDate: '2020-01-01T12:00:00.000+00:00',
            none: 0,
            low: 1,
            medium: 2,
            high: 3,
            critical: 4,
          },
        ],
        totalResultsCount: 0,
      });
      renderComponent(initialState);
      // The table has its own loader spinner so we have to assert that it is gone before we can start querying the table's content
      await waitFor(() => expect(screen.queryByText('Loading')).toBeNull());
      const tableRows = await screen.findAllByRole('row');
      expect(tableRows.length).toBe(11);
    });

    it('renders the correct amount pagination buttons', async () => {
      axiosMock.onGet(getSbomsByApplicationUrl(applicationId, 1, 10, SORT_BY_FIELDS.importDate, false)).reply(200, {
        applicationId: applicationId,
        results: [
          {
            applicationVersion: 'app1231',
            spec: 'SPDX',
            specVersion: '2.1',
            importDate: '2020-01-01T12:00:00.000+00:00',
            none: 0,
            low: 1,
            medium: 2,
            high: 3,
            critical: 4,
          },
        ],
        totalResultsCount: 35,
      });
      renderComponent(initialState);
      // The table has its own loader spinner so we have to assert that it is gone before we can start querying the table's content
      await waitFor(() => expect(screen.queryByText('Loading')).toBeNull());
      const pagination = await screen.findByRole('navigation');
      expect(pagination).toBeVisible();
      const paginationButtons = within(pagination).getAllByRole('button');
      expect(paginationButtons.length).toBe(5);
      expect(paginationButtons[0]).toHaveTextContent('1');
      expect(paginationButtons[0]).toHaveAttribute('aria-disabled', 'true');
      expect(paginationButtons[1]).toHaveTextContent('2');
      expect(paginationButtons[1]).toHaveAttribute('aria-disabled', 'false');
      expect(paginationButtons[2]).toHaveTextContent('3');
      expect(paginationButtons[2]).toHaveAttribute('aria-disabled', 'false');
      expect(paginationButtons[3]).toHaveTextContent('4');
      expect(paginationButtons[3]).toHaveAttribute('aria-disabled', 'false');
    });
  });

  describe('has a delete SBOM modal that', () => {
    beforeEach(async () => {
      axiosMock.onGet(getSbomsByApplicationUrl(applicationId, 1, 10, SORT_BY_FIELDS.importDate, false)).reply(200, {
        applicationId: applicationId,
        results: [
          {
            applicationVersion: 'app123',
            spec: 'SPDX',
            specVersion: '2.1',
            importDate: '2020-01-01T12:00:00.000+00:00',
            none: 0,
            low: 1,
            medium: 2,
            high: 3,
            critical: 4,
            isValid: true,
            releaseStatusPercentage: 0.0,
          },
        ],
        totalResultsCount: 0,
      });
      renderComponent(initialState);
      await waitFor(() => expect(screen.queryByText('Loading')).toBeNull());
      const dropdown = screen.getByRole('generic', { name: /app123-options/i }).querySelector('button');
      fireEvent.click(dropdown);
      fireEvent.click(screen.getByRole('button', { name: 'Delete SBOM' }));
    });

    it('renders correctly', async () => {
      expect(screen.getByText('Are you sure you want to delete app123?')).toBeVisible();
      expect(screen.getByRole('button', { name: 'Cancel' })).toBeVisible();
      expect(screen.getByRole('button', { name: 'Delete' })).toBeVisible();
    });

    it('closes correctly', async () => {
      fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
      expect(screen.queryByText('Are you sure you want to delete app123?')).not.toBeInTheDocument();
    });
  });
});
