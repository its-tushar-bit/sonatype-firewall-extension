/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { nxTextInputStateHelpers, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { compose, curryN, map, pick, prop } from 'ramda';
import { pathSet } from 'MainRoot/util/jsUtil';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { getConfigurationUrl } from 'MainRoot/util/CLMLocation';
import { validateNonEmpty } from 'MainRoot/util/validationUtil';
import { propSet, propSetConst } from 'MainRoot/util/reduxToolkitUtil';
import { selectFormState } from 'MainRoot/configuration/baseUrl/baseUrlConfigurationSelectors';

const REDUCER_NAME = 'baseUrlConfiguration';
export const CONFIG_PROPERTIES_PARAMS = '?property=baseUrl';

export const initialState = {
  loading: false,
  loadError: null,
  updateError: null,
  deleteError: null,
  isDirty: false,
  submitMaskState: null,
  submitMaskMessage: null,
  deleteMaskState: null,
  shouldDisplayNotice: false,
  formState: {
    baseUrl: nxTextInputStateHelpers.initialState(''),
  },
  serverData: null,
  showDeleteModal: false,
};

const clearedErrors = pick(['loadError', 'updateError', 'deleteError'], initialState);

function setFormStateFromServerData(state) {
  const { serverData } = state,
    formState = {
      baseUrl: nxTextInputStateHelpers.initialState(serverData?.baseUrl || ''),
    };

  return { ...state, formState };
}

const computeIsDirty = (state) => {
  const { formState, serverData } = state;
  const { baseUrl } = formState;
  const { baseUrl: serverBaseUrl } = serverData || {};
  const baseUrlChanged = serverBaseUrl === null ? baseUrl.trimmedValue !== '' : baseUrl.trimmedValue !== serverBaseUrl;
  return { ...state, isDirty: baseUrlChanged };
};

const setTextInput = curryN(4, function setTextInput(fieldName, validator, state, payload) {
  const stateWithUpdatedValue = pathSet(
    ['formState', fieldName],
    nxTextInputStateHelpers.userInput(validator, payload.payload),
    state
  );

  return computeIsDirty(stateWithUpdatedValue);
});

const startMaskSuccessTimer = (dispatch, action) => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve(dispatch(action));
    }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
  });
};

function toServerData(formState) {
  const textPropMapper = ({ trimmedValue }) => trimmedValue || null;

  return {
    ...map(textPropMapper, pick(['baseUrl'], formState)),
  };
}

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
    shouldDisplayNotice: !payload?.baseUrl,
    serverData: payload,
  });
}

function loadFailed(state, { payload }) {
  return {
    ...state,
    loading: false,
    shouldDisplayNotice: false,
    ...clearedErrors,
    loadError: payload.response?.status === 404 ? null : Messages.getHttpErrorMessage(payload),
  };
}

function updateRequested(state) {
  return {
    ...state,
    ...clearedErrors,
    submitMaskState: false,
    submitMaskMessage: 'Saving…',
  };
}

const updateFulfilled = (state) => {
  return {
    ...state,
    submitMaskState: true,
    isDirty: false,
    serverData: {
      baseUrl: state.formState.baseUrl.trimmedValue,
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
    shouldDisplayNotice: state.shouldDisplayNotice,
    serverData: state.serverData ? state.serverData : initialState.serverData,
  });
}

function deleteRequested(state) {
  return {
    ...state,
    ...clearedErrors,
    deleteMaskState: false,
    submitMaskMessage: 'Deleting…',
  };
}

function deleteFulfilled() {
  return {
    ...initialState,
    showDeleteModal: true,
    deleteMaskState: true,
    shouldDisplayNotice: true,
  };
}

function deleteFailed(state, { payload }) {
  return {
    ...state,
    deleteMaskState: null,
    deleteError: Messages.getHttpErrorMessage(payload),
  };
}

const load = createAsyncThunk(`${REDUCER_NAME}/load`, async (_, { rejectWithValue }) => {
  return axios.get(getConfigurationUrl().concat(CONFIG_PROPERTIES_PARAMS)).then(prop('data')).catch(rejectWithValue);
});

const del = createAsyncThunk(`${REDUCER_NAME}/delete`, async (_, { dispatch, rejectWithValue }) => {
  return axios
    .delete(getConfigurationUrl().concat(CONFIG_PROPERTIES_PARAMS))
    .then(() => {
      startMaskSuccessTimer(dispatch, actions.deleteMaskTimerDone).then(() =>
        dispatch(actions.setShowDeleteModal(false))
      );
    })
    .catch(rejectWithValue);
});

const update = createAsyncThunk(`${REDUCER_NAME}/update`, (_, { getState, dispatch, rejectWithValue }) => {
  const state = getState();
  const formState = selectFormState(state);
  const serverData = toServerData(formState);
  return axios
    .put(getConfigurationUrl(), { ...serverData })
    .then(() => {
      startMaskSuccessTimer(dispatch, actions.submitMaskTimerDone).then(() => dispatch(load()));
    })
    .catch(rejectWithValue);
});

const baseUrlConfigurationSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    resetForm,
    setInputValueBaseUrl: setTextInput('baseUrl', validateNonEmpty),
    submitMaskTimerDone: propSetConst('submitMaskState', null),
    deleteMaskTimerDone: propSetConst('deleteMaskState', null),
    setShowDeleteModal: compose(propSetConst('deleteError', null), propSet('showDeleteModal')),
  },
  extraReducers: {
    [load.pending]: loadRequested,
    [load.fulfilled]: loadFulfilled,
    [load.rejected]: loadFailed,
    [del.pending]: deleteRequested,
    [del.fulfilled]: deleteFulfilled,
    [del.rejected]: deleteFailed,
    [update.pending]: updateRequested,
    [update.fulfilled]: updateFulfilled,
    [update.rejected]: updateFailed,
  },
});

export default baseUrlConfigurationSlice.reducer;

export const actions = {
  ...baseUrlConfigurationSlice.actions,
  load,
  del,
  update,
};
