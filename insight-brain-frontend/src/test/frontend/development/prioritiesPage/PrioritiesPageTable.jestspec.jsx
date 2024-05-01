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

const mockData = generateMockData();
const mockResponsePage1 = generateMockResponseByPage(1);
const mockResponsePage2 = generateMockResponseByPage(2);

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

    renderComponent = (preloadedState) =>
      render(<PrioritiesPageTable />, { preloadedState: preloadedState || defaultPreloadedState });

    axiosMock
      .onGet(getPrioritiesPageTableData(publicAppId, scanId), { params: { pageSize: 10, page: 1 } })
      .reply(200, mockResponsePage1);
    axiosMock
      .onGet(getPrioritiesPageTableData(publicAppId, scanId), { params: { pageSize: 10, page: 2 } })
      .reply(200, mockResponsePage2);
  });

  it('makes correct network request', () => {
    renderComponent();

    expect(axiosMock.history.get.length).toBe(1);
    expect(axiosMock.history.get[0].url).toBe(getPrioritiesPageTableData(publicAppId, scanId));
    expect(axiosMock.history.get[0].params).toEqual({ pageSize: 10, page: 1 });

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
      .onGet(getPrioritiesPageTableData(publicAppId, scanId), { params: { pageSize: 10, page: 1 } })
      .reply(500, 'Error');

    renderComponent();

    const table = await screen.findByRole('table');
    expect(table).toBeInTheDocument();

    const alert = within(table).getByRole('alert');
    expect(alert).toBeInTheDocument();
  });

  it('clicking the retry button on error alert makes correct network request', async () => {
    axiosMock
      .onGet(getPrioritiesPageTableData(publicAppId, scanId), { params: { pageSize: 10, page: 1 } })
      .reply(500, 'Error');

    renderComponent();

    const table = await screen.findByRole('table');
    expect(table).toBeInTheDocument();

    expect(axiosMock.history.get.length).toBe(1);
    expect(axiosMock.history.get[0].url).toBe(getPrioritiesPageTableData(publicAppId, scanId));
    expect(axiosMock.history.get[0].params).toEqual({ pageSize: 10, page: 1 });

    const retryBtn = within(table).getByRole('button');
    fireEvent.click(retryBtn);

    expect(axiosMock.history.get.length).toBe(2);
    expect(axiosMock.history.get[1].url).toBe(getPrioritiesPageTableData(publicAppId, scanId));
    expect(axiosMock.history.get[1].params).toEqual({ pageSize: 10, page: 1 });
  });

  it('renders a table with 4 column headers', async () => {
    renderComponent();

    const table = await screen.findByRole('table');
    expect(table).toBeInTheDocument();

    const columnheaders = within(table).getAllByRole('columnheader');
    expect(columnheaders.length).toBe(4 + 1); //last column is to render chevron icon for clickable rows
  });

  it('renders column headers with correct names in the correct order', async () => {
    renderComponent();

    const table = await screen.findByRole('table');
    expect(table).toBeInTheDocument();

    const columnHeaders = within(table).getAllByRole('columnheader');
    expect(columnHeaders[0]).toHaveAccessibleName(/priority/i);
    expect(columnHeaders[1]).toHaveAccessibleName(/component/i);
    expect(columnHeaders[2]).toHaveAccessibleName(/highest policy threat/i);
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
      name: 'Some title', //TODO change later
    });
    expect(tooltip).toBeInTheDocument();
  });

  describe('accordions', () => {
    it('renders 2 open accordions with title "Top Priorities" and "All Other Findings"', async () => {
      renderComponent();

      const table = await screen.findByRole('table');
      expect(table).toBeInTheDocument();

      const accordions = screen.getAllByRole('group');
      expect(accordions).toHaveLength(2);

      const topPrioritiesAccordion = accordions[0];
      const allFindingsAccordion = accordions[1];

      expect(topPrioritiesAccordion).toHaveAttribute('aria-expanded', 'true');
      expect(allFindingsAccordion).toHaveAttribute('aria-expanded', 'true');

      expect(within(topPrioritiesAccordion).getByRole('button')).toHaveAccessibleName(/top priorities/i);
      expect(within(allFindingsAccordion).getByRole('button')).toHaveAccessibleName(/all other findings/i);
    });

    it('"Top Priorities" accordion when clicked hides the priority rows', async () => {
      renderComponent();

      const table = await screen.findByRole('table');
      expect(table).toBeInTheDocument();

      let rows = screen.getAllByRole('row');
      expect(rows.length).toBe(16);

      const accordions = screen.getAllByRole('group');

      const topPrioritiesAccordion = accordions[0];
      const topPrioritiesAccordionTitle = within(topPrioritiesAccordion).getByRole('button');

      expect(topPrioritiesAccordion).toHaveAttribute('aria-expanded', 'true');

      fireEvent.click(topPrioritiesAccordionTitle);

      expect(topPrioritiesAccordion).toHaveAttribute('aria-expanded', 'false');

      rows = screen.getAllByRole('row');
      expect(rows.length).toBe(13);

      fireEvent.click(topPrioritiesAccordionTitle);

      expect(topPrioritiesAccordion).toHaveAttribute('aria-expanded', 'true');

      rows = screen.getAllByRole('row');
      expect(rows.length).toBe(16);
    });

    it('"All Findings" accordion when clicked hides the all findings rows', async () => {
      renderComponent();

      const table = await screen.findByRole('table');
      expect(table).toBeInTheDocument();

      let rows = screen.getAllByRole('row');
      expect(rows.length).toBe(16);

      const accordions = screen.getAllByRole('group');

      const allFindingsAccordion = accordions[1];
      const allFindingsAccordionTitle = within(allFindingsAccordion).getByRole('button');

      expect(allFindingsAccordion).toHaveAttribute('aria-expanded', 'true');

      fireEvent.click(allFindingsAccordionTitle);

      expect(allFindingsAccordion).toHaveAttribute('aria-expanded', 'false');

      rows = screen.getAllByRole('row');
      expect(rows.length).toBe(6);

      fireEvent.click(allFindingsAccordionTitle);

      expect(allFindingsAccordion).toHaveAttribute('aria-expanded', 'true');

      rows = screen.getAllByRole('row');
      expect(rows.length).toBe(16);
    });
  });

  it('renders rows that when clicked navigates to component details page - violations section', async () => {
    renderComponent();

    const table = await screen.findByRole('table');
    expect(table).toBeInTheDocument();

    const rows = screen.getAllByRole('row');
    // 1st row is header row, 2nd row is Top Priorities row, 3rd row is the first component row
    const firstComponentRow = rows[2];
    const firstComponentHash = mockResponsePage1.topPriorities[0].componentHash;

    const secondComponentRow = rows[3];
    const secondComponentHash = mockResponsePage1.topPriorities[1].componentHash;

    fireEvent.click(firstComponentRow);
    expect(stateGoSpy).toHaveBeenCalledWith('applicationReport.componentDetails.overview', {
      hash: firstComponentHash,
      publicId: publicAppId,
      scanId,
    });

    fireEvent.click(secondComponentRow);
    expect(stateGoSpy).toHaveBeenCalledWith('applicationReport.componentDetails.overview', {
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
      expect(axiosMock.history.get[0].params).toEqual({ pageSize: 10, page: 1 });

      const table = await screen.findByRole('table');
      expect(table).toBeInTheDocument();

      let pagination = await screen.findByRole('navigation');
      expect(within(pagination).getAllByRole('button').length).toBe(3);

      const nextPageBtn = within(pagination).getByRole('button', { name: /goto next page/i });
      expect(nextPageBtn).toBeInTheDocument();

      fireEvent.click(nextPageBtn);

      expect(axiosMock.history.get.length).toBe(2);
      expect(axiosMock.history.get[1].url).toBe(getPrioritiesPageTableData(publicAppId, scanId));
      expect(axiosMock.history.get[1].params).toEqual({ pageSize: 10, page: 2 });

      await screen.findByRole('table');
      pagination = await screen.findByRole('navigation');

      const prevPageBtn = within(pagination).getByRole('button', { name: /goto previous page/i });
      expect(prevPageBtn).toBeInTheDocument();

      fireEvent.click(prevPageBtn);

      expect(axiosMock.history.get.length).toBe(3);
      expect(axiosMock.history.get[2].url).toBe(getPrioritiesPageTableData(publicAppId, scanId));
      expect(axiosMock.history.get[2].params).toEqual({ pageSize: 10, page: 1 });
    });

    it('correct data when page is changed', async () => {
      renderComponent();

      const table = await screen.findByRole('table');
      expect(table).toBeInTheDocument();

      let pagination = await screen.findByRole('navigation');
      expect(within(pagination).getAllByRole('button').length).toBe(3);

      await assertCorrectDataRowsByTypeAndPage('topPriorities', 1);
      await assertCorrectDataRowsByTypeAndPage('additionalPriorities', 1);

      const nextPageBtn = within(pagination).getByRole('button', { name: /goto next page/i });
      expect(nextPageBtn).toBeInTheDocument();

      fireEvent.click(nextPageBtn);

      pagination = await screen.findByRole('navigation');

      await assertCorrectDataRowsByTypeAndPage('topPriorities', 2);
      await assertCorrectDataRowsByTypeAndPage('additionalPriorities', 2);

      const prevPageBtn = within(pagination).getByRole('button', { name: /goto previous page/i });
      expect(prevPageBtn).toBeInTheDocument();

      fireEvent.click(prevPageBtn);

      await screen.findByRole('table');
      pagination = await screen.findByRole('navigation');

      await assertCorrectDataRowsByTypeAndPage('topPriorities', 1);
      await assertCorrectDataRowsByTypeAndPage('additionalPriorities', 1);
    }, 20000);
  });
});

async function assertCorrectDataRowsByTypeAndPage(priorityType, page) {
  const mockResponse = page === 1 ? mockResponsePage1 : mockResponsePage2;

  const {
    topPriorities,
    additionalPriorities: { results },
  } = mockResponse;

  const responseArrayToAssertAgainst = priorityType === 'topPriorities' ? topPriorities : results;
  for (let i = 0; i < responseArrayToAssertAgainst.length; i++) {
    const {
      priority,
      displayName,
      dependencyType,
      highestThreat,
      highestThreatPolicyName,
      highestThreatPolicyConstraintName,
      action,
      securityReachable,
    } = responseArrayToAssertAgainst[i];

    const table = await screen.findByRole('table');
    expect(table).toBeInTheDocument();

    const rows = screen.getAllByRole('row');

    /*
     * for top priority rows, skip table header and "top priorities" accordion row
     * for additional priority rows, skip table header, top priority accordion and data rows,
     * and all other findings accordion row
     */
    const row = rows[i + (priorityType === 'topPriorities' ? 2 : 6)];
    const cells = within(row).getAllByRole('cell');

    const priorityCell = cells[0];
    expect(priorityCell).toHaveTextContent(priority);

    const componentCell = cells[1];
    expect(componentCell).toHaveTextContent(displayName);

    /*
     * for additional priority rows skip top priority data rows,
     */
    expect(screen.getAllByTestId('dependency-type')[i + (priorityType === 'topPriorities' ? 0 : 3)]).toHaveTextContent(
      dependencyType.substring(0, 1)
    );

    if (securityReachable) {
      expect(componentCell).toHaveTextContent('Security-Reachable');
    }

    const policyCell = cells[2];
    expect(policyCell).toHaveTextContent(highestThreat);
    expect(policyCell).toHaveTextContent(highestThreatPolicyName);
    expect(policyCell).toHaveTextContent(highestThreatPolicyConstraintName);

    if (action !== 'none') {
      expect(policyCell).toHaveTextContent(action);
    }

    //TODO
    // const remediation = firstComponentCells[3];
    // expect(remediation).toHaveTextContent('Upgrade to 1.11.0');
    // expect(remediation).toHaveTextContent('Next version with no policy violations for this component and its dependencies')
  }
}

function generateMockData() {
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

  for (let i = 1; i <= NUM_OF_RESULTS_TOP_PRIORITIES; i++) {
    createEntry(topPriorities, i);
  }

  for (let i = NUM_OF_RESULTS_TOP_PRIORITIES + 1; i <= NUM_OF_RESULTS_ADDITIONAL_PRIORITIES; i++) {
    createEntry(additionalPriorities, i);
  }

  return {
    topPriorities,
    additionalPriorities,
  };
}

function generateMockResponseByPage(page) {
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
