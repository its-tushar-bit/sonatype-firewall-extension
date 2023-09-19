/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { getLegacyViolationModalUrl } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { selectSelectedOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { startSaveMaskSuccessTimer } from 'MainRoot/util/reduxUtil';
import { propSet } from 'MainRoot/util/jsUtil';
import { OWNER_ACTIONS } from 'MainRoot/OrgsAndPolicies/utility/constants';

const REDUCER_NAME = `${OWNER_ACTIONS}/legacyViolationModal`;

export const initialState = {
  submitError: null,
  submitMaskState: null,
  isModalOpen: false,
};

const closeModal = (state) => {
  state.submitError = null;
  state.submitMaskState = null;
  state.isModalOpen = false;
};

const legacyViolationFulfilled = (state) => {
  state.submitError = null;
  state.submitMaskState = true;
};

const legacyViolationFailed = (state, { payload }) => {
  state.submitMaskState = null;
  state.submitError = Messages.getHttpErrorMessage(payload);
};

const legacyViolation = createAsyncThunk(REDUCER_NAME, (_, { getState, dispatch, rejectWithValue }) => {
  const state = getState();
  const owner = selectSelectedOwner(state);
  const url = getLegacyViolationModalUrl(owner.publicId);

  return axios
    .put(url)
    .then(() => {
      startSaveMaskSuccessTimer(dispatch, actions.closeModal).then(() => {
        dispatch(
          stateGo('management.view.organization.app', {
            organizationId: owner.organizationId,
          })
        );
      });
    })
    .catch((err) => rejectWithValue(err));
});

const legacyViolationModalSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    openModal: propSet('isModalOpen', true),
    closeModal,
  },
  extraReducers: {
    [legacyViolation.pending]: propSet('submitMaskState', false),
    [legacyViolation.fulfilled]: legacyViolationFulfilled,
    [legacyViolation.rejected]: legacyViolationFailed,
  },
});

export const actions = {
  ...legacyViolationModalSlice.actions,
  legacyViolation: legacyViolation,
};
export default legacyViolationModalSlice.reducer;
