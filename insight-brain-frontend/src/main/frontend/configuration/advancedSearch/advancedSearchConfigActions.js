/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import { noPayloadActionCreator, payloadParamActionCreator } from '../../util/reduxUtil';
import { getAdvancedSearchIndexUrl, getAdvancedSearchConfigUrl } from '../../util/CLMLocation';

// Actions related to initial loading of the page
export const ADVANCED_SEARCH_CONFIG_LOAD_REQUESTED = 'ADVANCED_SEARCH_CONFIG_LOAD_REQUESTED';
export const ADVANCED_SEARCH_CONFIG_LOAD_FULFILLED = 'ADVANCED_SEARCH_CONFIG_LOAD_FULFILLED';
export const ADVANCED_SEARCH_CONFIG_LOAD_FAILED = 'ADVANCED_SEARCH_CONFIG_LOAD_FAILED';

const loadRequested = noPayloadActionCreator(ADVANCED_SEARCH_CONFIG_LOAD_REQUESTED);
const loadFulfilled = payloadParamActionCreator(ADVANCED_SEARCH_CONFIG_LOAD_FULFILLED);
const loadFailed = payloadParamActionCreator(ADVANCED_SEARCH_CONFIG_LOAD_FAILED);

export const ADVANCED_SEARCH_UPDATE_CURRENTLY_POLLING = 'ADVANCED_SEARCH_UPDATE_CURRENTLY_POLLING';
const updateCurrentlyPolling = payloadParamActionCreator(ADVANCED_SEARCH_UPDATE_CURRENTLY_POLLING);
const ADVANCED_SEARCH_POLLING_INTERVAL_MS = 2000;

function waitAndPollIfNeeded(dispatch, getState, currentlyPolling) {
  dispatch(updateCurrentlyPolling(currentlyPolling));
  if (currentlyPolling) {
    setTimeout(pollState(dispatch, getState), ADVANCED_SEARCH_POLLING_INTERVAL_MS);
  }
}

export function load() {
  return function (dispatch, getState) {
    dispatch(loadRequested());
    return axios
      .get(getAdvancedSearchConfigUrl())
      .then(({ data }) => {
        dispatch(loadFulfilled(data));
        waitAndPollIfNeeded(
          dispatch,
          getState,
          data.isFullIndexTriggered && !getState().advancedSearchConfig.currentlyPolling
        );
      })
      .catch((error) => {
        dispatch(loadFailed(error));
      });
  };
}

// Action related to saving the form
export const ADVANCED_SEARCH_CONFIG_SAVE_REQUESTED = 'ADVANCED_SEARCH_CONFIG_SAVE_REQUESTED';
export const ADVANCED_SEARCH_CONFIG_SAVE_FULFILLED = 'ADVANCED_SEARCH_CONFIG_SAVE_FULFILLED';
export const ADVANCED_SEARCH_CONFIG_SAVE_FAILED = 'ADVANCED_SEARCH_CONFIG_SAVE_FAILED';
export const ADVANCED_SEARCH_CONFIG_SAVE_SUBMIT_MASK_TIMER_DONE = 'ADVANCED_SEARCH_CONFIG_SAVE_SUBMIT_MASK_TIMER_DONE';

const saveRequested = noPayloadActionCreator(ADVANCED_SEARCH_CONFIG_SAVE_REQUESTED);
const saveFulfilled = noPayloadActionCreator(ADVANCED_SEARCH_CONFIG_SAVE_FULFILLED);
const saveFailed = payloadParamActionCreator(ADVANCED_SEARCH_CONFIG_SAVE_FAILED);

function startSubmitMaskSuccessTimer(dispatch) {
  setTimeout(() => {
    dispatch({ type: ADVANCED_SEARCH_CONFIG_SAVE_SUBMIT_MASK_TIMER_DONE });
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

export function save() {
  return function (dispatch, getState) {
    dispatch(saveRequested());
    return axios
      .put(getAdvancedSearchConfigUrl(), getState().advancedSearchConfig.formState)
      .then(() => {
        dispatch(saveFulfilled());
        startSubmitMaskSuccessTimer(dispatch);
      })
      .catch((error) => {
        dispatch(saveFailed(error));
      });
  };
}

// Cancel Form
export const ADVANCED_SEARCH_RESET_FORM = 'RESET_FORM';
export const resetForm = noPayloadActionCreator(ADVANCED_SEARCH_RESET_FORM);

// Opt-In Checkbox Related Actions
export const ADVANCED_SEARCH_SET_IS_ENABLED = 'ADVANCED_SEARCH_SET_IS_ENABLED';
export const setIsEnabled = payloadParamActionCreator(ADVANCED_SEARCH_SET_IS_ENABLED);

// Actions related to re-indexing
export const ADVANCED_SEARCH_TRIGGER_RE_INDEX = 'ADVANCED_SEARCH_TRIGGER_RE_INDEX';
const triggerReIndex = noPayloadActionCreator(ADVANCED_SEARCH_TRIGGER_RE_INDEX);

export function reIndex() {
  return function (dispatch, getState) {
    return axios
      .post(getAdvancedSearchIndexUrl(), {})
      .then(() => {
        dispatch(triggerReIndex());
        waitAndPollIfNeeded(dispatch, getState, true);
      })
      .catch((error) => {
        dispatch(advancedSearchReindexFailed(error));
      });
  };
}

export const ADVANCED_SEARCH_RE_INDEX_FAILED = 'ADVANCED_SEARCH_RE_INDEX_FAILED';
const advancedSearchReindexFailed = payloadParamActionCreator(ADVANCED_SEARCH_RE_INDEX_FAILED);

export const ADVANCED_SEARCH_POLL_STATE_SUCCESS = 'ADVANCED_SEARCH_POLL_STATE_SUCCESS';
const pollStateSuccess = payloadParamActionCreator(ADVANCED_SEARCH_POLL_STATE_SUCCESS);
export const ADVANCED_SEARCH_POLL_STATE_FAILED = 'ADVANCED_SEARCH_POLL_STATE_FAILED';
const pollStateFailed = payloadParamActionCreator(ADVANCED_SEARCH_POLL_STATE_FAILED);

// exported for testing
export function pollState(dispatch, getState) {
  return function () {
    if (getState().router.currentState.name !== 'advancedSearchConfig') {
      dispatch(updateCurrentlyPolling(false));
      return Promise.resolve();
    }
    return axios
      .get(getAdvancedSearchConfigUrl())
      .then(({ data }) => {
        dispatch(pollStateSuccess(data));
        waitAndPollIfNeeded(dispatch, getState, data.isFullIndexTriggered);
      })
      .catch((error) => {
        dispatch(pollStateFailed(error));
      });
  };
}
