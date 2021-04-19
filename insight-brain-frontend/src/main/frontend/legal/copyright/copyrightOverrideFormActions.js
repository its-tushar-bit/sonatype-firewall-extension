/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { noPayloadActionCreator, payloadParamActionCreator } from '../../util/reduxUtil';
import { getComponentCopyrightOverrideUrl, getSaveComponentCopyrightOverrideUrl } from '../../util/CLMLocation';
import axios from 'axios';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { isScopeOverride } from '../legalUtility';
import { saveObligation } from '../obligation/advancedLegalObligationActions';
import { refreshCopyrightDetails } from './componentCopyrightDetailsActions';

export const COPYRIGHT_OVERRIDE_SAVE_REQUESTED = 'COPYRIGHT_OVERRIDE_SAVE_REQUESTED';
export const COPYRIGHT_OVERRIDE_SAVE_FULFILLED = 'COPYRIGHT_OVERRIDE_SAVE_FULFILLED';
export const COPYRIGHT_OVERRIDE_FAILED = 'COPYRIGHT_OVERRIDE_FAILED';
export const COPYRIGHT_OVERRIDE_SUBMIT_MASK_DONE = 'COPYRIGHT_OVERRIDE_SUBMIT_MASK_DONE';
export const SET_DISPLAY_COPYRIGHT_OVERRIDE_MODAL = 'SET_DISPLAY_COPYRIGHT_OVERRIDE_MODAL';

export function saveCopyrightOverride({
  copyrights,
  scopeOwnerId,
  existingObligation,
  isCopyrightsDirty,
  isObligationDirty,
}) {
  return function (dispatch, getState) {
    if (isCopyrightsDirty) {
      const advancedLegalState = getState().advancedLegal;
      const { availableScopes } = advancedLegalState;
      const componentIdentifier = advancedLegalState.component.component.componentIdentifier;
      const existingComponentCopyrightScopeOwnerId =
        advancedLegalState.component.component.licenseLegalData.componentCopyrightScopeOwnerId;

      //If the scope is lower, then we need to create a new ComponentCopyright. We do this
      // by setting the ID to null. If scope is higher, we will modify the existing entity.
      const isScopeOverrideValue = isScopeOverride(
        existingComponentCopyrightScopeOwnerId,
        scopeOwnerId,
        availableScopes.values
      );
      const componentCopyrightId = isScopeOverrideValue
        ? null
        : advancedLegalState.component.component.licenseLegalData.componentCopyrightId;

      const payload = {
        id: componentCopyrightId,
        componentIdentifier: componentIdentifier,
        copyrightOverrides: copyrights.map((copyright) => ({
          ...copyright,
          id: isScopeOverrideValue ? null : copyright.id,
        })),
      };

      dispatch(saveRequested());
      const matchingScope = availableScopes.values.find((s) => s.id === scopeOwnerId);
      const visitedScope = availableScopes.values[0];
      return axios
        .post(getSaveComponentCopyrightOverrideUrl(matchingScope.type, matchingScope.publicId), payload)
        .then(() => {
          //Fetch the updated ComponentCopyright separately in case we need values at a higher scope.
          axios
            .get(getComponentCopyrightOverrideUrl(visitedScope.type, visitedScope.publicId, componentIdentifier))
            .then((getResponsePayload) => {
              const descriptiveResponse = {
                ...getResponsePayload.data.componentCopyrightDTO,
                componentCopyrightScopeOwnerId: getResponsePayload.data.ownerId,
                componentCopyrightLastUpdatedByUsername:
                  getResponsePayload.data.componentCopyrightDTO.lastUpdatedByUsername,
                componentCopyrightLastUpdatedAt: getResponsePayload.data.componentCopyrightDTO.lastUpdatedAt,
              };
              dispatch(saveFulfilled(descriptiveResponse));
              dispatch(refreshCopyrightDetails());
              isObligationDirty
                ? saveObligation(existingObligation.name)(dispatch, getState)
                : startSaveCopyrightOverrideSubmitMaskDoneTimer(dispatch);
            });
        })
        .catch((error) => {
          dispatch(saveFailed(error));
        });
    } else if (isObligationDirty) {
      return saveObligation(existingObligation.name)(dispatch, getState);
    } else {
      return;
    }
  };
}

function startSaveCopyrightOverrideSubmitMaskDoneTimer(dispatch) {
  setTimeout(() => {
    dispatch({ type: COPYRIGHT_OVERRIDE_SUBMIT_MASK_DONE });
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

const saveRequested = noPayloadActionCreator(COPYRIGHT_OVERRIDE_SAVE_REQUESTED);
const saveFulfilled = payloadParamActionCreator(COPYRIGHT_OVERRIDE_SAVE_FULFILLED);
const saveFailed = payloadParamActionCreator(COPYRIGHT_OVERRIDE_FAILED);
export const setDisplayCopyrightOverrideModal = payloadParamActionCreator(SET_DISPLAY_COPYRIGHT_OVERRIDE_MODAL);
