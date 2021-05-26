/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export const noticeState = {
  advancedLegal: {
    component: {
      component: {
        licenseLegalData: {
          showNoticesModal: false,
          noticeFiles: [
            {
              relPath: '/test/NOTICE',
              content: 'you must include notice for this fake notice file',
            },
            {
              relPath: '/test/sub/notice.txt',
              content: 'Apache Royale bla bla bla',
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
  componentNoticeDetails: {
    selectedNotice: 'selectedNotice',
    loadingNoticeDetails: 'loadingNoticeDetails',
  },
  router: {
    currentParams: {
      hash: 'fooHash',
      ownerType: 'organization',
      ownerId: 'org',
      noticeIndex: '0',
    },
    currentState: { name: 'componentNoticeDetails.noticeDetails' },
  },
};
