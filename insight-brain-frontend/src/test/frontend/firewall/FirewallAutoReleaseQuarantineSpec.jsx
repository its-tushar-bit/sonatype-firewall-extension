/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxTextLink } from '@sonatype/react-shared-components';

import * as enzymeUtils from '../enzymeUtils';

describe('FirewallAutoReleaseQuarantine', function () {
  let minimalProps, FirewallAutoReleaseQuarantine, getShallowComponent, stateMock, stateHrefSpy;

  beforeEach(function () {
    stateHrefSpy = jasmine.createSpy().and.returnValue('href');
    stateMock = {
      href: stateHrefSpy,
    };

    FirewallAutoReleaseQuarantine = require('inject-loader!../../../main/frontend/firewall/FirewallAutoReleaseQuarantine')()
      .default;

    minimalProps = {
      autoReleaseQuarantineCountMTD: 1,
      $state: stateMock,
    };

    getShallowComponent = enzymeUtils.getShallowComponent(FirewallAutoReleaseQuarantine, minimalProps);
  });

  it('renders a component with the "nx-card" class', function () {
    expect(getShallowComponent().find('.nx-card')).toExist();
  });

  it('renders a card header', function () {
    expect(getShallowComponent().find('.nx-card__header')).toExist();
    expect(getShallowComponent().find('.nx-card__header')).toHaveText('Auto Released from Quarantine');
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

  it('renders card footer', function () {
    const component = getShallowComponent();
    const footer = component.find('.nx-card__footer');
    expect(footer).toExist();
    const link = component.find(NxTextLink);
    expect(link).toHaveText('View Auto Release Quarantine');
    expect(link).toHaveProp('href', 'href');
    link.simulate('click');
    expect(stateHrefSpy).toHaveBeenCalledWith('firewall.firewallAutoUnquarantinePage');
  });
});
