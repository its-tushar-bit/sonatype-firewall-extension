/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { findIndex, isEmpty, pick, prop, propEq, reject } from 'ramda';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';

import { Messages } from 'MainRoot/util/CommonServices';
import { selectApplications } from './applicationsSelectors';
import { pathSetConst } from 'MainRoot/util/reduxToolkitUtil';
import { getApplicationsUrl, getMoveApplicationUrl } from '../util/CLMLocation';
import moveApplicationErrorMessages from 'MainRoot/owner.manager/move.application/move.application.messages';

const REDUCER_NAME = 'applications';

export const initialState = {
  loadingApplications: false,
  loadApplicationsError: null,
  applications: [],
  deleteModal: {
    deleting: null,
    success: null,
    errorState: null,
  },
};

const loadApplicationsRequested = (state) => {
  state.loadingApplications = true;
  state.loadApplicationsError = null;
};

const loadApplicationsFulfilled = (state, { payload }) => {
  state.loadingApplications = false;
  state.applications = payload;
};

const loadApplicationsFailed = (state, { payload }) => {
  state.loadingApplications = false;
  state.loadApplicationsError = Messages.getHttpErrorMessage(payload);
};

const loadApplications = createAsyncThunk(
  `${REDUCER_NAME}/loadApplications`,
  async (forceReload, { rejectWithValue, getState }) => {
    const state = getState();
    const applications = selectApplications(state);

    if (isEmpty(applications) || forceReload) {
      return axios.get(getApplicationsUrl()).then(prop('data')).catch(rejectWithValue);
    } else {
      return Promise.resolve(applications);
    }
  }
);

const updateApplication = createAsyncThunk(
  `${REDUCER_NAME}/updateApplication`,
  (applicationToSave, { rejectWithValue }) => {
    const payload = pick(['id', 'name', 'publicId', 'organizationId', 'contactInternalName'], applicationToSave);
    const isNew = !!applicationToSave.isNew;

    return axios[isNew ? 'post' : 'put'](getApplicationsUrl(), payload)
      .then(({ data }) => ({ isNew, application: data }))
      .catch(rejectWithValue);
  }
);

const updateApplicationFulfilled = (state, { payload }) => {
  const { isNew, application } = payload;

  if (isNew) {
    state.applications.push(application);
  } else {
    const index = findIndex(propEq('id', application.id), state.applications);
    state.applications[index] = application;
  }
};

const removeApplication = createAsyncThunk(
  `${REDUCER_NAME}/removeApplication`,
  (applicationToDelete, { dispatch, rejectWithValue }) => {
    return axios
      .delete(getApplicationsUrl() + `/${applicationToDelete.publicId}`)
      .then(() => {
        dispatch(actions.resetDeleteModalState());
        return applicationToDelete.id;
      })
      .catch(rejectWithValue);
  }
);

const removeApplicationFulfilled = (state, { payload }) => {
  state.deleteModal.success = true;
  state.deleteModal.deleting = null;
  state.deleteModal.errorState = null;
  state.applications = reject(propEq('id', payload))(state.applications);
};

const removeApplicationFailed = (state, { payload }) => {
  state.deleteModal.deleting = false;
  state.deleteModal.errorState = Messages.getHttpErrorMessage(payload);
};

const resetDeleteModalState = (state) => {
  state.deleteModal.deleting = null;
  state.deleteModal.success = null;
  state.deleteModal.errorState = null;
};

const moveApplication = ({ applicationId, organizationId }) => {
  return (dispatch) => {
    return axios
      .post(getMoveApplicationUrl(applicationId, organizationId))
      .then((response) => {
        return dispatch(actions.loadApplications(true)).then(() => {
          return response?.data?.warnings;
        });
      })
      .catch((error) => {
        if (error.response.status === 409 && error.response.data?.errors?.length) {
          // data.errors is an array of incompatibilities
          return Promise.reject({
            message: moveApplicationErrorMessages.ERROR_INCOMPATIBLE_DESTINATION,
            incompatibilities: error.response.data.errors,
          });
        }
        return Promise.reject(error);
      });
  };
};

const applicationsSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    resetDeleteModalState,
  },
  extraReducers: {
    [loadApplications.pending]: loadApplicationsRequested,
    [loadApplications.fulfilled]: loadApplicationsFulfilled,
    [loadApplications.rejected]: loadApplicationsFailed,

    [updateApplication.fulfilled]: updateApplicationFulfilled,

    [removeApplication.pending]: pathSetConst(['deleteModal', 'deleting'], true),
    [removeApplication.fulfilled]: removeApplicationFulfilled,
    [removeApplication.rejected]: removeApplicationFailed,
  },
});

export const actions = {
  ...applicationsSlice.actions,
  loadApplications,
  updateApplication,
  removeApplication,
  moveApplication,
};

export default applicationsSlice.reducer;
