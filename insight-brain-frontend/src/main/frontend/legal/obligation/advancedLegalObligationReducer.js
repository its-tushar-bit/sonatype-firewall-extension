/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  ADVANCED_LEGAL_CANCEL_ALL_OBLIGATIONS_MODAL,
  ADVANCED_LEGAL_CANCEL_ATTRIBUTION_MODAL,
  ADVANCED_LEGAL_CANCEL_OBLIGATION_MODAL,
  ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_FAILED,
  ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_REQUESTED,
  ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_SUBMIT_MASK_DONE,
  ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_SUCCEEDED,
  ADVANCED_LEGAL_SAVE_ATTRIBUTION_FAILED,
  ADVANCED_LEGAL_SAVE_ATTRIBUTION_FULFILLED,
  ADVANCED_LEGAL_SAVE_ATTRIBUTION_REQUESTED,
  ADVANCED_LEGAL_SAVE_ATTRIBUTION_SUBMIT_MASK_DONE,
  ADVANCED_LEGAL_SAVE_OBLIGATION_FAILED,
  ADVANCED_LEGAL_SAVE_OBLIGATION_REQUESTED,
  ADVANCED_LEGAL_SAVE_OBLIGATION_SUBMIT_MASK_DONE,
  ADVANCED_LEGAL_SAVE_OBLIGATION_SUCCEEDED,
  ADVANCED_LEGAL_SET_ATTRIBUTION_SCOPE,
  ADVANCED_LEGAL_SET_ATTRIBUTION_TEXT,
  ADVANCED_LEGAL_SET_OBLIGATION_COMMENT,
  ADVANCED_LEGAL_SET_OBLIGATION_SCOPE,
  ADVANCED_LEGAL_SET_OBLIGATION_STATUS,
  ADVANCED_LEGAL_SET_SHOW_ALL_OBLIGATIONS_MODAL,
  ADVANCED_LEGAL_SET_SHOW_ATTRIBUTION_MODAL,
  ADVANCED_LEGAL_SET_SHOW_OBLIGATION_MODAL,
} from './advancedLegalObligationActions';
import { __, find, findIndex, lensPath, merge, over, propEq } from 'ramda';
import { saveNoticesSubmitMaskDone, saveLicenseFilesSubmitMaskDone } from '../files/advancedLegalFileReducer';
import { TEXT_BASED_OBLIGATIONS } from '../advancedLegalConstants';

const updateLicenseLegalData = (newLicenseLegalData, state) =>
  over(lensPath(['component', 'component', 'licenseLegalData']), merge(__, newLicenseLegalData), state);

function updateAttribution(newAttribution, obligationName, state) {
  if (obligationName !== null && !TEXT_BASED_OBLIGATIONS.indexOf(obligationName) === -1) {
    return state;
  }
  const attributionIndex = findIndex(
    propEq('obligationName', obligationName),
    state.component.component.licenseLegalData.attributions
  );
  const lens = lensPath([
    'component',
    'component',
    'licenseLegalData',
    'attributions',
    attributionIndex === -1 ? state.component.component.licenseLegalData.attributions.length : attributionIndex,
  ]);
  return over(lens, merge(__, newAttribution), state);
}

function setAttributionText(payload, state) {
  return updateAttribution({ content: payload.value }, payload.name, state);
}

function setAttributionScope(payload, state) {
  return updateAttribution({ ownerId: payload.value }, payload.name, state);
}

function setShowAttributionModal(payload, state) {
  return updateAttribution(
    {
      showAttributionModal: payload.value,
      error: null,
    },
    payload.name,
    state
  );
}

function saveAttributionRequested(payload, state) {
  return updateAttribution(
    {
      error: null,
      saveAttributionSubmitMask: false,
    },
    payload.name,
    state
  );
}

function saveAttributionFulfilled(payload, state) {
  return updateAttribution(
    {
      id: payload.value.id,
      originalContent: payload.value.content,
      content: payload.value.content,
      originalOwnerId: payload.value.ownerId,
      ownerId: payload.value.ownerId,
      lastUpdatedByUsername: payload.value.lastUpdatedByUsername,
      lastUpdatedAt: payload.value.lastUpdatedAt,
      error: null,
      saveAttributionSubmitMask: true,
    },
    payload.name,
    state
  );
}

function saveAttributionFailed(payload, state) {
  return updateAttribution(
    {
      error: payload.value,
      saveAttributionSubmitMask: null,
    },
    payload.name,
    state
  );
}

function saveAttributionSubmitMaskDone(payload, state) {
  return updateAttribution(
    {
      saveAttributionSubmitMask: null,
      showAttributionModal: false,
    },
    payload.name,
    state
  );
}

function cancelAttributionModal(payload, state) {
  const attribution = find(
    propEq('obligationName', payload.name),
    state.component.component.licenseLegalData.attributions
  );
  return updateAttribution(
    {
      showAttributionModal: false,
      content: attribution.originalContent,
      ownerId: attribution.originalOwnerId,
    },
    payload.name,
    state
  );
}

function updateObligation(newObligation, obligationName, state) {
  const obligationIndex = findIndex(
    propEq('name', obligationName),
    state.component.component.licenseLegalData.obligations
  );
  const lens = lensPath(['component', 'component', 'licenseLegalData', 'obligations', obligationIndex]);
  return over(lens, merge(__, newObligation), state);
}

function setObligationStatus(payload, state) {
  return updateObligation({ status: payload.value }, payload.name, state);
}

function setObligationComment(payload, state) {
  return updateObligation({ comment: payload.value }, payload.name, state);
}

function setObligationScope(payload, state) {
  return updateObligation({ ownerId: payload.value }, payload.name, state);
}

function setShowObligationModal(payload, state) {
  return updateObligation(
    {
      error: null,
      showObligationModal: payload.value,
    },
    payload.name,
    state
  );
}

function saveObligationRequested(payload, state) {
  return updateObligation(
    {
      error: null,
      saveObligationSubmitMask: false,
    },
    payload.name,
    state
  );
}

function saveObligationSucceeded(payload, state) {
  return updateObligation(
    {
      id: payload.value.id,
      originalStatus: payload.value.status,
      status: payload.value.status,
      originalComment: payload.value.comment,
      comment: payload.value.comment,
      originalOwnerId: payload.value.ownerId,
      ownerId: payload.value.ownerId,
      lastUpdatedByUsername: payload.value.lastUpdatedByUsername,
      lastUpdatedAt: payload.value.lastUpdatedAt,
      error: null,
      saveObligationSubmitMask: true,
    },
    payload.name,
    state
  );
}

function saveObligationFailed(payload, state) {
  return updateObligation(
    {
      error: payload.value,
      saveObligationSubmitMask: null,
    },
    payload.name,
    state
  );
}

function saveObligationSubmitMaskDone(payload, state) {
  return updateObligation(
    {
      saveObligationSubmitMask: null,
      showObligationModal: false,
    },
    payload.name,
    state
  );
}

function cancelObligationModal(payload, state) {
  const obligation = find(propEq('name', payload.name), state.component.component.licenseLegalData.obligations);
  return updateObligation(
    {
      showObligationModal: false,
      status: obligation.originalStatus,
      comment: obligation.originalComment,
      ownerId: obligation.originalOwnerId,
    },
    payload.name,
    state
  );
}

function setShowAllObligationsModal(payload, state) {
  return updateLicenseLegalData(
    {
      showAllObligationsModal: payload,
    },
    state
  );
}

function cancelAllObligationsModal(payload, state) {
  return updateLicenseLegalData(
    {
      showAllObligationsModal: false,
    },
    state
  );
}

function saveAllObligationsRequested(payload, state) {
  return updateLicenseLegalData(
    {
      saveAllObligationsSubmitMask: false,
      saveAllObligationsError: null,
    },
    state
  );
}

function saveAllObligationsSucceeded(payload, state) {
  return updateLicenseLegalData(
    {
      saveAllObligationsSubmitMask: true,
      saveAllObligationsError: null,
    },
    state
  );
}

function saveAllObligationsFailed(payload, state) {
  return updateLicenseLegalData(
    {
      saveAllObligationsError: payload.value,
      saveAllObligationsSubmitMask: null,
    },
    state
  );
}

function saveAllObligationsSubmitMaskDone(payload, state) {
  return updateLicenseLegalData(
    {
      saveAllObligationsSubmitMask: null,
      showAllObligationsModal: false,
    },
    state
  );
}

export const advancedLegalObligationReducerActionMap = {
  [ADVANCED_LEGAL_SET_ATTRIBUTION_TEXT]: setAttributionText,
  [ADVANCED_LEGAL_SET_ATTRIBUTION_SCOPE]: setAttributionScope,
  [ADVANCED_LEGAL_SET_SHOW_ATTRIBUTION_MODAL]: setShowAttributionModal,
  [ADVANCED_LEGAL_SAVE_ATTRIBUTION_REQUESTED]: saveAttributionRequested,
  [ADVANCED_LEGAL_SAVE_ATTRIBUTION_FULFILLED]: saveAttributionFulfilled,
  [ADVANCED_LEGAL_SAVE_ATTRIBUTION_FAILED]: saveAttributionFailed,
  [ADVANCED_LEGAL_SAVE_ATTRIBUTION_SUBMIT_MASK_DONE]: saveAttributionSubmitMaskDone,
  [ADVANCED_LEGAL_CANCEL_ATTRIBUTION_MODAL]: cancelAttributionModal,
  [ADVANCED_LEGAL_SET_OBLIGATION_STATUS]: setObligationStatus,
  [ADVANCED_LEGAL_SET_OBLIGATION_COMMENT]: setObligationComment,
  [ADVANCED_LEGAL_SET_OBLIGATION_SCOPE]: setObligationScope,
  [ADVANCED_LEGAL_SET_SHOW_OBLIGATION_MODAL]: setShowObligationModal,
  [ADVANCED_LEGAL_SET_SHOW_ALL_OBLIGATIONS_MODAL]: setShowAllObligationsModal,
  [ADVANCED_LEGAL_CANCEL_ALL_OBLIGATIONS_MODAL]: cancelAllObligationsModal,
  [ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_REQUESTED]: saveAllObligationsRequested,
  [ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_SUCCEEDED]: saveAllObligationsSucceeded,
  [ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_FAILED]: saveAllObligationsFailed,
  [ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_SUBMIT_MASK_DONE]: saveAllObligationsSubmitMaskDone,
  [ADVANCED_LEGAL_SAVE_OBLIGATION_REQUESTED]: saveObligationRequested,
  [ADVANCED_LEGAL_SAVE_OBLIGATION_SUCCEEDED]: saveObligationSucceeded,
  [ADVANCED_LEGAL_SAVE_OBLIGATION_FAILED]: saveObligationFailed,
  [ADVANCED_LEGAL_SAVE_OBLIGATION_SUBMIT_MASK_DONE]: (payload, state) => {
    // Manually chain required actions together, until the components have independent reducers.
    const state1 = saveObligationSubmitMaskDone(payload, state);
    const state2 = saveNoticesSubmitMaskDone(payload, state1);
    const state3 = saveLicenseFilesSubmitMaskDone(payload, state2);
    const state4 = saveAttributionSubmitMaskDone(payload, state3);
    return state4;
  },
  [ADVANCED_LEGAL_CANCEL_OBLIGATION_MODAL]: cancelObligationModal,
};
