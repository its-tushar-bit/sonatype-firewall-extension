/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import axios from 'axios';
import { head as first, prop } from 'ramda';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { next, prev, steps } from './firewallOnboardingUtils';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { getUnconfiguredRepositoriesManager } from 'MainRoot/util/CLMLocation';

export const REDUCER_NAME = 'firewallOnboarding';

export const initialState = {
  loading: false,
  currentStep: first(steps),
  selectedRepositories: [],
  repositories: {
    loading: false,
    loadError: null,
    proxy: [],
    hosted: [],
  },
  unconfiguredRepoManagers: {
    repoManagers: [],
    loading: false,
    loadError: null,
  },
};

const continueToNextStep = (state) => {
  if (next(state.currentStep)) {
    state.currentStep = next(state.currentStep);
  }
};

const goBackToPreviousStep = (state) => {
  if (prev(state.currentStep)) {
    state.currentStep = prev(state.currentStep);
  }
};

const loadUnconfiguredRepoManagers = createAsyncThunk(
  `${REDUCER_NAME}/loadUnconfiguredRepoManagers`,
  (_, { rejectWithValue }) => {
    return axios.get(getUnconfiguredRepositoriesManager()).then(prop('data')).catch(rejectWithValue);
  }
);

const loadUnconfiguredRepoManagersRequested = (state) => ({
  ...state,
  unconfiguredRepoManagers: {
    ...initialState.unconfiguredRepoManagers,
    loading: true,
  },
});

const loadUnconfiguredRepoManagersFulfilled = (state, { payload }) => ({
  ...state,
  unconfiguredRepoManagers: {
    repoManagers: payload,
    loading: false,
    loadError: null,
  },
});

const loadUnconfiguredRepoManagersFailed = (state, { payload }) => ({
  ...state,
  unconfiguredRepoManagers: {
    ...state.unconfiguredRepoManagers,
    loading: false,
    loadError: Messages.getHttpErrorMessage(payload),
  },
});

const firewallOnboardingSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    continueToNextStep,
    goBackToPreviousStep,
  },
  extraReducers: {
    [loadUnconfiguredRepoManagers.pending]: loadUnconfiguredRepoManagersRequested,
    [loadUnconfiguredRepoManagers.fulfilled]: loadUnconfiguredRepoManagersFulfilled,
    [loadUnconfiguredRepoManagers.rejected]: loadUnconfiguredRepoManagersFailed,
  },
});

export const actions = {
  ...firewallOnboardingSlice.actions,
  loadUnconfiguredRepoManagers,
};

export default firewallOnboardingSlice.reducer;
