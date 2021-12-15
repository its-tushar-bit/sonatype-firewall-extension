/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import LoadWrapper from '../../../main/frontend/react/LoadWrapper';

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
    };

    getShallowComponent = enzymeUtils.getShallowComponent(QuarantinedComponentReport, minimalProps);
  });

  it('renders a component with the "nx-page-main" class', function () {
    expect(getShallowComponent().find('.nx-page-main')).toExist();
  });

  it('renders a loading LoadWrapper when dataLoading is true', function () {
    const component = getShallowComponent();
    const loadWrapper = component.find(LoadWrapper);
    expect(loadWrapper).toHaveProp('loading', true);
  });

  it('passes any loadError to the LoadWrapper', function () {
    const component = getShallowComponent({ loadError: 'error' });
    const loadWrapper = component.find(LoadWrapper);
    expect(loadWrapper).toHaveProp('error', 'error');
  });

  it('calls loadQuarantineReportData when the LoadWrapper retryHandler is invoked', function () {
    const loadWrapper = getShallowComponent().find(LoadWrapper),
      retryHandler = loadWrapper.prop('retryHandler');

    expect(loadQuarantineReportDataSpy).not.toHaveBeenCalled();

    retryHandler();

    expect(loadQuarantineReportDataSpy).toHaveBeenCalled();
  });
});
