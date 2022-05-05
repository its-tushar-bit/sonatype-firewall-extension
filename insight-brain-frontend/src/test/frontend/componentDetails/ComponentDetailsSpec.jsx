/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as enzymeUtils from '../enzymeUtils';
import { NxLoadError, NxLoadWrapper } from '@sonatype/react-shared-components';

import { getTabsConfiguration } from 'MainRoot/componentDetails/ComponentDetails';
import ComponentDetails from 'MainRoot/componentDetails/ComponentDetails';
import { ComponentDetailsFooter } from 'MainRoot/componentDetails/ComponentDetailsFooter';
import * as routerContext from 'MainRoot/react/RouterStateContext';
import * as ComponentDetailsTabsFile from 'MainRoot/componentDetails/ComponentDetailsTabs';
import ComponentDetailsTabs from 'MainRoot/componentDetails/ComponentDetailsTabs';
import UnknownComponentAlert from 'MainRoot/componentDetails/UnknownComponentAlert';
import * as ComponentDetailsBackButton from 'MainRoot/componentDetails/ComponentDetailsBackButton';

const assertTabs = (component, activeTabId, isUnknown, isClaimed, isExact) => {
  let tabs = component.find(ComponentDetailsTabs);

  expect(tabs).toHaveProp('activeTabId', activeTabId);
  expect(tabs).toHaveProp('onTabChange', jasmine.any(Function));
  expect(tabs).toHaveProp('tabsConfiguration', getTabsConfiguration(isUnknown, isExact, isClaimed));
};

describe('ComponentDetails', function () {
  let minimalProps,
    getShallowComponent,
    getMountedComponent,
    loadComponentDetailsSpy,
    stateMock,
    stateGetSpy,
    onTabChangeSpy,
    toggleShowMatchersPopoverSpy,
    backButtonMock;

  beforeEach(function () {
    backButtonMock = spyOn(ComponentDetailsBackButton, 'default').and.returnValue(
      <div>Component Details Back Button</div>
    );
    loadComponentDetailsSpy = jasmine.createSpy('loadComponentDetails');
    onTabChangeSpy = jasmine.createSpy('onTabChange');
    toggleShowMatchersPopoverSpy = jasmine.createSpy('toggleShowMatchersPopover');

    stateGetSpy = jasmine.createSpy('$state.get').and.returnValue({ data: { title: 'some title' } });
    stateMock = {
      get: stateGetSpy,
      href: () => {},
    };
    spyOn(routerContext, 'useRouterState').and.returnValue(stateMock);
    spyOn(ComponentDetailsTabsFile, 'default').and.returnValue(<div>Tabs</div>);

    minimalProps = {
      componentDetails: null,
      activeTabId: 'overview',
      loadComponentDetails: loadComponentDetailsSpy,
      onTabChange: onTabChangeSpy,
      pagination: null,
      loadError: null,
      loading: false,
      toggleShowMatchersPopover: toggleShowMatchersPopoverSpy,
      isProprietary: false,
    };

    getShallowComponent = enzymeUtils.getShallowComponent(ComponentDetails, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(ComponentDetails, minimalProps);
  });

  it('renders a component', () => {
    expect(getShallowComponent()).toExist();
  });

  it('renders a ComponentDetailsBackButton', () => {
    const dependencyTreeRouterParams = { publicId: 'testPublicId', scanId: 'testScanId' };

    const el = getShallowComponent({ dependencyTreeRouterParams }),
      backBtn = el.find(backButtonMock);

    expect(backBtn).toHaveProp('publicId', dependencyTreeRouterParams.publicId);
    expect(backBtn).toHaveProp('scanId', dependencyTreeRouterParams.scanId);
  });

  it('renders the tabs at all times', () => {
    let component = getShallowComponent({ loading: false });
    assertTabs(component, minimalProps.activeTabId, false, false, false);

    component = getShallowComponent({ loading: true });
    assertTabs(component, minimalProps.activeTabId, false, false, false);
  });

  it('calls loadComponentDetails only when mounted', () => {
    // mount component loading to avoid having to supply a `componentDetailsProp`.
    const component = getMountedComponent({ loading: true });
    expect(loadComponentDetailsSpy).toHaveBeenCalledTimes(1);

    component.update();

    expect(loadComponentDetailsSpy).toHaveBeenCalledTimes(1);
    component.unmount();
  });

  describe('renders unknown component alert', function () {
    it('does not render if the page is loading', () => {
      const component = getShallowComponent({ loading: true });
      const alertEl = component.find(UnknownComponentAlert);

      expect(alertEl).not.toExist();
    });

    it('does not render if there is no unknown match state', function () {
      const component = getShallowComponent();
      const alertEl = component.find(UnknownComponentAlert);

      expect(alertEl).not.toExist();
    });

    it('renders unknown component alert when there is an unknown match state', function () {
      const component = getShallowComponent({
        componentDetails: {
          name: 'Mock Component Name',
          hash: 'some-crazy-hash',
          matchState: 'unknown',
        },
        loading: false,
      });
      const loadWrapper = component.find(NxLoadWrapper);
      const alertEl = loadWrapper.dive().find(UnknownComponentAlert);
      const proprietaryAlert = loadWrapper.dive().find('#proprietary-component-matched-alert');

      expect(alertEl).toHaveProp('onClaimClick');
      expect(proprietaryAlert).not.toExist();
    });

    it('renders proprietary-component-matched-alert when there is an unknown match state but claimed as proprietary', function () {
      const component = getShallowComponent({
        componentDetails: {
          name: 'Mock Component Name',
          hash: 'some-crazy-hash',
          matchState: 'unknown',
        },
        loading: false,
        isProprietary: true,
      });
      const loadWrapper = component.find(NxLoadWrapper);
      const alertEl = loadWrapper.dive().find(UnknownComponentAlert);
      const proprietaryAlert = loadWrapper.dive().find('#proprietary-component-matched-alert');

      expect(alertEl).not.toExist();
      expect(proprietaryAlert).toExist();
    });
  });

  describe('goToClaim', () => {
    it('calls onTabChange with the proper `claim` id', () => {
      const componentDetails = {
        name: 'Mock Component Name',
        hash: 'some-crazy-hash',
        matchState: 'unknown',
      };
      const component = getShallowComponent({ componentDetails });
      const loadWrapper = component.find(NxLoadWrapper);
      const alertEl = loadWrapper.dive().find(UnknownComponentAlert);

      alertEl.invoke('onClaimClick')();

      expect(minimalProps.onTabChange).toHaveBeenCalledWith('claim');
    });
  });

  describe('handleTabChange', () => {
    const getOnTabChangeProp = (element) => {
      const tabs = element.find(ComponentDetailsTabs);
      return tabs.invoke('onTabChange');
    };

    it('calls onTabChange with the appropriate tab id', () => {
      let component, changeFn;

      const componentDetails = {
        name: 'Mock Component Name',
        hash: 'some-crazy-hash',
        matchState: 'exact',
      };

      component = getShallowComponent({ componentDetails, activeTabId: 'overview' });
      changeFn = getOnTabChangeProp(component);

      changeFn('violations');
      expect(minimalProps.onTabChange).toHaveBeenCalledWith('violations');

      changeFn('security');
      expect(minimalProps.onTabChange).toHaveBeenCalledWith('security');

      changeFn('legal');
      expect(minimalProps.onTabChange).toHaveBeenCalledWith('legal');

      changeFn('labels');
      expect(minimalProps.onTabChange).toHaveBeenCalledWith('labels');

      changeFn('audit');
      expect(minimalProps.onTabChange).toHaveBeenCalledWith('audit');

      // Starting on another tab to be able to check the listener on the default 0 tab
      component = getMountedComponent({ componentDetails, activeTabId: 'violations' });
      changeFn = getOnTabChangeProp(component);

      changeFn('overview');
      expect(minimalProps.onTabChange).toHaveBeenCalledWith('overview');
    });

    it('does not call onTabChange when clicking on the same tab twice', function () {
      let component, changeFn;

      const componentDetails = {
        name: 'Mock Component Name',
        hash: 'some-crazy-hash',
        matchState: 'exact',
      };

      component = getShallowComponent({ componentDetails, activeTabId: 'overview' });
      changeFn = getOnTabChangeProp(component);

      changeFn('overview');
      expect(minimalProps.onTabChange).not.toHaveBeenCalled();
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
      const el = getShallowComponent({ loadError: 'Mock message' });
      const loadWrapper = el.find(NxLoadWrapper);
      const error = loadWrapper.dive().find(NxLoadError);

      expect(error).toExist();
      expect(error).toHaveProp('error', 'Mock message');
    });

    it('calls loadComponentDetails when the user clicks the retry button', () => {
      const el = getShallowComponent({ loadError: 'Mock message' });
      const loadWrapper = el.find(NxLoadWrapper);
      const error = loadWrapper.dive().find(NxLoadError);

      error.props().retryHandler();
      expect(loadComponentDetailsSpy).toHaveBeenCalled();
    });

    it('renders the tabs', () => {
      const el = getShallowComponent({ loadError: 'Mock message' });
      assertTabs(el, minimalProps.activeTabId, false, false, false);
    });
  });

  describe('when there page is loading ', () => {
    it('renders a NxLoadWrapper component', () => {
      const el = getShallowComponent({ loading: true }).find(NxLoadWrapper);
      expect(el).toExist();
    });

    it('renders the tabs', () => {
      const el = getShallowComponent({ loadError: 'Mock message' });
      assertTabs(el, minimalProps.activeTabId, false, false, false);
    });
  });

  it('hides a ComponentDetailsFooter component when landing from the dependency tree', () => {
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
      dependencyTreeRouterParams: { publicId: 'publicId', scanId: 'scanId' },
    });
    expect(el.find(ComponentDetailsFooter)).not.toExist();
  });
});
