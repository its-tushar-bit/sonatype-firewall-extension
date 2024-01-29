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
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { setLeftNavigationOpen } from 'MainRoot/util/preferenceStore';
import { setShowWelcomeModalToTrueInStore } from 'MainRoot/firewall/firewallWelcomeModalStore';
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
  launchFirewall: {
    saving: false,
    saveError: null,
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
      .then(([repositoriesDto, formats]) => ({
        repositories: repositoriesDto.data.repositories.map((item) => item.repository),
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

const launchFirewall = createAsyncThunk(
  `${REDUCER_NAME}/launchFirewall`,
  (_, { dispatch, getState, rejectWithValue }) => {
    const protectionRules = selectProtectionRules(getState());

    const unconfiguredRepoManager = selectUnconfiguredRepoManager(getState());
    if (!unconfiguredRepoManager) {
      const errorMessage = 'There is no unconfigured repository manager selected';
      return rejectWithValue(errorMessage);
    }

    const repositories = selectRepositoriesList(getState());

    const errorMessage = 'Firewall configuration request could not be completed. Try again';
    return axios
      .put(getConfigureFirewallOnboardingUrl(), { ...protectionRules })
      .then(() => {
        return axios
          .put(getConfigureRepositoriesUrl(unconfiguredRepoManager.id), repositories)
          .then(() => {
            setShowWelcomeModalToTrueInStore();
            setLeftNavigationOpen(true);
            dispatch(actions.finishConfiguration());
            dispatch(stateGo('firewall.firewallPage'));
          })
          .catch(() => {
            return rejectWithValue(errorMessage);
          });
      })
      .catch(() => {
        return rejectWithValue(errorMessage);
      });
  }
);

const launchFirewallRequested = (state) => ({
  ...state,
  launchFirewall: {
    saving: true,
    saveError: null,
  },
});

const launchFirewallFulfilled = (state) => ({
  ...state,
  launchFirewall: {
    saving: false,
    saveError: null,
  },
});

const launchFirewallFailed = (state, { payload }) => ({
  ...state,
  launchFirewall: {
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
    [launchFirewall.pending]: launchFirewallRequested,
    [launchFirewall.fulfilled]: launchFirewallFulfilled,
    [launchFirewall.rejected]: launchFirewallFailed,
  },
});

export const actions = {
  ...firewallOnboardingSlice.actions,
  loadUnconfiguredRepoManagers,
  loadRepositories,
  launchFirewall,
};

export default firewallOnboardingSlice.reducer;
