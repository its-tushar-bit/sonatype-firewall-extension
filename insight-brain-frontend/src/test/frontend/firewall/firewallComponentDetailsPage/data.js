/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export const componentDetailsData = {
  hash: '684aeca90db2a55234f5',
  matchState: 'exact',
  declaredLicenses: [
    {
      licenseId: 'Apache-2.0',
      licenseName: 'Apache-2.0',
    },
    {
      licenseId: 'UNKNOWN',
      licenseName: 'Non-Standard',
    },
  ],
  observedLicenses: [
    {
      licenseId: 'Apache-2.0',
      licenseName: 'Apache-2.0',
    },
  ],
  overriddenLicenses: [],
  policyMaxThreatLevelsByCategory: {
    security: 7,
    other: 1,
    quality: 1,
  },
  effectiveLicenses: [
    {
      licenseId: 'Apache-2.0',
      licenseName: 'Apache-2.0',
    },
    {
      licenseId: 'UNKNOWN',
      licenseName: 'Non-Standard',
    },
  ],
  effectiveLicenseStatus: null,
  catalogDate: 1132682795000,
  relativePopularity: 0,
  securityVulnerabilities: [
    {
      refId: 'sonatype-2018-0330',
      severity: 6.5,
      source: 'sonatype',
      summary: 'Apache Ant - Path Traversal issue in archive extraction',
      status: 'Open',
      url: null,
      vulnerabilityCategories: ['data'],
      aliases: [],
      cwe: '22',
      cvssVectorSource: 'sonatype_cvss_3',
      cvssVector: 'CVSS:3.0/AV:N/AC:H/PR:N/UI:N/S:C/C:L/I:L/A:L',
    },
    {
      refId: 'CVE-2020-1945',
      severity: 6.3,
      source: 'cve',
      summary:
        'Apache Ant 1.1 to 1.9.14 and 1.10.0 to 1.10.7 uses the default temporary directory identified by the Java system property java.io.tmpdir for several tasks and may thus leak sensitive information. The fixcrlf and replaceregexp tasks also copy files from the temporary directory back into the build tree allowing an attacker to inject modified source files into the build process.',
      status: 'Open',
      url: null,
      vulnerabilityCategories: ['data', 'functional'],
      aliases: [],
      cwe: '668',
      cvssVectorSource: 'cve_cvss_3',
      cvssVector: 'CVSS:3.1/AV:L/AC:H/PR:L/UI:N/S:U/C:H/I:H/A:N',
    },
    {
      refId: 'CVE-2021-36374',
      severity: 5.5,
      source: 'cve',
      summary:
        'When reading a specially crafted ZIP archive, or a derived formats, an Apache Ant build can be made to allocate large amounts of memory that leads to an out of memory error, even for small inputs. This can be used to disrupt builds using Apache Ant. Commonly used derived formats from ZIP archives are for instance JAR files and many office files. Apache Ant prior to 1.9.16 and 1.10.11 were affected.',
      status: 'Open',
      url: null,
      vulnerabilityCategories: ['data'],
      aliases: [],
      cwe: '400',
      cvssVectorSource: 'cve_cvss_3',
      cvssVector: 'CVSS:3.1/AV:L/AC:L/PR:N/UI:R/S:U/C:N/I:N/A:H',
    },
    {
      refId: 'CVE-2012-2098',
      severity: 5.0,
      source: 'cve',
      summary:
        'Algorithmic complexity vulnerability in the sorting algorithms in bzip2 compressing stream (BZip2CompressorOutputStream) in Apache Commons Compress before 1.4.1 allows remote attackers to cause a denial of service (CPU consumption) via a file with many repeating inputs.',
      status: 'Open',
      url: null,
      vulnerabilityCategories: ['data', 'functional'],
      aliases: [],
      cwe: '310',
      cvssVectorSource: 'cve_cvss_2',
      cvssVector: 'AV:N/AC:L/Au:N/C:N/I:N/A:P',
    },
  ],
  website: null,
  policyAlerts: [
    {
      trigger: {
        policyId: '2bf5384868f048ffa6c600ae20898561',
        policyName: 'Security-Medium',
        threatLevel: 7,
        componentFacts: [
          {
            componentIdentifier: {
              format: 'maven',
              coordinates: {
                artifactId: 'ant',
                classifier: '',
                extension: 'jar',
                groupId: 'ant',
                version: '1.6.1',
              },
            },
            hash: '684aeca90db2a55234f5',
            constraintFacts: [
              {
                constraintId: '79159e3489e9437a901a57a4fc449dd1',
                constraintName: 'Medium risk CVSS score',
                operatorName: 'AND',
                conditionFacts: [
                  {
                    conditionTypeId: 'SecurityVulnerabilitySeverity',
                    conditionIndex: 0,
                    summary: 'Security Vulnerability Severity >= 4',
                    reason: 'Found security vulnerability CVE-2012-2098 with severity >= 4 (severity = 5.0)',
                    reference: {
                      value: 'CVE-2012-2098',
                      type: 'SECURITY_VULNERABILITY_REFID',
                    },
                    triggerJson: '{"conditionIndex":0,"trigger":{"refId":"CVE-2012-2098","severity":5.0}}',
                  },
                  {
                    conditionTypeId: 'SecurityVulnerabilitySeverity',
                    conditionIndex: 1,
                    summary: 'Security Vulnerability Severity < 7',
                    reason: 'Found security vulnerability CVE-2012-2098 with severity < 7 (severity = 5.0)',
                    reference: {
                      value: 'CVE-2012-2098',
                      type: 'SECURITY_VULNERABILITY_REFID',
                    },
                    triggerJson: '{"conditionIndex":1,"trigger":{"refId":"CVE-2012-2098","severity":5.0}}',
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
                  value: '1.6.1',
                },
              ],
              name: 'ant',
            },
          },
        ],
      },
      actions: [],
    },
    {
      trigger: {
        policyId: '2bf5384868f048ffa6c600ae20898561',
        policyName: 'Security-Medium',
        threatLevel: 7,
        componentFacts: [
          {
            componentIdentifier: {
              format: 'maven',
              coordinates: {
                artifactId: 'ant',
                classifier: '',
                extension: 'jar',
                groupId: 'ant',
                version: '1.6.1',
              },
            },
            hash: '684aeca90db2a55234f5',
            constraintFacts: [
              {
                constraintId: '79159e3489e9437a901a57a4fc449dd1',
                constraintName: 'Medium risk CVSS score',
                operatorName: 'AND',
                conditionFacts: [
                  {
                    conditionTypeId: 'SecurityVulnerabilitySeverity',
                    conditionIndex: 0,
                    summary: 'Security Vulnerability Severity >= 4',
                    reason: 'Found security vulnerability CVE-2020-1945 with severity >= 4 (severity = 6.3)',
                    reference: {
                      value: 'CVE-2020-1945',
                      type: 'SECURITY_VULNERABILITY_REFID',
                    },
                    triggerJson: '{"conditionIndex":0,"trigger":{"refId":"CVE-2020-1945","severity":6.3}}',
                  },
                  {
                    conditionTypeId: 'SecurityVulnerabilitySeverity',
                    conditionIndex: 1,
                    summary: 'Security Vulnerability Severity < 7',
                    reason: 'Found security vulnerability CVE-2020-1945 with severity < 7 (severity = 6.3)',
                    reference: {
                      value: 'CVE-2020-1945',
                      type: 'SECURITY_VULNERABILITY_REFID',
                    },
                    triggerJson: '{"conditionIndex":1,"trigger":{"refId":"CVE-2020-1945","severity":6.3}}',
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
                  value: '1.6.1',
                },
              ],
              name: 'ant',
            },
          },
        ],
      },
      actions: [],
    },
    {
      trigger: {
        policyId: '2bf5384868f048ffa6c600ae20898561',
        policyName: 'Security-Medium',
        threatLevel: 7,
        componentFacts: [
          {
            componentIdentifier: {
              format: 'maven',
              coordinates: {
                artifactId: 'ant',
                classifier: '',
                extension: 'jar',
                groupId: 'ant',
                version: '1.6.1',
              },
            },
            hash: '684aeca90db2a55234f5',
            constraintFacts: [
              {
                constraintId: '79159e3489e9437a901a57a4fc449dd1',
                constraintName: 'Medium risk CVSS score',
                operatorName: 'AND',
                conditionFacts: [
                  {
                    conditionTypeId: 'SecurityVulnerabilitySeverity',
                    conditionIndex: 0,
                    summary: 'Security Vulnerability Severity >= 4',
                    reason: 'Found security vulnerability CVE-2021-36374 with severity >= 4 (severity = 5.5)',
                    reference: {
                      value: 'CVE-2021-36374',
                      type: 'SECURITY_VULNERABILITY_REFID',
                    },
                    triggerJson: '{"conditionIndex":0,"trigger":{"refId":"CVE-2021-36374","severity":5.5}}',
                  },
                  {
                    conditionTypeId: 'SecurityVulnerabilitySeverity',
                    conditionIndex: 1,
                    summary: 'Security Vulnerability Severity < 7',
                    reason: 'Found security vulnerability CVE-2021-36374 with severity < 7 (severity = 5.5)',
                    reference: {
                      value: 'CVE-2021-36374',
                      type: 'SECURITY_VULNERABILITY_REFID',
                    },
                    triggerJson: '{"conditionIndex":1,"trigger":{"refId":"CVE-2021-36374","severity":5.5}}',
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
                  value: '1.6.1',
                },
              ],
              name: 'ant',
            },
          },
        ],
      },
      actions: [],
    },
    {
      trigger: {
        policyId: '2bf5384868f048ffa6c600ae20898561',
        policyName: 'Security-Medium',
        threatLevel: 7,
        componentFacts: [
          {
            componentIdentifier: {
              format: 'maven',
              coordinates: {
                artifactId: 'ant',
                classifier: '',
                extension: 'jar',
                groupId: 'ant',
                version: '1.6.1',
              },
            },
            hash: '684aeca90db2a55234f5',
            constraintFacts: [
              {
                constraintId: '79159e3489e9437a901a57a4fc449dd1',
                constraintName: 'Medium risk CVSS score',
                operatorName: 'AND',
                conditionFacts: [
                  {
                    conditionTypeId: 'SecurityVulnerabilitySeverity',
                    conditionIndex: 0,
                    summary: 'Security Vulnerability Severity >= 4',
                    reason: 'Found security vulnerability sonatype-2018-0330 with severity >= 4 (severity = 6.5)',
                    reference: {
                      value: 'sonatype-2018-0330',
                      type: 'SECURITY_VULNERABILITY_REFID',
                    },
                    triggerJson: '{"conditionIndex":0,"trigger":{"refId":"sonatype-2018-0330","severity":6.5}}',
                  },
                  {
                    conditionTypeId: 'SecurityVulnerabilitySeverity',
                    conditionIndex: 1,
                    summary: 'Security Vulnerability Severity < 7',
                    reason: 'Found security vulnerability sonatype-2018-0330 with severity < 7 (severity = 6.5)',
                    reference: {
                      value: 'sonatype-2018-0330',
                      type: 'SECURITY_VULNERABILITY_REFID',
                    },
                    triggerJson: '{"conditionIndex":1,"trigger":{"refId":"sonatype-2018-0330","severity":6.5}}',
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
                  value: '1.6.1',
                },
              ],
              name: 'ant',
            },
          },
        ],
      },
      actions: [],
    },
    {
      trigger: {
        policyId: '364170b6b4134f7d9344324b2b020954',
        policyName: 'Architecture-Cleanup',
        threatLevel: 1,
        componentFacts: [
          {
            componentIdentifier: {
              format: 'maven',
              coordinates: {
                artifactId: 'ant',
                classifier: '',
                extension: 'jar',
                groupId: 'ant',
                version: '1.6.1',
              },
            },
            hash: '684aeca90db2a55234f5',
            constraintFacts: [
              {
                constraintId: '00b994379bf14f34af91ae06b3b0ccaf',
                constraintName: 'Test components',
                operatorName: 'OR',
                conditionFacts: [
                  {
                    conditionTypeId: 'Coordinates',
                    conditionIndex: 1,
                    summary: 'Coordinates match maven:ant:ant:*:*:*',
                    reason: 'Coordinates were ant : ant : 1.6.1 (match ant : ant : * : * : *)',
                    reference: null,
                    triggerJson: null,
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
                  value: '1.6.1',
                },
              ],
              name: 'ant',
            },
          },
        ],
      },
      actions: [],
    },
    {
      trigger: {
        policyId: '40e923f8957741f98d0813110681fdb3',
        policyName: 'Architecture-Quality',
        threatLevel: 1,
        componentFacts: [
          {
            componentIdentifier: {
              format: 'maven',
              coordinates: {
                artifactId: 'ant',
                classifier: '',
                extension: 'jar',
                groupId: 'ant',
                version: '1.6.1',
              },
            },
            hash: '684aeca90db2a55234f5',
            constraintFacts: [
              {
                constraintId: '48d8fa5b3a5b441481705c8085fc730c',
                constraintName: 'Version is old',
                operatorName: 'OR',
                conditionFacts: [
                  {
                    conditionTypeId: 'AgeInDays',
                    conditionIndex: 0,
                    summary: 'Age older than 1825 days',
                    reason: 'Found component older than 5 years',
                    reference: null,
                    triggerJson: null,
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
                  value: '1.6.1',
                },
              ],
              name: 'ant',
            },
          },
        ],
      },
      actions: [],
    },
    {
      trigger: {
        policyId: '40e923f8957741f98d0813110681fdb3',
        policyName: 'Architecture-Quality',
        threatLevel: 1,
        componentFacts: [
          {
            componentIdentifier: {
              format: 'maven',
              coordinates: {
                artifactId: 'ant',
                classifier: '',
                extension: 'jar',
                groupId: 'ant',
                version: '1.6.1',
              },
            },
            hash: '684aeca90db2a55234f5',
            constraintFacts: [
              {
                constraintId: '76cc74e30f5041da8956ec29d35cc339',
                constraintName: 'Version is unpopular',
                operatorName: 'OR',
                conditionFacts: [
                  {
                    conditionTypeId: 'RelativePopularity',
                    conditionIndex: 0,
                    summary: 'Relative Popularity (Percentage) <= 10',
                    reason: 'Relative popularity was <= 10% (relative popularity = 0%)',
                    reference: null,
                    triggerJson: null,
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
                  value: '1.6.1',
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
  licenseThreatLevel: 6,
  licenseThreatGroupNames: ['Non Standard'],
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
      version: '1.6.1',
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
  violatedPolicyCount: null,
  highestSecurityVulnerabilitySeverity: null,
  securityVulnerabilityCount: null,
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
        value: '1.6.1',
      },
    ],
    name: 'ant',
  },
  observedLicenseIds: ['Apache-2.0'],
  declaredLicenseIds: ['Apache-2.0', 'UNKNOWN'],
  artifactId: 'ant',
  groupId: 'ant',
  version: '1.6.1',
};

export const labelsData = {
  labelsByOwner: [
    {
      ownerId: 'ff7688303b844b08bd9854d3e53802ce',
      ownerName: 'maven-central',
      ownerType: 'repository',
      labels: [
        {
          id: '27ac8136009f4a6cbc198a2616a11154',
          ownerId: 'ff7688303b844b08bd9854d3e53802ce',
          label: 'Architecture-Blacklisted',
          labelLowercase: 'architecture-blacklisted',
          description: 'Components which have been blacklisted from use',
          color: 'orange',
        },
      ],
    },
  ],
};

export const policyViolationsData = [
  {
    policyViolationId: 'ee61c6a2c1464a4e8b65784f587b8303',
    componentIdentifier: {
      format: 'maven',
      coordinates: {
        artifactId: 'ant',
        classifier: '',
        extension: 'jar',
        groupId: 'ant',
        version: '1.6.1',
      },
    },
    componentDisplayName: {
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
          value: '1.6.1',
        },
      ],
      name: 'ant',
    },
    hash: '684aeca90db2a55234f5',
    policyId: '2bf5384868f048ffa6c600ae20898561',
    policyName: 'Security-Medium',
    policyOwner: {
      ownerId: 'ROOT_ORGANIZATION_ID',
      ownerName: 'Root Organization',
      ownerType: 'organization',
    },
    policyThreatLevel: 7,
    policyThreatCategory: 'SECURITY',
    constraints: [
      {
        constraintId: '79159e3489e9437a901a57a4fc449dd1',
        constraintName: 'Medium risk CVSS score',
        constraintOperator: 'AND',
        conditions: [
          {
            conditionType: 'SecurityVulnerabilitySeverity',
            conditionSummary: 'Security Vulnerability Severity >= 4',
            conditionReason: 'Found security vulnerability CVE-2012-2098 with severity >= 4 (severity = 5.0)',
            conditionTriggerReference: {
              value: 'CVE-2012-2098',
              type: 'SECURITY_VULNERABILITY_REFID',
            },
          },
          {
            conditionType: 'SecurityVulnerabilitySeverity',
            conditionSummary: 'Security Vulnerability Severity < 7',
            conditionReason: 'Found security vulnerability CVE-2012-2098 with severity < 7 (severity = 5.0)',
            conditionTriggerReference: {
              value: 'CVE-2012-2098',
              type: 'SECURITY_VULNERABILITY_REFID',
            },
          },
        ],
      },
    ],
    constraintFactsJson:
      '[{"constraintId":"79159e3489e9437a901a57a4fc449dd1","constraintName":"Medium risk CVSS score","operatorName":"AND","conditionFacts":[{"conditionTypeId":"SecurityVulnerabilitySeverity","conditionIndex":0,"summary":"Security Vulnerability Severity >= 4","reason":"Found security vulnerability CVE-2012-2098 with severity >= 4 (severity = 5.0)","reference":{"value":"CVE-2012-2098","type":"SECURITY_VULNERABILITY_REFID"},"triggerJson":"{\\"conditionIndex\\":0,\\"trigger\\":{\\"refId\\":\\"CVE-2012-2098\\",\\"severity\\":5.0}}"},{"conditionTypeId":"SecurityVulnerabilitySeverity","conditionIndex":1,"summary":"Security Vulnerability Severity < 7","reason":"Found security vulnerability CVE-2012-2098 with severity < 7 (severity = 5.0)","reference":{"value":"CVE-2012-2098","type":"SECURITY_VULNERABILITY_REFID"},"triggerJson":"{\\"conditionIndex\\":1,\\"trigger\\":{\\"refId\\":\\"CVE-2012-2098\\",\\"severity\\":5.0}}"}]}]',
    waived: false,
    policyActionTypeId: 'fail',
    lastReported: '2024-01-11T16:59:15.633-05:00',
  },
  {
    policyViolationId: '13ab35d88def4e168ec5a1043d13b2de',
    componentIdentifier: {
      format: 'maven',
      coordinates: {
        artifactId: 'ant',
        classifier: '',
        extension: 'jar',
        groupId: 'ant',
        version: '1.6.1',
      },
    },
    componentDisplayName: {
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
          value: '1.6.1',
        },
      ],
      name: 'ant',
    },
    hash: '684aeca90db2a55234f5',
    policyId: '2bf5384868f048ffa6c600ae20898561',
    policyName: 'Security-Medium',
    policyOwner: {
      ownerId: 'ROOT_ORGANIZATION_ID',
      ownerName: 'Root Organization',
      ownerType: 'organization',
    },
    policyThreatLevel: 7,
    policyThreatCategory: 'SECURITY',
    constraints: [
      {
        constraintId: '79159e3489e9437a901a57a4fc449dd1',
        constraintName: 'Medium risk CVSS score',
        constraintOperator: 'AND',
        conditions: [
          {
            conditionType: 'SecurityVulnerabilitySeverity',
            conditionSummary: 'Security Vulnerability Severity >= 4',
            conditionReason: 'Found security vulnerability CVE-2020-1945 with severity >= 4 (severity = 6.3)',
            conditionTriggerReference: {
              value: 'CVE-2020-1945',
              type: 'SECURITY_VULNERABILITY_REFID',
            },
          },
          {
            conditionType: 'SecurityVulnerabilitySeverity',
            conditionSummary: 'Security Vulnerability Severity < 7',
            conditionReason: 'Found security vulnerability CVE-2020-1945 with severity < 7 (severity = 6.3)',
            conditionTriggerReference: {
              value: 'CVE-2020-1945',
              type: 'SECURITY_VULNERABILITY_REFID',
            },
          },
        ],
      },
    ],
    constraintFactsJson:
      '[{"constraintId":"79159e3489e9437a901a57a4fc449dd1","constraintName":"Medium risk CVSS score","operatorName":"AND","conditionFacts":[{"conditionTypeId":"SecurityVulnerabilitySeverity","conditionIndex":0,"summary":"Security Vulnerability Severity >= 4","reason":"Found security vulnerability CVE-2020-1945 with severity >= 4 (severity = 6.3)","reference":{"value":"CVE-2020-1945","type":"SECURITY_VULNERABILITY_REFID"},"triggerJson":"{\\"conditionIndex\\":0,\\"trigger\\":{\\"refId\\":\\"CVE-2020-1945\\",\\"severity\\":6.3}}"},{"conditionTypeId":"SecurityVulnerabilitySeverity","conditionIndex":1,"summary":"Security Vulnerability Severity < 7","reason":"Found security vulnerability CVE-2020-1945 with severity < 7 (severity = 6.3)","reference":{"value":"CVE-2020-1945","type":"SECURITY_VULNERABILITY_REFID"},"triggerJson":"{\\"conditionIndex\\":1,\\"trigger\\":{\\"refId\\":\\"CVE-2020-1945\\",\\"severity\\":6.3}}"}]}]',
    waived: false,
    policyActionTypeId: 'fail',
    lastReported: '2024-01-11T16:59:15.633-05:00',
  },
  {
    policyViolationId: '8ada310270ae4eae996921c5fa0aa97e',
    componentIdentifier: {
      format: 'maven',
      coordinates: {
        artifactId: 'ant',
        classifier: '',
        extension: 'jar',
        groupId: 'ant',
        version: '1.6.1',
      },
    },
    componentDisplayName: {
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
          value: '1.6.1',
        },
      ],
      name: 'ant',
    },
    hash: '684aeca90db2a55234f5',
    policyId: '2bf5384868f048ffa6c600ae20898561',
    policyName: 'Security-Medium',
    policyOwner: {
      ownerId: 'ROOT_ORGANIZATION_ID',
      ownerName: 'Root Organization',
      ownerType: 'organization',
    },
    policyThreatLevel: 7,
    policyThreatCategory: 'SECURITY',
    constraints: [
      {
        constraintId: '79159e3489e9437a901a57a4fc449dd1',
        constraintName: 'Medium risk CVSS score',
        constraintOperator: 'AND',
        conditions: [
          {
            conditionType: 'SecurityVulnerabilitySeverity',
            conditionSummary: 'Security Vulnerability Severity >= 4',
            conditionReason: 'Found security vulnerability CVE-2021-36374 with severity >= 4 (severity = 5.5)',
            conditionTriggerReference: {
              value: 'CVE-2021-36374',
              type: 'SECURITY_VULNERABILITY_REFID',
            },
          },
          {
            conditionType: 'SecurityVulnerabilitySeverity',
            conditionSummary: 'Security Vulnerability Severity < 7',
            conditionReason: 'Found security vulnerability CVE-2021-36374 with severity < 7 (severity = 5.5)',
            conditionTriggerReference: {
              value: 'CVE-2021-36374',
              type: 'SECURITY_VULNERABILITY_REFID',
            },
          },
        ],
      },
    ],
    constraintFactsJson:
      '[{"constraintId":"79159e3489e9437a901a57a4fc449dd1","constraintName":"Medium risk CVSS score","operatorName":"AND","conditionFacts":[{"conditionTypeId":"SecurityVulnerabilitySeverity","conditionIndex":0,"summary":"Security Vulnerability Severity >= 4","reason":"Found security vulnerability CVE-2021-36374 with severity >= 4 (severity = 5.5)","reference":{"value":"CVE-2021-36374","type":"SECURITY_VULNERABILITY_REFID"},"triggerJson":"{\\"conditionIndex\\":0,\\"trigger\\":{\\"refId\\":\\"CVE-2021-36374\\",\\"severity\\":5.5}}"},{"conditionTypeId":"SecurityVulnerabilitySeverity","conditionIndex":1,"summary":"Security Vulnerability Severity < 7","reason":"Found security vulnerability CVE-2021-36374 with severity < 7 (severity = 5.5)","reference":{"value":"CVE-2021-36374","type":"SECURITY_VULNERABILITY_REFID"},"triggerJson":"{\\"conditionIndex\\":1,\\"trigger\\":{\\"refId\\":\\"CVE-2021-36374\\",\\"severity\\":5.5}}"}]}]',
    waived: false,
    policyActionTypeId: 'fail',
    lastReported: '2024-01-11T16:59:15.633-05:00',
  },
  {
    policyViolationId: 'e0ec27d700ad4cb1a6f3c6bdfd6e1066',
    componentIdentifier: {
      format: 'maven',
      coordinates: {
        artifactId: 'ant',
        classifier: '',
        extension: 'jar',
        groupId: 'ant',
        version: '1.6.1',
      },
    },
    componentDisplayName: {
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
          value: '1.6.1',
        },
      ],
      name: 'ant',
    },
    hash: '684aeca90db2a55234f5',
    policyId: '2bf5384868f048ffa6c600ae20898561',
    policyName: 'Security-Medium',
    policyOwner: {
      ownerId: 'ROOT_ORGANIZATION_ID',
      ownerName: 'Root Organization',
      ownerType: 'organization',
    },
    policyThreatLevel: 7,
    policyThreatCategory: 'SECURITY',
    constraints: [
      {
        constraintId: '79159e3489e9437a901a57a4fc449dd1',
        constraintName: 'Medium risk CVSS score',
        constraintOperator: 'AND',
        conditions: [
          {
            conditionType: 'SecurityVulnerabilitySeverity',
            conditionSummary: 'Security Vulnerability Severity >= 4',
            conditionReason: 'Found security vulnerability sonatype-2018-0330 with severity >= 4 (severity = 6.5)',
            conditionTriggerReference: {
              value: 'sonatype-2018-0330',
              type: 'SECURITY_VULNERABILITY_REFID',
            },
          },
          {
            conditionType: 'SecurityVulnerabilitySeverity',
            conditionSummary: 'Security Vulnerability Severity < 7',
            conditionReason: 'Found security vulnerability sonatype-2018-0330 with severity < 7 (severity = 6.5)',
            conditionTriggerReference: {
              value: 'sonatype-2018-0330',
              type: 'SECURITY_VULNERABILITY_REFID',
            },
          },
        ],
      },
    ],
    constraintFactsJson:
      '[{"constraintId":"79159e3489e9437a901a57a4fc449dd1","constraintName":"Medium risk CVSS score","operatorName":"AND","conditionFacts":[{"conditionTypeId":"SecurityVulnerabilitySeverity","conditionIndex":0,"summary":"Security Vulnerability Severity >= 4","reason":"Found security vulnerability sonatype-2018-0330 with severity >= 4 (severity = 6.5)","reference":{"value":"sonatype-2018-0330","type":"SECURITY_VULNERABILITY_REFID"},"triggerJson":"{\\"conditionIndex\\":0,\\"trigger\\":{\\"refId\\":\\"sonatype-2018-0330\\",\\"severity\\":6.5}}"},{"conditionTypeId":"SecurityVulnerabilitySeverity","conditionIndex":1,"summary":"Security Vulnerability Severity < 7","reason":"Found security vulnerability sonatype-2018-0330 with severity < 7 (severity = 6.5)","reference":{"value":"sonatype-2018-0330","type":"SECURITY_VULNERABILITY_REFID"},"triggerJson":"{\\"conditionIndex\\":1,\\"trigger\\":{\\"refId\\":\\"sonatype-2018-0330\\",\\"severity\\":6.5}}"}]}]',
    waived: false,
    policyActionTypeId: 'fail',
    lastReported: '2024-01-11T16:59:15.633-05:00',
  },
  {
    policyViolationId: '86f8f452627941ac81025853a39813dc',
    componentIdentifier: {
      format: 'maven',
      coordinates: {
        artifactId: 'ant',
        classifier: '',
        extension: 'jar',
        groupId: 'ant',
        version: '1.6.1',
      },
    },
    componentDisplayName: {
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
          value: '1.6.1',
        },
      ],
      name: 'ant',
    },
    hash: '684aeca90db2a55234f5',
    policyId: '364170b6b4134f7d9344324b2b020954',
    policyName: 'Architecture-Cleanup',
    policyOwner: {
      ownerId: 'ROOT_ORGANIZATION_ID',
      ownerName: 'Root Organization',
      ownerType: 'organization',
    },
    policyThreatLevel: 1,
    policyThreatCategory: 'OTHER',
    constraints: [
      {
        constraintId: '00b994379bf14f34af91ae06b3b0ccaf',
        constraintName: 'Test components',
        constraintOperator: 'OR',
        conditions: [
          {
            conditionType: 'Coordinates',
            conditionSummary: 'Coordinates match maven:ant:ant:*:*:*',
            conditionReason: 'Coordinates were ant : ant : 1.6.1 (match ant : ant : * : * : *)',
            conditionTriggerReference: null,
          },
        ],
      },
    ],
    constraintFactsJson:
      '[{"constraintId":"00b994379bf14f34af91ae06b3b0ccaf","constraintName":"Test components","operatorName":"OR","conditionFacts":[{"conditionTypeId":"Coordinates","conditionIndex":1,"summary":"Coordinates match maven:ant:ant:*:*:*","reason":"Coordinates were ant : ant : 1.6.1 (match ant : ant : * : * : *)","reference":null,"triggerJson":null}]}]',
    waived: false,
    policyActionTypeId: null,
    lastReported: '2024-01-11T16:59:15.633-05:00',
  },
  {
    policyViolationId: '589086584b3d4bb6849896e0e2be64c5',
    componentIdentifier: {
      format: 'maven',
      coordinates: {
        artifactId: 'ant',
        classifier: '',
        extension: 'jar',
        groupId: 'ant',
        version: '1.6.1',
      },
    },
    componentDisplayName: {
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
          value: '1.6.1',
        },
      ],
      name: 'ant',
    },
    hash: '684aeca90db2a55234f5',
    policyId: '40e923f8957741f98d0813110681fdb3',
    policyName: 'Architecture-Quality',
    policyOwner: {
      ownerId: 'ROOT_ORGANIZATION_ID',
      ownerName: 'Root Organization',
      ownerType: 'organization',
    },
    policyThreatLevel: 1,
    policyThreatCategory: 'QUALITY',
    constraints: [
      {
        constraintId: '48d8fa5b3a5b441481705c8085fc730c',
        constraintName: 'Version is old',
        constraintOperator: 'OR',
        conditions: [
          {
            conditionType: 'AgeInDays',
            conditionSummary: 'Age older than 1825 days',
            conditionReason: 'Found component older than 5 years',
            conditionTriggerReference: null,
          },
        ],
      },
    ],
    constraintFactsJson:
      '[{"constraintId":"48d8fa5b3a5b441481705c8085fc730c","constraintName":"Version is old","operatorName":"OR","conditionFacts":[{"conditionTypeId":"AgeInDays","conditionIndex":0,"summary":"Age older than 1825 days","reason":"Found component older than 5 years","reference":null,"triggerJson":null}]}]',
    waived: false,
    policyActionTypeId: null,
    lastReported: '2024-01-11T16:59:15.633-05:00',
  },
  {
    policyViolationId: '94127f63ad6f4a65bed91a4b53b96fb3',
    componentIdentifier: {
      format: 'maven',
      coordinates: {
        artifactId: 'ant',
        classifier: '',
        extension: 'jar',
        groupId: 'ant',
        version: '1.6.1',
      },
    },
    componentDisplayName: {
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
          value: '1.6.1',
        },
      ],
      name: 'ant',
    },
    hash: '684aeca90db2a55234f5',
    policyId: '40e923f8957741f98d0813110681fdb3',
    policyName: 'Architecture-Quality',
    policyOwner: {
      ownerId: 'ROOT_ORGANIZATION_ID',
      ownerName: 'Root Organization',
      ownerType: 'organization',
    },
    policyThreatLevel: 1,
    policyThreatCategory: 'QUALITY',
    constraints: [
      {
        constraintId: '76cc74e30f5041da8956ec29d35cc339',
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
    constraintFactsJson:
      '[{"constraintId":"76cc74e30f5041da8956ec29d35cc339","constraintName":"Version is unpopular","operatorName":"OR","conditionFacts":[{"conditionTypeId":"RelativePopularity","conditionIndex":0,"summary":"Relative Popularity (Percentage) <= 10","reason":"Relative popularity was <= 10% (relative popularity = 0%)","reference":null,"triggerJson":null}]}]',
    waived: false,
    policyActionTypeId: null,
    lastReported: '2024-01-11T16:59:15.633-05:00',
  },
];

export const applicableLabelsData = {
  labelsByOwner: [
    {
      ownerId: 'ff7688303b844b08bd9854d3e53802ce',
      ownerName: 'maven-central',
      ownerType: 'repository',
      labels: [],
    },
    {
      ownerId: 'f86bbf0ee69742298363a36dc54e8a36',
      ownerName: '72B7AFE9-0FE2FE06-762B1E5B-43258149-63E1C1BB',
      ownerType: 'repository_manager',
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
          id: '27ac8136009f4a6cbc198a2616a11154',
          label: 'Architecture-Blacklisted',
          description: 'Components which have been blacklisted from use',
          color: 'orange',
          ownerId: 'ROOT_ORGANIZATION_ID',
          ownerType: 'ORGANIZATION',
        },
        {
          id: '3954f37b0e9b431c9d46f3a9938556eb',
          label: 'Architecture-Cleanup',
          description: 'Components which are relics of a build and should not be included in the distribution',
          color: 'orange',
          ownerId: 'ROOT_ORGANIZATION_ID',
          ownerType: 'ORGANIZATION',
        },
        {
          id: '65df4122b63e468ebea14feb21f85fd0',
          label: 'Architecture-Deprecated',
          description: 'Components we want to discourage from developer use',
          color: 'orange',
          ownerId: 'ROOT_ORGANIZATION_ID',
          ownerType: 'ORGANIZATION',
        },
      ],
    },
  ],
};

export const firewallTestData = {
  componentDetailsPage: {
    isLoadingPolicyViolations: false,
    policyViolationsError: null,
    policyExistingWaivers: {
      waiversByOwner: [
        {
          ownerId: '8098fb28cfc84ff99b3c34e66d2b9ccf',
          ownerName: 'maven-central',
          ownerType: 'repository',
          waivers: [
            {
              id: '468e1552699445d48e448bf22740ad8b',
              hash: '7a3c2521ae0c6f53e044',
              policyId: '6f085a73545f443ab92ce7a109c83935',
              ownerId: '8098fb28cfc84ff99b3c34e66d2b9ccf',
              comment: '',
              createTime: 1661928739954,
              expiryTime: null,
              creatorId: 'admin',
              creatorName: 'Admin BuiltIn',
              constraintFactsJson:
                '[{"constraintId":"d17bd2a78ada49d6b40df2dd596d8e19","constraintName":"older than one day","operatorName":"AND","conditionFacts":[{"conditionTypeId":"License","conditionIndex":0,"summary":"License is \'Apache-1.1\'","reason":"Found \'Apache-1.1\' license","reference":null,"triggerJson":"{\\"conditionIndex\\":0,\\"trigger\\":{\\"id\\":\\"Apache-1.1\\"}}"}]}]',
              constraintFacts: [
                {
                  constraintId: 'd17bd2a78ada49d6b40df2dd596d8e19',
                  constraintName: 'older than one day',
                  operatorName: 'AND',
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
              policyName: 'test-policy',
            },
          ],
        },
      ],
    },
    waivers: [
      {
        id: '468e1552699445d48e448bf22740ad8b',
        hash: '7a3c2521ae0c6f53e044',
        policyId: '6f085a73545f443ab92ce7a109c83935',
        ownerId: '8098fb28cfc84ff99b3c34e66d2b9ccf',
        comment: '',
        createTime: 1661928739954,
        expiryTime: null,
        creatorId: 'admin',
        creatorName: 'Admin BuiltIn',
        constraintFactsJson:
          '[{"constraintId":"d17bd2a78ada49d6b40df2dd596d8e19","constraintName":"older than one day","operatorName":"AND","conditionFacts":[{"conditionTypeId":"License","conditionIndex":0,"summary":"License is \'Apache-1.1\'","reason":"Found \'Apache-1.1\' license","reference":null,"triggerJson":"{\\"conditionIndex\\":0,\\"trigger\\":{\\"id\\":\\"Apache-1.1\\"}}"}]}]',
        constraintFacts: [
          {
            constraintId: 'd17bd2a78ada49d6b40df2dd596d8e19',
            constraintName: 'older than one day',
            operatorName: 'AND',
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
        policyName: 'test-policy',
        policyWaiverId: '468e1552699445d48e448bf22740ad8b',
        scopeOwnerId: '8098fb28cfc84ff99b3c34e66d2b9ccf',
        scopeOwnerType: 'repository',
        scopeOwnerName: 'maven-central',
      },
    ],
    componentDetails: {
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
  },
};

export const firewallViolationsTestData = [
  {
    policyViolationId: '8ada310270ae4eae996921c5fa0aa97e',
    componentIdentifier: {
      format: 'maven',
      coordinates: {
        artifactId: 'ant',
        classifier: '',
        extension: 'jar',
        groupId: 'ant',
        version: '1.6.1',
      },
    },
    componentDisplayName: {
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
          value: '1.6.1',
        },
      ],
      name: 'ant',
    },
    hash: '684aeca90db2a55234f5',
    policyId: '2bf5384868f048ffa6c600ae20898561',
    policyName: 'Security-Medium very very very very very very very long name',
    policyOwner: {
      ownerId: 'ROOT_ORGANIZATION_ID',
      ownerName: 'Root Organization',
      ownerType: 'organization',
    },
    policyThreatLevel: 7,
    policyThreatCategory: 'SECURITY',
    constraints: [
      {
        constraintId: '79159e3489e9437a901a57a4fc449dd1',
        constraintName: 'Medium risk CVSS score',
        constraintOperator: 'AND',
        conditions: [
          {
            conditionType: 'SecurityVulnerabilitySeverity',
            conditionSummary: 'Security Vulnerability Severity >= 4',
            conditionReason: 'Found security vulnerability CVE-2021-36374 with severity >= 4 (severity = 5.5)',
            conditionTriggerReference: {
              value: 'CVE-2021-36374',
              type: 'SECURITY_VULNERABILITY_REFID',
            },
          },
          {
            conditionType: 'SecurityVulnerabilitySeverity',
            conditionSummary: 'Security Vulnerability Severity < 7',
            conditionReason: 'Found security vulnerability CVE-2021-36374 with severity < 7 (severity = 5.5)',
            conditionTriggerReference: {
              value: 'CVE-2021-36374',
              type: 'SECURITY_VULNERABILITY_REFID',
            },
          },
        ],
      },
    ],
    constraintFactsJson:
      '[{"constraintId":"79159e3489e9437a901a57a4fc449dd1","constraintName":"Medium risk CVSS score","operatorName":"AND","conditionFacts":[{"conditionTypeId":"SecurityVulnerabilitySeverity","conditionIndex":0,"summary":"Security Vulnerability Severity >= 4","reason":"Found security vulnerability CVE-2021-36374 with severity >= 4 (severity = 5.5)","reference":{"value":"CVE-2021-36374","type":"SECURITY_VULNERABILITY_REFID"},"triggerJson":"{\\"conditionIndex\\":0,\\"trigger\\":{\\"refId\\":\\"CVE-2021-36374\\",\\"severity\\":5.5}}"},{"conditionTypeId":"SecurityVulnerabilitySeverity","conditionIndex":1,"summary":"Security Vulnerability Severity < 7","reason":"Found security vulnerability CVE-2021-36374 with severity < 7 (severity = 5.5)","reference":{"value":"CVE-2021-36374","type":"SECURITY_VULNERABILITY_REFID"},"triggerJson":"{\\"conditionIndex\\":1,\\"trigger\\":{\\"refId\\":\\"CVE-2021-36374\\",\\"severity\\":5.5}}"}]}]',
    waived: false,
    policyActionTypeId: 'fail',
    lastReported: '2024-05-06T10:12:12.593-04:00',
  },
  {
    policyViolationId: 'e0ec27d700ad4cb1a6f3c6bdfd6e1066',
    componentIdentifier: {
      format: 'maven',
      coordinates: {
        artifactId: 'ant',
        classifier: '',
        extension: 'jar',
        groupId: 'ant',
        version: '1.6.1',
      },
    },
    componentDisplayName: {
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
          value: '1.6.1',
        },
      ],
      name: 'ant',
    },
    hash: '684aeca90db2a55234f5',
    policyId: '2bf5384868f048ffa6c600ae20898561',
    policyName: 'Security-Medium very very very very very very very long name',
    policyOwner: {
      ownerId: 'ROOT_ORGANIZATION_ID',
      ownerName: 'Root Organization',
      ownerType: 'organization',
    },
    policyThreatLevel: 7,
    policyThreatCategory: 'SECURITY',
    constraints: [
      {
        constraintId: '79159e3489e9437a901a57a4fc449dd1',
        constraintName: 'Medium risk CVSS score',
        constraintOperator: 'AND',
        conditions: [
          {
            conditionType: 'SecurityVulnerabilitySeverity',
            conditionSummary: 'Security Vulnerability Severity >= 4',
            conditionReason: 'Found security vulnerability sonatype-2018-0330 with severity >= 4 (severity = 6.5)',
            conditionTriggerReference: {
              value: 'sonatype-2018-0330',
              type: 'SECURITY_VULNERABILITY_REFID',
            },
          },
          {
            conditionType: 'SecurityVulnerabilitySeverity',
            conditionSummary: 'Security Vulnerability Severity < 7',
            conditionReason: 'Found security vulnerability sonatype-2018-0330 with severity < 7 (severity = 6.5)',
            conditionTriggerReference: {
              value: 'sonatype-2018-0330',
              type: 'SECURITY_VULNERABILITY_REFID',
            },
          },
        ],
      },
    ],
    constraintFactsJson:
      '[{"constraintId":"79159e3489e9437a901a57a4fc449dd1","constraintName":"Medium risk CVSS score","operatorName":"AND","conditionFacts":[{"conditionTypeId":"SecurityVulnerabilitySeverity","conditionIndex":0,"summary":"Security Vulnerability Severity >= 4","reason":"Found security vulnerability sonatype-2018-0330 with severity >= 4 (severity = 6.5)","reference":{"value":"sonatype-2018-0330","type":"SECURITY_VULNERABILITY_REFID"},"triggerJson":"{\\"conditionIndex\\":0,\\"trigger\\":{\\"refId\\":\\"sonatype-2018-0330\\",\\"severity\\":6.5}}"},{"conditionTypeId":"SecurityVulnerabilitySeverity","conditionIndex":1,"summary":"Security Vulnerability Severity < 7","reason":"Found security vulnerability sonatype-2018-0330 with severity < 7 (severity = 6.5)","reference":{"value":"sonatype-2018-0330","type":"SECURITY_VULNERABILITY_REFID"},"triggerJson":"{\\"conditionIndex\\":1,\\"trigger\\":{\\"refId\\":\\"sonatype-2018-0330\\",\\"severity\\":6.5}}"}]}]',
    waived: false,
    policyActionTypeId: 'fail',
    lastReported: '2024-05-06T10:12:12.593-04:00',
  },
  {
    policyViolationId: 'ee61c6a2c1464a4e8b65784f587b8303',
    componentIdentifier: {
      format: 'maven',
      coordinates: {
        artifactId: 'ant',
        classifier: '',
        extension: 'jar',
        groupId: 'ant',
        version: '1.6.1',
      },
    },
    componentDisplayName: {
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
          value: '1.6.1',
        },
      ],
      name: 'ant',
    },
    hash: '684aeca90db2a55234f5',
    policyId: '2bf5384868f048ffa6c600ae20898561',
    policyName: 'Security-Medium very very very very very very very long name',
    policyOwner: {
      ownerId: 'ROOT_ORGANIZATION_ID',
      ownerName: 'Root Organization',
      ownerType: 'organization',
    },
    policyThreatLevel: 7,
    policyThreatCategory: 'SECURITY',
    constraints: [
      {
        constraintId: '79159e3489e9437a901a57a4fc449dd1',
        constraintName: 'Medium risk CVSS score',
        constraintOperator: 'AND',
        conditions: [
          {
            conditionType: 'SecurityVulnerabilitySeverity',
            conditionSummary: 'Security Vulnerability Severity >= 4',
            conditionReason: 'Found security vulnerability CVE-2012-2098 with severity >= 4 (severity = 5.0)',
            conditionTriggerReference: {
              value: 'CVE-2012-2098',
              type: 'SECURITY_VULNERABILITY_REFID',
            },
          },
          {
            conditionType: 'SecurityVulnerabilitySeverity',
            conditionSummary: 'Security Vulnerability Severity < 7',
            conditionReason: 'Found security vulnerability CVE-2012-2098 with severity < 7 (severity = 5.0)',
            conditionTriggerReference: {
              value: 'CVE-2012-2098',
              type: 'SECURITY_VULNERABILITY_REFID',
            },
          },
        ],
      },
    ],
    constraintFactsJson:
      '[{"constraintId":"79159e3489e9437a901a57a4fc449dd1","constraintName":"Medium risk CVSS score","operatorName":"AND","conditionFacts":[{"conditionTypeId":"SecurityVulnerabilitySeverity","conditionIndex":0,"summary":"Security Vulnerability Severity >= 4","reason":"Found security vulnerability CVE-2012-2098 with severity >= 4 (severity = 5.0)","reference":{"value":"CVE-2012-2098","type":"SECURITY_VULNERABILITY_REFID"},"triggerJson":"{\\"conditionIndex\\":0,\\"trigger\\":{\\"refId\\":\\"CVE-2012-2098\\",\\"severity\\":5.0}}"},{"conditionTypeId":"SecurityVulnerabilitySeverity","conditionIndex":1,"summary":"Security Vulnerability Severity < 7","reason":"Found security vulnerability CVE-2012-2098 with severity < 7 (severity = 5.0)","reference":{"value":"CVE-2012-2098","type":"SECURITY_VULNERABILITY_REFID"},"triggerJson":"{\\"conditionIndex\\":1,\\"trigger\\":{\\"refId\\":\\"CVE-2012-2098\\",\\"severity\\":5.0}}"}]}]',
    waived: false,
    policyActionTypeId: 'fail',
    lastReported: '2024-05-06T10:12:12.593-04:00',
  },
];
