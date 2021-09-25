/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { pathSet } from '../../util/jsUtil';

import { getVulnerabilitiesUrl } from '../../util/CLMLocation';
import { Messages } from '../../util/CommonServices';
import { selectVersionExplorerRequestData } from '../overview/overviewSelectors';

const REDUCER_NAME = 'componentDetailsVulnerabilities';

const initialState = {
  vulnerabilities: {
    data: null,
    loading: false,
    error: null,
  },
};
const loadVulnerabilities = createAsyncThunk(
  `${REDUCER_NAME}/loadVulnerabilities`,
  (_, { getState, rejectWithValue }) => {
    return axios
      .get(getVulnerabilitiesUrl(selectVersionExplorerRequestData(getState())))
      .then((result) => result)
      .catch(rejectWithValue);
  }
);

const loadVulnerabilitiesFulfilled = (state, { payload }) => {
  state.vulnerabilities = {
    data: payload.data.securityVulnerabilities,
    loading: false,
    error: null,
  };
};

function loadVulnerabilitiesFailed(state, { payload }) {
  state.vulnerabilities.loading = false;
  state.vulnerabilities.error = Messages.getHttpErrorMessage(payload);
}

const componentDetailsVulnerabilitiesSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {},
  extraReducers: {
    [loadVulnerabilities.pending]: pathSet(['vulnerabilities', 'loading'], true),
    [loadVulnerabilities.fulfilled]: loadVulnerabilitiesFulfilled,
    [loadVulnerabilities.rejected]: loadVulnerabilitiesFailed,
  },
});

export default componentDetailsVulnerabilitiesSlice.reducer;
export const actions = {
  ...componentDetailsVulnerabilitiesSlice.actions,
  loadVulnerabilities,
};
