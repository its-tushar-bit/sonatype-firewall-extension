/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import PolicyViolationConstraintInfo from 'MainRoot/violation/PolicyViolationConstraintInfo';

describe('PolicyViolationConstraintInfo', function () {
  const minimalProps = {
    isFirewallContext: false,
    constraintViolations: [
      {
        constraintName: 'test constraint',
        reasons: [{ reason: 'reason 1' }, { reason: 'reason 2' }],
        conditions: [{ conditionReason: 'conditionReason 1' }, { conditionReason: 'conditionReason 2' }],
      },
    ],
  };
  const getShallowComponent = enzymeUtils.getShallowComponent(PolicyViolationConstraintInfo, minimalProps);

  it('renders Policy Constraint title', function () {
    const header = getShallowComponent().find('.nx-tile-header .nx-tile-header__title h3');
    expect(header).toHaveText('Policy Constraint');
  });

  it('renders an h3 with policy constraint name', function () {
    const header = getShallowComponent().find('div.nx-tile-content h3.nx-h3');
    expect(header).toHaveText('test constraint is in violation for the following reason(s):');
  });

  it('renders violation reasons', function () {
    const reasonListItems = getShallowComponent().find(
      'div.nx-list--violation-reasons #policy-violation-reasons .nx-list__item'
    );
    expect(reasonListItems.length).toBe(2);
    expect(reasonListItems.at(0)).toHaveText('reason 1');
    expect(reasonListItems.at(1)).toHaveText('reason 2');
  });

  it('renders conditions if isFirewallContext is active', () => {
    const component = getShallowComponent({ isFirewallContext: true });
    expect(component.find('.nx-tile-header__subtitle')).not.toExist();
  });

  it('renders violation conditions', function () {
    const reasonListItems = getShallowComponent({ isFirewallContext: true }).find(
      'div.nx-list--violation-reasons #policy-violation-reasons .nx-list__item'
    );
    expect(reasonListItems.length).toBe(2);
    expect(reasonListItems.at(0)).toHaveText('conditionReason 1');
    expect(reasonListItems.at(1)).toHaveText('conditionReason 2');
  });
});
