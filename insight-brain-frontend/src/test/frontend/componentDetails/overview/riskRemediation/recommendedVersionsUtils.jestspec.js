/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  getAsyncRecommendationsPrioritiesPage,
  setRemediations,
} from 'MainRoot/componentDetails/overview/riskRemediation/recommendedVersionsUtils';

describe('recommendedVersionUtils', () => {
  it('returns list with one element if no remediation array is sent', () => {
    const actualVersion = '2.4.9';
    const stageId = 'build';
    const result = setRemediations([], actualVersion, stageId);
    expect(result.length).toBe(1);
    const element = result[0];
    expect(element.id).toBe('no-versions-available');
    expect(element.text).toBe('There are no suggested versions for this component');
  });

  describe('returns list', () => {
    const remediation = {
      versionChanges: [
        {
          type: 'next-no-violations',
          data: {
            component: {
              packageUrl: 'pkg:maven/org.springframework.boot/spring-boot-jarmode-layertools@2.4.10?type=jar',
              hash: null,
              componentIdentifier: {
                format: 'maven',
                coordinates: {
                  artifactId: 'spring-boot-jarmode-layertools',
                  classifier: '',
                  extension: 'jar',
                  groupId: 'org.springframework.boot',
                  version: '2.4.10',
                },
              },
              displayName: 'org.springframework.boot : spring-boot-jarmode-layertools : 2.4.10',
            },
          },
        },
        {
          type: 'next-non-failing',
          data: {
            component: {
              packageUrl: 'pkg:maven/org.springframework.boot/spring-boot-jarmode-layertools@2.4.9?type=jar',
              hash: null,
              componentIdentifier: {
                format: 'maven',
                coordinates: {
                  artifactId: 'spring-boot-jarmode-layertools',
                  classifier: '',
                  extension: 'jar',
                  groupId: 'org.springframework.boot',
                  version: '2.4.9',
                },
              },
              displayName: 'org.springframework.boot : spring-boot-jarmode-layertools : 2.4.9',
            },
          },
        },
        {
          type: 'next-non-failing-with-dependencies',
          data: {
            component: {
              packageUrl: 'pkg:maven/org.springframework.boot/spring-boot-jarmode-layertools@2.4.9?type=jar',
              hash: null,
              componentIdentifier: {
                format: 'maven',
                coordinates: {
                  artifactId: 'spring-boot-jarmode-layertools',
                  classifier: '',
                  extension: 'jar',
                  groupId: 'org.springframework.boot',
                  version: '2.4.9',
                },
              },
              displayName: 'org.springframework.boot : spring-boot-jarmode-layertools : 2.4.9',
            },
          },
        },
      ],
    };
    const actualVersion = '2.4.9';
    const stageId = 'build';
    const result = setRemediations(remediation, actualVersion, stageId);

    it('with one element', () => {
      expect(result.length).toBe(1);
    });

    it('first element is a new version', () => {
      const element = result[0];
      expect(element.id).toBe('next-no-violation-version');
      expect(element.text).toBe('Next version with no policy violation');
      expect(element.type).toBe('next-no-violations');
      expect(element.version).toBe('2.4.10');
    });
  });

  describe('return a list scenario 2', () => {
    const remediation = {
      versionChanges: [
        {
          type: 'next-non-failing',
          data: {
            component: {
              packageUrl: 'pkg:maven/com.h2database/h2@1.4.200?type=jar',
              hash: null,
              componentIdentifier: {
                format: 'maven',
                coordinates: {
                  artifactId: 'h2',
                  classifier: '',
                  extension: 'jar',
                  groupId: 'com.h2database',
                  version: '1.4.200',
                },
              },
              displayName: 'com.h2database : h2 : 1.4.200',
            },
          },
        },
        {
          type: 'next-non-failing-with-dependencies',
          data: {
            component: {
              packageUrl: 'pkg:maven/com.h2database/h2@1.4.200?type=jar',
              hash: null,
              componentIdentifier: {
                format: 'maven',
                coordinates: {
                  artifactId: 'h2',
                  classifier: '',
                  extension: 'jar',
                  groupId: 'com.h2database',
                  version: '1.4.200',
                },
              },
              displayName: 'com.h2database : h2 : 1.4.200',
            },
          },
        },
      ],
    };
    const actualVersion = '1.4.200';
    const stageId = 'build';
    const result = setRemediations(remediation, actualVersion, stageId);

    it('with no elements', () => {
      expect(result.length).toBe(1);
      const element = result[0];
      expect(element.id).toBe('no-versions-available');
      expect(element.text).toBe('There are no suggested versions for this component');
    });
  });

  describe('return a list in correct order', () => {
    const remediation = {
      versionChanges: [
        {
          type: 'next-non-failing',
          data: {
            component: {
              packageUrl: 'pkg:maven/com.h2database/h2@1.4.200?type=jar',
              hash: null,
              componentIdentifier: {
                format: 'maven',
                coordinates: {
                  artifactId: 'h2',
                  classifier: '',
                  extension: 'jar',
                  groupId: 'com.h2database',
                  version: '1.4.250',
                },
              },
              displayName: 'com.h2database : h2 : 1.4.200',
            },
          },
        },
        {
          type: 'next-non-failing-with-dependencies',
          data: {
            component: {
              packageUrl: 'pkg:maven/com.h2database/h2@1.4.200?type=jar',
              hash: null,
              componentIdentifier: {
                format: 'maven',
                coordinates: {
                  artifactId: 'h2',
                  classifier: '',
                  extension: 'jar',
                  groupId: 'com.h2database',
                  version: '1.4.220',
                },
              },
              displayName: 'com.h2database : h2 : 1.4.200',
            },
          },
        },
        {
          type: 'next-no-violations-with-dependencies',
          data: {
            component: {
              packageUrl: 'pkg:maven/com.h2database/h2@1.4.200?type=jar',
              hash: null,
              componentIdentifier: {
                format: 'maven',
                coordinates: {
                  artifactId: 'h2',
                  classifier: '',
                  extension: 'jar',
                  groupId: 'com.h2database',
                  version: '1.4.600',
                },
              },
              displayName: 'com.h2database : h2 : 1.4.200',
            },
          },
        },
        {
          type: 'next-no-violations',
          data: {
            component: {
              packageUrl: 'pkg:maven/com.h2database/h2@1.4.200?type=jar',
              hash: null,
              componentIdentifier: {
                format: 'maven',
                coordinates: {
                  artifactId: 'h2',
                  classifier: '',
                  extension: 'jar',
                  groupId: 'com.h2database',
                  version: '1.4.500',
                },
              },
              displayName: 'com.h2database : h2 : 1.4.050',
            },
          },
        },
      ],
      suggestedVersionChange: {
        type: 'recommended-non-breaking-with-dependencies',
        isGolden: true,
        data: {
          component: {
            packageUrl: 'pkg:maven/com.h2database/h2@1.4.200?type=jar',
            hash: null,
            componentIdentifier: {
              format: 'maven',
              coordinates: {
                artifactId: 'h2',
                classifier: '',
                extension: 'jar',
                groupId: 'com.h2database',
                version: '1.4.800',
              },
            },
            displayName: 'com.h2database : h2 : 1.4.200',
          },
        },
      },
    };

    const actualVersion = '1.4.200';
    const stageId = 'build';
    const result = setRemediations(remediation, actualVersion, stageId);

    it('should return 5 results with correct data', () => {
      expect(result.length).toBe(5);
    });

    it('first element is a recommended-non-breaking-with-dependenceies version', () => {
      const firstElement = result[0];
      expect(firstElement.id).toBe('recommended-non-breaking-with-dependencies-version');
      expect(firstElement.text).toBe(
        'No breaking changes, No policy violations for this component, No policy violations for its dependencies'
      );
      expect(firstElement.type).toBe('recommended-non-breaking-with-dependencies');
      expect(firstElement.version).toBe('1.4.800');
      expect(firstElement.isGolden).toBe(true);
    });

    it('second element is a next-no-violations-with-dependencies version', () => {
      const secondElement = result[1];
      expect(secondElement.id).toBe('next-no-violation-dependencies-version');
      expect(secondElement.text).toBe('Next version with no policy violations for this component and its dependencies');
      expect(secondElement.type).toBe('next-no-violations-with-dependencies');
      expect(secondElement.version).toBe('1.4.600');
      expect(secondElement.isGolden).toBe(false);
    });

    it('third element is a next-no-violations version', () => {
      const thirdElement = result[2];
      expect(thirdElement.id).toBe('next-no-violation-version');
      expect(thirdElement.text).toBe('Next version with no policy violation');
      expect(thirdElement.type).toBe('next-no-violations');
      expect(thirdElement.version).toBe('1.4.500');
      expect(thirdElement.isGolden).toBe(false);
    });

    it('fourth element is a next-non-failing-with-dependencies version', () => {
      const fourthElement = result[3];
      expect(fourthElement.id).toBe('next-no-fail-dependencies-version');
      expect(fourthElement.text).toBe('Next version with no Build failure for this component and its dependencies');
      expect(fourthElement.type).toBe('next-non-failing-with-dependencies');
      expect(fourthElement.version).toBe('1.4.220');
      expect(fourthElement.isGolden).toBe(false);
    });

    it('fifth element is a next-non-failing version', () => {
      const fifthElement = result[4];
      expect(fifthElement.id).toBe('next-no-fail-version');
      expect(fifthElement.text).toBe('Next version with no Build failure');
      expect(fifthElement.type).toBe('next-non-failing');
      expect(fifthElement.version).toBe('1.4.250');
      expect(fifthElement.isGolden).toBe(false);
    });
  });

  describe('return list with no duplicate versions', () => {
    const remediation = {
      versionChanges: [
        {
          type: 'next-non-failing',
          data: {
            component: {
              packageUrl: 'pkg:maven/com.h2database/h2@1.4.200?type=jar',
              hash: null,
              componentIdentifier: {
                format: 'maven',
                coordinates: {
                  artifactId: 'h2',
                  classifier: '',
                  extension: 'jar',
                  groupId: 'com.h2database',
                  version: '1.5',
                },
              },
              displayName: 'com.h2database : h2 : 1.4.200',
            },
          },
        },
        {
          type: 'next-non-failing-with-dependencies',
          data: {
            component: {
              packageUrl: 'pkg:maven/com.h2database/h2@1.4.200?type=jar',
              hash: null,
              componentIdentifier: {
                format: 'maven',
                coordinates: {
                  artifactId: 'h2',
                  classifier: '',
                  extension: 'jar',
                  groupId: 'com.h2database',
                  version: '1.4',
                },
              },
              displayName: 'com.h2database : h2 : 1.4.200',
            },
          },
        },
        {
          type: 'next-no-violations-with-dependencies',
          data: {
            component: {
              packageUrl: 'pkg:maven/com.h2database/h2@1.4.200?type=jar',
              hash: null,
              componentIdentifier: {
                format: 'maven',
                coordinates: {
                  artifactId: 'h2',
                  classifier: '',
                  extension: 'jar',
                  groupId: 'com.h2database',
                  version: '1.5',
                },
              },
              displayName: 'com.h2database : h2 : 1.4.200',
            },
          },
        },
        {
          type: 'next-no-violations',
          data: {
            component: {
              packageUrl: 'pkg:maven/com.h2database/h2@1.4.200?type=jar',
              hash: null,
              componentIdentifier: {
                format: 'maven',
                coordinates: {
                  artifactId: 'h2',
                  classifier: '',
                  extension: 'jar',
                  groupId: 'com.h2database',
                  version: '1.5',
                },
              },
              displayName: 'com.h2database : h2 : 1.4.050',
            },
          },
        },
      ],
      suggestedVersionChange: {
        type: 'recommended-non-breaking-with-dependencies',
        isGolden: true,
        data: {
          component: {
            packageUrl: 'pkg:maven/com.h2database/h2@1.4.200?type=jar',
            hash: null,
            componentIdentifier: {
              format: 'maven',
              coordinates: {
                artifactId: 'h2',
                classifier: '',
                extension: 'jar',
                groupId: 'com.h2database',
                version: '1.7',
              },
            },
            displayName: 'com.h2database : h2 : 1.4.200',
          },
        },
      },
    };

    const actualVersion = '1.4.200';
    const stageId = 'build';
    const result = setRemediations(remediation, actualVersion, stageId);

    it('should return 3 results without any duplicate versions', () => {
      expect(result.length).toBe(3);
      expect(result[0].version).toBe('1.7');
      expect(result[0].type).toBe('recommended-non-breaking-with-dependencies');

      expect(result[1].version).toBe('1.5');
      expect(result[1].type).toBe('next-no-violations-with-dependencies');

      expect(result[2].version).toBe('1.4');
      expect(result[2].type).toBe('next-non-failing-with-dependencies');
    });
  });

  describe('breaking changes count in recommendations', () => {
    const allVersions = [
      {
        componentIdentifier: {
          coordinates: {
            artifactId: 'h2',
            classifier: '',
            extension: 'jar',
            groupId: 'com.h2database',
            version: '1.4.200',
          },
        },
        displayName: {
          name: 'h2',
        },
        breakingChangesCount: 0,
      },
      {
        componentIdentifier: {
          coordinates: {
            artifactId: 'h2',
            classifier: '',
            extension: 'jar',
            groupId: 'com.h2database',
            version: '1.5.0',
          },
        },
        displayName: {
          name: 'h2',
        },
        breakingChangesCount: 2,
      },
      {
        componentIdentifier: {
          coordinates: {
            artifactId: 'h2',
            classifier: '',
            extension: 'jar',
            groupId: 'com.h2database',
            version: '2.0.0',
          },
        },
        displayName: {
          name: 'h2',
        },
        breakingChangesCount: 5,
      },
    ];

    const remediation = {
      suggestedVersionChange: {
        type: 'recommended-non-breaking-with-dependencies',
        isGolden: true,
        data: {
          component: {
            componentIdentifier: {
              coordinates: {
                artifactId: 'h2',
                classifier: '',
                extension: 'jar',
                groupId: 'com.h2database',
                version: '1.5.0',
              },
            },
            thirdParty: false,
          },
        },
      },
      versionChanges: [
        {
          type: 'next-no-violations',
          data: {
            component: {
              componentIdentifier: {
                coordinates: {
                  artifactId: 'h2',
                  classifier: '',
                  extension: 'jar',
                  groupId: 'com.h2database',
                  version: '2.0.0',
                },
              },
              thirdParty: false,
            },
          },
        },
        {
          type: 'next-non-failing',
          data: {
            component: {
              componentIdentifier: {
                coordinates: {
                  artifactId: 'h2',
                  classifier: '',
                  extension: 'jar',
                  groupId: 'com.h2database',
                  version: '1.5.0',
                },
              },
              thirdParty: false,
            },
          },
        },
      ],
    };

    it('includes breaking changes count for golden versions', () => {
      const result = getAsyncRecommendationsPrioritiesPage(remediation, '1.4.0', 'build', allVersions);

      expect(result.breakingChangesCount).toBe(0);
      expect(result.version).toBe('1.5.0');
      expect(result.isGolden).toBe(true);
    });

    it('includes breaking changes count for next version with no violations', () => {
      // Suggested version is non-golden
      const nonGoldenRemediation = {
        ...remediation,
        suggestedVersionChange: null,
      };

      const result = getAsyncRecommendationsPrioritiesPage(nonGoldenRemediation, '1.4.0', 'build', allVersions);

      expect(result.breakingChangesCount).toBe(5);
      expect(result.version).toBe('2.0.0');
      expect(result.isGolden).toBe(false);
    });

    it('returns null breaking changes count when version not found', () => {
      // Create remediation with a version not in allVersions
      const missingVersionRemediation = {
        versionChanges: [
          {
            type: 'next-no-violations',
            data: {
              component: {
                componentIdentifier: {
                  coordinates: {
                    artifactId: 'h2',
                    classifier: '',
                    extension: 'jar',
                    groupId: 'com.h2database',
                    version: '3.0.0', // Version not in allVersions
                  },
                },
                thirdParty: false,
              },
            },
          },
        ],
      };

      const result = getAsyncRecommendationsPrioritiesPage(missingVersionRemediation, '1.4.0', 'build', allVersions);

      expect(result.breakingChangesCount).toBeNull();
      expect(result.version).toBe('3.0.0');
    });

    it('handles null breaking changes count in allVersions', () => {
      const allVersionsWithNull = [
        ...allVersions.slice(0, 1),
        {
          componentIdentifier: {
            coordinates: {
              artifactId: 'h2',
              classifier: '',
              extension: 'jar',
              groupId: 'com.h2database',
              version: '1.5.0',
            },
          },
          displayName: {
            name: 'h2',
          },
          breakingChangesCount: null,
        },
        ...allVersions.slice(2),
      ];

      const remediationWithVersion = {
        versionChanges: [
          {
            type: 'next-no-violations',
            data: {
              component: {
                componentIdentifier: {
                  coordinates: {
                    artifactId: 'h2',
                    classifier: '',
                    extension: 'jar',
                    groupId: 'com.h2database',
                    version: '1.5.0',
                  },
                },
                thirdParty: false,
              },
            },
          },
        ],
      };

      const result = getAsyncRecommendationsPrioritiesPage(
        remediationWithVersion,
        '1.4.0',
        'build',
        allVersionsWithNull
      );

      expect(result.breakingChangesCount).toBeNull();
      expect(result.version).toBe('1.5.0');
    });
  });
});
