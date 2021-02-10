/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import {
  getApplicationsUrl,
  getDeleteComponentObligationAttributionUrl,
  getLicenseLegalApplicationReportUrl,
  getLicenseLegalComponentUrl,
  getOwnerHierarchyUrl,
  getSaveComponentObligationAttributionUrl,
  getComponentObligationAttributionUrl
} from '../util/CLMLocation';
import { noPayloadActionCreator, payloadParamActionCreator } from '../util/reduxUtil';
import { capitalize } from '../util/jsUtil';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { findIndex, pick, propEq } from 'ramda';
import { Messages } from '../util/CommonServices';

export const ADVANCED_LEGAL_LOAD_APPLICATIONS_REQUESTED = 'ADVANCED_LEGAL_LOAD_APPLICATIONS_REQUESTED';
export const ADVANCED_LEGAL_LOAD_APPLICATIONS_FULFILLED = 'ADVANCED_LEGAL_LOAD_APPLICATIONS_FULFILLED';
export const ADVANCED_LEGAL_LOAD_APPLICATIONS_FAILED = 'ADVANCED_LEGAL_LOAD_APPLICATIONS_FAILED';

export const ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_REQUESTED = 'ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_REQUESTED';
export const ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FULFILLED = 'ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FULFILLED';
export const ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FAILED = 'ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FAILED';

export const ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED = 'ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED';
export const ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED = 'ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED';
export const ADVANCED_LEGAL_LOAD_COMPONENT_FAILED = 'ADVANCED_LEGAL_LOAD_COMPONENT_FAILED';

export const ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_REQUESTED = 'ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_REQUESTED';
export const ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FULFILLED = 'ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FULFILLED';
export const ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FAILED = 'ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FAILED';

const loadApplicationsRequested = noPayloadActionCreator(ADVANCED_LEGAL_LOAD_APPLICATIONS_REQUESTED);
const loadApplicationsFulfilled = payloadParamActionCreator(ADVANCED_LEGAL_LOAD_APPLICATIONS_FULFILLED);
const loadApplicationsFailed = payloadParamActionCreator(ADVANCED_LEGAL_LOAD_APPLICATIONS_FAILED);

const loadApplicationReportRequested = noPayloadActionCreator(ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_REQUESTED);
const loadApplicationReportFulfilled = payloadParamActionCreator(ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FULFILLED);
const loadApplicationReportFailed = payloadParamActionCreator(ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FAILED);

const loadComponentRequested = noPayloadActionCreator(ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED);
const loadComponentFulfilled = payloadParamActionCreator(ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED);
const loadComponentFailed = payloadParamActionCreator(ADVANCED_LEGAL_LOAD_COMPONENT_FAILED);

const loadAvailableScopesRequested = noPayloadActionCreator(ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_REQUESTED);
const loadAvailableScopesFulfilled = payloadParamActionCreator(ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FULFILLED);
const loadAvailableScopesFailed = payloadParamActionCreator(ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FAILED);

export const ADVANCED_LEGAL_SET_ATTRIBUTION_TEXT = 'ADVANCED_LEGAL_SET_ATTRIBUTION_TEXT';
export const ADVANCED_LEGAL_SET_OBLIGATION_FULFILLED = 'ADVANCED_LEGAL_SET_OBLIGATION_FULFILLED';
export const ADVANCED_LEGAL_SET_ATTRIBUTION_SCOPE = 'ADVANCED_LEGAL_SET_ATTRIBUTION_SCOPE';
export const ADVANCED_LEGAL_SET_SHOW_ATTRIBUTION_MODAL = 'ADVANCED_LEGAL_SET_SHOW_ATTRIBUTION_MODAL';

export const setAttributionText = payloadParamActionCreator(ADVANCED_LEGAL_SET_ATTRIBUTION_TEXT);
export const setObligationFulfilled = payloadParamActionCreator(ADVANCED_LEGAL_SET_OBLIGATION_FULFILLED);
export const setAttributionScope = payloadParamActionCreator(ADVANCED_LEGAL_SET_ATTRIBUTION_SCOPE);
export const setShowAttributionModal = payloadParamActionCreator(ADVANCED_LEGAL_SET_SHOW_ATTRIBUTION_MODAL);

export const ADVANCED_LEGAL_SAVE_ATTRIBUTION_REQUESTED = 'ADVANCED_LEGAL_SAVE_ATTRIBUTION_REQUESTED';
export const ADVANCED_LEGAL_SAVE_ATTRIBUTION_FULFILLED = 'ADVANCED_LEGAL_SAVE_ATTRIBUTION_FULFILLED';
export const ADVANCED_LEGAL_SAVE_ATTRIBUTION_FAILED = 'ADVANCED_LEGAL_SAVE_ATTRIBUTION_FAILED';
export const ADVANCED_LEGAL_SAVE_ATTRIBUTION_SUBMIT_MASK_DONE
    = 'ADVANCED_LEGAL_SAVE_ATTRIBUTION_SUBMIT_MASK_DONE';

const saveAttributionRequested = payloadParamActionCreator(ADVANCED_LEGAL_SAVE_ATTRIBUTION_REQUESTED);
const saveAttributionFulfilled = payloadParamActionCreator(ADVANCED_LEGAL_SAVE_ATTRIBUTION_FULFILLED);
const saveAttributionFailed = payloadParamActionCreator(ADVANCED_LEGAL_SAVE_ATTRIBUTION_FAILED);

export function saveAttribution(name) {
  return (dispatch, getState) => {
    dispatch(saveAttributionRequested({ name: name }));

    const advancedLegalState = getState().advancedLegal;
    const obligationState = advancedLegalState.component.obligations.filter(obligation => obligation.name === name)[0];
    const attributionState = obligationState.attributions[0];
    const ownerId = attributionState.ownerId;
    const scope = advancedLegalState.availableScopes.values.filter(s => s.id === ownerId)[0];
    const ownerType = scope.type;
    const ownerPublicId = scope.publicId;
    const componentIdentifier = advancedLegalState.component.component.componentIdentifier;

    if (attributionState.id !== null && attributionState.content === '') {
      return axios.delete(getDeleteComponentObligationAttributionUrl(attributionState.id))
          .then(() => {
            axios.get(getComponentObligationAttributionUrl(ownerType, ownerPublicId, componentIdentifier, name))
                .then(payload => {
                  const value = payload.data.length > 0 ? pick(['id', 'content', 'ownerId'], payload.data[0]) :
                    { id: null, content: '', ownerId: 'ROOT_ORGANIZATION_ID' };
                  dispatch(saveAttributionFulfilled({ name: name, value: value }));
                  startSaveAttributionSubmitMaskDoneTimer(dispatch, { name: name });
                })
                .catch(error => {
                  dispatch(saveAttributionFailed({ name: name, value: Messages.getHttpErrorMessage(error) }));
                });
          })
          .catch(error => {
            dispatch(saveAttributionFailed({ name: name, value: Messages.getHttpErrorMessage(error) }));
          });
    }
    else {
      const payload = {
        'id': attributionState.id,
        'componentIdentifier': componentIdentifier,
        'obligationName': name,
        'content': attributionState.content
      };

      if (payload.id !== null) {
        const originalOwnerId = attributionState.originalOwnerId;
        const availableScopeValues = advancedLegalState.availableScopes.values;
        const originalOwnerLevel = findIndex(propEq('id', originalOwnerId), availableScopeValues);
        const newOwnerLevel = findIndex(propEq('id', ownerId), availableScopeValues);
        const isOverride = originalOwnerLevel > newOwnerLevel;

        if (isOverride) {
          payload.id = null;
        }
      }

      return axios.post(getSaveComponentObligationAttributionUrl(ownerType, ownerPublicId), payload)
          .then(payload => {
            dispatch(saveAttributionFulfilled({ name: name, value: payload.data }));
            startSaveAttributionSubmitMaskDoneTimer(dispatch, { name: name });
          })
          .catch(error => {
            dispatch(saveAttributionFailed({ name: name, value: Messages.getHttpErrorMessage(error) }));
          });
    }
  };
}

function startSaveAttributionSubmitMaskDoneTimer(dispatch, payload) {
  setTimeout(() => {
    dispatch({ type: ADVANCED_LEGAL_SAVE_ATTRIBUTION_SUBMIT_MASK_DONE, payload: payload });
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

export function loadApplications() {
  return (dispatch) => {
    dispatch(loadApplicationsRequested());

    return axios.get(getApplicationsUrl())
        .then(({ data }) => {
          dispatch(loadApplicationsFulfilled(data));
        })
        .catch(error => {
          dispatch(loadApplicationsFailed(error));
        });
  };
}

export function loadApplicationReport(publicId) {
  return (dispatch) => {
    dispatch(loadApplicationReportRequested());

    return axios.get(getLicenseLegalApplicationReportUrl(publicId))
        .then(({ data }) => {
          dispatch(loadApplicationReportFulfilled(data));
        })
        .catch(error => {
          dispatch(loadApplicationReportFailed(error));
        });
  };
}

export function loadComponent(orgOrApp, ownerId, hash) {
  return (dispatch) => {
    dispatch(loadComponentRequested());

    return axios.get(getLicenseLegalComponentUrl(orgOrApp, ownerId, hash))
        .then(({ data }) => {
          dispatch(loadComponentFulfilled(data));
        })
        .catch(error => {
          dispatch(loadComponentFailed(error));
        });
  };
}

export function loadAvailableScopes(ownerType, ownerId) {
  return (dispatch) => {
    dispatch(loadAvailableScopesRequested());

    return axios.get(getOwnerHierarchyUrl(ownerType, ownerId))
        .then(({ data }) => {
          let payload = {
            values: processOwnerHierarchy(data)
          };
          dispatch(loadAvailableScopesFulfilled(payload));
        })
        .catch(error => {
          dispatch(loadAvailableScopesFailed(error));
        });
  };
}

/**
 * Flattens the Org/Apps hierarchy
 */
function processOwnerHierarchy(context) {
  // note that since the context data only includes the ancestors of the owner, `children` should
  // never have more than one element
  const processedChildren = context.children ? processOwnerHierarchy(context.children[0]) : [],
      { type, id, publicId, name } = context,
      label = capitalize(type);

  return processedChildren.concat({ type, id, publicId, name, label });
}
