/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { noPayloadActionCreator, payloadParamActionCreator } from '../../util/reduxUtil';
import { find, propEq } from 'ramda';
import axios from 'axios';
import { getSaveLegalFileUrl, getLegalFileUrl } from '../../util/CLMLocation';
import { Messages } from '../../util/CommonServices';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { isScopeOverride } from '../legalUtility';

export const ADVANCED_LEGAL_SET_SHOW_NOTICES_MODAL = 'ADVANCED_LEGAL_SET_SHOW_NOTICES_MODAL';
export const ADVANCED_LEGAL_CANCEL_NOTICES_MODAL = 'ADVANCED_LEGAL_CANCEL_NOTICES_MODAL';
export const ADVANCED_LEGAL_SET_NOTICE_CONTENT = 'ADVANCED_LEGAL_SET_NOTICE_CONTENT';
export const ADVANCED_LEGAL_SET_NOTICE_STATUS = 'ADVANCED_LEGAL_SET_NOTICE_STATUS';
export const ADVANCED_LEGAL_ADD_NOTICE = 'ADVANCED_LEGAL_ADD_NOTICE';
export const ADVANCED_LEGAL_SET_NOTICES_SCOPE = 'ADVANCED_LEGAL_SET_NOTICES_SCOPE';
export const ADVANCED_LEGAL_SAVE_NOTICES_REQUESTED = 'ADVANCED_LEGAL_SAVE_NOTICES_REQUESTED';
export const ADVANCED_LEGAL_SAVE_NOTICES_SUCCEEDED = 'ADVANCED_LEGAL_SAVE_NOTICES_SUCCEEDED';
export const ADVANCED_LEGAL_SAVE_NOTICES_FAILED = 'ADVANCED_LEGAL_SAVE_NOTICES_FAILED';
export const ADVANCED_LEGAL_SAVE_NOTICES_SUBMIT_MASK_DONE = 'ADVANCED_LEGAL_SAVE_NOTICES_SUBMIT_MASK_DONE';

export const setShowNoticesModal = payloadParamActionCreator(ADVANCED_LEGAL_SET_SHOW_NOTICES_MODAL);
export const cancelNoticesModal = noPayloadActionCreator(ADVANCED_LEGAL_CANCEL_NOTICES_MODAL);
export const setNoticeContent = payloadParamActionCreator(ADVANCED_LEGAL_SET_NOTICE_CONTENT);
export const setNoticeStatus = payloadParamActionCreator(ADVANCED_LEGAL_SET_NOTICE_STATUS);
export const addNotice = payloadParamActionCreator(ADVANCED_LEGAL_ADD_NOTICE);
export const setNoticesScope = payloadParamActionCreator(ADVANCED_LEGAL_SET_NOTICES_SCOPE);

const saveNoticesRequested = noPayloadActionCreator(ADVANCED_LEGAL_SAVE_NOTICES_REQUESTED);
const saveNoticesSucceeded = payloadParamActionCreator(ADVANCED_LEGAL_SAVE_NOTICES_SUCCEEDED);
const saveNoticesFailed = payloadParamActionCreator(ADVANCED_LEGAL_SAVE_NOTICES_FAILED);
const saveNoticesSubmitMaskDone = noPayloadActionCreator(ADVANCED_LEGAL_SAVE_NOTICES_SUBMIT_MASK_DONE);

export function saveNotices() {
  return (dispatch, getState) => {
    dispatch(saveNoticesRequested());

    const advancedLegalState = getState().advancedLegal;
    const { values: availableScopeValues } = advancedLegalState.availableScopes;
    const { licenseLegalData, componentIdentifier } = advancedLegalState.component.component;
    const {
      componentLegalFileId,
      componentLegalFileScopeOwnerId: ownerId,
      originalComponentLegalFileScopeOwnerId: originalOwnerId,
      noticeFiles
    } = licenseLegalData;
    const scope = find(propEq('id', ownerId), availableScopeValues);
    const ownerType = scope.type;
    const ownerPublicId = scope.publicId;
    const isScopeOverrideValue = isScopeOverride(originalOwnerId, ownerId, availableScopeValues);
    const payload = {
      id: isScopeOverrideValue ? null : componentLegalFileId,
      componentIdentifier,
      legalFileOverrides: noticeFiles.map(noticeFile => {
        return {
          id: isScopeOverrideValue ? null : noticeFile.id,
          legalFileType: 'notice',
          originalContentHash: noticeFile.originalContentHash,
          content: noticeFile.content,
          status: noticeFile.status
        };
      })
    };

    return axios.post(getSaveLegalFileUrl(ownerType, ownerPublicId), payload)
        .then(() => {
          axios.get(getLegalFileUrl(ownerType, ownerPublicId, componentIdentifier))
              .then(responsePayload => {
                dispatch(saveNoticesSucceeded(responsePayload.data));
                startSaveNoticesSubmitMaskDoneTimer(dispatch);
              })
              .catch(error => {
                dispatch(saveNoticesFailed(Messages.getHttpErrorMessage(error)));
              });
        })
        .catch(error => {
          dispatch(saveNoticesFailed(Messages.getHttpErrorMessage(error)));
        });
  };
}

function startSaveNoticesSubmitMaskDoneTimer(dispatch) {
  setTimeout(() => dispatch(saveNoticesSubmitMaskDone()), SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}
