/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { omit, pick } from 'ramda';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { getOrganizationsUrl, getApplicationsUrl } from '../util/CLMLocation';
import { pathSetConst } from 'MainRoot/util/reduxToolkitUtil';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { actions as organizationActions } from './organizationsSlice';
import { actions as applicationsActions } from './applicationsSlice';

const REDUCER_NAME = 'ownerEditor';

export const initialState = {
  deleteModal: {
    deleting: null,
    success: null,
    errorState: null,
  },
};

const removeOwnerFulfilled = (state) => {
  state.deleteModal.success = true;
  state.deleteModal.deleting = null;
  state.deleteModal.errorState = null;
};

const removeOwnerFailed = (state, { payload }) => {
  state.deleteModal.deleting = false;
  state.deleteModal.errorState = Messages.getHttpErrorMessage(payload);
};

const resetDeleteModalState = (state) => {
  state.deleteModal.success = null;
  state.deleteModal.deleting = null;
  state.deleteModal.errorState = null;
};

const removeOwner = createAsyncThunk(
  `${REDUCER_NAME}/removeOrganization`,
  ({ ownerToDelete, isApp }, { dispatch, rejectWithValue }) => {
    const url = isApp
      ? `${getApplicationsUrl()}/${ownerToDelete.publicId}`
      : `${getOrganizationsUrl()}/${ownerToDelete.id}`;

    return axios
      .delete(url)
      .then(() => {
        isApp
          ? dispatch(applicationsActions.removeApplicationFromList(ownerToDelete.id))
          : dispatch(organizationActions.removeOrganizationFromList(ownerToDelete.id));
      })
      .catch(rejectWithValue);
  }
);

const updateOwner = createAsyncThunk(
  `${REDUCER_NAME}/updateApplication`,
  ({ ownerToSave, isApp }, { dispatch, rejectWithValue }) => {
    const url = isApp ? getApplicationsUrl() : getOrganizationsUrl();
    const payload = isApp
      ? pick(['id', 'name', 'publicId', 'organizationId', 'contactInternalName'], ownerToSave)
      : omit(['isNew'], ownerToSave);

    const isNew = !!ownerToSave.isNew;

    return axios[isNew ? 'post' : 'put'](url, payload)
      .then(({ data }) => {
        const updatedOwner = { isNew, [isApp ? 'application' : 'organization']: data };

        isApp
          ? dispatch(applicationsActions.updateApplication(updatedOwner))
          : dispatch(organizationActions.updateOrganization(updatedOwner));

        return updatedOwner;
      })
      .catch(rejectWithValue);
  }
);

const ownerEditorSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    resetDeleteModalState,
  },
  extraReducers: {
    [removeOwner.pending]: pathSetConst(['deleteModal', 'deleting'], true),
    [removeOwner.fulfilled]: removeOwnerFulfilled,
    [removeOwner.rejected]: removeOwnerFailed,
  },
});

export const actions = {
  ...ownerEditorSlice.actions,
  updateOwner,
  removeOwner,
};

export default ownerEditorSlice.reducer;
