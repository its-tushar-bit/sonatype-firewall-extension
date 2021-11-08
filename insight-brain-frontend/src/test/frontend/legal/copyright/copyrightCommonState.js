/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { copyrightDetailsStateNameSuffix } from '../../../../main/frontend/legal/copyright/copyrightDetailsUtils';

export const copyrightState = {
  advancedLegal: {
    component: {
      component: {
        licenseLegalData: {
          copyrights: [
            { originalContentHash: 'hash1', content: 'content1' },
            { originalContentHash: 'hash2', content: 'content2' },
            { originalContentHash: null, content: 'content3' },
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
  componentCopyrightDetails: {
    selectedCopyright: 'selectedCopyright',
    filePathsPage: 1,
    loadingCopyrightDetails: 'loadingCopyrightDetails',
    loadingFilePaths: 'loadingFilePaths',
    loadingCopyrightContext: 'loadingCopyrightContext',
    errorCopyrightFileCounts: 'errorCopyrightFileCounts',
    errorCopyrightContext: 'errorCopyrightContext',
    errorFilePaths: 'errorFilePaths',
    filePaths: ['path1', 'path2'],
    totalFileMatches: 2,
    copyrightContexts: ['context1', 'context2'],
    copyrightFileCounts: { path1: 1, path2: 2 },
  },
  router: {
    currentParams: {
      hash: 'fooHash',
      componentIdentifier: 'fooComponentIdentifier',
      ownerType: 'organization',
      ownerId: 'org',
      copyrightIndex: '12',
    },
    currentState: { name: `copyrightDetails.${copyrightDetailsStateNameSuffix}` },
  },
  copyrightOverrides: {
    showEditCopyrightOverrideModal: false,
  },
};
