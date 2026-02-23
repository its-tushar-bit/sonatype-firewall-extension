/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from '../../../SpecUtil';
import FirewallSecurityTab from 'MainRoot/firewall/firewallComponentDetailsPage/security/FirewallSecurityTab';
import * as FirewallPolicyViolationsTile from 'MainRoot/firewall/firewallComponentDetailsPage/policyViolations/policyViolationsTile/FirewallPolicyViolationsTile';
import * as VulnerabilitiesTableTile from 'MainRoot/componentDetails/VulnerabilitiesTableTile/VulnerabilitiesTableTile';
import * as firewallPolicyViolationsSelectors from 'MainRoot/firewall/firewallComponentDetailsPage/policyViolations/firewallPolicyViolationsSelectors.js';
import * as vulnerabilitiesSelector from 'MainRoot/componentDetails/VulnerabilitiesTableTile/vulnerabilitiesSelectors';

import 'TestRoot/SpecUtil';

describe('FirewallSecurityTab', () => {
  let minState = {
      firewall: {
        componentDetailsPage: {
          isLoadingComponentDetails: false,
          componentDetails: {},
          componentDetailsError: null,
          policyViolationsError: null,
          policyViolations: [
            {
              policyViolationId: '049d0649944148b0be20ab9d75b5ff7a',
              policyId: 'f2c65f8d67e8405eb7d789dc227a37d4',
              policyName: 'Security-Medium',
              policyThreatLevel: 7,
              policyThreatCategory: 'SECURITY',
              constraints: [
                {
                  constraintId: 'f60e96454d5148b69e45326409f5d976',
                  constraintName: 'Medium risk CVSS score',
                  constraintOperator: 'AND',
                  conditions: [
                    {
                      conditionType: 'SecurityVulnerabilitySeverity',
                      conditionSummary: 'Security Vulnerability Severity >= 4',
                      conditionReason: 'Found security vulnerability CVE-2012-2098 with severity >= 4 (severity = 5.0)',
                      conditionTriggerReference: {
                        value: 'CVE-2012-2098',
                        type: 'SECURITY_VULNERABILITY_REFID',
                      },
                    },
                    {
                      conditionType: 'SecurityVulnerabilitySeverity',
                      conditionSummary: 'Security Vulnerability Severity < 7',
                      conditionReason: 'Found security vulnerability CVE-2012-2098 with severity < 7 (severity = 5.0)',
                      conditionTriggerReference: {
                        value: 'CVE-2012-2098',
                        type: 'SECURITY_VULNERABILITY_REFID',
                      },
                    },
                  ],
                },
              ],
              constraintFactsJson:
                '[{"constraintId":"f60e96454d5148b69e45326409f5d976","constraintName":"Medium risk CVSS score","operatorName":"AND","conditionFacts":[{"conditionTypeId":"SecurityVulnerabilitySeverity","conditionIndex":0,"summary":"Security Vulnerability Severity >= 4","reason":"Found security vulnerability CVE-2012-2098 with severity >= 4 (severity = 5.0)","reference":{"value":"CVE-2012-2098","type":"SECURITY_VULNERABILITY_REFID"},"triggerJson":"{\\"conditionIndex\\":0,\\"trigger\\":{\\"refId\\":\\"CVE-2012-2098\\",\\"severity\\":5.0}}"},{"conditionTypeId":"SecurityVulnerabilitySeverity","conditionIndex":1,"summary":"Security Vulnerability Severity < 7","reason":"Found security vulnerability CVE-2012-2098 with severity < 7 (severity = 5.0)","reference":{"value":"CVE-2012-2098","type":"SECURITY_VULNERABILITY_REFID"},"triggerJson":"{\\"conditionIndex\\":1,\\"trigger\\":{\\"refId\\":\\"CVE-2012-2098\\",\\"severity\\":5.0}}"}]}]',
              policyActionTypeId: null,
            },
          ],
          isLoadingPolicyViolations: false,
        },
      },
      componentDetailsVulnerabilities: {
        vulnerabilities: {
          data: [
            {
              refId: 'CVE-2012-2098',
              severity: 5,
              source: 'cve',
              summary:
                'Algorithmic complexity vulnerability in the sorting algorithms in bzip2 compressing stream (BZip2CompressorOutputStream) in Apache Commons Compress before 1.4.1 allows remote attackers to cause a denial of service (CPU consumption) via a file with many repeating inputs.',
              status: 'Open',
              url: null,
              vulnerabilityCategories: ['functional', 'data'],
              aliases: [],
            },
          ],
          loading: false,
          error: null,
        },
      },
    },
    originalSelectSecurityPolicyViolations = firewallPolicyViolationsSelectors.selectSecurityPolicyViolations,
    originalSelectVulnerabilitiesSortedSlice = vulnerabilitiesSelector.selectVulnerabilitiesSortedSlice,
    spyVulnerabilitiesTableTile,
    spyFirewallPolicyViolationsTile;

  beforeEach(() => {
    spyFirewallPolicyViolationsTile = jest
      .spyOn(FirewallPolicyViolationsTile, 'default')
      .mockImplementation(() => <div>FirewallPolicyViolationsTile</div>);
    spyVulnerabilitiesTableTile = jest
      .spyOn(VulnerabilitiesTableTile, 'default')
      .mockImplementation(() => <div>VulnerabilitiesTableTile</div>);
    jest.spyOn(vulnerabilitiesSelector, 'selectVulnerabilitiesSortedSlice').mockImplementation(() => {
      return originalSelectVulnerabilitiesSortedSlice(minState);
    });

    jest.spyOn(firewallPolicyViolationsSelectors, 'selectSecurityPolicyViolations').mockImplementation(() => {
      return originalSelectSecurityPolicyViolations(minState);
    });

    SpecUtil.mockReduxStore(minState);
  });

  it('renders the FirewallPolicyViolationsTile and VulnerabilitiesTableTile', () => {
    render(<FirewallSecurityTab />);
    expect(screen.queryByText(/FirewallPolicyViolationsTile/)).toBeVisible();
    expect(spyFirewallPolicyViolationsTile.prototype.constructor).toHaveBeenCalledWith(
      {
        violations: originalSelectSecurityPolicyViolations(minState),
        showProxyState: true,
        title: 'Security Violations',
      }, // params
      {} // state
    );
    expect(screen.queryByText(/VulnerabilitiesTableTile/)).toBeVisible();
    expect(spyVulnerabilitiesTableTile.prototype.constructor).toHaveBeenCalledWith(
      {
        vulnerabilities: originalSelectVulnerabilitiesSortedSlice(minState),
        isLoadingComponentDetails: false,
        componentDetailsLoadError: null,
        loadComponentDetails: expect.any(Function),
        loadVulnerabilities: expect.any(Function),
        toggleVulnerabilityPopoverWithEffects: expect.any(Function),
      }, // params
      {} // state
    );
  });
});
