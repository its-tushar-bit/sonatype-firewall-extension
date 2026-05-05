/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import { render, screen, fireEvent, within, axiosMockAdapter, userEvent } from 'TestRoot/SpecUtil';
import ReportContent from 'MainRoot/applicationReport/ReportContent';
import * as applicationReportActions from 'MainRoot/applicationReport/applicationReportActions';
import * as applicationReportSelectors from 'MainRoot/applicationReport/applicationReportSelectors';
import * as productFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';

import * as RouterActions from 'MainRoot/reduxUiRouter/routerActions';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';

import { getApplicationSummaryUrl, getPermissionContextTestUrl } from 'MainRoot/util/CLMLocation';
const mockComponentIdentifier = {
  coordinates: {
    artifactId: 'guava-gwt',
    classifier: 'sources',
    extension: 'jar',
    groupId: 'com.google.guava',
    version: '30.1-jre',
  },
  format: 'maven',
};

const displayedEntries = [
  {
    filename: 'Component 1',
    policyThreatLevel: 10,
    policyName: 'Security-High',
    hash: 'hash1',
    componentIdentifier: mockComponentIdentifier,
    derivedViolationState: 'open',
  },
  {
    filename: 'Component 2',
    policyThreatLevel: 9,
    policyName: 'Security-High',
    hash: 'hash2',
    derivedViolationState: 'open',
  },
  {
    filename: 'Component 3',
    policyThreatLevel: 8,
    policyName: 'Security-Medium',
    hash: 'hash3',
    derivedViolationState: 'open',
  },
  {
    filename: 'Component 4',
    policyThreatLevel: 7,
    policyName: 'Security-Medium',
    hash: 'hash4',
    derivedViolationState: 'open',
  },
];

const routerCurrentParams = {
  repositoryId: 'repositoryId',
  publicId: 'publicId',
  scanId: 'scanId',
};

const selectedReport = {
  displayedEntries: displayedEntries,
  reportVersion: 3,
  knownArtifactCount: 10,
  totalArtifactCount: 10,
  policyComponentCount: 1,
  legacyViolationCount: 0,
  criticalViolationCount: 1,
  severeViolationCount: 1,
  moderateViolationCount: 1,
  nonLowViolationCount: 3,
};

describe('ReportContent component', function () {
  let renderComponent, stateGoSpy, axiosMock;

  beforeEach(function () {
    jest.spyOn(applicationReportActions, 'goToDependencyTreePage').mockReturnValue({ type: 'type' });
    jest.spyOn(applicationReportActions, 'goToBulkWaivePage').mockReturnValue({ type: 'type' });
    jest.spyOn(applicationReportSelectors, 'selectIsAggregated').mockReturnValue(true);
    jest.spyOn(applicationReportSelectors, 'selectDisplayedComponentList').mockReturnValue(displayedEntries);
    jest.spyOn(applicationReportSelectors, 'selectDependencyTreeUnavailableMessage').mockReturnValue('');
    jest.spyOn(applicationReportSelectors, 'selectDependencyTreeIsAvailable').mockReturnValue(true);
    jest.spyOn(applicationReportSelectors, 'selectSelectedReport').mockReturnValue(selectedReport);
    jest.spyOn(applicationReportSelectors, 'selectAllComponentsList').mockReturnValue(displayedEntries);
    jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue(routerCurrentParams);

    jest.spyOn(productFeaturesSelectors, 'selectHasBulkWaivers').mockReturnValue(true);
    stateGoSpy = jest.spyOn(RouterActions, 'stateGo');

    renderComponent = (additionalProps = {}) => render(<ReportContent {...additionalProps} />);

    axiosMock = axiosMockAdapter();

    axiosMock.onGet(getApplicationSummaryUrl(routerCurrentParams.publicId)).reply(200, { id: 'internal-id-123' });
    axiosMock
      .onPut(getPermissionContextTestUrl('application', 'internal-id-123'))
      .reply(200, ['WAIVE_POLICY_VIOLATIONS']);
  });

  it('renders aggregate by component toggle', function () {
    renderComponent();
    const aggregateByComponentToggle = screen.getByLabelText('Aggregate by component');

    expect(aggregateByComponentToggle).toBeVisible();
  });

  it('renders aggregate by component toggle tooltip', async function () {
    SpecUtil.requestIdleCallbackInvokeImmediateJest();

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
    jest.spyOn(applicationReportActions, 'toggleAggregateReportEntries').mockReturnValue({ type: 'type' });
    renderComponent();
    const aggregateByComponentToggle = screen.getByLabelText('Aggregate by component');

    fireEvent.click(aggregateByComponentToggle);
    expect(applicationReportActions.toggleAggregateReportEntries).toHaveBeenCalled();
  });

  it('renders bulk waive button', async function () {
    renderComponent();
    const bulkWaiveButton = await screen.findByRole('button', { name: 'Bulk Waive' });

    expect(bulkWaiveButton).toBeVisible();
    expect(bulkWaiveButton).not.toBeDisabled();
  });

  it('dispatches action when bulk waive button is clicked', async function () {
    renderComponent();

    const bulkWaiveButton = await screen.findByRole('button', { name: 'Bulk Waive' });
    fireEvent.click(bulkWaiveButton);
    expect(applicationReportActions.goToBulkWaivePage).toHaveBeenCalledTimes(1);
  });

  it('disables bulk waive button when there are no open violations', async function () {
    applicationReportSelectors.selectAllComponentsList.mockReturnValue([
      { derivedViolationState: 'waived' },
      { derivedViolationState: 'legacy' },
    ]);
    renderComponent();
    const bulkWaiveButton = await screen.findByRole('button', { name: 'Bulk Waive' });
    expect(bulkWaiveButton).toBeDisabled();
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
    applicationReportSelectors.selectDisplayedComponentList.mockReturnValue([]);

    renderComponent();
    expect(screen.getByText('No Results')).toBeVisible();
  });

  it('render the table with descendent sort direction', function () {
    jest.spyOn(applicationReportSelectors, 'selectSortConfiguration').mockReturnValue({
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
    jest.spyOn(applicationReportSelectors, 'selectSortConfiguration').mockReturnValue({
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
    jest.spyOn(applicationReportActions, 'setStringFieldFilter').mockReturnValue({ type: 'type' });
    jest.spyOn(applicationReportSelectors, 'selectSortConfiguration').mockReturnValue({
      key: 'policyThreatLevel',
      sortFields: ['-policyThreatLevel', 'policyName', 'derivedComponentName'],
      dir: 'desc',
    });
    jest.spyOn(applicationReportSelectors, 'selectSubstringFilters').mockReturnValue({
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
    jest.spyOn(applicationReportActions, 'toggleShowFilterPopover').mockReturnValue({ type: 'type' });
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
    applicationReportSelectors.selectDependencyTreeIsAvailable.mockReturnValue(false);
    const tooltipText = 'some random tooltip text';
    applicationReportSelectors.selectDependencyTreeUnavailableMessage.mockReturnValue(tooltipText);
    SpecUtil.requestIdleCallbackInvokeImmediateJest();

    renderComponent();
    const button = await screen.findByRole('button', { name: /view dependency tree/i });

    expect(button).toHaveTextContent(/view dependency tree/i);
    expect(button).toBeVisible();
    expect(button).toHaveClass('disabled');
    fireEvent.click(button);
    expect(applicationReportActions.goToDependencyTreePage).toHaveBeenCalledTimes(0);
    fireEvent.mouseOver(button);
    expect(await screen.findByText(tooltipText)).toBeInTheDocument();
  });

  describe('row clicks', () => {
    describe('when page is not navigated from the priorities page', () => {
      it('navigates with the applicationReport state', () => {
        renderComponent();

        const firstComponentRow = screen.getAllByRole('row')[2];
        fireEvent.click(firstComponentRow);

        expect(stateGoSpy).toHaveBeenCalledWith('applicationReport.componentDetails', {
          publicId: routerCurrentParams.publicId,
          scanId: routerCurrentParams.scanId,
          hash: 'hash1',
        });
      });
    });

    describe('Container Images Evaluation', () => {
      beforeEach(() => {
        jest
          .spyOn(applicationReportSelectors, 'selectIsContainerImagesEvaluationEnabledAndProxyStage')
          .mockReturnValue(true);
        jest.spyOn(applicationReportSelectors, 'selectReportStageId').mockReturnValue('proxy');
        jest.spyOn(applicationReportSelectors, 'selectDependencyTreeIsAvailable').mockReturnValue(false);
      });

      it('should hide dependency tree and filter buttons', () => {
        const viewDependencyTree = screen.queryByRole('button', { name: /view dependency tree/i });
        const filterButton = screen.queryByRole('button', { name: /filter/i });

        renderComponent();

        expect(viewDependencyTree).not.toBeInTheDocument();
        expect(filterButton).not.toBeInTheDocument();
      });

      it('navigates to the Firewall container component details state', async () => {
        const user = userEvent.setup();
        renderComponent();

        const firstComponentRow = screen.getAllByRole('row')[2];
        await user.click(firstComponentRow);

        expect(stateGoSpy).toHaveBeenCalledWith('firewall.containerComponentDetails.overview', {
          hash: 'hash1',
          publicId: 'publicId',
          scanId: 'scanId',
          origin: 'firewall.containerRepositoryResults',
        });
      });

      it('renders Waive All Fail Policy Violations button and navigate to add waiver page when button is clicked', async () => {
        const user = userEvent.setup();
        jest.spyOn(applicationReportSelectors, 'selectActiveProxyFailedViolationCount').mockReturnValue(1);
        renderComponent();

        const waiveAllViolationsButton = screen.getByRole('button', { name: 'Waive All Fail Policy Violations' });
        expect(waiveAllViolationsButton).toBeInTheDocument();

        await user.click(waiveAllViolationsButton);
        expect(stateGoSpy).toHaveBeenCalledWith('firewall.addContainerImageWaiver', {
          publicId: 'publicId',
          scanId: 'scanId',
          origin: 'firewall.containerRepositoryResults',
        });
      });

      it('enables Waive All Fail Policy Violations button when activeProxyFailedViolationCount is greater than 0', () => {
        jest.spyOn(applicationReportSelectors, 'selectActiveProxyFailedViolationCount').mockReturnValue(5);

        renderComponent();
        const waiveAllViolationsButton = screen.getByRole('button', { name: 'Waive All Fail Policy Violations' });
        expect(waiveAllViolationsButton).toBeInTheDocument();
        expect(waiveAllViolationsButton).not.toBeDisabled();
      });

      it('disables Waive All Fail Policy Violations button button when activeProxyFailedViolationCount is 0', () => {
        jest.spyOn(applicationReportSelectors, 'selectActiveProxyFailedViolationCount').mockReturnValue(0);

        renderComponent();
        const waiveAllViolationsButton = screen.getByRole('button', { name: 'Waive All Fail Policy Violations' });
        expect(waiveAllViolationsButton).toBeInTheDocument();
        expect(waiveAllViolationsButton).toBeDisabled();
      });
    });
  });

  describe('Pro Tier Gating', () => {
    it('shows Preview Bulk Waive button instead of Bulk Waive when feature is absent', async () => {
      jest.spyOn(productFeaturesSelectors, 'selectHasBulkWaivers').mockReturnValue(false);

      renderComponent();

      expect(await screen.findByRole('button', { name: 'Preview Bulk Waive' })).toBeVisible();
      expect(screen.queryByRole('button', { name: 'Bulk Waive' })).not.toBeInTheDocument();
    });
  });
});
