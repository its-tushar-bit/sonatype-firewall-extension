/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import userEvent from '@testing-library/user-event';
import { faker } from '@faker-js/faker';
import { mergeDeepRight } from 'ramda';
import { render, screen, within, axiosMockAdapter, waitFor, act } from 'TestRoot/SpecUtil';
import PrioritiesPageTable from 'MainRoot/development/prioritiesPage/PrioritiesPageTable';
import * as RouterActions from 'MainRoot/reduxUiRouter/routerActions';
import { getCompositeSourceControlUrl, getPrioritiesPageTableData } from 'MainRoot/util/CLMLocation';
import * as RouterStateContextModule from 'MainRoot/react/RouterStateContext';
import { defaultIntegrationParamsMap, validIntegrationTypes } from 'MainRoot/development/prioritiesPage/utils';

const appId = 'a1950e2a897240d4878ddb2450d64e10';
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
    applicationReport: {
      metadata: {
        application: {
          id: appId,
        },
      },
    },
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

  const mockRouterState = {
    get: () => ({}),
    href: () => '#',
  };

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    stateGoSpy = jest.spyOn(RouterActions, 'stateGo');
    jest.spyOn(RouterStateContextModule, 'useRouterState').mockImplementation(() => mockRouterState);

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

  it('makes correct network request', async () => {
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

    await waitFor(() => {
      expect(loading).not.toBeInTheDocument();
    });
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
    const user = userEvent.setup();
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
    await user.click(retryBtn);

    expect(axiosMock.history.get.length).toBe(2);
    expect(axiosMock.history.get[1].url).toBe(getPrioritiesPageTableData(publicAppId, scanId));
    expect(axiosMock.history.get[1].params).toEqual({
      pageSize: DEFAULT_PAGE_SIZE,
      page: 1,
      componentNameFilter: '',
      filterOnPolicyActions: false,
    });
  });

  it('renders column headers with correct names in the correct order', async () => {
    renderComponent(
      mergeDeepRight(defaultPreloadedState, {
        productFeatures: {
          productFeatures: {
            'manual-pull-requests': true,
          },
        },
      })
    );

    const table = await screen.findByRole('table');
    expect(table).toBeInTheDocument();

    const columnHeaders = within(table).getAllByRole('columnheader');
    expect(columnHeaders).toHaveLength(6);
    expect(columnHeaders[0]).toHaveAccessibleName(/priority/i);
    expect(columnHeaders[1]).toHaveAccessibleName(/component/i);
    expect(columnHeaders[2]).toHaveAccessibleName(/build action/i);
    expect(columnHeaders[3]).toHaveAccessibleName(/reachability/i);
    expect(columnHeaders[4]).toHaveAccessibleName(/suggested remediation/i);
    expect(columnHeaders[5]).toHaveAccessibleName(/next step/i);
  });

  it('renders the priority column header with an icon and tooltip', async () => {
    renderComponent();
    const user = userEvent.setup();

    const table = await screen.findByRole('table');
    expect(table).toBeInTheDocument();

    const priorityColumnHeader = screen.getAllByRole('columnheader')[0];

    const infoIcon = within(priorityColumnHeader).getByRole('img', { hidden: true });
    expect(infoIcon).toBeInTheDocument();

    await user.hover(infoIcon);
    const tooltip = await screen.findByRole('tooltip', {
      name:
        'Priority of actionable items based on the policy action, component reachability status, and threat score severity.',
    });
    expect(tooltip).toBeInTheDocument();
  });

  describe('branch name loading', () => {
    async function assertBranchNameIsLoaded(store, branchName) {
      const table = screen.getByRole('table');
      expect(table).toBeInTheDocument();

      const loading = within(table).getByText('Loading…');
      expect(loading).toBeInTheDocument();

      await waitFor(() => {
        expect(loading).not.toBeInTheDocument();
      });

      const state = store.getState();
      expect(state.prioritiesPage.branchName).toBe(branchName);
    }

    it('loads branch name from source control config base branch value', async () => {
      const branchName = 'custom-branch-name';
      axiosMock.onGet(getCompositeSourceControlUrl('application', appId)).reply(200, {
        baseBranch: {
          value: branchName,
          parentValue: null,
        },
      });

      const { store } = renderComponent();
      await assertBranchNameIsLoaded(store, branchName);
    });

    it('loads branch name from source control config base branch parentValue', async () => {
      const branchName = 'custom-branch-name';
      axiosMock.onGet(getCompositeSourceControlUrl('application', appId)).reply(200, {
        baseBranch: {
          value: null,
          parentValue: branchName,
        },
      });

      const { store } = renderComponent();
      await assertBranchNameIsLoaded(store, branchName);
    });

    it('fetches repository default branch from API', async () => {
      const apiBranchName = 'main';
      const reportBranchName = 'feature-branch';

      axiosMock.onGet(getCompositeSourceControlUrl('application', appId)).reply(200, {
        baseBranch: {
          value: apiBranchName,
          parentValue: null,
        },
      });

      const { store } = renderComponent(
        mergeDeepRight(defaultPreloadedState, {
          applicationReport: {
            metadata: {
              branchName: reportBranchName,
            },
          },
        })
      );

      await assertBranchNameIsLoaded(store, apiBranchName);

      expect(axiosMock.history.get.some((req) => req.url === getCompositeSourceControlUrl('application', appId))).toBe(
        true
      );
    });
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
    beforeEach(() => {
      axiosMock.onGet(getCompositeSourceControlUrl('application', appId)).reply(200, {
        baseBranch: {
          value: 'custom-branch-name',
        },
      });
    });

    it('filters components by name', async () => {
      jest.useFakeTimers();
      const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
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
      await user.clear(filterInput);
      await user.type(filterInput, 'ABC');

      act(() => {
        jest.runAllTimers();
      });

      expect(axiosMock.history.get.length).toEqual(17); // 1 branch name loading + 1 initial request + 15 async recommendation requests
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

      // wait for table in order to avoid act() warnings
      await screen.findByRole('table');
    });
  });

  describe('component action filter toggle', () => {
    it('toggles the "Fail/Warn Policy Actions only" filter and makes correct network requests', async () => {
      renderComponent();
      const user = userEvent.setup();

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

      await user.click(toggle);

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

  describe('pagination', () => {
    beforeEach(() => {
      axiosMock.onGet(getCompositeSourceControlUrl('application', appId)).reply(200, {
        baseBranch: {
          value: 'custom-branch-name',
        },
      });
    });

    it('renders a pagination section', async () => {
      renderComponent();
      const paginationBtnBar = await screen.findByRole('navigation');
      expect(paginationBtnBar).toBeInTheDocument();
    });

    it('makes correct network requests when page is changed', async () => {
      const user = userEvent.setup();
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
      expect(within(pagination).getAllByRole('button')).toHaveLength(3);

      const nextPageBtn = within(pagination).getByRole('button', { name: /goto next page/i });
      expect(nextPageBtn).toBeInTheDocument();

      // allVersions call for each row in first page + branch name call
      expect(axiosMock.history.get).toHaveLength(17);
      for (let i = 2; i < 17; i++) {
        expect(axiosMock.history.get[i].url).toMatch(
          /^\/rest\/ci\/componentDetails\/application\/testPublicAppId\/allVersions/
        );
      }

      await user.click(nextPageBtn);

      expect(axiosMock.history.get).toHaveLength(33);
      expect(axiosMock.history.get[17].params).toEqual({
        pageSize: DEFAULT_PAGE_SIZE,
        page: 2,
        componentNameFilter: '',
        filterOnPolicyActions: false,
      });

      // allVersions call for each row in second page page
      for (let i = 18; i < 33; i++) {
        expect(axiosMock.history.get[i].url).toMatch(
          /^\/rest\/ci\/componentDetails\/application\/testPublicAppId\/allVersions/
        );
      }

      await screen.findByRole('table');
      pagination = await screen.findByRole('navigation');

      const prevPageBtn = within(pagination).getByRole('button', { name: /goto previous page/i });
      expect(prevPageBtn).toBeInTheDocument();

      await user.click(prevPageBtn);

      expect(axiosMock.history.get).toHaveLength(49);
      expect(axiosMock.history.get[33].params).toEqual({
        pageSize: DEFAULT_PAGE_SIZE,
        page: 1,
        componentNameFilter: '',
        filterOnPolicyActions: false,
      });

      // allVersions call for each row in first row page, again
      for (let i = 34; i < 49; i++) {
        expect(axiosMock.history.get[i].url).toMatch(
          /^\/rest\/ci\/componentDetails\/application\/testPublicAppId\/allVersions/
        );
      }
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

        // wait for table in order to avoid act() warnings
        await screen.findByRole('table');
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
