/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS, nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import { curryN, pick, prop } from 'ramda';
import { pathSet } from 'MainRoot/util/jsUtil';
import { Messages } from 'MainRoot/util/CommonServices';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { getConfigurationUrl } from 'MainRoot/util/CLMLocation';
import { propSetConst } from 'MainRoot/util/reduxToolkitUtil';
import { selectFormState } from './userTokensConfigurationSelectors';

const { userInput } = nxTextInputStateHelpers;

const REDUCER_NAME = 'userTokensConfiguration';
export const CONFIG_PROPERTIES_PARAMS = '?property=userTokenDefaultExpirationDays';
const DEFAULT_EXPIRATION_DAYS = 30;

export const initialState = {
  loading: false,
  loadError: null,
  updateError: null,
  isDirty: false,
  submitMaskState: null,
  formState: {
    userTokensEnabled: true,
    expirationEnabled: false,
    expirationDays: userInput(validateExpirationDays, String(DEFAULT_EXPIRATION_DAYS)),
  },
  serverData: null,
};

const clearedErrors = pick(['loadError', 'updateError'], initialState);

function inputToInt(value) {
  const cleaned = value.replace(/,/g, '');
  return Number.parseInt(cleaned, 10);
}

function validateExpirationDays(value) {
  const trimmedValue = value.trim();

  if (!trimmedValue) {
    return 'Must be non-empty.';
  }

  const numValue = inputToInt(trimmedValue);

  if (!Number.isInteger(numValue)) {
    return 'Must be a valid integer.';
  }

  if (numValue < 1) {
    return 'Must be at least 1 day.';
  }

  if (numValue > 365) {
    return 'Must be at most 365 days.';
  }

  return null;
}

function setFormStateFromServerData(state) {
  const { serverData } = state;
  const formState = {
    userTokensEnabled: true,
    expirationEnabled: serverData?.userTokenDefaultExpirationDays != null,
    expirationDays: userInput(
      validateExpirationDays,
      String(serverData?.userTokenDefaultExpirationDays ?? DEFAULT_EXPIRATION_DAYS)
    ),
  };

  return { ...state, formState };
}

const computeIsDirty = (state) => {
  const { formState, serverData } = state;
  const { expirationEnabled: formExpirationEnabled, expirationDays: formExpirationDays } = formState;
  const { userTokenDefaultExpirationDays: serverExpirationDays } = serverData || {};

  const serverExpirationEnabled = serverExpirationDays != null;
  const expirationEnabledChanged = formExpirationEnabled !== serverExpirationEnabled;
  const expirationDaysChanged =
    formExpirationEnabled && inputToInt(formExpirationDays.value) !== (serverExpirationDays ?? DEFAULT_EXPIRATION_DAYS);

  return { ...state, isDirty: expirationEnabledChanged || expirationDaysChanged };
};

const toggleExpirationEnabled = curryN(2, function toggleExpiration(state) {
  const isEnabled = state.formState.expirationEnabled;
  return computeIsDirty(pathSet(['formState', 'expirationEnabled'], !isEnabled, state));
});

const setExpirationDays = (state, action) => {
  const updatedState = pathSet(
    ['formState', 'expirationDays'],
    userInput(validateExpirationDays, action.payload),
    state
  );
  return computeIsDirty(updatedState);
};

const startMaskSuccessTimer = (dispatch, action) => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve(dispatch(action));
    }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
  });
};

function loadRequested(state) {
  return {
    ...state,
    ...clearedErrors,
    loading: true,
  };
}

function loadFulfilled(state, { payload }) {
  return resetForm({
    ...state,
    serverData: payload,
  });
}

function loadFailed(state, { payload }) {
  return {
    ...state,
    loading: false,
    ...clearedErrors,
    loadError: payload.response?.status === 404 ? null : Messages.getHttpErrorMessage(payload),
  };
}

function updateRequested(state) {
  return {
    ...state,
    ...clearedErrors,
    submitMaskState: false,
  };
}

const updateFulfilled = (state) => {
  const expirationDays = state.formState.expirationEnabled ? inputToInt(state.formState.expirationDays.value) : null;
  return {
    ...state,
    submitMaskState: true,
    isDirty: false,
    serverData: {
      userTokenDefaultExpirationDays: expirationDays,
    },
  };
};

function updateFailed(state, { payload }) {
  return {
    ...state,
    updateError: Messages.getHttpErrorMessage(payload),
    submitMaskState: null,
  };
}

function resetForm(state) {
  return setFormStateFromServerData({
    ...initialState,
    serverData: state.serverData ?? initialState.serverData,
  });
}

const load = createAsyncThunk(`${REDUCER_NAME}/load`, async (_, { rejectWithValue }) => {
  return axios.get(getConfigurationUrl().concat(CONFIG_PROPERTIES_PARAMS)).then(prop('data')).catch(rejectWithValue);
});

const update = createAsyncThunk(`${REDUCER_NAME}/update`, (_, { getState, dispatch, rejectWithValue }) => {
  const state = getState();
  const formState = selectFormState(state);
  const serverData = {
    userTokenDefaultExpirationDays: formState.expirationEnabled ? inputToInt(formState.expirationDays.value) : null,
  };
  return axios
    .put(getConfigurationUrl(), serverData)
    .then(() => {
      startMaskSuccessTimer(dispatch, actions.submitMaskTimerDone).then(() => dispatch(load()));
    })
    .catch(rejectWithValue);
});

const userTokensConfigurationSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    resetForm,
    toggleExpirationEnabled: toggleExpirationEnabled(),
    setExpirationDays,
    submitMaskTimerDone: propSetConst('submitMaskState', null),
  },
  extraReducers: {
    [load.pending]: loadRequested,
    [load.fulfilled]: loadFulfilled,
    [load.rejected]: loadFailed,
    [update.pending]: updateRequested,
    [update.fulfilled]: updateFulfilled,
    [update.rejected]: updateFailed,
  },
});

export default userTokensConfigurationSlice.reducer;

export const actions = {
  ...userTokensConfigurationSlice.actions,
  load,
  update,
};
