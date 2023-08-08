/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { path } from 'ramda';

import { propSet } from 'MainRoot/util/reduxToolkitUtil';
import { getApplicablePolicies } from '../util/CLMLocation';
import { selectOwnerProperties } from './orgsAndPoliciesSelectors';
import { selectCurrentRouteName } from 'MainRoot/reduxUiRouter/routerSelectors';

const REDUCER_NAME = 'orgsAndPolicies';

export const initialState = {
  selectedOwner: {},
  policiesByOwner: null,
};

const setSelectedOwner = (state, { payload }) => {
  state.selectedOwner = payload;
};

const setSelectedOwnerContact = (state, { payload }) => {
  state.selectedOwner.contact = payload;
};

const selectedOwnerParentOrganizationUpdated = (
  state,
  { payload: { organizationName, organizationId, parentOrganizationId } }
) => {
  state.selectedOwner.organizationName = organizationName;
  state.selectedOwner.organizationId = organizationId;
  state.selectedOwner.parentOrganizationId = parentOrganizationId;
};

const loadApplicablePoliciesByOwner = createAsyncThunk(
  `${REDUCER_NAME}/loadApplicablePoliciesByOwner`,
  (_, { getState, rejectWithValue }) => {
    let ownerType, ownerId;
    const currentRouteName = selectCurrentRouteName(getState());
    if (currentRouteName === 'management.view.repository_container') {
      ownerType = 'repository_container';
      ownerId = 'REPOSITORY_CONTAINER_ID';
    } else {
      const ownerProperties = selectOwnerProperties(getState());
      ownerType = ownerProperties.ownerType;
      ownerId = ownerProperties.ownerId;
    }
    return axios
      .get(getApplicablePolicies(ownerType, ownerId))
      .then(path(['data', 'policiesByOwner']))
      .catch(rejectWithValue);
  }
);

const rootSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setSelectedOwner,
    setSelectedOwnerContact,
    selectedOwnerParentOrganizationUpdated,
  },
  extraReducers: {
    [loadApplicablePoliciesByOwner.fulfilled]: propSet('policiesByOwner'),
  },
});

export const actions = {
  ...rootSlice.actions,
  loadApplicablePoliciesByOwner,
};

export default rootSlice.reducer;
