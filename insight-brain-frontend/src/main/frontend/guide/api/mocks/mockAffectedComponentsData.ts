/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import type { AffectedComponentVersion } from '@guide/ui-core/types';

/**
 * Mock affected components for CVE-2021-44228 (Log4Shell).
 * Provides 30 records across Maven, npm, and PyPI ecosystems for pagination testing.
 */
export const mockAffectedComponentsLog4Shell: AffectedComponentVersion[] = [
  // Maven ecosystem
  { ecosystem: 'maven', namespace: 'org.apache.logging.log4j', packageName: 'log4j-core', version: '2.14.1', fullPackageName: 'pkg:maven/org.apache.logging.log4j/log4j-core@2.14.1' },
  { ecosystem: 'maven', namespace: 'org.apache.logging.log4j', packageName: 'log4j-core', version: '2.14.0', fullPackageName: 'pkg:maven/org.apache.logging.log4j/log4j-core@2.14.0' },
  { ecosystem: 'maven', namespace: 'org.apache.logging.log4j', packageName: 'log4j-core', version: '2.13.3', fullPackageName: 'pkg:maven/org.apache.logging.log4j/log4j-core@2.13.3' },
  { ecosystem: 'maven', namespace: 'org.apache.logging.log4j', packageName: 'log4j-core', version: '2.13.2', fullPackageName: 'pkg:maven/org.apache.logging.log4j/log4j-core@2.13.2' },
  { ecosystem: 'maven', namespace: 'org.apache.logging.log4j', packageName: 'log4j-core', version: '2.13.1', fullPackageName: 'pkg:maven/org.apache.logging.log4j/log4j-core@2.13.1' },
  { ecosystem: 'maven', namespace: 'org.apache.logging.log4j', packageName: 'log4j-core', version: '2.13.0', fullPackageName: 'pkg:maven/org.apache.logging.log4j/log4j-core@2.13.0' },
  { ecosystem: 'maven', namespace: 'org.apache.logging.log4j', packageName: 'log4j-core', version: '2.12.4', fullPackageName: 'pkg:maven/org.apache.logging.log4j/log4j-core@2.12.4' },
  { ecosystem: 'maven', namespace: 'org.apache.logging.log4j', packageName: 'log4j-core', version: '2.12.3', fullPackageName: 'pkg:maven/org.apache.logging.log4j/log4j-core@2.12.3' },
  { ecosystem: 'maven', namespace: 'org.apache.logging.log4j', packageName: 'log4j-core', version: '2.12.2', fullPackageName: 'pkg:maven/org.apache.logging.log4j/log4j-core@2.12.2' },
  { ecosystem: 'maven', namespace: 'org.apache.logging.log4j', packageName: 'log4j-core', version: '2.12.1', fullPackageName: 'pkg:maven/org.apache.logging.log4j/log4j-core@2.12.1' },
  { ecosystem: 'maven', namespace: 'org.apache.logging.log4j', packageName: 'log4j-to-slf4j', version: '2.14.1', fullPackageName: 'pkg:maven/org.apache.logging.log4j/log4j-to-slf4j@2.14.1' },
  { ecosystem: 'maven', namespace: 'org.apache.logging.log4j', packageName: 'log4j-api', version: '2.14.1', fullPackageName: 'pkg:maven/org.apache.logging.log4j/log4j-api@2.14.1' },
  { ecosystem: 'maven', namespace: 'org.springframework.boot', packageName: 'spring-boot-starter-log4j2', version: '2.6.1', fullPackageName: 'pkg:maven/org.springframework.boot/spring-boot-starter-log4j2@2.6.1' },
  { ecosystem: 'maven', namespace: 'org.springframework.boot', packageName: 'spring-boot-starter-log4j2', version: '2.5.7', fullPackageName: 'pkg:maven/org.springframework.boot/spring-boot-starter-log4j2@2.5.7' },
  { ecosystem: 'maven', namespace: 'org.elasticsearch.client', packageName: 'elasticsearch-rest-high-level-client', version: '7.15.0', fullPackageName: 'pkg:maven/org.elasticsearch.client/elasticsearch-rest-high-level-client@7.15.0' },
  // npm ecosystem
  { ecosystem: 'npm', packageName: 'log4js', version: '6.3.0', fullPackageName: 'pkg:npm/log4js@6.3.0' },
  { ecosystem: 'npm', packageName: 'log4js', version: '6.2.1', fullPackageName: 'pkg:npm/log4js@6.2.1' },
  { ecosystem: 'npm', packageName: 'node-log4js', version: '1.0.0', fullPackageName: 'pkg:npm/node-log4js@1.0.0' },
  { ecosystem: 'npm', packageName: 'apache-log4j-node', version: '2.14.1', fullPackageName: 'pkg:npm/apache-log4j-node@2.14.1' },
  { ecosystem: 'npm', packageName: 'log4js-extensions', version: '1.0.5', fullPackageName: 'pkg:npm/log4js-extensions@1.0.5' },
  { ecosystem: 'npm', packageName: 'node-log-manager', version: '3.2.1', fullPackageName: 'pkg:npm/node-log-manager@3.2.1' },
  // PyPI ecosystem
  { ecosystem: 'pypi', packageName: 'log4j2-python', version: '0.1.0', fullPackageName: 'pkg:pypi/log4j2-python@0.1.0' },
  { ecosystem: 'pypi', packageName: 'py4j-log4j', version: '0.10.9.1', fullPackageName: 'pkg:pypi/py4j-log4j@0.10.9.1' },
  { ecosystem: 'pypi', packageName: 'django-log4j', version: '2.14.1', fullPackageName: 'pkg:pypi/django-log4j@2.14.1' },
  { ecosystem: 'pypi', packageName: 'flask-log4j', version: '1.0.0', fullPackageName: 'pkg:pypi/flask-log4j@1.0.0' },
  { ecosystem: 'pypi', packageName: 'log4j-bridge', version: '0.5.0', fullPackageName: 'pkg:pypi/log4j-bridge@0.5.0' },
  // Go ecosystem
  { ecosystem: 'go', namespace: 'github.com/apache', packageName: 'logging-log4j', version: 'v2.14.1', fullPackageName: 'pkg:go/github.com/apache/logging-log4j@v2.14.1' },
  // Cargo ecosystem
  { ecosystem: 'cargo', packageName: 'log4j', version: '0.1.0', fullPackageName: 'pkg:cargo/log4j@0.1.0' },
  // Additional for pagination
  { ecosystem: 'maven', namespace: 'com.example', packageName: 'logging-library', version: '1.0.0', fullPackageName: 'pkg:maven/com.example/logging-library@1.0.0' },
  { ecosystem: 'npm', packageName: 'typescript-log-wrapper', version: '4.0.0', fullPackageName: 'pkg:npm/typescript-log-wrapper@4.0.0' },
];

/**
 * Mock affected components for CVE-2022-22965 (Spring4Shell).
 */
export const mockAffectedComponentsSpring4Shell: AffectedComponentVersion[] = [
  { ecosystem: 'maven', namespace: 'org.springframework', packageName: 'spring-beans', version: '5.3.17', fullPackageName: 'pkg:maven/org.springframework/spring-beans@5.3.17' },
  { ecosystem: 'maven', namespace: 'org.springframework', packageName: 'spring-beans', version: '5.3.16', fullPackageName: 'pkg:maven/org.springframework/spring-beans@5.3.16' },
  { ecosystem: 'maven', namespace: 'org.springframework.boot', packageName: 'spring-boot-starter-web', version: '2.6.5', fullPackageName: 'pkg:maven/org.springframework.boot/spring-boot-starter-web@2.6.5' },
];

/**
 * Get mock affected components for a vulnerability.
 */
export function getMockAffectedComponents(vulnId: string): AffectedComponentVersion[] {
  switch (vulnId) {
    case 'CVE-2021-44228':
      return mockAffectedComponentsLog4Shell;
    case 'CVE-2022-22965':
      return mockAffectedComponentsSpring4Shell;
    default:
      return [];
  }
}
