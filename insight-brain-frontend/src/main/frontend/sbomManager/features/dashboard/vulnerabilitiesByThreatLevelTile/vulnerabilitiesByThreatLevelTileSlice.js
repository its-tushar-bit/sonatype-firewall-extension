/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { always, pick } from 'ramda';

import { UI_ROUTER_ON_FINISH } from 'MainRoot/reduxUiRouter/routerActions';
import { getVulnerabilitesByThreatLevelUrl } from 'MainRoot/util/CLMLocation';

const REDUCER_NAME = 'vulnerabilitiesByThreatLevelTile';

const vulnerabilitesInitialState = Object.freeze({
  critical: {
    unannotated: null,
    annotated: null,
    total: null,
  },
  high: {
    unannotated: null,
    annotated: null,
    total: null,
  },
  medium: {
    unannotated: null,
    annotated: null,
    total: null,
  },
  low: {
    unannotated: null,
    annotated: null,
    total: null,
  },
});

const vulnerabiltiesTotalInitialState = Object.freeze({
  totalVulnerabilities: null,
  totalVulnerabilitiesAnnotated: null,
  totalVulnerabilitiesUnannotated: null,
});

export const initialState = Object.freeze({
  loading: true,
  loadError: null,
  vulnerabilities: { ...vulnerabilitesInitialState },
  vulnerabilitiesTotal: { ...vulnerabiltiesTotalInitialState },
});

const loadVulnerabilitesByThreatLevelRequested = (state) => {
  state.loading = true;
  state.loadError = null;
  state.vulnerabilities = { ...vulnerabilitesInitialState };
  state.vulnerabilitiesTotal = { ...vulnerabiltiesTotalInitialState };
};

const loadVulnerabilitesByThreatLevelFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = payload;
  state.vulnerabilities = { ...vulnerabilitesInitialState };
  state.vulnerabilitiesTotal = { ...vulnerabiltiesTotalInitialState };
};

const loadVulnerabilitesByThreatLevelFulfilled = (state, { payload }) => {
  state.loading = false;
  state.loadError = null;

  state.vulnerabilities = {
    critical: {
      annotated: payload.criticalAnnotated,
      unannotated: payload.criticalUnannotated,
      total: payload.critical,
    },
    high: {
      annotated: payload.highAnnotated,
      unannotated: payload.highUnannotated,
      total: payload.high,
    },
    medium: {
      annotated: payload.mediumAnnotated,
      unannotated: payload.mediumUnannotated,
      total: payload.medium,
    },
    low: {
      annotated: payload.lowAnnotated,
      unannotated: payload.lowUnannotated,
      total: payload.low,
    },
  };

  state.vulnerabilitiesTotal = pick(
    ['totalVulnerabilities', 'totalVulnerabilitiesAnnotated', 'totalVulnerabilitiesUnannotated'],
    payload
  );
};

const loadVulnerabilitesByThreatLevel = createAsyncThunk(
  `${REDUCER_NAME}/loadVulnerabilitesByThreatLevel`,
  async (_, { rejectWithValue }) =>
    axios
      .get(getVulnerabilitesByThreatLevelUrl())
      .then((response) => response.data)
      .catch((err) => rejectWithValue(err))
);

const vulnerabilitiesByThreatLevelTileSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {},
  extraReducers: {
    [loadVulnerabilitesByThreatLevel.pending]: loadVulnerabilitesByThreatLevelRequested,
    [loadVulnerabilitesByThreatLevel.fulfilled]: loadVulnerabilitesByThreatLevelFulfilled,
    [loadVulnerabilitesByThreatLevel.rejected]: loadVulnerabilitesByThreatLevelFailed,
    [UI_ROUTER_ON_FINISH]: always(initialState),
  },
});

export const actions = {
  ...vulnerabilitiesByThreatLevelTileSlice.actions,
  loadVulnerabilitesByThreatLevel,
};

export default vulnerabilitiesByThreatLevelTileSlice.reducer;
