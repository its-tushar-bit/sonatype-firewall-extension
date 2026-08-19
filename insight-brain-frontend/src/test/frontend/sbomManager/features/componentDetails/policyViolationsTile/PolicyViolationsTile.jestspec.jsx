/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { axiosMockAdapter, screen, render, waitFor, within } from 'TestRoot/SpecUtil';
import { getSbomPolicyViolationReportUrl } from 'MainRoot/util/CLMLocation';

import PolicyViolationsTile from 'MainRoot/sbomManager/features/componentDetails/policyViolationsTile/PolicyViolationsTile';

describe('PolicyViolationsTile', () => {
  let renderTile;

  const axiosMock = axiosMockAdapter();

  const APPLICATION_PUBLIC_ID = 'APPLICATION-PUBLIC-ID';
  const SBOM_VERSION = 'SBOM-VERSION';
  const FILE_COORDINATE_ID = 'FILE_COORDINATE_ID';
  const COMPONENT_REF = 'COMPONENT_REF';

  const componentProps = Object.freeze({
    applicationPublicId: APPLICATION_PUBLIC_ID,
    sbomVersion: SBOM_VERSION,
  });

  const mockPolicyWithNoViolationsResponse = Object.freeze({
    hash: 'POLICY-HASH',
    componentIdentifier: null,
    policyId: 'POLICY-ID',
    policyName: 'POLICY-NAME',
    policyThreatLevel: 1,
    waivedViolations: [],
    allViolations: [],
  });

  const mockPolicyResponse = Object.freeze({
    hash: 'POLICY-HASH',
    componentIdentifier: null,
    policyId: 'POLICY-ID',
    policyName: 'POLICY-NAME',
    policyThreatLevel: 1,
    waivedViolations: [],
    allViolations: [
      {
        policyId: 'POLICY-ID-1',
        policyViolationId: 'POLICY-VIOLATION-ID-1',
        policyName: 'POLICY-NAME-1',
        policyThreatLevel: 7,
        waived: false,
        grandfathered: false,
        constraintFactsJson:
          '[{"constraintId":"606ade47a9524fb493b285380c8879c4","constraintName":"Unknown 3rd party component","operatorName":"AND","conditionFacts":[{"conditionTypeId":"MatchState","conditionIndex":0,"summary":"Match State is unknown","reason":"Match state was \'Unknown\'","reference":null,"triggerJson":null},{"conditionTypeId":"Proprietary","conditionIndex":1,"summary":"Proprietary is false","reason":"Component does not contain proprietary packages","reference":null,"triggerJson":null},{"conditionTypeId":"DataSource","conditionIndex":2,"summary":"Data Source has support for identity","reason":"Data Source has support for Identity","reference":null,"triggerJson":null}]}]',
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
        policyThreatCategory: 'OTHER',
      },
      {
        policyId: 'POLICY-ID-2',
        policyViolationId: 'POLICY-VIOLATION-ID-2',
        policyName: 'POLICY-NAME-2',
        policyThreatLevel: 9,
        waived: false,
        grandfathered: false,
        constraintFactsJson:
          '[{"constraintId":"606ade47a9524fb493b285380c8879c4","constraintName":"Unknown 3rd party component","operatorName":"AND","conditionFacts":[{"conditionTypeId":"MatchState","conditionIndex":0,"summary":"Match State is unknown","reason":"Match state was \'Unknown\'","reference":null,"triggerJson":null},{"conditionTypeId":"Proprietary","conditionIndex":1,"summary":"Proprietary is false","reason":"Component does not contain proprietary packages","reference":null,"triggerJson":null},{"conditionTypeId":"DataSource","conditionIndex":2,"summary":"Data Source has support for identity","reason":"Data Source has support for Identity","reference":null,"triggerJson":null}]}]',
        actions: [],
        constraints: [
          {
            constraintId: '606ade47a9524fb493b285380c8879c4',
            constraintName: 'Unknown 3rd party component',
            constraintOperator: 'AND',
            conditions: [
              {
                conditionType: 'MatchState',
                conditionSummary: 'Match State is unknown',
                conditionReason: "Match state was 'Unknown'",
                conditionTriggerReference: null,
              },
              {
                conditionType: 'Proprietary',
                conditionSummary: 'Proprietary is false',
                conditionReason: 'Component does not contain proprietary packages',
                conditionTriggerReference: null,
              },
              {
                conditionType: 'DataSource',
                conditionSummary: 'Data Source has support for identity',
                conditionReason: 'Data Source has support for Identity',
                conditionTriggerReference: null,
              },
            ],
          },
        ],
        policyThreatCategory: 'OTHER',
      },
    ],
  });

  const mockState = Object.freeze({
    productFeatures: {
      productFeatures: {
        'sbom-manager': true,
        'sbom-policies': true,
        loading: false,
      },
    },
    router: {
      currentState: { name: 'sbomManager.component' },
      currentParams: {
        applicationPublicId: APPLICATION_PUBLIC_ID,
        versionId: SBOM_VERSION,
        sbomVersion: SBOM_VERSION,
      },
    },

    sbomComponentDetailsPage: {
      componentDetails: {
        fileCoordinateId: FILE_COORDINATE_ID,
        componentRef: COMPONENT_REF,
      },
      sbomPolicyViolations: {
        loading: true,
        error: null,
        policy: null,
      },
    },
  });

  beforeEach(() => {
    renderTile = () => render(<PolicyViolationsTile {...componentProps} />, { preloadedState: { ...mockState } });
  });

  it('renders the correct violations in order', async () => {
    axiosMock
      .onGet(getSbomPolicyViolationReportUrl(APPLICATION_PUBLIC_ID, SBOM_VERSION, COMPONENT_REF, FILE_COORDINATE_ID))
      .reply(200, mockPolicyResponse);

    renderTile();

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    expect(await screen.findByText('Policy Violations')).toBeVisible();

    const tableRows = await screen.findAllByRole('row');

    // +1 including the header
    expect(tableRows.length).toBe(3);

    const headerRow = tableRows[0];
    const headerRowCells = within(headerRow).getAllByRole('columnheader');
    expect(headerRowCells[0]).toHaveTextContent('Threat');
    expect(headerRowCells[1]).toHaveTextContent('Policy');
    expect(headerRowCells[2]).toHaveTextContent('Constraint Name');
    expect(headerRowCells[3]).toHaveTextContent('Condition');

    const firstRow = tableRows[1];
    const firstRowCells = within(firstRow).getAllByRole('cell');
    expect(firstRowCells[0]).toHaveTextContent('9');
    expect(firstRowCells[1]).toHaveTextContent('POLICY-NAME-2');
    expect(firstRowCells[2]).toHaveTextContent('Unknown 3rd party component');
    expect(firstRowCells[3]).toHaveTextContent(
      'Component does not contain proprietary packagesData Source has support for Identity'
    );

    const secondRow = tableRows[2];
    const secondRowCells = within(secondRow).getAllByRole('cell');
    expect(secondRowCells[0]).toHaveTextContent('7');
    expect(secondRowCells[1]).toHaveTextContent('POLICY-NAME-1');
    expect(secondRowCells[2]).toHaveTextContent('High risk CVSS score');
    expect(secondRowCells[3]).toHaveTextContent(
      'Found security vulnerability sonatype-2022-6243 with severity >= 7 (severity = 8.4)Found security vulnerability sonatype-2022-6243 with severity < 9 (severity = 8.4)'
    );
  });

  it('renders correct empty content', async () => {
    axiosMock
      .onGet(getSbomPolicyViolationReportUrl(APPLICATION_PUBLIC_ID, SBOM_VERSION, COMPONENT_REF, FILE_COORDINATE_ID))
      .reply(200, mockPolicyWithNoViolationsResponse);

    renderTile();

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    expect(await screen.findByText('Policy Violations')).toBeVisible();

    expect(screen.getByText('No policy violations')).toBeVisible();
  });

  it('renders error', async () => {
    axiosMock
      .onGet(getSbomPolicyViolationReportUrl(APPLICATION_PUBLIC_ID, SBOM_VERSION, COMPONENT_REF, FILE_COORDINATE_ID))
      .reply(500, {});

    renderTile();

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    expect(screen.getByText('An error occurred loading data. Error')).toBeVisible();
  });
});
