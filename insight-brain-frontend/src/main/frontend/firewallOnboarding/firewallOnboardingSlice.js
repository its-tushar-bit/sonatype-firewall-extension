/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import axios from 'axios';
import { head as first, path } from 'ramda';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { next, prev, steps, updateRepositories } from './firewallOnboardingUtils';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import {
  getUnconfiguredRepositoriesManager,
  getRepositoryListUrl,
  getSupportedRepositoriesFormat,
  getConfigureRepositoriesUrl,
  getConfigureFirewallOnboardingUrl,
} from 'MainRoot/util/CLMLocation';
import {
  selectProtectionRules,
  selectRepositoriesList,
  selectUnconfiguredRepoManager,
} from './firewallOnboardingSelectors';

export const REDUCER_NAME = 'firewallOnboarding';

/** @typedef {import('./types').FirewallOnboardingState} FirewallOnboardingState */

/** @type FirewallOnboardingState  */
export const initialState = {
  loading: false,
  isConfiguring: true,
  currentStep: first(steps),
  showWelcomeScreen: true,
  supportedFormats: [],
  repositories: {
    loading: false,
    loadError: null,
    saving: false,
    saveError: null,
    list: null,
  },
  unconfiguredRepoManagers: {
    repoManagers: [],
    loading: false,
    loadError: null,
  },
  protectionRules: {
    supplyChainAttacksProtectionEnabled: false,
    namespaceConfusionProtectionEnabled: false,
    configuring: false,
    configureError: null,
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

const hideWelcomeScreen = (state) => ({
  ...state,
  showWelcomeScreen: false,
});

const configureRepositories = (/** @type FirewallOnboardingState */ state, { payload: repositories }) => {
  state.repositories.list = updateRepositories(state.repositories.list, repositories);
};

const loadUnconfiguredRepoManagers = createAsyncThunk(
  `${REDUCER_NAME}/loadUnconfiguredRepoManagers`,
  (_, { dispatch, rejectWithValue, getState }) => {
    // Only call the endpoint if
    // unconfigured repositories list is not populated.
    if (path([REDUCER_NAME, 'repositories', 'list'], getState())) {
      return;
    }
    return axios
      .get(getUnconfiguredRepositoriesManager())
      .then(({ data }) => {
        dispatch(actions.loadRepositories(data[0]));
        return data;
      })
      .catch(rejectWithValue);
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

const loadRepositories = createAsyncThunk(
  `${REDUCER_NAME}/loadRepositories`,
  (unconfiguredRepoManager, { rejectWithValue }) => {
    if (!unconfiguredRepoManager) {
      const errorMessage = 'There is no unconfigured repository manager selected';
      return rejectWithValue(errorMessage);
    }

    const promises = [
      axios.get(getRepositoryListUrl(unconfiguredRepoManager.id)),
      axios.get(getSupportedRepositoriesFormat()),
    ];
    return Promise.all(promises)
      .then(([repositories, formats]) => ({
        repositories: repositories.data,
        supportedFormats: formats.data?.regexpsByRepositoryFormat || {},
      }))
      .catch(rejectWithValue);
  }
);

const loadRepositoriesRequested = (state) => ({
  ...state,
  repositories: {
    ...state.repositories,
    loading: true,
    loadError: null,
  },
});

const loadRepositoriesFulfilled = (state, { payload: { repositories, supportedFormats } }) => {
  return {
    ...state,
    supportedFormats: Object.keys(supportedFormats),
    repositories: {
      ...state.repositories,
      list: repositories.filter((repo) => repo.format != null),
      loading: false,
      loadError: null,
    },
  };
};

const loadRepositoriesFailed = (state, { payload }) => {
  return {
    ...state,
    supportedFormats: [],
    repositories: {
      ...state.repositories,
      loading: false,
      loadError: Messages.getHttpErrorMessage(payload),
    },
  };
};

const saveRepositories = createAsyncThunk(`${REDUCER_NAME}/saveRepositories`, (_, { getState, rejectWithValue }) => {
  const repoManager = selectUnconfiguredRepoManager(getState());
  const repositories = selectRepositoriesList(getState());
  return axios.put(getConfigureRepositoriesUrl(repoManager.id), repositories).catch(rejectWithValue);
});

const saveRepositoriesRequested = (state) => ({
  ...state,
  repositories: {
    ...state.repositories,
    saving: true,
    saveError: null,
  },
});

const saveRepositoriesFulfilled = (state) => ({
  ...state,
  repositories: {
    ...state.repositories,
    saving: false,
    saveError: null,
  },
});

const saveRepositoriesFailed = (state, { payload }) => ({
  ...state,
  repositories: {
    ...state.repositories,
    saving: false,
    saveError: Messages.getHttpErrorMessage(payload),
  },
});

const toggleProtectionRule = (state, { payload }) => ({
  ...state,
  protectionRules: {
    ...state.protectionRules,
    [payload]: !state.protectionRules[payload],
  },
});

const configureProtectionRules = createAsyncThunk(
  `${REDUCER_NAME}/configureProtectionRules`,
  (_, { getState, rejectWithValue }) => {
    const protectionRules = selectProtectionRules(getState());
    return axios.put(getConfigureFirewallOnboardingUrl(), { ...protectionRules }).catch(rejectWithValue);
  }
);

const configureProtectionRulesRequested = (state) => ({
  ...state,
  protectionRules: {
    ...state.protectionRules,
    configuring: true,
    configureError: null,
  },
});

const configureProtectionRulesFulfilled = (state) => ({
  ...state,
  protectionRules: {
    ...state.protectionRules,
    configuring: false,
    configureError: null,
  },
});

const configureProtectionRulesFailed = (state, { payload }) => ({
  ...state,
  protectionRules: {
    ...state.protectionRules,
    configuring: false,
    configureError: Messages.getHttpErrorMessage(payload),
  },
});

const finishConfiguration = (state) => ({
  ...state,
  isConfiguring: false,
});

const firewallOnboardingSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    continueToNextStep,
    goBackToPreviousStep,
    hideWelcomeScreen,
    configureRepositories,
    toggleProtectionRule,
    finishConfiguration,
  },
  extraReducers: {
    [loadUnconfiguredRepoManagers.pending]: loadUnconfiguredRepoManagersRequested,
    [loadUnconfiguredRepoManagers.fulfilled]: loadUnconfiguredRepoManagersFulfilled,
    [loadUnconfiguredRepoManagers.rejected]: loadUnconfiguredRepoManagersFailed,
    [loadRepositories.pending]: loadRepositoriesRequested,
    [loadRepositories.fulfilled]: loadRepositoriesFulfilled,
    [loadRepositories.rejected]: loadRepositoriesFailed,
    [saveRepositories.pending]: saveRepositoriesRequested,
    [saveRepositories.fulfilled]: saveRepositoriesFulfilled,
    [saveRepositories.rejected]: saveRepositoriesFailed,
    [configureProtectionRules.pending]: configureProtectionRulesRequested,
    [configureProtectionRules.fulfilled]: configureProtectionRulesFulfilled,
    [configureProtectionRules.rejected]: configureProtectionRulesFailed,
  },
});

export const actions = {
  ...firewallOnboardingSlice.actions,
  loadUnconfiguredRepoManagers,
  loadRepositories,
  saveRepositories,
  configureProtectionRules,
};

export default firewallOnboardingSlice.reducer;
