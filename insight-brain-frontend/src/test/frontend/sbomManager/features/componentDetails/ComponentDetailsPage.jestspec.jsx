/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, fireEvent, queryByText } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { axiosMockAdapter, render, setupPortalContainer, waitFor, within } from 'TestRoot/SpecUtil';
import {
  getApplicationSummaryUrl,
  getSbomComponentDetailsUrl,
  getSbomVulnerabibilityAnalysisReferenceData,
  getSbomVulnerabilityAnnotationUrl,
  getSbomPolicyViolationReportUrl,
} from 'MainRoot/util/CLMLocation';
import ComponentDetailsPage from 'MainRoot/sbomManager/features/componentDetails/ComponentDetailsPage';
import {
  defaultSortConfiguration,
  policyViolationDetailsDrawerInitialState,
  sbomPolicyViolationsInitialState,
} from 'MainRoot/sbomManager/features/componentDetails/componentDetailsSlice';

describe('ComponentDetailsPage', () => {
  let renderPage;
  const applicationPublicId = 'app_123';
  const applicationInternalId = 'internalId';
  const fileCoordinateId = 'file-coordinate-id';
  const sbomVersion = '1.0-SNAPSHOT_TEST';
  const componentHash = 'componentHash';
  const axiosMock = axiosMockAdapter();

  const availableResponses = [
    {
      key: 'can_not_fix',
      value: 'Can not fix',
    },
    {
      key: 'rollback',
      value: 'Rollback',
    },
    {
      key: 'update',
      value: 'Update',
    },
    {
      key: 'will_not_fix',
      value: 'Will not fix',
    },
    {
      key: 'workaround_available',
      value: 'Workaround available',
    },
  ];

  const availableAnalysisStatuses = [
    {
      key: 'resolved',
      value: 'Resolved',
    },

    {
      key: 'resolved_with_pedigree',
      value: 'Resolved with pedigree',
    },

    {
      key: 'exploitable',
      value: 'Exploitable',
    },

    {
      key: 'in_triage',
      value: 'In triage',
    },

    {
      key: 'false_positive',
      value: 'False positive',
    },

    {
      key: 'not_affected',
      value: 'Not affected',
    },
  ];

  const availableJustifications = [
    {
      key: 'code_not_present',
      value: 'Code not present',
    },
    {
      key: 'code_not_reachable',
      value: 'Code not reachable',
    },
    {
      key: 'protected_at_perimeter',
      value: 'Protected at perimeter',
    },
    {
      key: 'protected_at_runtime',
      value: 'Protected at runtime',
    },
    {
      key: 'protected_by_compiler',
      value: 'Protected by compiler',
    },
    {
      key: 'protected_by_mitigating_control',
      value: 'Protected by mitigating control',
    },
    {
      key: 'requires_configuration',
      value: 'Requires configuration',
    },
    {
      key: 'requires_dependency',
      value: 'Requires dependency',
    },
    {
      key: 'requires_environment',
      value: 'Requires environment',
    },
  ];

  const vulnerabilityAnalysisReferenceData = {
    states: availableAnalysisStatuses,
    justifications: availableJustifications,
    responses: availableResponses,
  };

  const mockComponentDetails = {
    name: 'jackson-databind',
    hash: 'f07c773f7b3a03c3801d',
    fileCoordinateId,
    packageUrl: 'pkg:maven/net.sf.jason/jason-schema@1.2.11',
    dependencyType: 'Direct',
    version: '3.4.6',
    componentIdentifier: {
      format: 'maven',
      coordinates: {
        artifactId: 'jackson-databind',
        extension: 'jar',
        groupId: 'com.fasterxml.jackson.core',
        version: '3.4.6',
      },
    },
    displayName: 'com.fasterxml.jackson.core : jackson-databind : 2.4.1',
    metadata: {
      organizationName: 'test-org',
      applicationName: 'sbom',
      sbomCreationTime: 1713279301273,
      scanId: 'scan-id',
    },
    matchState: 'similar',
    filenames: null,
    vulnerabilitySummary: {
      highestCvssScore: 9,
      verifiedVulnerabilitiesCount: 10,
      unverifiedVulnerabilitiesCount: 5,
      sonatypeIdentifiedVulnerabilitiesCount: 0,
    },
    disclosedVulnerabilities: [
      {
        cvssScore: 9,
        issue: 'sonatype-2018-0863',
        description: 'short description',
        analysisStatus: 'resolved',
        justification: 'code_not_present',
        details: 'Unreachable code',
        verified: true,
        identificationSources: 'SBOM,Sonatype',
      },
      {
        cvssScore: 7,
        issue: 'sonatype-2018-9999',
        description: 'short description',
        analysisStatus: null,
        justification: null,
        details: null,
        verified: true,
        identificationSources: 'SBOM,Sonatype',
        latestPreviousAnnotation: {
          sbomVersion: '1.0',
          analysisStatus: 'exploitable',
          justification: 'requires_dependency',
          response: 'can_not_fix',
          detail: 'some details',
        },
      },
    ],
    sonatypeIdentifiedVulnerabilities: [
      {
        cvssScore: 9,
        issue: 'CVE-0863',
      },
    ],
    additionalVulnerabilities: [],
    policyViolationSummary: {
      severe: 1,
      critical: 3,
    },
  };

  const mockSbomPolicyViolationReport = {
    hash: 'd5c2005c9e3279201e12',
    componentIdentifier: {
      format: 'maven',
      coordinates: {
        artifactId: 'h2',
        classifier: '',
        extension: 'jar',
        groupId: 'com.h2database',
        version: '2.1.214',
      },
    },
    policyId: '769617ef5d174c30bb33127d71f18664',
    policyName: 'Security-High',
    policyThreatLevel: 9,
    activeViolations: [
      {
        policyId: '769617ef5d174c30bb33127d71f18664',
        policyViolationId: '347a469f98cd4ccf93924afb85b891cc',
        policyName: 'Security-High',
        policyThreatLevel: 9,
        waived: false,
        grandfathered: false,
        legacyViolation: false,
        constraintFactsJson:
          '[{"constraintId":"0f01fea7ccc646f2b373bad6a46db009","constraintName":"High risk CVSS score","operatorName":"AND","conditionFacts":[{"conditionTypeId":"SecurityVulnerabilitySeverity","conditionIndex":0,"summary":"Security Vulnerability Severity >= 7","reason":"Found security vulnerability sonatype-2022-6243 with severity >= 7 (severity = 8.4)","reference":{"value":"sonatype-2022-6243","type":"SECURITY_VULNERABILITY_REFID"},"triggerJson":"{\\"conditionIndex\\":0,\\"trigger\\":{\\"refId\\":\\"sonatype-2022-6243\\",\\"severity\\":8.4}}"},{"conditionTypeId":"SecurityVulnerabilitySeverity","conditionIndex":1,"summary":"Security Vulnerability Severity < 9","reason":"Found security vulnerability sonatype-2022-6243 with severity < 9 (severity = 8.4)","reference":{"value":"sonatype-2022-6243","type":"SECURITY_VULNERABILITY_REFID"},"triggerJson":"{\\"conditionIndex\\":1,\\"trigger\\":{\\"refId\\":\\"sonatype-2022-6243\\",\\"severity\\":8.4}}"}]}]',
        actions: [],
        constraints: [
          {
            constraintId: '0f01fea7ccc646f2b373bad6a46db009',
            constraintName: 'High risk CVSS score',
            constraintOperator: 'AND',
            conditions: [
              {
                conditionType: 'SecurityVulnerabilitySeverity',
                conditionSummary: 'Security Vulnerability Severity >= 7',
                conditionReason: 'Found security vulnerability sonatype-2022-6243 with severity >= 7 (severity = 8.4)',
                conditionTriggerReference: {
                  value: 'sonatype-2022-6243',
                  type: 'SECURITY_VULNERABILITY_REFID',
                },
              },
              {
                conditionType: 'SecurityVulnerabilitySeverity',
                conditionSummary: 'Security Vulnerability Severity < 9',
                conditionReason: 'Found security vulnerability sonatype-2022-6243 with severity < 9 (severity = 8.4)',
                conditionTriggerReference: {
                  value: 'sonatype-2022-6243',
                  type: 'SECURITY_VULNERABILITY_REFID',
                },
              },
            ],
          },
        ],
        policyThreatCategory: 'SECURITY',
      },
      {
        policyId: 'd9d531bb26924869b1ea80a357d6b11d',
        policyViolationId: 'af3f658dc0924f1aa94c48ea188f7c69',
        policyName: 'Architecture-Quality',
        policyThreatLevel: 1,
        waived: false,
        grandfathered: false,
        legacyViolation: false,
        constraintFactsJson:
          '[{"constraintId":"d0be8ceca9de45e599c8b690330493b5","constraintName":"Version is unpopular","operatorName":"OR","conditionFacts":[{"conditionTypeId":"RelativePopularity","conditionIndex":0,"summary":"Relative Popularity (Percentage) <= 10","reason":"Relative popularity was <= 10% (relative popularity = 0%)","reference":null,"triggerJson":null}]}]',
        actions: [],
        constraints: [
          {
            constraintId: 'd0be8ceca9de45e599c8b690330493b5',
            constraintName: 'Version is unpopular',
            constraintOperator: 'OR',
            conditions: [
              {
                conditionType: 'RelativePopularity',
                conditionSummary: 'Relative Popularity (Percentage) <= 10',
                conditionReason: 'Relative popularity was <= 10% (relative popularity = 0%)',
                conditionTriggerReference: null,
              },
            ],
          },
        ],
        policyThreatCategory: 'QUALITY',
      },
      {
        policyId: 'ee8ed41d2b2e4b3889841089648416f3',
        policyViolationId: '448d64a2adae4a5eafad9033ffbdeb23',
        policyName: 'Security-Medium',
        policyThreatLevel: 7,
        waived: false,
        grandfathered: false,
        legacyViolation: false,
        constraintFactsJson:
          '[{"constraintId":"e7471e76a3aa4e3a86e5f49655fee05c","constraintName":"Medium risk CVSS score","operatorName":"AND","conditionFacts":[{"conditionTypeId":"SecurityVulnerabilitySeverity","conditionIndex":0,"summary":"Security Vulnerability Severity >= 4","reason":"Found security vulnerability sonatype-2018-0863 with severity >= 4 (severity = 6.0)","reference":{"value":"sonatype-2018-0863","type":"SECURITY_VULNERABILITY_REFID"},"triggerJson":"{\\"conditionIndex\\":0,\\"trigger\\":{\\"refId\\":\\"sonatype-2018-0863\\",\\"severity\\":6.0}}"},{"conditionTypeId":"SecurityVulnerabilitySeverity","conditionIndex":1,"summary":"Security Vulnerability Severity < 7","reason":"Found security vulnerability sonatype-2018-0863 with severity < 7 (severity = 6.0)","reference":{"value":"sonatype-2018-0863","type":"SECURITY_VULNERABILITY_REFID"},"triggerJson":"{\\"conditionIndex\\":1,\\"trigger\\":{\\"refId\\":\\"sonatype-2018-0863\\",\\"severity\\":6.0}}"}]}]',
        actions: [],
        constraints: [
          {
            constraintId: 'e7471e76a3aa4e3a86e5f49655fee05c',
            constraintName: 'Medium risk CVSS score',
            constraintOperator: 'AND',
            conditions: [
              {
                conditionType: 'SecurityVulnerabilitySeverity',
                conditionSummary: 'Security Vulnerability Severity >= 4',
                conditionReason: 'Found security vulnerability sonatype-2018-0863 with severity >= 4 (severity = 6.0)',
                conditionTriggerReference: {
                  value: 'sonatype-2018-0863',
                  type: 'SECURITY_VULNERABILITY_REFID',
                },
              },
              {
                conditionType: 'SecurityVulnerabilitySeverity',
                conditionSummary: 'Security Vulnerability Severity < 7',
                conditionReason: 'Found security vulnerability sonatype-2018-0863 with severity < 7 (severity = 6.0)',
                conditionTriggerReference: {
                  value: 'sonatype-2018-0863',
                  type: 'SECURITY_VULNERABILITY_REFID',
                },
              },
            ],
          },
        ],
        policyThreatCategory: 'SECURITY',
      },
    ],
    waivedViolations: [],
    allViolations: [
      {
        policyId: '769617ef5d174c30bb33127d71f18664',
        policyViolationId: '347a469f98cd4ccf93924afb85b891cc',
        policyName: 'Security-High',
        policyThreatLevel: 9,
        waived: false,
        grandfathered: false,
        legacyViolation: false,
        constraintFactsJson:
          '[{"constraintId":"0f01fea7ccc646f2b373bad6a46db009","constraintName":"High risk CVSS score","operatorName":"AND","conditionFacts":[{"conditionTypeId":"SecurityVulnerabilitySeverity","conditionIndex":0,"summary":"Security Vulnerability Severity >= 7","reason":"Found security vulnerability sonatype-2022-6243 with severity >= 7 (severity = 8.4)","reference":{"value":"sonatype-2022-6243","type":"SECURITY_VULNERABILITY_REFID"},"triggerJson":"{\\"conditionIndex\\":0,\\"trigger\\":{\\"refId\\":\\"sonatype-2022-6243\\",\\"severity\\":8.4}}"},{"conditionTypeId":"SecurityVulnerabilitySeverity","conditionIndex":1,"summary":"Security Vulnerability Severity < 9","reason":"Found security vulnerability sonatype-2022-6243 with severity < 9 (severity = 8.4)","reference":{"value":"sonatype-2022-6243","type":"SECURITY_VULNERABILITY_REFID"},"triggerJson":"{\\"conditionIndex\\":1,\\"trigger\\":{\\"refId\\":\\"sonatype-2022-6243\\",\\"severity\\":8.4}}"}]}]',
        actions: [],
        constraints: [
          {
            constraintId: '0f01fea7ccc646f2b373bad6a46db009',
            constraintName: 'High risk CVSS score',
            constraintOperator: 'AND',
            conditions: [
              {
                conditionType: 'SecurityVulnerabilitySeverity',
                conditionSummary: 'Security Vulnerability Severity >= 7',
                conditionReason: 'Found security vulnerability sonatype-2022-6243 with severity >= 7 (severity = 8.4)',
                conditionTriggerReference: {
                  value: 'sonatype-2022-6243',
                  type: 'SECURITY_VULNERABILITY_REFID',
                },
              },
              {
                conditionType: 'SecurityVulnerabilitySeverity',
                conditionSummary: 'Security Vulnerability Severity < 9',
                conditionReason: 'Found security vulnerability sonatype-2022-6243 with severity < 9 (severity = 8.4)',
                conditionTriggerReference: {
                  value: 'sonatype-2022-6243',
                  type: 'SECURITY_VULNERABILITY_REFID',
                },
              },
            ],
          },
        ],
        policyThreatCategory: 'SECURITY',
      },
      {
        policyId: 'd9d531bb26924869b1ea80a357d6b11d',
        policyViolationId: 'af3f658dc0924f1aa94c48ea188f7c69',
        policyName: 'Architecture-Quality',
        policyThreatLevel: 1,
        waived: false,
        grandfathered: false,
        legacyViolation: false,
        constraintFactsJson:
          '[{"constraintId":"d0be8ceca9de45e599c8b690330493b5","constraintName":"Version is unpopular","operatorName":"OR","conditionFacts":[{"conditionTypeId":"RelativePopularity","conditionIndex":0,"summary":"Relative Popularity (Percentage) <= 10","reason":"Relative popularity was <= 10% (relative popularity = 0%)","reference":null,"triggerJson":null}]}]',
        actions: [],
        constraints: [
          {
            constraintId: 'd0be8ceca9de45e599c8b690330493b5',
            constraintName: 'Version is unpopular',
            constraintOperator: 'OR',
            conditions: [
              {
                conditionType: 'RelativePopularity',
                conditionSummary: 'Relative Popularity (Percentage) <= 10',
                conditionReason: 'Relative popularity was <= 10% (relative popularity = 0%)',
                conditionTriggerReference: null,
              },
            ],
          },
        ],
        policyThreatCategory: 'QUALITY',
      },
      {
        policyId: 'ee8ed41d2b2e4b3889841089648416f3',
        policyViolationId: '448d64a2adae4a5eafad9033ffbdeb23',
        policyName: 'Security-Medium',
        policyThreatLevel: 7,
        waived: false,
        grandfathered: false,
        legacyViolation: false,
        constraintFactsJson:
          '[{"constraintId":"e7471e76a3aa4e3a86e5f49655fee05c","constraintName":"Medium risk CVSS score","operatorName":"AND","conditionFacts":[{"conditionTypeId":"SecurityVulnerabilitySeverity","conditionIndex":0,"summary":"Security Vulnerability Severity >= 4","reason":"Found security vulnerability sonatype-2018-0863 with severity >= 4 (severity = 6.0)","reference":{"value":"sonatype-2018-0863","type":"SECURITY_VULNERABILITY_REFID"},"triggerJson":"{\\"conditionIndex\\":0,\\"trigger\\":{\\"refId\\":\\"sonatype-2018-0863\\",\\"severity\\":6.0}}"},{"conditionTypeId":"SecurityVulnerabilitySeverity","conditionIndex":1,"summary":"Security Vulnerability Severity < 7","reason":"Found security vulnerability sonatype-2018-0863 with severity < 7 (severity = 6.0)","reference":{"value":"sonatype-2018-0863","type":"SECURITY_VULNERABILITY_REFID"},"triggerJson":"{\\"conditionIndex\\":1,\\"trigger\\":{\\"refId\\":\\"sonatype-2018-0863\\",\\"severity\\":6.0}}"}]}]',
        actions: [],
        constraints: [
          {
            constraintId: 'e7471e76a3aa4e3a86e5f49655fee05c',
            constraintName: 'Medium risk CVSS score',
            constraintOperator: 'AND',
            conditions: [
              {
                conditionType: 'SecurityVulnerabilitySeverity',
                conditionSummary: 'Security Vulnerability Severity >= 4',
                conditionReason: 'Found security vulnerability sonatype-2018-0863 with severity >= 4 (severity = 6.0)',
                conditionTriggerReference: {
                  value: 'sonatype-2018-0863',
                  type: 'SECURITY_VULNERABILITY_REFID',
                },
              },
              {
                conditionType: 'SecurityVulnerabilitySeverity',
                conditionSummary: 'Security Vulnerability Severity < 7',
                conditionReason: 'Found security vulnerability sonatype-2018-0863 with severity < 7 (severity = 6.0)',
                conditionTriggerReference: {
                  value: 'sonatype-2018-0863',
                  type: 'SECURITY_VULNERABILITY_REFID',
                },
              },
            ],
          },
        ],
        policyThreatCategory: 'SECURITY',
      },
    ],
  };

  const openDeleteAnnotationModal = async () => {
    renderPage();

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    const vulnerabilityRows = screen.getAllByRole('row');
    const dropdownFirstRow = within(vulnerabilityRows[1]).getByRole('button');
    fireEvent.click(dropdownFirstRow);
    const deleteButton = screen.getByRole('button', { name: 'Delete Annotation' });
    expect(deleteButton).toBeVisible();
    fireEvent.click(deleteButton);

    const dialog = screen.queryByRole('dialog');
    expect(dialog).toBeInTheDocument();
    const header = within(dialog).getByRole('heading', { level: 2 });
    expect(header).toHaveTextContent('Delete annotation for sonatype-2018-0863');
    const body = within(dialog).getByText(
      'Are you sure you want to delete "Resolved" annotation for sonatype-2018-0863?'
    );
    expect(body).toBeInTheDocument();

    return dialog;
  };

  const openCopyAnnotationModal = async () => {
    renderPage();

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    const vulnerabilityRows = screen.getAllByRole('row');
    const dropdownFirstRow = within(vulnerabilityRows[2]).getByRole('button');
    fireEvent.click(dropdownFirstRow);
    const cooyButton = screen.getByRole('button', { name: 'Copy Annotation' });
    expect(cooyButton).toBeVisible();
    fireEvent.click(cooyButton);

    const dialog = screen.queryByRole('dialog');
    expect(dialog).toBeInTheDocument();
    const header = within(dialog).getByRole('heading', { level: 2 });
    expect(header).toHaveTextContent('Copy annotation for sonatype-2018-9999');
    const body = within(dialog).getByText(
      'Are you sure you want to copy "Exploitable" annotation for sonatype-2018-9999 from previous version 1.0?'
    );
    expect(body).toBeInTheDocument();

    return dialog;
  };

  const getVexDialog = async () => {
    let dialog;
    await waitFor(async () => {
      // There are 2 dialogs, one for the vulnerability details and one for the VEX annotations
      dialog = screen.getAllByRole('dialog', { hidden: true })[1];
      expect(dialog).toBeInTheDocument();
      await fireEvent.animationEnd(dialog);
    });

    return dialog;
  };

  beforeAll(() => setupPortalContainer());

  beforeEach(() => {
    const preloadedState = {
      productFeatures: {
        productFeatures: {
          'sbom-manager': true,
          'sbom-policies': true,
          loading: true,
        },
      },
      router: {
        currentState: { name: 'sbomManager.component' },
        currentParams: {
          applicationPublicId,
          versionId: sbomVersion,
          sbomVersion,
          componentHash,
        },
      },
      sbomComponentDetailsPage: {
        loading: true,
        loadError: null,
        publicAppId: null,
        componentDetails: null,
        activeTabIndex: 0,

        disclosedVulnerabilitiesSortConfiguration: { ...defaultSortConfiguration },
        additionalVulnerabilitiesSortConfiguration: { ...defaultSortConfiguration },

        vulnerabilityAnalysisReferenceData,
        policyViolationDetailsDrawer: { ...policyViolationDetailsDrawerInitialState },
        sbomPolicyViolations: { ...sbomPolicyViolationsInitialState },
        componentDetailsPaginationData: {
          pagination: { currentPage: 1, pageCount: 3 },
          pagesData: {
            0: [{ hash: 'componentHash1' }, { hash: 'componentHash2' }, { hash: 'componentHash3' }],
            1: [{ hash: 'componentHash4' }, { hash: 'componentHash' }, { hash: 'componentHash6' }],
            2: [{ hash: 'componentHash7' }, { hash: 'componentHash8' }],
          },
          totalNumberOfComponents: 102,
        },
      },
    };
    renderPage = (additionalPreloadedState = {}) =>
      render(<ComponentDetailsPage />, { preloadedState: { ...preloadedState, ...additionalPreloadedState } });
  });

  describe('Page Content', () => {
    it('should render page content successfully', async () => {
      axiosMock
        .onGet(getApplicationSummaryUrl(applicationPublicId))
        .reply(200, { id: applicationInternalId, name: 'test-app' });
      axiosMock
        .onGet(getSbomComponentDetailsUrl(applicationInternalId, sbomVersion, componentHash))
        .reply(200, mockComponentDetails);
      axiosMock
        .onGet(getSbomPolicyViolationReportUrl(applicationInternalId, sbomVersion, fileCoordinateId))
        .reply(200, mockSbomPolicyViolationReport);

      renderPage();

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());
      expect(await screen.findByText(mockComponentDetails.displayName)).toBeVisible();
      expect(screen.getByText(mockComponentDetails.metadata.organizationName)).toBeVisible();
      expect(screen.getByText(mockComponentDetails.metadata.applicationName)).toBeVisible();
      expect(screen.getByText('Maven')).toBeVisible();
      expect(screen.getByText('Direct Dependency')).toBeVisible();
      expect(screen.getByText('pkg:maven/net.sf.jason/jason-schema@1.2.11')).toBeVisible();
      expect(screen.getByText('Component Summary')).toBeVisible();
      expect(screen.getByText('Highest CVSS Score')).toBeVisible();
      expect(await screen.findByText('Match State: Similar')).toBeVisible();

      const highestCvssScoreContainer = await screen.findByTestId('highestCvssScore');
      expect(highestCvssScoreContainer).toBeInTheDocument();
      expect(highestCvssScoreContainer.textContent).toEqual(
        mockComponentDetails.vulnerabilitySummary.highestCvssScore.toString()
      );

      expect(screen.getByText('Vulnerabilities Verified')).toBeVisible();
      const verifiedContainer = await screen.findByTestId('verified');
      expect(verifiedContainer).toBeInTheDocument();
      expect(verifiedContainer.textContent).toEqual(
        mockComponentDetails.vulnerabilitySummary.verifiedVulnerabilitiesCount + ' Sonatype Verified'
      );

      const unverifiedContainer = await screen.findByTestId('unverified');
      expect(unverifiedContainer).toBeInTheDocument();
      expect(unverifiedContainer.textContent).toEqual(
        mockComponentDetails.vulnerabilitySummary.unverifiedVulnerabilitiesCount + ' Unverified'
      );

      expect(screen.getByText('Disclosed Vulnerabilities')).toBeVisible();
      expect(screen.getByText('Additional Sonatype Identified Vulnerabilities')).toBeVisible();
      expect(
        screen.getByText(
          'Additional vulnerabilities in this SBOM, detected by Sonatype vulnerability detection system.'
        )
      ).toBeVisible();
      const tableRows = await screen.findAllByRole('row');
      expect(tableRows.length).toBe(5); // Including the header
      const link = screen.getByText('sonatype-2018-0863');
      fireEvent.click(link);

      expect(screen.getByRole('complementary')).toBeInTheDocument();
      expect(screen.getByRole('complementary')).toHaveTextContent('Vulnerability Details sonatype-2018-0863');

      const previousComponentLink = (await screen.findByText('Previous Component')).closest('a');
      expect(previousComponentLink).toBeVisible();

      const nextComponentLink = (await screen.findByText('Next Component')).closest('a');
      expect(nextComponentLink).toBeVisible();
      expect(screen.getByText('52 of 102')).toBeVisible();
    });

    it('should render correct tabs', async () => {
      axiosMock
        .onGet(getApplicationSummaryUrl(applicationPublicId))
        .reply(200, { id: applicationInternalId, name: 'test-app' });
      axiosMock
        .onGet(getSbomComponentDetailsUrl(applicationInternalId, sbomVersion, componentHash))
        .reply(200, mockComponentDetails);
      axiosMock
        .onGet(getSbomPolicyViolationReportUrl(applicationInternalId, sbomVersion, fileCoordinateId, componentHash))
        .reply(200, mockSbomPolicyViolationReport);

      renderPage();

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      const tabs = screen.getAllByRole('tab');
      expect(tabs.length).toBe(3);
      expect(tabs[0]).toHaveTextContent('Vulnerability');
      expect(tabs[1]).toHaveTextContent('Policy Violations');
      expect(tabs[2]).toHaveTextContent('Original BOM');
    });

    it('should render the tooltip on hovering over the copy icon', async () => {
      axiosMock
        .onGet(getApplicationSummaryUrl(applicationPublicId))
        .reply(200, { id: applicationInternalId, name: 'test-app' });
      axiosMock
        .onGet(getSbomComponentDetailsUrl(applicationInternalId, sbomVersion, componentHash))
        .reply(200, mockComponentDetails);
      axiosMock
        .onGet(getSbomPolicyViolationReportUrl(applicationInternalId, sbomVersion, fileCoordinateId, componentHash))
        .reply(200, mockSbomPolicyViolationReport);
      renderPage();
      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      const copyIconContainer = await screen.findByTestId('copyIconContainer');
      expect(copyIconContainer).toBeInTheDocument();

      fireEvent.mouseOver(copyIconContainer);
      const tooltip = await screen.findByRole('tooltip');
      expect(tooltip).toHaveTextContent('Copy PackageURL to clipboard');
    });

    it('and it should change the text from the tooltip when clicking the copy icon', async () => {
      const user = userEvent.setup();

      axiosMock
        .onGet(getApplicationSummaryUrl(applicationPublicId))
        .reply(200, { id: applicationInternalId, name: 'test-app' });
      axiosMock
        .onGet(getSbomComponentDetailsUrl(applicationInternalId, sbomVersion, componentHash))
        .reply(200, mockComponentDetails);
      axiosMock
        .onGet(getSbomPolicyViolationReportUrl(applicationInternalId, sbomVersion, fileCoordinateId, componentHash))
        .reply(200, mockSbomPolicyViolationReport);
      renderPage();
      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      const copyIconContainer = await screen.findByTestId('copyIconContainer');
      expect(copyIconContainer).toBeInTheDocument();

      await user.click(copyIconContainer);
      const tooltip = await screen.findByRole('tooltip');
      expect(tooltip).toHaveTextContent('Copied');
    });

    it('and it should change the text back after 2 seconds', async () => {
      const user = userEvent.setup();

      axiosMock
        .onGet(getApplicationSummaryUrl(applicationPublicId))
        .reply(200, { id: applicationInternalId, name: 'test-app' });
      axiosMock
        .onGet(getSbomComponentDetailsUrl(applicationInternalId, sbomVersion, componentHash))
        .reply(200, mockComponentDetails);
      axiosMock
        .onGet(getSbomPolicyViolationReportUrl(applicationInternalId, sbomVersion, fileCoordinateId, componentHash))
        .reply(200, mockSbomPolicyViolationReport);
      renderPage();
      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      const copyIconContainer = await screen.findByTestId('copyIconContainer');
      expect(copyIconContainer).toBeInTheDocument();

      await user.click(copyIconContainer);
      let tooltip = await screen.findByRole('tooltip');
      expect(tooltip).toHaveTextContent('Copied');

      await waitFor(() => expect(tooltip).toHaveTextContent('Copy PackageURL to clipboard'), { timeout: 3000 });
    });
  });

  it('should close Vulnerability details drawer when close button is clicked', async () => {
    axiosMock
      .onGet(getApplicationSummaryUrl(applicationPublicId))
      .reply(200, { id: applicationInternalId, name: 'test-app' });
    axiosMock
      .onGet(getSbomComponentDetailsUrl(applicationInternalId, sbomVersion, componentHash))
      .reply(200, mockComponentDetails);
    axiosMock
      .onGet(getSbomPolicyViolationReportUrl(applicationInternalId, sbomVersion, fileCoordinateId, componentHash))
      .reply(200, mockSbomPolicyViolationReport);
    const { container } = renderPage();

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    const link = screen.getByText('sonatype-2018-0863');
    fireEvent.click(link);

    const vulnerabilityDetailsPopover = container.querySelector('aside');
    expect(screen.queryByRole('complementary')).toBeInTheDocument();
    const closeButton = vulnerabilityDetailsPopover.querySelector('button');
    expect(closeButton).toBeInTheDocument();
    fireEvent.click(closeButton);
    expect(screen.queryByRole('complementary')).not.toBeInTheDocument();
  });

  it('should close Vex Annotation drawer when its close button is clicked', async () => {
    axiosMock
      .onGet(getApplicationSummaryUrl(applicationPublicId))
      .reply(200, { id: applicationInternalId, name: 'test-app' });
    axiosMock
      .onGet(getSbomComponentDetailsUrl(applicationInternalId, sbomVersion, componentHash))
      .reply(200, mockComponentDetails);

    axiosMock.onGet(getSbomVulnerabibilityAnalysisReferenceData()).reply(200, vulnerabilityAnalysisReferenceData);
    axiosMock
      .onGet(getSbomPolicyViolationReportUrl(applicationInternalId, sbomVersion, fileCoordinateId, componentHash))
      .reply(200, mockSbomPolicyViolationReport);

    renderPage();

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    const vulnerabilityRows = screen.getAllByRole('row');
    const dropdownFirstRow = within(vulnerabilityRows[1]).getByRole('button');
    fireEvent.click(dropdownFirstRow);
    const editButton = screen.getByRole('button', { name: 'Edit Annotation' });
    expect(editButton).toBeVisible();
    fireEvent.click(editButton);

    const dialog = await getVexDialog();
    expect(dialog).toHaveTextContent('Annotate sonatype-2018-0863');
    expect(dialog).toHaveTextContent('Unreachable code');
    const closeButton = within(dialog).getByRole('button', { name: 'Close' });
    expect(closeButton).toBeInTheDocument();
    fireEvent.click(closeButton);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('should open Vex annotation drawer and save form successfully', async () => {
    axiosMock
      .onGet(getApplicationSummaryUrl(applicationPublicId))
      .reply(200, { id: applicationInternalId, name: 'test-app' });
    axiosMock
      .onGet(getSbomComponentDetailsUrl(applicationInternalId, sbomVersion, componentHash))
      .reply(200, mockComponentDetails);

    axiosMock.onGet(getSbomVulnerabibilityAnalysisReferenceData()).reply(200, vulnerabilityAnalysisReferenceData);
    axiosMock
      .onPut(getSbomVulnerabilityAnnotationUrl(applicationInternalId, sbomVersion, 'sonatype-2018-0863'))
      .reply(200, {});
    axiosMock
      .onGet(getSbomPolicyViolationReportUrl(applicationInternalId, sbomVersion, fileCoordinateId, componentHash))
      .reply(200, mockSbomPolicyViolationReport);

    renderPage();

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    const vulnerabilityRows = screen.getAllByRole('row');
    const dropdownFirstRow = within(vulnerabilityRows[1]).getByRole('button');
    fireEvent.click(dropdownFirstRow);
    const editButton = screen.getByRole('button', { name: 'Edit Annotation' });
    expect(editButton).toBeVisible();
    fireEvent.click(editButton);

    const dialog = await getVexDialog();
    expect(dialog).toHaveTextContent('Annotate sonatype-2018-0863');

    const saveButton = within(dialog).getByRole('button', { name: 'Update' });
    expect(saveButton).toBeInTheDocument();
    fireEvent.click(saveButton);
    await waitFor(() => expect(screen.getByText(/Success/)).toBeInTheDocument());
  });

  describe('should open Delete Annotation modal', () => {
    it('and should close it when cancel', async () => {
      axiosMock
        .onGet(getApplicationSummaryUrl(applicationPublicId))
        .reply(200, { id: applicationInternalId, name: 'test-app' });
      axiosMock
        .onGet(getSbomComponentDetailsUrl(applicationInternalId, sbomVersion, componentHash))
        .reply(200, mockComponentDetails);

      axiosMock.onGet(getSbomVulnerabibilityAnalysisReferenceData()).reply(200, vulnerabilityAnalysisReferenceData);
      axiosMock
        .onPut(getSbomVulnerabilityAnnotationUrl(applicationInternalId, sbomVersion, 'sonatype-2018-0863'))
        .reply(200, {});
      axiosMock
        .onGet(getSbomPolicyViolationReportUrl(applicationInternalId, sbomVersion, fileCoordinateId, componentHash))
        .reply(200, mockSbomPolicyViolationReport);

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      const dialog = await openDeleteAnnotationModal();
      const cancelButton = queryByText(dialog.querySelector('.nx-form__cancel-btn'), 'Cancel');
      expect(cancelButton).toBeInTheDocument();
      fireEvent.click(cancelButton);
      await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    });

    it('and should delete data when submit', async () => {
      axiosMock
        .onGet(getApplicationSummaryUrl(applicationPublicId))
        .reply(200, { id: applicationInternalId, name: 'test-app' });
      axiosMock
        .onGet(getSbomComponentDetailsUrl(applicationInternalId, sbomVersion, componentHash))
        .reply(200, mockComponentDetails);

      axiosMock.onGet(getSbomVulnerabibilityAnalysisReferenceData()).reply(200, vulnerabilityAnalysisReferenceData);
      axiosMock
        .onDelete(getSbomVulnerabilityAnnotationUrl(applicationInternalId, sbomVersion, 'sonatype-2018-0863'))
        .reply(200, {});
      axiosMock
        .onGet(getSbomPolicyViolationReportUrl(applicationInternalId, sbomVersion, fileCoordinateId, componentHash))
        .reply(200, mockSbomPolicyViolationReport);

      const dialog = await openDeleteAnnotationModal();
      const submitButton = queryByText(dialog.querySelector('.nx-form__submit-btn'), 'Delete');
      expect(submitButton).toBeInTheDocument();
      fireEvent.click(submitButton);
      await waitFor(() => expect(screen.getByText(/Success/)).toBeInTheDocument());
    });

    it('and should display retry button when errors', async () => {
      axiosMock
        .onGet(getApplicationSummaryUrl(applicationPublicId))
        .reply(200, { id: applicationInternalId, name: 'test-app' });
      axiosMock
        .onGet(getSbomComponentDetailsUrl(applicationInternalId, sbomVersion, componentHash))
        .reply(200, mockComponentDetails);
      axiosMock
        .onGet(getSbomPolicyViolationReportUrl(applicationInternalId, sbomVersion, fileCoordinateId, componentHash))
        .reply(200, mockSbomPolicyViolationReport);

      axiosMock.onGet(getSbomVulnerabibilityAnalysisReferenceData()).reply(200, vulnerabilityAnalysisReferenceData);
      axiosMock
        .onDelete(getSbomVulnerabilityAnnotationUrl(applicationInternalId, sbomVersion, 'sonatype-2018-0863'))
        .reply(500, {});

      const dialog = await openDeleteAnnotationModal();
      const submitButton = queryByText(dialog.querySelector('.nx-form__submit-btn'), 'Delete');
      expect(submitButton).toBeInTheDocument();
      fireEvent.click(submitButton);
      await waitFor(() =>
        expect(
          screen.getByText(/An error occurred saving data. Request failed with status code 500/)
        ).toBeInTheDocument()
      );
    });
  });

  describe('should open Copy Annotation modal', () => {
    it('and should close it when cancel', async () => {
      axiosMock
        .onGet(getApplicationSummaryUrl(applicationPublicId))
        .reply(200, { id: applicationInternalId, name: 'test-app' });
      axiosMock
        .onGet(getSbomComponentDetailsUrl(applicationInternalId, sbomVersion, componentHash))
        .reply(200, mockComponentDetails);
      axiosMock
        .onGet(getSbomPolicyViolationReportUrl(applicationInternalId, sbomVersion, fileCoordinateId, componentHash))
        .reply(200, mockSbomPolicyViolationReport);
      axiosMock.onGet(getSbomVulnerabibilityAnalysisReferenceData()).reply(200, vulnerabilityAnalysisReferenceData);
      axiosMock
        .onPut(getSbomVulnerabilityAnnotationUrl(applicationInternalId, sbomVersion, 'sonatype-2018-0863'))
        .reply(200, {});
      const dialog = await openCopyAnnotationModal();
      const cancelButton = queryByText(dialog.querySelector('.nx-form__cancel-btn'), 'Cancel');
      expect(cancelButton).toBeInTheDocument();
      fireEvent.click(cancelButton);
      await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    });

    it('and should copy data when submit', async () => {
      axiosMock
        .onGet(getApplicationSummaryUrl(applicationPublicId))
        .reply(200, { id: applicationInternalId, name: 'test-app' });
      axiosMock
        .onGet(getSbomComponentDetailsUrl(applicationInternalId, sbomVersion, componentHash))
        .reply(200, mockComponentDetails);
      axiosMock
        .onGet(getSbomPolicyViolationReportUrl(applicationInternalId, sbomVersion, fileCoordinateId, componentHash))
        .reply(200, mockSbomPolicyViolationReport);
      axiosMock.onGet(getSbomVulnerabibilityAnalysisReferenceData()).reply(200, vulnerabilityAnalysisReferenceData);
      axiosMock
        .onPut(getSbomVulnerabilityAnnotationUrl(applicationInternalId, sbomVersion, 'sonatype-2018-9999'))
        .reply(200, {});

      const dialog = await openCopyAnnotationModal();
      const submitButton = queryByText(dialog.querySelector('.nx-form__submit-btn'), 'Copy');
      expect(submitButton).toBeInTheDocument();
      fireEvent.click(submitButton);
      await waitFor(() => expect(screen.getByText(/Success/)).toBeInTheDocument());
    });

    it('and should display retry button when errors', async () => {
      axiosMock
        .onGet(getApplicationSummaryUrl(applicationPublicId))
        .reply(200, { id: applicationInternalId, name: 'test-app' });
      axiosMock
        .onGet(getSbomComponentDetailsUrl(applicationInternalId, sbomVersion, componentHash))
        .reply(200, mockComponentDetails);
      axiosMock
        .onGet(getSbomPolicyViolationReportUrl(applicationInternalId, sbomVersion, fileCoordinateId, componentHash))
        .reply(200, mockSbomPolicyViolationReport);
      axiosMock.onGet(getSbomVulnerabibilityAnalysisReferenceData()).reply(200, vulnerabilityAnalysisReferenceData);
      axiosMock
        .onPut(getSbomVulnerabilityAnnotationUrl(applicationInternalId, sbomVersion, 'sonatype-2018-9999'))
        .reply(500, {});

      const dialog = await openCopyAnnotationModal();
      const submitButton = queryByText(dialog.querySelector('.nx-form__submit-btn'), 'Copy');
      expect(submitButton).toBeInTheDocument();
      fireEvent.click(submitButton);
      await waitFor(() =>
        expect(
          screen.getByText(/An error occurred saving data. Request failed with status code 500/)
        ).toBeInTheDocument()
      );
    });
  });

  it('should open Vex Annotation drawer and display error message when failed loading analysis reference data', async () => {
    axiosMock
      .onGet(getApplicationSummaryUrl(applicationPublicId))
      .reply(200, { id: applicationInternalId, name: 'test-app' });
    axiosMock
      .onGet(getSbomComponentDetailsUrl(applicationInternalId, sbomVersion, componentHash))
      .reply(200, mockComponentDetails);
    axiosMock
      .onGet(getSbomPolicyViolationReportUrl(applicationInternalId, sbomVersion, fileCoordinateId, componentHash))
      .reply(200, mockSbomPolicyViolationReport);

    renderPage();

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    const vulnerabilityRows = screen.getAllByRole('row');
    const dropdownFirstRow = within(vulnerabilityRows[1]).getByRole('button');
    fireEvent.click(dropdownFirstRow);
    const editButton = screen.getByRole('button', { name: 'Add Annotation' });
    expect(editButton).toBeVisible();
    fireEvent.click(editButton);

    const dialog = await getVexDialog();

    expect(dialog).toHaveTextContent('Annotate sonatype-2018-0863');
    expect(dialog).toHaveTextContent(/An error occurred loading data./);

    // 2 retry buttons, 1 in the dialog content and 1 in the footer
    expect(within(dialog).getAllByRole('button', { name: 'Retry' })).toHaveLength(2);
  });

  it('shows error when the SBOM Manager license is disabled', async () => {
    renderPage({
      productFeatures: {
        productFeatures: {
          loading: false,
        },
      },
    });

    const errorMessage = await screen.findByText(
      'An error occurred loading data. The SBOM Manager license feature is not enabled.'
    );
    expect(errorMessage).toBeVisible();
  });

  it('shows error when there is an error loading data', async () => {
    axiosMock
      .onGet(getApplicationSummaryUrl(applicationPublicId))
      .reply(200, { id: applicationInternalId, name: 'test-app' });
    axiosMock
      .onGet(getSbomComponentDetailsUrl(applicationInternalId, sbomVersion, componentHash))
      .reply(500, 'There was an error');
    axiosMock
      .onGet(getSbomPolicyViolationReportUrl(applicationInternalId, sbomVersion, fileCoordinateId, componentHash))
      .reply(200, mockSbomPolicyViolationReport);

    renderPage();

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    const errorMessage = await screen.findByText('An error occurred loading data. There was an error');
    expect(errorMessage).toBeVisible();

    const retryButton = await screen.getByRole('button');
    expect(retryButton).toBeVisible();
  });

  describe('SBOM Policies not supported', () => {
    it('should not show policy violations tab', async () => {
      axiosMock
        .onGet(getApplicationSummaryUrl(applicationPublicId))
        .reply(200, { id: applicationInternalId, name: 'test-app' });
      axiosMock
        .onGet(getSbomComponentDetailsUrl(applicationInternalId, sbomVersion, componentHash))
        .reply(200, mockComponentDetails);

      renderPage({
        productFeatures: {
          productFeatures: {
            'sbom-manager': true,
            loading: true,
          },
        },
      });

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      const tabs = screen.getAllByRole('tab');
      expect(tabs.length).toBe(2);
      expect(tabs[0]).toHaveTextContent('Vulnerability');
      expect(tabs[1]).toHaveTextContent('Original BOM');
    });
  });
});
