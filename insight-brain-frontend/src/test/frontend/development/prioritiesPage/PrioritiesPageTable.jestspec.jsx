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

const NUM_OF_RESULTS_TOP_PRIORITIES = 3;
const NUM_OF_RESULTS_ADDITIONAL_PRIORITIES = 20;
const PAGE_SIZE = 10;

const mockData = generateMockData(NUM_OF_RESULTS_TOP_PRIORITIES, NUM_OF_RESULTS_ADDITIONAL_PRIORITIES);
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
    prioritiesPage: {
      topPrioritiesData: null,
      additionalPrioritiesData: null,
      loadingTableData: false,
      loadErrorTableData: null,
      loadingMetadata: false,
      loadErrorMetaData: null,
      recommendations: {},
      pageSize: 10,
      pageCount: 1,
      page: 1,
      total: null,
      optionalComponentNameFilter: '',
    },
  };

  beforeEach(() => {
    axiosMock = axiosMockAdapter();

    stateGoSpy = jest.spyOn(RouterActions, 'stateGo');

    renderComponent = (preloadedState) =>
      render(<PrioritiesPageTable />, { preloadedState: preloadedState || defaultPreloadedState });

    axiosMock
      .onGet(getPrioritiesPageTableData(publicAppId, scanId), {
        params: { pageSize: 10, page: 1, optionalComponentNameFilter: '' },
      })
      .reply(200, mockResponsePage1);
    axiosMock
      .onGet(getPrioritiesPageTableData(publicAppId, scanId), {
        params: { pageSize: 10, page: 2, optionalComponentNameFilter: '' },
      })
      .reply(200, mockResponsePage2);
  });

  it('makes correct network request', () => {
    renderComponent();

    expect(axiosMock.history.get.length).toBe(1);
    expect(axiosMock.history.get[0].url).toBe(getPrioritiesPageTableData(publicAppId, scanId));
    expect(axiosMock.history.get[0].params).toEqual({ pageSize: 10, page: 1, optionalComponentNameFilter: '' });

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
        params: { pageSize: 10, page: 1, optionalComponentNameFilter: '' },
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
        params: { pageSize: 10, page: 1, optionalComponentNameFilter: '' },
      })
      .reply(500, 'Error');

    renderComponent();

    const table = await screen.findByRole('table');
    expect(table).toBeInTheDocument();

    expect(axiosMock.history.get.length).toBe(1);
    expect(axiosMock.history.get[0].url).toBe(getPrioritiesPageTableData(publicAppId, scanId));
    expect(axiosMock.history.get[0].params).toEqual({ pageSize: 10, page: 1, optionalComponentNameFilter: '' });

    const buttons = within(table).getAllByRole('button');
    expect(buttons.length).toBe(2);
    const retryBtn = buttons[1];
    fireEvent.click(retryBtn);

    expect(axiosMock.history.get.length).toBe(2);
    expect(axiosMock.history.get[1].url).toBe(getPrioritiesPageTableData(publicAppId, scanId));
    expect(axiosMock.history.get[1].params).toEqual({ pageSize: 10, page: 1, optionalComponentNameFilter: '' });
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
    expect(columnHeaders[2]).toHaveAccessibleName(/policy/i);
    expect(columnHeaders[3]).toHaveAccessibleName(/recommendation/i);
  });

  it('renders the priority column header with an icon and tooltip', async () => {
    renderComponent();

    const table = await screen.findByRole('table');
    expect(table).toBeInTheDocument();

    const priorityColumnHeader = screen.getByRole('columnheader', { name: /priority/i });

    const infoIcon = within(priorityColumnHeader).getByRole('img', { hidden: true });
    expect(infoIcon).toBeInTheDocument();

    fireEvent.mouseOver(infoIcon);
    const tooltip = await screen.findByRole('tooltip', {
      name:
        "Priority of actionable items based on this application's policy, component reachability status, recommendation availability, and threat score severity.",
    });
    expect(tooltip).toBeInTheDocument();
  });

  it('renders a "No violations" message if number of priorities is 0', async () => {
    const numOfTopPriorities = 0;
    const numOfAdditionalPriorities = 0;
    const mockData = generateMockData(numOfTopPriorities, numOfAdditionalPriorities);
    const mockResponsePage1 = generateMockResponseByPage(1, mockData);
    axiosMock
      .onGet(getPrioritiesPageTableData(publicAppId, scanId), {
        params: { pageSize: 10, page: 1, optionalComponentNameFilter: '' },
      })
      .reply(200, mockResponsePage1);

    renderComponent();

    const table = await screen.findByRole('table');
    expect(table).toBeInTheDocument();

    const rows = screen.getAllByRole('row');
    expect(rows.length).toBe(3);

    const rowWithMessage = rows[2];
    expect(rowWithMessage).toHaveTextContent('All clear! No violations were found during this evaluation.');
  });

  it('filters components by name', async () => {
    jest.useFakeTimers();
    const filteredResponse = {
      topPriorities: [
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
      additionalPriorities: [
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
    };

    axiosMock
      .onGet(getPrioritiesPageTableData(publicAppId, scanId), {
        params: { pageSize: 10, page: 1, optionalComponentNameFilter: 'ABC' },
      })
      .reply(200, filteredResponse);

    renderComponent();
    expect(axiosMock.history.get.length).toEqual(1);
    expect(axiosMock.history.get[0].params).toEqual({ pageSize: 10, page: 1, optionalComponentNameFilter: '' });

    const table = await screen.findByRole('table');
    expect(table).toBeInTheDocument();

    const filterInput = screen.getByPlaceholderText('component name');
    fireEvent.change(filterInput, { target: { value: 'ABC' } });

    jest.runAllTimers();

    expect(axiosMock.history.get.length).toEqual(15); // 1 initial request + 13 async recommendation requests + 1 filtered request

    expect(axiosMock.history.get[14].params).toEqual({ pageSize: 10, page: 1, optionalComponentNameFilter: 'ABC' });
  });

  describe('Top Priorities Accordion', () => {
    it('renders an accordion with the title "Top Priorities"', async () => {
      const numOfTopPriorities = 1;
      const numOfAdditionalPriorities = 10;
      const mockData = generateMockData(numOfTopPriorities, numOfAdditionalPriorities);
      const mockResponsePage1 = generateMockResponseByPage(1, mockData);
      axiosMock
        .onGet(getPrioritiesPageTableData(publicAppId, scanId), {
          params: { pageSize: 10, page: 1, optionalComponentNameFilter: '' },
        })
        .reply(200, mockResponsePage1);

      renderComponent();

      const table = await screen.findByRole('table');
      expect(table).toBeInTheDocument();

      const accordions = screen.getAllByRole('group');
      const topPrioritiesAccordion = accordions[0];

      expect(within(topPrioritiesAccordion).getByRole('button')).toHaveAccessibleName(`Top Priorities`);
    });

    it('renders an open accordion with title "Top Priorities"', async () => {
      renderComponent();

      const table = await screen.findByRole('table');
      expect(table).toBeInTheDocument();

      const topPrioritiesAccordion = screen.getByRole('group');

      expect(topPrioritiesAccordion).toHaveAttribute('aria-expanded', 'true');

      expect(within(topPrioritiesAccordion).getByRole('button')).toHaveAccessibleName(`Top Priorities`);
    });

    it('hides the priority rows when clicked on accordion', async () => {
      renderComponent();

      const table = await screen.findByRole('table');
      expect(table).toBeInTheDocument();

      let rows = screen.getAllByRole('row');
      expect(rows.length).toBe(17);

      const accordions = screen.getAllByRole('group');

      const topPrioritiesAccordion = accordions[0];
      const topPrioritiesAccordionTitle = within(topPrioritiesAccordion).getByRole('button');

      expect(topPrioritiesAccordion).toHaveAttribute('aria-expanded', 'true');

      fireEvent.click(topPrioritiesAccordionTitle);

      expect(topPrioritiesAccordion).toHaveAttribute('aria-expanded', 'false');

      rows = screen.getAllByRole('row');
      expect(rows.length).toBe(14);

      fireEvent.click(topPrioritiesAccordionTitle);

      expect(topPrioritiesAccordion).toHaveAttribute('aria-expanded', 'true');

      rows = screen.getAllByRole('row');
      expect(rows.length).toBe(17);
    });

    it('hides the accordion when not on first page', async () => {
      renderComponent();

      const table = await screen.findByRole('table');
      expect(table).toBeInTheDocument();

      let pagination = await screen.findByRole('navigation');
      expect(within(pagination).getAllByRole('button').length).toBe(3);

      expect(screen.getByRole('group')).toBeInTheDocument();

      const nextPageBtn = within(pagination).getByRole('button', { name: /goto next page/i });
      expect(nextPageBtn).toBeInTheDocument();

      fireEvent.click(nextPageBtn);

      await screen.findByRole('table');

      expect(screen.queryByRole('group')).not.toBeInTheDocument();

      pagination = await screen.findByRole('navigation');

      const prevPageBtn = within(pagination).getByRole('button', { name: /goto previous page/i });
      expect(prevPageBtn).toBeInTheDocument();

      fireEvent.click(prevPageBtn);

      await screen.findByRole('table');

      expect(screen.getByRole('group')).toBeInTheDocument();
    });
  });

  it('renders a "Remaining Priorities" row ', async () => {
    renderComponent();

    const table = await screen.findByRole('table');
    expect(table).toBeInTheDocument();

    //table header row + component filter row + top priorities header row + priority rows
    const remainingFindingsRowNumber = 1 + 1 + 1 + NUM_OF_RESULTS_TOP_PRIORITIES;
    expect(within(table).getAllByRole('row')[remainingFindingsRowNumber]).toHaveTextContent(/remaining priorities/i);
  });

  it('renders rows that when clicked navigates to component details page - violations section', async () => {
    renderComponent();

    const table = await screen.findByRole('table');
    expect(table).toBeInTheDocument();

    const rows = screen.getAllByRole('row');
    // 1st row is header row, 2nd row is component filter, 3nd row is Top Priorities row, 4th row is the first component row
    const firstComponentRow = rows[3];
    const firstComponentHash = mockResponsePage1.topPriorities[0].componentHash;

    const secondComponentRow = rows[4];
    const secondComponentHash = mockResponsePage1.topPriorities[1].componentHash;

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
      expect(axiosMock.history.get[0].params).toEqual({ pageSize: 10, page: 1, optionalComponentNameFilter: '' });

      const table = await screen.findByRole('table');
      expect(table).toBeInTheDocument();

      let pagination = await screen.findByRole('navigation');
      expect(within(pagination).getAllByRole('button').length).toBe(3);

      const nextPageBtn = within(pagination).getByRole('button', { name: /goto next page/i });
      expect(nextPageBtn).toBeInTheDocument();

      fireEvent.click(nextPageBtn);

      expect(axiosMock.history.get.length).toBe(15);
      expect(axiosMock.history.get[14].url).toBe(getPrioritiesPageTableData(publicAppId, scanId));
      expect(axiosMock.history.get[14].params).toEqual({ pageSize: 10, page: 2, optionalComponentNameFilter: '' });

      await screen.findByRole('table');
      pagination = await screen.findByRole('navigation');

      const prevPageBtn = within(pagination).getByRole('button', { name: /goto previous page/i });
      expect(prevPageBtn).toBeInTheDocument();

      fireEvent.click(prevPageBtn);

      expect(axiosMock.history.get.length).toBe(23);
      expect(axiosMock.history.get[22].url).toBe(getPrioritiesPageTableData(publicAppId, scanId));
      expect(axiosMock.history.get[22].params).toEqual({ pageSize: 10, page: 1, optionalComponentNameFilter: '' });
    });
  });
});

function generateMockData(numOfTopPriorities, numOfAdditionalPriorities) {
  const topPriorities = [];
  const additionalPriorities = [];

  const createEntry = (list, index) => {
    const hasFail = faker.datatype.boolean();
    list.push({
      displayName: faker.lorem.word(1),
      componentHash: faker.git.commitSha(),
      dependencyType: faker.helpers.arrayElement(['Direct', 'Transitive', 'Inner Source']),
      hasFailActionOnComponent: hasFail,
      action: hasFail ? 'fail' : faker.helpers.arrayElement(['none', 'warn']),
      highestThreat: faker.datatype.number({ min: 0, max: 10 }),
      highestThreatPolicyName: faker.lorem.slug(),
      highestThreatPolicyConstraintName: faker.lorem.sentence(),
      priority: index,
      securityReachable: faker.datatype.boolean(),
    });
  };

  for (let i = 1; i <= numOfTopPriorities; i++) {
    createEntry(topPriorities, i);
  }

  for (let i = numOfTopPriorities + 1; i <= numOfAdditionalPriorities; i++) {
    createEntry(additionalPriorities, i);
  }

  return {
    topPriorities,
    additionalPriorities,
  };
}

function generateMockResponseByPage(page, mockData) {
  const { topPriorities, additionalPriorities } = mockData;

  return {
    topPriorities,
    additionalPriorities: {
      total: NUM_OF_RESULTS_ADDITIONAL_PRIORITIES,
      page,
      pageSize: PAGE_SIZE,
      pageCount: Math.floor(NUM_OF_RESULTS_ADDITIONAL_PRIORITIES / PAGE_SIZE),
      results: additionalPriorities.slice((page - 1) * 10, page * 10),
    },
  };
}
