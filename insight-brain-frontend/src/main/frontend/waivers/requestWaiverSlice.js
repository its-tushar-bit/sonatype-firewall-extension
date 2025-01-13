/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';

import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import { always, prop, isEmpty } from 'ramda';
import { getWaiverRequestWebhooksCountUrl, saveRequestWaiverUrl } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { selectViolationId } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectComments, selectWaiverReasonId } from './requestWaiverSelectors';
import { startSaveMaskSuccessTimer } from 'MainRoot/util/reduxUtil';
import { propSet } from 'MainRoot/util/jsUtil';
import { returnToAddWaiverOriginPage } from './waiverActions';

const { initialState: rscInitialState, userInput } = nxTextInputStateHelpers;

const REDUCER_NAME = `requestWaiver`;
export const initialState = {
  loading: false,
  submitError: null,
  isDirty: false,
  comments: rscInitialState(''),
  submitMaskState: null,
  waiverReasonId: null,
  webhooks: {
    loading: false,
    error: null,
    waiverRequestWebhookAvailable: false,
  },
};

const submitRequestWaiverRequested = (state) => {
  state.submitMaskState = false;
  state.submitError = null;
};

const submitRequestWaiverFulfilled = (state) => {
  state.submitMaskState = true;
  state.comments = userInput(null, '');
  state.isDirty = false;
  state.submitError = null;
};

const submitRequestWaiverFailed = (state, { payload }) => {
  state.submitMaskState = null;
  state.submitError = Messages.getHttpErrorMessage(payload);
};

const setRequestWaiverComments = (state, { payload }) => {
  state.comments = userInput(null, payload);
  return computeIsDirty(state, payload);
};

const setWaiverReasonId = (state, { payload }) => {
  state.waiverReasonId = payload ? payload : null;
  return state;
};

const submitRequestWaiver = createAsyncThunk(
  `${REDUCER_NAME}/submitRequestWaiver`,
  ({ policyViolationLink, addWaiverLink }, { rejectWithValue, getState, dispatch }) => {
    const state = getState();
    const policyViolationId = selectViolationId(state);
    const comment = selectComments(state).trimmedValue;
    const reasonId = selectWaiverReasonId(state);
    return axios
      .post(saveRequestWaiverUrl(policyViolationId), { reasonId, policyViolationLink, addWaiverLink, comment })
      .then(({ data }) => {
        startSaveMaskSuccessTimer(dispatch, actions.saveMaskTimerDone).then(() => {
          dispatch(returnToAddWaiverOriginPage());
        });
        return data;
      })
      .catch(rejectWithValue);
  }
);

const computeIsDirty = (state, comments) => {
  state.isDirty = !isEmpty(comments);
};

const getWaiverRequestWebhooksPending = (state) => {
  state.webhooks.loading = true;
  state.webhooks.error = null;
  state.webhooks.waiverRequestWebhookAvailable = false;
};

const getWaiverRequestWebhooksFailed = (state, { payload }) => {
  state.webhooks.loading = false;
  state.webhooks.error = Messages.getHttpErrorMessage(payload);
  state.webhooks.waiverRequestWebhookAvailable = false;
};

const getWaiverRequestWebhooksFulfilled = (state, { payload }) => {
  state.webhooks.loading = false;
  state.webhooks.error = null;
  state.webhooks.waiverRequestWebhookAvailable = !!payload;
};

const getWaiverRequestWebhooks = createAsyncThunk(
  `${REDUCER_NAME}/getWaiverRequestWebhook`,
  (_, { rejectWithValue }) => {
    return axios.get(getWaiverRequestWebhooksCountUrl()).then(prop('data')).catch(rejectWithValue);
  }
);

const requestWaiverSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setWaiverReasonId,
    setRequestWaiverComments,
    clearInitState: always(initialState),
    saveMaskTimerDone: propSet('submitMaskState', null),
  },
  extraReducers: {
    [submitRequestWaiver.pending]: submitRequestWaiverRequested,
    [submitRequestWaiver.fulfilled]: submitRequestWaiverFulfilled,
    [submitRequestWaiver.rejected]: submitRequestWaiverFailed,
    [getWaiverRequestWebhooks.pending]: getWaiverRequestWebhooksPending,
    [getWaiverRequestWebhooks.fulfilled]: getWaiverRequestWebhooksFulfilled,
    [getWaiverRequestWebhooks.rejected]: getWaiverRequestWebhooksFailed,
  },
});

export const actions = {
  ...requestWaiverSlice.actions,
  submitRequestWaiver,
  getWaiverRequestWebhooks,
};

export default requestWaiverSlice.reducer;
