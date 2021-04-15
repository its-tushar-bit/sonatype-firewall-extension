/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
window.PolicyTileMockData = {
  getApplicablePolicies: function () {
    return {
      policiesByOwner: [
        {
          ownerId: 'testappid',
          ownerName: 'Test Application',
          ownerType: 'application',
          policies: [
            {
              id: 'testpolicyid',
              name: 'Test Policy 1',
              ownerId: 'testappid',
              enabled: true,
              threatLevel: 0,
              constraints: [
                {
                  id: 'constraintid',
                  name: 'Proprietary',
                  enabled: true,
                  operator: 'OR',
                  conditions: [
                    {
                      conditionTypeId: 'Proprietary',
                      operator: 'is true',
                      value: null,
                    },
                  ],
                },
              ],
              actions: {
                develop: [
                  {
                    actionTypeId: 'warn',
                    target: null,
                  },
                ],
                build: [
                  {
                    actionTypeId: 'warn',
                    target: null,
                  },
                ],
                'stage-release': [
                  {
                    actionTypeId: 'fail',
                    target: null,
                  },
                ],
                release: [
                  {
                    actionTypeId: 'fail',
                    target: null,
                  },
                ],
                operate: [
                  {
                    actionTypeId: 'warn',
                    target: null,
                  },
                ],
              },
              monitorNotifyActions: null,
            },
            {
              id: 'testarchpolicyid',
              name: 'Architecture Quality',
              ownerId: 'testappid',
              enabled: true,
              threatLevel: 5,
              constraints: [
                {
                  id: 'unpopularconstraintid',
                  name: 'Unpopular',
                  enabled: true,
                  operator: 'OR',
                  conditions: [
                    {
                      conditionTypeId: 'License',
                      operator: 'is',
                      value: 'GPL-UNSPECIFIED',
                    },
                  ],
                },
                {
                  id: 'oldconstraintid',
                  name: 'Old',
                  enabled: true,
                  operator: 'OR',
                  conditions: [
                    {
                      conditionTypeId: 'AgeInDays',
                      operator: 'older than',
                      value: '1825',
                    },
                  ],
                },
              ],
              actions: {
                proxy: [
                  {
                    actionTypeId: 'warn',
                    target: null,
                  },
                ],
                develop: [
                  {
                    actionTypeId: 'fail',
                    target: null,
                  },
                ],
                build: [
                  {
                    actionTypeId: 'warn',
                    target: null,
                  },
                ],
                'stage-release': [
                  {
                    actionTypeId: 'warn',
                    target: null,
                  },
                ],
              },
              monitorNotifyActions: [],
            },
          ],
          policyTags: [],
        },
        {
          ownerId: 'testorgid',
          ownerName: 'Test Organization',
          ownerType: 'organization',
          policies: [
            {
              id: 'orgtestpolicyid',
              name: 'Org Test Policy1',
              ownerId: 'testorgid',
              enabled: true,
              threatLevel: 1,
              constraints: [
                {
                  id: 'oldpolicyid',
                  name: 'Old',
                  enabled: true,
                  operator: 'OR',
                  conditions: [
                    {
                      conditionTypeId: 'AgeInDays',
                      operator: 'older than',
                      value: '1095',
                    },
                  ],
                },
              ],
              actions: {
                proxy: [
                  {
                    actionTypeId: 'warn',
                    target: null,
                  },
                ],
                develop: [
                  {
                    actionTypeId: 'warn',
                    target: null,
                  },
                ],
                build: [
                  {
                    actionTypeId: 'warn',
                    target: null,
                  },
                ],
                'stage-release': [
                  {
                    actionTypeId: 'fail',
                    target: null,
                  },
                ],
                release: [
                  {
                    actionTypeId: 'fail',
                    target: null,
                  },
                ],
                operate: [
                  {
                    actionTypeId: 'fail',
                    target: null,
                  },
                ],
              },
              monitorNotifyActions: null,
            },
          ],
          policyTags: [],
        },
        {
          ownerId: 'ROOT_ORGANIZATION_ID',
          ownerName: 'Root Organization',
          ownerType: 'organization',
          policies: [],
          policyTags: [],
        },
      ],
    };
  },
  getPolicyMonitoring: function () {
    return {
      data: {
        policyMonitoringByOwner: [
          {
            ownerName: 'testApp',
            policyMonitoring: { stageTypeId: 'release' },
          },
        ],
      },
    };
  },
};
