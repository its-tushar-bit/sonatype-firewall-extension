/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { pathSet } from '../../util/jsUtil';

import { getVulnerabilitiesUrl, getVulnerabilityJsonDetailUrl } from '../../util/CLMLocation';
import { Messages } from '../../util/CommonServices';
import { selectVersionExplorerRequestData } from '../overview/overviewSelectors';
import { selectVulnerabityRefId } from './vulnerabilitiesSelectors';

const REDUCER_NAME = 'componentDetailsVulnerabilities';

const initialState = {
  vulnerabilities: {
    data: null,
    loading: false,
    error: null,
  },
  showVulnerabilityDetailPopover: false,
  selectedRefId: null,
  vulnerabilityDetails: {
    loading: false,
    error: null,
    details: null,
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

const loadVulnerabilityDetails = createAsyncThunk(
  `${REDUCER_NAME}/loadVulnerabilityDetails`,
  (_, { getState, rejectWithValue }) => {
    const refId = selectVulnerabityRefId(getState());
    return axios
      .get(getVulnerabilityJsonDetailUrl(refId))
      .then(({ data }) => data)
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

function loadVulnerabilityDetailsFulfilled(state, { payload }) {
  state.vulnerabilityDetails.loading = false;
  state.vulnerabilityDetails.error = null;
  state.vulnerabilityDetails.details = payload;
}

function loadVulnerabilityDetailsFailed(state, { payload }) {
  state.vulnerabilityDetails.loading = false;
  state.vulnerabilityDetails.error = Messages.getHttpErrorMessage(payload);
}

function setVulnerabilityIdAndToggleVisibility(state, { payload }) {
  state.selectedRefId = payload;
  state.showVulnerabilityDetailPopover = !state.showVulnerabilityDetailPopover;
}

const componentDetailsVulnerabilitiesSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setVulnerabilityIdAndToggleVisibility,
  },
  extraReducers: {
    [loadVulnerabilities.pending]: pathSet(['vulnerabilities', 'loading'], true),
    [loadVulnerabilities.fulfilled]: loadVulnerabilitiesFulfilled,
    [loadVulnerabilities.rejected]: loadVulnerabilitiesFailed,

    [loadVulnerabilityDetails.pending]: pathSet(['vulnerabilityDetails', 'loading'], true),
    [loadVulnerabilityDetails.fulfilled]: loadVulnerabilityDetailsFulfilled,
    [loadVulnerabilityDetails.rejected]: loadVulnerabilityDetailsFailed,
  },
});

export default componentDetailsVulnerabilitiesSlice.reducer;
export const actions = {
  ...componentDetailsVulnerabilitiesSlice.actions,
  loadVulnerabilities,
  loadVulnerabilityDetails,
};
