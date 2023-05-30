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
import { getConfigureRepositoriesUrl, getUnconfiguredRepositoriesManager } from 'MainRoot/util/CLMLocation';
import {
  selectRepositoriesList,
  selectUnconfiguredRepoManager,
} from 'MainRoot/firewallOnboarding/firewallOnboardingSelectors';

export const REDUCER_NAME = 'firewallOnboarding';

export const initialState = {
  loading: false,
  currentStep: first(steps),
  selectedRepositories: [],
  repositories: {
    loading: false,
    loadError: null,
    list: [
      {
        id: 'id',
        repositoryManagerId: 'repoManagerId',
        publicId: 'publicId',
        repositoryType: 'proxy',
        auditEnabled: true,
        quarantineEnabled: true,
        policyCompliantComponentSelectionEnabled: false,
        namespaceConfusionProtectionEnabled: false,
        format: 'maven',
      },
    ],
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

const saveRepositoriesRequested = (state) => ({
  ...state,
  repositories: {
    ...state.repositories,
    loading: true,
    loadError: null,
  },
});

const saveRepositoriesFulfilled = (state) => ({
  ...state,
  repositories: {
    ...state.repositories,
    loading: false,
    loadError: null,
  },
});

const saveRepositoriesFailed = (state, { payload }) => ({
  ...state,
  repositories: {
    ...state.repositories,
    loading: false,
    loadError: Messages.getHttpErrorMessage(payload),
  },
});

const saveRepositories = createAsyncThunk(`${REDUCER_NAME}/saveRepositories`, (_, { getState, rejectWithValue }) => {
  const repoManager = selectUnconfiguredRepoManager(getState());
  const repositories = selectRepositoriesList(getState());
  return axios
    .put(getConfigureRepositoriesUrl(repoManager.id), repositories)
    .then(console.log('success!'))
    .catch(rejectWithValue);
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
    [saveRepositories.pending]: saveRepositoriesRequested,
    [saveRepositories.fulfilled]: saveRepositoriesFulfilled,
    [saveRepositories.rejected]: saveRepositoriesFailed,
  },
});

export const actions = {
  ...firewallOnboardingSlice.actions,
  loadUnconfiguredRepoManagers,
  saveRepositories,
};

export default firewallOnboardingSlice.reducer;
