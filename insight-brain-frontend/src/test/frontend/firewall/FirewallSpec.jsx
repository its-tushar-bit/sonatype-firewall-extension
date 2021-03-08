/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import React from 'react';
import LoadWrapper from '../../../main/frontend/react/LoadWrapper';
import FirewallAutoUnquarantineStatus from '../../../main/frontend/firewall/FirewallAutoUnquarantineStatus';
import FirewallStatus from '../../../main/frontend/firewall/FirewallStatus';
import FirewallAutoReleaseQuarantine from '../../../main/frontend/firewall/FirewallAutoReleaseQuarantine';
import FirewallQuarantine from '../../../main/frontend/firewall/FirewallQuarantine';
import FirewallQuarantineTable from '../../../main/frontend/firewall/FirewallQuarantineTable';

describe('Firewall', function() {
  let minimalProps,
      Firewall,
      loadDataSpy,
      openConfigurationModalSpy,
      stateMock,
      getShallowComponent,
      FirewallConfigurationModalMock;

  beforeEach(function() {
    FirewallConfigurationModalMock = jasmine.createSpy('FirewallConfigurationModalMock')
        .and.returnValue(<div>FirewallConfigurationModal</div>);

    Firewall = require(
        'inject-loader!../../../main/frontend/firewall/Firewall')({
      './config/FirewallConfigurationModalContainer': FirewallConfigurationModalMock
    }).default;

    loadDataSpy = jasmine.createSpy('loadData');
    openConfigurationModalSpy = jasmine.createSpy('openConfigurationModal');
    stateMock = jasmine.createSpy('state');

    minimalProps = {
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
      quarantinedComponentCount: 8,
      $state: stateMock,
      loadData: loadDataSpy,
      openConfigurationModal: openConfigurationModalSpy
    };

    getShallowComponent = enzymeUtils.getShallowComponent(Firewall, minimalProps);
  });

  it('renders a component with the "nx-page-main" class', function() {
    expect(getShallowComponent().find('.nx-page-main')).toExist();
  });

  it('renders a FirewallStatus card', function() {
    const component = getShallowComponent(),
        card = component.find(FirewallStatus);

    expect(card).toExist();
    expect(card).toHaveProp('totalComponentCount', 7);
    expect(card).toHaveProp('repositoryCount', 6);
  });

  it('renders a card container', function() {
    const component = getShallowComponent();
    expect(component.find('.nx-card-container')).toExist();
  });

  it('renders a FirewallQuarantineStatus card', function() {
    const component = getShallowComponent(),
        card = component.find(FirewallAutoUnquarantineStatus);

    expect(card).toExist();
    expect(card).toHaveProp('quarantineEnabled', true);
    expect(card).toHaveProp('quarantineEnabledRepositoryCount', 5);
    expect(card).toHaveProp('repositoryCount', 6);
  });

  it('renders a FirewallAutoUnquarantineStatus card', function() {
    const component = getShallowComponent(),
        card = component.find(FirewallAutoUnquarantineStatus);

    expect(card).toExist();
    expect(card).toHaveProp('autoUnquarantineEnabled', false);
    expect(card).toHaveProp('enabledPolicyConditionTypesCount', 3);
    expect(card).toHaveProp('totalPolicyConditionTypesCount', 4);
    expect(card).toHaveProp('showCounts', true);
    expect(card).toHaveProp('openConfigurationModal', openConfigurationModalSpy);
  });

  it('renders a FirewallQuarantine card', function() {
    const component = getShallowComponent(),
        card = component.find(FirewallQuarantine);

    expect(card).toExist();
    expect(card).toHaveProp('quarantinedComponentCount', 8);
  });

  it('renders a FirewallAutoReleaseQuarantine card', function() {
    const component = getShallowComponent(),
        card = component.find(FirewallAutoReleaseQuarantine);

    expect(card).toExist();
    expect(card).toHaveProp('autoReleaseQuarantineCountMTD', 1);
  });

  it('renders a FirewallQuarantineTable', function() {
    const component = getShallowComponent(),
        card = component.find(FirewallQuarantineTable);

    expect(card).toExist();
  });

  it('renders the FirewallConfigurationModal component if isShowConfigurationModal is true', function() {
    const component = getShallowComponent({isShowConfigurationModal: true});
    expect(component.find(FirewallConfigurationModalMock)).toExist();
  });

  it('does not render the FirewallConfigurationModal component if isShowConfigurationModal is false ', function() {
    const component = getShallowComponent({isShowConfigurationModal: false});
    expect(component.find(FirewallConfigurationModalMock)).not.toExist();
  });

  it('renders a loading LoadWrapper when loadedStatus is false', function() {
    const component = getShallowComponent({loadedStatus: false});
    const loadWrapper = component.find(LoadWrapper);
    expect(loadWrapper).toHaveProp('loading', true);
  });

  it('renders a loading LoadWrapper when loadedReleaseQuarantineSummary is false', function() {
    const component = getShallowComponent({loadedReleaseQuarantineSummary: false});
    const loadWrapper = component.find(LoadWrapper);
    expect(loadWrapper).toHaveProp('loading', true);
  });

  it('renders a loading LoadWrapper when loadedConfiguration is false', function() {
    const component = getShallowComponent({loadedConfiguration: false});
    const loadWrapper = component.find(LoadWrapper);
    expect(loadWrapper).toHaveProp('loading', true);
  });

  it('renders a loading LoadWrapper when loadedQuarantineSummary is false', function() {
    const component = getShallowComponent({loadedQuarantineSummary: false});
    const loadWrapper = component.find(LoadWrapper);
    expect(loadWrapper).toHaveProp('loading', true);
  });

  it('passes any loadError to the LoadWrapper', function() {
    const component = getShallowComponent({loadError: 'error'});
    const loadWrapper = component.find(LoadWrapper);
    expect(loadWrapper).toHaveProp('error', 'error');
  });

  it('shows appropriate error when not isEnabled', function() {
    const component = getShallowComponent({isEnabled: false, loadedStatus: true});
    const loadWrapper = component.find(LoadWrapper);
    expect(loadWrapper).toHaveProp('error', 'The Firewall feature is disabled');
  });

  it('calls loadData when the LoadWrapper retryHandler is invoked', function() {
    const loadWrapper = getShallowComponent().find(LoadWrapper),
        retryHandler = loadWrapper.prop('retryHandler');

    expect(loadDataSpy).not.toHaveBeenCalled();

    retryHandler();

    expect(loadDataSpy).toHaveBeenCalled();
  });
});
