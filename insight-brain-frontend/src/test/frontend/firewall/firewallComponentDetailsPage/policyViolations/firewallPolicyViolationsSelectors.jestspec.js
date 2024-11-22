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
        isLoadingPolicyViolations: false,
        policyViolationsError: null,
        componentName: 'ant : ant : 1.6',
        name: 'ant',
      },
    },
  };

  it('selectPolicyViolations', () => {
    expect(firewallPolicyViolationsSelectors.selectPolicyViolations(minState)).toEqual(
      minState.firewall.componentDetailsPage.policyViolations
    );
  });

  it('selectSecurityPolicyViolations', () => {
    expect(Object.keys(firewallPolicyViolationsSelectors.selectSecurityPolicyViolations(minState))).toEqual(
      firewallPolicyViolationsSelectors
        .selectPolicyViolations(minState)
        .filter((violation) => violation.policyThreatCategory === 'SECURITY')
    );
  });

  it('selectWaiversByOwner', () => {
    expect(firewallPolicyViolationsSelectors.selectWaiversByOwner(minState)).toEqual(
      minState.firewall.componentDetailsPage.policyExistingWaivers.waiversByOwner
    );
  });

  it('selectWaivers', () => {
    expect(firewallPolicyViolationsSelectors.selectWaivers(minState)).toEqual(
      minState.firewall.componentDetailsPage.waivers
    );
  });

  it('selectDisplayName', () => {
    expect(firewallPolicyViolationsSelectors.selectDisplayName(minState)).toEqual(
      minState.firewall.componentDetailsPage.componentDetails.displayName
    );
  });

  it('selectComponentName', () => {
    expect(firewallPolicyViolationsSelectors.selectComponentName(minState)).toEqual(
      minState.firewall.componentDetailsPage.componentName
    );
  });

  it('selectComponentNameWithoutVersion', () => {
    expect(firewallPolicyViolationsSelectors.selectComponentNameWithoutVersion(minState)).toEqual(
      minState.firewall.componentDetailsPage.name
    );
  });
});
