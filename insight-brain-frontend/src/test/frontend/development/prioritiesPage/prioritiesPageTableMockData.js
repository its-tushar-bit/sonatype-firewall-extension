/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export const mockData = [
  {
    policyThreatLevel: 10,
    policyName: 'Security-Critical',
    policyThreatCategory: 'SECURITY',
    constraints: [
      {
        constraintId: '607861e2f2f343a7ab4857f522fed304',
        constraintName: 'Critical risk CVSS score',
        constraintOperator: 'AND',
        conditions: [
          {
            conditionType: 'SecurityVulnerabilitySeverity',
            conditionSummary: 'Security Vulnerability Severity >= 9',
            conditionReason: 'Found security vulnerability CVE-2023-40743 with severity >= 9 (severity = 9.8)',
            conditionTriggerReference: {
              value: 'CVE-2023-40743',
              type: 'SECURITY_VULNERABILITY_REFID',
            },
          },
        ],
      },
    ],
    actions: [{ actionType: 'fail', actionSummary: 'Build Failed' }],
    hash: '892c772f7c486b3c09d2',
    displayName: {
      parts: [
        {
          field: 'Group',
          value: 'axis',
        },
        {
          value: ' : ',
        },
        {
          field: 'Artifact',
          value: 'axis',
        },
        {
          value: ' : ',
        },
        {
          field: 'Version',
          value: '1.2',
        },
      ],
      name: 'axis',
    },
    directDependency: true,
    innerSource: false,
    derivedDependencyType: 'direct',
  },
  {
    policyThreatLevel: 8,
    policyName: 'Security-High',
    policyThreatCategory: 'SECURITY',
    policyViolationId: 'e50a817c313d41f3b5a44e51248d5bad',
    constraints: [
      {
        constraintId: '607861e2f2f343a7ab4857f522fed304',
        constraintName: 'High risk CVSS score',
        constraintOperator: 'AND',
        conditions: [
          {
            conditionType: 'SecurityVulnerabilitySeverity',
            conditionSummary: 'Security Vulnerability Severity >= 8',
            conditionReason: 'Found security vulnerability CVE-2017-7525 with severity >= 9 (severity = 9.8)',
            conditionTriggerReference: {
              value: 'CVE-2017-7525',
              type: 'SECURITY_VULNERABILITY_REFID',
            },
          },
        ],
      },
    ],
    actions: [{ actionType: 'warn', actionSummary: 'Build Warning' }],
    hash: 'cf05e1449bccc5dae87b',
    displayName: {
      parts: [
        {
          field: 'Group',
          value: 'com.fasterxml.jackson.core',
        },
        {
          value: ' : ',
        },
        {
          field: 'Artifact',
          value: 'jackson-databind',
        },
        {
          value: ' : ',
        },
        {
          field: 'Version',
          value: '2.0.4',
        },
      ],
      name: 'jackson-databind',
    },
    directDependency: true,
    innerSource: false,
    hasDependencyTypeInfo: true,
    dependencyInfo: {},
    derivedDependencyType: 'direct',
    derivedInnerSource: false,
  },
  {
    policyThreatLevel: 7,
    policyName: 'Security-Medium',
    policyThreatCategory: 'SECURITY',
    policyViolationId: '29d4c9513eca4cb3a6fc327853d66899',
    constraints: [
      {
        constraintId: '607861e2f2f343a7ab4857f522fed304',
        constraintName: 'Medium risk CVSS score',
        constraintOperator: 'AND',
        conditions: [
          {
            conditionType: 'SecurityVulnerabilitySeverity',
            conditionSummary: 'Security Vulnerability Severity >= 7',
            conditionReason: 'Found security vulnerability sonatype-2015-0002 with severity >= 9 (severity = 9.0)',
            conditionTriggerReference: {
              value: 'sonatype-2015-0002',
              type: 'SECURITY_VULNERABILITY_REFID',
            },
          },
        ],
      },
    ],
    actions: [{ actionType: 'warn', actionSummary: 'Build Warning' }],
    hash: '40fb048097caeacdb11d',
    displayName: {
      parts: [
        {
          field: 'Group',
          value: 'commons-collections',
        },
        {
          value: ' : ',
        },
        {
          field: 'Artifact',
          value: 'commons-collections',
        },
        {
          value: ' : ',
        },
        {
          field: 'Version',
          value: '3.1',
        },
      ],
      name: 'commons-collections',
    },
    directDependency: false,
    innerSource: false,
    parentComponentPurls: [
      'pkg:maven/commons-beanutils/commons-beanutils@1.6?type=jar',
      'pkg:maven/commons-digester/commons-digester@1.4.1?type=jar',
    ],
    hasDependencyTypeInfo: true,
    dependencyInfo: {
      rootAncestors: [
        'maven:artifactId\u001ftiles-core\u001eclassifier\u001f\u001eextension\u001fjar\u001egroupId\u001forg.apache.tiles\u001eversion\u001f2.2.2',
      ],
    },
    derivedDependencyType: 'transitive',
    derivedInnerSource: false,
  },
  {
    policyThreatLevel: 7,
    policyName: 'Security-Medium',
    policyThreatCategory: 'SECURITY',
    policyViolationId: '68aa7b65b60a4a97ad577655e3eb555f',
    constraints: [
      {
        constraintId: '607861e2f2f343a7ab4857f522fed304',
        constraintName: 'Medium risk CVSS score',
        constraintOperator: 'AND',
        conditions: [
          {
            conditionType: 'SecurityVulnerabilitySeverity',
            conditionSummary: 'Security Vulnerability Severity >= 7',
            conditionReason: 'Found security vulnerability CVE-2007-4575 with severity >= 9 (severity = 9.3)',
            conditionTriggerReference: {
              value: 'CVE-2007-4575',
              type: 'SECURITY_VULNERABILITY_REFID',
            },
          },
        ],
      },
    ],
    actions: [],
    hash: '20554954120b3cc9f088',
    displayName: {
      parts: [
        {
          field: 'Group',
          value: 'hsqldb',
        },
        {
          value: ' : ',
        },
        {
          field: 'Artifact',
          value: 'hsqldb',
        },
        {
          value: ' : ',
        },
        {
          field: 'Version',
          value: '1.8.0.7',
        },
      ],
      name: 'hsqldb',
    },
    directDependency: true,
    innerSource: false,
    hasDependencyTypeInfo: true,
    dependencyInfo: {},
    derivedDependencyType: 'direct',
    derivedInnerSource: false,
  },
];
