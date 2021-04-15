/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default {
  getRepositoriesUrl: function () {
    return {
      repositories: [
        {
          oldestEvalTimestamp: null,
          managerInstanceId: 'df8ed3e3784d44ca922b406359f84811',
          repository: {
            id: '94ecade291ed42cfa63051dcf6644e2e',
            repositoryManagerId: '70524eae543a4b45bb965a905e0f78f7',
            publicId: 'apache-snapshots',
            enabled: true,
            quarantineEnabled: false,
            format: null,
          },
        },
        {
          oldestEvalTimestamp: null,
          managerInstanceId: 'df8ed3e3784d44ca922b406359f84811',
          repository: {
            id: 'd00f2f6e1b594d0e98505d5ef1518a1f',
            repositoryManagerId: '70524eae543a4b45bb965a905e0f78f7',
            publicId: 'central',
            enabled: false,
            quarantineEnabled: false,
            format: null,
          },
        },
      ],
    };
  },
};
