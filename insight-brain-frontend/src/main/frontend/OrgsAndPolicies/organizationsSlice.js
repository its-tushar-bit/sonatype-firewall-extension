/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { isEmpty, findIndex, prop, propEq, reject } from 'ramda';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';

import { getOrganizationsUrl } from '../util/CLMLocation';
import { selectOrganizations } from './organizationsSelectors';

const REDUCER_NAME = 'organizations';

export const initialState = {
  organizations: [],
};

const loadOrganizationsFulfilled = (state, { payload }) => {
  state.organizations = payload;
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

const organizationsSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    removeOrganizationFromList,
    updateOrganization,
  },
  extraReducers: {
    [loadOrganizations.fulfilled]: loadOrganizationsFulfilled,
  },
});

export const actions = {
  ...organizationsSlice.actions,
  loadOrganizations,
};

export default organizationsSlice.reducer;
