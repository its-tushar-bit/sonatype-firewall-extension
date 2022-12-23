/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import * as enzymeUtils from 'TestRoot/enzymeUtils';
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
  let minimalProps,
    getShallowComponent,
    getMountedComponent,
    loadComponentDetailsSpy,
    onTabChangeSpy,
    loadComponentPolicyViolationsSpy,
    loadExistingWaiversDataSpy,
    reevaluateComponentSpy,
    firewallLoadApplicableLabelsSpy;

  const setCustomComponentDetailsPageResponseStateParamsOnMinimalProps = (key, value) => ({
    ...minimalProps,
    componentDetailsPageResponseState: { ...minimalProps.componentDetailsPageResponseState, [key]: value },
  });

  beforeEach(function () {
    loadComponentDetailsSpy = jasmine.createSpy('loadComponentDetails');
    onTabChangeSpy = jasmine.createSpy('onTabChange');
    loadComponentPolicyViolationsSpy = jasmine.createSpy('loadComponentPolicyViolations');
    loadExistingWaiversDataSpy = jasmine.createSpy('loadExistingWaiversData');
    reevaluateComponentSpy = jasmine.createSpy('reevaluateComponent');
    firewallLoadApplicableLabelsSpy = jasmine.createSpy('firewallLoadApplicableLabels');

    spyOn(ComponentDetailsTabsFile, 'default').and.returnValue(<div>Tabs</div>);

    minimalProps = {
      loadComponentDetails: loadComponentDetailsSpy,
      onComponentDetailsPageTabChange: onTabChangeSpy,
      routeParams: {
        repositoryId: 'repositoryId',
        componentHash: 'componentHash',
        matchState: 'exact',
        proprietary: 'false',
        tabId: 'overview',
        componentIdentifier: 'componentIdentifier',
        pathname: 'pathname',
      },
      componentDetailsPageResponseState: {
        componentDetails: {},
        isLoadingComponentDetails: false,
        componentDetailsError: null,
      },
      loadComponentPolicyViolations: loadComponentPolicyViolationsSpy,
      loadExistingWaiversData: loadExistingWaiversDataSpy,
      reevaluateComponent: reevaluateComponentSpy,
      firewallLoadApplicableLabels: firewallLoadApplicableLabelsSpy,
      isFirewall: true,
    };

    getShallowComponent = enzymeUtils.getShallowComponent(FirewallComponentDetailsPage, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(FirewallComponentDetailsPage, minimalProps);
  });

  it('renders a component', () => {
    expect(getShallowComponent()).toExist();
  });

  it('renders the tabs at all times', () => {
    let component = getMountedComponent(
      setCustomComponentDetailsPageResponseStateParamsOnMinimalProps('isLoadingComponentDetails', false)
    );
    assertTabs(component, minimalProps.routeParams.tabId);

    component = getMountedComponent(
      setCustomComponentDetailsPageResponseStateParamsOnMinimalProps('isLoadingComponentDetails', true)
    );
    assertTabs(component, minimalProps.routeParams.tabId);
  });

  it('calls loadComponentDetails only when mounted', () => {
    // mount component loading to avoid having to supply a `componentDetailsProp`.
    let component = getMountedComponent({
      ...minimalProps,
      componentDetailsPageResponseState: {
        ...minimalProps.componentDetailsPageResponseState,
        isLoadingComponentDetails: false,
      },
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
      expect(minimalProps.onComponentDetailsPageTabChange).toHaveBeenCalledWith('violations');

      changeFn('security');
      expect(minimalProps.onComponentDetailsPageTabChange).toHaveBeenCalledWith('security');

      changeFn('legal');
      expect(minimalProps.onComponentDetailsPageTabChange).toHaveBeenCalledWith('legal');

      changeFn('labels');
      expect(minimalProps.onComponentDetailsPageTabChange).toHaveBeenCalledWith('labels');

      // Starting on another tab to be able to check the listener on the default 0 tab
      component = getMountedComponent({
        ...minimalProps,
        routeParams: { ...minimalProps.routeParams, tabId: 'violations' },
      });
      changeFn = getOnTabChangeProp(component);

      changeFn('overview');
      expect(minimalProps.onComponentDetailsPageTabChange).toHaveBeenCalledWith('overview');
    });

    it('does not call onTabChange when clicking on the same tab twice', function () {
      let component, changeFn;

      component = getShallowComponent({
        ...minimalProps,
        routeParams: { ...minimalProps.routeParams, tabId: 'overview' },
      });
      changeFn = getOnTabChangeProp(component);

      changeFn('overview');
      expect(minimalProps.onComponentDetailsPageTabChange).not.toHaveBeenCalled();
    });
  });

  describe('when there is an error loading the report', () => {
    it('renders a NxLoadError component', () => {
      const el = getShallowComponent(
        setCustomComponentDetailsPageResponseStateParamsOnMinimalProps('componentDetailsError', 'Mock message')
      );
      const loadWrapper = el.find(NxLoadWrapper);
      const error = loadWrapper.dive().find(NxLoadError);

      expect(error).toExist();
      expect(error).toHaveProp('error', 'Mock message');
    });

    it('calls loadComponentDetails when the user clicks the retry button', () => {
      const el = getShallowComponent(
        setCustomComponentDetailsPageResponseStateParamsOnMinimalProps('componentDetailsError', 'Mock message')
      );
      const loadWrapper = el.find(NxLoadWrapper);
      const error = loadWrapper.dive().find(NxLoadError);

      error.props().retryHandler();
      expect(loadComponentDetailsSpy).toHaveBeenCalled();
    });

    it('renders the tabs', () => {
      const el = getShallowComponent(
        setCustomComponentDetailsPageResponseStateParamsOnMinimalProps('componentDetailsError', 'Mock message')
      );
      assertTabs(el, minimalProps.routeParams.tabId);
    });
  });

  describe('when the page is loading ', () => {
    it('renders a NxLoadWrapper component', () => {
      const el = getShallowComponent(
        setCustomComponentDetailsPageResponseStateParamsOnMinimalProps('isLoadingComponentDetails', true)
      ).find(NxLoadWrapper);
      expect(el).toExist();
    });

    it('renders re-evaluate button', () => {
      let component = getMountedComponent();
      expect(component.find('#firewall-component-details-page__reevaluate-button')).toExist();
    });

    it('should re-evaluate component when click on button', () => {
      let component = getMountedComponent({
        ...minimalProps,
        CDPResponseState: { ...minimalProps.CDPResponseState, isLoadingComponentDetails: false },
      });
      component.find('#firewall-component-details-page__reevaluate-button').first().simulate('click');

      component.update();
      expect(reevaluateComponentSpy).toHaveBeenCalledTimes(1);
      component.unmount();
    });
  });

  describe('loadComponentPolicyViolations', () => {
    it('calls loadComponentPolicyViolations only when mounted', () => {
      let component = getMountedComponent({
        ...minimalProps,
        CDPResponseState: { ...minimalProps.CDPResponseState, isLoadingComponentDetails: false },
      });
      expect(loadComponentPolicyViolationsSpy).toHaveBeenCalledTimes(1);

      component.update();

      expect(loadComponentPolicyViolationsSpy).toHaveBeenCalledTimes(1);
      component.unmount();
    });
  });

  describe('loadComponentPolicyViolations', () => {
    it('calls loadComponentPolicyViolations only when mounted', () => {
      // mount component loading to avoid having to supply a `componentDetailsProp`.
      let component = getMountedComponent({
        ...minimalProps,
        CDPResponseState: { ...minimalProps.CDPResponseState, isLoadingComponentDetails: false },
      });
      expect(loadComponentPolicyViolationsSpy).toHaveBeenCalledTimes(1);

      component.update();

      expect(loadComponentPolicyViolationsSpy).toHaveBeenCalledTimes(1);
      component.unmount();
    });
  });

  describe('loadExistingWaiversData', () => {
    it('calls loadExistingWaiversData only when mounted', () => {
      let component = getMountedComponent();
      expect(loadComponentPolicyViolationsSpy).toHaveBeenCalledTimes(1);

      component.update();

      expect(loadComponentPolicyViolationsSpy).toHaveBeenCalledTimes(1);
      component.unmount();
    });
  });
});
