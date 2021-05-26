/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  loadComponentAndNoticeDetails,
  NOTICE_DETAILS_SELECTED_NOTICE,
  refreshNoticeFilesDetails,
} from '../../../../../main/frontend/legal/files/notices/componentNoticeDetailsActions';

describe('ComponentNoticeDetailsAction', function () {
  let store;
  let initialState = {
    advancedLegal: {
      component: {
        component: {
          componentIdentifier: 'componentIdentifier',
          hash: 'componentHash',
          licenseLegalData: {
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
      },
      availableScopes: {
        values: [
          { id: 'org', publicId: 'org', type: 'organization' },
          {
            id: 'ROOT_ORGANIZATION_ID',
            publicId: 'ROOT_ORGANIZATION_ID',
            type: 'organization',
          },
        ],
      },
    },
    router: {
      currentParams: {
        ownerType: 'organization',
        ownerId: 'org',
        hash: 'componentHash',
        noticeIndex: '1',
      },
    },
    componentNoticeDetails: {
      noticeIndex: 1,
    },
  };

  describe('load notice details', function () {
    it('immediately dispatches a NOTICE_DETAILS_SELECTED_NOTICE action. Component already in state', function () {
      store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(loadComponentAndNoticeDetails('organization', 'org', 'componentHash', 1));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(NOTICE_DETAILS_SELECTED_NOTICE);
    });
  });

  describe('refresh notice file details', function () {
    it('immediately dispatches a NOTICE_DETAILS_SELECTED_NOTICE action', function () {
      store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(refreshNoticeFilesDetails());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(NOTICE_DETAILS_SELECTED_NOTICE);
    });
  });
});
