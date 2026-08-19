/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import FirewallPolicyViolationsTable, {
  sortPolicyByThreat,
} from 'MainRoot/firewall/firewallComponentDetailsPage/policyViolations/policyViolationsTile/FirewallPolicyViolationsTable';

describe('FirewallPolicyViolationsTable', () => {
  let renderComponentTable, notSortArray, expectArray;

  beforeEach(() => {
    notSortArray = [
      {
        policyThreatLevel: 1,
        policyName: 'Architecture-Quality',
      },
      {
        policyThreatLevel: 1,
        policyName: 'Architecture-Quality',
      },
      {
        policyThreatLevel: 5,
        policyName: 'test-policy',
      },
      {
        policyThreatLevel: 1,
        policyName: 'Architecture-Cleanup',
      },
      {
        policyThreatLevel: 7,
        policyName: 'Security-Medium',
      },
      {
        policyThreatLevel: 7,
        policyName: 'Security-Medium',
      },
    ];

    expectArray = [
      {
        policyThreatLevel: 7,
        policyName: 'Security-Medium',
      },
      {
        policyThreatLevel: 7,
        policyName: 'Security-Medium',
      },
      {
        policyThreatLevel: 5,
        policyName: 'test-policy',
      },
      {
        policyThreatLevel: 1,
        policyName: 'Architecture-Cleanup',
      },
      {
        policyThreatLevel: 1,
        policyName: 'Architecture-Quality',
      },
      {
        policyThreatLevel: 1,
        policyName: 'Architecture-Quality',
      },
    ];

    renderComponentTable = () => render(<FirewallPolicyViolationsTable />);
  });

  it('render component with headers', () => {
    renderComponentTable();

    const threat = screen.getByText('Threat');
    const policyAction = screen.getByText('Policy/Action');
    const constraintName = screen.getByText('Constraint Name');
    const condition = screen.getByText('Condition');
    expect(threat).toBeVisible();
    expect(policyAction).toBeVisible();
    expect(constraintName).toBeVisible();
    expect(condition).toBeVisible();
  });

  it('test sorting policy violations', () => {
    const sorted = sortPolicyByThreat(notSortArray);
    expect(expectArray).toEqual(sorted);
  });
});
