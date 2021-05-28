/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export const licenseFilesState = {
  advancedLegal: {
    component: {
      component: {
        licenseLegalData: {
          licenseFiles: [
            {
              relPath: '/test/LICENSE',
              content: 'BSD 3-clause license',
            },
            {
              relPath: '/test/sub/license.txt',
              content: 'BSD 2-clause licenses',
            },
          ],
        },
      },
      licenseLegalMetadata: 'licenseLegalMetadata',
      loading: 'loading',
      error: 'error',
    },
    availableScopes: {
      loading: false,
      error: null,
      values: [],
    },
  },
  componentLicenseFileDetails: {
    selectedLicense: 'selectedLicense',
    loadingLicenseDetails: 'loadingLicenseDetails',
  },
  router: {
    currentParams: {
      hash: 'fooHash',
      ownerType: 'organization',
      ownerId: 'org',
      licenseIndex: '0',
    },
    currentState: { name: 'componentLicenseFilesDetails.licenseFilesDetails' },
  },
};
