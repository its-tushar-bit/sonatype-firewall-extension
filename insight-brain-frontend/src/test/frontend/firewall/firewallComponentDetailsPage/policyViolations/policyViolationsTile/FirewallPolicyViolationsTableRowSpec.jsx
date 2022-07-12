/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, within } from 'TestRoot/SpecUtil';
import FirewallPolicyViolationsTableRow from 'MainRoot/firewall/firewallComponentDetailsPage/policyViolations/policyViolationsTile/FirewallPolicyViolationsTableRow';

describe('FirewallPolicyViolationsTableRow component', () => {
  let minimalProps, renderComponent;

  beforeEach(() => {
    minimalProps = {
      violation: {
        policyId: 'd98fb873ed1f48e5b00316d8acddbc0f',
        policyName: 'Security-Medium',
        threatLevel: 7,
        componentFacts: [
          {
            componentIdentifier: {
              format: 'maven',
              coordinates: {
                artifactId: 'ant',
                classifier: '',
                extension: 'jar',
                groupId: 'ant',
                version: '1.6',
              },
            },
            hash: '7a3c2521ae0c6f53e044',
            constraintFacts: [
              {
                constraintId: 'c6436a5a051046b1ba2aa94e9fd82a51',
                constraintName: 'Medium risk CVSS score',
                operatorName: 'AND',
                conditionFacts: [
                  {
                    conditionTypeId: 'SecurityVulnerabilitySeverity',
                    conditionIndex: 0,
                    summary: 'Security Vulnerability Severity >= 4',
                    reason: 'Found security vulnerability CVE-2012-2098 with severity >= 4 (severity = 5.0)',
                    reference: {
                      value: 'CVE-2012-2098',
                      type: 'SECURITY_VULNERABILITY_REFID',
                    },
                    triggerJson: '{"conditionIndex":0,"trigger":{"refId":"CVE-2012-2098","severity":5.0}}',
                  },
                  {
                    conditionTypeId: 'SecurityVulnerabilitySeverity',
                    conditionIndex: 1,
                    summary: 'Security Vulnerability Severity < 7',
                    reason: 'Found security vulnerability CVE-2012-2098 with severity < 7 (severity = 5.0)',
                    reference: {
                      value: 'CVE-2012-2098',
                      type: 'SECURITY_VULNERABILITY_REFID',
                    },
                    triggerJson: '{"conditionIndex":1,"trigger":{"refId":"CVE-2012-2098","severity":5.0}}',
                  },
                ],
              },
            ],
            pathnames: [],
            displayName: {
              parts: [
                {
                  field: 'Group',
                  value: 'ant',
                },
                {
                  value: ' : ',
                },
                {
                  field: 'Artifact',
                  value: 'ant',
                },
                {
                  value: ' : ',
                },
                {
                  field: 'Version',
                  value: '1.6',
                },
              ],
              name: 'ant',
            },
          },
        ],
      },
    };
    renderComponent = (minimalProps) => render(<FirewallPolicyViolationsTableRow {...minimalProps} />);
  });

  it('render the component', () => {
    const { container } = render(<FirewallPolicyViolationsTableRow {...minimalProps} />);
    expect(container).toBeVisible();
  });

  it('render table row with data', () => {
    renderComponent(minimalProps);

    const rows = screen.getAllByRole('row');

    expect(within(rows[0].childNodes[0]).getByText('7')).toBeVisible();
    expect(within(rows[0].childNodes[1]).getByText('Security-Medium')).toBeVisible();
    expect(within(rows[0].childNodes[2]).getByText('Medium risk CVSS score')).toBeVisible();
    expect(
      within(rows[0].childNodes[3]).getByText(
        'Found security vulnerability CVE-2012-2098 with severity >= 4 (severity = 5.0)'
      )
    ).toBeVisible();
  });
});
