/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
// import axios from 'axios';
import * as R from 'ramda';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';

import { ROI_SECURITY_VIOLATION_TYPES } from 'MainRoot/configuration/roiConfiguration/roiConfigurationPageSlice';
import { checkPermissions } from 'MainRoot/util/authorizationUtil';

// import { getConfigurationUrl } from 'MainRoot/util/CLMLocation';

const { initialState: inputInitialState, userInput } = nxTextInputStateHelpers;

const ensureValidNumber = R.when(R.either(R.complement(R.is(Number)), Number.isNaN), R.always(0));

const REDUCER_NAME = 'editRoiConfigurationPage';

export const generateDefaultNumericState = (enabled, minimum, initialValue) =>
  Object.freeze({
    enabled,
    minimum: ensureValidNumber(minimum),
    input: inputInitialState(R.when(R.is(Number), R.toString)(initialValue)),
  });

export const ROI_CURRENCY_TYPES = Object.freeze([
  'developerHourlyRate',
  'supplyChainAttacksBlocked',
  'namespaceAttacksBlocked',
  'safeComponentsAutoSelected',
]);
export const ROI_INTEGER_TYPES = Object.freeze(['fixRate']);
export const ROI_BOOLEAN_TYPES = Object.freeze(['waivedViolations']);

const defaultConfiguration = Object.freeze({
  developerHourlyRate: generateDefaultNumericState(true, 0, 0),
  fixRate: generateDefaultNumericState(true, 0, 0),

  securityViolation: {
    critical: generateDefaultNumericState(true, 0, 0),
    high: generateDefaultNumericState(true, 0, 0),
    medium: generateDefaultNumericState(false, 0, 0),
    low: generateDefaultNumericState(true, 0, 0),
  },

  supplyChainAttacksBlocked: generateDefaultNumericState(true, 0, 0),
  namespaceAttacksBlocked: generateDefaultNumericState(true, 0, 0),
  safeComponentsAutoSelected: generateDefaultNumericState(true, 0, 0),

  waivedViolations: true,
});

export const initialState = Object.freeze({
  loading: true,
  error: null,
  showRestoreDefaultsModal: false,
  configuration: { ...defaultConfiguration },
});

// validation
const CURRENCY_STRING_REGEX = /^\s*-?\d+(,\d+)*(\.\d+)?\s*$/g;
const INTEGER_STRING_REGEX = /^\s*-?\d+(,\d+)*\s*$/g;

const currencyValueValidator = R.curry((minimum, value) =>
  R.cond([
    [R.compose(R.isEmpty, R.trim), R.always('Must be non-empty.')],
    [R.compose(R.not, R.test(CURRENCY_STRING_REGEX)), R.always('Must be a valid numeric format.')],
    [
      R.lt(R.__, ensureValidNumber(minimum)),
      R.always(`Must be greater than or equal to ${ensureValidNumber(minimum)}.`),
    ],
    [R.T, R.always(null)],
  ])(value)
);

const integerValueValidator = R.curry((minimum, value) =>
  R.cond([
    [R.compose(R.isEmpty, R.trim), R.always('Must be non-empty.')],
    [R.compose(R.not, R.test(INTEGER_STRING_REGEX)), R.always('Must be a valid positive integer.')],
    [
      R.lt(R.__, ensureValidNumber(minimum)),
      R.always(`Must be greater than or equal to ${ensureValidNumber(minimum)}.`),
    ],
    [R.T, R.always(null)],
  ])(value)
);

// update-configuration-value
const updateConfigurationValue = (state, { payload }) => {
  const type = R.cond([
    [R.includes(R.__, ROI_CURRENCY_TYPES), R.always('currency')],
    [R.includes(R.__, ROI_INTEGER_TYPES), R.always('integer')],
    [R.T, R.always(null)],
  ])(payload.key);

  if (type && state.configuration[payload.key].enabled === true) {
    const validators = {
      currency: currencyValueValidator,
      integer: integerValueValidator,
    };
    const validator = validators[type](state.configuration[payload.key].minimum);
    state.configuration[payload.key].input = userInput(validator, payload.value);
  }
};

const toggleConfigurationBooleanValue = (state, { payload }) => {
  if (R.includes(payload.key, ROI_BOOLEAN_TYPES)) {
    state.configuration[payload.key] = !state.configuration[payload.key];
  }
};

const toggleSecurityViolationEnabled = (state, { payload }) => {
  if (R.includes(payload.key, ROI_SECURITY_VIOLATION_TYPES)) {
    state.configuration.securityViolation[payload.key].enabled = !state.configuration.securityViolation[payload.key]
      .enabled;
  }
};

const updateSecurityViolationValue = (state, { payload }) => {
  if (state.configuration.securityViolation[payload.key].enabled === true) {
    state.configuration.securityViolation[payload.key].input = userInput(
      currencyValueValidator(state.configuration.securityViolation[payload.key].minimum),
      payload.value
    );
  }
};

// restore-defaults-modal
const setShowRestoreDefaultsModal = (state, { payload }) => {
  state.showRestoreDefaultsModal = payload;
};

// load-configuration
const loadConfigurationRequested = (state) => {
  state.loading = true;
  state.error = null;
};

const loadConfigurationRejected = (state, { payload }) => {
  state.loading = false;
  state.error = payload;
};

const loadConfigurationFulfilled = (state) => {
  state.loading = false;
  state.error = null;
  // TODO: map payload to configuration state.
};

const loadConfiguration = createAsyncThunk(`${REDUCER_NAME}/loadConfiguration`, async (_, { rejectWithValue }) => {
  try {
    await checkPermissions(['CONFIGURE_SYSTEM']);
    return Promise.resolve({});
  } catch (error) {
    return rejectWithValue(error);
  }
});

const editRoiConfigurationPageSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    updateConfigurationValue,
    updateSecurityViolationValue,
    toggleConfigurationBooleanValue,
    toggleSecurityViolationEnabled,
    setShowRestoreDefaultsModal,
  },
  extraReducers: {
    [loadConfiguration.pending]: loadConfigurationRequested,
    [loadConfiguration.rejected]: loadConfigurationRejected,
    [loadConfiguration.fulfilled]: loadConfigurationFulfilled,
  },
});

export default editRoiConfigurationPageSlice.reducer;

export const actions = {
  ...editRoiConfigurationPageSlice.actions,
  loadConfiguration,
};
