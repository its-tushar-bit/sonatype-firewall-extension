/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import LoadWrapper from '../../../main/frontend/react/LoadWrapper';
import PolicyViolationsTile from 'MainRoot/quarantinedComponentReport/policyViolationsTile/PolicyViolationsTile';

describe('QuarantinedComponentReport', function () {
  let minimalProps, QuarantinedComponentReport, loadQuarantineReportDataSpy, getShallowComponent;

  beforeEach(function () {
    QuarantinedComponentReport = require('inject-loader!../../../main/frontend/quarantinedComponentReport/QuarantinedComponentReport')(
      {}
    ).default;

    loadQuarantineReportDataSpy = jasmine.createSpy('loadQuarantineReportData');

    minimalProps = {
      token: 'token',
      loadError: null,
      loadQuarantineReportData: loadQuarantineReportDataSpy,
      componentOverview: {
        componentOverviewLoading: true,
      },
      violations: { activePolicyViolations: [] },
      violationsLoading: true,
      violationsLoadError: null,
    };

    getShallowComponent = enzymeUtils.getShallowComponent(QuarantinedComponentReport, minimalProps);
  });

  it('renders a component with the "nx-page-main" class', function () {
    expect(getShallowComponent().find('.nx-page-main')).toExist();
  });

  it('renders a loading LoadWrapper when dataLoading is true', function () {
    const component = getShallowComponent();
    const loadWrapper = component.find(LoadWrapper).first();
    expect(loadWrapper).toHaveProp('loading', true);
  });

  it('passes any loadError to the LoadWrapper', function () {
    const component = getShallowComponent({ loadError: 'error' });
    const loadWrapper = component.find(LoadWrapper).first();
    expect(loadWrapper).toHaveProp('error', 'error');
  });

  it('calls loadQuarantineReportData when the LoadWrapper retryHandler is invoked', function () {
    const loadWrapper = getShallowComponent().find(LoadWrapper).first(),
      retryHandler = loadWrapper.prop('retryHandler');

    expect(loadQuarantineReportDataSpy).not.toHaveBeenCalled();

    retryHandler();

    expect(loadQuarantineReportDataSpy).toHaveBeenCalled();
  });

  it('renders a loading LoadWrapper when violationsLoading is true', function () {
    const component = getShallowComponent();
    const loadWrapper = component.find(LoadWrapper).at(1);
    expect(loadWrapper).toHaveProp('loading', true);
  });

  it('passes any violationsLoadError to the LoadWrapper', function () {
    const component = getShallowComponent({ violationsLoadError: 'error' });
    const loadWrapper = component.find(LoadWrapper).at(1);
    expect(loadWrapper).toHaveProp('error', 'error');
  });

  it('renders PolicyViolationsTile with Props', function () {
    const policyViolationsTile = getShallowComponent({ loading: false })
      .find(LoadWrapper)
      .at(1)
      .find(PolicyViolationsTile);
    const violations = { activePolicyViolations: [] };
    expect(policyViolationsTile).toExist();
    expect(policyViolationsTile).toHaveProp('violations', violations);
  });

  it('calls loadQuarantineReportData when the LoadWrapper retryHandler is invoked', function () {
    const loadWrapper = getShallowComponent().find(LoadWrapper).at(1),
      retryHandler = loadWrapper.prop('retryHandler');

    expect(loadQuarantineReportDataSpy).not.toHaveBeenCalled();

    retryHandler();

    expect(loadQuarantineReportDataSpy).toHaveBeenCalled();
  });
});
