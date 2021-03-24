/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import React from 'react';
import LoadWrapper from '../../../../main/frontend/react/LoadWrapper';
import BackButton from '../../../../main/frontend/react/BackButton';
import FirewallAutoReleaseQuarantineMtd
  from '../../../../main/frontend/firewall/autounquarantine/FirewallAutoReleaseQuarantineMtd';
import FirewallAutoReleaseQuarantineYtd
  from '../../../../main/frontend/firewall/autounquarantine/FirewallAutoReleaseQuarantineYtd';
import FirewallAutoUnquarantineStatus from '../../../../main/frontend/firewall/FirewallAutoUnquarantineStatus';
import FirewallPolicyConditionTypes
  from '../../../../main/frontend/firewall/autounquarantine/FirewallPolicyConditionTypes';
import FirewallUnquarantineTable from '../../../../main/frontend/firewall/autounquarantine/FirewallUnquarantineTable';

describe('FirewallAutoUnquarantinePage', function() {
  let minimalProps,
      FirewallAutoUnquarantinePage,
      loadDataSpy,
      openConfigurationModalSpy,
      stateMock,
      getShallowComponent,
      FirewallConfigurationModalMock;

  beforeEach(function() {
    FirewallConfigurationModalMock = jasmine.createSpy('FirewallConfigurationModalMock')
        .and.returnValue(<div>FirewallConfigurationModal</div>);

    FirewallAutoUnquarantinePage = require(
        'inject-loader!../../../../main/frontend/firewall/autounquarantine/FirewallAutoUnquarantinePage')({
      '../config/FirewallConfigurationModalContainer': FirewallConfigurationModalMock
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
      autoReleaseQuarantineCountYTD: 2,
      enabledPolicyConditionTypesCount: 3,
      totalPolicyConditionTypesCount: 4,
      autoUnquarantineEnabled: false,
      $state: stateMock,
      loadData: loadDataSpy,
      openConfigurationModal: openConfigurationModalSpy
    };

    getShallowComponent = enzymeUtils.getShallowComponent(FirewallAutoUnquarantinePage, minimalProps);
  });

  it('renders a component with the "nx-page-main" class', function() {
    expect(getShallowComponent().find('.nx-page-main')).toExist();
  });

  it('renders a BackButton with correct state and text properties', function() {
    const component = getShallowComponent();
    const backButton = component.find(BackButton);
    expect(backButton).toExist();
    expect(backButton).toHaveProp('text', 'Back to Quarantine');
    expect(backButton).toHaveProp('$state', stateMock);
  });

  it('renders a page title', function() {
    const component = getShallowComponent();
    expect(component.find('.nx-page-title')).toExist();
    expect(component.find('.nx-h1')).toHaveText('Auto Release from Quarantine');
  });

  it('renders a card container', function() {
    const component = getShallowComponent();
    expect(component.find('.nx-card-container')).toExist();
  });

  it('renders a FirewallAutoReleaseQuarantineMtd card', function() {
    const component = getShallowComponent(),
        card = component.find(FirewallAutoReleaseQuarantineMtd);

    expect(card).toExist();
    expect(card).toHaveProp('autoReleaseQuarantineCountMTD', 1);
  });

  it('renders a FirewallAutoReleaseQuarantineYtd card', function() {
    const component = getShallowComponent(),
        card = component.find(FirewallAutoReleaseQuarantineYtd);

    expect(card).toExist();
    expect(card).toHaveProp('autoReleaseQuarantineCountYTD', 2);
  });

  it('renders a FirewallAutoUnquarantineStatus card', function() {
    const component = getShallowComponent(),
        card = component.find(FirewallAutoUnquarantineStatus);

    expect(card).toExist();
    expect(card).toHaveProp('autoUnquarantineEnabled', false);
    expect(card).toHaveProp('enabledPolicyConditionTypesCount', 3);
    expect(card).toHaveProp('totalPolicyConditionTypesCount', 4);
    expect(card).toHaveProp('openConfigurationModal', openConfigurationModalSpy);
  });

  it('renders a FirewallPolicyConditionTypes card', function() {
    const component = getShallowComponent(),
        card = component.find(FirewallPolicyConditionTypes);

    expect(card).toExist();
    expect(card).toHaveProp('openConfigurationModal', openConfigurationModalSpy);
  });

  it('renders a FirewallUnquarantineTable', function() {
    const component = getShallowComponent(),
        card = component.find(FirewallUnquarantineTable);

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
