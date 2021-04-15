/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
window.ProprietaryMockData = {
  getProprietaryConfigurationStoreMockData: function () {
    return {
      proprietaryConfigByOwners: [
        {
          ownerId: 'ownerID',
          ownerName: 'App Name',
          ownerType: 'application',
          proprietaryConfig: [
            {
              id: 'configId',
              ownerId: 'ownerId',
              packages: ['com.sonatype', 'com.local'],
              regexes: ['.*/test\\.zip'],
            },
          ],
        },
        {
          ownerId: 'ROOT_ORGANIZATION_ID',
          ownerName: 'Root Organization',
          ownerType: 'organization',
          proprietaryConfig: [
            {
              id: null,
              ownerId: 'ROOT_ORGANIZATION_ID',
              packages: [],
              regexes: ['.*/foo\\.zip'],
            },
          ],
        },
      ],
    };
  },
  getProprietaryConfiguration: function () {
    return {
      proprietaryConfigByOwners: [
        {
          ownerId: 'ownerID',
          ownerName: 'App Name',
          ownerType: 'application',
          proprietaryConfig: {
            id: 'configId',
            ownerId: 'ownerId',
            packages: ['com.sonatype', 'com.local'],
            regexes: ['.*/test\\.zip'],
          },
        },
        {
          ownerId: 'ROOT_ORGANIZATION_ID',
          ownerName: 'Root Organization',
          ownerType: 'organization',
          proprietaryConfig: {
            id: null,
            ownerId: 'ROOT_ORGANIZATION_ID',
            packages: [],
            regexes: ['.*/foo\\.zip'],
          },
        },
      ],
    };
  },
};
