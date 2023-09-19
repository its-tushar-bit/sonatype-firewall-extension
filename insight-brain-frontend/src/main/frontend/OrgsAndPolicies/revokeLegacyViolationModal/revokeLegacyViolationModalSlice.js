/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { getRevokeLegacyViolationUrl } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { selectSelectedOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { startSaveMaskSuccessTimer } from 'MainRoot/util/reduxUtil';
import { propSet } from 'MainRoot/util/jsUtil';
import { OWNER_ACTIONS } from 'MainRoot/OrgsAndPolicies/utility/constants';

const REDUCER_NAME = `${OWNER_ACTIONS}/revokeLegacyViolationModal`;

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

const revokeLegacyViolationFulfilled = (state) => {
  state.submitError = null;
  state.submitMaskState = true;
};

const revokeLegacyViolationFailed = (state, { payload }) => {
  state.submitMaskState = null;
  state.submitError = Messages.getHttpErrorMessage(payload);
};

const revokeLegacyViolationModal = createAsyncThunk(
  `${REDUCER_NAME}/revoke`,
  (_, { getState, dispatch, rejectWithValue }) => {
    const state = getState();
    const owner = selectSelectedOwner(state);
    const url = getRevokeLegacyViolationUrl(owner.publicId);

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
  }
);

const revokeLegacyViolationModalSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    openModal: propSet('isModalOpen', true),
    closeModal,
  },
  extraReducers: {
    [revokeLegacyViolationModal.pending]: propSet('submitMaskState', false),
    [revokeLegacyViolationModal.fulfilled]: revokeLegacyViolationFulfilled,
    [revokeLegacyViolationModal.rejected]: revokeLegacyViolationFailed,
  },
});

export default revokeLegacyViolationModalSlice.reducer;
export const actions = {
  ...revokeLegacyViolationModalSlice.actions,
  revokeLegacyViolation: revokeLegacyViolationModal,
};
