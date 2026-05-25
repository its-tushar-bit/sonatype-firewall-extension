/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk } from '@reduxjs/toolkit';
import createSlice from 'MainRoot/reduxConfig/createSlice';
import axios from 'axios';
import { Messages } from 'MainRoot/util/CommonServices';
import { getWaiverDetailsUrl } from 'MainRoot/util/CLMLocation';
import { prop } from 'ramda';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { checkPermissions } from 'MainRoot/util/authorizationUtil';

const REDUCER_NAME = 'waiverDetails';

export const initialState = Object.freeze({
  loading: false,
  loadError: null,
  waiverDetails: null,
  hasWaivePermission: false,
});

const mapWaiverOwnerType = {
  all_repositories: 'repository_container',
  root_organization: 'organization',
};

// Axios request to get waiver details
const loadWaiver = createAsyncThunk(`${REDUCER_NAME}/loadWaiver`, async (_, { getState, rejectWithValue }) => {
  const { ownerType: ownerTypeRaw, ownerId, waiverId } = selectRouterCurrentParams(getState());
  const ownerType = mapWaiverOwnerType[ownerTypeRaw] || ownerTypeRaw;
  try {
    const waiverDetails = await axios.get(getWaiverDetailsUrl(ownerType, ownerId, waiverId)).then(prop('data'));
    const isRepositoryScope = ownerTypeRaw !== 'root_organization' && ownerTypeRaw !== 'all_repositories';
    const permOwnerType = isRepositoryScope ? 'repository' : 'global';
    const permOwnerId = isRepositoryScope ? ownerId : 'global';
    const hasWaivePermission = await checkPermissions(['WAIVE_POLICY_VIOLATIONS'], permOwnerType, permOwnerId)
      .then(() => true)
      .catch(() => false);
    return { waiverDetails, hasWaivePermission };
  } catch (error) {
    return rejectWithValue(error);
  }
});

const loadWaiverRequested = (state) => ({
  ...state,
  loading: true,
  loadError: null,
});

const loadWaiverFulfilled = (state, { payload }) => ({
  ...state,
  loading: false,
  loadError: null,
  waiverDetails: payload.waiverDetails,
  hasWaivePermission: payload.hasWaivePermission,
});

const loadWaiverFailed = (state, { payload }) => ({
  ...state,
  loading: false,
  loadError: Messages.getHttpErrorMessage(payload),
});

const waiverDetailsSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {},
  extraReducers: {
    [loadWaiver.pending]: loadWaiverRequested,
    [loadWaiver.fulfilled]: loadWaiverFulfilled,
    [loadWaiver.rejected]: loadWaiverFailed,
  },
});

export default waiverDetailsSlice.reducer;
export const actions = {
  ...waiverDetailsSlice.actions,
  loadWaiver,
};
