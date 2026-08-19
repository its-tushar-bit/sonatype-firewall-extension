/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default {
  getApplicablePolicies: function (ownerType, ownerId, ownerName) {
    return {
      policiesByOwner: [
        {
          ownerId: ownerId || 'f3cea033acf84984ae08d9250db4aa7b',
          ownerName: ownerName || 'Org1 Heh',
          ownerType: ownerType || 'organization',
          policies: [
            {
              id: '4d6b4ac75ea148b2aa6ca36e6899cc78',
              name: 'Org Policy 3',
              ownerId: ownerId || 'f3cea033acf84984ae08d9250db4aa7b',
              enabled: true,
              threatLevel: 0,
              constraints: [
                {
                  id: 'd4fe6780471e4543bcb0e28d0e122b69',
                  name: 'Unpopular',
                  enabled: true,
                  operator: 'OR',
                  conditions: [
                    {
                      conditionTypeId: 'RelativePopularity',
                      operator: '<',
                      value: '10',
                    },
                  ],
                },
              ],
              actions: {
                develop: [{ actionTypeId: 'warn', target: null }],
                build: [{ actionTypeId: 'fail', target: null }],
                'stage-release': [{ actionTypeId: 'fail', target: null }],
                release: [{ actionTypeId: 'warn', target: null }],
                operate: [{ actionTypeId: 'warn', target: null }],
                proxy: [{ actionTypeId: 'warn', target: null }],
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
  getConditionTypeUrl: function () {
    return [
      {
        id: 'Label',
        name: 'Label',
        supportedOperators: ['is', 'is not'],
        threatCategory: 'OTHER',
        valueTypeId: 'LabelValueType',
      },
      {
        id: 'License',
        name: 'License',
        supportedOperators: ['is', 'is not'],
        threatCategory: 'LICENSE',
        valueTypeId: 'LicenseValueType',
      },
      {
        id: 'LicenseStatus',
        name: 'License Status',
        supportedOperators: ['is', 'is not'],
        threatCategory: 'LICENSE',
        valueTypeId: 'LicenseStatusValueType',
      },
      {
        id: 'License Threat Group',
        name: 'License Threat Group',
        supportedOperators: ['is', 'is not'],
        threatCategory: 'LICENSE',
        valueTypeId: 'LicenseThreatGroupValueType',
      },
      {
        id: 'License Threat Group Level',
        name: 'License Threat Group Level',
        supportedOperators: ['<=', '>='],
        threatCategory: 'LICENSE',
        valueTypeId: 'IntegerValueType',
      },
      {
        id: 'SecurityVulnerabilitySeverity',
        name: 'Security Vulnerability Severity',
        supportedOperators: ['=', '<', '<=', '>', '>='],
        threatCategory: 'SECURITY',
        valueTypeId: 'FloatValueType',
      },
      {
        id: 'SecurityVulnerabilityStatus',
        name: 'Security Vulnerability Status',
        supportedOperators: ['is', 'is not'],
        threatCategory: 'SECURITY',
        valueTypeId: 'SecurityVulnerabilityStatusValueType',
      },
      {
        id: 'RelativePopularity',
        name: 'Relative Popularity (Percentage)',
        supportedOperators: ['=', '<', '<=', '>', '>='],
        threatCategory: 'QUALITY',
        valueTypeId: 'PercentageValueType',
      },
      {
        id: 'AgeInDays',
        name: 'Age',
        supportedOperators: ['older than', 'younger than'],
        threatCategory: 'QUALITY',
        valueTypeId: 'AgeInDaysValueType',
      },
      {
        id: 'MatchState',
        name: 'Match State',
        supportedOperators: ['is', 'is not'],
        threatCategory: 'OTHER',
        valueTypeId: 'MatchStateValueType',
      },
      {
        id: 'Coordinates',
        name: 'Coordinates',
        supportedOperators: ['match', 'do not match'],
        threatCategory: 'OTHER',
        valueTypeId: 'CoordinatesValueType',
      },
      {
        id: 'Proprietary',
        name: 'Proprietary',
        supportedOperators: ['is true', 'is false'],
        threatCategory: 'OTHER',
        valueTypeId: null,
      },
      {
        id: 'IdentificationSource',
        name: 'Identification Source',
        supportedOperators: ['is', 'is not'],
        threatCategory: 'OTHER',
        valueTypeId: 'IdentificationSourceValueType',
      },
    ];
  },
};
