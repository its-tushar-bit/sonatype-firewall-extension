/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { fromPairs, keys, map, pair, path, __ } from 'ramda';

import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { getDashboardStageUrl, getActionStageUrl, getCliStageUrl, getSbomStageUrl } from 'MainRoot/util/CLMLocation';

const REDUCER_NAME = 'stages';

export const urlsByPurpose = {
  dashboard: getDashboardStageUrl(),
  action: getActionStageUrl(),
  cli: getCliStageUrl(),
  sbom: getSbomStageUrl(),
};
const initialPurposeState = Object.freeze({
  loading: false,
  error: null,
  stageTypes: null,
});
export const validPurposes = keys(urlsByPurpose);
export const initialState = Object.freeze(fromPairs(map(pair(__, initialPurposeState), validPurposes)));

const getAlreadyLoadedStageTypes = (state, purpose) => path(['stages', purpose, 'stageTypes'], state);
const getShortName = ({ stageTypeId, stageName }) => (stageTypeId === 'stage-release' ? 'Stage' : stageName);
const addShortName = (stageType) => Object.freeze({ ...stageType, shortName: getShortName(stageType) });

export const loadStageTypes = createAsyncThunk(
  `${REDUCER_NAME}/loadStageTypes`,
  (purpose, { rejectWithValue, getState }) => {
    if (!validPurposes.includes(purpose)) return rejectWithValue(`purpose must be one of ${validPurposes.join(', ')}`);

    const alreadyLoadedStageTypes = getAlreadyLoadedStageTypes(getState(), purpose);
    if (alreadyLoadedStageTypes) return Promise.resolve({ purpose, data: alreadyLoadedStageTypes });
    return axios
      .get(urlsByPurpose[purpose])
      .then(({ data }) => ({ purpose, data: purpose === 'cli' ? data : map(addShortName, data) }))
      .catch(rejectWithValue);
  }
);

const loadStageTypesRequested = (state, { meta }) => {
  state[meta.arg].loading = true;
};

const loadStageTypesFulfilled = (state, { payload }) => {
  const { purpose, data } = payload;

  state[purpose] = { ...initialPurposeState, stageTypes: Object.freeze(data) };
};

const loadStageTypesFailed = (state, { payload, meta }) => {
  state[meta.arg] = { ...initialPurposeState, error: Messages.getHttpErrorMessage(payload) };
};

const stagesSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {},
  extraReducers: {
    [loadStageTypes.pending]: loadStageTypesRequested,
    [loadStageTypes.fulfilled]: loadStageTypesFulfilled,
    [loadStageTypes.rejected]: loadStageTypesFailed,
  },
});

export const loadCliStages = () => loadStageTypes('cli');
export const loadActionStages = () => loadStageTypes('action');
export const loadDashboardStages = () => loadStageTypes('dashboard');
export const loadSbomStages = () => loadStageTypes('sbom');

export default stagesSlice.reducer;
export const actions = {
  loadCliStages,
  loadActionStages,
  loadDashboardStages,
  loadSbomStages,
};
