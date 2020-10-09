/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import ReportPage from '../../../../main/frontend/applicationReport/react/ReportPage';

describe('Report Page component', function() {
  let getShallowComponent,
      loadReportActionMock;

  beforeEach(function() {

    loadReportActionMock = jasmine.createSpy('loadReport');

    const minimalProps = {
      metadata: {
        reportTitle: 'Title',
        application: {
          name: 'App Name'
        }
      },
      publicId: 'publicId',
      scanId: 'scanId',
      selectedReport: {
        reportVersion: 3,
        knownArtifactCount: 1,
        totalArtifactCount: 2,
        policyComponentCount: 1,
        grandfatheredPolicyViolationCount: 0,
        criticalViolationCount: 1,
        severeViolationCount: 2,
        moderateViolationCount: 3,
        nonLowViolationCount: 0
      },
      loadReport: loadReportActionMock
    };

    getShallowComponent = enzymeUtils.getShallowComponent(ReportPage, minimalProps);
  });

  it('renders a MaximizedContainer', function() {
    const shallowComponent = getShallowComponent();
    expect(shallowComponent).toMatchSelector('MaximizedContainer');
  });

  it('renders a ReportTitle ReportStatusBar ReportFilters ReportContent', function() {
    const shallowComponent = getShallowComponent();
    const reportTitle = shallowComponent.find('ReportTitle');
    const reportStatusBar = shallowComponent.find('ReportStatusBar');
    const reportFilters = shallowComponent.find('ReportFilters');
    const reportContent = shallowComponent.find('ReportContent');
    expect(reportTitle).toExist();
    expect(reportStatusBar).toExist();
    expect(reportFilters).toExist();
    expect(reportContent).toExist();
  });

  it('renders ReportTitle with props, ', function() {
    const reportTitle = getShallowComponent().find('ReportTitle');
    const metadata = {
      reportTitle: 'Title',
      application: {
        name: 'App Name'
      }
    };
    const selectedReport = {
      reportVersion: 3,
      knownArtifactCount: 1,
      totalArtifactCount: 2,
      policyComponentCount: 1,
      grandfatheredPolicyViolationCount: 0,
      criticalViolationCount: 1,
      severeViolationCount: 2,
      moderateViolationCount: 3,
      nonLowViolationCount: 0
    };

    expect(reportTitle).toExist();
    expect(reportTitle).toHaveProp('metadataDetails', metadata);
    expect(reportTitle).toHaveProp('scanId', 'scanId');
    expect(reportTitle).toHaveProp('publicId', 'publicId');
    expect(reportTitle).toHaveProp('selectedReport', selectedReport);
    expect(reportTitle).toHaveProp('loadError', undefined);
  });

  it('renders aside for filters', function() {
    const component = getShallowComponent();
    const aside = component.find('aside');
    const pageDiv = getShallowComponent().find('div.nx-page');
    const pageContentDiv = getShallowComponent().find('div.nx-page-content');
    const pageMainDiv = getShallowComponent().find('div.nx-page-main');
    expect(pageDiv).toExist();
    expect(pageContentDiv).toExist();
    expect(pageMainDiv).toExist();
    expect(aside).toExist();
    expect(aside).toHaveProp('className', 'nx-page-sidebar');
  });

  it('renders ReportStatusBar with props, ', function() {
    const reportStatusBar = getShallowComponent().find('ReportStatusBar');
    const selectedReport = {
      reportVersion: 3,
      knownArtifactCount: 1,
      totalArtifactCount: 2,
      policyComponentCount: 1,
      grandfatheredPolicyViolationCount: 0,
      criticalViolationCount: 1,
      severeViolationCount: 2,
      moderateViolationCount: 3,
      nonLowViolationCount: 0
    };
    expect(reportStatusBar).toExist();
    expect(reportStatusBar).toHaveProp('selectedReport', selectedReport);
  });

  it('renders ReportContent with props, ', function() {
    const props = {
      selectedReport: {
        reportVersion: 3
      },
      sortConfiguration: {
        key: 'policyThreatLevel',
        sortFields: ['-policyThreatLevel', 'policyName', 'derivedComponentName'],
        dir: 'desc'
      }
    };

    const reportContent = getShallowComponent(props).find('ReportContent');
    expect(reportContent).toExist();
    expect(reportContent).toHaveProp('selectedReport', props.selectedReport);
    expect(reportContent).toHaveProp('sortConfiguration', props.sortConfiguration);
  });
});
