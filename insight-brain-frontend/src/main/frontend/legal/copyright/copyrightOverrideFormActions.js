/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {noPayloadActionCreator, payloadParamActionCreator} from '../../util/reduxUtil';
import {getSaveCopyrightOverrideUrl} from '../../util/CLMLocation';
import axios from 'axios';
import {SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS} from '@sonatype/react-shared-components';
import {isScopeOverride} from '../legalUtility';

export const COPYRIGHT_OVERRIDE_SAVE_REQUESTED = 'COPYRIGHT_OVERRIDE_SAVE_REQUESTED';
export const COPYRIGHT_OVERRIDE_SAVE_FULFILLED = 'COPYRIGHT_OVERRIDE_SAVE_FULFILLED';
export const COPYRIGHT_OVERRIDE_FAILED = 'COPYRIGHT_OVERRIDE_FAILED';
export const COPYRIGHT_OVERRIDE_SUBMIT_MASK_DONE = 'COPYRIGHT_OVERRIDE_SUBMIT_MASK_DONE';
export const SET_DISPLAY_COPYRIGHT_OVERRIDE_MODAL = 'SET_DISPLAY_COPYRIGHT_OVERRIDE_MODAL';

export function saveCopyrightOverride({copyrights, scopeOwnerId}) {
  return function(dispatch, getState) {
    const advancedLegalState = getState().advancedLegal;
    const {availableScopes} = advancedLegalState;
    const componentIdentifier = advancedLegalState.component.component.componentIdentifier;
    const existingComponentCopyrightScopeOwnerId =
        advancedLegalState.component.component.licenseLegalData.componentCopyrightScopeOwnerId;

    //If the scope is lower, then we need to create a new ComponentCopyright. We do this
    // by setting the ID to null. If scope is higher, we will modify the existing entity.
    const componentCopyrightId = isScopeOverride(existingComponentCopyrightScopeOwnerId, scopeOwnerId,
        availableScopes.values) ? null : advancedLegalState.component.component.licenseLegalData.componentCopyrightId;

    const payload = {
      id: componentCopyrightId,
      componentIdentifier: componentIdentifier,
      copyrightOverrides: copyrights
    };

    dispatch(saveRequested());
    const matchingScope = availableScopes.values.find(s => s.id === scopeOwnerId);
    return axios.post(getSaveCopyrightOverrideUrl(matchingScope.type, matchingScope.publicId), payload)
        .then((responsePayload) => {
          const descriptiveResponse = {
            ...responsePayload.data,
            componentCopyrightScopeOwnerId: scopeOwnerId
          };
          dispatch(saveFulfilled(descriptiveResponse));
          startSaveCopyrightOverrideSubmitMaskDoneTimer(dispatch, descriptiveResponse);
        })
        .catch(error => {
          dispatch(saveFailed(error));
        });
  };
}

function startSaveCopyrightOverrideSubmitMaskDoneTimer(dispatch, payload) {
  setTimeout(() => {
    dispatch({type: COPYRIGHT_OVERRIDE_SUBMIT_MASK_DONE, payload: payload});
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

const saveRequested = noPayloadActionCreator(COPYRIGHT_OVERRIDE_SAVE_REQUESTED);
const saveFulfilled = payloadParamActionCreator(COPYRIGHT_OVERRIDE_SAVE_FULFILLED);
const saveFailed = payloadParamActionCreator(COPYRIGHT_OVERRIDE_FAILED);
export const setDisplayCopyrightOverrideModal = payloadParamActionCreator(SET_DISPLAY_COPYRIGHT_OVERRIDE_MODAL);
