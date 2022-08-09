/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { getOrganizationsUrl, getApplicationsUrl } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { actions as organizationActions } from '../organizationsSlice';
import { actions as applicationsActions } from '../applicationsSlice';
import { selectSelectedOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { selectIsApplication } from 'MainRoot/reduxUiRouter/routerSelectors';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { startSaveMaskSuccessTimer } from 'MainRoot/util/reduxUtil';
import { propSet } from 'MainRoot/util/jsUtil';
import { OWNER_EDITOR } from 'MainRoot/OrgsAndPolicies/utility/constants';

const REDUCER_NAME = `${OWNER_EDITOR}/delete`;

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

const removeOwnerFulfilled = (state) => {
  state.submitError = null;
  state.submitMaskState = true;
};

const removeOwnerFailed = (state, { payload }) => {
  state.submitMaskState = null;
  state.submitError = Messages.getHttpErrorMessage(payload);
};

const removeOwner = createAsyncThunk(`${REDUCER_NAME}/removeOwner`, (_, { getState, dispatch, rejectWithValue }) => {
  const state = getState();
  const ownerToDelete = selectSelectedOwner(state);
  const isApp = selectIsApplication(state);
  const url = isApp
    ? `${getApplicationsUrl()}/${ownerToDelete.publicId}`
    : `${getOrganizationsUrl()}/${ownerToDelete.id}`;

  return axios
    .delete(url)
    .then(() => {
      isApp
        ? dispatch(applicationsActions.removeApplicationFromList(ownerToDelete.id))
        : dispatch(organizationActions.removeOrganizationFromList(ownerToDelete.id));

      startSaveMaskSuccessTimer(dispatch, actions.closeModal).then(() => {
        dispatch(
          stateGo('management.view.organization', {
            organizationId: isApp ? ownerToDelete.organizationId : ownerToDelete.parentOrganizationId,
          })
        );
      });
    })
    .catch((err) => rejectWithValue(err));
});

const deleteOwner = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    openModal: propSet('isModalOpen', true),
    closeModal,
  },
  extraReducers: {
    [removeOwner.pending]: propSet('submitMaskState', false),
    [removeOwner.fulfilled]: removeOwnerFulfilled,
    [removeOwner.rejected]: removeOwnerFailed,
  },
});

export default deleteOwner.reducer;
export const actions = {
  ...deleteOwner.actions,
  removeOwner,
};
