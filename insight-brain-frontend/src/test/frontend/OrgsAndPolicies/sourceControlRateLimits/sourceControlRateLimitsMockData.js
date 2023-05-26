/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export const SOURCE_CONTROL_RATE_LIMITS_ORGANIZATION_MOCK_DATA = {
  ownerType: 'organization',
  ownerId: 'ROOT_ORGANIZATION_ID',
  ownerPublicId: 'ROOT_ORGANIZATION_ID',
  ownerName: 'Root Organization',
  userRateLimits: [
    {
      user: 'userA',
      provider: 'github',
      definingOwners: [
        {
          ownerPublicId: 'ROOT_ORGANIZATION_ID',
          ownerId: 'ROOT_ORGANIZATION_ID',
          ownerName: 'Root Organization',
          ownerType: 'ORGANIZATION',
        },
      ],
      associatedApplications: [
        {
          ownerPublicId: 'algs4__bobbysmith00001111',
          ownerId: '8a0912f8b6e64079973519bd956b3fb3',
          ownerName: 'Algs4 - Bobbysmith00001111',
          ownerType: 'APPLICATION',
        },
      ],
      rateLimits: [
        {
          category: 'core',
          remaining: 3000,
          limit: 5000,
          resetEpochTime: 1684752055,
        },
        {
          category: 'graphql',
          remaining: 5000,
          limit: 5000,
          resetEpochTime: 1684755655,
        },
      ],
    },
    {
      user: 'userB',
      provider: 'github',
      definingOwners: [
        {
          ownerPublicId: 'dbd8bbcb3b1a41018bbabb5d1b056f62',
          ownerId: 'dbd8bbcb3b1a41018bbabb5d1b056f62',
          ownerName: 'org4',
          ownerType: 'ORGANIZATION',
        },
        {
          ownerPublicId: 'relay-devtools',
          ownerId: '17c4ab720bf64becba6be857bda65ffa',
          ownerName: 'relay-devtools',
          ownerType: 'APPLICATION',
        },
      ],
      associatedApplications: [
        {
          ownerPublicId: 'dclassify',
          ownerId: '63b59494fd7047d582b3aa4c530533cd',
          ownerName: 'dclassify',
          ownerType: 'APPLICATION',
        },
        {
          ownerPublicId: 'relay-devtools',
          ownerId: '17c4ab720bf64becba6be857bda65ffa',
          ownerName: 'relay-devtools',
          ownerType: 'APPLICATION',
        },
      ],
      rateLimits: [
        {
          category: 'core',
          remaining: 5000,
          limit: 5000,
          resetEpochTime: 1684759255,
        },
        {
          category: 'graphql',
          remaining: 2000,
          limit: 5000,
          resetEpochTime: 1684762855,
        },
      ],
    },
  ],
};

export const SOURCE_CONTROL_RATE_LIMITS_APPLICATION_MOCK_DATA = {
  ownerType: 'application',
  ownerId: '17c4ab720bf64becba6be857bda65ffa',
  ownerPublicId: 'relay-devtools',
  ownerName: 'relay-devtools',
  userRateLimits: [
    {
      user: 'userA',
      provider: 'github',
      definingOwners: [
        {
          ownerPublicId: 'relay-devtools',
          ownerId: '17c4ab720bf64becba6be857bda65ffa',
          ownerName: 'relay-devtools',
          ownerType: 'APPLICATION',
        },
      ],
      associatedApplications: [
        {
          ownerPublicId: 'relay-devtools',
          ownerId: '17c4ab720bf64becba6be857bda65ffa',
          ownerName: 'relay-devtools',
          ownerType: 'APPLICATION',
        },
      ],
      rateLimits: [
        {
          category: 'core',
          remaining: 1000,
          limit: 5000,
          resetEpochTime: 1684752055,
        },
        {
          category: 'graphql',
          remaining: 0,
          limit: 5000,
          resetEpochTime: 1684755655,
        },
      ],
    },
  ],
};
