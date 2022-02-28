/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import moment from 'moment-timezone';

import { render, screen, fireEvent } from 'TestRoot/SpecUtil';
import ReportPage from 'MainRoot/applicationReport/react/ReportPage';
import * as routerContext from 'MainRoot/react/RouterStateContext';
import * as applicationReportSelectors from 'MainRoot/applicationReport/applicationReportSelectors';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import * as applicationReportActions from 'MainRoot/applicationReport/applicationReportActions';

describe('Report Page component', () => {
  let renderComponent,
    loadReportIfNeededSpy,
    routerContextMock,
    applicationReport,
    displayedEntries,
    selectedReport,
    metadata,
    router;

  beforeAll(() => {
    moment.tz.setDefault('America/New_York');
  });

  afterAll(() => {
    moment.tz.setDefault();
  });

  beforeEach(() => {
    displayedEntries = [
      {
        derivedComponentName: 'componentA : 1.0.0',
        displayName: {
          name: 'componentA : 1.0.0',
          parts: [{ field: 'packageId', value: 'componentA' }, { value: ' : ' }, { field: 'version', value: '1.0.0' }],
        },
        policyName: 'Security-Critical',
        policyThreatLevel: 10,
      },
      {
        derivedComponentName: 'componentB : 1.0.0',
        displayName: {
          name: 'componentB : 1.0.0',
          parts: [{ field: 'packageId', value: 'componentB' }, { value: ' : ' }, { field: 'version', value: '1.0.0' }],
        },
        policyName: 'Security-Medium',
        policyThreatLevel: 7,
      },
      {
        derivedComponentName: 'componentC : 1.0.0',
        displayName: {
          name: 'componentC : 1.0.0',
          parts: [{ field: 'packageId', value: 'componentC' }, { value: ' : ' }, { field: 'version', value: '1.0.0' }],
        },
        policyName: 'Component-Unknown',
        policyThreatLevel: 2,
      },
      {
        derivedComponentName: 'componentD : 1.0.0',
        displayName: {
          name: 'componentD : 1.0.0',
          parts: [{ field: 'packageId', value: 'componentD' }, { value: ' : ' }, { field: 'version', value: '1.0.0' }],
        },
        policyName: 'Architecture-Quality',
        policyThreatLevel: 1,
      },
      {
        derivedComponentName: 'componentE : 1.0.0',
        displayName: {
          name: 'componentE : 1.0.0',
          parts: [{ field: 'packageId', value: 'componentE' }, { value: ' : ' }, { field: 'version', value: '1.0.0' }],
        },
        policyName: 'None',
        policyThreatLevel: 0,
      },
    ];
    selectedReport = {
      displayedEntries: displayedEntries,
      reportVersion: 3,
      knownArtifactCount: 250,
      totalArtifactCount: 500,
      policyComponentCount: 555,
      grandfatheredPolicyViolationCount: 33,
      criticalViolationCount: 111,
      severeViolationCount: 222,
      moderateViolationCount: 333,
      nonLowViolationCount: 123,
    };

    metadata = {
      scanTriggerType: 'Unknown',
      reportTitle: 'Title',
      reportTime: moment('2018-11-11 15:13:11').toDate().getTime(),
      application: {
        id: '704e2674ffe845a7ac037524ce32ae89',
        publicId: 'App Name',
        name: 'App Name',
        organizationId: '8637a3377e8f40748e263474d4a131c5',
      },
    };

    router = {
      currentParams: {
        publicId: 'publicId',
        scanId: 'scanId',
      },
    };

    applicationReport = {
      selectedReport: selectedReport,
      metadata: metadata,
      exactValueFilters: {},
      reevaluating: false,
      loadError: null,
      pendingLoads: {},
    };

    spyOn(routerSelectors, 'selectRouterSlice').and.returnValue(router);
    spyOn(routerSelectors, 'selectRouterCurrentParams').and.returnValue(router.currentParams);
    spyOn(applicationReportSelectors, 'selectApplicationReportSlice').and.returnValue(applicationReport);
    spyOn(applicationReportSelectors, 'selectApplicationReportMetaData').and.returnValue(metadata);
    spyOn(applicationReportSelectors, 'selectSelectedReport').and.returnValue(selectedReport);
    spyOn(applicationReportSelectors, 'selectIsAggregated').and.returnValue(true);
    spyOn(applicationReportSelectors, 'selectDisplayedComponentList').and.returnValue(selectedReport.displayedEntries);
    spyOn(applicationReportSelectors, 'selectDependencyTreeIsAvailable').and.returnValue(true);
    spyOn(applicationReportSelectors, 'selectIsPolicyTypeFilterEnabled').and.returnValue(true);
    spyOn(applicationReportSelectors, 'selectDependencyTreeUnavailableMessage').and.returnValue('');

    loadReportIfNeededSpy = spyOn(applicationReportActions, 'loadReportIfNeeded').and.callThrough();
    spyOn(applicationReportActions, 'toggleAggregateReportEntries');
    spyOn(applicationReportActions, 'goToDependencyTreePage').and.returnValue({ type: 'type' });

    routerContextMock = {
      href: jasmine.createSpy('href').and.returnValue('mockValue'),
      get: jasmine.createSpy('get').and.returnValue('mockGetValue'),
    };
    spyOn(routerContext, 'useRouterState').and.returnValue(routerContextMock);

    renderComponent = () => {
      render(<ReportPage />);
    };
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
    applicationReportSelectors.selectDisplayedComponentList.and.returnValue([]);
    renderComponent();

    const backToAllReports = screen.getByRole('link', { name: 'All Reports' });
    expect(backToAllReports).toBeVisible();
  });

  it('renders a ReportTitle', () => {
    applicationReportSelectors.selectDisplayedComponentList.and.returnValue([]);
    renderComponent();

    const header = screen.getByRole('heading', { name: 'App Name Title' });
    const reevaluateButton = screen.getByRole('button', { name: 'Re-Evaluate Report' });
    const options = screen.getByRole('button', { name: 'Options' });
    const description = screen.getByText(`Triggered by ${metadata.scanTriggerType} on 2018-11-11 15:13:11 UTC-05:00`);

    expect(header).toBeVisible();
    expect(description).toBeVisible();
    expect(reevaluateButton).toBeVisible();
    expect(options).toBeVisible();

    fireEvent.click(options);
    expect(screen.getByRole('link', { name: 'Generate PDF' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'View SBOM' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'View raw data' })).toBeVisible();

    const viewVulnerabilitiesLink = screen.getByRole('link', {
      name: 'Reevaluate the report in order to enable Vulnerabilities view',
    });
    expect(viewVulnerabilitiesLink).toBeVisible();
    expect(viewVulnerabilitiesLink).toHaveTextContent(/view vulnerabilities/i);

    expect(screen.getByRole('link', { name: 'View legacy report' })).toBeVisible();
  });

  it('renders a ReportStatusBar', () => {
    applicationReportSelectors.selectDisplayedComponentList.and.returnValue([]);
    selectedReport.nonLowViolationCount = 1;
    selectedReport.policyComponentCount = 1;
    renderComponent();

    const criticalThreatIndicator = screen.getByText(selectedReport.criticalViolationCount);
    const severeThreatIndicator = screen.getByText(selectedReport.severeViolationCount);
    const moderateThreatIndicator = screen.getByText(selectedReport.moderateViolationCount);
    const totalViolationText = screen.getByText(`${selectedReport.nonLowViolationCount} VIOLATION`);
    const affectedComponentText = screen.getByText(`Affecting ${selectedReport.policyComponentCount} component`);
    const totalArtifactText = screen.getByText(`${selectedReport.totalArtifactCount} COMPONENTS`);
    const coveragePercentageText = screen.getByText('50% of all components identified');
    const grandfatheredCountText = screen.getByText(
      `${selectedReport.grandfatheredPolicyViolationCount} Grandfathered`
    );

    expect(criticalThreatIndicator).toBeVisible();
    expect(criticalThreatIndicator).toHaveClassName('iq-threat-indicator critical');
    expect(severeThreatIndicator).toBeVisible();
    expect(severeThreatIndicator).toHaveClassName('iq-threat-indicator severe');
    expect(moderateThreatIndicator).toBeVisible();
    expect(moderateThreatIndicator).toHaveClassName('iq-threat-indicator moderate');
    expect(totalViolationText).toBeVisible();
    expect(affectedComponentText).toBeVisible();
    expect(totalArtifactText).toBeVisible();
    expect(coveragePercentageText).toBeVisible();
    expect(grandfatheredCountText).toBeVisible();
  });

  it('renders ReportContent with 3 actions', () => {
    renderComponent();

    const aggregateByComponentToggleTooltip =
      'By default the Application Report aggregates violations by component. ' +
      'To see all violations not Aggregated by Component, please switch the toggle off.';

    const aggregateByComponentToggle = screen.getByRole('switch', { name: aggregateByComponentToggleTooltip });
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

  it('renders ReportContent with information', () => {
    renderComponent();

    const componentRaws = screen.getAllByRole('row');
    expect(componentRaws.length).toBeGreaterThanOrEqual(6);

    expect(screen.getByRole('img', { name: 'Critical' })).toBeVisible();
    expect(screen.getByText(`${displayedEntries[0].policyThreatLevel}`)).toBeVisible();
    expect(screen.getByRole('cell', { name: `${displayedEntries[0].policyName}` })).toBeVisible();
    expect(screen.getByRole('cell', { name: `${displayedEntries[0].derivedComponentName}` })).toBeVisible();

    expect(screen.getByRole('img', { name: 'Severe' })).toBeVisible();
    expect(screen.getByText(`${displayedEntries[1].policyThreatLevel}`)).toBeVisible();
    expect(screen.getByRole('cell', { name: `${displayedEntries[1].policyName}` })).toBeVisible();
    expect(screen.getByRole('cell', { name: `${displayedEntries[1].derivedComponentName}` })).toBeVisible();

    expect(screen.getByRole('img', { name: 'Moderate' })).toBeVisible();
    expect(screen.getByText(`${displayedEntries[2].policyThreatLevel}`)).toBeVisible();
    expect(screen.getByRole('cell', { name: `${displayedEntries[2].policyName}` })).toBeVisible();
    expect(screen.getByRole('cell', { name: `${displayedEntries[2].derivedComponentName}` })).toBeVisible();

    expect(screen.getByRole('img', { name: 'Low' })).toBeVisible();
    expect(screen.getByText(`${displayedEntries[3].policyThreatLevel}`)).toBeVisible();
    expect(screen.getByRole('cell', { name: `${displayedEntries[3].policyName}` })).toBeVisible();
    expect(screen.getByRole('cell', { name: `${displayedEntries[3].derivedComponentName}` })).toBeVisible();

    expect(screen.getByRole('img', { name: 'None' })).toBeVisible();
    expect(screen.getByText(`${displayedEntries[4].policyThreatLevel}`)).toBeVisible();
    expect(screen.getByRole('cell', { name: `${displayedEntries[4].policyName}` })).toBeVisible();
    expect(screen.getByRole('cell', { name: `${displayedEntries[4].derivedComponentName}` })).toBeVisible();
  });

  it('does not render warning message when policy types filter is enabled', () => {
    applicationReportSelectors.selectIsPolicyTypeFilterEnabled.and.returnValue(true);
    renderComponent();
    expect(
      screen.queryByText(
        'This report has not been upgraded for the new Policy Types filter introduced in release 61. ' +
          'Re-evaluate in order to enable the Policy Types filter.'
      )
    ).not.toBeInTheDocument();
  });

  it('renders warning message when policy types filter is not enabled', () => {
    applicationReportSelectors.selectIsPolicyTypeFilterEnabled.and.returnValue(false);
    renderComponent();
    expect(
      screen.getByText(
        'This report has not been upgraded for the new Policy Types filter introduced in release 61. ' +
          'Re-evaluate in order to enable the Policy Types filter.'
      )
    ).toBeVisible();
  });
});
