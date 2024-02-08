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
import { selectOwnerInfo } from 'MainRoot/reduxUiRouter/routerSelectors';

export const ORIGINAL_SOURCES_OVERRIDE_SAVE_REQUESTED = 'ORIGINAL_SOURCES_OVERRIDE_SAVE_REQUESTED';
export const ORIGINAL_SOURCES_OVERRIDE_SAVE_FULFILLED = 'ORIGINAL_SOURCES_OVERRIDE_SAVE_FULFILLED';
export const ORIGINAL_SOURCES_OVERRIDE_FAILED = 'ORIGINAL_SOURCES_OVERRIDE_FAILED';
export const ORIGINAL_SOURCES_OVERRIDE_SUBMIT_MASK_DONE = 'ORIGINAL_SOURCES_OVERRIDE_SUBMIT_MASK_DONE';
export const SET_DISPLAY_ORIGINAL_SOURCES_OVERRIDE_MODAL = 'SET_DISPLAY_ORIGINAL_SOURCES_OVERRIDE_MODAL';

export function saveOriginalSourcesOverride({ sources, existingObligation, areSourcesDirty, isObligationDirty }) {
  return function (dispatch, getState) {
    if (areSourcesDirty) {
      const advancedLegalState = getState().advancedLegal;
      const { componentIdentifier } = advancedLegalState.component.component;
      const hash = getState().router?.currentParams?.hash;
      let { ownerType, ownerId } = selectOwnerInfo(getState());
      if (ownerType === 'global') {
        ownerType = 'organization';
        ownerId = 'ROOT_ORGANIZATION_ID';
      }
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
      const componentPromise = hash
        ? loadComponent(ownerType, ownerId, hash)
        : loadComponentByComponentIdentifier(JSON.stringify(componentIdentifier));
      return axios
        .post(getSaveComponentOriginalSourcesOverrideUrl(ownerType, ownerId), payload)
        .then(dispatch(componentPromise))
        .then(() => {
          if (isObligationDirty) {
            return saveObligation(existingObligation.name, true)(dispatch, getState)
              .then(() => {
                dispatch(saveFulfilled());
              })
              .catch((error) => {
                dispatch(saveFailed(error));
              });
          } else {
            return startSaveOriginalSourcesOverrideSubmitMaskDoneTimer(dispatch);
          }
        })
        .catch((error) => {
          dispatch(saveFailed(error));
        });
    } else if (isObligationDirty) {
      dispatch(saveRequested());
      return saveObligation(existingObligation.name, true)(dispatch, getState)
        .then(() => {
          dispatch(saveFulfilled());
        })
        .catch((error) => {
          dispatch(saveFailed(error));
        });
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
