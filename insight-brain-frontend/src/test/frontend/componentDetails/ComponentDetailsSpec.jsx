/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as enzymeUtils from '../enzymeUtils';
import { NxLoadError, NxLoadingSpinner, NxStatefulTabs, NxTab, NxButton } from '@sonatype/react-shared-components';

import ComponentDetails from '../../../main/frontend/componentDetails/ComponentDetails';
import { ComponentDetailsFooter } from '../../../main/frontend/componentDetails/ComponentDetailsFooter';
import * as routerContext from '../../../main/frontend/react/RouterStateContext';
import * as fullAuditLog from '../../../main/frontend/componentDetails/auditLog/AuditLogContainer';
import * as violationsTab from '../../../main/frontend/componentDetails/ViolationsTableTile/ViolationsTableTileContainer';
import * as overviewTab from '../../../main/frontend/componentDetails/overview/OverviewContainer';
import * as vulnerailitiesTile from '../../../main/frontend/componentDetails/VulnerabilitiesTableTile/VulnerabilitiesTableTileContainer';
import * as licenseDetectionsTile from 'MainRoot/componentDetails/LicenseDetectionsTile/LicenseDetectionsTileContainer';
import MenuBarBackButton from '../../../main/frontend/mainHeader/MenuBar/MenuBarBackButton';

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
      activeTabId: 'overview',
      loadComponentDetails: loadComponentDetailsSpy,
      onTabChange: onTabChangeSpy,
      pagination: null,
      loadError: null,
      loading: false,
    };

    getShallowComponent = enzymeUtils.getShallowComponent(ComponentDetails, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(ComponentDetails, minimalProps);
  });

  it('renders a component', () => {
    expect(getShallowComponent()).toExist();
  });

  it('renders a MenuBarBackButton', () => {
    const el = getShallowComponent(),
      backBtn = el.find(MenuBarBackButton);

    expect(backBtn).toHaveProp('stateName', 'applicationReport.policy');
  });

  it('calls loadComponentDetails if there is NO componentDetails in the state', () => {
    const component = getMountedComponent();
    expect(loadComponentDetailsSpy).toHaveBeenCalled();
    component.unmount();
  });

  describe('renders unknown component alert', function () {
    it('does not render if there is no unknown match state', function () {
      const component = getShallowComponent();
      const alertEl = component.find('.iq-component-details-unknown-component-alert');

      expect(alertEl).not.toExist();
    });

    it('renders unknown component alert when there is an unknown match state', function () {
      const component = getShallowComponent({
        componentDetails: {
          name: 'Mock Component Name',
          hash: 'some-crazy-hash',
          matchState: 'unknown',
        },
      });
      const alertEl = component.find('.iq-component-details-unknown-component-alert');
      const claimButton = alertEl.find(NxButton).at(0);
      const addButton = alertEl.find(NxButton).at(1);

      expect(alertEl).toExist();
      expect(alertEl.children().first().text()).toEqual('The component is unknown.');

      expect(claimButton).toExist();
      expect(claimButton).toHaveProp('title', 'Claim Component');
      expect(claimButton.text()).toEqual('Claim Component');

      expect(addButton).toExist();
      expect(addButton).toHaveProp('title', 'Add Propietary Component Matchers');
      expect(addButton.text()).toEqual('Add Propietary Component Matchers');
    });
  });

  describe('renders tabs', function () {
    it('does not render tabs if there is no selected component', function () {
      const component = getShallowComponent(),
        tabBar = component.find(NxStatefulTabs);

      expect(tabBar).not.toExist();
    });

    it('renders 5 tabs with the appropriate names when there is componentDetails prop', function () {
      const component = getShallowComponent({
          componentDetails: {
            name: 'Mock Component Name',
            hash: 'some-crazy-hash',
            matchState: 'exact',
          },
        }),
        tabBar = component.find(NxStatefulTabs);

      expect(tabBar).toExist();

      const tabs = tabBar.find(NxTab);

      expect(tabs.at(0)).toHaveProp('children', 'Overview');
      expect(tabs.at(1)).toHaveProp('children', 'Policy Violations');
      expect(tabs.at(2)).toHaveProp('children', 'Security');
      expect(tabs.at(3)).toHaveProp('children', 'Legal');
      expect(tabs.at(4)).toHaveProp('children', 'Audit Log');
    });

    it('calls onTabChange action with the appropriate activeTabId when clicking on a tab', function () {
      // Mock containers so that `getMountedComponent` doesn't complain.
      spyOn(fullAuditLog, 'default').and.returnValue(<div>auditLog</div>);
      spyOn(violationsTab, 'ViolationsTableTileContainer').and.returnValue(<div>violations</div>);
      spyOn(overviewTab, 'OverviewContainer').and.returnValue(<div>overview</div>);
      spyOn(vulnerailitiesTile, 'VulnerabilitiesTableTileContainer').and.returnValue(<div>vulnerabilities</div>);
      spyOn(licenseDetectionsTile, 'LicenseDetectionsTileContainer').and.returnValue(<div>license detections</div>);

      let component = getMountedComponent({
          componentDetails: {
            name: 'Mock Component Name',
            hash: 'some-crazy-hash',
            matchState: 'exact',
          },
        }),
        tabBar = component.find(NxStatefulTabs),
        tabs = tabBar.find(NxTab);

      tabs.at(1).simulate('click');
      expect(onTabChangeSpy).toHaveBeenCalledWith('violations');

      tabs.at(2).simulate('click');
      expect(onTabChangeSpy).toHaveBeenCalledWith('security');

      tabs.at(3).simulate('click');
      expect(onTabChangeSpy).toHaveBeenCalledWith('legal');

      tabs.at(4).simulate('click');
      expect(onTabChangeSpy).toHaveBeenCalledWith('audit');

      /** Starting on another tab to be able to check the listener on the default 0 tab */
      const onTabChangeInInfoSpy = jasmine.createSpy('onTabChange');
      (component = getMountedComponent({
        componentDetails: {
          name: 'Mock Component Name',
          hash: 'some-crazy-hash',
          matchState: 'exact',
        },
        activeTabId: 'security',
        onTabChange: onTabChangeInInfoSpy,
      })),
        (tabBar = component.find(NxStatefulTabs)),
        (tabs = tabBar.find(NxTab));

      tabs.at(0).simulate('click');
      expect(onTabChangeInInfoSpy).toHaveBeenCalledWith('overview');
    });

    it('does not call onTabChange when clicking on the same tab twice', function () {
      // Mock `AuditLogContainer` so that `getMountedComponent` doesn't complain.
      spyOn(fullAuditLog, 'default').and.returnValue(<div>hello</div>);
      spyOn(violationsTab, 'ViolationsTableTileContainer').and.returnValue(<div>violations</div>);
      spyOn(overviewTab, 'OverviewContainer').and.returnValue(<div>overview</div>);

      const component = getMountedComponent({
          componentDetails: {
            name: 'Mock Component Name',
            hash: 'some-crazy-hash',
            matchState: 'exact',
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
          matchState: 'exact',
        },
        pagination: mockPagination,
      });
      expect(el.find(ComponentDetailsFooter)).toExist();
    });
  });

  describe('when there is an error loading the report', () => {
    it('renders a NxLoadError component', () => {
      const el = getShallowComponent({ loadError: 'Mock message' }).find(NxLoadError);
      expect(el).toExist();
      expect(el).toHaveProp('error', 'Mock message');
    });

    it('calls loadComponentDetails when the user clicks the retry button', () => {
      const component = getMountedComponent({ loadError: 'Mock message' });
      component.find(NxLoadError).props().retryHandler();
      expect(loadComponentDetailsSpy).toHaveBeenCalled();
      component.unmount();
    });

    describe('when app fails to get componentDetails and there is no loadError', () => {
      it('renders a componentDetails error', () => {
        const el = getShallowComponent(minimalProps).find(NxLoadError);
        expect(el).toExist();
        expect(el).toHaveProp('error', 'Error getting component details.');
      });
    });
  });

  describe('when there are pending loads', () => {
    it('renders a NxLoadingSpinner component', () => {
      const el = getShallowComponent({ loading: true }).find(NxLoadingSpinner);
      expect(el).toExist();
    });
  });
});
