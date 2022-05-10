/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createReducerFromActionMap, propSetConst } from '../../util/reduxUtil';
import { Messages } from '../../utilAngular/CommonServices';
import { ADVANCED_LEGAL_SAVE_OBLIGATION_SUBMIT_MASK_DONE } from '../obligation/advancedLegalObligationActions';
import {
  ORIGINAL_SOURCES_OVERRIDE_FAILED,
  ORIGINAL_SOURCES_OVERRIDE_SAVE_FULFILLED,
  ORIGINAL_SOURCES_OVERRIDE_SAVE_REQUESTED,
  ORIGINAL_SOURCES_OVERRIDE_SUBMIT_MASK_DONE,
  SET_DISPLAY_ORIGINAL_SOURCES_OVERRIDE_MODAL,
} from 'MainRoot/legal/originalSources/originalSourcesFormActions';

const initialState = Object.freeze({
  saveOriginalSourceError: null,
  submitMaskState: null,
  showOriginalSourcesModal: false,
});

function saveCopyrightOverrideFailed(payload, state) {
  return {
    ...state,
    saveOriginalSourceError: Messages.getHttpErrorMessage(payload),
    submitMaskState: null,
  };
}

function saveCopyrightFulfilled(_, state) {
  return {
    ...state,
    saveOriginalSourceError: null,
    submitMaskState: true,
  };
}

function setEditCopyrightOverrideModal(payload, state) {
  if (payload === false) {
    return {
      ...state,
      saveOriginalSourceError: null,
      submitMaskState: null,
      showOriginalSourcesModal: false,
    };
  }
  return {
    ...state,
    showOriginalSourcesModal: payload,
  };
}

function saveCopyrightOverrideSubmitMaskDone(state) {
  return {
    ...state,
    saveOriginalSourceError: null,
    submitMaskState: null,
    showOriginalSourcesModal: false,
  };
}

const reducerActionMap = {
  [ORIGINAL_SOURCES_OVERRIDE_FAILED]: saveCopyrightOverrideFailed,
  [ORIGINAL_SOURCES_OVERRIDE_SAVE_REQUESTED]: propSetConst('submitMaskState', false),
  [ORIGINAL_SOURCES_OVERRIDE_SAVE_FULFILLED]: saveCopyrightFulfilled,
  [ORIGINAL_SOURCES_OVERRIDE_SUBMIT_MASK_DONE]: saveCopyrightOverrideSubmitMaskDone,
  [ADVANCED_LEGAL_SAVE_OBLIGATION_SUBMIT_MASK_DONE]: (_payload, state) => saveCopyrightOverrideSubmitMaskDone(state),
  [SET_DISPLAY_ORIGINAL_SOURCES_OVERRIDE_MODAL]: setEditCopyrightOverrideModal,
};

const originalSourcesFormReducer = createReducerFromActionMap(reducerActionMap, initialState);
export default originalSourcesFormReducer;
