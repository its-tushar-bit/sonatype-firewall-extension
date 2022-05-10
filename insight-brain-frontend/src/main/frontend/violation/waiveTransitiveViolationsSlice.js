/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { propSet, propSetConst } from '../util/reduxToolkitUtil';
import { getWaiveTransitiveViolationsUrl } from '../util/CLMLocation';
import { Messages } from '../utilAngular/CommonServices';
import { getExpiryTime } from '../util/waiverUtils';
import { TRANSITIVE_VIOLATIONS_TOGGLE_WAIVE } from './transitiveViolationsActions';

const REDUCER_NAME = 'waiveTransitiveViolationsReducer';

const initialState = {
  scope: null,
  expiration: 'never',
  comments: '',
  submitMaskState: null,
  saveError: null,
};

function cancel() {
  return {
    ...initialState,
  };
}

function saveRequested(state) {
  return {
    ...state,
    submitMaskState: false,
  };
}

function saveFulfilled(state) {
  return {
    ...state,
    submitMaskState: true,
    saveError: initialState.saveError,
  };
}

function saveFailed(state, { payload }) {
  return {
    ...state,
    submitMaskState: null,
    saveError: Messages.getHttpErrorMessage(payload.response),
  };
}

const save = createAsyncThunk(`${REDUCER_NAME}/save`, (_, { getState, dispatch, rejectWithValue }) => {
  const { scope, expiration, comments } = getState().waiveTransitiveViolations;
  const { scanId, hash } = getState().router.currentParams;
  return axios
    .post(getWaiveTransitiveViolationsUrl(scope, scanId, hash), {
      expiryTime: expiration === 'never' ? null : getExpiryTime(expiration),
      comment: comments === '' ? null : comments,
      applyToAllComponents: false,
    })
    .then(() => {
      startSubmitMaskSuccessTimer(dispatch);
    })
    .catch(rejectWithValue);
});

function startSubmitMaskSuccessTimer(dispatch) {
  setTimeout(() => {
    dispatch(actions.submitMaskTimerDone());
    dispatch({ type: TRANSITIVE_VIOLATIONS_TOGGLE_WAIVE });
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

const waiveTransitiveViolationsSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setScope: propSet('scope'),
    setExpiration: propSet('expiration'),
    setComments: propSet('comments'),
    cancel,
    submitMaskTimerDone: propSetConst('submitMaskState', null),
  },
  extraReducers: {
    [save.pending]: saveRequested,
    [save.fulfilled]: saveFulfilled,
    [save.rejected]: saveFailed,
  },
});

export default waiveTransitiveViolationsSlice.reducer;
export const actions = {
  ...waiveTransitiveViolationsSlice.actions,
  save,
};
