/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import ListWaiversBackButton from '../../../main/frontend/waivers/ListWaiversBackButton';
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';
import * as routerContext from '../../../main/frontend/react/RouterStateContext';

describe('ListWaiversBackButton', function () {
  let minimalProps, getShallowComponent, routerContextMock, hrefSpy;

  beforeEach(function () {
    minimalProps = {
      violationId: 'violationId',
      hash: 'hash',
      repositoryPolicyId: 'repositoryPolicyId',
      componentIdentifier: 'componentIdentifier',
      componentHash: 'componentHash',
      matchState: 'matchState',
      pathname: 'pathname',
      tabId: 'violations',
      isFirewall: false,
    };
    hrefSpy = jasmine.createSpy('href').and.callFake((stateName) => {
      switch (stateName) {
        case 'applicationReport.componentDetails.violations':
          return 'componentDetailsHref';
        case `firewall.componentDetailsPage.violations`:
          return 'firewallComponentDetailsPageHref';
        case `repository.componentDetailsPage.violations`:
          return 'repositoryComponentDetailsPageHref';
        default:
          return 'violationDetailsHref';
      }
    });
    routerContextMock = { href: hrefSpy };
    spyOn(routerContext, 'useRouterState').and.returnValue(routerContextMock);

    getShallowComponent = enzymeUtils.getShallowComponent(ListWaiversBackButton, minimalProps);
  });

  it('renders an MenuBarBackButton with title `Back to Violation Details` if only violationId is supplied as props', () => {
    const component = getShallowComponent();

    expect(routerContext.useRouterState).toHaveBeenCalled();
    expect(hrefSpy).toHaveBeenCalledWith('sidebarView.violation', {
      id: 'violationId',
      type: undefined,
      sidebarReference: undefined,
    });
    expect(component).toMatchSelector(MenuBarBackButton);
    expect(component).toHaveProp('text', 'Back to Violation Details');
    expect(component).toHaveProp('href', 'violationDetailsHref');
  });

  it('renders an MenuBarBackButton with title `Back to Violation Details` with type and sidebarReference props', () => {
    const component = getShallowComponent({ type: 'type', sidebarReference: 'ref1' });

    expect(routerContext.useRouterState).toHaveBeenCalled();
    expect(hrefSpy).toHaveBeenCalledWith('sidebarView.violation', {
      id: 'violationId',
      type: 'type',
      sidebarReference: 'ref1',
    });
    expect(component).toMatchSelector(MenuBarBackButton);
    expect(component).toHaveProp('text', 'Back to Violation Details');
    expect(component).toHaveProp('href', 'violationDetailsHref');
  });

  it('renders an MenuBarBackButton with title `Back to Violation Details` if hash is not present', () => {
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
    expect(component).toMatchSelector(MenuBarBackButton);
    expect(component).toHaveProp('text', 'Back to Violation Details');
    expect(component).toHaveProp('href', 'violationDetailsHref');
  });

  it('renders an MenuBarBackButton with title `Back to Violation Details` if scanId is not present', () => {
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
    expect(component).toMatchSelector(MenuBarBackButton);
    expect(component).toHaveProp('text', 'Back to Violation Details');
    expect(component).toHaveProp('href', 'violationDetailsHref');
  });

  it('renders an MenuBarBackButton with title `Back to Violation Details` if publicId is not present', () => {
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
    expect(component).toMatchSelector(MenuBarBackButton);
    expect(component).toHaveProp('text', 'Back to Violation Details');
    expect(component).toHaveProp('href', 'violationDetailsHref');
  });

  it('renders an MenuBarBackButton with title `Back to Component Details` if hash & scanId & publicId & previousRouterStateNameForComponentDetails are provided as props', () => {
    const component = getShallowComponent({
      hash: 'hash',
      scanId: 'scanId',
      publicId: 'publicId',
      previousRouterStateNameForComponentDetails: 'applicationReport.componentDetails.violations',
      isFirewall: false,
      isFirewallOrRepositoryComponent: false,
    });

    expect(routerContext.useRouterState).toHaveBeenCalled();
    expect(hrefSpy).toHaveBeenCalledWith('applicationReport.componentDetails.violations', {
      hash: 'hash',
      scanId: 'scanId',
      publicId: 'publicId',
    });
    expect(component).toMatchSelector(MenuBarBackButton);
    expect(component).toHaveProp('text', 'Back to Component Details');
    expect(component).toHaveProp('href', 'componentDetailsHref');
  });

  it('renders an MenuBarBackButton with title `Back to Component Details` for Firewall route if prevParams and previousRouterStateNameForComponentDetails are provided as props', () => {
    const component = getShallowComponent({
      previousRouterStateNameForComponentDetails: 'firewall.componentDetailsPage.violations',
      repositoryId: 'repositoryPolicyId',
      componentIdentifier: 'componentIdentifier',
      componentHash: 'hash',
      matchState: 'matchState',
      pathname: 'pathname',
      isFirewall: true,
      isFirewallOrRepositoryComponent: true,
    });

    expect(routerContext.useRouterState).toHaveBeenCalled();
    expect(hrefSpy).toHaveBeenCalledWith('firewall.componentDetailsPage.violations', {
      repositoryId: 'repositoryPolicyId',
      componentIdentifier: 'componentIdentifier',
      componentHash: 'hash',
      matchState: 'matchState',
      pathname: 'pathname',
      componentDisplayName: undefined,
    });
    expect(component).toMatchSelector(MenuBarBackButton);
    expect(component).toHaveProp('text', 'Back to Component Details');
    expect(component).toHaveProp('href', 'firewallComponentDetailsPageHref');
  });

  it('renders an MenuBarBackButton with title `Back to Component Details` for Respository route if prevParams and previousRouterStateNameForComponentDetails are provided as props', () => {
    const component = getShallowComponent({
      previousRouterStateNameForComponentDetails: 'firewall.componentDetailsPage.violations',
      repositoryId: 'repositoryPolicyId',
      componentIdentifier: 'componentIdentifier',
      componentHash: 'hash',
      matchState: 'matchState',
      pathname: 'pathname',
      isFirewall: false,
      isFirewallOrRepositoryComponent: true,
    });

    expect(routerContext.useRouterState).toHaveBeenCalled();
    expect(hrefSpy).toHaveBeenCalledWith('repository.componentDetailsPage.violations', {
      repositoryId: 'repositoryPolicyId',
      componentIdentifier: 'componentIdentifier',
      componentHash: 'hash',
      matchState: 'matchState',
      pathname: 'pathname',
      componentDisplayName: undefined,
    });
    expect(component).toMatchSelector(MenuBarBackButton);
    expect(component).toHaveProp('text', 'Back to Component Details');
    expect(component).toHaveProp('href', 'repositoryComponentDetailsPageHref');
  });
});
