/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { getLicensedSolutionsUrl } from '../../../util/CLMLocation';

const REDUCER_NAME = 'solutionSwitcher';

export const initialState = {
  licensedSolutions: [],
  isFetched: false,
  loading: false,
  loadError: null,
};

const fetchLicensedSolutionsFulfilled = (state, { payload }) => {
  state.loading = false;
  state.loadError = null;
  state.licensedSolutions = payload;
};

const fetchLicensedSolutionsPending = (state) => {
  state.loading = true;
  state.loadError = null;
};

const fetchLicensedSolutionsRejected = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const fetchLicensedSolutions = createAsyncThunk(`${REDUCER_NAME}/fetchLicensedSolutions`, (_, { rejectWithValue }) => {
  return axios
    .get(getLicensedSolutionsUrl())
    .then(({ data }) => data)
    .catch(rejectWithValue);
});

const solutionSwitcherSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  extraReducers: {
    [fetchLicensedSolutions.fulfilled]: fetchLicensedSolutionsFulfilled,
    [fetchLicensedSolutions.pending]: fetchLicensedSolutionsPending,
    [fetchLicensedSolutions.rejected]: fetchLicensedSolutionsRejected,
  },
});

export default solutionSwitcherSlice.reducer;
export const actions = {
  ...solutionSwitcherSlice.actions,
  fetchLicensedSolutions,
};
