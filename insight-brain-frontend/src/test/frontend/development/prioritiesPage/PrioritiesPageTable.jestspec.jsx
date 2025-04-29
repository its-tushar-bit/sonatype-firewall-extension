/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, screen, within, fireEvent, axiosMockAdapter } from 'TestRoot/SpecUtil';
import PrioritiesPageTable from 'MainRoot/development/prioritiesPage/PrioritiesPageTable';
import * as RouterActions from 'MainRoot/reduxUiRouter/routerActions';
import { getPrioritiesPageTableData } from 'MainRoot/util/CLMLocation';
import { faker } from '@faker-js/faker';
import {
  defaultIntegrationParamsMap,
  validIntegrationTypes,
} from '../../../../main/frontend/development/prioritiesPage/utils';

const publicAppId = 'testPublicAppId';
const scanId = 'testScanId';

const NUM_OF_RESULTS = 30;
const DEFAULT_PAGE_SIZE = 15;

const mockData = generateMockData(NUM_OF_RESULTS);
const mockResponsePage1 = generateMockResponseByPage(1, mockData);
const mockResponsePage2 = generateMockResponseByPage(2, mockData);

describe('PrioritiesPageTable', () => {
  let renderComponent, stateGoSpy, axiosMock;

  const defaultPreloadedState = {
    router: {
      currentParams: {
        publicAppId,
        scanId,
        filterOnPolicyActions: false,
        componentNameFilter: '',
      },
      currentState: {
        name: 'prioritiesPageFromReports',
      },
    },
  };

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    stateGoSpy = jest.spyOn(RouterActions, 'stateGo');

    axiosMock
      .onGet(getPrioritiesPageTableData(publicAppId, scanId), {
        params: { pageSize: DEFAULT_PAGE_SIZE, page: 1, componentNameFilter: '', filterOnPolicyActions: false },
      })
      .reply(200, mockResponsePage1);
    axiosMock
      .onGet(getPrioritiesPageTableData(publicAppId, scanId), {
        params: { pageSize: DEFAULT_PAGE_SIZE, page: 2, componentNameFilter: '', filterOnPolicyActions: false },
      })
      .reply(200, mockResponsePage2);

    renderComponent = (preloadedState) =>
      render(<PrioritiesPageTable />, { preloadedState: preloadedState || defaultPreloadedState });
  });

  it('makes correct network request', () => {
    renderComponent();

    expect(axiosMock.history.get.length).toBe(1);
    expect(axiosMock.history.get[0].url).toBe(getPrioritiesPageTableData(publicAppId, scanId));
    expect(axiosMock.history.get[0].params).toEqual({
      pageSize: DEFAULT_PAGE_SIZE,
      page: 1,
      componentNameFilter: '',
      filterOnPolicyActions: false,
    });

    const table = screen.getByRole('table');
    expect(table).toBeInTheDocument();

    const loading = within(table).getByText('Loading…');
    expect(loading).toBeInTheDocument();
  });

  it('renders a loading spinner within the table', () => {
    renderComponent();

    const table = screen.getByRole('table');
    expect(table).toBeInTheDocument();

    const loading = within(table).getByText('Loading…');
    expect(loading).toBeInTheDocument();
  });

  it('renders an error within the table when network call fails', async () => {
    axiosMock
      .onGet(getPrioritiesPageTableData(publicAppId, scanId), {
        params: { pageSize: DEFAULT_PAGE_SIZE, page: 1, componentNameFilter: '', filterOnPolicyActions: false },
      })
      .reply(500, 'some_error');

    renderComponent();

    const table = await screen.findByRole('table');
    expect(table).toBeInTheDocument();

    const alert = within(table).getByRole('alert');
    expect(alert).toBeInTheDocument();
    expect(alert).toHaveTextContent('some_error');
  });

  it('clicking the retry button on error alert makes correct network request', async () => {
    axiosMock
      .onGet(getPrioritiesPageTableData(publicAppId, scanId), {
        params: { pageSize: DEFAULT_PAGE_SIZE, page: 1, componentNameFilter: '', filterOnPolicyActions: false },
      })
      .reply(500, 'Error');

    renderComponent();

    const table = await screen.findByRole('table');
    expect(table).toBeInTheDocument();

    expect(axiosMock.history.get.length).toBe(1);
    expect(axiosMock.history.get[0].url).toBe(getPrioritiesPageTableData(publicAppId, scanId));
    expect(axiosMock.history.get[0].params).toEqual({
      pageSize: DEFAULT_PAGE_SIZE,
      page: 1,
      componentNameFilter: '',
      filterOnPolicyActions: false,
    });

    const retryBtn = within(table).getByRole('button');
    fireEvent.click(retryBtn);

    expect(axiosMock.history.get.length).toBe(2);
    expect(axiosMock.history.get[1].url).toBe(getPrioritiesPageTableData(publicAppId, scanId));
    expect(axiosMock.history.get[1].params).toEqual({
      pageSize: DEFAULT_PAGE_SIZE,
      page: 1,
      componentNameFilter: '',
      filterOnPolicyActions: false,
    });
  });

  it('renders a table with 4 column headers', async () => {
    renderComponent();

    const table = await screen.findByRole('table');
    expect(table).toBeInTheDocument();

    const rows = within(table).getAllByRole('row');
    const headerRow = rows[0];
    const columnheaders = within(headerRow).getAllByRole('columnheader');
    expect(columnheaders.length).toBe(4 + 1); //last column is to render chevron icon for clickable rows
  });

  it('renders column headers with correct names in the correct order', async () => {
    renderComponent();

    const table = await screen.findByRole('table');
    expect(table).toBeInTheDocument();

    const columnHeaders = within(table).getAllByRole('columnheader');
    expect(columnHeaders[0]).toHaveAccessibleName(/priority/i);
    expect(columnHeaders[1]).toHaveAccessibleName(/component/i);
    expect(columnHeaders[2]).toHaveAccessibleName(/reason for priority/i);
    expect(columnHeaders[3]).toHaveAccessibleName(/suggested fix/i);
  });

  it('renders the priority column header with an icon and tooltip', async () => {
    renderComponent();

    const table = await screen.findByRole('table');
    expect(table).toBeInTheDocument();

    const priorityColumnHeader = screen.getAllByRole('columnheader')[0];

    const infoIcon = within(priorityColumnHeader).getByRole('img', { hidden: true });
    expect(infoIcon).toBeInTheDocument();

    fireEvent.mouseOver(infoIcon);
    const tooltip = await screen.findByRole('tooltip', {
      name:
        'Priority of actionable items based on the policy action, component reachability status, and threat score severity.',
    });
    expect(tooltip).toBeInTheDocument();
  });

  describe('empty message when there are no priorities', () => {
    beforeEach(() => axiosMock.reset());

    it('renders the right message by default (when fail/warn toggle is off)', async () => {
      const resp = generateMockResponseByPage(1, []);
      axiosMock.onGet(getPrioritiesPageTableData(publicAppId, scanId)).reply(200, resp);

      renderComponent();

      expect(axiosMock.history.get[0].url).toBe(getPrioritiesPageTableData(publicAppId, scanId));
      expect(axiosMock.history.get[0].params).toEqual({
        pageSize: DEFAULT_PAGE_SIZE,
        page: 1,
        componentNameFilter: '',
        filterOnPolicyActions: false,
      });

      const table = await screen.findByRole('table');
      expect(table).toBeInTheDocument();

      const rows = screen.getAllByRole('row');
      expect(rows.length).toBe(2);

      const rowWithMessage = rows[1];
      expect(rowWithMessage).toHaveTextContent('All clear! No violations were found during this evaluation.');
    });

    it('renders the right message (when fail/warn toggle is on)', async () => {
      const resp = generateMockResponseByPage(1, []);
      axiosMock.onGet(getPrioritiesPageTableData(publicAppId, scanId)).reply(200, resp);

      const routerState = {
        router: {
          currentParams: {
            publicAppId,
            scanId,
            filterOnPolicyActions: 'true',
          },
          currentState: {
            name: 'prioritiesPageFromReports',
          },
        },
      };
      renderComponent(routerState);

      expect(axiosMock.history.get[0].url).toBe(getPrioritiesPageTableData(publicAppId, scanId));
      expect(axiosMock.history.get[0].params).toEqual({
        pageSize: DEFAULT_PAGE_SIZE,
        page: 1,
        componentNameFilter: '',
        filterOnPolicyActions: true,
      });

      const table = await screen.findByRole('table');
      expect(table).toBeInTheDocument();

      const toggle = screen.getByRole('switch', { name: 'Fail/Warn Policy Actions only' });
      expect(toggle).toBeInTheDocument();
      expect(toggle).toBeChecked();

      const rows = screen.getAllByRole('row');
      expect(rows.length).toBe(2);

      const rowWithMessage = rows[1];
      expect(rowWithMessage).toHaveTextContent(
        'No violations with Fail/Warn policy actions were found during this evaluation.'
      );
    });
  });

  describe('component name filter', () => {
    it('filters components by name', async () => {
      jest.useFakeTimers();
      const filteredResponse = {
        priorities: {
          total: 1,
          page: 1,
          pageSize: DEFAULT_PAGE_SIZE,
          pageCount: 1,
          results: [
            {
              displayName: 'ABC',
              componentHash: faker.git.commitSha(),
              dependencyType: faker.helpers.arrayElement(['Direct', 'Transitive', 'Inner Source']),
              hasFailActionOnComponent: true,
              action: 'fail',
              highestThreat: faker.datatype.number({ min: 0, max: 10 }),
              highestThreatPolicyName: faker.lorem.slug(),
              highestThreatPolicyConstraintName: faker.lorem.sentence(),
              priority: 1,
              securityReachable: faker.helpers.arrayElement([true, false, null]),
            },
            {
              displayName: 'ABC',
              componentHash: faker.git.commitSha(),
              dependencyType: faker.helpers.arrayElement(['Direct', 'Transitive', 'Inner Source']),
              hasFailActionOnComponent: true,
              action: 'fail',
              highestThreat: faker.datatype.number({ min: 0, max: 10 }),
              highestThreatPolicyName: faker.lorem.slug(),
              highestThreatPolicyConstraintName: faker.lorem.sentence(),
              priority: 1,
              securityReachable: faker.helpers.arrayElement([true, false, null]),
            },
          ],
        },
      };

      axiosMock
        .onGet(getPrioritiesPageTableData(publicAppId, scanId), {
          params: { pageSize: DEFAULT_PAGE_SIZE, page: 1, componentNameFilter: 'ABC', filterOnPolicyActions: false },
        })
        .reply(200, filteredResponse);

      renderComponent();
      expect(axiosMock.history.get.length).toEqual(1);
      expect(axiosMock.history.get[0].params).toEqual({
        pageSize: DEFAULT_PAGE_SIZE,
        page: 1,
        componentNameFilter: '',
        filterOnPolicyActions: false,
      });

      const toggle = screen.getByRole('switch', { name: 'Fail/Warn Policy Actions only' });
      expect(toggle).toBeInTheDocument();
      expect(toggle).not.toBeChecked();

      const table = await screen.findByRole('table');
      expect(table).toBeInTheDocument();

      const filterInput = screen.getByPlaceholderText('Filter by component');
      fireEvent.change(filterInput, { target: { value: 'ABC' } });

      jest.runAllTimers();

      expect(axiosMock.history.get.length).toEqual(16); // 1 initial request + 15 async recommendation requests
      expect(stateGoSpy).toHaveBeenCalledWith('prioritiesPageFromReports', {
        publicAppId,
        scanId,
        filterOnPolicyActions: false,
        componentNameFilter: 'ABC',
      });
    });

    it('loads the table data for the filtered component name', async () => {
      const routerState = {
        router: {
          currentParams: {
            publicAppId,
            scanId,
            filterOnPolicyActions: false,
            componentNameFilter: 'some_component_name',
          },
        },
      };

      renderComponent(routerState);
      expect(axiosMock.history.get.length).toEqual(1);
      expect(axiosMock.history.get[0].params).toEqual({
        pageSize: DEFAULT_PAGE_SIZE,
        page: 1,
        componentNameFilter: 'some_component_name',
        filterOnPolicyActions: false,
      });
    });
  });

  describe('component action filter toggle', () => {
    it('toggles the "Fail/Warn Policy Actions only" filter and makes correct network requests', async () => {
      renderComponent();

      const defaultParams = {
        pageSize: DEFAULT_PAGE_SIZE,
        page: 1,
        componentNameFilter: '',
        filterOnPolicyActions: false,
      };

      expect(axiosMock.history.get.length).toBe(1);
      expect(axiosMock.history.get[0].url).toBe(getPrioritiesPageTableData(publicAppId, scanId));
      expect(axiosMock.history.get[0].params).toEqual(defaultParams);

      const table = await screen.findByRole('table');
      expect(table).toBeInTheDocument();

      const toggle = screen.getByRole('switch', { name: 'Fail/Warn Policy Actions only' });
      expect(toggle).toBeInTheDocument();
      expect(toggle).not.toBeChecked();

      fireEvent.click(toggle);

      expect(stateGoSpy).toHaveBeenCalledWith('prioritiesPageFromReports', {
        publicAppId,
        scanId,
        filterOnPolicyActions: true,
        componentNameFilter: '',
      });
    });

    it('sets the toggle to checked if query param filterOnPolicyActions=true is provided', async () => {
      const routerState = {
        router: {
          currentParams: {
            publicAppId,
            scanId,
            filterOnPolicyActions: 'true',
          },
        },
      };

      renderComponent(routerState);

      expect(axiosMock.history.get.length).toEqual(1);
      expect(axiosMock.history.get[0].params).toEqual({
        pageSize: DEFAULT_PAGE_SIZE,
        page: 1,
        componentNameFilter: '',
        filterOnPolicyActions: true,
      });

      const toggle = await screen.findByRole('switch', { name: 'Fail/Warn Policy Actions only' });
      expect(toggle).toBeInTheDocument();
      expect(toggle).toBeChecked();
    });

    it('sets the toggle to unchecked if query param filterOnPolicyActions=false is provided', async () => {
      const routerState = {
        router: {
          currentParams: {
            publicAppId,
            scanId,
            filterOnPolicyActions: 'false',
          },
        },
      };

      renderComponent(routerState);

      expect(axiosMock.history.get.length).toEqual(1);
      expect(axiosMock.history.get[0].params).toEqual({
        pageSize: DEFAULT_PAGE_SIZE,
        page: 1,
        componentNameFilter: '',
        filterOnPolicyActions: false,
      });

      const toggle = await screen.findByRole('switch', { name: 'Fail/Warn Policy Actions only' });
      expect(toggle).toBeInTheDocument();
      expect(toggle).not.toBeChecked();
    });
  });

  it('if the report is triggered by CM, toggle should be unchecked and disabled, even if query param filterOnPolicyActions=true is provided', async () => {
    const metadata = {
      forMonitoring: true,
    };
    const state = {
      applicationReport: {
        metadata,
      },
      router: {
        currentParams: {
          publicAppId,
          scanId,
          filterOnPolicyActions: 'true',
        },
      },
    };

    renderComponent(state);

    expect(axiosMock.history.get.length).toEqual(1);
    expect(axiosMock.history.get[0].params).toEqual({
      pageSize: DEFAULT_PAGE_SIZE,
      page: 1,
      componentNameFilter: '',
      filterOnPolicyActions: false,
    });

    const toggle = await screen.findByRole('switch', { name: 'Fail/Warn Policy Actions only' });
    expect(toggle).toBeInTheDocument();
    expect(toggle).toBeDisabled();
    expect(toggle).not.toBeChecked();
  });

  it('renders rows that when clicked navigates to component details page - violations section', async () => {
    renderComponent();

    expect(axiosMock.history.get.length).toBe(1);
    expect(axiosMock.history.get[0].params).toEqual({
      pageSize: DEFAULT_PAGE_SIZE,
      page: 1,
      componentNameFilter: '',
      filterOnPolicyActions: false,
    });

    const table = await screen.findByRole('table');
    expect(table).toBeInTheDocument();

    const rows = screen.getAllByRole('row');
    expect(rows.length).toBe(16);

    // 1st row is header row, 2nd row is the first component row
    const firstComponentRow = rows[1];
    const firstComponentHash = mockResponsePage1.priorities.results[0].componentHash;

    const secondComponentRow = rows[2];
    const secondComponentHash = mockResponsePage1.priorities.results[1].componentHash;

    fireEvent.click(firstComponentRow);
    expect(stateGoSpy).toHaveBeenCalledWith(
      'componentDetailsPageWithinPrioritiesPageContainerFromReports.componentDetails.overview',
      {
        hash: firstComponentHash,
        publicId: publicAppId,
        scanId,
      }
    );

    fireEvent.click(secondComponentRow);
    expect(stateGoSpy).toHaveBeenCalledWith(
      'componentDetailsPageWithinPrioritiesPageContainerFromReports.componentDetails.overview',
      {
        hash: secondComponentHash,
        publicId: publicAppId,
        scanId,
      }
    );
  });

  describe('pagination', () => {
    it('renders a pagination section', async () => {
      renderComponent();
      const paginationBtnBar = await screen.findByRole('navigation');
      expect(paginationBtnBar).toBeInTheDocument();
    });

    it('makes correct network requests when page is changed', async () => {
      renderComponent();
      expect(axiosMock.history.get.length).toBe(1);
      expect(axiosMock.history.get[0].params).toEqual({
        pageSize: DEFAULT_PAGE_SIZE,
        page: 1,
        componentNameFilter: '',
        filterOnPolicyActions: false,
      });

      const table = await screen.findByRole('table');
      expect(table).toBeInTheDocument();

      let pagination = await screen.findByRole('navigation');
      expect(within(pagination).getAllByRole('button').length).toBe(3);

      const nextPageBtn = within(pagination).getByRole('button', { name: /goto next page/i });
      expect(nextPageBtn).toBeInTheDocument();

      fireEvent.click(nextPageBtn);

      expect(axiosMock.history.get.length).toBe(17);
      expect(axiosMock.history.get[16].url).toBe(getPrioritiesPageTableData(publicAppId, scanId));
      expect(axiosMock.history.get[16].params).toEqual({
        pageSize: DEFAULT_PAGE_SIZE,
        page: 2,
        componentNameFilter: '',
        filterOnPolicyActions: false,
      });

      await screen.findByRole('table');
      pagination = await screen.findByRole('navigation');

      const prevPageBtn = within(pagination).getByRole('button', { name: /goto previous page/i });
      expect(prevPageBtn).toBeInTheDocument();

      fireEvent.click(prevPageBtn);

      expect(axiosMock.history.get.length).toBe(33);
      expect(axiosMock.history.get[32].url).toBe(getPrioritiesPageTableData(publicAppId, scanId));
      expect(axiosMock.history.get[32].params).toEqual({
        pageSize: DEFAULT_PAGE_SIZE,
        page: 1,
        componentNameFilter: '',
        filterOnPolicyActions: false,
      });
    });
  });

  describe('makes correct network requests and sets filter defaults when integrationType is provided', () => {
    validIntegrationTypes.forEach((integrationType) => {
      it(`when integration=${integrationType}`, async () => {
        const defaultPolicyActionFilterState = defaultIntegrationParamsMap[integrationType].filterOnPolicyActions;
        const routerState = {
          router: {
            currentParams: {
              publicAppId,
              scanId,
              filterOnPolicyActions: false,
              componentNameFilter: '',
              integrationType,
            },
            currentState: {
              name: 'prioritiesPageFromIntegrations',
            },
          },
        };

        renderComponent(routerState);
        expect(axiosMock.history.get.length).toEqual(1);
        expect(axiosMock.history.get[0].params).toEqual({
          pageSize: DEFAULT_PAGE_SIZE,
          page: 1,
          componentNameFilter: '',
          filterOnPolicyActions: defaultPolicyActionFilterState,
        });

        expect(stateGoSpy).toHaveBeenCalledWith('prioritiesPageFromIntegrations', {
          publicAppId,
          scanId,
          filterOnPolicyActions: defaultPolicyActionFilterState ? true : '',
          componentNameFilter: '',
          integrationType,
        });
      });
    });
  });
});

function generateMockData(numOfPriorities) {
  const priorities = [];

  for (let i = 1; i <= numOfPriorities; i++) {
    const hasFail = faker.datatype.boolean();
    priorities.push({
      displayName: faker.lorem.word(),
      componentHash: faker.git.commitSha(),
      dependencyType: faker.helpers.arrayElement(['Direct', 'Transitive', 'Inner Source']),
      hasFailActionOnComponent: hasFail,
      action: hasFail ? 'fail' : faker.helpers.arrayElement(['none', 'warn']),
      highestThreat: faker.datatype.number({ min: 0, max: 10 }),
      highestThreatPolicyName: faker.lorem.slug(),
      highestThreatPolicyConstraintName: faker.lorem.sentence(),
      priority: i,
      securityReachable: faker.helpers.arrayElement([true, false, null]),
    });
  }

  return priorities;
}

function generateMockResponseByPage(page, mockData) {
  return {
    priorities: {
      total: mockData.length,
      page,
      pageSize: DEFAULT_PAGE_SIZE,
      pageCount: Math.floor(NUM_OF_RESULTS / DEFAULT_PAGE_SIZE),
      results: mockData.slice((page - 1) * DEFAULT_PAGE_SIZE, page * DEFAULT_PAGE_SIZE),
    },
  };
}
