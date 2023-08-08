/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { getImportPoliciesUrl } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { selectSelectedOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { selectImportPoliciesSlice } from './importPoliciesSelectors';
import { startSaveMaskSuccessTimer } from 'MainRoot/util/reduxUtil';
import { propSet } from 'MainRoot/util/jsUtil';
import { OWNER_ACTIONS } from 'MainRoot/OrgsAndPolicies/utility/constants';
import { nxFileUploadStateHelpers } from '@sonatype/react-shared-components';
import { actions as ownerSummaryActions } from 'MainRoot/OrgsAndPolicies/ownerSummarySlice';

const REDUCER_NAME = `${OWNER_ACTIONS}/importPolicies`;

const { initialState: rscInitialFileUploadState, userInput: userFileUploadInput } = nxFileUploadStateHelpers;

export const initialState = {
  submitError: null,
  submitMaskState: null,
  isModalOpen: false,
  ownerFile: rscInitialFileUploadState(null),
};

const closeModal = (state) => {
  state.submitError = null;
  state.submitMaskState = null;
  state.isModalOpen = false;
  state.ownerFile = rscInitialFileUploadState(null);
};

const importPoliciesFulfilled = (state) => {
  state.submitError = null;
  state.submitMaskState = true;
};

const selectFile = (state, { payload }) => {
  state.ownerFile = userFileUploadInput(payload);
};

const importPoliciesFailed = (state, { payload }) => {
  state.submitMaskState = null;
  state.submitError = Messages.getHttpErrorMessage(payload);
};

const importPolicies = createAsyncThunk(REDUCER_NAME, (_, { getState, dispatch, rejectWithValue }) => {
  const state = getState();
  const owner = selectSelectedOwner(state);
  const url = getImportPoliciesUrl(owner.id);
  const { ownerFile } = selectImportPoliciesSlice(state);
  const formData = new FormData();
  formData.append('file', ownerFile.files[0]);
  return axios
    .post(url, formData)
    .then(() => {
      startSaveMaskSuccessTimer(dispatch, actions.closeModal).then(() => {
        dispatch(ownerSummaryActions.loadOwnerSummary());
      });
    })
    .catch((err) => rejectWithValue(err));
});

const importPoliciesSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    openModal: propSet('isModalOpen', true),
    closeModal,
    selectFile,
  },
  extraReducers: {
    [importPolicies.pending]: propSet('submitMaskState', false),
    [importPolicies.fulfilled]: importPoliciesFulfilled,
    [importPolicies.rejected]: importPoliciesFailed,
  },
});

export default importPoliciesSlice.reducer;
export const actions = {
  ...importPoliciesSlice.actions,
  importPolicies,
};
