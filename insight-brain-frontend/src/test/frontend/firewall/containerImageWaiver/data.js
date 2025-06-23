/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export const activeViolationsResult = [
  {
    applicationId: '56303ab393af47e887515f1034a2e586',
    applicationName: 'YWJkEokaTRKVYcMszbMOTA-library-alpine-3.6',
    componentIdentifier: {
      format: 'container',
      coordinates: {
        name: 'apk-tools',
        namespace: 'alpine:3.6.5',
        version: '2.7.6-r0',
      },
    },
    hash: '9cd309492780e10b8349',
    id: 'e693980b3fcb455cb812c1e9db75ea5c',
    policyId: 'adb53ef39b0b4cbc8084192b130159cf',
    policyName: 'docker-policy-2.7.6-r0',
    threatCategory: 'OTHER',
    threatLevel: 6,
    time: 0,
    filename: 'apk-tools@2.7.6-r0?nexustype=container',
  },
  {
    applicationId: '56303ab393af47e887515f1034a2e586',
    applicationName: 'YWJkEokaTRKVYcMszbMOTA-library-alpine-3.6',
    componentIdentifier: {
      format: 'container',
      coordinates: {
        name: 'libc-dev',
        namespace: 'alpine:3.6.5',
        version: '0.7.1-r0',
      },
    },
    hash: '16ba57bc725b115caa47',
    id: '5ecf34dbc7754c818602e47b7572cbf3',
    policyId: '0bc8df83f6534b0fb5cff77130a75e89',
    policyName: 'docker-all',
    threatCategory: 'OTHER',
    threatLevel: 9,
    time: 0,
    filename: 'libc-dev@0.7.1-r0?nexustype=container',
  },

  {
    applicationId: '56303ab393af47e887515f1034a2e586',
    applicationName: 'YWJkEokaTRKVYcMszbMOTA-library-alpine-3.6',
    componentIdentifier: {
      format: 'container',
      coordinates: {
        name: 'libressl',
        namespace: 'alpine:3.6.5',
        version: '2.5.5-r2',
      },
    },
    hash: '619f27db8ed8059bb7dc',
    id: '93fddee70aab4020b5420b26d5811026',
    policyId: '1606aa16d5cd4cf6b0b014b85e94f7db',
    policyName: 'docker-policy-2.5.5-r2',
    threatCategory: 'OTHER',
    threatLevel: 3,
    time: 0,
    filename: 'libressl@2.5.5-r2?nexustype=container',
  },
];

export const waiverReasons = [
  {
    id: '9b704ef5bc064fc29d7fe08a251ee9a6',
    type: 'system',
    reasonText: 'Acknowledged violation',
  },
  {
    id: '42069f58114f4df8b435a40a415d2835',
    type: 'system',
    reasonText: 'Mitigated externally',
  },
  {
    id: '39984de3d6e64f508df82b4cbfd72f70',
    type: 'system',
    reasonText: 'No upgrade path',
  },
  {
    id: 'f6990a32cd8d4ea78853ca829d948927',
    type: 'system',
    reasonText: 'Not exploitable',
  },
  {
    id: '19bbf1a7d591497698ab3172461d971a',
    type: 'system',
    reasonText: 'Not reachable',
  },
  {
    id: '3446e70e60e04676a90131f3dea9bdb5',
    type: 'system',
    reasonText: 'Researching',
  },
  {
    id: 'c991ef95866d4903ad0c6c217ac47c07',
    type: 'system',
    reasonText: 'Other',
  },
];
