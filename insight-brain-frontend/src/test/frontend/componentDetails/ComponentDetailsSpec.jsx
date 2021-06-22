/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as enzymeUtils from '../enzymeUtils';
import { NxStatefulTabs, NxTab } from '@sonatype/react-shared-components';

import ComponentDetails from '../../../main/frontend/componentDetails/ComponentDetails';
import { ComponentDetailsFooter } from '../../../main/frontend/componentDetails/ComponentDetailsFooter';
import BackButton from '../../../main/frontend/react/BackButton';
import LoadError from '../../../main/frontend/react/LoadError';
import * as routerContext from '../../../main/frontend/react/RouterStateContext';
import * as fullAuditLog from '../../../main/frontend/componentDetails/auditLog/AuditLogContainer';
import * as violationsTab from '../../../main/frontend/componentDetails/violations/PolicyViolationsContainer';

describe('ComponentDetails', function () {
  let minimalProps,
    getShallowComponent,
    getMountedComponent,
    loadComponentDetailsSpy,
    stateMock,
    stateGetSpy,
    onTabChangeSpy;

  beforeEach(function () {
    loadComponentDetailsSpy = jasmine.createSpy('loadComponentDetails');
    onTabChangeSpy = jasmine.createSpy('onTabChange');

    stateGetSpy = jasmine.createSpy('$state.get').and.returnValue({ data: { title: 'some title' } });
    stateMock = {
      get: stateGetSpy,
      href: () => {},
    };
    spyOn(routerContext, 'useRouterState').and.returnValue(stateMock);

    minimalProps = {
      componentDetails: null,
      activeTabId: 'remediation',
      loadComponentDetails: loadComponentDetailsSpy,
      onTabChange: onTabChangeSpy,
      pagination: null,
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

  it('calls loadComponentDetails if there is NO componentDetails in the state', () => {
    const component = getMountedComponent();
    expect(loadComponentDetailsSpy).toHaveBeenCalled();
    component.unmount();
  });

  it('does not calls loadComponentDetails if there IS a componentDetails in the state', () => {
    const component = getMountedComponent({
      componentDetails: {
        name: 'Mock Component Name',
        hash: 'some-crazy-hash',
      },
    });
    expect(loadComponentDetailsSpy).not.toHaveBeenCalled();
    component.unmount();
  });

  describe('renders tabs', function () {
    it('does not render tabs if there is no selected component', function () {
      const component = getShallowComponent(),
        tabBar = component.find(NxStatefulTabs);

      expect(tabBar).not.toExist();
    });

    it('renders 6 tabs with the appropriate names when there is componentDetails prop', function () {
      const component = getShallowComponent({
          componentDetails: {
            name: 'Mock Component Name',
            hash: 'some-crazy-hash',
          },
        }),
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

    it('calls onTabChange action with the appropriate activeTabId when clicking on a tab', function () {
      // Mock containers so that `getMountedComponent` doesn't complain.
      spyOn(fullAuditLog, 'default').and.returnValue(<div>auditLog</div>);
      spyOn(violationsTab, 'PolicyViolationsContainer').and.returnValue(<div>violations</div>);

      let component = getMountedComponent({
          componentDetails: {
            name: 'Mock Component Name',
            hash: 'some-crazy-hash',
          },
        }),
        tabBar = component.find(NxStatefulTabs),
        tabs = tabBar.find(NxTab);

      tabs.at(1).simulate('click');
      expect(onTabChangeSpy).toHaveBeenCalledWith('info');

      tabs.at(2).simulate('click');
      expect(onTabChangeSpy).toHaveBeenCalledWith('violations');

      tabs.at(3).simulate('click');
      expect(onTabChangeSpy).toHaveBeenCalledWith('security');

      tabs.at(4).simulate('click');
      expect(onTabChangeSpy).toHaveBeenCalledWith('legal');

      tabs.at(5).simulate('click');
      expect(onTabChangeSpy).toHaveBeenCalledWith('audit');

      /** Starting on another tab to be able to check the listener on the default 0 tab */
      const onTabChangeInInfoSpy = jasmine.createSpy('onTabChange');
      (component = getMountedComponent({
        componentDetails: {
          name: 'Mock Component Name',
          hash: 'some-crazy-hash',
        },
        activeTabId: 'info',
        onTabChange: onTabChangeInInfoSpy,
      })),
        (tabBar = component.find(NxStatefulTabs)),
        (tabs = tabBar.find(NxTab));

      tabs.at(0).simulate('click');
      expect(onTabChangeInInfoSpy).toHaveBeenCalledWith('remediation');
    });

    it('does not call onTabChange when clicking on the same tab twice', function () {
      // Mock `AuditLogContainer` so that `getMountedComponent` doesn't complain.
      spyOn(fullAuditLog, 'default').and.returnValue(<div>hello</div>);

      const component = getMountedComponent({
          componentDetails: {
            name: 'Mock Component Name',
            hash: 'some-crazy-hash',
          },
        }),
        tabBar = component.find(NxStatefulTabs),
        tabs = tabBar.find(NxTab),
        defaultTab = tabs.at(0);

      defaultTab.simulate('click');
      expect(onTabChangeSpy).not.toHaveBeenCalled();
    });
  });

  describe('pagination', () => {
    it('renders a ComponentDetailsFooter component and spreads the pagination prop to it, when pagination prop is passed', () => {
      const mockPagination = {
        next: '/next-component',
      };
      const el = getShallowComponent({
        componentDetails: {
          name: 'Mock Component Name',
          hash: 'some-crazy-hash',
        },
        pagination: mockPagination,
      });
      expect(el.find(ComponentDetailsFooter)).toExist();
    });
  });

  describe('when there is an error loading the report', () => {
    beforeEach(() => {
      minimalProps = {
        componentDetails: null,
        activeTabId: 'remediation',
        loadComponentDetails: loadComponentDetailsSpy,
        onTabChange: onTabChangeSpy,
        pagination: null,
        applicationReportLoadError: 'Mock message',
      };
    });

    it('renders a LoadError component', () => {
      const el = getShallowComponent(minimalProps).find(LoadError);
      expect(el).toExist();
      expect(el).toHaveProp('error', 'Mock message');
    });

    it('calls loadComponentDetails when the user clicks the retry button', () => {
      const component = getMountedComponent(minimalProps);
      component.find(LoadError).props().retryHandler();
      expect(loadComponentDetailsSpy).toHaveBeenCalled();
      component.unmount();
    });

    describe('when app fails to get componentDetails and there is no applicationReportError', () => {
      beforeEach(() => {
        minimalProps = {
          componentDetails: null,
          activeTabId: 'remediation',
          loadComponentDetails: loadComponentDetailsSpy,
          onTabChange: onTabChangeSpy,
          pagination: null,
          applicationReportLoadError: null,
        };
      });

      it('renders a componentDetails error', () => {
        const el = getShallowComponent(minimalProps).find(LoadError);
        expect(el).toExist();
        expect(el).toHaveProp('error', 'Error getting component details.');
      });
    });
  });
});
