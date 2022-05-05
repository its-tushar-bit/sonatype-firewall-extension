/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { prop, filter, equals } from 'ramda';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { Messages } from 'MainRoot/util/CommonServices';
import { propSet } from 'MainRoot/util/jsUtil';
import { propSetConst } from 'MainRoot/util/reduxToolkitUtil';
import { getProprietaryConfigUrl } from 'MainRoot/util/CLMLocation';
import { selectCurrentConfigs } from 'MainRoot/OrgsAndPolicies/proprietarySelectors';
import { selectOwnerProperties } from './orgsAndPoliciesSelectors';

const REDUCER_NAME = 'proprietary';

export const matcherTypes = { PACKAGE: 'Package', REGEX: 'Regular Expression' };

export const initialState = {
  isDirty: false,
  loading: false,
  loadError: null,
  submitError: null,
  currentConfig: {},
  serverConfig: {},
  proprietaryConfigs: [],
  localMatchers: [],
  packageMatcher: undefined,
  regexMatcher: undefined,
  matcherType: matcherTypes.PACKAGE,
};

const loadProprietaryConfigRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadProprietaryConfigFulfilled = (state, { payload }) => {
  const { localMatchers, currentConfig, proprietaryConfigs } = payload;
  state.loading = false;
  state.localMatchers = localMatchers;
  state.currentConfig = currentConfig;
  state.serverConfig = currentConfig;
  state.proprietaryConfigs = proprietaryConfigs;
};

const loadProprietaryConfigFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const saveProprietaryConfigFulfilled = (state, { payload }) => {
  state.isDirty = false;
  state.currentConfig = payload;
  state.serverConfig = payload;
};

const saveProprietaryConfigFailed = (state, { payload }) => {
  state.submitError = Messages.getHttpErrorMessage(payload);
};

const computeIsDirty = (state) => {
  const { currentConfig, serverConfig } = state;
  const isDirty =
    !equals(currentConfig.packages, serverConfig.packages) || !equals(currentConfig.regexes, serverConfig.regexes);

  return propSet('isDirty', isDirty, state);
};

const removeMatcher = (state, { payload }) => {
  const type = payload.type === matcherTypes.REGEX ? 'regexes' : 'packages';

  const newLocalMatchers = filter(
    (aMatcher) => aMatcher.type !== payload.type || aMatcher.matcher !== payload.matcher,
    state.localMatchers
  );

  return computeIsDirty({
    ...state,
    localMatchers: newLocalMatchers,
    currentConfig: {
      ...state.currentConfig,
      [type]: filter((aMatcher) => aMatcher !== payload.matcher, state.currentConfig[type]),
    },
  });
};

const addMatcher = (state, { payload }) => {
  const type = payload.type === matcherTypes.REGEX ? 'regexes' : 'packages';

  return computeIsDirty({
    ...state,
    localMatchers: [...state.localMatchers, payload],
    currentConfig: {
      ...state.currentConfig,
      [type]: [...state.currentConfig[type], payload.matcher],
    },
  });
};

const setMatcherPackageValue = (state, { payload }) => {
  state.packageMatcher = payload;
};

const setMatcherRegexValue = (state, { payload }) => {
  state.regexMatcher = payload;
};

const resetMatcher = (state) => {
  state.packageMatcher = initialState.packageMatcher;
  state.regexMatcher = initialState.regexMatcher;
};

const setMatcherType = (state, { payload }) => {
  state.matcherType = payload;
  state.packageMatcher = initialState.packageMatcher;
  state.regexMatcher = initialState.regexMatcher;
};

const loadProprietaryConfig = createAsyncThunk(
  `${REDUCER_NAME}/loadProprietaryConfig`,
  (_, { getState, rejectWithValue }) => {
    const { ownerType, ownerId } = selectOwnerProperties(getState());

    return axios
      .get(getProprietaryConfigUrl(ownerType, ownerId))
      .then(({ data: { proprietaryConfigByOwners } }) => {
        const localMatchers = [];
        const proprietaryConfig = proprietaryConfigByOwners[0].proprietaryConfig;

        proprietaryConfig.packages.forEach((component) => {
          localMatchers.push({
            type: matcherTypes.PACKAGE,
            matcher: component,
          });
        });

        proprietaryConfig.regexes.forEach((regex) => {
          localMatchers.push({
            type: matcherTypes.REGEX,
            matcher: regex,
          });
        });

        return {
          proprietaryConfigs: proprietaryConfigByOwners,
          currentConfig: proprietaryConfig,
          localMatchers,
        };
      })
      .catch(rejectWithValue);
  }
);

const saveProprietaryConfig = createAsyncThunk(
  `${REDUCER_NAME}/saveProprietaryConfig`,
  ({ setPristine }, { getState, rejectWithValue }) => {
    const state = getState();
    const { ownerType, ownerId } = selectOwnerProperties(state);
    const proprietaryConfig = selectCurrentConfigs(state);

    setPristine();

    return axios
      .put(getProprietaryConfigUrl(ownerType, ownerId), proprietaryConfig)
      .then(prop('data'))
      .catch(rejectWithValue);
  }
);

const proprietarySlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    removeMatcher,
    setMatcherPackageValue,
    setMatcherRegexValue,
    resetMatcher,
    setMatcherType,
    addMatcher,
  },
  extraReducers: {
    [loadProprietaryConfig.pending]: loadProprietaryConfigRequested,
    [loadProprietaryConfig.fulfilled]: loadProprietaryConfigFulfilled,
    [loadProprietaryConfig.rejected]: loadProprietaryConfigFailed,
    [saveProprietaryConfig.pending]: propSetConst('submitError', null),
    [saveProprietaryConfig.fulfilled]: saveProprietaryConfigFulfilled,
    [saveProprietaryConfig.rejected]: saveProprietaryConfigFailed,
  },
});

export const actions = {
  ...proprietarySlice.actions,
  loadProprietaryConfig,
  saveProprietaryConfig,
};

export default proprietarySlice.reducer;
