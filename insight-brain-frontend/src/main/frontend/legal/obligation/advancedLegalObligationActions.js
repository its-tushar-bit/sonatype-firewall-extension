/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import {
  getComponentObligationAttributionUrl,
  getComponentObligationUrl,
  getDeleteComponentObligationAttributionUrl,
  getDeleteComponentObligationsUrl,
  getSaveComponentObligationAttributionUrl,
  getSaveComponentObligationsUrl,
  getSaveComponentObligationUrl,
} from '../../util/CLMLocation';
import { payloadParamActionCreator } from '../../util/reduxUtil';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { find, pick, propEq } from 'ramda';
import { Messages } from '../../utilAngular/CommonServices';
import { isScopeOverride } from '../legalUtility';
import { OBLIGATION_STATUS_OPEN } from '../advancedLegalConstants';

export const ADVANCED_LEGAL_SET_ATTRIBUTION_TEXT = 'ADVANCED_LEGAL_SET_ATTRIBUTION_TEXT';
export const ADVANCED_LEGAL_SET_ATTRIBUTION_SCOPE = 'ADVANCED_LEGAL_SET_ATTRIBUTION_SCOPE';
export const ADVANCED_LEGAL_SET_SHOW_ATTRIBUTION_MODAL = 'ADVANCED_LEGAL_SET_SHOW_ATTRIBUTION_MODAL';
export const ADVANCED_LEGAL_CANCEL_ATTRIBUTION_MODAL = 'ADVANCED_LEGAL_CANCEL_ATTRIBUTION_MODAL';

export const setAttributionText = payloadParamActionCreator(ADVANCED_LEGAL_SET_ATTRIBUTION_TEXT);
export const setAttributionScope = payloadParamActionCreator(ADVANCED_LEGAL_SET_ATTRIBUTION_SCOPE);
export const setShowAttributionModal = payloadParamActionCreator(ADVANCED_LEGAL_SET_SHOW_ATTRIBUTION_MODAL);
export const cancelAttributionModal = payloadParamActionCreator(ADVANCED_LEGAL_CANCEL_ATTRIBUTION_MODAL);

export const ADVANCED_LEGAL_SAVE_ATTRIBUTION_REQUESTED = 'ADVANCED_LEGAL_SAVE_ATTRIBUTION_REQUESTED';
export const ADVANCED_LEGAL_SAVE_ATTRIBUTION_FULFILLED = 'ADVANCED_LEGAL_SAVE_ATTRIBUTION_FULFILLED';
export const ADVANCED_LEGAL_SAVE_ATTRIBUTION_FAILED = 'ADVANCED_LEGAL_SAVE_ATTRIBUTION_FAILED';
export const ADVANCED_LEGAL_SAVE_ATTRIBUTION_SUBMIT_MASK_DONE = 'ADVANCED_LEGAL_SAVE_ATTRIBUTION_SUBMIT_MASK_DONE';

const saveAttributionRequested = payloadParamActionCreator(ADVANCED_LEGAL_SAVE_ATTRIBUTION_REQUESTED);
const saveAttributionFulfilled = payloadParamActionCreator(ADVANCED_LEGAL_SAVE_ATTRIBUTION_FULFILLED);
const saveAttributionFailed = payloadParamActionCreator(ADVANCED_LEGAL_SAVE_ATTRIBUTION_FAILED);

export function saveAttribution({ obligationName, existingObligation, isAttributionDirty, isObligationDirty }) {
  return (dispatch, getState) => {
    if (isAttributionDirty) {
      return saveAndRefreshAttribution(dispatch, getState, obligationName, isObligationDirty);
    } else if (isObligationDirty) {
      return saveObligation(existingObligation.name)(dispatch, getState);
    } else {
      return;
    }
  };
}

function saveAndRefreshAttribution(dispatch, getState, obligationName, isObligationDirty) {
  const advancedLegalState = getState().advancedLegal;
  const attributionState = find(
    propEq('obligationName', obligationName),
    advancedLegalState.component.component.licenseLegalData.attributions
  );
  const ownerId = attributionState.ownerId;
  const scopeVisited = advancedLegalState.availableScopes.values[0];
  const scope = find(propEq('id', ownerId), advancedLegalState.availableScopes.values);
  const ownerType = scope.type;
  const ownerPublicId = scope.publicId;
  const componentIdentifier = advancedLegalState.component.component.componentIdentifier;

  dispatch(saveAttributionRequested({ name: obligationName }));

  if (attributionState.id !== null && attributionState.content === '') {
    return axios
      .delete(getDeleteComponentObligationAttributionUrl(attributionState.id))
      .then(() =>
        onAttributionSaveSuccess(
          dispatch,
          scopeVisited.type,
          scopeVisited.publicId,
          componentIdentifier,
          obligationName,
          isObligationDirty,
          getState
        )
      )
      .catch((error) => {
        dispatch(
          saveAttributionFailed({
            name: obligationName,
            value: Messages.getHttpErrorMessage(error),
          })
        );
      });
  } else {
    const attributionPayload = getAttributionPayload(advancedLegalState, obligationName, attributionState);
    return axios
      .post(getSaveComponentObligationAttributionUrl(ownerType, ownerPublicId), attributionPayload)
      .then(() =>
        onAttributionSaveSuccess(
          dispatch,
          scopeVisited.type,
          scopeVisited.publicId,
          componentIdentifier,
          obligationName,
          isObligationDirty,
          getState
        )
      )
      .catch((error) => {
        dispatch(
          saveAttributionFailed({
            name: obligationName,
            value: Messages.getHttpErrorMessage(error),
          })
        );
      });
  }
}

function getAttributionPayload(advancedLegalState, obligationName, attributionState) {
  const payload = {
    id: attributionState.id,
    componentIdentifier: advancedLegalState.component.component.componentIdentifier,
    obligationName: obligationName,
    content: attributionState.content,
  };
  if (
    payload.id !== null &&
    isScopeOverride(
      attributionState.originalOwnerId,
      attributionState.ownerId,
      advancedLegalState.availableScopes.values
    )
  ) {
    payload.id = null;
  }
  return payload;
}

const onAttributionSaveSuccess = (
  dispatch,
  ownerType,
  ownerPublicId,
  componentIdentifier,
  name,
  isObligationDirty,
  getState
) =>
  axios
    .get(getComponentObligationAttributionUrl(ownerType, ownerPublicId, componentIdentifier, name))
    .then((payload) => {
      const value =
        payload.data.length > 0
          ? pick(['id', 'content', 'ownerId', 'lastUpdatedByUsername', 'lastUpdatedAt'], payload.data[0])
          : { id: null, content: '', ownerId: 'ROOT_ORGANIZATION_ID' };
      dispatch(saveAttributionFulfilled({ name, value }));

      isObligationDirty
        ? saveObligation(name)(dispatch, getState)
        : startSaveAttributionSubmitMaskDoneTimer(dispatch, { name });
    })
    .catch((error) => {
      dispatch(
        saveAttributionFailed({
          name,
          value: Messages.getHttpErrorMessage(error),
        })
      );
    });

function startSaveAttributionSubmitMaskDoneTimer(dispatch, payload) {
  setTimeout(() => {
    dispatch({
      type: ADVANCED_LEGAL_SAVE_ATTRIBUTION_SUBMIT_MASK_DONE,
      payload,
    });
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

export const ADVANCED_LEGAL_SET_OBLIGATION_STATUS = 'ADVANCED_LEGAL_SET_OBLIGATION_STATUS';
export const ADVANCED_LEGAL_SET_OBLIGATION_COMMENT = 'ADVANCED_LEGAL_SET_OBLIGATION_COMMENT';
export const ADVANCED_LEGAL_SET_OBLIGATION_SCOPE = 'ADVANCED_LEGAL_SET_OBLIGATION_SCOPE';
export const ADVANCED_LEGAL_SET_SHOW_OBLIGATION_MODAL = 'ADVANCED_LEGAL_SET_SHOW_OBLIGATION_MODAL';
export const ADVANCED_LEGAL_CANCEL_OBLIGATION_MODAL = 'ADVANCED_LEGAL_CANCEL_OBLIGATION_MODAL';
export const ADVANCED_LEGAL_SET_SHOW_ALL_OBLIGATIONS_MODAL = 'ADVANCED_LEGAL_SET_SHOW_ALL_OBLIGATIONS_MODAL';
export const ADVANCED_LEGAL_CANCEL_ALL_OBLIGATIONS_MODAL = 'ADVANCED_LEGAL_CANCEL_ALL_OBLIGATIONS_MODAL ';

export const setObligationStatus = payloadParamActionCreator(ADVANCED_LEGAL_SET_OBLIGATION_STATUS);
export const setObligationComment = payloadParamActionCreator(ADVANCED_LEGAL_SET_OBLIGATION_COMMENT);
export const setObligationScope = payloadParamActionCreator(ADVANCED_LEGAL_SET_OBLIGATION_SCOPE);
export const setShowObligationModal = payloadParamActionCreator(ADVANCED_LEGAL_SET_SHOW_OBLIGATION_MODAL);
export const cancelObligationModal = payloadParamActionCreator(ADVANCED_LEGAL_CANCEL_OBLIGATION_MODAL);
export const setShowAllObligationsModal = payloadParamActionCreator(ADVANCED_LEGAL_SET_SHOW_ALL_OBLIGATIONS_MODAL);
export const cancelAllObligationsModal = payloadParamActionCreator(ADVANCED_LEGAL_CANCEL_ALL_OBLIGATIONS_MODAL);

export const ADVANCED_LEGAL_SAVE_OBLIGATION_REQUESTED = 'ADVANCED_LEGAL_SAVE_OBLIGATION_REQUESTED';
export const ADVANCED_LEGAL_SAVE_OBLIGATION_SUCCEEDED = 'ADVANCED_LEGAL_SAVE_OBLIGATION_SUCCEEDED';
export const ADVANCED_LEGAL_SAVE_OBLIGATION_FAILED = 'ADVANCED_LEGAL_SAVE_OBLIGATION_FAILED';
export const ADVANCED_LEGAL_SAVE_OBLIGATION_SUBMIT_MASK_DONE = 'ADVANCED_LEGAL_SAVE_OBLIGATION_SUBMIT_MASK_DONE';

export const ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_REQUESTED = 'ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_REQUESTED';
export const ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_SUCCEEDED = 'ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_SUCCEEDED';
export const ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_FAILED = 'ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_FAILED';
export const ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_SUBMIT_MASK_DONE =
  'ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_SUBMIT_MASK_DONE';

const saveObligationRequested = payloadParamActionCreator(ADVANCED_LEGAL_SAVE_OBLIGATION_REQUESTED);
const saveObligationSucceeded = payloadParamActionCreator(ADVANCED_LEGAL_SAVE_OBLIGATION_SUCCEEDED);
const saveObligationFailed = payloadParamActionCreator(ADVANCED_LEGAL_SAVE_OBLIGATION_FAILED);

const saveAllObligationsFailed = payloadParamActionCreator(ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_FAILED);

export function saveObligation(name, throwError = false) {
  return (dispatch, getState) => {
    dispatch(saveObligationRequested({ name }));

    const advancedLegalState = getState().advancedLegal;
    const obligationState = find(
      propEq('name', name),
      advancedLegalState.component.component.licenseLegalData.obligations
    );
    const ownerId = obligationState.ownerId;
    const scopeVisited = advancedLegalState.availableScopes.values[0];
    const scope = find(propEq('id', ownerId), advancedLegalState.availableScopes.values);
    const ownerType = scope.type;
    const ownerPublicId = scope.publicId;
    const componentIdentifier = advancedLegalState.component.component.componentIdentifier;

    if (
      obligationState.id !== null &&
      obligationState.comment === '' &&
      obligationState.status === OBLIGATION_STATUS_OPEN
    ) {
      return axios
        .delete(getDeleteComponentObligationsUrl([obligationState.id]))
        .then(() =>
          onObligationSaveSuccess(dispatch, scopeVisited.type, scopeVisited.publicId, componentIdentifier, name)
        )
        .catch((error) => {
          dispatch(
            saveObligationFailed({
              name,
              value: Messages.getHttpErrorMessage(error),
            })
          );
          if (throwError) {
            throw new Error(Messages.getHttpErrorMessage(error));
          }
        });
    } else {
      const obligationPayload = getObligationPayload(advancedLegalState, obligationState);
      return axios
        .post(getSaveComponentObligationUrl(ownerType, ownerPublicId), obligationPayload)
        .then(() =>
          onObligationSaveSuccess(dispatch, scopeVisited.type, scopeVisited.publicId, componentIdentifier, name)
        )
        .catch((error) => {
          dispatch(
            saveObligationFailed({
              name,
              value: Messages.getHttpErrorMessage(error),
            })
          );
          if (throwError) {
            throw new Error(Messages.getHttpErrorMessage(error));
          }
        });
    }
  };
}

export function saveAllObligations(status, comment, ownerId) {
  return (dispatch, getState) => {
    dispatch({ type: ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_REQUESTED });

    const advancedLegalState = getState().advancedLegal;
    const payload = getAllObligationsPayload(advancedLegalState, status, comment, ownerId);
    const scopeVisited = advancedLegalState.availableScopes.values[0];
    const scope = find(propEq('id', ownerId), advancedLegalState.availableScopes.values);
    const ownerType = scope.type;
    const ownerPublicId = scope.publicId;
    const componentIdentifier = advancedLegalState.component.component.componentIdentifier;
    if (comment === '' && status === OBLIGATION_STATUS_OPEN) {
      const obligationIdsToDelete = advancedLegalState.component.component.licenseLegalData.obligations
        .filter((o) => o.id != null && o.ownerId === ownerId)
        .map((o) => o.id);
      if (obligationIdsToDelete.length === 0) {
        dispatch({ type: ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_SUCCEEDED });
        startSaveAllObligationsSubmitMaskDoneTimer(dispatch);
        return Promise.resolve();
      }
      return axios
        .delete(getDeleteComponentObligationsUrl(obligationIdsToDelete))
        .then(fulfillComponentObligations(advancedLegalState, dispatch, scopeVisited, componentIdentifier))
        .catch((error) => {
          dispatch(
            saveAllObligationsFailed({
              value: Messages.getHttpErrorMessage(error),
            })
          );
        });
    } else {
      return axios
        .post(getSaveComponentObligationsUrl(ownerType, ownerPublicId), payload)
        .then(fulfillComponentObligations(advancedLegalState, dispatch, scopeVisited, componentIdentifier))
        .catch((error) => {
          dispatch(saveAllObligationsFailed({ value: error }));
        });
    }
  };
}

function fulfillComponentObligations(advancedLegalState, dispatch, scopeVisited, componentIdentifier) {
  return () => {
    const promises = [];
    advancedLegalState.component.component.licenseLegalData.obligations.forEach((obligation) => {
      promises.push(
        onObligationSaveSuccess(
          dispatch,
          scopeVisited.type,
          scopeVisited.publicId,
          componentIdentifier,
          obligation.name,
          true
        )
      );
    });
    return Promise.all(promises)
      .then(() => {
        dispatch({ type: ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_SUCCEEDED });
        startSaveAllObligationsSubmitMaskDoneTimer(dispatch);
      })
      .catch((error) => {
        dispatch(saveAllObligationsFailed({ value: error }));
      });
  };
}

function getAllObligationsPayload(advancedLegalState, status, comment, ownerId) {
  return advancedLegalState.component.component.licenseLegalData.obligations.map((obligation) => {
    return {
      id: requiresNewObligation(
        obligation.id,
        obligation.originalOwnerId,
        ownerId,
        advancedLegalState.availableScopes.values
      )
        ? null
        : obligation.id,
      componentIdentifier: advancedLegalState.component.component.componentIdentifier,
      name: obligation.name,
      comment,
      status,
    };
  });
}

function getObligationPayload(advancedLegalState, obligationState) {
  const payload = {
    id: obligationState.id,
    componentIdentifier: advancedLegalState.component.component.componentIdentifier,
    name: obligationState.name,
    comment: obligationState.comment,
    status: obligationState.status,
  };

  if (
    requiresNewObligation(
      payload.id,
      obligationState.originalOwnerId,
      obligationState.ownerId,
      advancedLegalState.availableScopes.values
    )
  ) {
    payload.id = null;
  }
  return payload;
}

const requiresNewObligation = (id, originalOwnerId, ownerId, availableScopeValues) =>
  id !== null && isScopeOverride(originalOwnerId, ownerId, availableScopeValues);

const onObligationSaveSuccess = (dispatch, ownerType, ownerPublicId, componentIdentifier, name, rethrowError = false) =>
  axios
    .get(getComponentObligationUrl(ownerType, ownerPublicId, componentIdentifier, name))
    .then((payload) => {
      const value = payload.data
        ? pick(['id', 'comment', 'ownerId', 'status', 'lastUpdatedByUsername', 'lastUpdatedAt'], payload.data)
        : {
            id: null,
            comment: '',
            ownerId: 'ROOT_ORGANIZATION_ID',
            status: OBLIGATION_STATUS_OPEN,
          };
      dispatch(saveObligationSucceeded({ name, value }));
      startSaveObligationSubmitMaskDoneTimer(dispatch, { name });
    })
    .catch((error) => {
      dispatch(
        saveObligationFailed({
          name,
          value: Messages.getHttpErrorMessage(error),
        })
      );
      if (rethrowError) {
        throw error;
      }
    });

function startSaveObligationSubmitMaskDoneTimer(dispatch, payload) {
  setTimeout(() => {
    dispatch({
      type: ADVANCED_LEGAL_SAVE_OBLIGATION_SUBMIT_MASK_DONE,
      payload,
    });
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

function startSaveAllObligationsSubmitMaskDoneTimer(dispatch) {
  setTimeout(() => {
    dispatch({ type: ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_SUBMIT_MASK_DONE });
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}
