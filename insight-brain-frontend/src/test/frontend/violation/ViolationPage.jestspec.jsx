/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, screen, within } from 'TestRoot/SpecUtil';
import ViolationPage, { MISSING_VIOLATION_ID_MESSAGE_TEXT } from 'MainRoot/violation/ViolationPage';
import { always } from 'ramda';
import * as routerStateContext from 'MainRoot/react/RouterStateContext';

describe('ViolationPage', () => {
  let minimalProps,
    loadViolationSpy,
    fetchStageTypesSpy,
    stateGoSpy,
    loadFirewallPolicyVulnerabilityDetailsSpy,
    loadFirewallViolationDetailsSpy,
    loadSimilarWaiversSpy,
    routerContextMock;

  beforeEach(function () {
    routerContextMock = {
      href: jest.fn('href').mockImplementation(() => '#/dashboard/violations'),
      get: jest.fn('get').mockImplementation((state) => state),
      includes: jest.fn(),
    };

    jest.spyOn(routerStateContext, 'useRouterState').mockReturnValue(routerContextMock);

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

  it('calls loadFirewallViolationDetails with params --temp', function () {
    renderComponent({ isFirewallContext: true });
    expect(loadFirewallViolationDetailsSpy).toHaveBeenCalledWith('02a6107559a94c39b04d4ec8374b9508');
  });

  it('renders a back button with correct href', () => {
    renderComponent();
    const backButton = screen.getByRole('link');
    expect(backButton).toBeInTheDocument();
    expect(backButton).toHaveAttribute('href', '#/dashboard/violations');
  });

  it('does not render a back button if isFromPolicyViolations is true', () => {
    renderComponent({ isFromPolicyViolations: true });
    const backButton = screen.queryByRole('link');
    expect(backButton).not.toBeInTheDocument();
  });

  describe('Tabs with no active waiver', () => {
    describe('For non firewall', () => {
      it('renders 3 tabs for security violation', () => {
        renderComponent({
          violationLoading: false,
          violationDetails: getViolationDetailsForMediumSecurityWithVulnDataPresent(),
          vulnerabilityDetails: getVulnerabilityDetails(),
          stageTypes: getStateTypes(),
        });

        assert3TabsIncludingViolationTabRendered();
      });

      it('renders 2 tabs for non security violations', () => {
        const violationDetails = {
          ...getViolationDetailsForMediumSecurityWithVulnDataPresent(),
          policyThreatCategory: 'license',
        };

        renderComponent({
          violationLoading: false,
          violationDetails,
          vulnerabilityDetails: getVulnerabilityDetails(),
          stageTypes: getStateTypes(),
        });

        assert2TabsWhichExcludeViolationTabRendered();
      });

      it('renders 2 tabs when there is no associated vulnerability', async () => {
        renderComponent({
          violationLoading: false,
          violationDetails: getViolationDetailsForMediumSecurityWithVulnDataPresent(),
          vulnerabilityDetails: null,
          stageTypes: getStateTypes(),
        });

        assert2TabsWhichExcludeViolationTabRendered();
      });
    });

    describe('For firewall', () => {
      it('renders 3 tabs for security violation', () => {
        renderComponent({
          violationLoading: false,
          violationDetails: getViolationDetailsForMediumSecurityWithVulnDataPresent(),
          vulnerabilityDetails: getVulnerabilityDetails(),
          stageTypes: getStateTypes(),
          isFirewall: true,
        });

        assert3TabsIncludingViolationTabRendered();
      });

      it('renders 2 tabs for non security violations', () => {
        const violationDetails = {
          ...getViolationDetailsForMediumSecurityWithVulnDataPresent(),
          policyThreatCategory: 'license',
        };

        renderComponent({
          violationLoading: false,
          violationDetails,
          vulnerabilityDetails: getVulnerabilityDetails(),
          stageTypes: getStateTypes(),
          isFirewall: true,
        });

        assert2TabsWhichExcludeViolationTabRendered();
      });
    });

    describe('Tabs with active waivers', () => {
      describe('For non firewall', () => {
        it('renders 3 tabs for security violation', () => {
          renderComponent({
            violationLoading: false,
            violationDetails: getViolationDetailsForMediumSecurityWithVulnDataPresent(),
            vulnerabilityDetails: getVulnerabilityDetails(),
            stageTypes: getStateTypes(),
            activeWaivers: [getActiveWaiver(1), getActiveWaiver(2), getActiveWaiver(3)],
          });

          const tabs = assert3TabsIncludingViolationTabRendered();

          const indicator = tabs[1].querySelector('.iq-waiver-indicator-tab');
          expect(indicator.textContent).toContain('3 Applicable Waivers');
        });

        it('renders 2 tabs for non security violations', () => {
          const violationDetails = {
            ...getViolationDetailsForMediumSecurityWithVulnDataPresent(),
            policyThreatCategory: 'license',
          };

          renderComponent({
            violationLoading: false,
            violationDetails,
            vulnerabilityDetails: getVulnerabilityDetails(),
            stageTypes: getStateTypes(),
            activeWaivers: [getActiveWaiver(5), getActiveWaiver(6)],
          });

          const tabs = assert2TabsWhichExcludeViolationTabRendered();
          expect(tabs[0].textContent).toContain('2 Applicable Waivers');
        });
      });

      describe('For firewall', () => {
        it('renders 2 tabs for security violation', () => {
          renderComponent({
            violationLoading: false,
            violationDetails: getViolationDetailsForMediumSecurityWithVulnDataPresent(),
            vulnerabilityDetails: getVulnerabilityDetails(),
            stageTypes: getStateTypes(),
            activeWaivers: [getActiveWaiver(1), getActiveWaiver(2), getActiveWaiver(3)],
            isFirewall: true,
          });

          const tabs = assert3TabsIncludingViolationTabRendered();

          expect(tabs[1].textContent).toContain('3 Applicable Waivers');
        });

        it('renders 2 tabs for non security violations', () => {
          const violationDetails = {
            ...getViolationDetailsForMediumSecurityWithVulnDataPresent(),
            policyThreatCategory: 'license',
          };

          renderComponent({
            violationLoading: false,
            violationDetails,
            vulnerabilityDetails: getVulnerabilityDetails(),
            stageTypes: getStateTypes(),
            activeWaivers: [getActiveWaiver(5), getActiveWaiver(6)],
            isFirewall: true,
          });

          const tabs = assert2TabsWhichExcludeViolationTabRendered();
          expect(tabs[0].textContent).toContain('2 Applicable Waivers');
        });
      });
    });
  });

  function renderComponent(props) {
    props = props || {};
    props = { ...minimalProps, ...props };

    const preloadState = {};

    return render(<ViolationPage {...props} />, preloadState);
  }

  function assert3TabsIncludingViolationTabRendered() {
    const tabs = screen.getAllByRole('tab');

    expect(screen.getAllByRole('tab').length).toEqual(3);
    expect(within(tabs[0]).getAllByText('Vulnerability Details')[0]).toBeVisible();
    expect(within(tabs[1]).getAllByText('Applicable Waivers')[0]).toBeVisible();
    expect(within(tabs[2]).getAllByText('Similar Waivers')[0]).toBeVisible();

    // check that the tab-contents is also rendered
    expect(screen.getByTestId('security-vulnerability-details-tile')).toBeVisible();

    return tabs;
  }

  function assert2TabsWhichExcludeViolationTabRendered() {
    const tabs = screen.queryAllByRole('tab');
    expect(tabs.length).toEqual(2);
    expect(within(tabs[0]).getAllByText('Applicable Waivers')[0]).toBeVisible();
    expect(within(tabs[1]).getAllByText('Similar Waivers')[0]).toBeVisible();

    expect(screen.queryByText('Vulnerability Details')).not.toBeInTheDocument();
    expect(screen.queryByTestId('security-vulnerability-details-tile')).not.toBeInTheDocument();

    return tabs;
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

  function getViolationDetailsForMediumSecurityWithVulnDataPresent() {
    return {
      policyId: 'cc3a29201b9544419cc2d8db93a75516',
      policyName: 'Security-Medium',
      policyViolationId: 'b0a2274d3c174e1c8fcf5a19e7308853',
      threatLevel: 7,
      constraintViolations: [
        {
          constraintId: '29d66c61fed642139e496a93e050d3e2',
          constraintName: 'Medium risk CVSS score',
          reasons: [
            {
              reason: 'Found security vulnerability CVE-2024-38809 with severity >= 4 (severity = 5.3)',
              reference: {
                type: 'SECURITY_VULNERABILITY_REFID',
                value: 'CVE-2024-38809',
              },
            },
            {
              reason: 'Found security vulnerability CVE-2024-38809 with severity < 7 (severity = 5.3)',
              reference: {
                type: 'SECURITY_VULNERABILITY_REFID',
                value: 'CVE-2024-38809',
              },
            },
          ],
        },
      ],
      applicationPublicId: 'some-app',
      applicationName: 'some-app',
      organizationName: 'Root Organization',
      openTime: '2024-10-01T17:20:16.066-04:00',
      fixTime: null,
      hash: 'cc3459b4abd436331608',
      policyThreatCategory: 'security',
      displayName: {
        parts: [
          {
            field: 'Group',
            value: 'org.springframework',
          },
          {
            value: ' : ',
          },
          {
            field: 'Artifact',
            value: 'spring-web',
          },
          {
            value: ' : ',
          },
          {
            field: 'Version',
            value: '6.1.3',
          },
        ],
        name: 'spring-web',
      },
      componentIdentifier: {
        format: 'maven',
        coordinates: {
          artifactId: 'spring-web',
          classifier: '',
          extension: 'jar',
          groupId: 'org.springframework',
          version: '6.1.3',
        },
      },
      filename: 'spring-web@6.1.3?type=jar',
      stageData: {
        build: {
          mostRecentEvaluationTime: '2024-10-01T17:20:16.066-04:00',
          mostRecentScanId: '9b28c9c3c43b4dbfbbbe3bbca5ee8853',
          actionTypeId: 'fail',
        },
      },
      policyOwner: {
        ownerId: 'ROOT_ORGANIZATION_ID',
        ownerName: 'Root Organization',
        ownerType: 'organization',
      },
      reachabilityStatus: null,
      waived: true,
    };
  }

  function getStateTypes() {
    return [
      {
        stageTypeId: 'source',
        stageName: 'Source',
        shortName: 'Source',
      },
      {
        stageTypeId: 'build',
        stageName: 'Build',
        shortName: 'Build',
      },
      {
        stageTypeId: 'stage-release',
        stageName: 'Stage Release',
        shortName: 'Stage',
      },
      {
        stageTypeId: 'release',
        stageName: 'Release',
        shortName: 'Release',
      },
      {
        stageTypeId: 'operate',
        stageName: 'Operate',
        shortName: 'Operate',
      },
    ];
  }

  function getVulnerabilityDetails() {
    return {
      identifier: 'CVE-2024-7254',
      vulnIds: ['CVE-2024-7254'],
      vulnerabilityLink: 'http://web.nvd.nist.gov/view/vuln/detail?vulnId=CVE-2024-7254',
      source: {
        shortName: 'CVE',
        longName: 'National Vulnerability Database',
      },
      mainSeverity: {
        source: 'sonatype_cvss_4',
        sourceLabel: 'Sonatype CVSS 4',
        score: 8.7,
        vector: 'CVSS:4.0/AV:N/AC:L/AT:N/PR:N/UI:N/VC:N/VI:N/VA:H/SC:N/SI:N/SA:N',
      },
      severityScores: [],
      weakness: {
        cweSource: 'CVE',
        cweIds: [
          {
            id: '20',
            uri: 'https://cwe.mitre.org/data/definitions/20.html',
          },
        ],
      },
      categories: ['data'],
      description: 'some-description',
      explanationMarkdown: 'some-markdown',
      componentExplanationMarkdown: '',
      detectionMarkdown: 'detection-markdown',
      componentDetectionMarkdown: '',
      recommendationMarkdown: 'recommendation-markdown',
      componentRecommendationMarkdown: '',
      rootCauses: [
        {
          listOfPaths: ['avro-tools-1.10.2.jar', 'com/google/protobuf/CodedInputStream.class'],
          versionRange: '( , 3.1.0)',
        },
      ],
      advisories: [
        {
          referenceType: 'PROJECT',
          url: 'https://github.com/advisories/GHSA-735f-pc8j-v9w8',
        },
      ],
      vulnerableVersionRanges: ['[1.8.0,1.12.0]'],
      researchType: 'DEEP_DIVE',
      isAdvancedVulnerabilityDetection: true,
      detectionType: 'SECONDARY',
    };
  }

  function getActiveWaiver(identifier) {
    return {
      policyWaiverId: `${identifier}-policyWaiverId`,
      comment: `${identifier}-comment`,
      scopeOwnerType: `${identifier}-scopeOwnerType`,
      scopeOwnerId: `${identifier}-scopeOwnerId`,
      scopeOwnerName: `${identifier}-scopeOwnerName`,
      hash: `${identifier}-hash`,
      policyId: `${identifier}-policyId`,
    };
  }
});
