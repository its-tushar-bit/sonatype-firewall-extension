/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { cleanup } from '@testing-library/react';
import { render, screen, axiosMockAdapter, waitFor, fireEvent, queryByTextWithin, within } from 'TestRoot/SpecUtil';
import FirewallLabelsTab from 'MainRoot/firewall/firewallComponentDetailsPage/labels/FirewallLabelsTab';
import {
  getComponentMultiLicensesUrl,
  getLicensesWithSyntheticFilterUrl,
  getLicenseOverrideUrl,
  getComponentPolicyViolationsUrl,
  getComponentDetailsUrl,
  getOrganizationsUrl,
  getApplicableLabelsUrl,
  getSaveLabelScopeUrl,
  getApplicableLabelScopesUrl,
  removeLabel,
  getComponentLabels,
} from 'MainRoot/util/CLMLocation';
import { initialState } from 'MainRoot/firewall/firewallReducer';

describe('FirewallLabelsTab', () => {
  let mock,
    clientType = 'ci',
    ownerType = 'repository',
    ownerId = '603ac500381f48cba8433df1bc916991',
    componentIdentifier =
      '{"format":"maven","coordinates":{"artifactId":"ant","classifier":"","extension":"jar","groupId":"ant","version":"1.6"}}',
    hash = '7a3c2521ae0c6f53e044',
    matchState = 'exact',
    pathname = 'ant/ant/1.6/ant-1.6.jar',
    labelId = '3c55ae7183a246d6b153b1665f087d33';

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
        pathname,
        tabId: 'legal',
      },
      currentState: {
        name: 'firewall.componentDetailsPage.legal',
        url: '/legal',
        data: {},
      },
    },
    componentDetails: {
      pendingLoads: new Set(),
      isSavingLabelScope: false,
      labels: [],
      applicableLabels: [
        {
          id: labelId,
          label: 'Architecture-Blacklisted',
          description: 'Components which have been blacklisted from use',
          color: 'orange',
          ownerId: 'ROOT_ORGANIZATION_ID',
          ownerType: 'organization',
        },
        {
          id: '1f820f1bbf4347acbb69120c47c58b80',
          label: 'Architecture-Cleanup',
          description: 'Components which are relics of a build and should not be included in the distribution',
          color: 'orange',
          ownerId: 'ROOT_ORGANIZATION_ID',
          ownerType: 'organization',
        },
        {
          id: 'bf6ee2316449415eac6e439463bab2ef',
          label: 'Architecture-Deprecated',
          description: 'Components we want to discourage from developer use',
          color: 'orange',
          ownerId: 'ROOT_ORGANIZATION_ID',
          ownerType: 'organization',
        },
      ],
      applicableLabelScopes: [],
      loadError: null,
      showApplyLabelModal: false,
      applyLabelMaskState: null,
      removeLabelMaskState: null,
      labelModalMaskState: null,
      selectedLabelDetails: {},
      selectedLabelOwnerType: '',
      labelScopeToSave: {},
      applicableLabelsLoadError: null,
      removeAppliedLabelError: null,
      showRemoveLabelModal: false,
      applicableLabelScopesLoadError: null,
      saveLabelScopeError: null,
      showMatchersPopover: false,
      setProprietaryMatchers: {
        submitMaskState: null,
        submitError: null,
        data: {
          pathnames: [],
          regex: '',
        },
      },
      dependencyTreeSubset: null,
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
    }),
    applicableLabelsUrl = getApplicableLabelsUrl(ownerType, ownerId),
    saveLabelScopeUrl = getSaveLabelScopeUrl(ownerType, ownerId, hash),
    applicableLabelScopesUrl = getApplicableLabelScopesUrl(ownerType, ownerId, labelId),
    removeLabelUrl = removeLabel('organization', 'ROOT_ORGANIZATION_ID', hash, 'bf6ee2316449415eac6e439463bab2ef'),
    componentLabelsUrl = getComponentLabels(ownerId, hash, ownerType);

  beforeEach(() => {
    mock = axiosMockAdapter();

    mock.onGet(applicableLabelsUrl).reply(200, {
      labelsByOwner: [
        {
          ownerId,
          ownerName: 'maven-central',
          ownerType: 'repository',
          labels: [],
        },
        {
          ownerId: 'REPOSITORY_CONTAINER_ID',
          ownerName: 'Repository Managers',
          ownerType: 'repository_container',
          labels: [],
        },
        {
          ownerId: 'ROOT_ORGANIZATION_ID',
          ownerName: 'Root Organization',
          ownerType: 'organization',
          labels: [
            {
              id: labelId,
              label: 'Architecture-Blacklisted',
              description: 'Components which have been blacklisted from use',
              color: 'orange',
              ownerId: 'ROOT_ORGANIZATION_ID',
              ownerType: 'REPOSITORY',
            },
            {
              id: '1f820f1bbf4347acbb69120c47c58b80',
              label: 'Architecture-Cleanup',
              description: 'Components which are relics of a build and should not be included in the distribution',
              color: 'orange',
              ownerId: 'ROOT_ORGANIZATION_ID',
              ownerType: 'REPOSITORY',
            },
            {
              id: 'bf6ee2316449415eac6e439463bab2ef',
              label: 'Architecture-Deprecated',
              description: 'Components we want to discourage from developer use',
              color: 'orange',
              ownerId: 'ROOT_ORGANIZATION_ID',
              ownerType: 'REPOSITORY',
            },
          ],
        },
      ],
    });

    mock.onGet(applicableLabelScopesUrl).reply(200, {
      id: 'ROOT_ORGANIZATION_ID',
      name: 'Root Organization',
      type: 'organization',
      children: [
        {
          id: 'REPOSITORY_CONTAINER_ID',
          name: 'Repository Managers',
          type: 'repository_container',
          children: [
            {
              id: ownerId,
              name: 'maven-central',
              type: 'repository',
              children: null,
            },
          ],
        },
      ],
    });

    mock.onDelete(removeLabelUrl).reply(204);

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
          ownerId,
          ownerName: 'maven-central',
          ownerType: 'repository',
          licenseOverride: {
            id: 'b0cb960552734a5d9b00c1d44a7635fa',
            ownerId,
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
  });

  afterEach(() => {
    cleanup();
  });

  it('will show "Manage Labels" transfer list with "Available Labels" and "Applied Labels" half lists', async () => {
    render(<FirewallLabelsTab />, { preloadedState });
    let foundLabels;

    await waitFor(() => {
      expect(screen.queryByText(/Manage Labels/)).toBeVisible();
      expect(screen.queryByText(/Available Labels/)).toBeVisible();
      expect(screen.queryByText(/Applied Labels/)).toBeVisible();
    });

    await waitFor(() => {
      foundLabels = queryByTextWithin(/Architecture/, '.iq-transfer-list__control-box').all;
      expect(foundLabels.length).toBe(3);
    });
  });

  describe('will add labels', () => {
    const executeCommonAssertions = async () => {
      render(<FirewallLabelsTab />, { preloadedState });
      let foundLabels;

      await waitFor(() => {
        foundLabels = queryByTextWithin(/Architecture/, '.iq-transfer-list__control-box').all;
        expect(foundLabels.length).toBe(3);
      });

      fireEvent.click(foundLabels[0]);

      let applyLabelsModal;

      await waitFor(() => {
        applyLabelsModal = document.querySelector('.nx-modal');
        expect(applyLabelsModal).toBeVisible();
      });

      expect(queryByTextWithin(/Architecture-Blacklisted/, applyLabelsModal).first).toBeVisible();

      const select = screen.getByRole('combobox');
      const options = within(select).getAllByRole('option');
      expect(options.length).toBe(3);

      expect(options[0]).toHaveTextContent(/Root Organization/i);
      expect(options[1]).toHaveTextContent(/Repository Managers/i);
      expect(options[2]).toHaveTextContent(/maven-central/i);

      fireEvent.change(select, { target: { value: '603ac500381f48cba8433df1bc916991' } });
      fireEvent.click(queryByTextWithin(/Submit/, applyLabelsModal).first);

      return applyLabelsModal;
    };

    it('from "Available Labels" to "Applied Labels" half list', async () => {
      mock
        .onPost(saveLabelScopeUrl, {
          color: 'orange',
          description: 'Components which have been blacklisted from use',
          id: labelId,
          label: 'Architecture-Blacklisted',
          ownerId: 'ROOT_ORGANIZATION_ID',
        })
        .reply(204);

      mock.onGet(componentLabelsUrl).reply(200, {
        labelsByOwner: [
          {
            ownerId,
            ownerName: 'maven-central',
            ownerType: 'repository',
            labels: [
              {
                id: labelId,
                ownerId,
                label: 'Architecture-Blacklisted',
                labelLowercase: 'architecture-blacklisted',
                description: 'Components which have been blacklisted from use',
                color: 'orange',
              },
            ],
          },
        ],
      });

      const applyLabelsModal = await executeCommonAssertions();

      await waitFor(() => {
        expect(queryByTextWithin(/Success!/, applyLabelsModal).first).toBeVisible();
      });
    });

    it('from "Available Labels" to "Applied Labels" half list but server fails', async () => {
      mock.onPost(saveLabelScopeUrl).reply(500);

      const applyLabelsModal = await executeCommonAssertions();

      await waitFor(() => {
        expect(queryByTextWithin(/An error occurred saving data. Error 500/, applyLabelsModal).first).toBeVisible();
      });
    });
  });

  describe('will remove labels', () => {
    const customPreloadedState = {
      ...preloadedState,
      componentDetails: {
        ...preloadedState.componentDetails,
        labels: [
          {
            id: 'bf6ee2316449415eac6e439463bab2ef',
            label: 'Architecture-Deprecated',
            description: 'Components we want to discourage from developer use',
            color: 'orange',
            ownerId: 'ROOT_ORGANIZATION_ID',
            ownerType: 'organization',
          },
        ],
        applicableLabels: [
          {
            id: labelId,
            label: 'Architecture-Blacklisted',
            description: 'Components which have been blacklisted from use',
            color: 'orange',
            ownerId: 'ROOT_ORGANIZATION_ID',
            ownerType: 'organization',
          },
          {
            id: '1f820f1bbf4347acbb69120c47c58b80',
            label: 'Architecture-Cleanup',
            description: 'Components which are relics of a build and should not be included in the distribution',
            color: 'orange',
            ownerId: 'ROOT_ORGANIZATION_ID',
            ownerType: 'organization',
          },
        ],
      },
    };

    const executeCommonAssertions = async () => {
      render(<FirewallLabelsTab />, { preloadedState: customPreloadedState });
      let appliedLabels;

      await waitFor(() => {
        appliedLabels = queryByTextWithin(
          /Architecture/,
          document.querySelectorAll('.iq-transfer-list__control-box')[1]
        ).all;
        expect(appliedLabels.length).toBe(1);
      });

      fireEvent.click(appliedLabels[0]);

      let deleteLabelsModal;

      await waitFor(() => {
        deleteLabelsModal = document.querySelector('.nx-modal');
        expect(
          queryByTextWithin(/Are you sure you want to remove this label\?/, deleteLabelsModal).first
        ).toBeVisible();
      });

      fireEvent.click(queryByTextWithin(/Remove/, '.nx-modal .nx-btn-bar').first);

      return deleteLabelsModal;
    };

    it('from "Applied Labels" to "Available Labels" half list', async () => {
      mock.onGet(componentLabelsUrl).reply(200, {
        labelsByOwner: [],
      });

      const deleteLabelsModal = await executeCommonAssertions();

      await waitFor(() => {
        expect(queryByTextWithin(/Success!/, deleteLabelsModal).first).toBeVisible();
      });
    });

    it('from "Applied Labels" to "Available Labels" half list but server fails', async () => {
      mock.onDelete(removeLabelUrl).reply(500);

      const deleteLabelsModal = await executeCommonAssertions();

      await waitFor(() => {
        expect(queryByTextWithin(/An error occurred removing label. Error 500/, deleteLabelsModal).first).toBeVisible();
      });
    });
  });
});
