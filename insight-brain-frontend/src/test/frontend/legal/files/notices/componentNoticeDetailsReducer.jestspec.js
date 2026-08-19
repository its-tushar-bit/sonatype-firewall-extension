/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reduce from '../../../../../main/frontend/legal/files/notices/componentNoticeDetailsReducer';
import { NOTICE_DETAILS_SELECTED_NOTICE } from '../../../../../main/frontend/legal/files/notices/componentNoticeDetailsActions';

describe('componentNoticeDetailsReducer', function () {
  describe('initial state', function () {
    it('is used if no state is provided', function () {
      const action = { type: 'UNKNOWN' };
      const newState = reduce(undefined, action);
      expect(newState).not.toBeUndefined();
    });

    it('has default fields', function () {
      const action = { type: 'UNKNOWN' };
      const newState = reduce(undefined, action);

      expect(newState.selectedNotice).toBeNull();
      expect(newState.noticeIndex).toBeNull();
      expect(newState.loadingNoticeDetails).toBeTruthy();
    });
  });

  describe('unknown action', function () {
    it('returns original state', function () {
      const state = { foo: 'bar' };
      const action = {
        type: 'UNKNOWN',
      };
      const newState = reduce(state, action);
      expect(newState).toBe(state);
    });
  });

  describe('Notice Details action', function () {
    const originalState = {
      noticeIndex: null,
      selectedNotice: null,
      loadingNoticeDetails: true,
    };

    it('NOTICE_DETAILS_SELECTED_NOTICE sets selected notice', function () {
      const newState = reduce(originalState, {
        type: NOTICE_DETAILS_SELECTED_NOTICE,
        payload: {
          notice: { relPath: '/notice/', content: 'notice' },
          noticeIndex: 1,
        },
      });
      expect(newState.loadingNoticeDetails).toBeFalsy();
      expect(newState.selectedNotice).toEqual({ relPath: '/notice/', content: 'notice' });
      expect(newState.noticeIndex).toBe(1);
    });
  });
});
