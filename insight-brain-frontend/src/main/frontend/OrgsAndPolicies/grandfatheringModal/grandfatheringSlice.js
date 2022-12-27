/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { getGrandfatheringModalUrl } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { selectSelectedOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { startSaveMaskSuccessTimer } from 'MainRoot/util/reduxUtil';
import { propSet } from 'MainRoot/util/jsUtil';
import { OWNER_ACTIONS } from 'MainRoot/OrgsAndPolicies/utility/constants';

const REDUCER_NAME = `${OWNER_ACTIONS}/grandfathering`;

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

const grandfatheringFulfilled = (state) => {
  state.submitError = null;
  state.submitMaskState = true;
};

const grandfatheringFailed = (state, { payload }) => {
  state.submitMaskState = null;
  state.submitError = Messages.getHttpErrorMessage(payload);
};

const grandfathering = createAsyncThunk(REDUCER_NAME, (_, { getState, dispatch, rejectWithValue }) => {
  const state = getState();
  const owner = selectSelectedOwner(state);
  const url = getGrandfatheringModalUrl(owner.publicId);

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

const grandfatheringSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    openModal: propSet('isModalOpen', true),
    closeModal,
  },
  extraReducers: {
    [grandfathering.pending]: propSet('submitMaskState', false),
    [grandfathering.fulfilled]: grandfatheringFulfilled,
    [grandfathering.rejected]: grandfatheringFailed,
  },
});

export default grandfatheringSlice.reducer;
export const actions = {
  ...grandfatheringSlice.actions,
  grandfathering,
};
