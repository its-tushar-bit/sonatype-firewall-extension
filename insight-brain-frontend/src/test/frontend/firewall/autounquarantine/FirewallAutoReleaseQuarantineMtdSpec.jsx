/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';

describe('FirewallAutoReleaseQuarantineMtd', function () {
  let minimalProps, FirewallAutoReleaseQuarantineMtd, getShallowComponent;

  beforeEach(function () {
    FirewallAutoReleaseQuarantineMtd = require('inject-loader!../../../../main/frontend/firewall/autounquarantine/FirewallAutoReleaseQuarantineMtd')()
      .default;

    minimalProps = {
      autoReleaseQuarantineCountMTD: 1,
    };

    getShallowComponent = enzymeUtils.getShallowComponent(FirewallAutoReleaseQuarantineMtd, minimalProps);
  });

  it('renders a component with the "nx-card" class', function () {
    expect(getShallowComponent().find('.nx-card')).toExist();
  });

  it('renders a card header', function () {
    expect(getShallowComponent().find('.nx-card__header')).toExist();
    expect(getShallowComponent().find('.nx-card__header')).toHaveText('Auto Released (Month to Date)');
  });

  it('renders card content', function () {
    const component = getShallowComponent();
    const content = component.find('.nx-card__content');
    expect(content).toExist();
    const callout = component.find('.nx-card__call-out');
    expect(callout).toHaveText('1');
    const text = component.find('.nx-card__text');
    expect(text).toHaveText('components released month-to-date');
  });
});
