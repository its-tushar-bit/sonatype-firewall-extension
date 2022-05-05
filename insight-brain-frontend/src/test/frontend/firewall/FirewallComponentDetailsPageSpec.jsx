/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as enzymeUtils from '../enzymeUtils';
import { NxLoadError, NxLoadWrapper } from '@sonatype/react-shared-components';

import ComponentDetailsTabs from 'MainRoot/componentDetails/ComponentDetailsTabs';
import * as ComponentDetailsTabsFile from 'MainRoot/componentDetails/ComponentDetailsTabs';
import { tabsConfiguration } from 'MainRoot/firewall/firewallComponentDetailsPage/FirewallComponentDetailsPage';
import FirewallComponentDetailsPage from 'MainRoot/firewall/firewallComponentDetailsPage/FirewallComponentDetailsPage';

const assertTabs = (component, activeTabId) => {
  let tabs;

  tabs = component.find(ComponentDetailsTabs);

  expect(tabs).toHaveProp('activeTabId', activeTabId);
  expect(tabs).toHaveProp('onTabChange', jasmine.any(Function));
  expect(tabs).toHaveProp('tabsConfiguration', tabsConfiguration);
};

describe('FirewallComponentDetailsPage', function () {
  let minimalProps, getShallowComponent, getMountedComponent, loadComponentDetailsSpy, onTabChangeSpy;

  const setCustomCDPResponseStateParamsOnMinimalProps = (key, value) => ({
    ...minimalProps,
    CDPResponseState: { ...minimalProps.CDPResponseState, [key]: value },
  });

  beforeEach(function () {
    loadComponentDetailsSpy = jasmine.createSpy('loadComponentDetails');
    onTabChangeSpy = jasmine.createSpy('onTabChange');

    spyOn(ComponentDetailsTabsFile, 'default').and.returnValue(<div>Tabs</div>);

    minimalProps = {
      loadComponentDetails: loadComponentDetailsSpy,
      onCDPTabChange: onTabChangeSpy,
      routeParams: {
        repositoryId: 'repositoryId',
        componentHash: 'componentHash',
        matchState: 'exact',
        proprietary: 'false',
        tabId: 'overview',
      },
      CDPResponseState: {
        componentDetails: {},
        isLoadingComponentDetails: false,
        componentDetailsError: null,
      },
    };

    getShallowComponent = enzymeUtils.getShallowComponent(FirewallComponentDetailsPage, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(FirewallComponentDetailsPage, minimalProps);
  });

  it('renders a component', () => {
    expect(getShallowComponent()).toExist();
  });

  it('renders the tabs at all times', () => {
    let component = getMountedComponent(
      setCustomCDPResponseStateParamsOnMinimalProps('isLoadingComponentDetails', false)
    );
    assertTabs(component, minimalProps.routeParams.tabId);

    component = getMountedComponent(setCustomCDPResponseStateParamsOnMinimalProps('isLoadingComponentDetails', true));
    assertTabs(component, minimalProps.routeParams.tabId);
  });

  it('calls loadComponentDetails only when mounted', () => {
    // mount component loading to avoid having to supply a `componentDetailsProp`.
    let component = getMountedComponent({
      ...minimalProps,
      CDPResponseState: { ...minimalProps.CDPResponseState, isLoadingComponentDetails: false },
    });
    expect(loadComponentDetailsSpy).toHaveBeenCalledTimes(1);

    component.update();

    expect(loadComponentDetailsSpy).toHaveBeenCalledTimes(1);
    component.unmount();
  });

  describe('handleTabChange', () => {
    const getOnTabChangeProp = (element) => {
      const tabs = element.find(ComponentDetailsTabs);
      return tabs.invoke('onTabChange');
    };

    it('calls onTabChange with the appropriate tab id', () => {
      let component, changeFn;

      component = getShallowComponent({
        ...minimalProps,
        routeParams: { ...minimalProps.routeParams, tabId: 'overview' },
      });
      changeFn = getOnTabChangeProp(component);

      changeFn('violations');
      expect(minimalProps.onCDPTabChange).toHaveBeenCalledWith('violations');

      changeFn('security');
      expect(minimalProps.onCDPTabChange).toHaveBeenCalledWith('security');

      changeFn('legal');
      expect(minimalProps.onCDPTabChange).toHaveBeenCalledWith('legal');

      changeFn('labels');
      expect(minimalProps.onCDPTabChange).toHaveBeenCalledWith('labels');

      // Starting on another tab to be able to check the listener on the default 0 tab
      component = getMountedComponent({
        ...minimalProps,
        routeParams: { ...minimalProps.routeParams, tabId: 'violations' },
      });
      changeFn = getOnTabChangeProp(component);

      changeFn('overview');
      expect(minimalProps.onCDPTabChange).toHaveBeenCalledWith('overview');
    });

    it('does not call onTabChange when clicking on the same tab twice', function () {
      let component, changeFn;

      component = getShallowComponent({
        ...minimalProps,
        routeParams: { ...minimalProps.routeParams, tabId: 'overview' },
      });
      changeFn = getOnTabChangeProp(component);

      changeFn('overview');
      expect(minimalProps.onCDPTabChange).not.toHaveBeenCalled();
    });
  });

  describe('when there is an error loading the report', () => {
    it('renders a NxLoadError component', () => {
      const el = getShallowComponent(
        setCustomCDPResponseStateParamsOnMinimalProps('componentDetailsError', 'Mock message')
      );
      const loadWrapper = el.find(NxLoadWrapper);
      const error = loadWrapper.dive().find(NxLoadError);

      expect(error).toExist();
      expect(error).toHaveProp('error', 'Mock message');
    });

    it('calls loadComponentDetails when the user clicks the retry button', () => {
      const el = getShallowComponent(
        setCustomCDPResponseStateParamsOnMinimalProps('componentDetailsError', 'Mock message')
      );
      const loadWrapper = el.find(NxLoadWrapper);
      const error = loadWrapper.dive().find(NxLoadError);

      error.props().retryHandler();
      expect(loadComponentDetailsSpy).toHaveBeenCalled();
    });

    it('renders the tabs', () => {
      const el = getShallowComponent(
        setCustomCDPResponseStateParamsOnMinimalProps('componentDetailsError', 'Mock message')
      );
      assertTabs(el, minimalProps.routeParams.tabId);
    });
  });

  describe('when there page is loading ', () => {
    it('renders a NxLoadWrapper component', () => {
      const el = getShallowComponent(
        setCustomCDPResponseStateParamsOnMinimalProps('isLoadingComponentDetails', true)
      ).find(NxLoadWrapper);
      expect(el).toExist();
    });

    it('renders the tabs', () => {
      const el = getShallowComponent(
        setCustomCDPResponseStateParamsOnMinimalProps('componentDetailsError', 'Mock message')
      );
      assertTabs(el, minimalProps.routeParams.tabId);
    });
  });
});
