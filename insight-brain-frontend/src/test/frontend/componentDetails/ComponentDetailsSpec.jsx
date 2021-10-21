/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as enzymeUtils from '../enzymeUtils';
import { NxLoadError, NxLoadingSpinner, NxTabs, NxTab, NxButton } from '@sonatype/react-shared-components';

import ComponentDetails from 'MainRoot/componentDetails/ComponentDetails';
import { ComponentDetailsFooter } from 'MainRoot/componentDetails/ComponentDetailsFooter';
import * as routerContext from 'MainRoot/react/RouterStateContext';
import * as fullAuditLog from 'MainRoot/componentDetails/auditLog/AuditLogContainer';
import * as violationsTab from 'MainRoot/componentDetails/ViolationsTableTile/ViolationsTableTileContainer';
import * as overviewTab from 'MainRoot/componentDetails/overview/OverviewContainer';
import * as claimTab from 'MainRoot/componentDetails/claim/ClaimContainer';
import * as vulnerailitiesTile from 'MainRoot/componentDetails/VulnerabilitiesTableTile/VulnerabilitiesTableTileContainer';
import * as licenseDetectionsTile from 'MainRoot/componentDetails/ComponentDetailsLegalTab/LicenseDetectionsTile/LicenseDetectionsTileContainer';
import * as editLicensesPopoverContainer from 'MainRoot/componentDetails/ComponentDetailsLegalTab/EditLicensesPopover/EditLicensesPopoverContainer';
import * as ManageComponentLabels from 'MainRoot/componentDetails/ManageComponentLabels/ManageComponentLabelsContainer';
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';

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
      expect(addButton).toHaveProp('title', 'Add Proprietary Component Matchers');
      expect(addButton.text()).toEqual('Add Proprietary Component Matchers');
    });

    it('calls onTabChange when Claim button was clicked', () => {
      const component = getShallowComponent({
        componentDetails: {
          name: 'Mock Component Name',
          hash: 'some-crazy-hash',
          matchState: 'unknown',
        },
      });
      const alertEl = component.find('.iq-component-details-unknown-component-alert');
      const claimButton = alertEl.find(NxButton).at(0);
      claimButton.simulate('click');

      expect(onTabChangeSpy).toHaveBeenCalledWith('claim');
    });
  });

  describe('renders tabs', function () {
    it('does not render tabs if there is no selected component', function () {
      const component = getShallowComponent(),
        tabBar = component.find(NxTabs);

      expect(tabBar).not.toExist();
    });

    it('renders 6 tabs with the appropriate names when there is componentDetails prop', function () {
      const component = getShallowComponent({
          componentDetails: {
            name: 'Mock Component Name',
            hash: 'some-crazy-hash',
            matchState: 'exact',
            identificationSource: 'Sonatype',
          },
        }),
        tabBar = component.find(NxTabs);

      expect(tabBar).toExist();

      const tabs = tabBar.find(NxTab);

      expect(tabs.at(0)).toHaveProp('children', 'Overview');
      expect(tabs.at(1)).toHaveProp('children', 'Policy Violations');
      expect(tabs.at(2)).toHaveProp('children', 'Security');
      expect(tabs.at(3)).toHaveProp('children', 'Legal');
      expect(tabs.at(4)).toHaveProp('children', 'Labels');
      expect(tabs.at(5)).toHaveProp('children', 'Audit Log');
    });

    it('renders 7 tabs with the appropriate names when there is componentDetails prop and component was claimed', function () {
      const component = getShallowComponent({
          componentDetails: {
            name: 'Mock Component Name',
            hash: 'some-crazy-hash',
            matchState: 'exact',
            identificationSource: 'Manual',
          },
        }),
        tabBar = component.find(NxTabs);

      expect(tabBar).toExist();

      const tabs = tabBar.find(NxTab);

      expect(tabs.at(0)).toHaveProp('children', 'Overview');
      expect(tabs.at(1)).toHaveProp('children', 'Policy Violations');
      expect(tabs.at(2)).toHaveProp('children', 'Security');
      expect(tabs.at(3)).toHaveProp('children', 'Legal');
      expect(tabs.at(4)).toHaveProp('children', 'Labels');
      expect(tabs.at(5)).toHaveProp('children', 'Claim');
      expect(tabs.at(6)).toHaveProp('children', 'Audit Log');
    });

    it('renders 3 tabs with the appropriate names when there is an unknown component', function () {
      const component = getShallowComponent({
          componentDetails: {
            name: 'Mock Component Name',
            hash: 'some-crazy-hash',
            matchState: 'unknown',
          },
        }),
        tabBar = component.find(NxTabs);

      expect(tabBar).toExist();

      const tabs = tabBar.find(NxTab);

      expect(tabs.at(0)).toHaveProp('children', 'Overview');
      expect(tabs.at(1)).toHaveProp('children', 'Policy Violations');
      expect(tabs.at(2)).toHaveProp('children', 'Claim');
    });

    it('calls onTabChange action with the appropriate activeTabId when clicking on a tab for component', function () {
      // Mock containers so that `getMountedComponent` doesn't complain.
      spyOn(fullAuditLog, 'default').and.returnValue(<div>auditLog</div>);
      spyOn(violationsTab, 'ViolationsTableTileContainer').and.returnValue(<div>violations</div>);
      spyOn(overviewTab, 'OverviewContainer').and.returnValue(<div>overview</div>);
      spyOn(vulnerailitiesTile, 'VulnerabilitiesTableTileContainer').and.returnValue(<div>vulnerabilities</div>);
      spyOn(licenseDetectionsTile, 'LicenseDetectionsTileContainer').and.returnValue(<div>license detections</div>);
      spyOn(editLicensesPopoverContainer, 'default').and.returnValue(<div>edit licenses popover</div>);
      spyOn(ManageComponentLabels, 'default').and.returnValue(<div>Manage Labels</div>);

      let component = getMountedComponent({
          componentDetails: {
            name: 'Mock Component Name',
            hash: 'some-crazy-hash',
            matchState: 'exact',
          },
        }),
        tabBar = component.find(NxTabs),
        tabs = tabBar.find(NxTab);

      tabs.at(1).simulate('click');
      expect(onTabChangeSpy).toHaveBeenCalledWith('violations');

      tabs.at(2).simulate('click');
      expect(onTabChangeSpy).toHaveBeenCalledWith('security');

      tabs.at(3).simulate('click');
      expect(onTabChangeSpy).toHaveBeenCalledWith('legal');

      tabs.at(4).simulate('click');
      expect(onTabChangeSpy).toHaveBeenCalledWith('labels');

      tabs.at(5).simulate('click');
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
        (tabBar = component.find(NxTabs)),
        (tabs = tabBar.find(NxTab));

      tabs.at(0).simulate('click');
      expect(onTabChangeInInfoSpy).toHaveBeenCalledWith('overview');
    });

    it('calls onTabChange action with the appropriate activeTabId when clicking on a tab for unknown component', function () {
      // Mock containers so that `getMountedComponent` doesn't complain.
      spyOn(fullAuditLog, 'default').and.returnValue(<div>auditLog</div>);
      spyOn(violationsTab, 'ViolationsTableTileContainer').and.returnValue(<div>violations</div>);
      spyOn(overviewTab, 'OverviewContainer').and.returnValue(<div>overview</div>);
      spyOn(claimTab, 'ClaimContainer').and.returnValue(<div>claim</div>);

      let component = getMountedComponent({
          componentDetails: {
            name: 'Mock Component Name',
            hash: 'some-crazy-hash',
            matchState: 'unknown',
          },
        }),
        tabBar = component.find(NxTabs),
        tabs = tabBar.find(NxTab);

      tabs.at(1).simulate('click');
      expect(onTabChangeSpy).toHaveBeenCalledWith('violations');

      tabs.at(2).simulate('click');
      expect(onTabChangeSpy).toHaveBeenCalledWith('claim');

      /** Starting on another tab to be able to check the listener on the default 0 tab */
      const onTabChangeInInfoSpy = jasmine.createSpy('onTabChange');
      (component = getMountedComponent({
        componentDetails: {
          name: 'Mock Component Name',
          hash: 'some-crazy-hash',
          matchState: 'unknown',
        },
        activeTabId: 'violations',
        onTabChange: onTabChangeInInfoSpy,
      })),
        (tabBar = component.find(NxTabs)),
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
        tabBar = component.find(NxTabs),
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
