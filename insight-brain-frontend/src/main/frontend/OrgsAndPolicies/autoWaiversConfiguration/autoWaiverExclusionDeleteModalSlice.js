/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk } from '@reduxjs/toolkit';
import createSlice from 'MainRoot/reduxConfig/createSlice';
import { Messages } from 'MainRoot/util/CommonServices';
import { getAutoWaiverExclusionsByExclusionIdUrl } from 'MainRoot/util/CLMLocation';
import { startSaveMaskSuccessTimer } from 'MainRoot/util/reduxUtil';
import { selectSelectedOwnerTypeAndId } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { selectAutoWaiverExclusionDeleteModalData } from 'MainRoot/OrgsAndPolicies/autoWaiversConfiguration/autoWaiverExclusionDeleteModalSelectors';
import { actions as autoWaiverExclusionActions } from './autoWaiverExclusionsSlice';

const REDUCER_NAME = 'autoWaiverExclusionDeleteModal';

export const initialState = Object.freeze({
  isModalOpen: false,
  submitMaskState: null,
  submitError: null,
  data: null,
});

const openModal = (state, { payload }) => {
  state.submitMaskState = null;
  state.isModalOpen = true;
  state.data = payload;
};

const closeModal = (state) => {
  state.submitError = null;
  state.submitMaskState = null;
  state.isModalOpen = false;
  state.data = null;
};

const deleteAutoWaiverExclusion = createAsyncThunk(
  `${REDUCER_NAME}/deleteExclusion`,
  async (_, { getState, dispatch, rejectWithValue }) => {
    const state = getState();
    const { ownerType, ownerId } = selectSelectedOwnerTypeAndId(state);
    const { autoPolicyWaiverId, autoPolicyWaiverExclusionId } = selectAutoWaiverExclusionDeleteModalData(state);

    return axios
      .delete(
        getAutoWaiverExclusionsByExclusionIdUrl(ownerType, ownerId, autoPolicyWaiverId, autoPolicyWaiverExclusionId)
      )
      .then(() => {
        startSaveMaskSuccessTimer(dispatch, actions.closeModal).then(() =>
          dispatch(autoWaiverExclusionActions.loadAutoWaiverExclusion())
        );
      })
      .catch(rejectWithValue);
  }
);

const deleteAutoWaiverExclusionRequested = (state) => {
  state.submitMaskState = false;
};

const deleteAutoWaiverExclusionFulfilled = (state) => {
  state.submitMaskState = true;
  state.submitError = null;
};

const deleteAutoWaiverExclusionFailed = (state, { payload }) => {
  state.submitMaskState = null;
  state.submitError = Messages.getHttpErrorMessage(payload);
};

const autoWaiverExclusionDeleteModalSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    closeModal,
    openModal,
  },
  extraReducers: {
    [deleteAutoWaiverExclusion.pending]: deleteAutoWaiverExclusionRequested,
    [deleteAutoWaiverExclusion.fulfilled]: deleteAutoWaiverExclusionFulfilled,
    [deleteAutoWaiverExclusion.rejected]: deleteAutoWaiverExclusionFailed,
  },
});

export const actions = {
  ...autoWaiverExclusionDeleteModalSlice.actions,
  deleteAutoWaiverExclusion,
};

export default autoWaiverExclusionDeleteModalSlice.reducer;
