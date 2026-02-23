/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, within } from 'TestRoot/SpecUtil';
import FirewallPolicyViolationsTableRow from 'MainRoot/firewall/firewallComponentDetailsPage/policyViolations/policyViolationsTile/FirewallPolicyViolationsTableRow';

import 'TestRoot/SpecUtil';

describe('FirewallPolicyViolationsTableRow component', () => {
  let minimalProps, renderComponent, showPopoverSpy;

  beforeEach(() => {
    showPopoverSpy = jest.fn().mockName('showPopover');
    minimalProps = {
      violation: {
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
        policyActionTypeId: 'fail',
        waived: false,
        applicableWaivers: [],
      },
      showPopover: showPopoverSpy,
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

  describe('enabling proxy state flags', () => {
    beforeEach(() => {
      renderComponent = (customMinimalProps = minimalProps) =>
        render(<FirewallPolicyViolationsTableRow {...{ ...customMinimalProps, showProxyState: true }} />);
    });

    it('render proxy failed state flag', () => {
      renderComponent();

      const rows = screen.getAllByRole('row');

      expect(within(rows[0].childNodes[1]).getByText('Proxy Failing')).toBeVisible();
    });

    it('render proxy warning state flag', () => {
      renderComponent({ ...minimalProps, violation: { ...minimalProps.violation, policyActionTypeId: 'warn' } });

      const rows = screen.getAllByRole('row');

      expect(within(rows[0].childNodes[1]).getByText('Proxy Warning')).toBeVisible();
    });
  });

  it('does not render an information indicator when the violation has been waived', () => {
    renderComponent({
      ...minimalProps,
      violation: { ...minimalProps.violation, applicableWaivers: ['waiver1'], waived: true },
    });

    const rows = screen.getAllByRole('row');

    expect(within(rows[0].childNodes[4]).getByText('1')).toBeVisible();
    expect(within(rows[0].childNodes[4]).getByText('Active Waiver')).toBeVisible();
  });

  it('renders an information indicator when there are unapplied waivers', () => {
    renderComponent({
      ...minimalProps,
      violation: { ...minimalProps.violation, applicableWaivers: ['waiver1'], waived: false },
    });

    const rows = screen.getAllByRole('row');

    console.log('rows', rows);

    expect(within(rows[0].childNodes[4]).getByText('Unapplied Waiver')).toBeVisible();
  });
});
