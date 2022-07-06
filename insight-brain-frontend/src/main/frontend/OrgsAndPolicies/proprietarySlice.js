/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { filter, equals, curry } from 'ramda';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { propSet } from 'MainRoot/util/jsUtil';
import { getProprietaryConfigUrl } from 'MainRoot/util/CLMLocation';
import { selectCurrentConfigs } from 'MainRoot/OrgsAndPolicies/proprietarySelectors';
import { selectOwnerProperties } from './orgsAndPoliciesSelectors';
import { startSaveMaskSuccessTimer } from 'MainRoot/util/reduxUtil';
import { validateNonEmpty } from 'MainRoot/util/validationUtil';

const { initialState: rscInitialState, userInput } = nxTextInputStateHelpers;

const REDUCER_NAME = 'proprietary';

const PACKAGE_BEGINNING_ENDING_PERIOD_ERROR = 'Value cannot begin or end with a period “.”';
const DUPLICATE_VALUE_ERROR = 'Duplicate value name';
const INVALID_JAVA_PACKAGE_NAME = 'Invalid Java package name';

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
  matcherValue: rscInitialState(''),
  matcherType: matcherTypes.PACKAGE,
  submitMaskState: null,
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

const saveProprietaryConfigPending = (state) => {
  state.submitError = null;
  state.submitMaskState = false;
};

const saveProprietaryConfigFulfilled = (state, { payload }) => {
  state.isDirty = false;
  state.currentConfig = payload;
  state.serverConfig = payload;
  state.submitMaskState = true;
};

const saveProprietaryConfigFailed = (state, { payload }) => {
  state.submitError = Messages.getHttpErrorMessage(payload);
  state.submitMaskState = null;
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

const resetMatcher = (state) => {
  state.matcherValue = initialState.matcherValue;
};

const setMatcherType = (state, { payload }) => {
  state.matcherType = payload;
  setMatcherValue(state, { payload: state.matcherValue.value });
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
  (_, { getState, rejectWithValue, dispatch }) => {
    const state = getState();
    const { ownerType, ownerId } = selectOwnerProperties(state);
    const proprietaryConfig = selectCurrentConfigs(state);

    return axios
      .put(getProprietaryConfigUrl(ownerType, ownerId), proprietaryConfig)
      .then(({ data }) => {
        startSaveMaskSuccessTimer(dispatch, actions.saveMaskTimerDone);
        return data;
      })
      .catch(rejectWithValue);
  }
);

const validatePeriodPlacement = (inputValue) =>
  inputValue.indexOf('.') === 0 || inputValue.lastIndexOf('.') === inputValue.length - 1
    ? PACKAGE_BEGINNING_ENDING_PERIOD_ERROR
    : null;

const validatePackagePrefix = (inputValue) =>
  inputValue.includes(' ') || inputValue.includes('/') ? INVALID_JAVA_PACKAGE_NAME : null;

const inputMatcherValidation = curry(function (state, isPackage, inputValue) {
  if (validateNonEmpty(inputValue)) {
    return validateNonEmpty(inputValue);
  }

  const hasError = state.localMatchers.find(
    ({ type, matcher }) => type === state.matcherType && matcher === inputValue
  );

  if (hasError) {
    return DUPLICATE_VALUE_ERROR;
  }

  if (isPackage) {
    return validatePeriodPlacement(inputValue) || validatePackagePrefix(inputValue);
  }
  return null;
});

const setMatcherValue = (state, { payload }) => {
  state.matcherValue =
    state.matcherType === matcherTypes.PACKAGE
      ? userInput(inputMatcherValidation(state, true), payload)
      : userInput(inputMatcherValidation(state, false), payload);
};

const proprietarySlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    removeMatcher,
    setMatcherValue,
    resetMatcher,
    setMatcherType,
    addMatcher,
    saveMaskTimerDone: propSet('submitMaskState', null),
  },
  extraReducers: {
    [loadProprietaryConfig.pending]: loadProprietaryConfigRequested,
    [loadProprietaryConfig.fulfilled]: loadProprietaryConfigFulfilled,
    [loadProprietaryConfig.rejected]: loadProprietaryConfigFailed,
    [saveProprietaryConfig.pending]: saveProprietaryConfigPending,
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
