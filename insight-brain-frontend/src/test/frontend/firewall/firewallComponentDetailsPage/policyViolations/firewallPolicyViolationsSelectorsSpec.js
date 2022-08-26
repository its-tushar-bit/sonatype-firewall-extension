/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as firewallPolicyViolationsSelectors from 'MainRoot/firewall/firewallComponentDetailsPage/policyViolations/firewallPolicyViolationsSelectors';

describe('firewallPolicyViolationsSelectors', () => {
  const minState = {
    firewall: {
      componentDetailsPage: {
        isLoadingComponentDetails: false,
        componentDetails: {
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
          overriddenLicenses: [],
          policyMaxThreatLevelsByCategory: {
            license: 5,
            other: 8,
            security: 7,
            quality: 1,
          },
          effectiveLicenses: [
            {
              licenseId: 'Apache-1.1',
              licenseName: 'Apache-1.1',
            },
          ],
          effectiveLicenseStatus: null,
          catalogDate: 1132682799000,
          relativePopularity: 3,
          securityVulnerabilities: [
            {
              refId: 'CVE-2012-2098',
              severity: 5,
              source: 'cve',
              summary:
                'Algorithmic complexity vulnerability in the sorting algorithms in bzip2 compressing stream (BZip2CompressorOutputStream) in Apache Commons Compress before 1.4.1 allows remote attackers to cause a denial of service (CPU consumption) via a file with many repeating inputs.',
              status: 'Open',
              url: null,
              vulnerabilityCategories: ['functional', 'data'],
              aliases: [],
            },
          ],
          website: null,
          policyAlerts: [
            {
              trigger: {
                policyId: '2b4fafb9e8894e3fba3b12385ff4a2fd',
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
                        version: '1.6',
                      },
                    },
                    hash: '7a3c2521ae0c6f53e044',
                    constraintFacts: [
                      {
                        constraintId: 'b68075cb73814cd0ac2d2828b3658d10',
                        constraintName: 'Test components',
                        operatorName: 'OR',
                        conditionFacts: [
                          {
                            conditionTypeId: 'Coordinates',
                            conditionIndex: 1,
                            summary: 'Coordinates match maven:ant:ant:*:*:*',
                            reason: 'Coordinates were ant : ant : 1.6 (match ant : ant : * : * : *)',
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
          version: '1.6',
          groupId: 'ant',
          artifactId: 'ant',
          declaredLicenseIds: ['Apache-1.1'],
          observedLicenseIds: ['Apache-1.1'],
        },
        componentDetailsError: null,
        policyViolations: [
          {
            policyViolationId: '17eca7cca5d64d129c75ca4afb5bf4cc',
            policyId: 'd14665edf4364a3aa6a15e1ebbad2fd4',
            policyName: 'Luis version warning policy',
            policyOwner: {
              ownerId: 'ROOT_ORGANIZATION_ID',
              ownerName: 'Root Organization',
              ownerType: 'organization',
            },
            policyThreatLevel: 8,
            policyThreatCategory: 'OTHER',
            constraints: [
              {
                constraintId: 'b82f48452e1646f18b0c4ebaab76b862',
                constraintName: 'component version is 1.6.3',
                constraintOperator: 'OR',
                conditions: [
                  {
                    conditionType: 'Coordinates',
                    conditionSummary: 'Coordinates do not match maven:ant:ant:1.6.3:*:*',
                    conditionReason: 'Coordinates were ant : ant : 1.6 (do not match ant : ant : * : * : 1.6.3)',
                    conditionTriggerReference: null,
                  },
                ],
              },
            ],
            constraintFactsJson:
              '[{"constraintId":"b82f48452e1646f18b0c4ebaab76b862","constraintName":"component version is 1.6.3","operatorName":"OR","conditionFacts":[{"conditionTypeId":"Coordinates","conditionIndex":0,"summary":"Coordinates do not match maven:ant:ant:1.6.3:*:*","reason":"Coordinates were ant : ant : 1.6 (do not match ant : ant : * : * : 1.6.3)","reference":null,"triggerJson":null}]}]',
            policyActionTypeId: 'warn',
            lastReported: '2022-08-01T22:54:16.897-05:00',
          },
        ],
        isLoadingPolicyViolations: false,
        policyViolationsError: null,
      },
    },
  };

  it('selectPolicyViolations', () => {
    expect(Object.keys(firewallPolicyViolationsSelectors.selectPolicyViolations(minState))).toEqual([
      ...Object.keys(minState.firewall.componentDetailsPage.componentDetails.policyAlerts.map((trigger) => trigger)),
    ]);
  });

  it('selectSecurityPolicyViolations', () => {
    expect(Object.keys(firewallPolicyViolationsSelectors.selectSecurityPolicyViolations(minState))).toEqual(
      firewallPolicyViolationsSelectors
        .selectPolicyViolations(minState)
        .filter((violation) => violation.policyThreatCategory === 'SECURITY')
    );
  });
});
