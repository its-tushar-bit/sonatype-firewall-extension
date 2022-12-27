/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { getRevokeGrandfatheringUrl } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { selectSelectedOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { startSaveMaskSuccessTimer } from 'MainRoot/util/reduxUtil';
import { propSet } from 'MainRoot/util/jsUtil';
import { OWNER_ACTIONS } from 'MainRoot/OrgsAndPolicies/utility/constants';

const REDUCER_NAME = `${OWNER_ACTIONS}/revokeGrandfathering`;

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

const revokeGrandfatheringFulfilled = (state) => {
  state.submitError = null;
  state.submitMaskState = true;
};

const revokeGrandfatheringFailed = (state, { payload }) => {
  state.submitMaskState = null;
  state.submitError = Messages.getHttpErrorMessage(payload);
};

const revokeGrandfathering = createAsyncThunk(
  `${REDUCER_NAME}/revoke`,
  (_, { getState, dispatch, rejectWithValue }) => {
    const state = getState();
    const owner = selectSelectedOwner(state);
    const url = getRevokeGrandfatheringUrl(owner.publicId);

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

const revokeGrandfatheringSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    openModal: propSet('isModalOpen', true),
    closeModal,
  },
  extraReducers: {
    [revokeGrandfathering.pending]: propSet('submitMaskState', false),
    [revokeGrandfathering.fulfilled]: revokeGrandfatheringFulfilled,
    [revokeGrandfathering.rejected]: revokeGrandfatheringFailed,
  },
});

export default revokeGrandfatheringSlice.reducer;
export const actions = {
  ...revokeGrandfatheringSlice.actions,
  revokeGrandfathering,
};
