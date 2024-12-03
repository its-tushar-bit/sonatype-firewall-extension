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
      },
    },
  };

  beforeEach(() => {
    axiosMock = axiosMockAdapter();

    stateGoSpy = jest.spyOn(RouterActions, 'stateGo');

    axiosMock
      .onGet(getPrioritiesPageTableData(publicAppId, scanId), {
        params: { pageSize: DEFAULT_PAGE_SIZE, page: 1, optionalComponentNameFilter: '', optionalActionFilter: true },
      })
      .reply(200, mockResponsePage1);
    axiosMock
      .onGet(getPrioritiesPageTableData(publicAppId, scanId), {
        params: { pageSize: DEFAULT_PAGE_SIZE, page: 2, optionalComponentNameFilter: '', optionalActionFilter: true },
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
      optionalComponentNameFilter: '',
      optionalActionFilter: true,
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
        params: { pageSize: DEFAULT_PAGE_SIZE, page: 1, optionalComponentNameFilter: '', optionalActionFilter: true },
      })
      .reply(500, 'Error');

    renderComponent();

    const table = await screen.findByRole('table');
    expect(table).toBeInTheDocument();

    const alert = within(table).getByRole('alert');
    expect(alert).toBeInTheDocument();
  });

  it('clicking the retry button on error alert makes correct network request', async () => {
    axiosMock
      .onGet(getPrioritiesPageTableData(publicAppId, scanId), {
        params: { pageSize: DEFAULT_PAGE_SIZE, page: 1, optionalComponentNameFilter: '', optionalActionFilter: true },
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
      optionalComponentNameFilter: '',
      optionalActionFilter: true,
    });

    const retryBtn = within(table).getByRole('button');
    fireEvent.click(retryBtn);

    expect(axiosMock.history.get.length).toBe(2);
    expect(axiosMock.history.get[1].url).toBe(getPrioritiesPageTableData(publicAppId, scanId));
    expect(axiosMock.history.get[1].params).toEqual({
      pageSize: DEFAULT_PAGE_SIZE,
      page: 1,
      optionalComponentNameFilter: '',
      optionalActionFilter: true,
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
        "Priority of actionable items based on this application's policy, component reachability status, recommendation availability, and threat score severity.",
    });
    expect(tooltip).toBeInTheDocument();
  });

  describe('empty message when there are no priorities', () => {
    it('renders the right message by default (when fail/warn toggle is on)', async () => {
      const numOfPriorities = 0;
      const mockData = generateMockData(numOfPriorities);
      const mockResponsePage1 = generateMockResponseByPage(1, mockData);
      axiosMock
        .onGet(getPrioritiesPageTableData(publicAppId, scanId), {
          params: { pageSize: DEFAULT_PAGE_SIZE, page: 1, optionalComponentNameFilter: '', optionalActionFilter: true },
        })
        .reply(200, mockResponsePage1);

      renderComponent();

      const table = await screen.findByRole('table');
      expect(table).toBeInTheDocument();

      const rows = screen.getAllByRole('row');
      expect(rows.length).toBe(2);

      const rowWithMessage = rows[1];
      expect(rowWithMessage).toHaveTextContent(
        'No violations with Fail/Warn policy actions were found during this evaluation.'
      );
    });

    it('renders the right message (when fail/warn toggle is off)', async () => {
      const numOfPriorities = 0;
      const mockData = generateMockData(numOfPriorities);
      const mockResponsePage1 = generateMockResponseByPage(1, mockData);
      axiosMock
        .onGet(getPrioritiesPageTableData(publicAppId, scanId), {
          params: {
            pageSize: DEFAULT_PAGE_SIZE,
            page: 1,
            optionalComponentNameFilter: '',
            optionalActionFilter: false,
          },
        })
        .reply(200, mockResponsePage1);

      renderComponent();

      const table = await screen.findByRole('table');
      expect(table).toBeInTheDocument();

      const toggle = screen.getByRole('switch', { name: 'Fail/Warn Policy Actions only' });
      expect(toggle).toBeInTheDocument();
      expect(toggle).toBeChecked();

      fireEvent.click(toggle);
      await screen.findByRole('table');

      const rows = screen.getAllByRole('row');
      expect(rows.length).toBe(2);

      const rowWithMessage = rows[1];
      expect(rowWithMessage).toHaveTextContent('All clear! No violations were found during this evaluation.');
    });
  });

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
            securityReachable: faker.datatype.boolean(),
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
            securityReachable: faker.datatype.boolean(),
          },
        ],
      },
    };

    axiosMock
      .onGet(getPrioritiesPageTableData(publicAppId, scanId), {
        params: { pageSize: DEFAULT_PAGE_SIZE, page: 1, optionalComponentNameFilter: 'ABC' },
      })
      .reply(200, filteredResponse);

    renderComponent();
    expect(axiosMock.history.get.length).toEqual(1);
    expect(axiosMock.history.get[0].params).toEqual({
      pageSize: DEFAULT_PAGE_SIZE,
      page: 1,
      optionalComponentNameFilter: '',
      optionalActionFilter: true,
    });

    const table = await screen.findByRole('table');
    expect(table).toBeInTheDocument();

    const filterInput = screen.getByPlaceholderText('Filter by component');
    fireEvent.change(filterInput, { target: { value: 'ABC' } });

    jest.runAllTimers();

    expect(axiosMock.history.get.length).toEqual(17); // 1 initial request + 15 async recommendation requests + 1 filtered request

    expect(axiosMock.history.get[16].params).toEqual({
      pageSize: DEFAULT_PAGE_SIZE,
      page: 1,
      optionalComponentNameFilter: 'ABC',
      optionalActionFilter: true,
    });
  });

  it('toggles the "Fail/Warn Policy Actions only" filter and makes correct network requests', async () => {
    renderComponent();

    const defaultParams = {
      pageSize: DEFAULT_PAGE_SIZE,
      page: 1,
      optionalComponentNameFilter: '',
      optionalActionFilter: true,
    };

    expect(axiosMock.history.get.length).toBe(1);
    expect(axiosMock.history.get[0].url).toBe(getPrioritiesPageTableData(publicAppId, scanId));
    expect(axiosMock.history.get[0].params).toEqual(defaultParams);

    const table = await screen.findByRole('table');
    expect(table).toBeInTheDocument();

    const toggle = screen.getByRole('switch', { name: 'Fail/Warn Policy Actions only' });
    expect(toggle).toBeInTheDocument();
    expect(toggle).toBeChecked();

    fireEvent.click(toggle);
    expect(axiosMock.history.get.length).toBe(17);
    expect(axiosMock.history.get[16].url).toBe(getPrioritiesPageTableData(publicAppId, scanId));
    expect(axiosMock.history.get[16].params).toEqual({
      ...defaultParams,
      optionalActionFilter: false,
    });
    expect(toggle).not.toBeChecked();
  });

  it('renders rows that when clicked navigates to component details page - violations section', async () => {
    renderComponent();

    expect(axiosMock.history.get.length).toBe(1);
    expect(axiosMock.history.get[0].params).toEqual({
      pageSize: DEFAULT_PAGE_SIZE,
      page: 1,
      optionalComponentNameFilter: '',
      optionalActionFilter: true,
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
    expect(stateGoSpy).toHaveBeenCalledWith('prioritiesPageContainer.componentDetails.overview', {
      hash: firstComponentHash,
      publicId: publicAppId,
      scanId,
    });

    fireEvent.click(secondComponentRow);
    expect(stateGoSpy).toHaveBeenCalledWith('prioritiesPageContainer.componentDetails.overview', {
      hash: secondComponentHash,
      publicId: publicAppId,
      scanId,
    });
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
        optionalComponentNameFilter: '',
        optionalActionFilter: true,
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
        optionalComponentNameFilter: '',
        optionalActionFilter: true,
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
        optionalComponentNameFilter: '',
        optionalActionFilter: true,
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
      securityReachable: faker.datatype.boolean(),
    });
  }

  return priorities;
}

function generateMockResponseByPage(page, mockData) {
  return {
    priorities: {
      total: NUM_OF_RESULTS,
      page,
      pageSize: DEFAULT_PAGE_SIZE,
      pageCount: Math.floor(NUM_OF_RESULTS / DEFAULT_PAGE_SIZE),
      results: mockData.slice((page - 1) * DEFAULT_PAGE_SIZE, page * DEFAULT_PAGE_SIZE),
    },
  };
}
