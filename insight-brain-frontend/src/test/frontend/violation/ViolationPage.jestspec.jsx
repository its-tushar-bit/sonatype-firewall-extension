/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import ViolationPage, { MISSING_VIOLATION_ID_MESSAGE_TEXT } from 'MainRoot/violation/ViolationPage';
import { always } from 'ramda';

describe('ViolationPage', () => {
  let minimalProps,
    loadViolationSpy,
    fetchStageTypesSpy,
    stateGoSpy,
    loadFirewallPolicyVulnerabilityDetailsSpy,
    loadFirewallViolationDetailsSpy,
    loadSimilarWaiversSpy;

  beforeEach(function () {
    loadViolationSpy = jest.fn();
    fetchStageTypesSpy = jest.fn();
    stateGoSpy = jest.fn();
    loadFirewallPolicyVulnerabilityDetailsSpy = jest.fn();
    loadFirewallViolationDetailsSpy = jest.fn();
    loadSimilarWaiversSpy = jest.fn();

    minimalProps = getMinimalProps();
  });

  it('renders a LoadWrapper within the page', () => {
    renderComponent();
    expect(screen.getByText('Loading…')).toBeInTheDocument();
  });

  it('renders an indicator that there is a problem if the policy violations are misssing database identifiers', () => {
    renderComponent({ selectedViolationId: null });

    expect(loadViolationSpy).not.toHaveBeenCalled();

    expect(screen.queryByText('Loading…')).toBeNull();
    expect(screen.getByText(MISSING_VIOLATION_ID_MESSAGE_TEXT)).toBeInTheDocument();
  });

  it('calls loadViolation with the $state id param, fetchStageTypes with the `dashboard` param and loadAddWaiverPermission on first load', function () {
    renderComponent({ selectedViolationId: 'any-given-valid-violation-id' });

    expect(loadViolationSpy).toHaveBeenCalledWith('any-given-valid-violation-id');
    expect(fetchStageTypesSpy).toHaveBeenCalledWith('dashboard');
  });

  it('calls loadViolation whenever the violation details is null and isFirewallContext is false', function () {
    renderComponent({
      violationDetails: null,
      isFirewallContext: false,
      stageTypes: [],
      stateGo: jest.fn().mockReturnValue({}),
    });

    expect(loadViolationSpy).toHaveBeenCalledWith('foo');
  });

  it('calls loadFirewallPolicyVulnerabilityDetails when there is a firewall context', function () {
    renderComponent({ isFirewallContext: true });

    expect(loadFirewallPolicyVulnerabilityDetailsSpy).toHaveBeenCalledWith('CVE-2012-2098');
    expect(loadViolationSpy).not.toHaveBeenCalled();
  });

  function renderComponent(props) {
    props = props || {};
    props = { ...minimalProps, ...props };

    const preloadState = {};

    return render(<ViolationPage {...props} />, preloadState);
  }

  function getMinimalProps() {
    return {
      $state: {
        get: always({
          data: {
            title: 'asdf',
          },
        }),
        href: always('qwerty'),
      },
      selectedViolationId: 'foo',
      loadViolation: loadViolationSpy,
      fetchStageTypes: fetchStageTypesSpy,
      loadVulnerabilityDetails: loadFirewallViolationDetailsSpy,
      setSelectPolicyViolation: jest.fn(),
      setFilterIdsSimilarWaivers: jest.fn(),
      stateGo: stateGoSpy,
      loading: false,
      isFirewallContext: false,
      isFirewall: false,
      vulnerabilityDetailsLoading: true,
      isVulnerabilityDetailsOutdated: false,
      refId: { value: 'CVE-2012-2098' },
      policyDetail: {
        policyViolationId: '02a6107559a94c39b04d4ec8374b9508',
        policyId: 'd98fb873ed1f48e5b00316d8acddbc0f',
        policyName: 'Security-Medium',
        policyOwner: {
          ownerId: 'ROOT_ORGANIZATION_ID',
          ownerName: 'Root Organization',
          ownerType: 'organization',
        },
        policyThreatLevel: 7,
        policyThreatCategory: 'SECURITY',
        constraints: [
          {
            constraintId: 'c6436a5a051046b1ba2aa94e9fd82a51',
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
        policyActionTypeId: null,
        lastReported: '2022-08-10T13:35:40.641+03:00',
      },
      constraintViolations: [
        {
          constraintId: 'c6436a5a051046b1ba2aa94e9fd82a51',
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
      policyViolations: [
        {
          policyViolationId: '02a6107559a94c39b04d4ec8374b9508',
          policyId: 'd98fb873ed1f48e5b00316d8acddbc0f',
          policyName: 'Security-Medium',
          policyOwner: {
            ownerId: 'ROOT_ORGANIZATION_ID',
            ownerName: 'Root Organization',
            ownerType: 'organization',
          },
          policyThreatLevel: 7,
          policyThreatCategory: 'SECURITY',
          constraints: [
            {
              constraintId: 'c6436a5a051046b1ba2aa94e9fd82a51',
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
          policyActionTypeId: null,
          lastReported: '2022-08-10T13:35:40.641+03:00',
        },
      ],
      selectPolicyId: '02a6107559a94c39b04d4ec8374b9508',
      loadFirewallPolicyVulnerabilityDetails: loadFirewallPolicyVulnerabilityDetailsSpy,
      loadFirewallViolationDetails: loadFirewallViolationDetailsSpy,
      loadSimilarWaivers: loadSimilarWaiversSpy,
      hasPermissionForAppWaivers: true,
      activeWaivers: [],
      expiredWaivers: [],
      matchExact: null,
    };
  }
});
