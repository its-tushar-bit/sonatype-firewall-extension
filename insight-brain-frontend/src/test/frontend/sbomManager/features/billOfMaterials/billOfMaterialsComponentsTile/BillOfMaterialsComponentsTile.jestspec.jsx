/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { update } from 'ramda';

import userEvent from '@testing-library/user-event';
import { axiosMockAdapter, fireEvent, render, screen, waitFor, within } from 'TestRoot/SpecUtil';

import { getBillOfMaterialsComponentsUrl } from 'MainRoot/util/CLMLocation';
import BillOfMaterialsComponentsTile from 'MainRoot/sbomManager/features/billOfMaterials/billOfMaterialsComponentsTile/BillOfMaterialsComponentsTile';
import {
  COMPONENTS_PER_PAGE,
  SORT_BY_FIELDS,
  SORT_DIRECTION,
} from 'MainRoot/sbomManager/features/billOfMaterials/billOfMaterialsComponentsTile/billOfMaterialsComponentsTileSlice';
import { UI_ROUTER_ON_FINISH } from 'MainRoot/reduxUiRouter/routerActions';

describe('BillOfMaterialsComponentsTile', () => {
  let axiosMock, initialState;

  const JEST_TIMER = 1000;
  const APPLICATION_INTERNAL_ID = 'APPLICATION-INTERNAL-ID';
  const SBOM_VERSION = 'SBOM-VERSION';

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

  const componentTemplate = ({
    hash,
    name,
    dependencyType,
    matchStateId,
    vulnerabilities,
    licenses,
    releaseStatusPercentage,
    policyViolationCount,
    filenames,
  }) =>
    Object.freeze({
      hash,
      packageUrl: `pkg:maven/com.package.${name}/artifact-id@1.2.3?type=jar`,
      name,
      version: '1.2.3',
      dependencyType,
      filenames: filenames || [`pkg:maven/com.package.${name}/artifact-id@1.2.3?type=jar`],
      matchStateId,
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
      licenses: licenses.map(([licenseId, licenseName, overrideStatus]) => ({
        licenseId,
        licenseName,
        overrideStatus,
      })),
      vulnerabilitySeverityNoneCount: vulnerabilities[0],
      vulnerabilitySeverityLowCount: vulnerabilities[1],
      vulnerabilitySeverityMediumCount: vulnerabilities[2],
      vulnerabilitySeverityHighCount: vulnerabilities[3],
      vulnerabilitySeverityCriticalCount: vulnerabilities[4],
      releaseStatusPercentage,
      policyViolationCount,
    });

  const componentParametersList = [
    {
      hash: 'hash-1',
      name: 'malice',
      dependencyType: 'direct',
      matchStateId: 'exact',
      vulnerabilities: [0, 1, 2, 3, 4],
      licenses: [
        ['BSD', 'BSD'],
        ['Apache', 'Apache'],
      ],
      releaseStatusPercentage: 0,
      policyViolationCount: 111,
    },
    {
      hash: 'hash-2',
      name: 'bob',
      dependencyType: 'transitive',
      matchStateId: 'exact',
      vulnerabilities: [0, 5, 6, 7, 8],
      licenses: [
        ['MIT', null],
        ['Public', 'Public'],
      ],
      releaseStatusPercentage: 20,
      policyViolationCount: 222,
    },
    {
      hash: 'hash-3',
      name: 'alice',
      dependencyType: 'unspecified',
      matchStateId: 'similar',
      vulnerabilities: [0, 9, 10, 11, 12],
      licenses: [
        ['Beer', null, 'OVERRIDDEN'],
        ['GNU', null, 'OVERRIDDEN'],
      ],
      releaseStatusPercentage: 100,
      policyViolationCount: 333,
    },
  ];

  const generateResponse = (parametersList, totalResultsCountOverride) => ({
    totalResultsCount: totalResultsCountOverride || parametersList.length,
    results: parametersList.map((params) => componentTemplate(params)),
  });

  const baseUrlParams = Object.freeze([
    APPLICATION_INTERNAL_ID,
    SBOM_VERSION,
    1,
    COMPONENTS_PER_PAGE,
    SORT_BY_FIELDS.vulnerabilities,
    false,
  ]);

  const renderComponent = (preloadedState) => render(<BillOfMaterialsComponentsTile />, { preloadedState });

  beforeEach(() => {
    axiosMock = axiosMockAdapter();

    initialState = {
      router: {
        currentParams: {
          versionId: SBOM_VERSION,
        },
      },
      billOfMaterialsPage: {
        internalAppId: APPLICATION_INTERNAL_ID,
        sbomMetadata: {
          displayNameSortingEnabled: true,
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

        componentSearch: null,
      },
    };

    axiosMock
      .onGet(getBillOfMaterialsComponentsUrl(...baseUrlParams))
      .reply(200, generateResponse(componentParametersList));
  });

  it('renders the correct title', async () => {
    jest.useFakeTimers();

    renderComponent(initialState);

    jest.advanceTimersByTime(JEST_TIMER);
    jest.useRealTimers();

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    expect(screen.getByRole('heading', { name: /Components/ })).toBeVisible();
  });

  it('renders the loading error message if an error occurred', async () => {
    jest.useFakeTimers();

    axiosMock
      .onGet(getBillOfMaterialsComponentsUrl(...baseUrlParams))
      .reply(() => Promise.reject({ response: { data: 'Error Message' } }));

    renderComponent(initialState);

    jest.advanceTimersByTime(JEST_TIMER);
    jest.useRealTimers();

    const errorAlert = await screen.findByRole('alert');
    expect(errorAlert).toBeVisible();
    expect(errorAlert).toHaveTextContent('An error occurred loading data. Error Message');
  });

  describe('Component Name Search', () => {
    it('should render component name search text field', async () => {
      jest.useFakeTimers();

      renderComponent(initialState);

      jest.advanceTimersByTime(JEST_TIMER);
      jest.useRealTimers();

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      const searchTextBox = screen.getByRole('textbox', {
        name: /Component Search/i,
      });

      expect(searchTextBox).toBeVisible();
    });

    it('clears the search input when the SBOM version changes without unmounting the tile', async () => {
      const user = userEvent.setup();

      const { store } = render(<BillOfMaterialsComponentsTile />, { preloadedState: initialState });

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      const searchTextBox = screen.getByRole('textbox', { name: /Component Search/i });
      await user.type(searchTextBox, 'junit');
      expect(searchTextBox).toHaveValue('junit');

      const bomRouteState = { name: 'sbomManager.management.view.bom', url: '', data: {} };
      store.dispatch({
        type: UI_ROUTER_ON_FINISH,
        payload: {
          fromState: bomRouteState,
          fromParams: { versionId: SBOM_VERSION },
          toState: bomRouteState,
          toParams: { versionId: 'NEXT-SBOM-VERSION' },
        },
      });

      await waitFor(() => expect(screen.getByRole('textbox', { name: /Component Search/i })).toHaveValue(''));
    });
  });

  describe('Filter By', () => {
    it('should have the Filter By button', async () => {
      jest.useFakeTimers();

      renderComponent(initialState);

      jest.advanceTimersByTime(JEST_TIMER);
      jest.useRealTimers();

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      const filterButton = screen.getByRole('button', { name: 'Filter By' });
      expect(filterButton).toBeVisible();
    });
  });

  describe('Components Table', () => {
    it('renders the loading indicator initially', () => {
      renderComponent(initialState);
      expect(screen.getByText('Loading…')).toBeVisible();
    });

    it('renders the correct empty message', async () => {
      jest.useFakeTimers();

      axiosMock
        .onGet(getBillOfMaterialsComponentsUrl(...baseUrlParams))
        .reply(200, { totalResultsCount: 0, results: [] });

      renderComponent(initialState);

      jest.advanceTimersByTime(JEST_TIMER);
      jest.useRealTimers();

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      expect(screen.getByText('No components found')).toBeVisible();
    });

    it('renders the correct number of components and content', async () => {
      jest.useFakeTimers();

      renderComponent(initialState);

      jest.advanceTimersByTime(JEST_TIMER);
      jest.useRealTimers();

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      const tableRows = await screen.findAllByRole('row');

      // +1 including the header
      expect(tableRows.length).toBe(4);

      const firstRow = tableRows[1];
      const firstRowCells = within(firstRow).getAllByRole('cell');
      expect(firstRowCells[0]).toHaveTextContent('D');
      expect(firstRowCells[1]).toHaveTextContent('com.package.malice : artifact-id : 1.2.3');
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
      expect(secondRowCells[3]).toHaveTextContent(/20%/);
      expect(secondRowCells[4]).toHaveTextContent('MIT, Public');

      const thirdRow = tableRows[3];
      const thirdRowCells = within(thirdRow).getAllByRole('cell');
      expect(thirdRowCells[0]).toHaveTextContent('');
      expect(thirdRowCells[1]).toHaveTextContent('com.package.alice : artifact-id : 1.2.3');
      expect(thirdRowCells[2]).toHaveTextContent('Critical1299+Severe1199+Moderate1099+Low999+');
      expect(thirdRowCells[3]).toHaveTextContent(/100%/);
      expect(thirdRowCells[4]).toHaveTextContent('Beer, GNUOverridden');

      const similarMatchIcon = await screen.findByTestId('similarMatchIcon');
      expect(similarMatchIcon).toBeInTheDocument();

      fireEvent.mouseOver(similarMatchIcon);
      const tooltip = await screen.findByRole('tooltip');
      expect(tooltip).toHaveTextContent(
        'Original Component Name: pkg:maven/com.package.alice/artifact-id@1.2.3?type=jar.' +
          'Similar component match: This component is similar to a known open source component' +
          ' within your application based on its attributes.'
      );
    });

    it('renders embedded match icon with tooltip', async () => {
      jest.useFakeTimers();

      const embeddedComponentParams = {
        hash: 'hash-embedded',
        name: 'uber-lib',
        dependencyType: 'direct',
        matchStateId: 'embedded',
        vulnerabilities: [0, 0, 0, 0, 0],
        licenses: [],
        releaseStatusPercentage: 0,
        policyViolationCount: 0,
        filenames: ['app-uber.jar'],
      };

      axiosMock
        .onGet(getBillOfMaterialsComponentsUrl(...baseUrlParams))
        .reply(200, generateResponse([embeddedComponentParams]));

      renderComponent(initialState);

      jest.advanceTimersByTime(JEST_TIMER);
      jest.useRealTimers();

      const embeddedMatchIcon = await screen.findByRole('img', { name: 'Embedded match state' });
      expect(embeddedMatchIcon).toBeInTheDocument();

      await userEvent.hover(embeddedMatchIcon);
      const tooltip = await screen.findByRole('tooltip');
      expect(tooltip).toHaveTextContent(
        'Embedded component match: This component was identified as an OSS constituent inside an uber JAR (app-uber.jar).'
      );
    });

    it('cannot sort by display name if 0 components', async () => {
      jest.useFakeTimers();

      axiosMock.onGet(getBillOfMaterialsComponentsUrl(...baseUrlParams)).reply(200, generateResponse([]));

      renderComponent(initialState);

      jest.advanceTimersByTime(JEST_TIMER);
      jest.useRealTimers();

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      const tableRows = await screen.findAllByRole('row');
      expect(tableRows.length).toBe(2);

      const sortByDisplayNameButton = screen.queryByRole('button', { name: /name unsorted/i });
      expect(sortByDisplayNameButton).toBeNull();
    });

    it('cannot sort by display name if 1 component', async () => {
      jest.useFakeTimers();

      axiosMock
        .onGet(getBillOfMaterialsComponentsUrl(...baseUrlParams))
        .reply(200, generateResponse([componentParametersList[0]]));

      renderComponent(initialState);

      jest.advanceTimersByTime(JEST_TIMER);
      jest.useRealTimers();

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      const tableRows = await screen.findAllByRole('row');
      expect(tableRows.length).toBe(2);

      const sortByDisplayNameButton = screen.queryByRole('button', { name: /name unsorted/i });
      expect(sortByDisplayNameButton).toBeNull();
    });

    it('cannot sort by display name if displayNameSortingEnabled is false', async () => {
      jest.useFakeTimers();

      renderComponent({
        ...initialState,
        billOfMaterialsPage: {
          internalAppId: APPLICATION_INTERNAL_ID,
          sbomMetadata: { displayNameSortingEnabled: false },
        },
      });

      jest.advanceTimersByTime(JEST_TIMER);
      jest.useRealTimers();

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      const tableRows = await screen.findAllByRole('row');
      expect(tableRows.length).toBe(4);

      const sortByDisplayNameButton = screen.queryByRole('button', { name: /name unsorted/i });
      expect(sortByDisplayNameButton).toBeNull();
    });

    it('can sort by display name if displayNameSortingEnabled is true and 2 components', async () => {
      jest.useFakeTimers();

      axiosMock
        .onGet(getBillOfMaterialsComponentsUrl(...baseUrlParams))
        .reply(200, generateResponse([componentParametersList[0], componentParametersList[1]]));

      renderComponent(initialState);

      jest.advanceTimersByTime(JEST_TIMER);
      jest.useRealTimers();

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      const tableRows = await screen.findAllByRole('row');
      expect(tableRows.length).toBe(3);

      const sortByDisplayNameButton = screen.getByRole('button', { name: /name unsorted/i });
      expect(sortByDisplayNameButton).toBeVisible();
    });

    it('can sort by display name if displayNameSortingEnabled is true and more than 2 components', async () => {
      jest.useFakeTimers();

      // Unsorted
      renderComponent(initialState);

      jest.advanceTimersByTime(JEST_TIMER);

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      let tableRows = await screen.findAllByRole('row');
      expect(tableRows.length).toBe(4);

      let firstRow = tableRows[1];
      let firstRowCells = within(firstRow).getAllByRole('cell');
      expect(firstRowCells[1]).toHaveTextContent('com.package.malice : artifact-id : 1.2.3');

      let secondRow = tableRows[2];
      let secondRowCells = within(secondRow).getAllByRole('cell');
      expect(secondRowCells[1]).toHaveTextContent('com.package.bob : artifact-id : 1.2.3');

      let thirdRow = tableRows[3];
      let thirdRowCells = within(thirdRow).getAllByRole('cell');
      expect(thirdRowCells[1]).toHaveTextContent('com.package.alice : artifact-id : 1.2.3');

      let sortByDisplayNameButton = screen.getByRole('button', { name: /name unsorted/i });
      expect(sortByDisplayNameButton).toBeVisible();

      // Ascending - reset and setup new mock
      axiosMock.reset();
      axiosMock
        .onGet(
          getBillOfMaterialsComponentsUrl(
            APPLICATION_INTERNAL_ID,
            SBOM_VERSION,
            1,
            COMPONENTS_PER_PAGE,
            SORT_BY_FIELDS.displayName,
            true
          )
        )
        .reply(
          200,
          generateResponse([componentParametersList[2], componentParametersList[1], componentParametersList[0]])
        );

      // Temporarily use real timers for the async request
      jest.useRealTimers();

      const requestCountBefore = axiosMock.history.get.length;

      fireEvent.click(sortByDisplayNameButton);

      // Wait for the API request to be made
      await waitFor(() => {
        expect(axiosMock.history.get.length).toBeGreaterThan(requestCountBefore);
      });

      // Wait for Loading to disappear
      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      // Wait for the sorted data to appear - initial state was 'malice' in first row, should change to 'alice'
      await waitFor(
        () => {
          const rows = screen.queryAllByRole('row');
          if (rows.length !== 4) return false;
          const firstDataRow = rows[1];
          const cells = within(firstDataRow).queryAllByRole('cell');
          // Check that we have cells and the first data row now has 'alice' (not 'malice')
          const hasAlice = cells.length > 1 && cells[1]?.textContent?.includes('alice');
          const notMalice = cells.length > 1 && !cells[1]?.textContent?.includes('malice');
          return hasAlice && notMalice;
        },
        { timeout: 5000 }
      );

      tableRows = await screen.findAllByRole('row');
      firstRow = tableRows[1];
      firstRowCells = within(firstRow).getAllByRole('cell');
      expect(firstRowCells.length).toBeGreaterThan(1);
      expect(firstRowCells[1]).toHaveTextContent('com.package.alice : artifact-id : 1.2.3');

      secondRow = tableRows[2];
      secondRowCells = within(secondRow).getAllByRole('cell');
      expect(secondRowCells[1]).toHaveTextContent('com.package.bob : artifact-id : 1.2.3');

      thirdRow = tableRows[3];
      thirdRowCells = within(thirdRow).getAllByRole('cell');
      expect(thirdRowCells[1]).toHaveTextContent('com.package.malice : artifact-id : 1.2.3');

      sortByDisplayNameButton = screen.getByRole('button', { name: /name ascending/i });
      expect(sortByDisplayNameButton).toBeVisible();

      // Descending - reset and setup new mock
      axiosMock.reset();
      axiosMock
        .onGet(
          getBillOfMaterialsComponentsUrl(
            APPLICATION_INTERNAL_ID,
            SBOM_VERSION,
            1,
            COMPONENTS_PER_PAGE,
            SORT_BY_FIELDS.displayName,
            false
          )
        )
        .reply(
          200,
          generateResponse([componentParametersList[0], componentParametersList[1], componentParametersList[2]])
        );

      const requestCountBefore2 = axiosMock.history.get.length;

      fireEvent.click(sortByDisplayNameButton);

      // Wait for the API request to be made
      await waitFor(() => {
        expect(axiosMock.history.get.length).toBeGreaterThan(requestCountBefore2);
      });

      // Wait for Loading to disappear
      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      // Wait for the sorted data to appear - descending sort should have 'malice' first (not 'alice')
      await waitFor(
        () => {
          const rows = screen.queryAllByRole('row');
          if (rows.length !== 4) return false;
          const firstDataRow = rows[1];
          const cells = within(firstDataRow).queryAllByRole('cell');
          // Check that we have cells and the first data row now has 'malice' (not 'alice')
          const hasMalice = cells.length > 1 && cells[1]?.textContent?.includes('malice');
          const notAlice = cells.length > 1 && !cells[1]?.textContent?.includes('alice');
          return hasMalice && notAlice;
        },
        { timeout: 5000 }
      );

      tableRows = await screen.findAllByRole('row');
      firstRow = tableRows[1];
      firstRowCells = within(firstRow).getAllByRole('cell');
      expect(firstRowCells[1]).toHaveTextContent('com.package.malice : artifact-id : 1.2.3');

      secondRow = tableRows[2];
      secondRowCells = within(secondRow).getAllByRole('cell');
      expect(secondRowCells[1]).toHaveTextContent('com.package.bob : artifact-id : 1.2.3');

      thirdRow = tableRows[3];
      thirdRowCells = within(thirdRow).getAllByRole('cell');
      expect(thirdRowCells[1]).toHaveTextContent('com.package.alice : artifact-id : 1.2.3');

      sortByDisplayNameButton = screen.getByRole('button', { name: /name descending/i });
      expect(sortByDisplayNameButton).toBeVisible();
    });
  });

  describe('Pagination Status', () => {
    it('should show the correct pagination status text on first page', async () => {
      jest.useFakeTimers();

      axiosMock
        .onGet(getBillOfMaterialsComponentsUrl(...baseUrlParams))
        .reply(200, generateResponse(componentParametersList, 2500));

      renderComponent(initialState);

      jest.advanceTimersByTime(JEST_TIMER);
      jest.useRealTimers();

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      const paginationStatus = screen.getByTestId('bill-of-materials-components-tile-pagination-status');
      expect(paginationStatus).toBeVisible();
      expect(paginationStatus).toHaveTextContent(`Showing 50 of 2,500 components`);
    });

    it('should show the correct pagination status text when total < maximum components per page', async () => {
      jest.useFakeTimers();

      axiosMock
        .onGet(getBillOfMaterialsComponentsUrl(...baseUrlParams))
        .reply(200, generateResponse(componentParametersList, 25));

      renderComponent(initialState);

      jest.advanceTimersByTime(JEST_TIMER);
      jest.useRealTimers();

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      const paginationStatus = screen.getByTestId('bill-of-materials-components-tile-pagination-status');
      expect(paginationStatus).toBeVisible();
      expect(paginationStatus).toHaveTextContent(`Showing 25 of 25 components`);
    });

    it('should show the correct pagination status for not first or last page', async () => {
      jest.useFakeTimers();

      axiosMock
        .onGet(getBillOfMaterialsComponentsUrl(...baseUrlParams))
        .reply(200, generateResponse(componentParametersList, 150));

      renderComponent(initialState);

      jest.advanceTimersByTime(JEST_TIMER);

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      axiosMock
        .onGet(getBillOfMaterialsComponentsUrl(...update(2, 2, baseUrlParams)))
        .reply(200, generateResponse(componentParametersList, 150));

      const nextButton = screen.getByRole('button', { name: 'goto next page' });
      expect(nextButton).toBeVisible();
      fireEvent.click(nextButton);

      jest.advanceTimersByTime(JEST_TIMER);
      jest.useRealTimers();

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      const paginationStatus = screen.getByTestId('bill-of-materials-components-tile-pagination-status');
      expect(paginationStatus).toBeVisible();

      expect(paginationStatus).toHaveTextContent(`Showing 51\u2014100 of 150 components`);
    });

    it('should show the correct pagination status for the last page', async () => {
      jest.useFakeTimers();

      axiosMock
        .onGet(getBillOfMaterialsComponentsUrl(...baseUrlParams))
        .reply(200, generateResponse(componentParametersList, 100));

      renderComponent(initialState);

      jest.advanceTimersByTime(JEST_TIMER);

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      axiosMock
        .onGet(getBillOfMaterialsComponentsUrl(...update(2, 2, baseUrlParams)))
        .reply(200, generateResponse(componentParametersList, 100));

      const nextButton = screen.getByRole('button', { name: 'goto next page' });
      expect(nextButton).toBeVisible();
      fireEvent.click(nextButton);

      jest.advanceTimersByTime(JEST_TIMER);
      jest.useRealTimers();

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      const paginationStatus = screen.getByTestId('bill-of-materials-components-tile-pagination-status');
      expect(paginationStatus).toBeVisible();

      expect(paginationStatus).toHaveTextContent(`Showing 100 of 100 components`);
    });
  });

  describe('Policy Violations', () => {
    it('renders the correct number of components and content', async () => {
      jest.useFakeTimers();

      const productFeaturesState = {
        productFeatures: {
          loading: false,
          loadError: null,
          productFeatures: {
            'sbom-manager': true,
            'sbom-policies': true,
          },
        },
      };

      renderComponent({ ...initialState, ...productFeaturesState });

      jest.advanceTimersByTime(JEST_TIMER);
      jest.useRealTimers();

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      const tableRows = await screen.findAllByRole('row');

      expect(tableRows.length).toBe(4);

      const firstRow = tableRows[1];
      const firstRowCells = within(firstRow).getAllByRole('cell');
      expect(firstRowCells[0]).toHaveTextContent('D');
      expect(firstRowCells[1]).toHaveTextContent('com.package.malice : artifact-id : 1.2.3');

      expect(firstRowCells[2]).toHaveTextContent('Critical499+Severe399+Moderate299+Low199+');
      expect(firstRowCells[3]).toHaveTextContent('111');
      expect(firstRowCells[4]).toHaveTextContent(/0%/);
      expect(firstRowCells[5]).toHaveTextContent('BSD, Apache');

      const secondRow = tableRows[2];
      const secondRowCells = within(secondRow).getAllByRole('cell');
      expect(secondRowCells[0]).toHaveTextContent('T');
      expect(secondRowCells[1]).toHaveTextContent('com.package.bob : artifact-id : 1.2.3');
      expect(secondRowCells[2]).toHaveTextContent('Critical899+Severe799+Moderate699+Low599+');
      expect(secondRowCells[3]).toHaveTextContent('222');
      expect(secondRowCells[4]).toHaveTextContent(/20%/);
      expect(secondRowCells[5]).toHaveTextContent('MIT, Public');

      const thirdRow = tableRows[3];
      const thirdRowCells = within(thirdRow).getAllByRole('cell');
      expect(thirdRowCells[0]).toHaveTextContent('');
      expect(thirdRowCells[1]).toHaveTextContent('com.package.alice : artifact-id : 1.2.3');
      expect(thirdRowCells[2]).toHaveTextContent('Critical1299+Severe1199+Moderate1099+Low999+');
      expect(thirdRowCells[3]).toHaveTextContent('333');
      expect(thirdRowCells[4]).toHaveTextContent(/100%/);
      expect(thirdRowCells[5]).toHaveTextContent('Beer, GNU');
    });
  });
});
