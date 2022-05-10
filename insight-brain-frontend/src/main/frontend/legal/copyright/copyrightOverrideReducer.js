/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createReducerFromActionMap, propSetConst } from '../../util/reduxUtil';
import {
  COPYRIGHT_OVERRIDE_FAILED,
  COPYRIGHT_OVERRIDE_SAVE_FULFILLED,
  COPYRIGHT_OVERRIDE_SAVE_REQUESTED,
  COPYRIGHT_OVERRIDE_SUBMIT_MASK_DONE,
  SET_DISPLAY_COPYRIGHT_OVERRIDE_MODAL,
} from './copyrightOverrideFormActions';
import { Messages } from '../../utilAngular/CommonServices';
import { ADVANCED_LEGAL_SAVE_OBLIGATION_SUBMIT_MASK_DONE } from '../obligation/advancedLegalObligationActions';

const initialState = Object.freeze({
  saveCopyrightError: null,
  submitMaskState: null,
  showEditCopyrightOverrideModal: false,
});

function saveCopyrightOverrideFailed(payload, state) {
  return {
    ...state,
    saveCopyrightError: Messages.getHttpErrorMessage(payload),
    submitMaskState: null,
  };
}

function saveCopyrightFulfilled(_, state) {
  return {
    ...state,
    saveCopyrightError: null,
    submitMaskState: true,
  };
}

function setEditCopyrightOverrideModal(payload, state) {
  if (payload === false) {
    return {
      ...state,
      saveCopyrightError: null,
      submitMaskState: null,
      showEditCopyrightOverrideModal: false,
    };
  }
  return {
    ...state,
    showEditCopyrightOverrideModal: payload,
  };
}

function saveCopyrightOverrideSubmitMaskDone(state) {
  return {
    ...state,
    saveCopyrightError: null,
    submitMaskState: null,
    showEditCopyrightOverrideModal: false,
  };
}

const reducerActionMap = {
  [COPYRIGHT_OVERRIDE_FAILED]: saveCopyrightOverrideFailed,
  [COPYRIGHT_OVERRIDE_SAVE_REQUESTED]: propSetConst('submitMaskState', false),
  [COPYRIGHT_OVERRIDE_SAVE_FULFILLED]: saveCopyrightFulfilled,
  [COPYRIGHT_OVERRIDE_SUBMIT_MASK_DONE]: saveCopyrightOverrideSubmitMaskDone,
  [ADVANCED_LEGAL_SAVE_OBLIGATION_SUBMIT_MASK_DONE]: (payload, state) => saveCopyrightOverrideSubmitMaskDone(state),
  [SET_DISPLAY_COPYRIGHT_OVERRIDE_MODAL]: setEditCopyrightOverrideModal,
};

const copyrightOverrideReducer = createReducerFromActionMap(reducerActionMap, initialState);
export default copyrightOverrideReducer;
