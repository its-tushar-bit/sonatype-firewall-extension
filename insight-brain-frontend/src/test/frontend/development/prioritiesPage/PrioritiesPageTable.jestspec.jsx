/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, screen, within, fireEvent } from 'TestRoot/SpecUtil';
import PrioritiesPageTable from 'MainRoot/development/prioritiesPage/PrioritiesPageTable';
import { mockData } from './prioritiesPageTableMockData';
import * as RouterActions from 'MainRoot/reduxUiRouter/routerActions';

const publicAppId = 'testPublicAppId';
const scanId = 'testScanId';

describe('PrioritiesPageTable', () => {
  let renderComponent, stateGoSpy;

  const defaultPreloadedState = {
    applicationReport: {
      selectedReport: {
        displayedEntries: mockData,
      },
    },
    router: {
      currentParams: {
        publicAppId,
        scanId,
      },
    },
  };

  beforeEach(() => {
    stateGoSpy = jest.spyOn(RouterActions, 'stateGo');

    renderComponent = (preloadedState) =>
      render(<PrioritiesPageTable />, { preloadedState: preloadedState || defaultPreloadedState });
  });

  it('renders a table with 4 column headers', () => {
    renderComponent();

    const table = screen.getByRole('table');
    expect(table).toBeInTheDocument();

    const columnheaders = within(table).getAllByRole('columnheader');
    expect(columnheaders.length).toBe(4 + 1); //last column is to render chevron icon for clickable rows
  });

  it('renders column headers with correct names in the correct order', () => {
    renderComponent();

    const table = screen.getByRole('table');
    expect(table).toBeInTheDocument();

    const columnHeaders = within(table).getAllByRole('columnheader');
    expect(columnHeaders[0]).toHaveAccessibleName(/priority/i);
    expect(columnHeaders[1]).toHaveAccessibleName(/component/i);
    expect(columnHeaders[2]).toHaveAccessibleName(/highest policy threat/i);
    expect(columnHeaders[3]).toHaveAccessibleName(/recommendation/i);
  });

  it('renders the priority column header with an icon and tooltip', async () => {
    renderComponent();

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
    it('renders 2 open accordions with title "Top Priorities" and "All Other Findings"', () => {
      renderComponent();

      const accordions = screen.getAllByRole('group');
      expect(accordions).toHaveLength(2);

      const topPrioritiesAccordion = accordions[0];
      const allFindingsAccordion = accordions[1];

      expect(topPrioritiesAccordion).toHaveAttribute('aria-expanded', 'true');
      expect(allFindingsAccordion).toHaveAttribute('aria-expanded', 'true');

      expect(within(topPrioritiesAccordion).getByRole('button')).toHaveAccessibleName(/top priorities/i);
      expect(within(allFindingsAccordion).getByRole('button')).toHaveAccessibleName(/all other findings/i);
    });

    it('"Top Priorities" accordion when clicked hides the priority rows', () => {
      renderComponent();

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

    it('"All Findings" accordion when clicked hides the all findings rows', () => {
      renderComponent();

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

  it('renders rows that when clicked navigates to component details page - violations section', () => {
    renderComponent();

    const rows = screen.getAllByRole('row');
    // 1st row is header row, 2nd row is Top Priorities row, 3rd row is the first component row
    const firstComponentRow = rows[2];
    const firstComponentHash = mockData[0].hash;

    const secondComponentRow = rows[3];
    const secondComponentHash = mockData[1].hash;

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

  describe('renders correct component information the rows', () => {
    it('first component details', () => {
      renderComponent();

      const rows = screen.getAllByRole('row');
      const firstComponentRow = rows[2];
      const firstComponentCells = within(firstComponentRow).getAllByRole('cell');

      const priority = firstComponentCells[0];
      expect(priority).toHaveTextContent('1');

      const component = firstComponentCells[1];
      expect(component).toHaveTextContent('axis : axis : 1.2');
      expect(component).toHaveTextContent('D');

      const policyDetails = firstComponentCells[2];
      expect(policyDetails).toHaveTextContent('10');
      expect(policyDetails).toHaveTextContent('Critical risk CVSS score');
      expect(policyDetails).toHaveTextContent('Security-Critical');
      expect(policyDetails).toHaveTextContent('fail');

      //TODO
      // const remediation = firstComponentCells[3];
      // expect(remediation).toHaveTextContent('Upgrade to 1.11.0');
      // expect(remediation).toHaveTextContent('Next version with no policy violations for this component and its dependencies')
    });

    it('second component details', () => {
      renderComponent();

      const rows = screen.getAllByRole('row');
      const secondComponentRow = rows[3];
      const secondComponentCells = within(secondComponentRow).getAllByRole('cell');

      const priority = secondComponentCells[0];
      expect(priority).toHaveTextContent('2');

      const components = secondComponentCells[1];
      expect(components).toHaveTextContent('com.fasterxml.jackson.core : jackson-databind : 2.0.4');
      expect(components).toHaveTextContent('D');

      const policyDetails = secondComponentCells[2];
      expect(policyDetails).toHaveTextContent('8');
      expect(policyDetails).toHaveTextContent('High risk CVSS score');
      expect(policyDetails).toHaveTextContent('Security-High');
      expect(policyDetails).toHaveTextContent('warn');

      //TODO
      // const remediation = firstComponentCells[3];
      // expect(remediation).toHaveTextContent('Upgrade to 1.11.0');
      // expect(remediation).toHaveTextContent('Next version with no policy violations for this component and its dependencies')
    });

    it('third component details', () => {
      renderComponent();

      const rows = screen.getAllByRole('row');
      const thirdComponentRow = rows[4];
      const thirdComponentCells = within(thirdComponentRow).getAllByRole('cell');

      const priority = thirdComponentCells[0];
      expect(priority).toHaveTextContent('3');

      const components = thirdComponentCells[1];
      expect(components).toHaveTextContent('commons-collections : commons-collections : 3.1');
      expect(components).toHaveTextContent('T');

      const policyDetails = thirdComponentCells[2];
      expect(policyDetails).toHaveTextContent('7');
      expect(policyDetails).toHaveTextContent('Medium risk CVSS score');
      expect(policyDetails).toHaveTextContent('Security-Medium');
      expect(policyDetails).toHaveTextContent('warn');

      //TODO
      // const remediation = firstComponentCells[3];
      // expect(remediation).toHaveTextContent('Upgrade to 1.11.0');
      // expect(remediation).toHaveTextContent('Next version with no policy violations for this component and its dependencies')
    });

    it('fourth component details', () => {
      renderComponent();

      const rows = screen.getAllByRole('row');
      const fourthComponentRow = rows[6];
      const fourthComponentCells = within(fourthComponentRow).getAllByRole('cell');

      const priority = fourthComponentCells[0];
      // TODO final implementation will have priority 4 since this will be under the All Findings Accordion
      expect(priority).toHaveTextContent('1');

      const components = fourthComponentCells[1];
      expect(components).toHaveTextContent('hsqldb : hsqldb : 1.8.0.7');
      expect(components).toHaveTextContent('D');

      const policyDetails = fourthComponentCells[2];
      expect(policyDetails).toHaveTextContent('7');
      expect(policyDetails).toHaveTextContent('Medium risk CVSS score');
      expect(policyDetails).toHaveTextContent('Security-Medium');
      expect(policyDetails).not.toHaveTextContent('fail');
      expect(policyDetails).not.toHaveTextContent('warn');

      //TODO
      // const remediation = firstComponentCells[3];
      // expect(remediation).toHaveTextContent('Upgrade to 1.11.0');
      // expect(remediation).toHaveTextContent('Next version with no policy violations for this component and its dependencies')
    });
  });
});
