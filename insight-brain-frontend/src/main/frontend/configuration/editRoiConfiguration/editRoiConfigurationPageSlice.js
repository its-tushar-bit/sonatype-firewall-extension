/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import axios from 'axios';
import * as R from 'ramda';

import { checkPermissions } from 'MainRoot/util/authorizationUtil';
import { getRoiConfigurationUrl, getRoiConfigurationRestoreDefaultsUrl } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/utilAngular/CommonServices';

import { selectEditRoiConfigurationPageSlice } from './editRoiConfigurationPageSelectors';

const { initialState: inputInitialState, userInput } = nxTextInputStateHelpers;

const ensureValidNumber = R.when(R.either(R.complement(R.is(Number)), Number.isNaN), R.always(0));
const inputToFloat = R.compose(Number.parseFloat, R.replace(/,/g, ''));
const inputToInt = R.compose(Number.parseInt, R.replace(/,/g, ''));

const REDUCER_NAME = 'editRoiConfigurationPage';

export const instantiateNumericState = (minimum, initialValue) =>
  Object.freeze({
    minimum: ensureValidNumber(minimum),
    input: inputInitialState(R.when(R.is(Number), R.toString)(initialValue)),
  });

export const ROI_CURRENCY_TYPES = Object.freeze([
  'dailyRiskCostOfUnfixedViolation',
  'malwareAttacksPrevented',
  'namespaceAttacksPrevented',
  'safeComponentsAutoSelected',
]);
export const ROI_INTEGER_TYPES = Object.freeze(['baselineDaysToResolveViolation']);
export const ROI_VALIDATABLE_FIELDS = Object.freeze([...ROI_CURRENCY_TYPES, ...ROI_INTEGER_TYPES]);

const mapConfigurationToPayload = (configuration) =>
  Object.freeze({
    currency: 'USD',
    baselineDaysToResolveViolation: inputToInt(configuration.baselineDaysToResolveViolation.input.value),
    dailyRiskCostOfUnfixedViolation: inputToFloat(configuration.dailyRiskCostOfUnfixedViolation.input.value),
    malwareAttacksPrevented: inputToFloat(configuration.malwareAttacksPrevented.input.value),
    namespaceAttacksPrevented: inputToFloat(configuration.namespaceAttacksPrevented.input.value),
    safeComponentsAutoSelected: inputToFloat(configuration.safeComponentsAutoSelected.input.value),
  });

const mapPayloadToConfiguration = (payload) =>
  Object.freeze({
    baselineDaysToResolveViolation: instantiateNumericState(
      payload.baselineDaysToResolveViolationMinimum,
      payload.baselineDaysToResolveViolation
    ),
    dailyRiskCostOfUnfixedViolation: instantiateNumericState(
      payload.dailyRiskCostOfUnfixedViolationMinimum,
      payload.dailyRiskCostOfUnfixedViolation
    ),
    malwareAttacksPrevented: instantiateNumericState(
      payload.malwareAttacksPreventedMinimum,
      payload.malwareAttacksPrevented
    ),
    namespaceAttacksPrevented: instantiateNumericState(
      payload.namespaceAttacksPreventedMinimum,
      payload.namespaceAttacksPrevented
    ),
    safeComponentsAutoSelected: instantiateNumericState(
      payload.safeComponentsAutoSelectedMinimum,
      payload.safeComponentsAutoSelected
    ),
  });

const initialConfiguration = Object.freeze({
  baselineDaysToResolveViolation: instantiateNumericState(0, 0),
  dailyRiskCostOfUnfixedViolation: instantiateNumericState(0, 0),
  malwareAttacksPrevented: instantiateNumericState(0, 0),
  namespaceAttacksPrevented: instantiateNumericState(0, 0),
  safeComponentsAutoSelected: instantiateNumericState(0, 0),
});

export const initialState = Object.freeze({
  loading: true,
  error: null,
  showRestoreDefaultsModal: false,
  configuration: { ...initialConfiguration },
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

  if (type) {
    const validators = {
      currency: currencyValueValidator,
      integer: integerValueValidator,
    };
    const validator = validators[type](state.configuration[payload.key].minimum);
    state.configuration[payload.key].input = userInput(validator, payload.value);
  }
};

// load-configuration
const loadConfigurationRequested = (state) => {
  state.loading = true;
  state.error = null;
  state.showRestoreDefaultsModal = false;
};

const loadConfigurationRejected = (state, { payload }) => {
  state.loading = false;
  state.error = Messages.getHttpErrorMessage(payload);
};

const loadConfigurationFulfilled = (state, { payload }) => {
  state.loading = false;
  state.error = null;
  state.configuration = mapPayloadToConfiguration(payload);
};

const loadConfiguration = createAsyncThunk(`${REDUCER_NAME}/loadConfiguration`, async (_, { rejectWithValue }) => {
  try {
    await checkPermissions(['CONFIGURE_SYSTEM']);
    const { data } = await axios.get(getRoiConfigurationUrl('usd'));
    return data;
  } catch (error) {
    return rejectWithValue(error);
  }
});

// update-configuration
const updateConfigurationRequested = (state) => {
  state.loading = true;
  state.error = null;
};

const updateConfigurationRejected = (state, { payload }) => {
  state.loading = false;
  state.error = Messages.getHttpErrorMessage(payload);
};

const updateConfigurationFulfilled = (state, { payload }) => {
  state.loading = false;
  state.error = null;
  state.configuration = mapPayloadToConfiguration(payload);
};

const updateConfiguration = createAsyncThunk(
  `${REDUCER_NAME}/updateConfiguration`,
  async (_, { getState, rejectWithValue }) => {
    const { configuration } = selectEditRoiConfigurationPageSlice(getState());
    const payload = mapConfigurationToPayload(configuration);
    try {
      await checkPermissions(['CONFIGURE_SYSTEM']);
      const { data } = await axios.post(getRoiConfigurationUrl(), payload);
      return data;
    } catch (error) {
      return rejectWithValue(error);
    }
  }
);

// restore-defaults-modal
const setShowRestoreDefaultsModal = (state, { payload }) => {
  state.showRestoreDefaultsModal = payload;
};

// restore-defaults
const restoreDefaultsRequested = (state) => {
  state.loading = true;
  state.error = null;
  state.showRestoreDefaultsModal = false;
};

const restoreDefaultsRejected = (state, { payload }) => {
  state.loading = false;
  state.error = Messages.getHttpErrorMessage(payload);
};

const restoreDefaultsFulfilled = (state, { payload }) => {
  state.loading = false;
  state.error = null;
  state.configuration = mapPayloadToConfiguration(payload);
};

const restoreDefaults = createAsyncThunk(`${REDUCER_NAME}/restoreDefaults`, async (_, { rejectWithValue }) => {
  try {
    await checkPermissions(['CONFIGURE_SYSTEM']);
    const { data } = await axios.post(getRoiConfigurationRestoreDefaultsUrl('usd'));
    return data;
  } catch (error) {
    return rejectWithValue(error);
  }
});

const editRoiConfigurationPageSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    updateConfigurationValue,
    setShowRestoreDefaultsModal,
  },
  extraReducers: {
    [loadConfiguration.pending]: loadConfigurationRequested,
    [loadConfiguration.rejected]: loadConfigurationRejected,
    [loadConfiguration.fulfilled]: loadConfigurationFulfilled,
    [updateConfiguration.pending]: updateConfigurationRequested,
    [updateConfiguration.rejected]: updateConfigurationRejected,
    [updateConfiguration.fulfilled]: updateConfigurationFulfilled,
    [restoreDefaults.pending]: restoreDefaultsRequested,
    [restoreDefaults.rejected]: restoreDefaultsRejected,
    [restoreDefaults.fulfilled]: restoreDefaultsFulfilled,
  },
});

export default editRoiConfigurationPageSlice.reducer;

export const actions = {
  ...editRoiConfigurationPageSlice.actions,
  loadConfiguration,
  updateConfiguration,
  restoreDefaults,
};
