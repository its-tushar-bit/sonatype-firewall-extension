/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import { NxLoadWrapper } from '@sonatype/react-shared-components';

describe('QuarantinedComponentReport', function () {
  let minimalProps, QuarantinedComponentReport, loadComponentSpy, getShallowComponent;

  beforeEach(function () {
    QuarantinedComponentReport = require('inject-loader!../../../main/frontend/quarantinedComponentReport/QuarantinedComponentReport')(
      {}
    ).default;

    loadComponentSpy = jasmine.createSpy('loadComponent');

    minimalProps = {
      token: 'token',
      loadError: null,
      dataLoading: false,
      repositoryComponentId: 'repcomid',
      loadComponent: loadComponentSpy,
    };

    getShallowComponent = enzymeUtils.getShallowComponent(QuarantinedComponentReport, minimalProps);
  });

  it('renders a component with the "nx-page-main" class', function () {
    expect(getShallowComponent().find('.nx-page-main')).toExist();
  });

  it('renders a loading LoadWrapper when dataLoading is true', function () {
    const component = getShallowComponent({
      dataLoading: true,
    });
    const loadWrapper = component.find(NxLoadWrapper);
    expect(loadWrapper).toHaveProp('loading', true);
  });

  it('passes any loadError to the LoadWrapper', function () {
    const component = getShallowComponent({ loadError: 'error' });
    const loadWrapper = component.find(NxLoadWrapper);
    expect(loadWrapper).toHaveProp('error', 'error');
  });

  it('calls loadFirewallData when the LoadWrapper retryHandler is invoked', function () {
    const loadWrapper = getShallowComponent().find(NxLoadWrapper),
      retryHandler = loadWrapper.prop('retryHandler');

    expect(loadComponentSpy).not.toHaveBeenCalled();

    retryHandler();

    expect(loadComponentSpy).toHaveBeenCalled();
  });
});
