/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import React from 'react';
import LoadWrapper from '../../../main/frontend/react/LoadWrapper';
import FirewallStatus from '../../../main/frontend/firewall/FirewallStatus';
import FirewallQuarantineTable from '../../../main/frontend/firewall/FirewallQuarantineTable';
import FirewallWelcomeModal from '../../../main/frontend/firewall/FirewallWelcomeModal';
import FirewallMetrics from 'MainRoot/firewall/FirewallMetrics';

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
      setQuarantineGridPolicyFilter: setQuarantineGridPolicyFilterSpy,
      setQuarantineGridSorting: () => {},
      setQuarantineGridPage: () => {},
      loadQuarantineList: () => {},
    };

    getShallowComponent = enzymeUtils.getShallowComponent(Firewall, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(Firewall, minimalProps);
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
    const scrollIntoViewMock = jasmine.createSpy('scrollIntoView');
    const getElementByIdMock = jasmine.createSpy('getElementById').and.returnValue({
      scrollIntoView: scrollIntoViewMock,
    });
    const getElementById = document.getElementById;
    document.getElementById = getElementByIdMock;

    const component = getMountedComponent();
    const metrics = component.find(FirewallMetrics);
    const button1 = metrics.find('#firewall-metrics-content-supply-chain-attacks-blocked').find('button.nx-text-link');
    const button2 = metrics.find('#firewall-metrics-content-namespace-attacks-blocked').find('button.nx-text-link');
    const button3 = metrics.find('#firewall-metrics-content-components-quarantined').find('button.nx-text-link');

    expect(button1).toExist();
    expect(button2).toExist();

    button1.simulate('click');
    expect(getElementByIdMock).toHaveBeenCalledWith('firewall-quarantine-table');
    expect(scrollIntoViewMock).toHaveBeenCalledWith({ behavior: 'smooth' });
    expect(scrollIntoViewMock.calls.count()).toBe(1);

    button2.simulate('click');
    expect(scrollIntoViewMock.calls.count()).toBe(2);

    button3.simulate('click');
    expect(scrollIntoViewMock.calls.count()).toBe(3);

    document.getElementById = getElementById;
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
      card = component.find(FirewallQuarantineTable);

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
