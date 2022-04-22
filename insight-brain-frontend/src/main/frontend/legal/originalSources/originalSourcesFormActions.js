/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { noPayloadActionCreator, payloadParamActionCreator } from '../../util/reduxUtil';
import { getSaveComponentOriginalSourcesOverrideUrl } from '../../util/CLMLocation';
import axios from 'axios';
import { saveObligation } from '../obligation/advancedLegalObligationActions';
import { loadComponent, loadComponentByComponentIdentifier } from 'MainRoot/legal/advancedLegalActions';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

export const ORIGINAL_SOURCES_OVERRIDE_SAVE_REQUESTED = 'ORIGINAL_SOURCES_OVERRIDE_SAVE_REQUESTED';
export const ORIGINAL_SOURCES_OVERRIDE_SAVE_FULFILLED = 'ORIGINAL_SOURCES_OVERRIDE_SAVE_FULFILLED';
export const ORIGINAL_SOURCES_OVERRIDE_FAILED = 'ORIGINAL_SOURCES_OVERRIDE_FAILED';
export const ORIGINAL_SOURCES_OVERRIDE_SUBMIT_MASK_DONE = 'ORIGINAL_SOURCES_OVERRIDE_SUBMIT_MASK_DONE';
export const SET_DISPLAY_ORIGINAL_SOURCES_OVERRIDE_MODAL = 'SET_DISPLAY_ORIGINAL_SOURCES_OVERRIDE_MODAL';

export function saveOriginalSourcesOverride({
  sources,
  scopeOwnerId,
  existingObligation,
  areSourcesDirty,
  isObligationDirty,
}) {
  return function (dispatch, getState) {
    if (areSourcesDirty) {
      const advancedLegalState = getState().advancedLegal;
      const { availableScopes } = advancedLegalState;
      const { componentIdentifier } = advancedLegalState.component.component;
      const hash = getState().router?.currentParams?.hash;
      const packageUrl = advancedLegalState.component.component.packageUrl;
      const payload = {
        componentIdentifier,
        packageUrl,
        sourceLinkOverrides: sources.map((source) => ({
          ...source,
          id: source.id,
        })),
      };

      dispatch(saveRequested());
      const matchingScope = availableScopes.values.find((s) => s.id === scopeOwnerId);
      const componentPromise = hash
        ? loadComponent(matchingScope.type, matchingScope.publicId, hash)
        : loadComponentByComponentIdentifier(JSON.stringify(componentIdentifier));
      return axios
        .post(getSaveComponentOriginalSourcesOverrideUrl(matchingScope.type, matchingScope.publicId), payload)
        .then(() => {
          dispatch(saveFulfilled());
          dispatch(componentPromise).then(
            isObligationDirty
              ? saveObligation(existingObligation.name)(dispatch, getState)
              : startSaveOriginalSourcesOverrideSubmitMaskDoneTimer(dispatch)
          );
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

function startSaveOriginalSourcesOverrideSubmitMaskDoneTimer(dispatch) {
  setTimeout(() => {
    dispatch({ type: ORIGINAL_SOURCES_OVERRIDE_SUBMIT_MASK_DONE });
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

const saveRequested = noPayloadActionCreator(ORIGINAL_SOURCES_OVERRIDE_SAVE_REQUESTED);
const saveFulfilled = noPayloadActionCreator(ORIGINAL_SOURCES_OVERRIDE_SAVE_FULFILLED);
const saveFailed = payloadParamActionCreator(ORIGINAL_SOURCES_OVERRIDE_FAILED);
export const setDisplayOriginalSourcesOverrideModal = payloadParamActionCreator(
  SET_DISPLAY_ORIGINAL_SOURCES_OVERRIDE_MODAL
);
