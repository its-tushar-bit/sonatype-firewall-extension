/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import FirewallPolicyViolationsTile from 'MainRoot/firewall/firewallComponentDetailsPage/policyViolations/policyViolationsTile/FirewallPolicyViolationsTile';

describe('FirewallPolicyViolationsTile component', () => {
  let minimalProps, renderComponentTile;

  beforeEach(() => {
    minimalProps = {
      title: 'Policy Violations',
    };
    renderComponentTile = (minimalProps) => render(<FirewallPolicyViolationsTile {...minimalProps} />);
  });

  it('It should render Title', () => {
    renderComponentTile(minimalProps);
    const policy = screen.getByText('Policy Violations');
    expect(policy).toBeVisible();
  });
});
