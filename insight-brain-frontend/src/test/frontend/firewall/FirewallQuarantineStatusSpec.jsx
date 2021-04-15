/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import StatusIndicatorIcon from '../../../main/frontend/react/statusIndicatorIcon/StatusIndicatorIcon';

describe('FirewallQuarantineStatus', function () {
  let minimalProps, FirewallQuarantineStatus, getShallowComponent;

  beforeEach(function () {
    FirewallQuarantineStatus = require('inject-loader!../../../main/frontend/firewall/FirewallQuarantineStatus')()
      .default;

    minimalProps = {
      quarantineEnabledRepositoryCount: 1,
      repositoryCount: 2,
      quarantineEnabled: true,
    };

    getShallowComponent = enzymeUtils.getShallowComponent(FirewallQuarantineStatus, minimalProps);
  });

  it('renders a component with the "nx-card" class', function () {
    expect(getShallowComponent().find('.nx-card')).toExist();
  });

  it('renders a card header', function () {
    expect(getShallowComponent().find('.nx-card__header')).toExist();
    expect(getShallowComponent().find('.nx-card__header')).toHaveText('Quarantine Status');
  });

  it('renders card content', function () {
    const component = getShallowComponent();
    const content = component.find('.nx-card__content');
    expect(content).toExist();
  });

  it('renders active status indicator when enabled', function () {
    const component = getShallowComponent(),
      indicator = component.find('.iq-status-indicator'),
      icon = indicator.find(StatusIndicatorIcon),
      text = indicator.find('span');

    expect(indicator).toExist();
    expect(icon).toExist();
    expect(text).toHaveText('Active');
    expect(icon).toHaveProp('status', true);
  });

  it('renders inactive status indicator when disabled', function () {
    const component = getShallowComponent({ quarantineEnabled: false }),
      indicator = component.find('.iq-status-indicator'),
      icon = indicator.find(StatusIndicatorIcon),
      text = indicator.find('span');

    expect(indicator).toExist();
    expect(icon).toExist();
    expect(text).toExist();
    expect(text).toHaveText('Inactive');
    expect(icon).toHaveProp('status', false);
  });

  it('renders counts', function () {
    const component = getShallowComponent(),
      counts = component.find('.nx-card__text');

    expect(counts).toExist();
    expect(counts).toHaveText('on 1 of 2 repositories');
  });
});
