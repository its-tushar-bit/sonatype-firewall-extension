/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import { render, screen, fireEvent, within } from 'TestRoot/SpecUtil';
import ReportContent from 'MainRoot/applicationReport/react/ReportContent';
import * as applicationReportActions from 'MainRoot/applicationReport/applicationReportActions';
import * as applicationReportSelectors from 'MainRoot/applicationReport/applicationReportSelectors';

const displayedEntries = [
  { filename: 'Component 1', policyThreatLevel: 10, policyName: 'Security-High' },
  { filename: 'Component 2', policyThreatLevel: 9, policyName: 'Security-High' },
  { filename: 'Component 3', policyThreatLevel: 8, policyName: 'Security-Medium' },
  { filename: 'Component 4', policyThreatLevel: 7, policyName: 'Security-Medium' },
];

describe('ReportContent component', function () {
  let renderComponent;

  beforeEach(function () {
    spyOn(applicationReportActions, 'goToDependencyTreePage').and.returnValue({ type: 'type' });
    spyOn(applicationReportSelectors, 'selectIsAggregated').and.returnValue(true);
    spyOn(applicationReportSelectors, 'selectDisplayedComponentList').and.returnValue(displayedEntries);
    spyOn(applicationReportSelectors, 'selectDependencyTreeUnavailableMessage').and.returnValue('');
    spyOn(applicationReportSelectors, 'selectDependencyTreeIsAvailable').and.returnValue(true);

    renderComponent = (additionalProps = {}) => render(<ReportContent {...additionalProps} />);
  });

  it('renders aggregate by component toggle', function () {
    renderComponent();
    const aggregateByComponentToggle = screen.getByLabelText('Aggregate by component');

    expect(aggregateByComponentToggle).toBeVisible();
  });

  it('renders aggregate by component toggle tooltip', async function () {
    renderComponent();
    const aggregateByComponentToggle = screen.getByLabelText('Aggregate by component');

    fireEvent.mouseOver(aggregateByComponentToggle);

    const tooltip = await screen.findByRole('tooltip');

    expect(
      within(tooltip).getByText(
        'By default the Application Report aggregates violations by component. To see all violations not Aggregated by Component, please switch the toggle off.'
      )
    ).toBeInTheDocument();
  });

  it('dispatches correct action when toggling aggregate by component toggle', function () {
    spyOn(applicationReportActions, 'toggleAggregateReportEntries').and.returnValue({ type: 'type' });
    renderComponent();
    const aggregateByComponentToggle = screen.getByLabelText('Aggregate by component');

    fireEvent.click(aggregateByComponentToggle);
    expect(applicationReportActions.toggleAggregateReportEntries).toHaveBeenCalled();
  });

  it('renders entries from selected report', function () {
    renderComponent();
    displayedEntries.forEach((component) => {
      const row = screen.getByText(component.filename).closest('tr');
      expect(within(row).getByText(component.policyThreatLevel)).toBeVisible();
      expect(within(row).getByText(component.policyName)).toBeVisible();
    });
  });

  it('renders emptyMessage when there are no results', function () {
    applicationReportSelectors.selectDisplayedComponentList.and.returnValue([]);

    renderComponent();
    expect(screen.getByText('No Results')).toBeVisible();
  });

  it('render the table with descendent sort direction', function () {
    spyOn(applicationReportSelectors, 'selectSortConfiguration').and.returnValue({
      key: 'policyThreatLevel',
      sortFields: ['-policyThreatLevel', 'policyName', 'derivedComponentName'],
      dir: 'desc',
    });
    renderComponent();

    expect(screen.getByRole('columnheader', { name: /threat/i })).toHaveAttribute('aria-sort', 'descending');
    expect(screen.getByRole('columnheader', { name: /policy/i })).toHaveAttribute('aria-sort', 'none');
    expect(screen.getByRole('columnheader', { name: /component/i })).toHaveAttribute('aria-sort', 'none');
  });

  it('render the table with ascendant sort direction', function () {
    spyOn(applicationReportSelectors, 'selectSortConfiguration').and.returnValue({
      key: 'policyName',
      sortFields: ['policyName', '-policyThreatLevel', 'derivedComponentName'],
      dir: 'asc',
    });
    renderComponent();

    expect(screen.getByRole('columnheader', { name: /threat/i })).toHaveAttribute('aria-sort', 'none');
    expect(screen.getByRole('columnheader', { name: /policy/i })).toHaveAttribute('aria-sort', 'ascending');
    expect(screen.getByRole('columnheader', { name: /component/i })).toHaveAttribute('aria-sort', 'none');
  });

  it('render the table header with filters', function () {
    spyOn(applicationReportActions, 'setStringFieldFilter').and.returnValue({ type: 'type' });
    spyOn(applicationReportSelectors, 'selectSortConfiguration').and.returnValue({
      key: 'policyThreatLevel',
      sortFields: ['-policyThreatLevel', 'policyName', 'derivedComponentName'],
      dir: 'desc',
    });
    spyOn(applicationReportSelectors, 'selectSubstringFilters').and.returnValue({
      policyName: 'policyName',
      derivedComponentName: 'derivedComponentName',
    });
    renderComponent();

    const policyNameFilter = screen.getByPlaceholderText('policy name');
    const derivedComponentNameFilter = screen.getByPlaceholderText('component name');

    expect(policyNameFilter).toHaveAttribute('value', 'policyName');
    expect(derivedComponentNameFilter).toHaveAttribute('value', 'derivedComponentName');
    fireEvent.change(policyNameFilter, { target: { value: 'High' } });
    fireEvent.change(derivedComponentNameFilter, { target: { value: 'A' } });
    expect(applicationReportActions.setStringFieldFilter).toHaveBeenCalledWith('policyName', 'High');
    expect(applicationReportActions.setStringFieldFilter).toHaveBeenCalledWith('derivedComponentName', 'A');
  });

  it('dispatches action on filter`s click', function () {
    spyOn(applicationReportActions, 'toggleShowFilterPopover').and.returnValue({ type: 'type' });
    renderComponent();
    const button = screen.getByRole('button', { name: /filter/i });

    expect(button).toBeVisible();
    fireEvent.click(button);
    expect(applicationReportActions.toggleShowFilterPopover).toHaveBeenCalledTimes(1);
  });

  it('when there is a dependency tree available the "view dependency tree" button is enabled', function () {
    renderComponent();
    const button = screen.getByRole('button', { name: /view dependency tree/i });

    expect(button).toBeVisible();
    expect(button).not.toHaveClass('disabled');
    fireEvent.click(button);
    expect(applicationReportActions.goToDependencyTreePage).toHaveBeenCalledTimes(1);
  });

  it('when there is not a dependency tree available the "view dependency tree" button is disabled', async function () {
    applicationReportSelectors.selectDependencyTreeIsAvailable.and.returnValue(false);
    const tooltipText = 'some random tooltip text';
    applicationReportSelectors.selectDependencyTreeUnavailableMessage.and.returnValue(tooltipText);
    renderComponent();
    const button = screen.getByRole('button', { name: tooltipText });

    expect(button).toHaveTextContent(/view dependency tree/i);
    expect(button).toBeVisible();
    expect(button).toHaveClass('disabled');
    fireEvent.click(button);
    expect(applicationReportActions.goToDependencyTreePage).toHaveBeenCalledTimes(0);
    fireEvent.mouseOver(button);
    expect(await screen.findByText(tooltipText)).toBeInTheDocument();
  });
});
