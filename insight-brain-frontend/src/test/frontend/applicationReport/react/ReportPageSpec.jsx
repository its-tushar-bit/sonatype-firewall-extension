/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import ReportPage from '../../../../main/frontend/applicationReport/react/ReportPage';
//import ReportFilters from '../../../../main/frontend/applicationReport/react/ReportFilters';
import ReportTitle from '../../../../main/frontend/applicationReport/react/ReportTitle';
import { NxLoadWrapper } from '@sonatype/react-shared-components';
import * as routerContext from 'MainRoot/react/RouterStateContext';

describe('Report Page component', function () {
  let getShallowComponent,
    loadReportActionMock,
    toggleAggregateReportEntriesSpy,
    setExactValueFilterSpy,
    routerContextMock;

  beforeEach(function () {
    loadReportActionMock = jasmine.createSpy('loadReport');
    toggleAggregateReportEntriesSpy = jasmine.createSpy('toggleAggregateReportEntries');
    setExactValueFilterSpy = jasmine.createSpy('setExactValueFilter');

    routerContextMock = {
      href: jasmine.createSpy('href').and.returnValue('mockValue'),
    };
    spyOn(routerContext, 'useRouterState').and.returnValue(routerContextMock);

    const minimalProps = {
      metadata: {
        reportTitle: 'Title',
        application: {
          name: 'App Name',
        },
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
        nonLowViolationCount: 0,
      },
      loading: false,
      loadReport: loadReportActionMock,
      toggleAggregateReportEntries: toggleAggregateReportEntriesSpy,
      setExactValueFilter: setExactValueFilterSpy,
    };

    getShallowComponent = enzymeUtils.getShallowComponent(ReportPage, minimalProps);
  });

  it('renders a ReportTitle wrapped in a NxLoadWrapper', function () {
    const component = getShallowComponent();

    expect(component.find(NxLoadWrapper)).toExist();
    expect(component.find(NxLoadWrapper).find(ReportTitle)).toExist();
  });

  describe('NxLoadWrapper', function () {
    it('has the loading flag set based on the corresponding prop', function () {
      expect(getShallowComponent().find(NxLoadWrapper)).toHaveProp('loading', false);
      expect(getShallowComponent({ loading: true }).find(NxLoadWrapper)).toHaveProp('loading', true);
    });

    it('has the error set to the loadError', function () {
      expect(getShallowComponent().find(NxLoadWrapper)).toHaveProp('error', undefined);
      expect(getShallowComponent({ loadError: 'foo' }).find(NxLoadWrapper)).toHaveProp('error', 'foo');
    });

    it('has the retryHandler set to the loadReport prop', function () {
      const loadReportIfNeeded = jasmine.createSpy();

      expect(getShallowComponent({ loadReportIfNeeded }).find(NxLoadWrapper)).toHaveProp(
        'retryHandler',
        loadReportIfNeeded
      );
    });
  });

  it('renders a ReportTitle ReportStatusBar ReportFilters ReportContent', function () {
    const shallowComponent = getShallowComponent();
    const reportTitle = shallowComponent.find('ReportTitle');
    const reportStatusBar = shallowComponent.find('ReportStatusBar');
    //const reportFilters = shallowComponent.find('ReportFilters');
    const reportContent = shallowComponent.find('ReportContent');
    expect(reportTitle).toExist();
    expect(reportStatusBar).toExist();
    //expect(reportFilters).toExist();
    expect(reportContent).toExist();
  });

  it('renders ReportTitle with props, ', function () {
    const reportTitle = getShallowComponent().find('ReportTitle');
    const metadata = {
      reportTitle: 'Title',
      application: {
        name: 'App Name',
      },
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
      nonLowViolationCount: 0,
    };

    expect(reportTitle).toExist();
    expect(reportTitle).toHaveProp('metadataDetails', metadata);
    expect(reportTitle).toHaveProp('scanId', 'scanId');
    expect(reportTitle).toHaveProp('publicId', 'publicId');
    expect(reportTitle).toHaveProp('selectedReport', selectedReport);
  });

  /*it('renders a ReportFilters', function () {
    const component = getShallowComponent();

    const reportElement = component.find(ReportFilters);
    expect(reportElement).toExist();
    expect(reportElement).toHaveProp('$state', mock$State);
    expect(reportElement).toHaveProp('setExactValueFilter', setExactValueFilterSpy);
  });*/

  it('renders ReportStatusBar with props, ', function () {
    const reportStatusBar = getShallowComponent().find('ReportStatusBar');
    expect(reportStatusBar).toExist();
  });

  it('renders ReportContent', function () {
    const reportContent = getShallowComponent().find('ReportContent');
    expect(reportContent).toExist();
  });

  it('renders a NxLoadWrapper', function () {
    const loadWrapper = getShallowComponent().find(NxLoadWrapper);
    expect(loadWrapper).toExist();
  });

  it('passes any error to the NxLoadWrapper', function () {
    const component = getShallowComponent({ loadError: 'error' });
    const loadWrapper = component.find(NxLoadWrapper);
    expect(loadWrapper).toHaveProp('error', 'error');
  });
});
