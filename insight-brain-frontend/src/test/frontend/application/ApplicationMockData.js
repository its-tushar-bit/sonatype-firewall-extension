/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default {
  getApplicationsData: function () {
    return [
      {
        id: '78c1d44c07584e57945f04890c672e82',
        name: 'applicationName',
        publicId: 'bom1-12345678',
        organizationId: 'organizationId',
      },
    ];
  },
  getApplicationSummaryData: function (size) {
    var results = [
      {
        id: '78c1d44c07584e57945f04890c672e82',
        name: 'application3',
        publicId: 'bom1-12345678',
        organizationId: '1',
        organizationName: 'Ye Ole Organization',
        policyEvaluations: {},
        policyEvaluationsResults: {},
      },
      {
        id: '9999999c07584e57945f04890c672e99',
        name: 'application2',
        publicId: 'bom1-12345678',
        organizationId: '2',
        organizationName: 'Big Org',
        policyEvaluations: {},
        policyEvaluationsResults: {},
      },
      {
        id: '053e89a476b34d7dac5d97665d2d241e',
        name: 'app1',
        publicId: 'bom1-12345678',
        organizationId: '3',
        organizationName: 'Big Org',
        policyEvaluations: {},
        policyEvaluationsResults: {},
      },
    ];

    var stage = 'build';

    results.forEach(function (result) {
      result.policyEvaluations[stage] = {
        stageTypeId: stage,
        scanId: '2e12e6a9811347a78031b8969b604c49',
        time: 1371487786570,
        user: 'anonymous',
      };
      result.policyEvaluationsResults[stage] = {
        alerts: [],
        affectedComponentCount: 0,
        criticalComponentCount: 0,
        severeComponentCount: 0,
        moderateComponentCount: 0,
      };
    });
    if (size) {
      results = [];
      for (let app = 0; app < size; app++) {
        results.push({
          id: 'id' + app,
          name: 'name' + app,
          publicId: 'publicId' + app,
          organizationId: '1',
          organizationName: 'Ye Ole Organization',
          policyEvaluations: {
            build: {
              stageTypeId: 'build',
              scanId: 'scanId' + app,
              time: 1371487786570,
              user: 'anonymous',
            },
          },
          policyEvaluationsResults: {
            build: {
              alerts: [],
              affectedComponentCount: 0,
              criticalComponentCount: 0,
              severeComponentCount: 0,
              moderateComponentCount: 0,
            },
          },
        });
      }
    }
    return results;
  },
  getApplicablePolicies: function () {
    return {
      policiesByOwner: [
        {
          ownerId: '78c1d44c07584e57945f04890c672e82',
          name: 'applicationName',
          type: 'application',
          policies: undefined /* Irrelevant currently, set to undefined to cause errors if we use in the future */,
        },
        {
          ownerId: '9999999c07584e57945f04890c672e99',
          name: 'orgName',
          type: 'organization',
          policies: [
            {
              id: '053e89a476b34d7dac5d97665d2d241e',
              name: 'asdffffrfff',
              enabled: true,
              threatLevel: 10,
              constraints: [
                {
                  id: '076688f8f45a43b3a6061ef7aad6de4e',
                  name: 'asf',
                  enabled: true,
                  operator: 'OR',
                  conditions: [
                    {
                      conditionTypeId: 'License',
                      operator: 'is',
                      value: 'AAL',
                    },
                    {
                      conditionTypeId: 'AgeInDays',
                      operator: 'older than',
                      value: '360',
                    },
                    {
                      conditionTypeId: 'SecurityVulnerabilitySeverity',
                      operator: '=',
                      value: '44',
                    },
                    {
                      conditionTypeId: 'DependencyDepth',
                      operator: 'is direct dependency',
                      value: null,
                    },
                  ],
                },
                {
                  id: '6c2755ee5ef6400e935e913fdeda4e6b',
                  name: 'jjj',
                  enabled: true,
                  operator: 'OR',
                  conditions: [
                    {
                      conditionTypeId: 'License',
                      operator: 'is',
                      value: 'AAL',
                    },
                  ],
                },
                {
                  id: 'ed721f80645042e0b4505c072f7b657d',
                  name: 'ffff',
                  enabled: true,
                  operator: 'OR',
                  conditions: [
                    {
                      conditionTypeId: 'License',
                      operator: 'is',
                      value: 'AAL',
                    },
                  ],
                },
                {
                  id: '7f7c035288004b60a580df3f3e14326a',
                  name: 'test',
                  enabled: true,
                  operator: 'OR',
                  conditions: [
                    {
                      conditionTypeId: 'LicenseStatus',
                      operator: 'is',
                      value: 'OPEN',
                    },
                  ],
                },
              ],
              actions: {
                develop: [],
                build: [
                  {
                    actionTypeId: 'fail',
                    target: null,
                  },
                ],
                release: [],
                operate: [],
              },
            },
          ],
        },
      ],
    };
  },
  getPolicyEvaluationData: function () {
    return {
      alerts: [],
      affectedComponentCount: 10,
      criticalComponentCount: 5,
      severeComponentCount: 3,
      moderateComponentCount: 2,
    };
  },
};
