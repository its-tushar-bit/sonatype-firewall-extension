/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk } from '@reduxjs/toolkit';
import { nxDateInputStateHelpers, nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import { always } from 'ramda';

import createSlice from 'MainRoot/reduxConfig/createSlice';
import { Messages } from 'MainRoot/util/CommonServices';
import { getCreatePolicyWaiverRequestUrl } from 'MainRoot/util/CLMLocation';
import { startSaveMaskSuccessTimer } from 'MainRoot/util/reduxUtil';
import { getExpiryTime, isCustomExpiryTimeValid } from 'MainRoot/util/waiverUtils';
import { getISODateFromDateInput, propSet } from 'MainRoot/util/jsUtil';
import { selectViolationId, selectRepositoryId } from 'MainRoot/reduxUiRouter/routerSelectors';
import {
  selectFirewallRequestWaiverComments,
  selectFirewallRequestWaiverNoteToReviewer,
  selectFirewallRequestWaiverComponentMatcherStrategy,
  selectFirewallRequestWaiverReasonId,
  selectFirewallRequestWaiverSelectedScope,
} from './firewallRequestWaiverSelectors';
import { returnToAddOrRequestWaiverOriginPage } from 'MainRoot/waivers/waiverActions';

const REDUCER_NAME = 'firewallRequestWaiver';

const { initialState: rscInitialState, userInput } = nxTextInputStateHelpers;

const customDateValidator = (value) => (isCustomExpiryTimeValid(value) ? null : 'Date must be in the future');

export const initialState = {
  loading: false,
  loadError: null,
  submitError: null,
  submitMaskState: null,
  isDirty: false,
  componentMatcherStrategy: null,
  selectedWaiverScope: null,
  expiryTime: null,
  customExpiryTime: nxDateInputStateHelpers.initialState(''),
  waiverReasonId: null,
  comments: rscInitialState(''),
  noteToReviewer: rscInitialState(''),
};

// Reducers

const setComponentMatcherStrategy = (state, { payload }) => {
  state.componentMatcherStrategy = payload || null;
  state.isDirty = true;
};

const setSelectedWaiverScope = (state, { payload }) => {
  state.selectedWaiverScope = payload || null;
  state.isDirty = true;
};

const setExpiryTime = (state, { payload }) => {
  state.expiryTime = payload || null;
  state.customExpiryTime = nxDateInputStateHelpers.initialState('');
  state.isDirty = true;
};

const setCustomExpiryTime = (state, { payload }) => {
  state.customExpiryTime = nxDateInputStateHelpers.userInput(customDateValidator, payload);
  state.isDirty = true;
};

const setWaiverReasonId = (state, { payload }) => {
  state.waiverReasonId = payload || null;
  state.isDirty = true;
};

const setComments = (state, { payload }) => {
  state.comments = userInput(null, payload);
  state.isDirty = true;
};

const setNoteToReviewer = (state, { payload }) => {
  state.noteToReviewer = userInput(null, payload);
  state.isDirty = true;
};

// Thunk

const submitFirewallWaiverRequest = createAsyncThunk(
  `${REDUCER_NAME}/submitFirewallWaiverRequest`,
  ({ expiration, expireWhenRemediationAvailableSelected }, { rejectWithValue, getState, dispatch }) => {
    const state = getState();
    const policyViolationId = selectViolationId(state);
    const repositoryId = selectRepositoryId(state);
    const comment = selectFirewallRequestWaiverComments(state).trimmedValue;
    const noteToReviewer = selectFirewallRequestWaiverNoteToReviewer(state).trimmedValue;
    const componentMatcherStrategy = selectFirewallRequestWaiverComponentMatcherStrategy(state);
    const waiverReasonId = selectFirewallRequestWaiverReasonId(state);
    const selectedWaiverScope = selectFirewallRequestWaiverSelectedScope(state);

    const ownerType = selectedWaiverScope?.type || 'repository';
    const ownerId = selectedWaiverScope?.id || repositoryId;

    return axios
      .post(getCreatePolicyWaiverRequestUrl(ownerType, ownerId, policyViolationId), {
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

const submitRequested = (state) => {
  state.submitMaskState = false;
  state.submitError = null;
};

const submitFulfilled = (state) => {
  state.submitMaskState = true;
  state.comments = userInput(null, '');
  state.noteToReviewer = userInput(null, '');
  state.isDirty = false;
  state.submitError = null;
};

const submitFailed = (state, { payload }) => {
  state.submitMaskState = null;
  state.submitError = Messages.getHttpErrorMessage(payload);
};

const firewallRequestWaiverSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setComponentMatcherStrategy,
    setSelectedWaiverScope,
    setExpiryTime,
    setCustomExpiryTime,
    setWaiverReasonId,
    setComments,
    setNoteToReviewer,
    clearState: always(initialState),
    saveMaskTimerDone: propSet('submitMaskState', null),
  },
  extraReducers: {
    [submitFirewallWaiverRequest.pending]: submitRequested,
    [submitFirewallWaiverRequest.fulfilled]: submitFulfilled,
    [submitFirewallWaiverRequest.rejected]: submitFailed,
  },
});

export const actions = {
  ...firewallRequestWaiverSlice.actions,
  submitFirewallWaiverRequest,
};

export default firewallRequestWaiverSlice.reducer;
