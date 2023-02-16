/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { findIndex, isEmpty, prop, propEq, reject } from 'ramda';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';

import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { getApplicationsUrl, getApplicationSummaryUrl, getApplicationUrl } from '../util/CLMLocation';
import { selectApplications } from './applicationsSelectors';

const REDUCER_NAME = 'applications';

export const initialState = {
  loadingApplications: false,
  loadApplicationsError: null,
  applications: [],
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

const loadApplicationById = createAsyncThunk(
  `${REDUCER_NAME}/loadApplicationById`,
  async (publicId, { rejectWithValue }) =>
    axios.get(getApplicationUrl(publicId)).then(prop('data')).catch(rejectWithValue)
);

const loadApplicationSummary = createAsyncThunk(
  `${REDUCER_NAME}/loadApplicationSummary`,
  async (id, { rejectWithValue }) => axios.get(getApplicationSummaryUrl(id)).then(prop('data')).catch(rejectWithValue)
);

const updateApplication = (state, { payload }) => {
  const { isNew, application } = payload;
  if (isNew) {
    state.applications.push(application);
  } else {
    const index = findIndex(propEq('id', application.id), state.applications);
    state.applications[index] = application;
  }
};

const removeApplicationFromList = (state, { payload }) => {
  state.applications = reject(propEq('id', payload))(state.applications);
};

const applicationsSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    removeApplicationFromList,
    updateApplication,
  },
  extraReducers: {
    [loadApplications.pending]: loadApplicationsRequested,
    [loadApplications.fulfilled]: loadApplicationsFulfilled,
    [loadApplications.rejected]: loadApplicationsFailed,
  },
});

export const actions = {
  ...applicationsSlice.actions,
  loadApplications,
  loadApplicationById,
  loadApplicationSummary,
};

export default applicationsSlice.reducer;
