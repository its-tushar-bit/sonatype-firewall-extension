/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { setRemediations } from '../../../../../main/frontend/componentDetails/overview/riskRemediation/recommendedVersionsUtils';

describe('recommendedVersionUtils', () => {
  it('returns list with one element if no remediation array is sent', () => {
    const actualVersion = '2.4.9';
    const stageId = 'build';
    const result = setRemediations([], actualVersion, stageId);
    expect(result.length).toBe(1);
    const element = result[0];
    expect(element.id).toBe('no-versions-available');
    expect(element.text).toBe('No recommended versions are available for the current component');
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

    it('with two elements', () => {
      expect(result.length).toBe(2);
    });

    it('first element is a new version', () => {
      const element = result[0];
      expect(element.id).toBe('next-no-violation-version-link');
      expect(element.text).toBe('Next version with no policy violation');
      expect(element.type).toBe('next-no-violations');
      expect(element.version).toBe('2.4.10');
    });

    it('second element is for current version', () => {
      const element = result[1];
      expect(element.id).toBe('next-no-fail-dependencies-version');
      expect(element.text).toBe(
        "The current version doesn't cause Build failure for this component and its dependencies"
      );
      expect(element.type).toBe('next-non-failing-with-dependencies');
      expect(element.version).toBe('2.4.9');
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
    it('with two elements', () => {
      expect(result.length).toBe(1);
      const element = result[0];
      expect(element.id).toBe('next-no-fail-dependencies-version');
      expect(element.text).toBe(
        "The current version doesn't cause Build failure for this component and its dependencies"
      );
      expect(element.type).toBe('next-non-failing-with-dependencies');
      expect(element.version).toBe('1.4.200');
    });
  });
});
