/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { update } from 'ramda';

import { axiosMockAdapter, fireEvent, render, screen, waitFor, within } from 'TestRoot/SpecUtil';

import { getBillOfMaterialsComponentsUrl } from 'MainRoot/util/CLMLocation';
import BillOfMaterialsComponentsTile from 'MainRoot/sbomManager/features/billOfMaterialsComponentsTile/BillOfMaterialsComponentsTile';
import {
  COMPONENTS_PER_PAGE,
  SORT_BY_FIELDS,
  SORT_DIRECTION,
} from 'MainRoot/sbomManager/features/billOfMaterialsComponentsTile/billOfMaterialsComponentsTileSlice';

import {
  cleanUpComponentsFilterDrawerPortalContainer,
  setupComponentsFilterDrawerPortalContainer,
} from './componentsFilterDrawer/ComponentsFilterDrawer.jestspec';

xdescribe('BillOfMaterialsComponentsTile', () => {
  let axiosMock, initialProps, initialState;

  const INTERNAL_APP_ID = 'internal-app-id';
  const SBOM_VERSION = 'sbom-version';

  const defaultSortConfiguration = Object.freeze({
    sortBy: SORT_BY_FIELDS.vulnerabilities,
    sortDirection: SORT_DIRECTION.DESC,
  });

  const defaultFilterConfiguration = Object.freeze({
    vulnerabilityThreatLevels: {
      critical: false,
      high: false,
      medium: false,
      low: false,
    },
    dependencyTypes: {
      direct: false,
      transitive: false,
      unspecified: false,
    },
  });

  const filterDrawerInitialState = Object.freeze({
    showDrawer: false,
    collapsibleItems: {
      showVulnerabilityThreatLevels: true,
      showDependencyTypes: true,
    },
  });

  const paginationInitialState = Object.freeze({
    pageCount: 1,
    currentPage: 0,
  });

  const componentTemplate = ({ hash, name, dependencyType, vulnerabilities, licenses, percentageAnnotated }) =>
    Object.freeze({
      hash,
      packageUrl: `pkg:maven/com.package.${name}/artifact-id@1.2.3?type=jar`,
      name,
      version: '1.2.3',
      dependencyType,
      componentIdentifier: {
        format: 'maven',
        coordinates: {
          artifactId: 'artifact-id',
          extension: 'jar',
          groupId: `com.package.${name}`,
          version: '1.4.5',
        },
      },
      displayName: `com.package.${name} : artifact-id : 1.2.3`,
      licenses: licenses.map(([licenseId, licenseName]) => ({ licenseId, licenseName })),
      vulnerabilitySeverityNoneCount: vulnerabilities[0],
      vulnerabilitySeverityLowCount: vulnerabilities[1],
      vulnerabilitySeverityMediumCount: vulnerabilities[2],
      vulnerabilitySeverityHighCount: vulnerabilities[3],
      vulnerabilitySeverityCriticalCount: vulnerabilities[4],
      percentageAnnotated,
    });

  const componentParametersList = [
    {
      hash: 'hash-1',
      name: 'alice',
      dependencyType: 'direct',
      vulnerabilities: [0, 1, 2, 3, 4],
      licenses: [
        ['BSD', 'BSD'],
        ['Apache', 'Apache'],
      ],
      percentageAnnotated: 0,
    },
    {
      hash: 'hash-2',
      name: 'bob',
      dependencyType: 'transitive',
      vulnerabilities: [0, 5, 6, 7, 8],
      licenses: [
        ['MIT', null],
        ['Public', 'Public'],
      ],
      percentageAnnotated: 50.5,
    },
    {
      hash: 'hash-3',
      name: 'malice',
      dependencyType: 'unspecified',
      vulnerabilities: [0, 9, 10, 11, 12],
      licenses: [
        ['Beer', null],
        ['GNU', null],
      ],
      percentageAnnotated: 100,
    },
  ];

  const generateResponse = (parametersList, totalResultsCountOverride) => ({
    totalResultsCount: totalResultsCountOverride || parametersList.length,
    results: parametersList.map((params) => componentTemplate(params)),
  });

  const baseUrlParams = Object.freeze([
    INTERNAL_APP_ID,
    SBOM_VERSION,
    1,
    COMPONENTS_PER_PAGE,
    SORT_BY_FIELDS.vulnerabilities,
    false,
  ]);

  const renderComponent = (props, preloadedState) => {
    setupComponentsFilterDrawerPortalContainer();
    render(<BillOfMaterialsComponentsTile {...props} />, { preloadedState });
  };

  beforeEach(() => {
    axiosMock = axiosMockAdapter();

    initialProps = {
      internalAppId: INTERNAL_APP_ID,
    };

    initialState = {
      router: {
        currentParams: {
          versionId: SBOM_VERSION,
        },
      },
      billOfMaterialsComponentsTile: {
        loadingComponents: true,
        loadingComponentsErrorMessage: null,
        components: null,
        totalNumberOfComponents: null,

        sortConfiguration: { ...defaultSortConfiguration },
        filterConfiguration: { ...defaultFilterConfiguration },
        pagination: { ...paginationInitialState },

        filterDrawer: { ...filterDrawerInitialState },
      },
    };

    axiosMock
      .onGet(getBillOfMaterialsComponentsUrl(...baseUrlParams))
      .reply(200, generateResponse(componentParametersList));
  });

  afterEach(() => {
    cleanUpComponentsFilterDrawerPortalContainer();
  });

  it('renders the correct title', async () => {
    renderComponent(initialProps, initialState);
    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());
    expect(screen.getByRole('heading', { name: /Components/ })).toBeVisible();
  });

  it('renders the loading error message if an error occurred', async () => {
    axiosMock
      .onGet(getBillOfMaterialsComponentsUrl(...baseUrlParams))
      .reply(() => Promise.reject({ response: { data: 'Error Message' } }));

    renderComponent(initialProps, initialState);

    const errorAlert = await screen.findByRole('alert');
    expect(errorAlert).toBeVisible();
    expect(errorAlert).toHaveTextContent('An error occurred loading data. Error Message');
  });

  describe('Filter By', () => {
    it('should have the Filter By button', async () => {
      renderComponent(initialProps, initialState);

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      const filterButton = screen.getByRole('button', { name: 'Filter By' });
      expect(filterButton).toBeVisible();
    });
  });

  describe('Components Table', () => {
    it('renders the loading indicator initially', () => {
      renderComponent(initialProps, initialState);
      expect(screen.getByText('Loading…')).toBeVisible();
    });

    it('renders the correct empty message', async () => {
      axiosMock
        .onGet(getBillOfMaterialsComponentsUrl(...baseUrlParams))
        .reply(200, { totalResultsCount: 0, results: [] });

      renderComponent(initialProps, initialState);

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      expect(screen.getByText('No components found')).toBeVisible();
    });

    it('sorts the header row cells correctly', async () => {
      renderComponent(initialProps, initialState);

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      const sortableFields = ['Type', 'Percentage Annotated'];
      for (const field of sortableFields) {
        const columnHeader = await screen.findByRole('columnheader', { name: field });
        expect(columnHeader).toBeVisible();

        fireEvent.click(columnHeader);
        expect(await screen.findByLabelText(`${field} ascending`)).toBeVisible();

        fireEvent.click(columnHeader);
        expect(await screen.findByLabelText(`${field} descending`)).toBeVisible();

        fireEvent.click(columnHeader);
        expect(await screen.findByLabelText(`${field} unsorted`)).toBeVisible();
        // Default state is set:
        expect(await screen.findByLabelText('Vulnerabilities descending')).toBeVisible();
      }

      const vulnerabilitiesColumnHeader = await screen.findByRole('columnheader', { name: /Vulnerabilities/i });
      expect(vulnerabilitiesColumnHeader).toBeVisible();

      fireEvent.click(vulnerabilitiesColumnHeader);
      expect(await screen.findByLabelText(`Vulnerabilities ascending`)).toBeVisible();

      fireEvent.click(vulnerabilitiesColumnHeader);
      expect(await screen.findByLabelText(`Vulnerabilities descending`)).toBeVisible();

      const anotherColumnHeader = await screen.findByRole('columnheader', { name: 'Type' });
      fireEvent.click(anotherColumnHeader);
      expect(await screen.findByLabelText(`Type ascending`)).toBeVisible();
    });

    it('renders the correct number of components and content', async () => {
      renderComponent(initialProps, initialState);

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      const tableRows = await screen.findAllByRole('row');

      // +1 including the header
      expect(tableRows.length).toBe(4);

      const firstRow = tableRows[1];
      const firstRowCells = within(firstRow).getAllByRole('cell');
      expect(firstRowCells[0]).toHaveTextContent('D');
      expect(firstRowCells[1]).toHaveTextContent('com.package.alice : artifact-id : 1.2.3');
      // The text content for each severity pill is contained in 3 divs:
      // • One div with the severity text
      // • One div for the actual severity
      // • One div with the overflow text in this case is set at 100 so the overflow text is 99+
      // The expected text of the vulnerabilities cell is the combinations of all of these.
      expect(firstRowCells[2]).toHaveTextContent('Critical499+Severe399+Moderate299+Low199+');
      expect(firstRowCells[3]).toHaveTextContent(/0%/);
      expect(firstRowCells[4]).toHaveTextContent('BSD, Apache');

      const secondRow = tableRows[2];
      const secondRowCells = within(secondRow).getAllByRole('cell');
      expect(secondRowCells[0]).toHaveTextContent('T');
      expect(secondRowCells[1]).toHaveTextContent('com.package.bob : artifact-id : 1.2.3');
      expect(secondRowCells[2]).toHaveTextContent('Critical899+Severe799+Moderate699+Low599+');
      expect(secondRowCells[3]).toHaveTextContent(/50.5%/);
      expect(secondRowCells[4]).toHaveTextContent('MIT, Public');

      const thirdRow = tableRows[3];
      const thirdRowCells = within(thirdRow).getAllByRole('cell');
      expect(thirdRowCells[0]).toHaveTextContent('');
      expect(thirdRowCells[1]).toHaveTextContent('com.package.malice : artifact-id : 1.2.3');
      expect(thirdRowCells[2]).toHaveTextContent('Critical1299+Severe1199+Moderate1099+Low999+');
      expect(thirdRowCells[3]).toHaveTextContent(/100%/);
      expect(thirdRowCells[4]).toHaveTextContent('Beer, GNU');
    });
  });

  describe('Pagination Status', () => {
    it('should show the correct pagination status text on first page', async () => {
      axiosMock
        .onGet(getBillOfMaterialsComponentsUrl(...baseUrlParams))
        .reply(200, generateResponse(componentParametersList, 2500));

      renderComponent(initialProps, initialState);

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      const paginationStatus = screen.getByTestId('bill-of-materials-components-tile-pagination-status');
      expect(paginationStatus).toBeVisible();
      expect(paginationStatus).toHaveTextContent(`Showing 50 of 2,500 components`);
    });

    it('should show the correct pagination status text when total < maximum components per page', async () => {
      axiosMock
        .onGet(getBillOfMaterialsComponentsUrl(...baseUrlParams))
        .reply(200, generateResponse(componentParametersList, 25));

      renderComponent(initialProps, initialState);

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      const paginationStatus = screen.getByTestId('bill-of-materials-components-tile-pagination-status');
      expect(paginationStatus).toBeVisible();
      expect(paginationStatus).toHaveTextContent(`Showing 25 of 25 components`);
    });

    it('should show the correct pagination status for not first or last page', async () => {
      axiosMock
        .onGet(getBillOfMaterialsComponentsUrl(...baseUrlParams))
        .reply(200, generateResponse(componentParametersList, 150));

      renderComponent(initialProps, initialState);

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      axiosMock
        .onGet(getBillOfMaterialsComponentsUrl(...update(2, 2, baseUrlParams)))
        .reply(200, generateResponse(componentParametersList, 150));

      const nextButton = screen.getByRole('button', { name: 'goto next page' });
      expect(nextButton).toBeVisible();
      fireEvent.click(nextButton);

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      const paginationStatus = screen.getByTestId('bill-of-materials-components-tile-pagination-status');
      expect(paginationStatus).toBeVisible();

      expect(paginationStatus).toHaveTextContent(`Showing 51\u2014100 of 150 components`);
    });

    it('should show the correct pagination status for the last page', async () => {
      axiosMock
        .onGet(getBillOfMaterialsComponentsUrl(...baseUrlParams))
        .reply(200, generateResponse(componentParametersList, 100));

      renderComponent(initialProps, initialState);

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      axiosMock
        .onGet(getBillOfMaterialsComponentsUrl(...update(2, 2, baseUrlParams)))
        .reply(200, generateResponse(componentParametersList, 100));

      const nextButton = screen.getByRole('button', { name: 'goto next page' });
      expect(nextButton).toBeVisible();
      fireEvent.click(nextButton);

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      const paginationStatus = screen.getByTestId('bill-of-materials-components-tile-pagination-status');
      expect(paginationStatus).toBeVisible();

      expect(paginationStatus).toHaveTextContent(`Showing 100 of 100 components`);
    });
  });
});
