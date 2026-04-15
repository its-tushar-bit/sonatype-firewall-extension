/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import moment from 'moment-timezone';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import * as applicationReportSelectors from 'MainRoot/applicationReport/applicationReportSelectors';
import * as productFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';
import * as applicationReportActions from 'MainRoot/applicationReport/applicationReportActions';
import * as routerContext from 'MainRoot/react/RouterStateContext';
import { fireEvent, render, screen, within, axiosMockAdapter } from 'TestRoot/SpecUtil';
import ReportPage from 'MainRoot/applicationReport/ReportPage';
import { FIREWALL_CONTAINER_REPOSITORY_RESULTS } from 'MainRoot/constants/states/firewall';
import {
  getApplicationSummaryUrl,
  getLatestReportInformation,
  getPermissionContextTestUrl,
} from 'MainRoot/util/CLMLocation';
import { act } from '@testing-library/react';
import { SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components/components/NxSubmitMask/NxSubmitMask';
import ReevaluationStatusModal from 'MainRoot/applicationReport/ReevaluationStatusModal';

describe('Report Page component', () => {
  let loadReportIfNeededSpy,
    selectHasUnscannedComponentsSpy,
    selectDisplayedComponentListSpy,
    selectIsDeveloperDashboardEnabledSpy,
    routerContextMock,
    applicationReport,
    displayedEntries,
    selectedReport,
    metadata,
    router,
    axiosMock;

  beforeAll(() => {
    moment.tz.setDefault('America/New_York');
  });

  afterAll(() => {
    moment.tz.setDefault();
  });

  beforeEach(() => {
    axiosMock = axiosMockAdapter();
    displayedEntries = getDefaultDisplayEntriesDataForTest();
    selectedReport = getDefaultSelectedReportForTest(displayedEntries);
    metadata = getDefaultMetadataForTest();
    router = getDefaultRouterStateForTest();
    applicationReport = getDefaultApplicationReportForTest(selectedReport, metadata);

    jest.spyOn(routerSelectors, 'selectRouterSlice').mockReturnValue(router);
    jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue(router.currentParams);
    jest.spyOn(applicationReportSelectors, 'selectApplicationReportSlice').mockReturnValue(applicationReport);
    jest.spyOn(applicationReportSelectors, 'selectApplicationReportMetaData').mockReturnValue(metadata);
    jest.spyOn(applicationReportSelectors, 'selectSelectedReport').mockReturnValue(selectedReport);
    jest.spyOn(applicationReportSelectors, 'selectIsAggregated').mockReturnValue(true);
    selectDisplayedComponentListSpy = jest
      .spyOn(applicationReportSelectors, 'selectDisplayedComponentList')
      .mockReturnValue(selectedReport.displayedEntries);
    jest.spyOn(applicationReportSelectors, 'selectDependencyTreeIsAvailable').mockReturnValue(true);
    jest.spyOn(applicationReportSelectors, 'selectIsPolicyTypeFilterEnabled').mockReturnValue(true);
    jest.spyOn(applicationReportSelectors, 'selectDependencyTreeUnavailableMessage').mockReturnValue('');
    jest.spyOn(applicationReportSelectors, 'selectDependencyTreeIsOldReport').mockReturnValue(false);
    jest.spyOn(applicationReportSelectors, 'selectApplicationReportLoading').mockReturnValue(false);

    selectHasUnscannedComponentsSpy = jest
      .spyOn(applicationReportSelectors, 'selectHasUnscannedComponents')
      .mockReturnValue(false);
    selectIsDeveloperDashboardEnabledSpy = jest
      .spyOn(productFeaturesSelectors, 'selectIsDeveloperDashboardEnabled')
      .mockReturnValue(false);

    loadReportIfNeededSpy = jest.spyOn(applicationReportActions, 'loadReportIfNeeded');
    jest.spyOn(applicationReportActions, 'toggleAggregateReportEntries');
    jest.spyOn(applicationReportActions, 'goToDependencyTreePage').mockReturnValue({ type: 'type' });

    routerContextMock = {
      href: jest.fn().mockReturnValue('mockValue'),
      get: jest.fn().mockReturnValue('mockGetValue'),
      includes: jest.fn(() => false),
    };
    jest.spyOn(routerContext, 'useRouterState').mockReturnValue(routerContextMock);

    axiosMock.onGet(getApplicationSummaryUrl(router.currentParams.publicId)).reply(200, { id: 'internal-id-123' });
    axiosMock
      .onPut(getPermissionContextTestUrl('application', 'internal-id-123'))
      .reply(200, ['WAIVE_POLICY_VIOLATIONS']);
  });

  it('renders an alert and retry button if there is an issue while loading information', () => {
    applicationReport.metadata = null;
    applicationReport.loadError = 'Server Error';
    renderComponent();

    const retryAlert = screen.getByRole('alert');
    expect(retryAlert).toBeVisible();
    expect(retryAlert).toHaveTextContent(`An error occurred loading data. ${applicationReport.loadError}`);

    const retryButton = screen.getByRole('button', { name: 'Retry' });
    expect(retryButton).toBeVisible();
    fireEvent.click(retryButton);
    expect(loadReportIfNeededSpy).toHaveBeenCalled();
  });

  it('renders an All Reports back button', () => {
    selectDisplayedComponentListSpy.mockReturnValue([]);
    renderComponent();

    const backToAllReports = screen.getByRole('link', { name: 'All Reports' });
    expect(backToAllReports).toBeVisible();
  });

  it('renders a "Back to Priorities" back button if navigated from Priorities Page', () => {
    selectDisplayedComponentListSpy.mockReturnValue([]);
    jest.spyOn(routerSelectors, 'selectIsPrioritiesPageContainer').mockReturnValue(true);

    renderComponent();

    const backToPriorities = screen.getByRole('link', { name: 'Back to Priorities' });
    expect(backToPriorities).toBeVisible();
  });

  it('renders a "Back to Firewall Dashboard" back button if navigated from Firewall Dashboard', () => {
    selectDisplayedComponentListSpy.mockReturnValue([]);
    jest.spyOn(routerSelectors, 'selectPrevStateIsFirewallDashboard').mockReturnValue(true);

    renderComponent();

    const backToFirewallDashboard = screen.getByRole('link', { name: 'Back to Firewall Dashboard' });
    expect(backToFirewallDashboard).toBeVisible();
  });

  it('renders a "Back to Repository Results" back button if it is for container report', () => {
    selectDisplayedComponentListSpy.mockReturnValue([]);
    jest
      .spyOn(applicationReportSelectors, 'selectIsContainerImagesEvaluationEnabledAndProxyStage')
      .mockReturnValue(true);

    renderComponent();

    const backToFirewallDashboard = screen.getByRole('link', { name: 'Back to Repository Results' });
    expect(backToFirewallDashboard).toBeVisible();
  });

  it('prefers origin param over previous-state firewall detection for container reports', () => {
    selectDisplayedComponentListSpy.mockReturnValue([]);
    router.currentParams.origin = FIREWALL_CONTAINER_REPOSITORY_RESULTS;
    jest.spyOn(routerSelectors, 'selectPrevStateIsFirewallDashboard').mockReturnValue(true);
    jest
      .spyOn(applicationReportSelectors, 'selectIsContainerImagesEvaluationEnabledAndProxyStage')
      .mockReturnValue(true);

    renderComponent();

    expect(screen.getByRole('link', { name: 'Back to Repository Results' })).toBeVisible();
  });

  it('renders a ReportTitle', async () => {
    selectDisplayedComponentListSpy.mockReturnValue([]);
    SpecUtil.requestIdleCallbackInvokeImmediateJest();

    renderComponent();

    const header = screen.getByRole('heading', { name: 'App Name Title' });
    const reevaluateButton = screen.getByRole('button', { name: 'Re-Evaluate Report' });
    const options = screen.getByRole('button', { name: 'Options' });
    const description = screen.getByText(`Triggered by ${metadata.scanTriggerType} on 2018-11-11 15:13:11 UTC-0500`);

    expect(header).toBeVisible();
    expect(reevaluateButton).toBeVisible();
    expect(options).toBeVisible();
    expect(description).toBeVisible();

    fireEvent.click(options);
    expect(screen.getByRole('link', { name: 'Export PDF' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'Export CycloneDX' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'Export SPDX' })).toBeVisible();

    const viewVulnerabilitiesLink = await screen.findByRole('link', {
      name: /view vulnerabilities/i,
    });
    expect(viewVulnerabilitiesLink).toBeVisible();
  });

  it('renders a ReportStatusBar', () => {
    selectDisplayedComponentListSpy.mockReturnValue([]);
    selectedReport.nonLowViolationCount = 1;
    selectedReport.policyComponentCount = 1;

    renderComponent();

    const criticalThreatIndicator = screen
      .getByText(selectedReport.criticalViolationCount)
      .closest('.nx-small-threat-counter');
    const severeThreatIndicator = screen
      .getByText(selectedReport.severeViolationCount)
      .closest('.nx-small-threat-counter');
    const moderateThreatIndicator = screen
      .getByText(selectedReport.moderateViolationCount)
      .closest('.nx-small-threat-counter');
    const totalViolationText = screen.getByText(`${selectedReport.nonLowViolationCount} VIOLATION`);
    const affectedComponentText = screen.getByText(`Affecting ${selectedReport.policyComponentCount} component`);
    const totalArtifactText = screen.getByText(`${selectedReport.totalArtifactCount} COMPONENTS`);
    const coveragePercentageText = screen.getByText('50% of all components identified');
    const legacyPolicyViolationsCountText = screen.getByText(
      `${selectedReport.legacyViolationCount} Legacy Violations`
    );

    expect(criticalThreatIndicator).toBeVisible();
    expect(criticalThreatIndicator).toHaveClass('nx-small-threat-counter--critical');
    expect(severeThreatIndicator).toBeVisible();
    expect(severeThreatIndicator).toHaveClass('nx-small-threat-counter--severe');
    expect(moderateThreatIndicator).toBeVisible();
    expect(moderateThreatIndicator).toHaveClass('nx-small-threat-counter--moderate');
    expect(totalViolationText).toBeVisible();
    expect(affectedComponentText).toBeVisible();
    expect(totalArtifactText).toBeVisible();
    expect(coveragePercentageText).toBeVisible();
    expect(legacyPolicyViolationsCountText).toBeVisible();
  });

  it('when developer-dashboard feature is disabled renders a ReportStatusBar without application risk score', () => {
    selectIsDeveloperDashboardEnabledSpy.mockReturnValue(false);
    renderComponent();
    expect(screen.queryByText(/application risk score/i)).not.toBeInTheDocument();
    expect(screen.queryByText(metadata.totalRisk)).not.toBeInTheDocument();
  });

  describe('when developer-dashboard feature is enabled', () => {
    beforeEach(() => {
      selectIsDeveloperDashboardEnabledSpy.mockReturnValue(true);
    });

    it('renders a ReportStatusBar with app risk score', () => {
      renderComponent();
      expect(screen.getByText(/app risk score/i)).toBeInTheDocument();
      expect(screen.getByTestId('iq-app-risk-score')).toHaveTextContent(metadata.totalRisk);
    });

    it('renders a ReportStatusBar with "N/A" when developer-dashboard feature is enabled and risk is -1', () => {
      applicationReport.metadata = { ...metadata, totalRisk: -1 };
      renderComponent();
      expect(screen.getByText(/app risk score/i)).toBeInTheDocument();
      expect(screen.getByTestId('iq-app-risk-score')).toHaveTextContent('N/A');
    });

    it('renders a ReportStatusBar with app risk score when risk is 0', () => {
      applicationReport.metadata = { ...metadata, totalRisk: 0 };
      renderComponent();
      expect(screen.getByText(/app risk score/i)).toBeInTheDocument();
      expect(screen.getByTestId('iq-app-risk-score')).toHaveTextContent('0');
    });

    it('renders a button "Learn more"', () => {
      renderComponent();
      expect(screen.getByRole('button', { name: /learn more/i })).toBeInTheDocument();
    });

    it('renders the "Application Risk Score" modal when the "Learn more" button is clicked', () => {
      renderComponent();
      const learnMoreBtn = screen.getByRole('button', { name: /learn more/i });
      fireEvent.click(learnMoreBtn);

      const modal = screen.getByRole('dialog');
      expect(modal).toBeInTheDocument();
      expect(within(modal).getByRole('heading', { name: /application risk score/i })).toBeInTheDocument();

      expect(
        screen.getByText(
          /Application risk score is the aggregate threat scores of your application's policy violations./i
        )
      ).toBeInTheDocument();
    });

    it('closes the "Application Risk Score" modal when the modal close button is clicked', () => {
      renderComponent();
      const learnMoreBtn = screen.getByRole('button', { name: /learn more/i });
      fireEvent.click(learnMoreBtn);

      const modal = screen.getByRole('dialog');
      expect(modal).toBeInTheDocument();
      expect(within(modal).getByRole('heading', { name: /application risk score/i })).toBeInTheDocument();

      const modalCloseBtn = within(modal).getByRole('button', { name: /close/i });
      expect(modalCloseBtn).toBeInTheDocument();

      fireEvent.click(modalCloseBtn);
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });
  });

  it('renders ReportContent with 3 actions', async () => {
    SpecUtil.requestIdleCallbackInvokeImmediateJest();
    renderComponent();

    const aggregateByComponentToggle = await screen.findByRole('switch', { name: /aggregate by component/i });
    const viewDependencyTreeButton = screen.getByRole('button', { name: 'View Dependency Tree' });
    const filterButton = screen.getByRole('button', { name: 'Filter' });

    expect(aggregateByComponentToggle).toBeVisible();
    expect(viewDependencyTreeButton).toBeVisible();
    expect(filterButton).toBeVisible();
  });

  it('renders ReportContent content table with 3 headers', () => {
    renderComponent();

    expect(screen.getByRole('table')).toBeVisible();
    expect(screen.getByRole('columnheader', { name: 'Threat' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Threat descending' })).toBeVisible();
    expect(screen.getByRole('columnheader', { name: 'Policy' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Policy unsorted' })).toBeVisible();
    expect(screen.getByRole('columnheader', { name: 'Component' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Component unsorted' })).toBeVisible();
  });

  it('renders ReportContent with information', async () => {
    SpecUtil.requestIdleCallbackInvokeImmediateJest();
    renderComponent();

    const componentRaws = screen.getAllByRole('row');
    expect(componentRaws.length).toBeGreaterThanOrEqual(6);

    expect(await screen.findByLabelText('Critical')).toBeVisible();
    expect(screen.getByText(`${displayedEntries[0].policyThreatLevel}`)).toBeVisible();
    expect(screen.getByRole('cell', { name: `${displayedEntries[0].policyName}` })).toBeVisible();
    expect(screen.getByRole('cell', { name: `${displayedEntries[0].derivedComponentName}` })).toBeVisible();

    expect(await screen.findByLabelText('Severe')).toBeVisible();
    expect(screen.getByText(`${displayedEntries[1].policyThreatLevel}`)).toBeVisible();
    expect(screen.getByRole('cell', { name: `${displayedEntries[1].policyName}` })).toBeVisible();
    expect(screen.getByRole('cell', { name: `${displayedEntries[1].derivedComponentName}` })).toBeVisible();

    expect(await screen.findByLabelText('Moderate')).toBeVisible();
    expect(screen.getByText(`${displayedEntries[2].policyThreatLevel}`)).toBeVisible();
    expect(screen.getByRole('cell', { name: `${displayedEntries[2].policyName}` })).toBeVisible();
    expect(screen.getByRole('cell', { name: `${displayedEntries[2].derivedComponentName}` })).toBeVisible();

    expect(await screen.findByLabelText('Low')).toBeVisible();
    expect(screen.getByText(`${displayedEntries[3].policyThreatLevel}`)).toBeVisible();
    expect(screen.getByRole('cell', { name: `${displayedEntries[3].policyName}` })).toBeVisible();
    expect(screen.getByRole('cell', { name: `${displayedEntries[3].derivedComponentName}` })).toBeVisible();

    expect(await screen.findByLabelText('None')).toBeVisible();
    expect(screen.getByText(`${displayedEntries[4].policyThreatLevel}`)).toBeVisible();
    expect(screen.getByRole('cell', { name: `${displayedEntries[4].policyName}` })).toBeVisible();
    expect(screen.getByRole('cell', { name: `${displayedEntries[4].derivedComponentName}` })).toBeVisible();
  }, 10000); // increasing timeout to stabilize spec (CLM-30887), on jenkins it seems to be taking around 8 seconds

  it('does not render warning message when policy types filter is enabled', () => {
    applicationReportSelectors.selectIsPolicyTypeFilterEnabled.mockReturnValue(true);
    renderComponent();
    expect(
      screen.queryByText(
        'This report has not been upgraded for the new Policy Types filter introduced in release 61. ' +
          'Re-evaluate in order to enable the Policy Types filter.'
      )
    ).not.toBeInTheDocument();
  });

  it('renders warning message when policy types filter is not enabled', () => {
    applicationReportSelectors.selectIsPolicyTypeFilterEnabled.mockReturnValue(false);
    renderComponent();
    expect(
      screen.getByText(
        'This report has not been upgraded for the new Policy Types filter introduced in release 61. ' +
          'Re-evaluate in order to enable the Policy Types filter.'
      )
    ).toBeVisible();
  });

  it('does not render old report warning message when dependencyTree is available', () => {
    applicationReportSelectors.selectDependencyTreeIsOldReport.mockReturnValue(false);
    renderComponent();
    expect(
      screen.queryByText('This report was generated with an older version of IQ. Please re-scan the application.')
    ).not.toBeInTheDocument();
  });

  it('renders old report warning message when dependencyTree is null', () => {
    applicationReportSelectors.selectDependencyTreeIsOldReport.mockReturnValue(true);
    renderComponent();
    expect(
      screen.getByText('This report was generated with an older version of IQ. Please re-scan the application.')
    ).toBeVisible();
  });

  it('renders reevaluation status modal with in-progress texts and close button when reevaluating is true', () => {
    applicationReport.reevaluating = true;
    renderComponent();
    expect(screen.getByText('Re-Evaluation Status')).toBeVisible();
    expect(screen.getByText('Re-Evaluating…')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Close' })).toBeInTheDocument();
  });

  it('renders reevaluation status modal with complete texts and no close button when reevaluating is false', () => {
    jest.useFakeTimers();

    const { rerender } = render(<ReevaluationStatusModal reevaluating={true} />);
    expect(screen.getByText('Re-Evaluating…')).toBeInTheDocument();

    // Renders a Re-Evaluation Complete content when reevaluating changes to false
    rerender(<ReevaluationStatusModal reevaluating={false} />);

    expect(screen.getByText('Re-Evaluation Complete')).toBeVisible();
    expect(screen.getByText('Success!')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Close' })).not.toBeInTheDocument();

    // Fast-forward until all timers have been executed
    act(() => {
      jest.advanceTimersByTime(SUCCESS_VISIBLE_TIME_MS);
    });

    expect(screen.queryByText('Re-Evaluation Complete')).not.toBeInTheDocument();
  });

  it('renders an alert with a custom message if there are insufficient permissions to reevaluate a report', () => {
    applicationReport.reevaluationError = 'Insufficient permissions';
    renderComponent();
    expect(screen.getByText('Insufficient Permissions to Re-Evaluate')).toBeVisible();
  });

  it('renders an alert if there is an issue reevaluating report', () => {
    applicationReport.reevaluationError = 'any random error message';
    renderComponent();
    expect(screen.getByText('any random error message')).toBeVisible();
  });

  it('does not render an alert if there is no issue reevaluating report', () => {
    applicationReport.reevaluationError = null;
    renderComponent();
    expect(screen.queryByText('Insufficient Permissions to Re-Evaluate')).not.toBeInTheDocument();
  });

  describe('when unscannable components exist in the report', () => {
    it('renders an error alert if the report contains unscannable components', async () => {
      selectHasUnscannedComponentsSpy.mockReturnValue(true);
      renderComponent();

      const unscannable = await screen.findByText('You have unscannable components in this build');
      expect(unscannable).toBeVisible();
    });

    it('displays a modal when the alert\'s "View" button is clicked and closes when the Close button is clicked', () => {
      selectHasUnscannedComponentsSpy.mockReturnValue(true);
      renderComponent();

      const viewButton = screen.getByRole('button', { name: 'View' });

      expect(viewButton).toBeVisible();
      fireEvent.click(viewButton);

      expect(screen.getByText('Unscannable Components')).toBeVisible();

      const closeButton = screen.getByRole('button', { name: 'Close' });
      expect(closeButton).toBeVisible();
      fireEvent.click(closeButton);

      expect(screen.queryByText('Unscannable Components')).toBeNull();
    });
  });

  it('should make request for latest report information', async () => {
    jest.spyOn(applicationReportSelectors, 'selectReportStageId').mockReturnValue('build');

    axiosMock.onGet(getLatestReportInformation('publicId', 'build')).reply(200, {
      id: 'some-other-scan-id',
      link: 'http://www.example.com/some-link',
      exists: true,
    });

    renderComponent();

    // initially not rendered while loading
    let warning = screen.queryAllByTestId('new-report-available-warning');
    expect(warning.length).toBe(0);

    expect(axiosMock.history.get.length).toEqual(2);
    expect(axiosMock.history.get[0].url).toEqual('/rest/application/services/summary/publicId');
    expect(axiosMock.history.get[1].url).toEqual('/rest/application/publicId/build/latestReportInformation');

    warning = await screen.findByTestId('new-report-available-warning');
    expect(warning).toBeVisible();
    expect(warning.textContent).toEqual(
      'A new version of this report is available. Click here to navigate to the latest report.'
    );
  });

  describe('Container Images Evaluation', () => {
    beforeEach(() => {
      jest.spyOn(applicationReportSelectors, 'selectReportStageId').mockReturnValue('proxy');
      jest
        .spyOn(applicationReportSelectors, 'selectIsContainerImagesEvaluationEnabledAndProxyStage')
        .mockReturnValue(true);
    });

    it('does not render dependency tree and filter button', () => {
      renderComponent();
      expect(screen.queryByRole('button', { name: 'View Dependency Tree' })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: 'Filter' })).not.toBeInTheDocument();
    });

    it('renders the "Re-Evaluate Container" button', () => {
      renderComponent();
      const reevaluateButton = screen.getByRole('button', { name: 'Re-Evaluate Container' });
      expect(reevaluateButton).toBeInTheDocument();
    });

    it('renders a ReportStatusBar with container risk score', () => {
      selectIsDeveloperDashboardEnabledSpy.mockReturnValue(true);
      renderComponent();
      expect(screen.getByText(/container risk score/i)).toBeInTheDocument();
      expect(screen.getByTestId('iq-app-risk-score')).toHaveTextContent(metadata.totalRisk);
    });

    it('renders learn more modal', () => {
      selectIsDeveloperDashboardEnabledSpy.mockReturnValue(true);
      renderComponent();
      const learnMoreBtn = screen.getByRole('button', { name: /learn more/i });
      fireEvent.click(learnMoreBtn);

      const modal = screen.getByRole('dialog');
      expect(modal).toBeInTheDocument();
      expect(within(modal).getByRole('heading', { name: /container risk score/i })).toBeInTheDocument();

      expect(
        screen.getByText(/Container risk score is the aggregate threat scores of your container's policy violations./i)
      ).toBeInTheDocument();
    });
  });

  describe('LegacyScannerBanner', () => {
    it('renders LegacyScannerBanner when containerScanningMode is neuvector', () => {
      metadata.containerScanningMode = 'neuvector';
      renderComponent();

      expect(screen.getByText('Legacy Scanner Used')).toBeVisible();
      expect(screen.getByText('Learn more about the new container scanner')).toBeVisible();
    });

    it('does not render LegacyScannerBanner when containerScanningMode is not neuvector', () => {
      metadata.containerScanningMode = 'sonatype';
      renderComponent();

      expect(screen.queryByText('Legacy Scanner Used')).not.toBeInTheDocument();
    });

    it('does not render LegacyScannerBanner when containerScanningMode is undefined', () => {
      delete metadata.containerScanningMode;
      renderComponent();

      expect(screen.queryByText('Legacy Scanner Used')).not.toBeInTheDocument();
    });
  });

  function renderComponent() {
    return render(<ReportPage />);
  }

  function getDefaultDisplayEntriesDataForTest() {
    return [
      {
        derivedComponentName: 'componentA : 1.0.0',
        derivedDependencyType: 'unknown',
        policyViolationId: 'some-violation-id-1',
        displayName: {
          name: 'componentA : 1.0.0',
          parts: [{ field: 'packageId', value: 'componentA' }, { value: ' : ' }, { field: 'version', value: '1.0.0' }],
        },
        policyName: 'Security-Critical',
        policyThreatLevel: 10,
      },
      {
        derivedComponentName: 'componentB : 1.0.0',
        derivedDependencyType: 'unknown',
        policyViolationId: 'some-violation-id-2',
        displayName: {
          name: 'componentB : 1.0.0',
          parts: [{ field: 'packageId', value: 'componentB' }, { value: ' : ' }, { field: 'version', value: '1.0.0' }],
        },
        policyName: 'Security-Medium',
        policyThreatLevel: 7,
      },
      {
        derivedComponentName: 'componentC : 1.0.0',
        derivedDependencyType: 'unknown',
        policyViolationId: 'some-violation-id-3',
        displayName: {
          name: 'componentC : 1.0.0',
          parts: [{ field: 'packageId', value: 'componentC' }, { value: ' : ' }, { field: 'version', value: '1.0.0' }],
        },
        policyName: 'Component-Unknown',
        policyThreatLevel: 2,
      },
      {
        derivedComponentName: 'componentD : 1.0.0',
        derivedDependencyType: 'unknown',
        policyViolationId: 'some-violation-id-4',
        displayName: {
          name: 'componentD : 1.0.0',
          parts: [{ field: 'packageId', value: 'componentD' }, { value: ' : ' }, { field: 'version', value: '1.0.0' }],
        },
        policyName: 'Architecture-Quality',
        policyThreatLevel: 1,
      },
      {
        derivedComponentName: 'componentE : 1.0.0',
        policyViolationId: 'some-violation-id-5',
        derivedDependencyType: 'unknown',
        displayName: {
          name: 'componentE : 1.0.0',
          parts: [{ field: 'packageId', value: 'componentE' }, { value: ' : ' }, { field: 'version', value: '1.0.0' }],
        },
        policyName: 'None',
        policyThreatLevel: 0,
      },
    ];
  }

  function getDefaultSelectedReportForTest(displayedEntries) {
    return {
      displayedEntries: displayedEntries,
      reportVersion: 3,
      knownArtifactCount: 250,
      totalArtifactCount: 500,
      policyComponentCount: 555,
      legacyViolationCount: 33,
      criticalViolationCount: 111,
      severeViolationCount: 222,
      moderateViolationCount: 333,
      nonLowViolationCount: 123,
    };
  }

  function getDefaultMetadataForTest() {
    return {
      scanTriggerType: 'Unknown',
      reportTitle: 'Title',
      reportTime: moment('2018-11-11 15:13:11').toDate().getTime(),
      application: {
        id: '704e2674ffe845a7ac037524ce32ae89',
        publicId: 'App Name',
        name: 'App Name',
        organizationId: '8637a3377e8f40748e263474d4a131c5',
      },
      totalRisk: 404,
    };
  }

  function getDefaultRouterStateForTest() {
    return {
      currentParams: {
        publicId: 'publicId',
        scanId: 'scanId',
      },
    };
  }

  function getDefaultApplicationReportForTest(selectReport, metadata) {
    return {
      selectedReport: selectedReport,
      metadata: metadata,
      exactValueFilters: {},
      reevaluating: false,
      loadError: null,
      pendingLoads: {},
    };
  }
});
