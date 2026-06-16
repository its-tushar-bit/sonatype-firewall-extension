/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { selectViolationSummary } from 'MainRoot/nosc/applications/applicationDetailSelectors';
import type { FlatViolation } from 'MainRoot/nosc/applications/applicationDetailTypes';

function violation(partial: Partial<FlatViolation> & Pick<FlatViolation, 'waived'>): FlatViolation {
  return {
    key: partial.key ?? 'k1',
    policyName: partial.policyName ?? 'Policy',
    policyThreatLevel: partial.policyThreatLevel ?? 10,
    policyThreatCategory: partial.policyThreatCategory ?? 'SECURITY',
    threatLabel: partial.threatLabel ?? 'Critical',
    threatColor: partial.threatColor ?? 'red',
    componentDisplay: partial.componentDisplay ?? 'component.jar',
    componentHash: partial.componentHash ?? 'hash',
    waived: partial.waived,
    legacy: partial.legacy ?? false,
    constraintName: partial.constraintName ?? '',
  };
}

function summaryFor(violations: ReadonlyArray<FlatViolation>) {
  return selectViolationSummary.resultFunc(violations);
}

describe('selectViolationSummary', () => {
  it('excludes waived violations from maliciousCount and severity buckets', () => {
    const violations = [
      violation({
        key: 'waived-mal',
        policyName: 'Malicious package policy',
        policyThreatCategory: 'MALICIOUS',
        waived: true,
      }),
      violation({
        key: 'open-mal',
        policyName: 'Malicious package policy',
        policyThreatCategory: 'MALICIOUS',
        waived: false,
      }),
    ];

    const summary = summaryFor(violations);
    expect(summary.maliciousCount).toBe(1);
    expect(summary.waivedViolations).toBe(1);
    expect(summary.openViolations).toBe(1);
    expect(summary.criticalCount).toBe(1);
  });
});
