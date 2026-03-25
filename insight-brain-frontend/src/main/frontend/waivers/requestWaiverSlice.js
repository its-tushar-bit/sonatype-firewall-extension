/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk } from '@reduxjs/toolkit';
import createSlice from 'MainRoot/reduxConfig/createSlice';

import { nxDateInputStateHelpers, nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import { always, isEmpty } from 'ramda';
import {
  getCreatePolicyWaiverRequestUrl,
  getReviewPolicyWaiverRequestUrl,
  getViewOrUpdatePolicyWaiverRequestUrl,
} from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/util/CommonServices';
import { selectViolationId } from 'MainRoot/reduxUiRouter/routerSelectors';
import {
  selectSelectedWaiverScope,
  selectComponentMatcherStrategy,
  selectWaiverReasonId,
  selectComments,
  selectNoteToReviewer,
  selectRejectionReason,
} from './requestWaiverSelectors';
import { selectWaiverRequestDetails } from 'MainRoot/waivers/requestWaiverDetails/requestWaiverDetailsSelectors';
import { startSaveMaskSuccessTimer } from 'MainRoot/util/reduxUtil';
import { getISODateFromDateInput, propSet } from 'MainRoot/util/jsUtil';
import { returnToAddOrRequestWaiverOriginPage, returnToReviewWaiverRequestOriginPage } from './waiverActions';
import { getExpiryTime, isCustomExpiryTimeValid, formatCustomDate } from 'MainRoot/util/waiverUtils';

const { initialState: rscInitialState, userInput } = nxTextInputStateHelpers;

const WAIVER_REQUEST_SCOPE_ERROR_MESSAGE = 'Cannot load Waiver Scope for the Waiver Request.';

const REDUCER_NAME = `requestWaiver`;

export const initialState = {
  loading: true,
  loadError: null,
  submitError: null,
  isDirty: false,
  submitMaskState: null,
  waiverReasonId: null,
  selectedWaiverScope: null,
  componentMatcherStrategy: null,
  expiryTime: null,
  customExpiryTime: nxDateInputStateHelpers.initialState(''),
  comments: rscInitialState(''),
  noteToReviewer: rscInitialState(''),
};

export const initialStateForReview = {
  submitError: null,
  rejectionReason: rscInitialState(''),
};

const initializeStateFromDetails = (state, { payload }) => {
  const waiverRequestDetails = payload;
  state.selectedWaiverScope = {
    id: waiverRequestDetails.scopeOwnerId,
    type: waiverRequestDetails.scopeOwnerType,
    name: waiverRequestDetails.scopeOwnerName,
  };
  state.componentMatcherStrategy = waiverRequestDetails.matcherStrategy || null;
  state.expiryTime = state.expiryTime = waiverRequestDetails.expiryTime == null ? null : 'custom'; //since the expiry time is saved as a custom date
  const customDate = nxDateInputStateHelpers.userInput(
    customDateValidator,
    formatCustomDate(waiverRequestDetails.expiryTime)
  );
  state.customExpiryTime = customDate || null;
  state.waiverReasonId = waiverRequestDetails.policyWaiverReasonId || null;
  state.comments = userInput(null, waiverRequestDetails.comment || '');
  state.noteToReviewer = userInput(null, waiverRequestDetails.noteToReviewer || '');
};

const loadSelectedWaiverScope = (state, { payload }) => {
  state.selectedWaiverScope = payload || null;
  state.loading = false;
  state.loadError = payload ? null : WAIVER_REQUEST_SCOPE_ERROR_MESSAGE;
};

const setWaiverReasonId = (state, { payload }) => {
  state.waiverReasonId = payload ? payload : null;
};

const setSelectedWaiverScope = (state, { payload }) => {
  state.selectedWaiverScope = payload ? payload : null;
};

const setComponentMatcherStrategy = (state, { payload }) => {
  state.componentMatcherStrategy = payload ? payload : null;
};

const setExpiryTime = (state, { payload }) => {
  state.expiryTime = payload ? payload : null;
  state.customExpiryTime = nxDateInputStateHelpers.initialState('');
};

const customDateValidator = (value) => (isCustomExpiryTimeValid(value) ? null : 'Date must be in the future');

const setCustomExpiryTime = (state, { payload }) => {
  state.customExpiryTime = nxDateInputStateHelpers.userInput(customDateValidator, payload);
};

const setRequestWaiverComments = (state, { payload }) => {
  state.comments = userInput(null, payload);
  return computeIsDirty(state, payload);
};

const setNoteToReviewer = (state, { payload }) => {
  state.noteToReviewer = userInput(null, payload);
  return computeIsDirty(state, payload);
};

const setRejectionReason = (state, { payload }) => {
  state.rejectionReason = userInput(null, payload);
  return computeIsDirty(state, payload);
};

const computeIsDirty = (state, payload) => {
  state.isDirty = !isEmpty(payload);
};

const clearSubmitError = (state) => {
  state.submitError = null;
};

const createRequestWaiver = createAsyncThunk(
  `${REDUCER_NAME}/createRequestWaiver`,
  ({ expiration, expireWhenRemediationAvailableSelected }, { rejectWithValue, getState, dispatch }) => {
    const state = getState();
    const selectedWaiverScope = selectSelectedWaiverScope(state);
    const policyViolationId = selectViolationId(state);
    const comment = selectComments(state).trimmedValue;
    const noteToReviewer = selectNoteToReviewer(state).trimmedValue;
    const componentMatcherStrategy = selectComponentMatcherStrategy(state);
    const waiverReasonId = selectWaiverReasonId(state);
    return axios
      .post(getCreatePolicyWaiverRequestUrl(selectedWaiverScope.type, selectedWaiverScope.id, policyViolationId), {
        comment,
        noteToReviewer,
        matcherStrategy: componentMatcherStrategy,
        expiryTime: typeof expiration === 'string' ? getISODateFromDateInput(expiration) : getExpiryTime(expiration),
        waiverReasonId,
        expireWhenRemediationAvailable: expireWhenRemediationAvailableSelected,
      })
      .then(({ data }) => {
        startSaveMaskSuccessTimer(dispatch, actions.saveMaskTimerDone).then(() => {
          dispatch(returnToAddOrRequestWaiverOriginPage());
        });
        return data;
      })
      .catch(rejectWithValue);
  }
);

const createRequestWaiverRequested = (state) => {
  state.submitMaskState = false;
  state.submitError = null;
};

const createRequestWaiverFulfilled = (state) => {
  state.submitMaskState = true;
  state.comments = userInput(null, '');
  state.noteToReviewer = userInput(null, '');
  state.isDirty = false;
  state.submitError = null;
};

const createRequestWaiverFailed = (state, { payload }) => {
  state.submitMaskState = null;
  state.submitError = Messages.getHttpErrorMessage(payload);
};

const reviewRequestWaiver = createAsyncThunk(
  `${REDUCER_NAME}/reviewRequestWaiver`,
  ({ status, expiration, expireWhenRemediationAvailableSelected }, { rejectWithValue, getState, dispatch }) => {
    const state = getState();
    const selectedWaiverScope = selectSelectedWaiverScope(state);
    const policyWaiverRequestId = selectWaiverRequestDetails(state).policyWaiverRequestId;
    const comment = selectComments(state).trimmedValue;
    const componentMatcherStrategy = selectComponentMatcherStrategy(state);
    const waiverReasonId = selectWaiverReasonId(state);
    const rejectionReason = selectRejectionReason(state).trimmedValue;
    return axios
      .post(getReviewPolicyWaiverRequestUrl(selectedWaiverScope.type, selectedWaiverScope.id, policyWaiverRequestId), {
        status,
        comment,
        matcherStrategy: componentMatcherStrategy,
        expiryTime: typeof expiration === 'string' ? getISODateFromDateInput(expiration) : getExpiryTime(expiration),
        waiverReasonId,
        expireWhenRemediationAvailable: expireWhenRemediationAvailableSelected,
        rejectionReason,
      })
      .then(({ data }) => {
        startSaveMaskSuccessTimer(dispatch, actions.saveMaskTimerDone).then(() => {
          dispatch(returnToReviewWaiverRequestOriginPage());
        });
        return data;
      })
      .catch(rejectWithValue);
  }
);

const reviewRequestWaiverRequested = (state) => {
  state.submitMaskState = false;
  state.submitError = null;
};

const reviewRequestWaiverFulfilled = (state) => {
  state.submitMaskState = true;
  state.comments = userInput(null, '');
  state.isDirty = false;
  state.submitError = null;
};

const reviewRequestWaiverFailed = (state, { payload }) => {
  state.submitMaskState = null;
  state.submitError = Messages.getHttpErrorMessage(payload);
};

const updatePolicyWaiverRequest = createAsyncThunk(
  `${REDUCER_NAME}/updatePolicyWaiverRequest`,
  ({ expiration, expireWhenRemediationAvailableSelected }, { rejectWithValue, getState, dispatch }) => {
    const state = getState();
    const selectedWaiverScope = selectSelectedWaiverScope(state);
    const policyWaiverRequestId = selectWaiverRequestDetails(getState()).policyWaiverRequestId;
    const comment = selectComments(state).trimmedValue;
    const noteToReviewer = selectNoteToReviewer(state).trimmedValue;
    const componentMatcherStrategy = selectComponentMatcherStrategy(state);
    const waiverReasonId = selectWaiverReasonId(state);
    return axios
      .put(
        getViewOrUpdatePolicyWaiverRequestUrl(selectedWaiverScope.type, selectedWaiverScope.id, policyWaiverRequestId),
        {
          comment,
          noteToReviewer,
          matcherStrategy: componentMatcherStrategy,
          expiryTime: typeof expiration === 'string' ? getISODateFromDateInput(expiration) : getExpiryTime(expiration),
          waiverReasonId,
          expireWhenRemediationAvailable: expireWhenRemediationAvailableSelected,
        }
      )
      .then(({ data }) => {
        startSaveMaskSuccessTimer(dispatch, actions.saveMaskTimerDone).then(() => {
          dispatch(returnToAddOrRequestWaiverOriginPage());
        });
        return data;
      })
      .catch(rejectWithValue);
  }
);

const updatePolicyWaiverRequestRequested = (state) => {
  state.submitMaskState = false;
  state.submitError = null;
};

const updatePolicyWaiverRequestFulfilled = (state) => {
  state.submitMaskState = true;
  state.comments = userInput(null, '');
  state.noteToReviewer = userInput(null, '');
  state.isDirty = false;
  state.submitError = null;
};

const updatePolicyWaiverRequestFailed = (state, { payload }) => {
  state.submitMaskState = null;
  state.submitError = Messages.getHttpErrorMessage(payload);
};

const requestWaiverSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    initializeStateFromDetails,
    loadSelectedWaiverScope,
    setWaiverReasonId,
    setSelectedWaiverScope,
    setComponentMatcherStrategy,
    setExpiryTime,
    setCustomExpiryTime,
    setRequestWaiverComments,
    setNoteToReviewer,
    setRejectionReason,
    clearSubmitError,
    clearInitState: always(initialState),
    clearStateForReview: always(initialStateForReview),
    saveMaskTimerDone: propSet('submitMaskState', null),
  },
  extraReducers: {
    [createRequestWaiver.pending]: createRequestWaiverRequested,
    [createRequestWaiver.fulfilled]: createRequestWaiverFulfilled,
    [createRequestWaiver.rejected]: createRequestWaiverFailed,
    [reviewRequestWaiver.pending]: reviewRequestWaiverRequested,
    [reviewRequestWaiver.fulfilled]: reviewRequestWaiverFulfilled,
    [reviewRequestWaiver.rejected]: reviewRequestWaiverFailed,
    [updatePolicyWaiverRequest.pending]: updatePolicyWaiverRequestRequested,
    [updatePolicyWaiverRequest.fulfilled]: updatePolicyWaiverRequestFulfilled,
    [updatePolicyWaiverRequest.rejected]: updatePolicyWaiverRequestFailed,
  },
});

export const actions = {
  ...requestWaiverSlice.actions,
  createRequestWaiver,
  reviewRequestWaiver,
  updatePolicyWaiverRequest,
};

export default requestWaiverSlice.reducer;
