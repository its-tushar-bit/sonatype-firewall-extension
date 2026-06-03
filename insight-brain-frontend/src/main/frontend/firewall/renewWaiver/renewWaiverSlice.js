/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk } from '@reduxjs/toolkit';
import createSlice from 'MainRoot/reduxConfig/createSlice';
import axios from 'axios';
import { Messages } from 'MainRoot/util/CommonServices';
import { getPolicyWaiverReasonsUrl, getFirewallWaiverDetailsUrl, renewWaiverUrl } from 'MainRoot/util/CLMLocation';
import { prop } from 'ramda';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import moment from 'moment';
import {
  isCustomExpiryTimeSelected,
  isNeverExpiryTimeSelected,
  normalizeFirewallOwnerType,
} from 'MainRoot/firewall/bulkWaive/firewallWaiverUtils';
import { isWaiverExpired } from 'MainRoot/util/waiverUtils';

const REDUCER_NAME = 'firewallRenewWaiver';

export const initialState = Object.freeze({
  loading: false,
  loadError: null,
  waiver: null,
  newExpiryTime: null,
  customExpiryTime: { value: '', isPristine: true },
  comment: { value: '', isPristine: true },
  reasonId: null,
  submitMaskState: null,
  submitError: null,
  isDirty: false,
  waiverReasons: [],
  waiverReasonsLoading: false,
  waiverReasonsError: null,
});

// Async thunk to load waiver details for renewal
const loadWaiverForRenewal = createAsyncThunk(
  `${REDUCER_NAME}/loadWaiverForRenewal`,
  (_, { getState, rejectWithValue }) => {
    const { ownerType: ownerTypeRaw, ownerId, waiverId } = selectRouterCurrentParams(getState());
    const ownerType = normalizeFirewallOwnerType(ownerTypeRaw);
    return axios
      .get(getFirewallWaiverDetailsUrl(ownerType, ownerId, waiverId))
      .then(prop('data'))
      .catch(rejectWithValue);
  }
);

// Async thunk to submit renewal
const submitRenewal = createAsyncThunk(
  `${REDUCER_NAME}/submitRenewal`,
  async (waiverId, { getState, rejectWithValue }) => {
    const state = getState();
    const slice = state[REDUCER_NAME];
    const { ownerType: ownerTypeRaw, ownerId, type, sidebarReference, sidebarId, page } = selectRouterCurrentParams(
      state
    );

    let newExpiryTime = null;
    if (isCustomExpiryTimeSelected(slice.newExpiryTime)) {
      if (slice.customExpiryTime?.value) {
        newExpiryTime = moment(slice.customExpiryTime.value, 'YYYY-MM-DD').endOf('day').toISOString();
      }
    } else if (!isNeverExpiryTimeSelected(slice.newExpiryTime) && slice.newExpiryTime) {
      const baseDate =
        slice.waiver?.expiryTime && !isWaiverExpired(slice.waiver.expiryTime)
          ? moment(slice.waiver.expiryTime)
          : moment();
      newExpiryTime = baseDate.add(parseInt(slice.newExpiryTime, 10), 'days').endOf('day').toISOString();
    }

    const payload = {
      waiverIds: [waiverId],
      newExpiryTime,
      comment: slice.comment.value || null,
      reasonId: slice.reasonId || null,
    };

    try {
      const response = await axios.post(renewWaiverUrl(), payload);
      const result = response.data;
      if (result.renewed === 0 && result.errors?.length > 0) {
        return rejectWithValue(result.errors[0]);
      }
      const { prevState, prevParams } = state.router;
      const returnStateName = prevState?.name || 'firewall.waiver.details';
      const returnParams =
        returnStateName === 'firewall.waiver.details'
          ? { ownerType: ownerTypeRaw, ownerId, waiverId, type, sidebarReference, sidebarId, page }
          : prevParams;
      return { returnStateName, returnParams };
    } catch (error) {
      return rejectWithValue(error);
    }
  }
);

// Async thunk to load waiver reasons
const loadWaiverReasons = createAsyncThunk(`${REDUCER_NAME}/loadWaiverReasons`, (_, { rejectWithValue }) =>
  axios.get(getPolicyWaiverReasonsUrl()).then(prop('data')).catch(rejectWithValue)
);

// Reducer handlers
const loadWaiverRequested = (state) => ({
  ...state,
  loading: true,
  loadError: null,
});

const loadWaiverFulfilled = (state, { payload }) => ({
  ...state,
  loading: false,
  loadError: null,
  waiver: payload,
  // For waivers with an existing expiry, default to 30 days to extend from current expiry.
  // For never-expiring waivers, default to Never (user must explicitly pick a new expiry).
  newExpiryTime: payload?.expiryTime ? '30' : 'never',
});

const loadWaiverFailed = (state, { payload }) => ({
  ...state,
  loading: false,
  loadError: Messages.getHttpErrorMessage(payload),
});

const submitRenewalRequested = (state) => ({
  ...state,
  submitMaskState: false,
  submitError: null,
  isDirty: false,
});

const submitRenewalFulfilled = (state, { payload }) => ({
  ...state,
  submitMaskState: true,
  submitError: null,
  returnStateName: payload.returnStateName,
  returnParams: payload.returnParams,
});

const submitRenewalFailed = (state, { payload }) => ({
  ...state,
  submitMaskState: null,
  submitError: Messages.getHttpErrorMessage(payload),
});

const loadWaiverReasonsRequested = (state) => ({
  ...state,
  waiverReasonsLoading: true,
  waiverReasonsError: null,
});

const loadWaiverReasonsFulfilled = (state, { payload }) => ({
  ...state,
  waiverReasonsLoading: false,
  waiverReasons: payload,
});

const loadWaiverReasonsFailed = (state, { payload }) => ({
  ...state,
  waiverReasonsLoading: false,
  waiverReasonsError: Messages.getHttpErrorMessage(payload),
});

// Action handlers
const setNewExpiryTime = (state, action) => ({
  ...state,
  newExpiryTime: action.payload,
  isDirty: true,
});

const setCustomExpiryTime = (state, action) => ({
  ...state,
  customExpiryTime: {
    value: action.payload,
    isPristine: false,
  },
  isDirty: true,
});

const setComment = (state, action) => ({
  ...state,
  comment: {
    value: action.payload,
    isPristine: false,
  },
  isDirty: true,
});

const setReasonId = (state, action) => ({
  ...state,
  reasonId: action.payload,
  isDirty: true,
});

const clearSubmitError = (state) => ({
  ...state,
  submitError: null,
});

const resetRenewWaiverState = () => initialState;

const renewWaiverSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setNewExpiryTime,
    setCustomExpiryTime,
    setComment,
    setReasonId,
    clearSubmitError,
    resetRenewWaiverState,
  },
  extraReducers: {
    [loadWaiverForRenewal.pending]: loadWaiverRequested,
    [loadWaiverForRenewal.fulfilled]: loadWaiverFulfilled,
    [loadWaiverForRenewal.rejected]: loadWaiverFailed,
    [submitRenewal.pending]: submitRenewalRequested,
    [submitRenewal.fulfilled]: submitRenewalFulfilled,
    [submitRenewal.rejected]: submitRenewalFailed,
    [loadWaiverReasons.pending]: loadWaiverReasonsRequested,
    [loadWaiverReasons.fulfilled]: loadWaiverReasonsFulfilled,
    [loadWaiverReasons.rejected]: loadWaiverReasonsFailed,
    // Reset state when leaving the page
    '@@reduxUiRouter/onFinish': (state, action) => {
      const { toState } = action.payload || {};
      if (toState && toState.name !== 'firewall.renewWaiver') {
        return initialState;
      }
      return state;
    },
  },
});

export default renewWaiverSlice.reducer;

export const actions = {
  ...renewWaiverSlice.actions,
  loadWaiverForRenewal,
  submitRenewal,
  loadWaiverReasons,
};
