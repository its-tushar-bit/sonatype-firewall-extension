/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { isEmpty, findIndex, prop, propEq, reject } from 'ramda';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';

import { getOrganizationsUrl, getOrganizationUrl } from '../util/CLMLocation';
import { selectOrganizations } from './organizationsSelectors';
import { Messages } from 'MainRoot/utilAngular/CommonServices';

const REDUCER_NAME = 'organizations';

export const initialState = {
  organizations: [],
  loading: false,
  loadError: null,
};

const loadOrganizationsRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadOrganizationsFulfilled = (state, { payload }) => {
  state.loading = false;
  state.organizations = payload;
};

const loadOrganizationsRejected = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const updateOrganization = (state, { payload }) => {
  const { isNew, organization } = payload;

  if (isNew) {
    state.organizations.push(organization);
  } else {
    const index = findIndex(propEq('id', organization.id), state.organizations);
    state.organizations[index] = organization;
  }
};

const removeOrganizationFromList = (state, { payload }) => {
  state.organizations = reject(propEq('id', payload))(state.organizations);
};

const loadOrganizations = createAsyncThunk(
  `${REDUCER_NAME}/loadOrganizations`,
  (forceReload, { rejectWithValue, getState }) => {
    const state = getState();
    const organizations = selectOrganizations(state);

    if (isEmpty(organizations) || forceReload) {
      return axios.get(getOrganizationsUrl()).then(prop('data')).catch(rejectWithValue);
    }

    return Promise.resolve(organizations);
  }
);

const loadOrganizationById = createAsyncThunk(`${REDUCER_NAME}/loadOrganizationById`, async (id, { rejectWithValue }) =>
  axios.get(getOrganizationUrl(id)).then(prop('data')).catch(rejectWithValue)
);

const organizationsSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    removeOrganizationFromList,
    updateOrganization,
  },
  extraReducers: {
    [loadOrganizations.pending]: loadOrganizationsRequested,
    [loadOrganizations.fulfilled]: loadOrganizationsFulfilled,
    [loadOrganizations.rejected]: loadOrganizationsRejected,
  },
});

export const actions = {
  ...organizationsSlice.actions,
  loadOrganizations,
  loadOrganizationById,
};

export default organizationsSlice.reducer;
