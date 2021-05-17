/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createReducerFromActionMap } from '../../../util/reduxUtil';
import { NOTICE_DETAILS_SELECTED_NOTICE } from './componentNoticeDetailsActions';

const initialState = Object.freeze({
  noticeIndex: null,
  selectedNotice: null,
  loadingNoticeDetails: true,
});

function selectedNoticeDetail(payload, state) {
  return {
    ...state,
    noticeIndex: parseInt(payload.noticeIndex),
    selectedNotice: payload.notice,
    loadingNoticeDetails: false,
  };
}

const reducerActionMap = {
  [NOTICE_DETAILS_SELECTED_NOTICE]: selectedNoticeDetail,
};

const componentNoticeDetailsReducer = createReducerFromActionMap(reducerActionMap, initialState);
export default componentNoticeDetailsReducer;
