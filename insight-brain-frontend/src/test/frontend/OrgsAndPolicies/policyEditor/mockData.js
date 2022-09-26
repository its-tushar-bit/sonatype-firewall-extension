/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export const conditionType = [
  {
    enabled: true,
    threatCategory: 'QUALITY',
    valueTypeId: 'AgeInDaysValueType',
    valueHint: 'Enter term',
    autoUnquarantineSupported: false,
    supportedOperators: ['older than', 'younger than'],
    name: 'Age',
    id: 'AgeInDays',
  },
];

export const actionStage = [
  { stageTypeId: 'proxy', stageName: 'Proxy' },
  { stageTypeId: 'develop', stageName: 'Develop' },
  { stageTypeId: 'source', stageName: 'Source' },
  { stageTypeId: 'build', stageName: 'Build' },
  { stageTypeId: 'stage-release', stageName: 'Stage Release' },
  { stageTypeId: 'release', stageName: 'Release' },
  { stageTypeId: 'operate', stageName: 'Operate' },
];

export const conditionValueType = [
  { dataType: 'Integer', allowMultiple: false, availableValues: null, id: 'AgeInDaysValueType' },
];

export const applicablePolicies = {
  organization: {
    ROOT_ORGANIZATION_ID: {
      policiesByOwner: [
        {
          ownerId: 'ROOT_ORGANIZATION_ID',
          ownerName: 'Root Organization',
          ownerType: 'organization',
          policies: [
            {
              id: '6e7b77f4e0dd4b62af6c25f051be7f78',
              name: 'Architecture-Cleanup',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 1,
              policyViolationGrandfatheringAllowed: true,
              constraints: [
                {
                  id: 'fe980110c55941ffa6317e8dc6ba3dfa',
                  name: 'Test components',
                  operator: 'OR',
                  conditions: [
                    {
                      conditionTypeId: 'Coordinates',
                      operator: 'match',
                      value: 'maven:junit:junit:*:*:*',
                      conditionIndex: 0,
                    },
                    {
                      conditionTypeId: 'Coordinates',
                      operator: 'match',
                      value: 'maven:ant:ant:*:*:*',
                      conditionIndex: 1,
                    },
                    {
                      conditionTypeId: 'Coordinates',
                      operator: 'match',
                      value: 'maven:org.apache.ant:ant:*:*:*',
                      conditionIndex: 2,
                    },
                    {
                      conditionTypeId: 'Coordinates',
                      operator: 'match',
                      value: 'maven:org.seleniumhq.selenium:*:*:*:*',
                      conditionIndex: 3,
                    },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: '2f49695abba44dfbb9de1f3ec87ba2b2',
              name: 'Architecture-Quality',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 1,
              policyViolationGrandfatheringAllowed: true,
              constraints: [
                {
                  id: 'f2a50ed36ec64d3fadc198a20aebed64',
                  name: 'Version is old',
                  operator: 'OR',
                  conditions: [
                    { conditionTypeId: 'AgeInDays', operator: 'older than', value: '1825', conditionIndex: 0 },
                  ],
                },
                {
                  id: '615c7d931fe044479f74c577db9d9581',
                  name: 'Version is unpopular',
                  operator: 'OR',
                  conditions: [
                    { conditionTypeId: 'RelativePopularity', operator: '<=', value: '10', conditionIndex: 0 },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: '15263322b1604e2ba0163016df6845a9',
              name: 'Component-Similar',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 7,
              policyViolationGrandfatheringAllowed: true,
              constraints: [
                {
                  id: 'c500f3e197ad4d17bb43b6da4790d4bb',
                  name: 'Unknown modification to component',
                  operator: 'AND',
                  conditions: [
                    { conditionTypeId: 'MatchState', operator: 'is', value: 'similar', conditionIndex: 0 },
                    {
                      conditionTypeId: 'Coordinates',
                      operator: 'do not match',
                      value: 'maven:org.eclipse.*:*:*:*:*',
                      conditionIndex: 1,
                    },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: '71e6585bb3ce4629b7647c8e393c5b90',
              name: 'Component-Unknown',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 2,
              policyViolationGrandfatheringAllowed: true,
              constraints: [
                {
                  id: '59b5aa8ddcf24f738f146985b6b37eef',
                  name: 'Unknown 3rd party component',
                  operator: 'AND',
                  conditions: [
                    { conditionTypeId: 'MatchState', operator: 'is', value: 'unknown', conditionIndex: 0 },
                    { conditionTypeId: 'Proprietary', operator: 'is false', value: null, conditionIndex: 1 },
                    {
                      conditionTypeId: 'DataSource',
                      operator: 'has support for',
                      value: 'identity',
                      conditionIndex: 2,
                    },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: '19445684df4f4df39df80c39686ea6b8',
              name: 'Custom Policy',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 9,
              policyViolationGrandfatheringAllowed: false,
              constraints: [
                {
                  id: 'dd3a399782cf4575b8af9bdca598fb6d',
                  name: 's',
                  operator: 'OR',
                  conditions: [
                    { conditionTypeId: 'AgeInDays', operator: 'older than', value: '1095', conditionIndex: 0 },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: 'aa3f4d00bf6945809ff1315941dbaeb3',
              name: 'Integrity-Rating',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 9,
              policyViolationGrandfatheringAllowed: false,
              constraints: [
                {
                  id: 'c8c4ba643e9544e9a3c2891dc61824d8',
                  name: 'Pending integrity rating',
                  operator: 'OR',
                  conditions: [{ conditionTypeId: 'IntegrityRating', operator: 'is', value: '2', conditionIndex: 0 }],
                },
                {
                  id: '4faa34fca9284a3b8df3e7462b0f37dc',
                  name: 'Suspicious integrity rating',
                  operator: 'OR',
                  conditions: [{ conditionTypeId: 'IntegrityRating', operator: 'is', value: '1', conditionIndex: 0 }],
                },
              ],
              actions: { proxy: 'fail' },
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: '9d5c30f793a54446a9601cf36c18e9e3',
              name: 'License-Banned',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 10,
              policyViolationGrandfatheringAllowed: false,
              constraints: [
                {
                  id: 'e42e5a263ebd4c0da8df3503e583cad3',
                  name: 'Age Constarint',
                  operator: 'OR',
                  conditions: [
                    { conditionTypeId: 'AgeInDays', operator: 'older than', value: '365', conditionIndex: 0 },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: true,
              policyActionsOverrides: { '05602dd5ba934c318ad011ca4e4f5cfe': { proxy: 'warn' } },
            },
            {
              id: '741ae19c406b4843b5393818808b9a3f',
              name: 'License-Commercial',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 7,
              policyViolationGrandfatheringAllowed: true,
              constraints: [
                {
                  id: 'd4f40734d68d4fc4a5133d0b8bef99b1',
                  name: 'License containing commercial terms detected',
                  operator: 'OR',
                  conditions: [
                    {
                      conditionTypeId: 'License Threat Group',
                      operator: 'is',
                      value: '79330a1a6e83476f9360bf58a7712746',
                      conditionIndex: 0,
                    },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: '6ba8065aa4ff4b05abbaa4e3eb0cf5a0',
              name: 'License-Copyleft',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 8,
              policyViolationGrandfatheringAllowed: false,
              constraints: [
                {
                  id: 'cc7cdfa2ce394f7d89e3ad5afe5ad04b',
                  name: 'License containing Copyleft terms detected',
                  operator: 'OR',
                  conditions: [
                    {
                      conditionTypeId: 'License Threat Group',
                      operator: 'is',
                      value: '3a59a91a13d144f1b20a629c905d987b',
                      conditionIndex: 0,
                    },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: '15f557b2f6034faa862b2f85311becb3',
              name: 'License-Modified Weak Copyleft',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 5,
              policyViolationGrandfatheringAllowed: true,
              constraints: [
                {
                  id: '41d7e9c7d8a94c33bf4fdb44604f163f',
                  name: 'Modified source code & license containing Weak Copyleft terms detected.',
                  operator: 'AND',
                  conditions: [
                    {
                      conditionTypeId: 'License Threat Group',
                      operator: 'is',
                      value: 'c71243540b594c87a435fbfa5d08ec38',
                      conditionIndex: 0,
                    },
                    { conditionTypeId: 'MatchState', operator: 'is', value: 'similar', conditionIndex: 1 },
                    {
                      conditionTypeId: 'Coordinates',
                      operator: 'do not match',
                      value: 'maven:org.eclipse.*:*:*:*:*',
                      conditionIndex: 2,
                    },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: 'e21f3514fada490fa7fb5bcfd82c288b',
              name: 'License-None',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 9,
              policyViolationGrandfatheringAllowed: false,
              constraints: [
                {
                  id: '2859dc55039a49779169529668d7d89e',
                  name: 'No source available; nothing declared',
                  operator: 'AND',
                  conditions: [
                    { conditionTypeId: 'License', operator: 'is', value: 'No-Sources', conditionIndex: 0 },
                    { conditionTypeId: 'License', operator: 'is', value: 'Not-Declared', conditionIndex: 1 },
                  ],
                },
                {
                  id: '48fdd5620dd24c928fa82e60400cda67',
                  name: 'No licenses in supplied source; nothing declared',
                  operator: 'AND',
                  conditions: [
                    { conditionTypeId: 'License', operator: 'is', value: 'No-Source-License', conditionIndex: 0 },
                    { conditionTypeId: 'License', operator: 'is', value: 'Not-Declared', conditionIndex: 1 },
                  ],
                },
                {
                  id: 'e35cad245c0b410890279c4fd9cf3b39',
                  name: 'Contact Sonatype Support - observed license issue: Not Provided',
                  operator: 'AND',
                  conditions: [
                    { conditionTypeId: 'License', operator: 'is', value: 'UNSPECIFIED', conditionIndex: 0 },
                    { conditionTypeId: 'License', operator: 'is', value: 'Not-Declared', conditionIndex: 1 },
                  ],
                },
                {
                  id: '7fb1293523e34896ada112aaf5fc8f7f',
                  name: 'Contact Sonatype Support - declared license issue: Not Provided NS',
                  operator: 'AND',
                  conditions: [
                    { conditionTypeId: 'License', operator: 'is', value: 'No-Sources', conditionIndex: 0 },
                    { conditionTypeId: 'License', operator: 'is', value: 'UNSPECIFIED', conditionIndex: 1 },
                  ],
                },
                {
                  id: '79cea8793ebb402a83406d3cc74a61e6',
                  name: 'Contact Sonatype Support - declared license issue: Not Provided NSL',
                  operator: 'AND',
                  conditions: [
                    { conditionTypeId: 'License', operator: 'is', value: 'No-Source-License', conditionIndex: 0 },
                    { conditionTypeId: 'License', operator: 'is', value: 'UNSPECIFIED', conditionIndex: 1 },
                  ],
                },
                {
                  id: 'f40778ce73b54a3abc53cb6b0c1af922',
                  name: 'Source license not available; no declaration available',
                  operator: 'AND',
                  conditions: [
                    { conditionTypeId: 'License', operator: 'is', value: 'Not-Supported', conditionIndex: 0 },
                    { conditionTypeId: 'License', operator: 'is', value: 'Not-Declared', conditionIndex: 1 },
                  ],
                },
                {
                  id: 'f234d5bf950c4e94b97f0ed0e0e5935e',
                  name: 'Contact Sonatype Support - declared license issue: Not Provided',
                  operator: 'AND',
                  conditions: [
                    { conditionTypeId: 'License', operator: 'is', value: 'UNSPECIFIED', conditionIndex: 0 },
                    { conditionTypeId: 'License', operator: 'is', value: 'Not-Supported', conditionIndex: 1 },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: '87fcc415a76b4969830febd7b9e33e98',
              name: 'License-Non Standard',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 5,
              policyViolationGrandfatheringAllowed: true,
              constraints: [
                {
                  id: '63a09275952e4aa1bd316ec5c9f9f3f2',
                  name: 'License containing non standard terms detected. Legal review required.',
                  operator: 'OR',
                  conditions: [
                    {
                      conditionTypeId: 'License Threat Group',
                      operator: 'is',
                      value: 'f12d6d2259a1461ca68186dc2cf91caf',
                      conditionIndex: 0,
                    },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: '7170d22553c840a49cb4e603d35d13c1',
              name: 'License-Threat Not Assigned',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 7,
              policyViolationGrandfatheringAllowed: true,
              constraints: [
                {
                  id: '79430a7e6c5a491ab93deff03331e660',
                  name: 'License threat group has not been assigned',
                  operator: 'AND',
                  conditions: [
                    {
                      conditionTypeId: 'License Threat Group',
                      operator: 'is',
                      value: 'UNASSIGNED_LICENSE_THREAT_GROUP_ID',
                      conditionIndex: 0,
                    },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: '2a1cb71651d14a60b0fa77ef829f5ec0',
              name: 'Security-Critical',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 10,
              policyViolationGrandfatheringAllowed: false,
              constraints: [
                {
                  id: '5c0a0f8f90a14474b218337882174c83',
                  name: 'Age Constraint',
                  operator: 'OR',
                  conditions: [
                    { conditionTypeId: 'AgeInDays', operator: 'older than', value: '730', conditionIndex: 0 },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: 'bbbddfc5b6104bd88c351e511f2773a3',
              name: 'Security-High',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 9,
              policyViolationGrandfatheringAllowed: false,
              constraints: [
                {
                  id: '7c4e15b1d50543a681859c220feafa6b',
                  name: 'High risk CVSS score',
                  operator: 'AND',
                  conditions: [
                    { conditionTypeId: 'SecurityVulnerabilitySeverity', operator: '>=', value: '7', conditionIndex: 0 },
                    { conditionTypeId: 'SecurityVulnerabilitySeverity', operator: '<', value: '9', conditionIndex: 1 },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: 'abe230cd77a94d58b8444f7594f56d62',
              name: 'Security-Low',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 3,
              policyViolationGrandfatheringAllowed: true,
              constraints: [
                {
                  id: 'f3053dfc430a4ceab7837761acd7fca8',
                  name: 'Low risk CVSS score',
                  operator: 'AND',
                  conditions: [
                    { conditionTypeId: 'SecurityVulnerabilitySeverity', operator: '>=', value: '0', conditionIndex: 0 },
                    { conditionTypeId: 'SecurityVulnerabilitySeverity', operator: '<', value: '4', conditionIndex: 1 },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: '12f2086417ab44f9a63ba5e91786c570',
              name: 'Security-Malicious',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 10,
              policyViolationGrandfatheringAllowed: false,
              constraints: [
                {
                  id: 'a8485c2b06b04e1facb3d75736238e6a',
                  name: 'Malicious vulnerability category',
                  operator: 'AND',
                  conditions: [
                    { conditionTypeId: 'AgeInDays', operator: 'older than', value: '1095', conditionIndex: 0 },
                  ],
                },
                {
                  id: 'a8485c2b06b04e1facb3d75736238e6e',
                  name: 'Malicious vulnerability category',
                  operator: 'AND',
                  conditions: [{ conditionTypeId: 'AgeInDays', operator: 'older than', value: '4', conditionIndex: 0 }],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: true,
              policyActionsOverrides: null,
            },
            {
              id: '23926a7504af45cfa1ef062c46bfa0ff',
              name: 'Security-Medium',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 7,
              policyViolationGrandfatheringAllowed: true,
              constraints: [
                {
                  id: 'e88abc1901c2433aa83317a163e78ba6',
                  name: 'Medium risk CVSS score',
                  operator: 'AND',
                  conditions: [
                    { conditionTypeId: 'SecurityVulnerabilitySeverity', operator: '>=', value: '4', conditionIndex: 0 },
                    { conditionTypeId: 'SecurityVulnerabilitySeverity', operator: '<', value: '7', conditionIndex: 1 },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: 'f7cbc5b0ca6448db9e97fc8dd6417aeb',
              name: 'Security-Namespace Conflict',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 10,
              policyViolationGrandfatheringAllowed: false,
              constraints: [
                {
                  id: 'f0bcc4e0078048d3a3c5b852eb32b5ac',
                  name: '3rd-party component name conflicts with proprietary component name',
                  operator: 'AND',
                  conditions: [
                    {
                      conditionTypeId: 'ProprietaryNameConflict',
                      operator: 'is present',
                      value: null,
                      conditionIndex: 0,
                    },
                  ],
                },
              ],
              actions: { proxy: 'fail' },
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
          ],
          policyTags: [
            {
              id: 'd07a20676a6444e28dfd5dcec244bfa2',
              policyId: '741ae19c406b4843b5393818808b9a3f',
              tagId: '838a1a1930394199bf0a8f93bf183d5e',
            },
            {
              id: '0b44f71e5419493cb60247d2a4e145d6',
              policyId: '6ba8065aa4ff4b05abbaa4e3eb0cf5a0',
              tagId: '838a1a1930394199bf0a8f93bf183d5e',
            },
            {
              id: 'e039d52c97cd4f508eceff273d272335',
              policyId: '15f557b2f6034faa862b2f85311becb3',
              tagId: '838a1a1930394199bf0a8f93bf183d5e',
            },
            {
              id: '65967f6961f94e15bbbfc0e959dc2905',
              policyId: 'e21f3514fada490fa7fb5bcfd82c288b',
              tagId: '838a1a1930394199bf0a8f93bf183d5e',
            },
            {
              id: '0069b777e9ae42fe9cb7d0fc696501c2',
              policyId: '87fcc415a76b4969830febd7b9e33e98',
              tagId: '838a1a1930394199bf0a8f93bf183d5e',
            },
            {
              id: 'd66b601c892b43d293922e844814ea61',
              policyId: '741ae19c406b4843b5393818808b9a3f',
              tagId: 'a0531d2e64954a42ae667c8c3ef8002c',
            },
            {
              id: 'b21b16dee6ed457cb856d7f07082a71f',
              policyId: 'e21f3514fada490fa7fb5bcfd82c288b',
              tagId: 'a0531d2e64954a42ae667c8c3ef8002c',
            },
            {
              id: '0de5440ea43e4b6aa560c79a98f7e0f4',
              policyId: '9d5c30f793a54446a9601cf36c18e9e3',
              tagId: 'a0531d2e64954a42ae667c8c3ef8002c',
            },
            {
              id: 'c5a770abd55f4fe4bb94f81b1f42056c',
              policyId: '9d5c30f793a54446a9601cf36c18e9e3',
              tagId: '68b3e3aa1768480fa86a303804f0b68a',
            },
          ],
        },
      ],
    },
    '05602dd5ba934c318ad011ca4e4f5cfe': {
      policiesByOwner: [
        {
          ownerId: '05602dd5ba934c318ad011ca4e4f5cfe',
          ownerName: 'fabian',
          ownerType: 'organization',
          policies: [
            {
              id: '38c6bffbe90042398c71b662b25b3394',
              name: 'Custom Pol',
              ownerId: '05602dd5ba934c318ad011ca4e4f5cfe',
              threatLevel: 5,
              policyViolationGrandfatheringAllowed: false,
              constraints: [
                {
                  id: 'aa9baccd1e9f4a1ca2f45363a29f8624',
                  name: 'Constrain name',
                  operator: 'OR',
                  conditions: [
                    { conditionTypeId: 'AgeInDays', operator: 'older than', value: '365', conditionIndex: 0 },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
          ],
          policyTags: [],
        },
        {
          ownerId: 'ROOT_ORGANIZATION_ID',
          ownerName: 'Root Organization',
          ownerType: 'organization',
          policies: [
            {
              id: '6e7b77f4e0dd4b62af6c25f051be7f78',
              name: 'Architecture-Cleanup',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 1,
              policyViolationGrandfatheringAllowed: true,
              constraints: [
                {
                  id: 'fe980110c55941ffa6317e8dc6ba3dfa',
                  name: 'Test components',
                  operator: 'OR',
                  conditions: [
                    {
                      conditionTypeId: 'Coordinates',
                      operator: 'match',
                      value: 'maven:junit:junit:*:*:*',
                      conditionIndex: 0,
                    },
                    {
                      conditionTypeId: 'Coordinates',
                      operator: 'match',
                      value: 'maven:ant:ant:*:*:*',
                      conditionIndex: 1,
                    },
                    {
                      conditionTypeId: 'Coordinates',
                      operator: 'match',
                      value: 'maven:org.apache.ant:ant:*:*:*',
                      conditionIndex: 2,
                    },
                    {
                      conditionTypeId: 'Coordinates',
                      operator: 'match',
                      value: 'maven:org.seleniumhq.selenium:*:*:*:*',
                      conditionIndex: 3,
                    },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: '2f49695abba44dfbb9de1f3ec87ba2b2',
              name: 'Architecture-Quality',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 1,
              policyViolationGrandfatheringAllowed: true,
              constraints: [
                {
                  id: 'f2a50ed36ec64d3fadc198a20aebed64',
                  name: 'Version is old',
                  operator: 'OR',
                  conditions: [
                    { conditionTypeId: 'AgeInDays', operator: 'older than', value: '1825', conditionIndex: 0 },
                  ],
                },
                {
                  id: '615c7d931fe044479f74c577db9d9581',
                  name: 'Version is unpopular',
                  operator: 'OR',
                  conditions: [
                    { conditionTypeId: 'RelativePopularity', operator: '<=', value: '10', conditionIndex: 0 },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: '15263322b1604e2ba0163016df6845a9',
              name: 'Component-Similar',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 7,
              policyViolationGrandfatheringAllowed: true,
              constraints: [
                {
                  id: 'c500f3e197ad4d17bb43b6da4790d4bb',
                  name: 'Unknown modification to component',
                  operator: 'AND',
                  conditions: [
                    { conditionTypeId: 'MatchState', operator: 'is', value: 'similar', conditionIndex: 0 },
                    {
                      conditionTypeId: 'Coordinates',
                      operator: 'do not match',
                      value: 'maven:org.eclipse.*:*:*:*:*',
                      conditionIndex: 1,
                    },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: '71e6585bb3ce4629b7647c8e393c5b90',
              name: 'Component-Unknown',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 2,
              policyViolationGrandfatheringAllowed: true,
              constraints: [
                {
                  id: '59b5aa8ddcf24f738f146985b6b37eef',
                  name: 'Unknown 3rd party component',
                  operator: 'AND',
                  conditions: [
                    { conditionTypeId: 'MatchState', operator: 'is', value: 'unknown', conditionIndex: 0 },
                    { conditionTypeId: 'Proprietary', operator: 'is false', value: null, conditionIndex: 1 },
                    {
                      conditionTypeId: 'DataSource',
                      operator: 'has support for',
                      value: 'identity',
                      conditionIndex: 2,
                    },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: '19445684df4f4df39df80c39686ea6b8',
              name: 'Custom Policy',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 9,
              policyViolationGrandfatheringAllowed: false,
              constraints: [
                {
                  id: 'dd3a399782cf4575b8af9bdca598fb6d',
                  name: 's',
                  operator: 'OR',
                  conditions: [
                    { conditionTypeId: 'AgeInDays', operator: 'older than', value: '1095', conditionIndex: 0 },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: 'aa3f4d00bf6945809ff1315941dbaeb3',
              name: 'Integrity-Rating',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 9,
              policyViolationGrandfatheringAllowed: false,
              constraints: [
                {
                  id: 'c8c4ba643e9544e9a3c2891dc61824d8',
                  name: 'Pending integrity rating',
                  operator: 'OR',
                  conditions: [{ conditionTypeId: 'IntegrityRating', operator: 'is', value: '2', conditionIndex: 0 }],
                },
                {
                  id: '4faa34fca9284a3b8df3e7462b0f37dc',
                  name: 'Suspicious integrity rating',
                  operator: 'OR',
                  conditions: [{ conditionTypeId: 'IntegrityRating', operator: 'is', value: '1', conditionIndex: 0 }],
                },
              ],
              actions: { proxy: 'fail' },
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: '9d5c30f793a54446a9601cf36c18e9e3',
              name: 'License-Banned',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 10,
              policyViolationGrandfatheringAllowed: false,
              constraints: [
                {
                  id: 'e42e5a263ebd4c0da8df3503e583cad3',
                  name: 'Age Constarint',
                  operator: 'OR',
                  conditions: [
                    { conditionTypeId: 'AgeInDays', operator: 'older than', value: '365', conditionIndex: 0 },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: true,
              policyActionsOverrides: { '05602dd5ba934c318ad011ca4e4f5cfe': { proxy: 'warn' } },
            },
            {
              id: '741ae19c406b4843b5393818808b9a3f',
              name: 'License-Commercial',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 7,
              policyViolationGrandfatheringAllowed: true,
              constraints: [
                {
                  id: 'd4f40734d68d4fc4a5133d0b8bef99b1',
                  name: 'License containing commercial terms detected',
                  operator: 'OR',
                  conditions: [
                    {
                      conditionTypeId: 'License Threat Group',
                      operator: 'is',
                      value: '79330a1a6e83476f9360bf58a7712746',
                      conditionIndex: 0,
                    },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: '6ba8065aa4ff4b05abbaa4e3eb0cf5a0',
              name: 'License-Copyleft',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 8,
              policyViolationGrandfatheringAllowed: false,
              constraints: [
                {
                  id: 'cc7cdfa2ce394f7d89e3ad5afe5ad04b',
                  name: 'License containing Copyleft terms detected',
                  operator: 'OR',
                  conditions: [
                    {
                      conditionTypeId: 'License Threat Group',
                      operator: 'is',
                      value: '3a59a91a13d144f1b20a629c905d987b',
                      conditionIndex: 0,
                    },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: '15f557b2f6034faa862b2f85311becb3',
              name: 'License-Modified Weak Copyleft',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 5,
              policyViolationGrandfatheringAllowed: true,
              constraints: [
                {
                  id: '41d7e9c7d8a94c33bf4fdb44604f163f',
                  name: 'Modified source code & license containing Weak Copyleft terms detected.',
                  operator: 'AND',
                  conditions: [
                    {
                      conditionTypeId: 'License Threat Group',
                      operator: 'is',
                      value: 'c71243540b594c87a435fbfa5d08ec38',
                      conditionIndex: 0,
                    },
                    { conditionTypeId: 'MatchState', operator: 'is', value: 'similar', conditionIndex: 1 },
                    {
                      conditionTypeId: 'Coordinates',
                      operator: 'do not match',
                      value: 'maven:org.eclipse.*:*:*:*:*',
                      conditionIndex: 2,
                    },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: 'e21f3514fada490fa7fb5bcfd82c288b',
              name: 'License-None',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 9,
              policyViolationGrandfatheringAllowed: false,
              constraints: [
                {
                  id: '2859dc55039a49779169529668d7d89e',
                  name: 'No source available; nothing declared',
                  operator: 'AND',
                  conditions: [
                    { conditionTypeId: 'License', operator: 'is', value: 'No-Sources', conditionIndex: 0 },
                    { conditionTypeId: 'License', operator: 'is', value: 'Not-Declared', conditionIndex: 1 },
                  ],
                },
                {
                  id: '48fdd5620dd24c928fa82e60400cda67',
                  name: 'No licenses in supplied source; nothing declared',
                  operator: 'AND',
                  conditions: [
                    { conditionTypeId: 'License', operator: 'is', value: 'No-Source-License', conditionIndex: 0 },
                    { conditionTypeId: 'License', operator: 'is', value: 'Not-Declared', conditionIndex: 1 },
                  ],
                },
                {
                  id: 'e35cad245c0b410890279c4fd9cf3b39',
                  name: 'Contact Sonatype Support - observed license issue: Not Provided',
                  operator: 'AND',
                  conditions: [
                    { conditionTypeId: 'License', operator: 'is', value: 'UNSPECIFIED', conditionIndex: 0 },
                    { conditionTypeId: 'License', operator: 'is', value: 'Not-Declared', conditionIndex: 1 },
                  ],
                },
                {
                  id: '7fb1293523e34896ada112aaf5fc8f7f',
                  name: 'Contact Sonatype Support - declared license issue: Not Provided NS',
                  operator: 'AND',
                  conditions: [
                    { conditionTypeId: 'License', operator: 'is', value: 'No-Sources', conditionIndex: 0 },
                    { conditionTypeId: 'License', operator: 'is', value: 'UNSPECIFIED', conditionIndex: 1 },
                  ],
                },
                {
                  id: '79cea8793ebb402a83406d3cc74a61e6',
                  name: 'Contact Sonatype Support - declared license issue: Not Provided NSL',
                  operator: 'AND',
                  conditions: [
                    { conditionTypeId: 'License', operator: 'is', value: 'No-Source-License', conditionIndex: 0 },
                    { conditionTypeId: 'License', operator: 'is', value: 'UNSPECIFIED', conditionIndex: 1 },
                  ],
                },
                {
                  id: 'f40778ce73b54a3abc53cb6b0c1af922',
                  name: 'Source license not available; no declaration available',
                  operator: 'AND',
                  conditions: [
                    { conditionTypeId: 'License', operator: 'is', value: 'Not-Supported', conditionIndex: 0 },
                    { conditionTypeId: 'License', operator: 'is', value: 'Not-Declared', conditionIndex: 1 },
                  ],
                },
                {
                  id: 'f234d5bf950c4e94b97f0ed0e0e5935e',
                  name: 'Contact Sonatype Support - declared license issue: Not Provided',
                  operator: 'AND',
                  conditions: [
                    { conditionTypeId: 'License', operator: 'is', value: 'UNSPECIFIED', conditionIndex: 0 },
                    { conditionTypeId: 'License', operator: 'is', value: 'Not-Supported', conditionIndex: 1 },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: '87fcc415a76b4969830febd7b9e33e98',
              name: 'License-Non Standard',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 5,
              policyViolationGrandfatheringAllowed: true,
              constraints: [
                {
                  id: '63a09275952e4aa1bd316ec5c9f9f3f2',
                  name: 'License containing non standard terms detected. Legal review required.',
                  operator: 'OR',
                  conditions: [
                    {
                      conditionTypeId: 'License Threat Group',
                      operator: 'is',
                      value: 'f12d6d2259a1461ca68186dc2cf91caf',
                      conditionIndex: 0,
                    },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: '7170d22553c840a49cb4e603d35d13c1',
              name: 'License-Threat Not Assigned',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 7,
              policyViolationGrandfatheringAllowed: true,
              constraints: [
                {
                  id: '79430a7e6c5a491ab93deff03331e660',
                  name: 'License threat group has not been assigned',
                  operator: 'AND',
                  conditions: [
                    {
                      conditionTypeId: 'License Threat Group',
                      operator: 'is',
                      value: 'UNASSIGNED_LICENSE_THREAT_GROUP_ID',
                      conditionIndex: 0,
                    },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: '2a1cb71651d14a60b0fa77ef829f5ec0',
              name: 'Security-Critical',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 10,
              policyViolationGrandfatheringAllowed: false,
              constraints: [
                {
                  id: '5c0a0f8f90a14474b218337882174c83',
                  name: 'Age Constraint',
                  operator: 'OR',
                  conditions: [
                    { conditionTypeId: 'AgeInDays', operator: 'older than', value: '730', conditionIndex: 0 },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: 'bbbddfc5b6104bd88c351e511f2773a3',
              name: 'Security-High',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 9,
              policyViolationGrandfatheringAllowed: false,
              constraints: [
                {
                  id: '7c4e15b1d50543a681859c220feafa6b',
                  name: 'High risk CVSS score',
                  operator: 'AND',
                  conditions: [
                    { conditionTypeId: 'SecurityVulnerabilitySeverity', operator: '>=', value: '7', conditionIndex: 0 },
                    { conditionTypeId: 'SecurityVulnerabilitySeverity', operator: '<', value: '9', conditionIndex: 1 },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: 'abe230cd77a94d58b8444f7594f56d62',
              name: 'Security-Low',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 3,
              policyViolationGrandfatheringAllowed: true,
              constraints: [
                {
                  id: 'f3053dfc430a4ceab7837761acd7fca8',
                  name: 'Low risk CVSS score',
                  operator: 'AND',
                  conditions: [
                    { conditionTypeId: 'SecurityVulnerabilitySeverity', operator: '>=', value: '0', conditionIndex: 0 },
                    { conditionTypeId: 'SecurityVulnerabilitySeverity', operator: '<', value: '4', conditionIndex: 1 },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: '12f2086417ab44f9a63ba5e91786c570',
              name: 'Security-Malicious',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 10,
              policyViolationGrandfatheringAllowed: false,
              constraints: [
                {
                  id: 'a8485c2b06b04e1facb3d75736238e6a',
                  name: 'Malicious vulnerability category',
                  operator: 'AND',
                  conditions: [
                    { conditionTypeId: 'AgeInDays', operator: 'older than', value: '1095', conditionIndex: 0 },
                  ],
                },
                {
                  id: 'a8485c2b06b04e1facb3d75736238e6e',
                  name: 'Malicious vulnerability category',
                  operator: 'AND',
                  conditions: [{ conditionTypeId: 'AgeInDays', operator: 'older than', value: '4', conditionIndex: 0 }],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: true,
              policyActionsOverrides: null,
            },
            {
              id: '23926a7504af45cfa1ef062c46bfa0ff',
              name: 'Security-Medium',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 7,
              policyViolationGrandfatheringAllowed: true,
              constraints: [
                {
                  id: 'e88abc1901c2433aa83317a163e78ba6',
                  name: 'Medium risk CVSS score',
                  operator: 'AND',
                  conditions: [
                    { conditionTypeId: 'SecurityVulnerabilitySeverity', operator: '>=', value: '4', conditionIndex: 0 },
                    { conditionTypeId: 'SecurityVulnerabilitySeverity', operator: '<', value: '7', conditionIndex: 1 },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: 'f7cbc5b0ca6448db9e97fc8dd6417aeb',
              name: 'Security-Namespace Conflict',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 10,
              policyViolationGrandfatheringAllowed: false,
              constraints: [
                {
                  id: 'f0bcc4e0078048d3a3c5b852eb32b5ac',
                  name: '3rd-party component name conflicts with proprietary component name',
                  operator: 'AND',
                  conditions: [
                    {
                      conditionTypeId: 'ProprietaryNameConflict',
                      operator: 'is present',
                      value: null,
                      conditionIndex: 0,
                    },
                  ],
                },
              ],
              actions: { proxy: 'fail' },
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
          ],
          policyTags: [
            {
              id: 'd07a20676a6444e28dfd5dcec244bfa2',
              policyId: '741ae19c406b4843b5393818808b9a3f',
              tagId: '838a1a1930394199bf0a8f93bf183d5e',
            },
            {
              id: '0b44f71e5419493cb60247d2a4e145d6',
              policyId: '6ba8065aa4ff4b05abbaa4e3eb0cf5a0',
              tagId: '838a1a1930394199bf0a8f93bf183d5e',
            },
            {
              id: 'e039d52c97cd4f508eceff273d272335',
              policyId: '15f557b2f6034faa862b2f85311becb3',
              tagId: '838a1a1930394199bf0a8f93bf183d5e',
            },
            {
              id: '65967f6961f94e15bbbfc0e959dc2905',
              policyId: 'e21f3514fada490fa7fb5bcfd82c288b',
              tagId: '838a1a1930394199bf0a8f93bf183d5e',
            },
            {
              id: '0069b777e9ae42fe9cb7d0fc696501c2',
              policyId: '87fcc415a76b4969830febd7b9e33e98',
              tagId: '838a1a1930394199bf0a8f93bf183d5e',
            },
            {
              id: 'd66b601c892b43d293922e844814ea61',
              policyId: '741ae19c406b4843b5393818808b9a3f',
              tagId: 'a0531d2e64954a42ae667c8c3ef8002c',
            },
            {
              id: 'b21b16dee6ed457cb856d7f07082a71f',
              policyId: 'e21f3514fada490fa7fb5bcfd82c288b',
              tagId: 'a0531d2e64954a42ae667c8c3ef8002c',
            },
            {
              id: '0de5440ea43e4b6aa560c79a98f7e0f4',
              policyId: '9d5c30f793a54446a9601cf36c18e9e3',
              tagId: 'a0531d2e64954a42ae667c8c3ef8002c',
            },
            {
              id: 'c5a770abd55f4fe4bb94f81b1f42056c',
              policyId: '9d5c30f793a54446a9601cf36c18e9e3',
              tagId: '68b3e3aa1768480fa86a303804f0b68a',
            },
          ],
        },
      ],
    },
  },
  application: {
    testapp: {
      policiesByOwner: [
        {
          ownerId: '5339c74cb96045dd80d257e8a168b476',
          ownerName: 'Test App',
          ownerType: 'application',
          policies: [],
          policyTags: [],
        },
        {
          ownerId: '05602dd5ba934c318ad011ca4e4f5cfe',
          ownerName: 'fabian',
          ownerType: 'organization',
          policies: [
            {
              id: '38c6bffbe90042398c71b662b25b3394',
              name: 'Custom Pol',
              ownerId: '05602dd5ba934c318ad011ca4e4f5cfe',
              threatLevel: 5,
              policyViolationGrandfatheringAllowed: false,
              constraints: [
                {
                  id: 'aa9baccd1e9f4a1ca2f45363a29f8624',
                  name: 'Constrain name',
                  operator: 'OR',
                  conditions: [
                    { conditionTypeId: 'AgeInDays', operator: 'older than', value: '365', conditionIndex: 0 },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
          ],
          policyTags: [],
        },
        {
          ownerId: 'ROOT_ORGANIZATION_ID',
          ownerName: 'Root Organization',
          ownerType: 'organization',
          policies: [
            {
              id: '6e7b77f4e0dd4b62af6c25f051be7f78',
              name: 'Architecture-Cleanup',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 1,
              policyViolationGrandfatheringAllowed: true,
              constraints: [
                {
                  id: 'fe980110c55941ffa6317e8dc6ba3dfa',
                  name: 'Test components',
                  operator: 'OR',
                  conditions: [
                    {
                      conditionTypeId: 'Coordinates',
                      operator: 'match',
                      value: 'maven:junit:junit:*:*:*',
                      conditionIndex: 0,
                    },
                    {
                      conditionTypeId: 'Coordinates',
                      operator: 'match',
                      value: 'maven:ant:ant:*:*:*',
                      conditionIndex: 1,
                    },
                    {
                      conditionTypeId: 'Coordinates',
                      operator: 'match',
                      value: 'maven:org.apache.ant:ant:*:*:*',
                      conditionIndex: 2,
                    },
                    {
                      conditionTypeId: 'Coordinates',
                      operator: 'match',
                      value: 'maven:org.seleniumhq.selenium:*:*:*:*',
                      conditionIndex: 3,
                    },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: '2f49695abba44dfbb9de1f3ec87ba2b2',
              name: 'Architecture-Quality',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 1,
              policyViolationGrandfatheringAllowed: true,
              constraints: [
                {
                  id: 'f2a50ed36ec64d3fadc198a20aebed64',
                  name: 'Version is old',
                  operator: 'OR',
                  conditions: [
                    { conditionTypeId: 'AgeInDays', operator: 'older than', value: '1825', conditionIndex: 0 },
                  ],
                },
                {
                  id: '615c7d931fe044479f74c577db9d9581',
                  name: 'Version is unpopular',
                  operator: 'OR',
                  conditions: [
                    { conditionTypeId: 'RelativePopularity', operator: '<=', value: '10', conditionIndex: 0 },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: '15263322b1604e2ba0163016df6845a9',
              name: 'Component-Similar',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 7,
              policyViolationGrandfatheringAllowed: true,
              constraints: [
                {
                  id: 'c500f3e197ad4d17bb43b6da4790d4bb',
                  name: 'Unknown modification to component',
                  operator: 'AND',
                  conditions: [
                    { conditionTypeId: 'MatchState', operator: 'is', value: 'similar', conditionIndex: 0 },
                    {
                      conditionTypeId: 'Coordinates',
                      operator: 'do not match',
                      value: 'maven:org.eclipse.*:*:*:*:*',
                      conditionIndex: 1,
                    },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: '71e6585bb3ce4629b7647c8e393c5b90',
              name: 'Component-Unknown',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 2,
              policyViolationGrandfatheringAllowed: true,
              constraints: [
                {
                  id: '59b5aa8ddcf24f738f146985b6b37eef',
                  name: 'Unknown 3rd party component',
                  operator: 'AND',
                  conditions: [
                    { conditionTypeId: 'MatchState', operator: 'is', value: 'unknown', conditionIndex: 0 },
                    { conditionTypeId: 'Proprietary', operator: 'is false', value: null, conditionIndex: 1 },
                    {
                      conditionTypeId: 'DataSource',
                      operator: 'has support for',
                      value: 'identity',
                      conditionIndex: 2,
                    },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: '19445684df4f4df39df80c39686ea6b8',
              name: 'Custom Policy',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 9,
              policyViolationGrandfatheringAllowed: false,
              constraints: [
                {
                  id: 'dd3a399782cf4575b8af9bdca598fb6d',
                  name: 's',
                  operator: 'OR',
                  conditions: [
                    { conditionTypeId: 'AgeInDays', operator: 'older than', value: '1095', conditionIndex: 0 },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: 'aa3f4d00bf6945809ff1315941dbaeb3',
              name: 'Integrity-Rating',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 9,
              policyViolationGrandfatheringAllowed: false,
              constraints: [
                {
                  id: 'c8c4ba643e9544e9a3c2891dc61824d8',
                  name: 'Pending integrity rating',
                  operator: 'OR',
                  conditions: [{ conditionTypeId: 'IntegrityRating', operator: 'is', value: '2', conditionIndex: 0 }],
                },
                {
                  id: '4faa34fca9284a3b8df3e7462b0f37dc',
                  name: 'Suspicious integrity rating',
                  operator: 'OR',
                  conditions: [{ conditionTypeId: 'IntegrityRating', operator: 'is', value: '1', conditionIndex: 0 }],
                },
              ],
              actions: { proxy: 'fail' },
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: '7170d22553c840a49cb4e603d35d13c1',
              name: 'License-Threat Not Assigned',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 7,
              policyViolationGrandfatheringAllowed: true,
              constraints: [
                {
                  id: '79430a7e6c5a491ab93deff03331e660',
                  name: 'License threat group has not been assigned',
                  operator: 'AND',
                  conditions: [
                    {
                      conditionTypeId: 'License Threat Group',
                      operator: 'is',
                      value: 'UNASSIGNED_LICENSE_THREAT_GROUP_ID',
                      conditionIndex: 0,
                    },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: '2a1cb71651d14a60b0fa77ef829f5ec0',
              name: 'Security-Critical',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 10,
              policyViolationGrandfatheringAllowed: false,
              constraints: [
                {
                  id: '5c0a0f8f90a14474b218337882174c83',
                  name: 'Age Constraint',
                  operator: 'OR',
                  conditions: [
                    { conditionTypeId: 'AgeInDays', operator: 'older than', value: '730', conditionIndex: 0 },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: 'bbbddfc5b6104bd88c351e511f2773a3',
              name: 'Security-High',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 9,
              policyViolationGrandfatheringAllowed: false,
              constraints: [
                {
                  id: '7c4e15b1d50543a681859c220feafa6b',
                  name: 'High risk CVSS score',
                  operator: 'AND',
                  conditions: [
                    { conditionTypeId: 'SecurityVulnerabilitySeverity', operator: '>=', value: '7', conditionIndex: 0 },
                    { conditionTypeId: 'SecurityVulnerabilitySeverity', operator: '<', value: '9', conditionIndex: 1 },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: 'abe230cd77a94d58b8444f7594f56d62',
              name: 'Security-Low',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 3,
              policyViolationGrandfatheringAllowed: true,
              constraints: [
                {
                  id: 'f3053dfc430a4ceab7837761acd7fca8',
                  name: 'Low risk CVSS score',
                  operator: 'AND',
                  conditions: [
                    { conditionTypeId: 'SecurityVulnerabilitySeverity', operator: '>=', value: '0', conditionIndex: 0 },
                    { conditionTypeId: 'SecurityVulnerabilitySeverity', operator: '<', value: '4', conditionIndex: 1 },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: '12f2086417ab44f9a63ba5e91786c570',
              name: 'Security-Malicious',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 10,
              policyViolationGrandfatheringAllowed: false,
              constraints: [
                {
                  id: 'a8485c2b06b04e1facb3d75736238e6a',
                  name: 'Malicious vulnerability category',
                  operator: 'AND',
                  conditions: [
                    { conditionTypeId: 'AgeInDays', operator: 'older than', value: '1095', conditionIndex: 0 },
                  ],
                },
                {
                  id: 'a8485c2b06b04e1facb3d75736238e6e',
                  name: 'Malicious vulnerability category',
                  operator: 'AND',
                  conditions: [{ conditionTypeId: 'AgeInDays', operator: 'older than', value: '4', conditionIndex: 0 }],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: true,
              policyActionsOverrides: null,
            },
            {
              id: '23926a7504af45cfa1ef062c46bfa0ff',
              name: 'Security-Medium',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 7,
              policyViolationGrandfatheringAllowed: true,
              constraints: [
                {
                  id: 'e88abc1901c2433aa83317a163e78ba6',
                  name: 'Medium risk CVSS score',
                  operator: 'AND',
                  conditions: [
                    { conditionTypeId: 'SecurityVulnerabilitySeverity', operator: '>=', value: '4', conditionIndex: 0 },
                    { conditionTypeId: 'SecurityVulnerabilitySeverity', operator: '<', value: '7', conditionIndex: 1 },
                  ],
                },
              ],
              actions: {},
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
            {
              id: 'f7cbc5b0ca6448db9e97fc8dd6417aeb',
              name: 'Security-Namespace Conflict',
              ownerId: 'ROOT_ORGANIZATION_ID',
              threatLevel: 10,
              policyViolationGrandfatheringAllowed: false,
              constraints: [
                {
                  id: 'f0bcc4e0078048d3a3c5b852eb32b5ac',
                  name: '3rd-party component name conflicts with proprietary component name',
                  operator: 'AND',
                  conditions: [
                    {
                      conditionTypeId: 'ProprietaryNameConflict',
                      operator: 'is present',
                      value: null,
                      conditionIndex: 0,
                    },
                  ],
                },
              ],
              actions: { proxy: 'fail' },
              notifications: {
                userNotifications: [],
                roleNotifications: [],
                jiraNotifications: [],
                webhookNotifications: [],
              },
              policyActionsOverrideAllowed: false,
              policyActionsOverrides: null,
            },
          ],
          policyTags: [
            {
              id: 'd07a20676a6444e28dfd5dcec244bfa2',
              policyId: '741ae19c406b4843b5393818808b9a3f',
              tagId: '838a1a1930394199bf0a8f93bf183d5e',
            },
            {
              id: '0b44f71e5419493cb60247d2a4e145d6',
              policyId: '6ba8065aa4ff4b05abbaa4e3eb0cf5a0',
              tagId: '838a1a1930394199bf0a8f93bf183d5e',
            },
            {
              id: 'e039d52c97cd4f508eceff273d272335',
              policyId: '15f557b2f6034faa862b2f85311becb3',
              tagId: '838a1a1930394199bf0a8f93bf183d5e',
            },
            {
              id: '65967f6961f94e15bbbfc0e959dc2905',
              policyId: 'e21f3514fada490fa7fb5bcfd82c288b',
              tagId: '838a1a1930394199bf0a8f93bf183d5e',
            },
            {
              id: '0069b777e9ae42fe9cb7d0fc696501c2',
              policyId: '87fcc415a76b4969830febd7b9e33e98',
              tagId: '838a1a1930394199bf0a8f93bf183d5e',
            },
            {
              id: 'd66b601c892b43d293922e844814ea61',
              policyId: '741ae19c406b4843b5393818808b9a3f',
              tagId: 'a0531d2e64954a42ae667c8c3ef8002c',
            },
            {
              id: 'b21b16dee6ed457cb856d7f07082a71f',
              policyId: 'e21f3514fada490fa7fb5bcfd82c288b',
              tagId: 'a0531d2e64954a42ae667c8c3ef8002c',
            },
            {
              id: '0de5440ea43e4b6aa560c79a98f7e0f4',
              policyId: '9d5c30f793a54446a9601cf36c18e9e3',
              tagId: 'a0531d2e64954a42ae667c8c3ef8002c',
            },
            {
              id: 'c5a770abd55f4fe4bb94f81b1f42056c',
              policyId: '9d5c30f793a54446a9601cf36c18e9e3',
              tagId: '68b3e3aa1768480fa86a303804f0b68a',
            },
          ],
        },
      ],
    },
  },
};

export const applicableCategories = {
  organization: {
    ROOT_ORGANIZATION_ID: {
      applicationCategoriesByOwner: [
        {
          ownerId: 'ROOT_ORGANIZATION_ID',
          ownerName: 'Root Organization',
          ownerType: 'organization',
          applicationCategories: [
            {
              id: '838a1a1930394199bf0a8f93bf183d5e',
              name: 'Distributed',
              description: 'Applications that are provided for consumption outside the company',
              organizationId: 'ROOT_ORGANIZATION_ID',
              color: 'light-green',
            },
            {
              id: 'a0531d2e64954a42ae667c8c3ef8002c',
              name: 'Hosted',
              description: 'Applications that are hosted such as services or software as a service.',
              organizationId: 'ROOT_ORGANIZATION_ID',
              color: 'dark-purple',
            },
            {
              id: '8f4679e999f247018b39346c7f72f87a',
              name: 'Internal',
              description: 'Applications that are used only by your employees',
              organizationId: 'ROOT_ORGANIZATION_ID',
              color: 'yellow',
            },
            {
              id: '68b3e3aa1768480fa86a303804f0b68a',
              name: 'New Category',
              description: 'Desc',
              organizationId: 'ROOT_ORGANIZATION_ID',
              color: 'light-blue',
            },
          ],
        },
      ],
    },
    '05602dd5ba934c318ad011ca4e4f5cfe': {
      applicationCategoriesByOwner: [
        {
          ownerId: '05602dd5ba934c318ad011ca4e4f5cfe',
          ownerName: 'fabian',
          ownerType: 'organization',
          applicationCategories: [
            {
              id: 'd24a39ec4a5e48ac817947995ca0fe02',
              name: 'h',
              description: 'h',
              organizationId: '05602dd5ba934c318ad011ca4e4f5cfe',
              color: 'light-green',
            },
          ],
        },
        {
          ownerId: 'ROOT_ORGANIZATION_ID',
          ownerName: 'Root Organization',
          ownerType: 'organization',
          applicationCategories: [
            {
              id: '838a1a1930394199bf0a8f93bf183d5e',
              name: 'Distributed',
              description: 'Applications that are provided for consumption outside the company',
              organizationId: 'ROOT_ORGANIZATION_ID',
              color: 'light-green',
            },
            {
              id: 'a0531d2e64954a42ae667c8c3ef8002c',
              name: 'Hosted',
              description: 'Applications that are hosted such as services or software as a service.',
              organizationId: 'ROOT_ORGANIZATION_ID',
              color: 'dark-purple',
            },
            {
              id: '8f4679e999f247018b39346c7f72f87a',
              name: 'Internal',
              description: 'Applications that are used only by your employees',
              organizationId: 'ROOT_ORGANIZATION_ID',
              color: 'yellow',
            },
            {
              id: '68b3e3aa1768480fa86a303804f0b68a',
              name: 'New Category',
              description: 'Desc',
              organizationId: 'ROOT_ORGANIZATION_ID',
              color: 'light-blue',
            },
          ],
        },
      ],
    },
  },
  application: {
    testapp: {
      applicationCategoriesByOwner: [
        { ownerId: 'teastapp', ownerName: 'Test App', ownerType: 'application', applicationCategories: [] },
        {
          ownerId: '05602dd5ba934c318ad011ca4e4f5cfe',
          ownerName: 'fabian',
          ownerType: 'organization',
          applicationCategories: [
            {
              id: 'd24a39ec4a5e48ac817947995ca0fe02',
              name: 'h',
              description: 'h',
              organizationId: '05602dd5ba934c318ad011ca4e4f5cfe',
              color: 'light-green',
            },
          ],
        },
        {
          ownerId: 'ROOT_ORGANIZATION_ID',
          ownerName: 'Root Organization',
          ownerType: 'organization',
          applicationCategories: [
            {
              id: '838a1a1930394199bf0a8f93bf183d5e',
              name: 'Distributed',
              description: 'Applications that are provided for consumption outside the company',
              organizationId: 'ROOT_ORGANIZATION_ID',
              color: 'light-green',
            },
            {
              id: 'a0531d2e64954a42ae667c8c3ef8002c',
              name: 'Hosted',
              description: 'Applications that are hosted such as services or software as a service.',
              organizationId: 'ROOT_ORGANIZATION_ID',
              color: 'dark-purple',
            },
            {
              id: '8f4679e999f247018b39346c7f72f87a',
              name: 'Internal',
              description: 'Applications that are used only by your employees',
              organizationId: 'ROOT_ORGANIZATION_ID',
              color: 'yellow',
            },
            {
              id: '68b3e3aa1768480fa86a303804f0b68a',
              name: 'New Category',
              description: 'Desc',
              organizationId: 'ROOT_ORGANIZATION_ID',
              color: 'light-blue',
            },
          ],
        },
      ],
    },
  },
};

export const policyTag = {
  organization: {
    ROOT_ORGANIZATION_ID: {
      '9d5c30f793a54446a9601cf36c18e9e3': [
        {
          id: '68b3e3aa1768480fa86a303804f0b68a',
          organizationId: 'ROOT_ORGANIZATION_ID',
          name: 'New Category',
          nameLowercaseNoWhitespace: 'newcategory',
          description: 'Desc',
          color: 'light-blue',
        },
        {
          id: 'a0531d2e64954a42ae667c8c3ef8002c',
          organizationId: 'ROOT_ORGANIZATION_ID',
          name: 'Hosted',
          nameLowercaseNoWhitespace: 'hosted',
          description: 'Applications that are hosted such as services or software as a service.',
          color: 'dark-purple',
        },
      ],
      '2a1cb71651d14a60b0fa77ef829f5ec0': [],
    },
    '05602dd5ba934c318ad011ca4e4f5cfe': {
      '9d5c30f793a54446a9601cf36c18e9e3': [
        {
          id: '68b3e3aa1768480fa86a303804f0b68a',
          organizationId: 'ROOT_ORGANIZATION_ID',
          name: 'New Category',
          nameLowercaseNoWhitespace: 'newcategory',
          description: 'Desc',
          color: 'light-blue',
        },
        {
          id: 'a0531d2e64954a42ae667c8c3ef8002c',
          organizationId: 'ROOT_ORGANIZATION_ID',
          name: 'Hosted',
          nameLowercaseNoWhitespace: 'hosted',
          description: 'Applications that are hosted such as services or software as a service.',
          color: 'dark-purple',
        },
      ],
      '2a1cb71651d14a60b0fa77ef829f5ec0': [],
    },
  },
  application: {
    testapp: {
      '9d5c30f793a54446a9601cf36c18e9e3': [],
      '2a1cb71651d14a60b0fa77ef829f5ec0': [],
      '12f2086417ab44f9a63ba5e91786c570': [],
    },
  },
};

export const savedPolicy = {
  id: '12f2086417ab44f9a63ba5e91786c570',
  name: 'Security-Malicious',
  ownerId: 'ROOT_ORGANIZATION_ID',
  threatLevel: 10,
  policyViolationGrandfatheringAllowed: false,
  constraints: [
    {
      id: 'a8485c2b06b04e1facb3d75736238e6a',
      name: 'Malicious vulnerability category',
      operator: 'AND',
      conditions: [{ conditionTypeId: 'AgeInDays', operator: 'older than', value: '1095', conditionIndex: 0 }],
    },
    {
      id: 'a8485c2b06b04e1facb3d75736238e6e',
      name: 'Malicious vulnerability category',
      operator: 'AND',
      conditions: [{ conditionTypeId: 'AgeInDays', operator: 'older than', value: '4', conditionIndex: 0 }],
    },
  ],
  actions: {},
  notifications: {
    userNotifications: [],
    roleNotifications: [],
    jiraNotifications: [],
    webhookNotifications: [],
  },
  policyActionsOverrideAllowed: true,
  policyActionsOverrides: null,
};
