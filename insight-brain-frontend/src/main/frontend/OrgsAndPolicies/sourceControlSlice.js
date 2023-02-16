/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { prop } from 'ramda';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { selectSelectedOwnerTypeAndId } from './orgsAndPoliciesSelectors';
import { getCompositeSourceControlUrl } from 'MainRoot/util/CLMLocation';

const REDUCER_NAME = 'sourceControl';

export const initialState = {
  loading: false,
  loadError: null,
  data: null,
};

const loadSourceControlRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadSourceControlFulfilled = (state, { payload }) => {
  state.loading = false;
  state.data = payload;
};

const loadSourceControlFailed = (state, { payload }) => {
  state.data = null;
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const loadSourceControl = createAsyncThunk(`${REDUCER_NAME}/loadSourceControl`, (_, { getState, rejectWithValue }) => {
  const state = getState();
  const { ownerType, ownerId } = selectSelectedOwnerTypeAndId(state);

  return axios.get(getCompositeSourceControlUrl(ownerType, ownerId)).then(prop('data')).catch(rejectWithValue);
});

const sourceControlSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {},
  extraReducers: {
    [loadSourceControl.pending]: loadSourceControlRequested,
    [loadSourceControl.fulfilled]: loadSourceControlFulfilled,
    [loadSourceControl.rejected]: loadSourceControlFailed,
  },
});

export const actions = {
  ...sourceControlSlice.actions,
  loadSourceControl,
};

export default sourceControlSlice.reducer;
