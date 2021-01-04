/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import ReportPage from '../../../../main/frontend/applicationReport/react/ReportPage';
import ReportFilters from '../../../../main/frontend/applicationReport/react/ReportFilters';
import ReportTitle from '../../../../main/frontend/applicationReport/react/ReportTitle';
import LoadWrapper from '../../../../main/frontend/react/LoadWrapper';

describe('Report Page component', function() {
  let getShallowComponent,
      loadReportActionMock,
      setAggregateReportEntriesSpy,
      setExactValueFilterSpy,
      mock$State;

  beforeEach(function() {

    loadReportActionMock = jasmine.createSpy('loadReport');
    setAggregateReportEntriesSpy = jasmine.createSpy('setAggregateReportEntries');
    setExactValueFilterSpy = jasmine.createSpy('setExactValueFilter');
    mock$State = jasmine.createSpyObj('$state', ['get', 'href']);

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
      loading: false,
      loadReport: loadReportActionMock,
      setAggregateReportEntries: setAggregateReportEntriesSpy,
      setExactValueFilter: setExactValueFilterSpy,
      $state: mock$State
    };

    getShallowComponent = enzymeUtils.getShallowComponent(ReportPage, minimalProps);
  });

  it('renders a ReportTitle wrapped in a LoadWrapper', function() {
    const component = getShallowComponent();

    expect(component.find(LoadWrapper)).toExist();
    expect(component.find(LoadWrapper).find(ReportTitle)).toExist();
  });

  describe('LoadWraper', function() {
    it('has the loading flag set based on the corresponding prop', function() {
      expect(getShallowComponent().find(LoadWrapper)).toHaveProp('loading', false);
      expect(getShallowComponent({ loading: true }).find(LoadWrapper)).toHaveProp('loading', true);
    });

    it('has the error set to the loadError', function() {
      expect(getShallowComponent().find(LoadWrapper)).toHaveProp('error', undefined);
      expect(getShallowComponent({ loadError: 'foo' }).find(LoadWrapper)).toHaveProp('error', 'foo');
    });

    it('has the retryHandler set to the loadReport prop', function() {
      const loadReport = jasmine.createSpy();

      expect(getShallowComponent({ loadReport }).find(LoadWrapper)).toHaveProp('retryHandler', loadReport);
    });
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
  });

  it('renders a ReportFilters', function() {
    const component = getShallowComponent();

    const reportElement = component.find(ReportFilters);
    expect(reportElement).toExist();
    expect(reportElement).toHaveProp('$state', mock$State);
    expect(reportElement).toHaveProp('setAggregateReportEntries', setAggregateReportEntriesSpy);
    expect(reportElement).toHaveProp('setExactValueFilter', setExactValueFilterSpy);
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
      },
      substringFilters: {
        policyName: 'policyName',
        derivedComponentName: 'derivedComponentName'
      }
    };

    const reportContent = getShallowComponent(props).find('ReportContent');
    expect(reportContent).toExist();
    expect(reportContent).toHaveProp('selectedReport', props.selectedReport);
    expect(reportContent).toHaveProp('sortConfiguration', props.sortConfiguration);
    expect(reportContent).toHaveProp('substringFilters', props.substringFilters);
  });

  it('renders a LoadWrapper', function() {
    const loadWrapper = getShallowComponent().find('LoadWrapper');
    expect(loadWrapper).toExist();
  });

  it('passes any error to the LoadWrapper', function() {
    const component = getShallowComponent({ loadError: 'error' });
    const loadWrapper = component.find(LoadWrapper);
    expect(loadWrapper).toHaveProp('error', 'error');
  });

});
