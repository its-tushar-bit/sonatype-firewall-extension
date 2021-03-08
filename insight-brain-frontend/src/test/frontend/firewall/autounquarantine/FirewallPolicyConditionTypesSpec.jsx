/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';

describe('FirewallPolicyConditionTypes', function() {
  let minimalProps,
      FirewallPolicyConditionTypes,
      openConfigurationModalSpy,
      getShallowComponent;

  beforeEach(function() {
    FirewallPolicyConditionTypes = require(
        'inject-loader!../../../../main/frontend/firewall/autounquarantine/FirewallPolicyConditionTypes')().default;

    openConfigurationModalSpy = jasmine.createSpy('openConfigurationModal');

    minimalProps = {
      openConfigurationModal: openConfigurationModalSpy
    };

    getShallowComponent = enzymeUtils.getShallowComponent(FirewallPolicyConditionTypes, minimalProps);
  });

  it('renders a component with the "nx-card" class', function() {
    expect(getShallowComponent().find('.nx-card')).toExist();
  });

  it('renders a card header', function() {
    expect(getShallowComponent().find('.nx-card__header')).toExist();
    expect(getShallowComponent().find('.nx-card__header')).toHaveText('Policies to be Auto Released from Quarantine');
  });

  it('renders card content', function() {
    const component = getShallowComponent();
    const content = component.find('.nx-card__content');
    expect(content).toExist();
    const moreLink = component.find('#firewall-policy-condition-types-config-link');
    expect(moreLink).toHaveText('more active policies');
    moreLink.simulate('click');
    expect(openConfigurationModalSpy).toHaveBeenCalled();
  });
});
