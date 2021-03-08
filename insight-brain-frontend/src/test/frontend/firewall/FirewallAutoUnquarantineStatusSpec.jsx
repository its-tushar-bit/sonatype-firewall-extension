/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import StatusIndicatorIcon from '../../../main/frontend/react/statusIndicatorIcon/StatusIndicatorIcon';

describe('FirewallAutoUnquarantineStatus', function() {
  let minimalProps,
      FirewallAutoUnquarantineStatus,
      openConfigurationModalSpy,
      getShallowComponent;

  beforeEach(function() {
    FirewallAutoUnquarantineStatus = require(
        'inject-loader!../../../main/frontend/firewall/FirewallAutoUnquarantineStatus')().default;

    openConfigurationModalSpy = jasmine.createSpy('openConfigurationModal');

    minimalProps = {
      openConfigurationModal: openConfigurationModalSpy,
      enabledPolicyConditionTypesCount: 1,
      totalPolicyConditionTypesCount: 2,
      autoUnquarantineEnabled: true,
      showCounts: true
    };

    getShallowComponent = enzymeUtils.getShallowComponent(FirewallAutoUnquarantineStatus, minimalProps);
  });

  it('renders a component with the "nx-card" class', function() {
    expect(getShallowComponent().find('.nx-card')).toExist();
  });

  it('renders a card header', function() {
    expect(getShallowComponent().find('.nx-card__header')).toExist();
    expect(getShallowComponent().find('.nx-card__header')).toHaveText('Auto Release from Quarantine Status');
  });

  it('renders card content', function() {
    const component = getShallowComponent();
    const content = component.find('.nx-card__content');
    expect(content).toExist();
  });

  it('renders active status indicator when enabled', function() {
    const component = getShallowComponent(),
        indicator = component.find('.iq-status-indicator'),
        icon = indicator.find(StatusIndicatorIcon),
        text = indicator.find('span');

    expect(indicator).toExist();
    expect(icon).toExist();
    expect(text).toHaveText('Active');
    expect(icon).toHaveProp('status', true);
  });

  it('renders inactive status indicator when disabled', function() {
    const component = getShallowComponent({autoUnquarantineEnabled: false}),
        indicator = component.find('.iq-status-indicator'),
        icon = indicator.find(StatusIndicatorIcon),
        text = indicator.find('span');

    expect(indicator).toExist();
    expect(icon).toExist();
    expect(text).toExist();
    expect(text).toHaveText('Inactive');
    expect(icon).toHaveProp('status', false);
  });

  it('renders counts when showCounts is true', function() {
    const component = getShallowComponent(),
        counts = component.find('.nx-card__text');

    expect(counts).toExist();
    expect(counts).toHaveText('releasing 1 of 2 policy types');
  });

  it('does not render counts when showCounts is false', function() {
    const component = getShallowComponent({showCounts: false}),
        counts = component.find('.nx-card__text');

    expect(counts).not.toExist();
  });

  it('renders card footer', function() {
    const component = getShallowComponent();
    const footer = component.find('.nx-card__footer');
    expect(footer).toExist();
    const configureLink = component.find('.nx-text-link');
    expect(configureLink).toHaveText('Configure');
    configureLink.simulate('click');
    expect(openConfigurationModalSpy).toHaveBeenCalled();
  });
});
