/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import React from 'react';
import LoadWrapper from '../../../main/frontend/react/LoadWrapper';
import FirewallStatus from '../../../main/frontend/firewall/FirewallStatus';
import FirewallWelcomeModal from '../../../main/frontend/firewall/FirewallWelcomeModal';
import FirewallMetrics from 'MainRoot/firewall/FirewallMetrics';
import FirewallTabs from 'MainRoot/firewall/FirewallTabs';

describe('FirewallPage', function () {
  let minimalProps,
    Firewall,
    loadFirewallDataSpy,
    setQuarantineGridPolicyFilterSpy,
    setQuarantineGridPolicyFilterWithProprietaryNameConflictSpy,
    setQuarantineGridPolicyFilterWithSecurityVulnerabilityCategoryMaliciousCodeSpy,
    stateGoSpy,
    openConfigurationModalSpy,
    stateMock,
    getShallowComponent,
    getMountedComponent,
    FirewallConfigurationModalMock;

  beforeEach(function () {
    FirewallConfigurationModalMock = jasmine
      .createSpy('FirewallConfigurationModalMock')
      .and.returnValue(<div>FirewallConfigurationModal</div>);

    Firewall = require('inject-loader!../../../main/frontend/firewall/FirewallPage')({
      './config/FirewallConfigurationModalContainer': FirewallConfigurationModalMock,
    }).default;

    loadFirewallDataSpy = jasmine.createSpy('loadFirewallData');
    setQuarantineGridPolicyFilterWithProprietaryNameConflictSpy = jasmine.createSpy(
      'setQuarantineGridPolicyFilterWithProprietaryNameConflict'
    );
    setQuarantineGridPolicyFilterWithSecurityVulnerabilityCategoryMaliciousCodeSpy = jasmine.createSpy(
      'setQuarantineGridPolicyFilterWithSecurityVulnerabilityCategoryMaliciousCode'
    );
    setQuarantineGridPolicyFilterSpy = jasmine.createSpy('setQuarantineGridPolicyFilter');

    stateGoSpy = jasmine.createSpy('stateGo');
    openConfigurationModalSpy = jasmine.createSpy('openConfigurationModal');
    stateMock = jasmine.createSpy('state');

    minimalProps = {
      showWelcomeModal: false,
      initializeWelcomeModal: () => {},
      closeWelcomeModal: () => {},
      loadedStatus: true,
      isShowConfigurationModal: true,
      loadError: null,
      isEnabled: true,
      loadedConfiguration: true,
      loadedReleaseQuarantineSummary: true,
      autoReleaseQuarantineCountMTD: 1,
      enabledPolicyConditionTypesCount: 3,
      totalPolicyConditionTypesCount: 4,
      autoUnquarantineEnabled: false,
      loadedQuarantineSummary: true,
      quarantineEnabled: true,
      quarantineEnabledRepositoryCount: 5,
      repositoryCount: 6,
      totalComponentCount: 7,
      $state: stateMock,
      loadFirewallData: loadFirewallDataSpy,
      openConfigurationModal: openConfigurationModalSpy,
      stateGo: stateGoSpy,
      quarantinePageCount: 10,
      setQuarantineGridPolicyFilterWithProprietaryNameConflict: setQuarantineGridPolicyFilterWithProprietaryNameConflictSpy,
      setQuarantineGridPolicyFilterWithSecurityVulnerabilityCategoryMaliciousCode: setQuarantineGridPolicyFilterWithSecurityVulnerabilityCategoryMaliciousCodeSpy,
      quarantinedComponentCount: 8,
      currentPage: 1,
      filterComponentName: 'test',
      filterRepositoryPublicId: 'testPublicId',
      policies: [],
      filterPolicies: [],
      quarantineList: [],
      loadedQuarantineList: true,
      componentsAutoReleased: 0,
      componentsQuarantined: 0,
      namespaceAttacksBlocked: 0,
      safeVersionsSelected: 0,
      supplyChainAttacksBlocked: 0,
      waivedComponents: 0,
      setQuarantineGridComponentNameFilter: () => {},
      setQuarantineGridRepositoryPublicIdFilter: () => {},
      setQuarantineGridPolicyFilter: setQuarantineGridPolicyFilterSpy,
      setQuarantineGridSorting: () => {},
      setQuarantineGridPage: () => {},
      loadQuarantineList: () => {},
    };

    getShallowComponent = enzymeUtils.getShallowComponent(Firewall, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(Firewall, minimalProps);
  });

  it('renders firewall metrics header with correct title and description', function () {
    let component = getShallowComponent(),
      header = component.find('.iq-firewall-metrics-label'),
      description = component.find('.iq-firewall-metrics-header').find('span');

    expect(header).toHaveText(`Component Data Insights`);
    expect(description).toHaveText(
      `These totals include quarantined, waived, and auto-released components that differ from those actively in quarantine.`
    );
  });

  it('renders a component with the "nx-page-main" class', function () {
    expect(getShallowComponent().find('.nx-page-main')).toExist();
  });

  it('renders welcome modal if showWelcomeModal is true', function () {
    expect(getShallowComponent({ showWelcomeModal: true }).find(FirewallWelcomeModal)).toExist();
  });

  it('renders a FirewallStatus card', function () {
    const component = getShallowComponent(),
      card = component.find(FirewallStatus);

    expect(card).toExist();
    expect(card).toHaveProp('quarantineEnabledRepositoryCount', 5);
    expect(card).toHaveProp('repositoryCount', 6);
    expect(card).toHaveProp('totalComponentCount', 7);
  });

  it('renders a FirewallMetrics', function () {
    const component = getShallowComponent(),
      metrics = component.find(FirewallMetrics);

    expect(metrics).toExist();
  });

  it('scrolls when FirewallMetrics "details" links are clicked', function () {
    jasmine.clock().install();

    const clickTabMock = jasmine.createSpy('clickTab');
    const scrollToPanelMock = jasmine.createSpy('scrollToPanel');
    const millisToScrollToPanel = 100;

    const component = getMountedComponent();
    const metrics = component.find(FirewallMetrics);
    const button1 = metrics.find('#firewall-metrics-content-supply-chain-attacks-blocked').find('button.nx-text-link');
    const button2 = metrics.find('#firewall-metrics-content-namespace-attacks-blocked').find('button.nx-text-link');
    const button3 = metrics.find('#firewall-metrics-content-components-quarantined').find('button.nx-text-link');
    const button4 = metrics.find('#firewall-metrics-content-components-waived').find('button.nx-text-link');

    expect(button1).toExist();
    expect(button2).toExist();
    expect(button3).toExist();
    expect(button4).toExist();

    const firewallTabsFuncRefs = component.find(FirewallTabs).get(0).ref;
    firewallTabsFuncRefs.current.clickTab = clickTabMock;
    firewallTabsFuncRefs.current.scrollToPanel = scrollToPanelMock;

    button1.simulate('click');
    expect(clickTabMock).toHaveBeenCalledTimes(1);
    expect(clickTabMock).toHaveBeenCalledWith('quarantine');
    jasmine.clock().tick(millisToScrollToPanel);
    expect(scrollToPanelMock).toHaveBeenCalledTimes(1);
    expect(scrollToPanelMock).toHaveBeenCalledWith('quarantine');

    button2.simulate('click');
    expect(clickTabMock).toHaveBeenCalledTimes(2);
    expect(clickTabMock).toHaveBeenCalledWith('quarantine');
    jasmine.clock().tick(millisToScrollToPanel);
    expect(scrollToPanelMock).toHaveBeenCalledTimes(2);
    expect(scrollToPanelMock).toHaveBeenCalledWith('quarantine');

    button3.simulate('click');
    expect(clickTabMock).toHaveBeenCalledTimes(3);
    expect(clickTabMock).toHaveBeenCalledWith('quarantine');
    jasmine.clock().tick(millisToScrollToPanel);
    expect(scrollToPanelMock).toHaveBeenCalledTimes(3);
    expect(scrollToPanelMock).toHaveBeenCalledWith('quarantine');

    button4.simulate('click');
    expect(clickTabMock).toHaveBeenCalledTimes(4);
    expect(clickTabMock).toHaveBeenCalledWith('waivers');
    jasmine.clock().tick(millisToScrollToPanel);
    expect(scrollToPanelMock).toHaveBeenCalledTimes(4);
    expect(scrollToPanelMock).toHaveBeenCalledWith('waivers');

    jasmine.clock().uninstall();
  });

  it('calls setQuarantineGridPolicyFilter when filterPolicies is greater than 0', function () {
    const minimalPropsWithFilter = {
      ...minimalProps,
      filterPolicies: ['a'],
    };
    getShallowComponent = enzymeUtils.getShallowComponent(Firewall, minimalPropsWithFilter);
    getMountedComponent = enzymeUtils.getMountedComponent(Firewall, minimalPropsWithFilter);
    const component = getMountedComponent();

    const scrollIntoViewMock = jasmine.createSpy('scrollIntoView');
    const getElementByIdMock = jasmine.createSpy('getElementById').and.returnValue({
      scrollIntoView: scrollIntoViewMock,
    });
    const getElementById = document.getElementById;
    document.getElementById = getElementByIdMock;

    const metrics = component.find(FirewallMetrics);
    const button = metrics.find('#firewall-metrics-content-components-quarantined').find('button.nx-text-link');
    button.simulate('click');

    expect(setQuarantineGridPolicyFilterSpy).toHaveBeenCalled();

    document.getElementById = getElementById;
  });

  it('does not call setQuarantineGridPolicyFilter when filterPolicies is not greater than 0', function () {
    const scrollIntoViewMock = jasmine.createSpy('scrollIntoView');
    const getElementByIdMock = jasmine.createSpy('getElementById').and.returnValue({
      scrollIntoView: scrollIntoViewMock,
    });
    const getElementById = document.getElementById;
    document.getElementById = getElementByIdMock;

    const component = getMountedComponent();
    const metrics = component.find(FirewallMetrics);
    const button = metrics.find('#firewall-metrics-content-components-quarantined').find('button.nx-text-link');
    button.simulate('click');

    expect(setQuarantineGridPolicyFilterSpy).not.toHaveBeenCalled();

    document.getElementById = getElementById;
  });

  it('renders a FirewallQuarantineTable', function () {
    const component = getShallowComponent(),
      card = component.find(FirewallTabs);

    expect(card).toExist();
  });

  it('renders the FirewallConfigurationModal component if isShowConfigurationModal is true', function () {
    const component = getShallowComponent({ isShowConfigurationModal: true });
    expect(component.find(FirewallConfigurationModalMock)).toExist();
  });

  it('does not render the FirewallConfigurationModal component if isShowConfigurationModal is false ', function () {
    const component = getShallowComponent({ isShowConfigurationModal: false });
    expect(component.find(FirewallConfigurationModalMock)).not.toExist();
  });

  it('renders a loading LoadWrapper when loadedReleaseQuarantineSummary is false', function () {
    const component = getShallowComponent({
      loadedReleaseQuarantineSummary: false,
    });
    const loadWrapper = component.find(LoadWrapper);
    expect(loadWrapper).toHaveProp('loading', true);
  });

  it('renders a loading LoadWrapper when loadedConfiguration is false', function () {
    const component = getShallowComponent({ loadedConfiguration: false });
    const loadWrapper = component.find(LoadWrapper);
    expect(loadWrapper).toHaveProp('loading', true);
  });

  it('renders a loading LoadWrapper when loadedQuarantineSummary is false', function () {
    const component = getShallowComponent({ loadedQuarantineSummary: false });
    const loadWrapper = component.find(LoadWrapper);
    expect(loadWrapper).toHaveProp('loading', true);
  });

  it('passes any loadError to the LoadWrapper', function () {
    const component = getShallowComponent({ loadError: 'error' });
    const loadWrapper = component.find(LoadWrapper);
    expect(loadWrapper).toHaveProp('error', 'error');
  });

  it('calls loadFirewallData when the LoadWrapper retryHandler is invoked', function () {
    const loadWrapper = getShallowComponent().find(LoadWrapper),
      retryHandler = loadWrapper.prop('retryHandler');

    expect(loadFirewallDataSpy).not.toHaveBeenCalled();

    retryHandler();

    expect(loadFirewallDataSpy).toHaveBeenCalled();
  });
});
