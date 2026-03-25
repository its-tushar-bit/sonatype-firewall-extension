/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk } from '@reduxjs/toolkit';
import createSlice from 'MainRoot/reduxConfig/createSlice';
import { Messages } from 'MainRoot/util/CommonServices';
import { getApplicableAutoWaiversURL, getAutoWaiversConfigurationURLWaiver } from 'MainRoot/util/CLMLocation';
import { actions as rootActions } from 'MainRoot/OrgsAndPolicies/rootSlice';
import { selectSelectedOwnerTypeAndId } from '../orgsAndPoliciesSelectors';
import { prop } from 'ramda';
import { startSaveMaskSuccessTimer } from 'MainRoot/util/reduxUtil';
import { propSet } from 'MainRoot/util/jsUtil';
import { selectAutoWaiverToDelete } from '../autoWaiversSelectors';

const REDUCER_NAME = 'applicableAutoWaivers';

export const initialState = Object.freeze({
  loading: false,
  loadError: null,
  data: [],
  isDeleteModalOpen: false,
  deleteSubmitMask: null,
  deleteError: null,
  deleting: false,
  autoWaiverIdToDelete: null,
});

const loadApplicableAutoWaiversRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadApplicableAutoWaiversFulfilled = (state, { payload }) => {
  state.loading = false;
  state.data = payload;
};

const loadApplicableAutoWaiversFailed = (state, { payload }) => {
  state.loading = false;
  state.data = [];
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const loadApplicableAutoWaivers = createAsyncThunk(
  `${REDUCER_NAME}/loadApplicableAutoWaivers`,
  async (_, { getState, rejectWithValue, dispatch }) => {
    await dispatch(rootActions.loadSelectedOwner());
    const state = getState();
    const { ownerType, ownerId } = selectSelectedOwnerTypeAndId(state);
    return axios.get(getApplicableAutoWaiversURL(ownerType, ownerId)).then(prop('data')).catch(rejectWithValue);
  }
);

const openDeleteModal = (state, { payload }) => {
  state.isDeleteModalOpen = true;
  state.deleteSubmitMask = null;
  state.deleteError = null;
  state.deleting = false;
  state.autoWaiverIdToDelete = payload;
};

const closeDeleteModal = (state) => {
  state.isDeleteModalOpen = false;
  state.deleteSubmitMask = null;
  state.deleteError = null;
  state.deleting = false;
  state.autoWaiverIdToDelete = null;
};

const deleteAutoWaiverRequested = (state) => {
  state.deleting = true;
  state.deleteError = null;
  state.deleteSubmitMask = false;
};

const deleteAutoWaiverFulfilled = (state) => {
  state.deleting = false;
  state.deleteError = null;
  state.deleteSubmitMask = true;
};

const deleteAutoWaiverFailed = (state, { payload }) => {
  state.deleting = false;
  state.deleteSubmitMask = null;
  state.deleteError = Messages.getHttpErrorMessage(payload);
};

const deleteAutoWaiver = createAsyncThunk(
  `${REDUCER_NAME}/deleteWaiver`,
  async (_, { getState, dispatch, rejectWithValue }) => {
    const state = getState();
    const { ownerType, ownerId } = selectSelectedOwnerTypeAndId(state);
    const autoWaiverId = selectAutoWaiverToDelete(state);

    return axios
      .delete(getAutoWaiversConfigurationURLWaiver(ownerType, ownerId, autoWaiverId))
      .then(() => {
        startSaveMaskSuccessTimer(dispatch, actions.saveMaskTimerDone).then(() => {
          dispatch(actions.loadApplicableAutoWaivers());
          dispatch(actions.closeDeleteModal());
        });
      })
      .catch(rejectWithValue);
  }
);

const applicableAutoWaiversSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: { openDeleteModal, closeDeleteModal, saveMaskTimerDone: propSet('deleteSubmitMask', null) },
  extraReducers: {
    [loadApplicableAutoWaivers.pending]: loadApplicableAutoWaiversRequested,
    [loadApplicableAutoWaivers.fulfilled]: loadApplicableAutoWaiversFulfilled,
    [loadApplicableAutoWaivers.rejected]: loadApplicableAutoWaiversFailed,

    [deleteAutoWaiver.pending]: deleteAutoWaiverRequested,
    [deleteAutoWaiver.fulfilled]: deleteAutoWaiverFulfilled,
    [deleteAutoWaiver.rejected]: deleteAutoWaiverFailed,
  },
});

export const actions = {
  ...applicableAutoWaiversSlice.actions,
  loadApplicableAutoWaivers,
  deleteAutoWaiver,
};

export default applicableAutoWaiversSlice.reducer;
