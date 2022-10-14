/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, axiosMockAdapter, waitFor, fireEvent } from 'TestRoot/SpecUtil';
import FirewallLegalTab from 'MainRoot/firewall/firewallComponentDetailsPage/legal/FirewallLegalTab';
import {
  getComponentMultiLicensesUrl,
  getLicensesWithSyntheticFilterUrl,
  getLicenseOverrideUrl,
  getComponentPolicyViolationsUrl,
  getComponentDetailsUrl,
  getOrganizationsUrl,
} from 'MainRoot/util/CLMLocation';
import { initialState } from 'MainRoot/firewall/firewallReducer';

describe('FirewallLegalTab', () => {
  let mock,
    clientType = 'ci',
    ownerType = 'repository',
    ownerId = '603ac500381f48cba8433df1bc916991',
    componentIdentifier =
      '{"format":"maven","coordinates":{"artifactId":"ant","classifier":"","extension":"jar","groupId":"ant","version":"1.6"}}',
    hash = '7a3c2521ae0c6f53e044',
    matchState = 'exact',
    proprietary = 'propietary',
    pathname = 'ant/ant/1.6/ant-1.6.jar';

  let preloadedState = {
    firewall: {
      componentDetailsPage: {
        ...initialState.componentDetailsPage,
        componentDetails: {
          ...initialState.componentDetailsPage.componentDetails,
          displayName: {
            parts: [
              { field: 'Group', value: 'ant' },
              { value: ' : ' },
              { field: 'Artifact', value: 'ant' },
              { value: ' : ' },
              { field: 'Version', value: '1.6' },
            ],
            name: 'ant',
          },
        },
        policyExistingWaivers: {
          waiversByOwner: [
            {
              ownerId: '603ac500381f48cba8433df1bc916991',
              ownerName: 'maven-central',
              ownerType: 'repository',
              waivers: [
                {
                  id: 'bb8f2460677445fcba96e92ec5791eb5',
                  hash: '7a3c2521ae0c6f53e044',
                  policyId: '7ec551b9521347ddb01b9274ee6509fe',
                  ownerId: '603ac500381f48cba8433df1bc916991',
                  comment: 'license waiver test',
                  createTime: 1665157280852,
                  expiryTime: null,
                  creatorId: 'admin',
                  creatorName: 'Admin BuiltIn',
                  constraintFactsJson:
                    '[{"constraintId":"436ec5f5dd6643dbabb1ef2c96c314eb","constraintName":"Apache 1 not allowed","operatorName":"OR","conditionFacts":[{"conditionTypeId":"License","conditionIndex":0,"summary":"License is \'Apache-1.1\'","reason":"Found \'Apache-1.1\' license","reference":null,"triggerJson":"{\\"conditionIndex\\":0,\\"trigger\\":{\\"id\\":\\"Apache-1.1\\"}}"}]}]',
                  constraintFacts: [
                    {
                      constraintId: '436ec5f5dd6643dbabb1ef2c96c314eb',
                      constraintName: 'Apache 1 not allowed',
                      operatorName: 'OR',
                      conditionFacts: [
                        {
                          conditionTypeId: 'License',
                          conditionIndex: 0,
                          summary: "License is 'Apache-1.1'",
                          reason: "Found 'Apache-1.1' license",
                          reference: null,
                          triggerJson: '{"conditionIndex":0,"trigger":{"id":"Apache-1.1"}}',
                        },
                      ],
                    },
                  ],
                  associatedPackageUrl: null,
                  componentMatchStrategy: 'EXACT_COMPONENT',
                  componentIdentifier: null,
                  policyName: 'Luis Policy',
                  policyWaiverId: 'bb8f2460677445fcba96e92ec5791eb5',
                  scopeOwnerId: '603ac500381f48cba8433df1bc916991',
                  scopeOwnerType: 'repository',
                  scopeOwnerName: 'maven-central',
                },
              ],
            },
          ],
        },
        policyViolations: [
          {
            policyViolationId: '89313f43e8fc488ea7d1c230c0e898d8',
            policyId: '7ec551b9521347ddb01b9274ee6509fe',
            policyName: 'Luis Policy',
            policyOwner: {
              ownerId: 'ROOT_ORGANIZATION_ID',
              ownerName: 'Root Organization',
              ownerType: 'organization',
            },
            policyThreatLevel: 5,
            policyThreatCategory: 'LICENSE',
            constraints: [
              {
                constraintId: '436ec5f5dd6643dbabb1ef2c96c314eb',
                constraintName: 'Apache 1 not allowed',
                constraintOperator: 'OR',
                conditions: [
                  {
                    conditionType: 'License',
                    conditionSummary: "License is 'Apache-1.1'",
                    conditionReason: "Found 'Apache-1.1' license",
                    conditionTriggerReference: null,
                  },
                ],
              },
            ],
            constraintFactsJson:
              '[{"constraintId":"436ec5f5dd6643dbabb1ef2c96c314eb","constraintName":"Apache 1 not allowed","operatorName":"OR","conditionFacts":[{"conditionTypeId":"License","conditionIndex":0,"summary":"License is \'Apache-1.1\'","reason":"Found \'Apache-1.1\' license","reference":null,"triggerJson":"{\\"conditionIndex\\":0,\\"trigger\\":{\\"id\\":\\"Apache-1.1\\"}}"}]}]',
            policyActionTypeId: 'fail',
            lastReported: '2022-10-05T11:12:50.249-05:00',
          },
        ],
      },
    },
    router: {
      currentParams: {
        repositoryId: ownerId,
        componentIdentifier,
        componentHash: hash,
        matchState,
        proprietary: 'false',
        pathname,
        tabId: 'legal',
      },
    },
    productFeatures: {
      'advanced-legal-pack': 'advanced-legal-pack',
    },
  };

  let componentMultiLicensesUrl = getComponentMultiLicensesUrl({
      clientType,
      ownerType,
      ownerId,
      componentIdentifier,
    }),
    licensesWithSyntheticFilterUrl = getLicensesWithSyntheticFilterUrl(),
    licenseOverrideUrl = getLicenseOverrideUrl(ownerType, ownerId, componentIdentifier),
    componentPolicyViolationsUrl = getComponentPolicyViolationsUrl(pathname, ownerId),
    componentDetailsUrl = getComponentDetailsUrl({
      clientType,
      ownerType,
      ownerId,
      componentIdentifier,
      hash,
      matchState,
      proprietary,
    });

  beforeEach(() => {
    mock = axiosMockAdapter();
    mock.onGet(componentMultiLicensesUrl).reply(200, {
      declaredLicenses: [
        {
          licenseId: 'Apache-1.1',
          licenseName: 'Apache-1.1',
          licenses: [{ license: { licenseId: 'Apache-1.1', licenseName: 'Apache-1.1' }, threatLevel: 0 }],
        },
      ],
      observedLicenses: [
        {
          licenseId: 'Apache-1.1',
          licenseName: 'Apache-1.1',
          licenses: [{ license: { licenseId: 'Apache-1.1', licenseName: 'Apache-1.1' }, threatLevel: 0 }],
        },
      ],
      effectiveLicenses: [
        {
          licenseId: 'Apache-1.1',
          licenseName: 'Apache-1.1',
          licenses: [{ license: { licenseId: 'Apache-1.1', licenseName: 'Apache-1.1' }, threatLevel: 0 }],
        },
      ],
      selectableLicenses: [{ licenseId: 'Apache-1.1', licenseName: 'Apache-1.1' }],
    });
    mock.onGet(licensesWithSyntheticFilterUrl).reply(200, [
      {
        id: 'Apache-1.1',
        shortDisplayName: 'Apache-1.1',
        longDisplayName: 'Apache License 1.1',
      },
    ]);
    mock.onGet(licenseOverrideUrl).reply(200, {
      licenseOverridesByOwner: [
        {
          ownerId: '603ac500381f48cba8433df1bc916991',
          ownerName: 'maven-central',
          ownerType: 'repository',
          licenseOverride: {
            id: 'b0cb960552734a5d9b00c1d44a7635fa',
            ownerId: '603ac500381f48cba8433df1bc916991',
            status: 'OVERRIDDEN',
            comment: 'another thest',
            licenseIds: ['Apache-1.1'],
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
          },
        },
        {
          ownerId: 'REPOSITORY_CONTAINER_ID',
          ownerName: 'All Repositories',
          ownerType: 'repository_container',
          licenseOverride: null,
        },
        {
          ownerId: 'ROOT_ORGANIZATION_ID',
          ownerName: 'Root Organization',
          ownerType: 'organization',
          licenseOverride: null,
        },
      ],
    });

    mock.onGet(componentPolicyViolationsUrl).reply(200, [
      {
        policyViolationId: '89313f43e8fc488ea7d1c230c0e898d8',
        policyId: '7ec551b9521347ddb01b9274ee6509fe',
        policyName: 'Luis Policy',
        policyOwner: {
          ownerId: 'ROOT_ORGANIZATION_ID',
          ownerName: 'Root Organization',
          ownerType: 'organization',
        },
        policyThreatLevel: 5,
        policyThreatCategory: 'LICENSE',
        constraints: [
          {
            constraintId: '436ec5f5dd6643dbabb1ef2c96c314eb',
            constraintName: 'Apache 1 not allowed',
            constraintOperator: 'OR',
            conditions: [
              {
                conditionType: 'License',
                conditionSummary: "License is 'Apache-1.1'",
                conditionReason: "Found 'Apache-1.1' license",
                conditionTriggerReference: null,
              },
            ],
          },
        ],
        constraintFactsJson:
          '[{"constraintId":"436ec5f5dd6643dbabb1ef2c96c314eb","constraintName":"Apache 1 not allowed","operatorName":"OR","conditionFacts":[{"conditionTypeId":"License","conditionIndex":0,"summary":"License is \'Apache-1.1\'","reason":"Found \'Apache-1.1\' license","reference":null,"triggerJson":"{\\"conditionIndex\\":0,\\"trigger\\":{\\"id\\":\\"Apache-1.1\\"}}"}]}]',
        policyActionTypeId: 'fail',
        lastReported: '2022-10-05T11:12:50.249-05:00',
      },
    ]);
    mock.onGet(componentDetailsUrl).reply(200, {
      hash: '7a3c2521ae0c6f53e044',
      matchState: 'exact',
      declaredLicenses: [
        {
          licenseId: 'Apache-1.1',
          licenseName: 'Apache-1.1',
        },
      ],
      observedLicenses: [
        {
          licenseId: 'Apache-1.1',
          licenseName: 'Apache-1.1',
        },
      ],
      overriddenLicenses: [
        {
          licenseId: 'Apache-1.1',
          licenseName: 'Apache-1.1',
        },
      ],
      policyMaxThreatLevelsByCategory: {
        license: 5,
        security: 7,
        quality: 1,
      },
      effectiveLicenses: [
        {
          licenseId: 'Apache-1.1',
          licenseName: 'Apache-1.1',
        },
      ],
      effectiveLicenseStatus: 'Overridden',
      catalogDate: 1132682799000,
      relativePopularity: 3,
      securityVulnerabilities: [],
      website: null,
      policyAlerts: [
        {
          trigger: {
            policyId: '7ec551b9521347ddb01b9274ee6509fe',
            policyName: 'Luis Policy',
            threatLevel: 5,
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
                    constraintId: '436ec5f5dd6643dbabb1ef2c96c314eb',
                    constraintName: 'Apache 1 not allowed',
                    operatorName: 'OR',
                    conditionFacts: [
                      {
                        conditionTypeId: 'License',
                        conditionIndex: 0,
                        summary: "License is 'Apache-1.1'",
                        reason: "Found 'Apache-1.1' license",
                        reference: null,
                        triggerJson: '{"conditionIndex":0,"trigger":{"id":"Apache-1.1"}}',
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
          actions: [],
        },
      ],
      licenseThreatLevel: 0,
      licenseThreatGroupNames: ['Liberal'],
      majorRevisionStep: false,
      identificationSource: 'Sonatype',
      identificationSourceComment: null,
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
      componentCategories: [
        {
          componentCategoryId: 10,
          path: 'Build Tools',
        },
      ],
      hygieneRating: null,
      integrityRating: {
        id: 3,
        label: 'Not Applicable',
      },
      breakingChangesCount: null,
      analyzerFeatures: {
        analysisSource: 'SDS',
        analysisType: null,
        scanClient: null,
        hasLicense: true,
        hasIdentity: true,
        hasSecurity: true,
        manifestContentType: null,
      },
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
      groupId: 'ant',
      artifactId: 'ant',
      version: '1.6',
      declaredLicenseIds: ['Apache-1.1'],
      observedLicenseIds: ['Apache-1.1'],
    });
    mock.onGet(getOrganizationsUrl() + '/icon/ROOT_ORGANIZATION_ID').reply(307);
  });

  it('renders the FirewallPolicyViolationsTile and VulnerabilitiesTableTile', async () => {
    render(<FirewallLegalTab />, { preloadedState });
    await waitFor(() => expect(screen.queryByText(/License Detections/)).toBeVisible());
    expect(screen.queryByText(/overridden/)).toBeVisible();
    expect(screen.queryAllByText(/Apache-1.1/).length).toBe(4);
    expect(screen.queryByText(/Review Obligations/)).toBeVisible;
    expect(screen.queryByText(/Legal Policy Violations/)).toBeVisible();
    expect(screen.queryByText(/Policy\/Action/)).toBeVisible();
    expect(screen.queryByText(/Luis Policy/)).toBeVisible();
    expect(screen.queryByText(/Threat/)).toBeVisible();
    expect(screen.queryByText(/5/)).toBeVisible();
    expect(screen.queryByText(/Constraint Name/)).toBeVisible();
    expect(screen.queryByText(/Apache 1 not allowed/)).toBeVisible();
    expect(screen.queryByText(/Condition/)).toBeVisible();
    expect(screen.queryByText(/Found 'Apache-1.1' license/)).toBeVisible();
    expect(screen.queryByText(/Select Row/)).toBeVisible();
    expect(screen.queryAllByRole('button')[2].ariaLabel).toMatch(
      "5; Luis PolicyProxy Failing; Apache 1 not allowed; Found 'Apache-1.1' license"
    );
  });

  it('contains "View Existing Waivers" button and shows "Component Waivers" popover on click', () => {
    render(<FirewallLegalTab />, { preloadedState });
    let viewExistingWaiversButton = screen.queryByText(/View Existing Waivers/);
    fireEvent.click(viewExistingWaiversButton);
    expect(viewExistingWaiversButton).toBeVisible();
    expect(screen.queryByText(/Component Waivers/)).toBeVisible();
    expect(screen.queryByText(/Policy\/Constraint/)).toBeVisible();
    expect(screen.queryAllByText(/Created/)[0]).toBeVisible();
    expect(screen.queryByText(/Scope/)).toBeVisible();
    expect(screen.queryByText(/Components/)).toBeVisible();
    expect(screen.queryByText(/Created by/)).toBeVisible();
    expect(screen.queryByText(/Comment/)).toBeVisible();

    expect(screen.queryAllByText(/Luis Policy/)[1]).toBeVisible();
    expect(screen.queryAllByText(/Apache 1 not allowed/)[1]).toBeVisible();
    expect(screen.queryByText(/10\/07\/2022/)).toBeVisible();
    expect(screen.queryByText(/Admin BuiltIn/)).toBeVisible();
    expect(screen.queryByText(/Repository - repository/)).toBeVisible();
    expect(screen.queryByText(/Comment/)).toBeVisible();
    expect(screen.queryByText(/license waiver test/)).toBeVisible();
  });

  it('will not show "Review Obligations" button when advanced-legal-pack is not present in productFeatures', () => {
    let customPreloadedState = { ...preloadedState, productFeatures: {} };

    render(<FirewallLegalTab />, { preloadedState: customPreloadedState });

    let reviewObligationsButton = screen.queryByText(/Review Obligations/);

    expect(reviewObligationsButton).toBeNull();
  });

  it('will show "Violation Details" popover on violation row click', () => {
    render(<FirewallLegalTab />, { preloadedState });
    let clickableElement = screen.queryByText(/Luis Policy/);
    expect(clickableElement).toBeVisible();
    fireEvent.click(clickableElement);
    expect(screen.queryByText(/Violation of/)).toBeVisible();
    expect(screen.queryAllByText(/Luis Policy/)[1]).toBeVisible();
    expect(screen.queryByText(/Policy Constraint/)).toBeVisible();
    expect(screen.queryAllByText(/Apache 1 not allowed/)[1]).toBeVisible();
    expect(screen.queryByText(/is in violation for the following reason\(s\)/)).toBeVisible();
    expect(screen.queryByText(/Policy Constraint/)).toBeVisible();
    expect(screen.queryAllByText(/Found 'Apache-1.1' license/)[1]).toBeVisible();
  });
});
