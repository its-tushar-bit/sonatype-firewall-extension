/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk } from '@reduxjs/toolkit';
import createSlice from 'MainRoot/reduxConfig/createSlice';
import axios from 'axios';
import { Messages } from 'MainRoot/util/CommonServices';
import { getFirewallWaiverDetailsUrl, getWaiverDetailsUrl } from 'MainRoot/util/CLMLocation';
import { prop } from 'ramda';
import { selectIsStandaloneFirewall, selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
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
  const state = getState();
  const { ownerType: ownerTypeRaw, ownerId, waiverId } = selectRouterCurrentParams(state);
  const ownerType = mapWaiverOwnerType[ownerTypeRaw] || ownerTypeRaw;
  // On Firewall routes, use the Firewall-scoped detail endpoint, which authorizes callers
  // based on their permitted proxy repositories rather than owner-level READ. Scoped users
  // (e.g. READ on a single Docker repo) can then open the waiver detail without a 403.
  const detailsUrl = selectIsStandaloneFirewall(state)
    ? getFirewallWaiverDetailsUrl(ownerType, ownerId, waiverId)
    : getWaiverDetailsUrl(ownerType, ownerId, waiverId);
  try {
    const waiverDetails = await axios.get(detailsUrl).then(prop('data'));
    const isGlobalScope = ownerTypeRaw === 'root_organization' || ownerTypeRaw === 'all_repositories';
    const permissionCheck = isGlobalScope
      ? checkPermissions(['WAIVE_POLICY_VIOLATIONS'])
      : checkPermissions(['WAIVE_POLICY_VIOLATIONS'], ownerTypeRaw, ownerId);
    const hasWaivePermission = await permissionCheck.then(() => true).catch(() => false);
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
