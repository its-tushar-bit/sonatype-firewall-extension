/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, axiosMockAdapter, waitFor, fireEvent, queryByTextWithin } from 'TestRoot/SpecUtil';
import FirewallLegalTab from 'MainRoot/firewall/firewallComponentDetailsPage/legal/FirewallLegalTab';
import {
  getComponentMultiLicensesUrl,
  getLicensesWithSyntheticFilterUrl,
  getLicenseOverrideUrl,
  getComponentPolicyViolationsUrl,
  getComponentDetailsUrl,
  getOrganizationsUrl,
  getBaseLicenseOverrideUrl,
  getDeleteLicenseOverrideUrl,
} from 'MainRoot/util/CLMLocation';
import { initialState } from 'MainRoot/firewall/firewallReducer';
import * as routerActions from 'MainRoot/reduxUiRouter/routerActions';

describe('FirewallLegalTab', () => {
  let mock,
    clientType = 'ci',
    ownerType = 'repository',
    ownerId = '603ac500381f48cba8433df1bc916991',
    componentIdentifier =
      '{"format":"maven","coordinates":{"artifactId":"ant","classifier":"","extension":"jar","groupId":"ant","version":"1.6"}}',
    hash = '7a3c2521ae0c6f53e044',
    matchState = 'exact',
    pathname = 'ant/ant/1.6/ant-1.6.jar',
    newComment = 'adding a new comment';

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
            waived: false,
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
        pathname,
        tabId: 'legal',
      },
      currentState: {
        name: 'firewall.componentDetailsPage.legal',
        url: '/legal',
        data: {},
      },
    },
    productFeatures: {
      productFeatures: {
        'advanced-legal-pack': true,
      },
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
    });
  let baseLicenseOverrideUrl = getBaseLicenseOverrideUrl(ownerType, ownerId);

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
        id: 'Apache-1.0',
        shortDisplayName: 'Apache-1.0',
        longDisplayName: 'Apache License 1.0',
      },
      {
        id: 'Apache-1.1',
        shortDisplayName: 'Apache-1.1',
        longDisplayName: 'Apache License 1.1',
      },
      {
        id: 'Apache-2.0',
        shortDisplayName: 'Apache-2.0',
        longDisplayName: 'Apache License 2.0',
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
            comment: 'another test',
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
          ownerName: 'Repository Managers',
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

    mock
      .onPost(baseLicenseOverrideUrl, {
        id: null,
        ownerId,
        status: 'OVERRIDDEN',
        comment: newComment,
        licenseIds: ['Apache-1.1', 'Apache-1.0'],
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
      })
      .reply(200, {
        id: 'b0cb960552734a5d9b00c1d44a7635fa',
        ownerId,
        status: 'OVERRIDDEN',
        comment: newComment,
        licenseIds: ['Apache-1.1', 'Apache-1.0'],
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
      });

    mock
      .onPost(baseLicenseOverrideUrl, {
        id: null,
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
        status: 'SELECTED',
        comment: 'adding a new comment',
        ownerId,
      })
      .reply(200, {
        id: 'b0cb960552734a5d9b00c1d44a7635fa',
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
        status: 'SELECTED',
        comment: 'adding a new comment',
        ownerId,
      });
    mock.onDelete(getDeleteLicenseOverrideUrl(ownerType, ownerId, 'b0cb960552734a5d9b00c1d44a7635fa')).reply(204);
    ['OPEN', 'ACKNOWLEDGED', 'CONFIRMED'].forEach((status) =>
      mock
        .onPost(baseLicenseOverrideUrl, {
          id: null,
          licenseIds: [],
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
          status,
          comment: 'adding a new comment',
          ownerId,
        })
        .reply(200, {
          id: 'b0cb960552734a5d9b00c1d44a7635fa',
          licenseIds: [],
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
          status,
          comment: 'adding a new comment',
          ownerId,
        })
    );
  });

  it('renders the FirewallPolicyViolationsTile and VulnerabilitiesTableTile', async () => {
    render(<FirewallLegalTab />, { preloadedState });
    await waitFor(() => expect(screen.queryByText(/License Detections/)).toBeVisible());
    expect(screen.queryByText(/overridden/)).toBeVisible();
    expect(screen.queryAllByText(/Apache-1.1/).length).toBe(4);
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
    expect(
      screen.getByRole('button', {
        name: /5; luis policy.*fail.*apache 1 not allowed; found 'apache-1\.1' license; unapplied waiver/i,
      })
    ).toBeVisible();
  });

  it('contains "View Existing Waivers" button and shows "Component Waivers" popover on click', () => {
    render(<FirewallLegalTab />, { preloadedState });
    let viewExistingWaiversButton = screen.queryByText(/View Existing Waivers/);
    fireEvent.click(viewExistingWaiversButton);
    expect(viewExistingWaiversButton).toBeVisible();
    expect(screen.queryByText(/Component Waivers/)).toBeVisible();
    expect(screen.queryAllByText(/Created/)[0]).toBeVisible();
    expect(screen.queryByText(/Scope/)).toBeVisible();
    expect(screen.queryAllByText(/Component/)[1]).toBeVisible();
    expect(screen.queryByText(/Author/)).toBeVisible();
    expect(screen.queryByText(/Comment/)).toBeVisible();

    expect(screen.queryByText(/2022-10-07/)).toBeVisible();
    expect(screen.queryByText(/Repository - maven-central/)).toBeVisible();
    expect(screen.queryByText(/Admin BuiltIn/)).toBeVisible();
    expect(screen.queryByText(/license waiver test/)).toBeVisible();
  });

  it('will not show "Review Obligations" button when advanced-legal-pack is not present in productFeatures', () => {
    let customPreloadedState = { ...preloadedState, productFeatures: {} };

    render(<FirewallLegalTab />, { preloadedState: customPreloadedState });

    let reviewObligationsButton = screen.queryByText(/Review Obligations/);

    expect(reviewObligationsButton).toBeNull();
  });

  it('will show "Review Obligations" button when advanced-legal-pack is present in productFeatures', async () => {
    render(<FirewallLegalTab />, { preloadedState });

    await waitFor(() => expect(screen.queryByText(/License Detections/)).toBeVisible());

    let reviewObligationsButton = screen.queryByText(/Review Obligations/);

    expect(reviewObligationsButton).toBeVisible();
  });

  it('will redirect to legal overview page when "Review Obligations" button is clicked with string componentIdentifier', async () => {
    const stateGoSpy = jest.spyOn(routerActions, 'stateGo');

    render(<FirewallLegalTab />, { preloadedState });

    await waitFor(() => expect(screen.queryByText(/License Detections/)).toBeVisible());

    let reviewObligationsButton = screen.queryByText(/Review Obligations/);
    expect(reviewObligationsButton).toBeVisible();

    fireEvent.click(reviewObligationsButton);

    // Verify stateGo was called with the correct state and parameters
    expect(stateGoSpy).toHaveBeenCalledWith('firewall.legalOverview', {
      componentIdentifier,
      repositoryId: ownerId,
      tabId: 'legal',
    });

    stateGoSpy.mockRestore();
  });

  it('will show "Edit Licenses" popover on Edit button click', async () => {
    render(<FirewallLegalTab />, { preloadedState });
    let editButton;
    await waitFor(() => {
      editButton = screen.queryByText(/Edit/);
      expect(editButton).toBeVisible();
    });
    fireEvent.click(editButton);
    expect(screen.queryByText(/Edit Licenses/)).toBeVisible();

    const popover = document.getElementById('edit-licenses-popover');

    expect(queryByTextWithin(/Effective Licenses/, popover).first).toBeVisible();
    expect(queryByTextWithin(/Declared Licenses/, popover).first).toBeVisible();
    expect(queryByTextWithin(/Observed Licenses/, popover).first).toBeVisible();
    expect(queryByTextWithin(/Apache-1.1/, '.iq-license-info-section').all.length).toBe(3);

    expect(queryByTextWithin(/Scope/, popover).first).toBeVisible();
    expect(queryByTextWithin(/Repository - maven-central/, popover).first).toBeVisible();
    expect(queryByTextWithin(/Repository Managers/, popover).first).toBeVisible();
    expect(queryByTextWithin(/Organization - Root Organization/, popover).first).toBeVisible();

    const statusDropDown = document.querySelector('#status-select');
    expect(statusDropDown).toBeVisible();
    expect(queryByTextWithin(/Open/, statusDropDown).first).toBeVisible();
    expect(queryByTextWithin(/Acknowledged/, statusDropDown).first).toBeVisible();
    expect(queryByTextWithin(/Overridden/, statusDropDown).first).toBeVisible();
    expect(queryByTextWithin(/Selected/, statusDropDown).first).toBeVisible();
    expect(queryByTextWithin(/Confirmed/, statusDropDown).first).toBeVisible();
    expect(queryByTextWithin(/Inherit Status/, statusDropDown).first).toBeVisible();

    expect(queryByTextWithin(/Comment/, popover).first).toBeVisible();

    expect(queryByTextWithin(/Save/, popover).first).toBeVisible();
    expect(queryByTextWithin(/Cancel/, popover).first).toBeVisible();
  });

  describe('"Edit Licenses" popover', () => {
    it('will show a transfer list with "Available Licenses" and "Selected Licenses" collections whitin "Edit Licenses" when Status Overriden is selected', async () => {
      render(<FirewallLegalTab />, { preloadedState });
      let editButton;
      await waitFor(() => {
        editButton = screen.queryByText(/Edit/);
        expect(editButton).toBeVisible();
      });
      fireEvent.click(editButton);
      expect(screen.queryByText(/Edit Licenses/)).toBeVisible();

      const popover = document.getElementById('edit-licenses-popover');

      const repositoryRadio = queryByTextWithin(/Repository - maven-central/, popover).first;
      expect(repositoryRadio).toBeVisible();
      fireEvent.click(repositoryRadio);

      const statusDropDown = document.querySelector('#status-select');
      expect(statusDropDown).toBeVisible();
      fireEvent.click(statusDropDown);
      fireEvent.click(queryByTextWithin(/Overridden/, statusDropDown).first);

      await waitFor(() => {
        expect(queryByTextWithin(/Available Licenses/, popover).first).toBeVisible();
      });
      expect(queryByTextWithin(/Selected Licenses/, popover).first).toBeVisible();

      expect(
        document.querySelectorAll('.nx-transfer-list__half:nth-child(1) .nx-transfer-list__item').length
      ).toBeGreaterThan(1);
      expect(document.querySelectorAll('.nx-transfer-list__half:nth-child(2) .nx-transfer-list__item').length).toBe(1);
      expect(document.querySelector('.nx-transfer-list__half:nth-child(2) .nx-transfer-list__item').textContent).toBe(
        'Apache-1.1'
      );
    });

    it('will set license status as OVERRIDDEN', async () => {
      render(<FirewallLegalTab />, { preloadedState });
      let editButton;

      await waitFor(() => {
        editButton = screen.queryByText(/Edit/);
        expect(editButton).toBeVisible();
      });

      fireEvent.click(editButton);
      expect(screen.queryByText(/Edit Licenses/)).toBeVisible();

      const popover = document.getElementById('edit-licenses-popover');

      const repositoryRadio = queryByTextWithin(/Repository - maven-central/, popover).first;
      expect(repositoryRadio).toBeVisible();
      fireEvent.click(repositoryRadio);

      const statusDropDown = document.querySelector('#status-select');
      expect(statusDropDown).toBeVisible();
      fireEvent.click(statusDropDown);
      fireEvent.click(queryByTextWithin(/Overridden/, statusDropDown).first);

      await waitFor(() => {
        expect(queryByTextWithin(/Available Licenses/, popover).first).toBeVisible();
      });
      expect(queryByTextWithin(/Selected Licenses/, popover).first).toBeVisible();

      const firstAvailableLicense = document.querySelector(
        '.nx-transfer-list__half:nth-child(1) .nx-transfer-list__item:nth-child(2) label'
      );

      expect(
        document.querySelectorAll('#license-detections-tile #effective-licenses-container .iq-legal-item').length
      ).toBe(1);

      fireEvent.click(firstAvailableLicense);

      await waitFor(() => {
        expect(document.querySelectorAll('.nx-transfer-list__half:nth-child(2) .nx-transfer-list__item').length).toBe(
          2
        );
      });

      const commentBox = document.querySelector('.nx-text-input__box textarea');
      fireEvent.change(commentBox, { target: { value: newComment } });

      await waitFor(() => {
        expect(queryByTextWithin(/Save/, popover).first.className.includes('disabled')).toBe(false);
      });

      fireEvent.submit(document.querySelector('.nx-form'));

      await waitFor(() => {
        expect(queryByTextWithin(/Save/, popover).first.className.includes('disabled')).toBe(false);
      });

      await waitFor(() => {
        expect(screen.queryByText('Success!')).toBeVisible();
      });
    });

    it('will set license status as SELECTED', async () => {
      render(<FirewallLegalTab />, { preloadedState });
      let editButton;

      await waitFor(() => {
        editButton = screen.queryByText(/Edit/);
        expect(editButton).toBeVisible();
      });

      fireEvent.click(editButton);
      expect(screen.queryByText(/Edit Licenses/)).toBeVisible();

      const popover = document.getElementById('edit-licenses-popover');

      await waitFor(() => {
        expect(queryByTextWithin(/Available Licenses/, popover).first).toBeVisible();
      });

      const repositoryRadio = queryByTextWithin(/Repository - maven-central/, popover).first;
      expect(repositoryRadio).toBeVisible();
      fireEvent.click(repositoryRadio);

      const statusDropDown = document.querySelector('#status-select');
      expect(statusDropDown).toBeVisible();
      fireEvent.change(statusDropDown, { target: { value: 'SELECTED' } });

      const selectedLicensesFieldset = document.querySelector('.iq-edit-licenses-form__selected-licenses');
      expect(queryByTextWithin(/Selected Licenses/, selectedLicensesFieldset).first).toBeVisible();

      const firstAvailableLicense = queryByTextWithin(/Apache-1.1/, selectedLicensesFieldset);
      expect(firstAvailableLicense.all.length).toBe(1);
      fireEvent.click(firstAvailableLicense.first);

      const commentBox = document.querySelector('.nx-text-input__box textarea');
      fireEvent.change(commentBox, { target: { value: newComment } });

      await waitFor(() => {
        expect(queryByTextWithin(/Save/, popover).first.className.includes('disabled')).toBe(false);
      });

      fireEvent.submit(document.querySelector('.nx-form'));

      await waitFor(() => {
        expect(queryByTextWithin(/Save/, popover).first.className.includes('disabled')).toBe(false);
      });

      await waitFor(() => {
        expect(screen.queryByText('Success!')).toBeVisible();
      });
    });

    ['OPEN', 'ACKNOWLEDGED', 'CONFIRMED', 'DELETE'].forEach((licenseStatus) => {
      it(`will set license status as ${licenseStatus === 'DELETE' ? 'INHERITED' : licenseStatus}`, async () => {
        render(<FirewallLegalTab />, { preloadedState });
        let editButton;

        await waitFor(() => {
          editButton = screen.queryByText(/Edit/);
          expect(editButton).toBeVisible();
        });

        fireEvent.click(editButton);
        expect(screen.queryByText(/Edit Licenses/)).toBeVisible();

        const popover = document.getElementById('edit-licenses-popover');

        await waitFor(() => {
          expect(queryByTextWithin(/Available Licenses/, popover).first).toBeVisible();
        });

        const repositoryRadio = queryByTextWithin(/Repository - maven-central/, popover).first;
        expect(repositoryRadio).toBeVisible();
        fireEvent.click(repositoryRadio);

        const statusDropDown = document.querySelector('#status-select');
        expect(statusDropDown).toBeVisible();
        fireEvent.change(statusDropDown, { target: { value: licenseStatus } });

        const commentBox = document.querySelector('.nx-text-input__box textarea');
        fireEvent.change(commentBox, { target: { value: newComment } });

        await waitFor(() => {
          expect(queryByTextWithin(/Save/, popover).first.className.includes('disabled')).toBe(false);
        });

        fireEvent.submit(document.querySelector('.nx-form'));

        await waitFor(() => {
          expect(queryByTextWithin(/Save/, popover).first.className.includes('disabled')).toBe(false);
        });

        await waitFor(() => {
          expect(screen.queryByText('Success!')).toBeVisible();
        });
      });
    });
  });
});
