/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import { NxStatefulTabs, NxTab } from '@sonatype/react-shared-components';

import ComponentDetails from '../../../main/frontend/componentDetails/ComponentDetails';
import BackButton from '../../../main/frontend/react/BackButton';
import * as routerContext from '../../../main/frontend/react/RouterStateContext';

describe('ComponentDetails', function () {
  let minimalProps,
    getShallowComponent,
    getMountedComponent,
    loadReportAndSelectComponentSpy,
    stateMock,
    stateGetSpy,
    stateGoSpy;

  beforeEach(function () {
    loadReportAndSelectComponentSpy = jasmine.createSpy('loadResults');
    stateGoSpy = jasmine.createSpy('stateGo');

    stateGetSpy = jasmine.createSpy('$state.get').and.returnValue({ data: { title: 'some title' } });
    stateMock = {
      get: stateGetSpy,
      href: () => {},
    };
    spyOn(routerContext, 'useRouterState').and.returnValue(stateMock);

    minimalProps = {
      componentDetails: null,
      publicId: 'publicId',
      scanId: 'scanId',
      unknownjs: false,
      hash: 'hash',
      tabId: 'remediation',
      loadReportAndSelectComponentByHash: loadReportAndSelectComponentSpy,
      stateGo: stateGoSpy,
    };

    getShallowComponent = enzymeUtils.getShallowComponent(ComponentDetails, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(ComponentDetails, minimalProps);
  });

  it('renders a component', () => {
    expect(getShallowComponent()).toExist();
  });

  it('renders a back button', () => {
    const el = getShallowComponent(),
      backBtn = el.find(BackButton);

    expect(backBtn).toHaveProp('stateName', 'applicationReport.policy');
    expect(backBtn).toHaveProp('$state', stateMock);
  });

  it('calls loadReportAndSelectComponentByHash if there is no componentDetails in the state', () => {
    const component = getMountedComponent();
    expect(loadReportAndSelectComponentSpy).toHaveBeenCalledWith('publicId', 'scanId', 'hash', false);
    component.unmount();
  });

  it('does not calls loadReportAndSelectComponentByHash if there is a componentDetails in the state', () => {
    const component = getMountedComponent({ componentDetails: { derivedComponentName: 'MockName' } });
    expect(loadReportAndSelectComponentSpy).not.toHaveBeenCalled();
    component.unmount();
  });

  describe('renders tabs', function () {
    it('does not render tabs if there is no selected component', function () {
      const component = getShallowComponent(),
        tabBar = component.find(NxStatefulTabs);

      expect(tabBar).not.toExist();
    });

    it('renders 6 tabs with the appropriate names when there is a selected component', function () {
      const component = getShallowComponent({ componentDetails: 'exists' }),
        tabBar = component.find(NxStatefulTabs);

      expect(tabBar).toExist();

      const tabs = tabBar.find(NxTab);

      expect(tabs.at(0)).toHaveProp('children', 'Remediation');
      expect(tabs.at(1)).toHaveProp('children', 'Component Info');
      expect(tabs.at(2)).toHaveProp('children', 'Policy Violations');
      expect(tabs.at(3)).toHaveProp('children', 'Security');
      expect(tabs.at(4)).toHaveProp('children', 'Legal');
      expect(tabs.at(5)).toHaveProp('children', 'Audit Log');
    });

    it('calls stateGo action with the appropriate state when clicking on a tab', function () {
      let component = getMountedComponent({ componentDetails: 'exists' }),
        tabBar = component.find(NxStatefulTabs),
        tabs = tabBar.find(NxTab);

      tabs.at(1).simulate('click');
      expect(stateGoSpy).toHaveBeenCalledWith('applicationReport.componentDetails.info', { hash: 'hash' });

      tabs.at(2).simulate('click');
      expect(stateGoSpy).toHaveBeenCalledWith('applicationReport.componentDetails.violations', { hash: 'hash' });

      tabs.at(3).simulate('click');
      expect(stateGoSpy).toHaveBeenCalledWith('applicationReport.componentDetails.security', { hash: 'hash' });

      tabs.at(4).simulate('click');
      expect(stateGoSpy).toHaveBeenCalledWith('applicationReport.componentDetails.legal', { hash: 'hash' });

      tabs.at(5).simulate('click');
      expect(stateGoSpy).toHaveBeenCalledWith('applicationReport.componentDetails.audit', { hash: 'hash' });

      /** Starting on another tab to be able to check the listener on the default 0 tab */
      const stateGoInInfoSpy = jasmine.createSpy('stateGo');
      (component = getMountedComponent({ componentDetails: 'exists', tabId: 'info', stateGo: stateGoInInfoSpy })),
        (tabBar = component.find(NxStatefulTabs)),
        (tabs = tabBar.find(NxTab));

      tabs.at(0).simulate('click');
      expect(stateGoInInfoSpy).toHaveBeenCalledWith('applicationReport.componentDetails.remediation', { hash: 'hash' });
    });

    it('does not call stateGo when clicking on the same tab twice', function () {
      const component = getMountedComponent({ componentDetails: 'exists' }),
        tabBar = component.find(NxStatefulTabs),
        tabs = tabBar.find(NxTab),
        defaultTab = tabs.at(0);

      defaultTab.simulate('click');
      expect(stateGoSpy).not.toHaveBeenCalled();
    });
  });
});
