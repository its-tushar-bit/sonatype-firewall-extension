/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import type { ComponentDetails, Vulnerability, SubComponent, DtsDimensions } from '@guide/ui-core/types';

const sub = (publishedDate: string): SubComponent => ({
  extension: 'tgz',
  classifier: '',
  sha1: '0000000000000000000000000000000000000000',
  publishedDate,
});

export function makeDts(overall: number): DtsDimensions {
  return {
    overall,
    age: Math.round(overall * 0.9),
    license: Math.round(overall * 1.1),
    popularity: Math.round(overall * 0.95),
    releaseStability: Math.round(overall * 1.05),
    security: overall > 7 ? Math.round(overall * 0.7) : Math.round(overall * 1.1),
  };
}

export const mockComponentDetail: ComponentDetails = {
  format: 'npm',
  originId: 'pkg:npm/lodash@4.17.21',
  namespace: '',
  name: 'lodash',
  version: '4.17.21',
  registryLink: 'https://www.npmjs.com/package/lodash',
  components: [sub('2021-02-20T00:00:00Z')],
  maxCvss: 7.2,
  licenses: [{ licenseName: 'MIT' }],
  categories: ['Utility'],
  latestStable: true,
  versionScore: 78,
  isMalware: false,
  dts: makeDts(78),
};

export const mockVulnerabilities: Vulnerability[] = [
  {
    vulnId: 'CVE-2021-23337',
    aliases: ['GHSA-35jh-r3h4-6jhm'],
    summary: 'Command injection vulnerability in lodash before 4.17.21',
    cvssSeverity: 7.2,
    sonatypeCvssSeverity: 7.2,
    cwes: ['CWE-78'],
    affectedEcosystems: ['npm'],
    publishedAt: '2021-02-15T00:00:00Z',
    source: 'NVD',
  },
  {
    vulnId: 'CVE-2020-8203',
    aliases: ['GHSA-p6mc-m468-83gw'],
    summary: 'Prototype pollution vulnerability in lodash',
    cvssSeverity: 7.4,
    sonatypeCvssSeverity: 7.4,
    cwes: ['CWE-1321'],
    affectedEcosystems: ['npm'],
    publishedAt: '2020-07-15T00:00:00Z',
    source: 'NVD',
  },
];

export const mockVersions: ComponentDetails[] = [
  {
    format: 'npm', originId: 'pkg:npm/lodash@4.17.21', namespace: '', name: 'lodash',
    version: '4.17.21', registryLink: 'https://www.npmjs.com/package/lodash',
    components: [sub('2021-02-20T00:00:00Z')], maxCvss: 7.2, licenses: [{ licenseName: 'MIT' }],
    latestStable: true, versionScore: 78, isMalware: false, dts: makeDts(78),
  },
  {
    format: 'npm', originId: 'pkg:npm/lodash@4.17.20', namespace: '', name: 'lodash',
    version: '4.17.20', registryLink: 'https://www.npmjs.com/package/lodash',
    components: [sub('2020-11-15T00:00:00Z')], maxCvss: 9.8, licenses: [{ licenseName: 'MIT' }],
    latestStable: false, versionScore: 55, isMalware: false, dts: makeDts(55),
  },
  {
    format: 'npm', originId: 'pkg:npm/lodash@4.17.19', namespace: '', name: 'lodash',
    version: '4.17.19', registryLink: 'https://www.npmjs.com/package/lodash',
    components: [sub('2020-07-21T00:00:00Z')], maxCvss: 9.8, licenses: [{ licenseName: 'MIT' }],
    latestStable: false, versionScore: 50, isMalware: false, dts: makeDts(50),
  },
  {
    format: 'npm', originId: 'pkg:npm/lodash@4.0.0', namespace: '', name: 'lodash',
    version: '4.0.0', registryLink: 'https://www.npmjs.com/package/lodash',
    components: [sub('2016-01-12T00:00:00Z')], maxCvss: 0, licenses: [{ licenseName: 'MIT' }],
    latestStable: false, versionScore: 30, isMalware: false, dts: makeDts(30),
  },
  {
    format: 'npm', originId: 'pkg:npm/lodash@3.10.1', namespace: '', name: 'lodash',
    version: '3.10.1', registryLink: 'https://www.npmjs.com/package/lodash',
    components: [sub('2015-09-01T00:00:00Z')], maxCvss: 0, licenses: [{ licenseName: 'MIT' }],
    latestStable: false, versionScore: 15, isMalware: true, dts: makeDts(15),
  },
];

export const mockDependencies: ComponentDetails[] = [
  {
    format: 'npm', originId: 'pkg:npm/underscore@1.13.6', namespace: '', name: 'underscore',
    version: '1.13.6', registryLink: 'https://www.npmjs.com/package/underscore',
    components: [sub('2022-09-01T00:00:00Z')], maxCvss: 0, licenses: [{ licenseName: 'MIT' }],
    latestStable: true, versionScore: 82, isMalware: false,
  },
  {
    format: 'npm', originId: 'pkg:npm/moment@2.29.4', namespace: '', name: 'moment',
    version: '2.29.4', registryLink: 'https://www.npmjs.com/package/moment',
    components: [sub('2022-07-05T00:00:00Z')], maxCvss: 0, licenses: [{ licenseName: 'MIT' }],
    latestStable: true, versionScore: 70, isMalware: false,
  },
];
