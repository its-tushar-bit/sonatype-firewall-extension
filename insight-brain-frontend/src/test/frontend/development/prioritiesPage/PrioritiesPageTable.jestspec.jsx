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

describe('PrioritiesPageTable', () => {
  let renderComponent, stateGoSpy, axiosMock;

  const mockResponse = generateMockResponse();

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

    axiosMock.onGet(getPrioritiesPageTableData(publicAppId, scanId)).reply(200, mockResponse);
  });

  it('makes correct network request', () => {
    renderComponent();

    expect(axiosMock.history.get.length).toBe(1);
    expect(axiosMock.history.get[0].url).toBe(getPrioritiesPageTableData(publicAppId, scanId));

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
    axiosMock.onGet(getPrioritiesPageTableData(publicAppId, scanId)).reply(500, 'Error');

    renderComponent();

    const table = await screen.findByRole('table');
    expect(table).toBeInTheDocument();

    const alert = within(table).getByRole('alert');
    expect(alert).toBeInTheDocument();
  });

  it('clicking the retry button on error alert makes correct network request', async () => {
    axiosMock.onGet(getPrioritiesPageTableData(publicAppId, scanId)).reply(500, 'Error');

    renderComponent();

    const table = await screen.findByRole('table');
    expect(table).toBeInTheDocument();

    expect(axiosMock.history.get.length).toBe(1);
    expect(axiosMock.history.get[0].url).toBe(getPrioritiesPageTableData(publicAppId, scanId));

    const retryBtn = within(table).getByRole('button');
    fireEvent.click(retryBtn);

    expect(axiosMock.history.get.length).toBe(2);
    expect(axiosMock.history.get[1].url).toBe(getPrioritiesPageTableData(publicAppId, scanId));
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
      expect(rows.length).toBe(7);

      const accordions = screen.getAllByRole('group');

      const topPrioritiesAccordion = accordions[0];
      const topPrioritiesAccordionTitle = within(topPrioritiesAccordion).getByRole('button');

      expect(topPrioritiesAccordion).toHaveAttribute('aria-expanded', 'true');

      fireEvent.click(topPrioritiesAccordionTitle);

      expect(topPrioritiesAccordion).toHaveAttribute('aria-expanded', 'false');

      rows = screen.getAllByRole('row');
      expect(rows.length).toBe(4);

      fireEvent.click(topPrioritiesAccordionTitle);

      expect(topPrioritiesAccordion).toHaveAttribute('aria-expanded', 'true');

      rows = screen.getAllByRole('row');
      expect(rows.length).toBe(7);
    });

    it('"All Findings" accordion when clicked hides the all findings rows', async () => {
      renderComponent();

      const table = await screen.findByRole('table');
      expect(table).toBeInTheDocument();

      let rows = screen.getAllByRole('row');
      expect(rows.length).toBe(7);

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
      expect(rows.length).toBe(7);
    });
  });

  it('renders rows that when clicked navigates to component details page - violations section', async () => {
    renderComponent();

    const table = await screen.findByRole('table');
    expect(table).toBeInTheDocument();

    const rows = screen.getAllByRole('row');
    // 1st row is header row, 2nd row is Top Priorities row, 3rd row is the first component row
    const firstComponentRow = rows[2];
    const firstComponentHash = mockResponse[0].componentHash;

    const secondComponentRow = rows[3];
    const secondComponentHash = mockResponse[1].componentHash;

    fireEvent.click(firstComponentRow);
    expect(stateGoSpy).toHaveBeenCalledWith('applicationReport.componentDetails.violations', {
      hash: firstComponentHash,
      publicId: publicAppId,
      scanId,
    });

    fireEvent.click(secondComponentRow);
    expect(stateGoSpy).toHaveBeenCalledWith('applicationReport.componentDetails.violations', {
      hash: secondComponentHash,
      publicId: publicAppId,
      scanId,
    });
  });

  it('renders correct component information in the rows', async () => {
    renderComponent();

    for (let i = 0; i < mockResponse.length; i++) {
      const {
        priority,
        displayName,
        dependencyType,
        highestThreat,
        highestThreatPolicyName,
        highestThreatPolicyConstraintName,
        action,
      } = mockResponse[i];

      const table = await screen.findByRole('table');
      expect(table).toBeInTheDocument();

      const rows = screen.getAllByRole('row');
      const row = rows[i + 2 + (i === 3 ? 1 : 0)]; //skip "all other findings" header row for 4th component info
      const cells = within(row).getAllByRole('cell');

      const priorityCell = cells[0];
      expect(priorityCell).toHaveTextContent(priority);

      const componentCell = cells[1];
      expect(componentCell).toHaveTextContent(displayName);
      expect(screen.getAllByTestId('dependency-type')[i]).toHaveTextContent(dependencyType.substring(0, 1));

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
  });
});

function generateMockResponse() {
  const response = [];
  const NUM_OF_RESULTS = 4;

  for (let i = 0; i < NUM_OF_RESULTS; i++) {
    const hasFail = faker.datatype.boolean();
    response.push({
      displayName: faker.lorem.word(1),
      componentHash: faker.git.commitSha(),
      dependencyType: faker.helpers.arrayElement(['Direct', 'Transitive', 'Inner Source']),
      hasFailActionOnComponent: hasFail,
      action: hasFail ? 'fail' : faker.helpers.arrayElement(['none', 'warn']),
      highestThreat: faker.datatype.number({ min: 0, max: 10 }),
      highestThreatPolicyName: faker.lorem.slug(),
      highestThreatPolicyConstraintName: faker.lorem.sentence(),
      priority: i,
    });
  }

  return response;
}
