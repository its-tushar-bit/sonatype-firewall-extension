/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
// import React from 'react';
import ListWaiversBackButton from '../../../main/frontend/waivers/ListWaiversBackButton';
import { NxBackButton } from '@sonatype/react-shared-components';
import * as routerContext from '../../../main/frontend/react/RouterStateContext';

describe('ListWaiversBackButton', function () {
  let minimalProps, getShallowComponent, routerContextMock, hrefSpy;

  beforeEach(function () {
    minimalProps = {
      violationId: 'violationId',
    };
    hrefSpy = jasmine.createSpy('href').and.callFake((stateName) => {
      return stateName === 'applicationReport.componentDetails.violations'
        ? 'componentDetailsHref'
        : 'violationDetailsHref';
    });
    routerContextMock = { href: hrefSpy };
    spyOn(routerContext, 'useRouterState').and.returnValue(routerContextMock);

    getShallowComponent = enzymeUtils.getShallowComponent(ListWaiversBackButton, minimalProps);
  });

  it('renders an NxBackButton with title `Violation Details` if only violationId is supplied as props', () => {
    const component = getShallowComponent();

    expect(routerContext.useRouterState).toHaveBeenCalled();
    expect(hrefSpy).toHaveBeenCalledWith('sidebarView.violation', {
      id: 'violationId',
      type: undefined,
      sidebarReference: undefined,
    });
    expect(component).toMatchSelector(NxBackButton);
    expect(component).toHaveProp('targetPageTitle', 'Violation Details');
    expect(component).toHaveProp('href', 'violationDetailsHref');
  });

  it('renders an NxBackButton with title `Violation Details` with type and sidebarReference props', () => {
    const component = getShallowComponent({ type: 'type', sidebarReference: 'ref1' });

    expect(routerContext.useRouterState).toHaveBeenCalled();
    expect(hrefSpy).toHaveBeenCalledWith('sidebarView.violation', {
      id: 'violationId',
      type: 'type',
      sidebarReference: 'ref1',
    });
    expect(component).toMatchSelector(NxBackButton);
    expect(component).toHaveProp('targetPageTitle', 'Violation Details');
    expect(component).toHaveProp('href', 'violationDetailsHref');
  });

  it('renders an NxBackButton with title `Violation Details` if hash is not present', () => {
    const component = getShallowComponent({
      scanId: 'scanId',
      publicId: 'publicId',
    });

    expect(routerContext.useRouterState).toHaveBeenCalled();
    expect(hrefSpy).toHaveBeenCalledWith('sidebarView.violation', {
      id: 'violationId',
      type: undefined,
      sidebarReference: undefined,
    });
    expect(component).toMatchSelector(NxBackButton);
    expect(component).toHaveProp('targetPageTitle', 'Violation Details');
    expect(component).toHaveProp('href', 'violationDetailsHref');
  });

  it('renders an NxBackButton with title `Violation Details` if scanId is not present', () => {
    const component = getShallowComponent({
      hash: 'hash',
      publicId: 'publicId',
    });

    expect(routerContext.useRouterState).toHaveBeenCalled();
    expect(hrefSpy).toHaveBeenCalledWith('sidebarView.violation', {
      id: 'violationId',
      type: undefined,
      sidebarReference: undefined,
    });
    expect(component).toMatchSelector(NxBackButton);
    expect(component).toHaveProp('targetPageTitle', 'Violation Details');
    expect(component).toHaveProp('href', 'violationDetailsHref');
  });

  it('renders an NxBackButton with title `Violation Details` if publicId is not present', () => {
    const component = getShallowComponent({
      hash: 'hash',
      scanId: 'scanId',
    });

    expect(routerContext.useRouterState).toHaveBeenCalled();
    expect(hrefSpy).toHaveBeenCalledWith('sidebarView.violation', {
      id: 'violationId',
      type: undefined,
      sidebarReference: undefined,
    });
    expect(component).toMatchSelector(NxBackButton);
    expect(component).toHaveProp('targetPageTitle', 'Violation Details');
    expect(component).toHaveProp('href', 'violationDetailsHref');
  });

  it('renders an NxBackButton with title `Component Details` if hash & scanId & publicId are provided as props', () => {
    const component = getShallowComponent({
      hash: 'hash',
      scanId: 'scanId',
      publicId: 'publicId',
    });

    expect(routerContext.useRouterState).toHaveBeenCalled();
    expect(hrefSpy).toHaveBeenCalledWith('applicationReport.componentDetails.violations', {
      hash: 'hash',
      scanId: 'scanId',
      publicId: 'publicId',
    });
    expect(component).toMatchSelector(NxBackButton);
    expect(component).toHaveProp('targetPageTitle', 'Component Details');
    expect(component).toHaveProp('href', 'componentDetailsHref');
  });
});
