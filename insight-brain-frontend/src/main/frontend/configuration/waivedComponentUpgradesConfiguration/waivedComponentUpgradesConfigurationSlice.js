/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { curryN, pick, prop } from 'ramda';
import { pathSet } from 'MainRoot/util/jsUtil';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { getConfigurationUrl } from 'MainRoot/util/CLMLocation';
import { propSetConst } from 'MainRoot/util/reduxToolkitUtil';
import { selectFormState } from './waivedComponentUpgradesConfigurationSelectors';

const REDUCER_NAME = 'waivedComponentUpgradesConfiguration';
export const CONFIG_PROPERTIES_PARAMS = '?property=waivedComponentUpgradeMonitoringEnabled';

export const initialState = {
  loading: false,
  loadError: null,
  updateError: null,
  isDirty: false,
  submitMaskState: null,
  formState: {
    waivedComponentUpgradeMonitoringEnabled: false,
  },
  serverData: null,
};

const clearedErrors = pick(['loadError', 'updateError'], initialState);

function setFormStateFromServerData(state) {
  const { serverData } = state,
    formState = {
      waivedComponentUpgradeMonitoringEnabled: serverData?.waivedComponentUpgradeMonitoringEnabled,
    };

  return { ...state, formState };
}

const computeIsDirty = (state) => {
  const { formState, serverData } = state;
  const { waivedComponentUpgradeMonitoringEnabled: formWaivedComponentUpgradeMonitoringEnabled } = formState;
  const { waivedComponentUpgradeMonitoringEnabled: serverWaivedComponentUpgradeMonitoringEnabled } = serverData || {};
  const waivedComponentUpgradeMonitoringEnabledChanged =
    formWaivedComponentUpgradeMonitoringEnabled !== serverWaivedComponentUpgradeMonitoringEnabled;
  return { ...state, isDirty: waivedComponentUpgradeMonitoringEnabledChanged };
};

const toggleWaivedComponentUpgradeMonitoringEnabled = curryN(2, function setTextInput(state) {
  const isEnabled = state.formState.waivedComponentUpgradeMonitoringEnabled;
  return computeIsDirty(pathSet(['formState', 'waivedComponentUpgradeMonitoringEnabled'], !isEnabled, state));
});

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
    loadError: Messages.getHttpErrorMessage(payload),
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
  return {
    ...state,
    submitMaskState: true,
    isDirty: false,
    serverData: {
      waivedComponentUpgradeMonitoringEnabled: state.formState.waivedComponentUpgradeMonitoringEnabled,
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
  return axios
    .put(getConfigurationUrl(), formState)
    .then(() => {
      startMaskSuccessTimer(dispatch, actions.submitMaskTimerDone).then(() => dispatch(load()));
    })
    .catch(rejectWithValue);
});

const waivedComponentUpgradeMonitoringEnabledConfigurationSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    resetForm,
    toggleIsEnabled: toggleWaivedComponentUpgradeMonitoringEnabled(),
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

export default waivedComponentUpgradeMonitoringEnabledConfigurationSlice.reducer;

export const actions = {
  ...waivedComponentUpgradeMonitoringEnabledConfigurationSlice.actions,
  load,
  update,
};
