/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { nxTextInputStateHelpers, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { any, compose, curryN, equals, map, pick, prop, values, keys, filter, fromPairs } from 'ramda';
import { createAsyncThunk } from '@reduxjs/toolkit';
import createSlice from 'MainRoot/reduxConfig/createSlice';

import { hasValidationErrors, validateNonEmpty } from '../../util/validationUtil';
import { validateZScalerHostName, validateZscalerApiKey } from './utils/validators';
import { pathSet } from '../../util/jsUtil';
import { propSet, propSetConst } from '../../util/reduxToolkitUtil';
import { getZScalerConfigUrl, getZScalerTestConfigUrl } from '../../util/CLMLocation';
import { Messages } from 'MainRoot/util/CommonServices';

const SUBMIT_MASK_SAVING_MESSAGE = 'Saving';
const SUBMIT_MASK_DELETING_MESSAGE = 'Deleting';
const SUBMIT_MASK_TEST_CONFIG_MESSAGE = 'Testing configuration...';
export const FAKE_PASSWORD = '\x00\x00\x00\x00\x00';

const REDUCER_NAME = 'zscalerConfig';

const validateCheckbox = (checkBoxValue) => (checkBoxValue ? null : 'This field is required');

export const initialState = {
  // the data object as it is on the server, based on the last GET or synthesized after the last save
  serverData: null,
  formState: {
    username: nxTextInputStateHelpers.initialState('', validateNonEmpty),
    password: nxTextInputStateHelpers.initialState('', validateNonEmpty),
    hostname: nxTextInputStateHelpers.initialState('', validateZScalerHostName),
    apiKey: nxTextInputStateHelpers.initialState('', validateZscalerApiKey),
    eula: {
      value: false,
      isPristine: true,
      validationErrors: 'This field is required',
      disabled: false,
    },
    configuredFormatState: {
      formats: new Set(),
      isPristine: true,
      validationErrors: 'At least one format must be selected',
    },
  },
  isDirty: false,
  isValid: false,
  hasAllRequiredData: false,
  hasAllRequiredDataForTestConfig: false,
  loading: false,
  submitMaskState: null, // one of null, false, or true as patterned in the NxStatefulSubmitMask examples
  submitMaskMessage: null,
  loadError: null,
  saveError: null,
  deleteError: null,
  testConfigError: null,
  testConfigSuccess: false,
  showDeleteModal: false,
  mustReenterPassword: false,
};

const textProps = ['username', 'password', 'hostname', 'apiKey'],
  booleanProps = ['mavenFormatEnabled', 'npmFormatEnabled', 'pypiFormatEnabled', 'nugetFormatEnabled'];

const clearedErrors = pick(['loadError', 'saveError', 'deleteError', 'testConfigError'], initialState);

function setFormStateFromServerData(state) {
  const { serverData } = state,
    formState = {
      username: nxTextInputStateHelpers.initialState(serverData.username),
      password: nxTextInputStateHelpers.initialState(FAKE_PASSWORD),
      hostname: nxTextInputStateHelpers.initialState(serverData.hostname),
      apiKey: nxTextInputStateHelpers.initialState(serverData.apiKey),
      eula: {
        value: true,
        isPristine: true,
        validationErrors: null,
        disabled: true,
      },
      configuredFormatState: {
        formats: new Set(
          keys(
            filter(
              Boolean,
              pick(['mavenFormatEnabled', 'npmFormatEnabled', 'pypiFormatEnabled', 'nugetFormatEnabled'], serverData)
            )
          )
        ),
        isPristine: true,
        validationErrors: null,
      },
    };

  return computeHasAllRequiredData({ ...state, formState });
}

function computeHasAllRequiredData(state) {
  const {
      formState: { username, password, hostname, apiKey, configuredFormatState, eula },
    } = state,
    // Check if hostname has value AND no validation errors
    isHostnameValid = hostname.value && (!hostname.validationErrors || hostname.validationErrors.length === 0),
    isApiKeyValid = apiKey.value && (!apiKey.validationErrors || apiKey.validationErrors.length === 0),
    hasAllRequiredData = !!(
      username.value &&
      password.value &&
      isHostnameValid &&
      isApiKeyValid &&
      configuredFormatState.formats.size > 0 &&
      eula.value
    ),
    hasAllRequiredDataForTestConfig = !!(username.value && password.value && isHostnameValid && isApiKeyValid);

  return { ...state, hasAllRequiredData, hasAllRequiredDataForTestConfig };
}

function computeIsDirty(state) {
  const { formState, serverData } = state;

  if (serverData) {
    const isTextPropDirty = (prop) => formState[prop].trimmedValue !== (serverData[prop] || ''),
      severConfiguredFormats = new Set(filter((prop) => serverData[prop] === true, booleanProps)),
      textPropsDirty = any(isTextPropDirty, ['username', 'hostname', 'apiKey']),
      booleanPropsDirty = !equals(severConfiguredFormats, formState.configuredFormatState.formats),
      passwordDirty = formState.password.value !== FAKE_PASSWORD;

    return {
      ...state,
      isDirty: textPropsDirty || booleanPropsDirty || passwordDirty,
    };
  } else {
    const textPropsDirty = any((prop) => formState[prop].trimmedValue !== '', textProps),
      booleanPropsDirty = !formState.configuredFormatState.isPristine || !formState.eula.isPristine;

    return { ...state, isDirty: textPropsDirty || booleanPropsDirty };
  }
}

function computeIsValid(state) {
  const { formState } = state,
    validationErrorsByProp = map(prop('validationErrors'), pick(textProps, formState)),
    isValid = !any(hasValidationErrors, values(validationErrorsByProp));

  return { ...state, isValid };
}

function computeMustReenterPassword(state) {
  const { formState, serverData } = state;

  if (!serverData) {
    return { ...state, mustReenterPassword: false };
  }

  const isTextPropDirty = (prop) => formState[prop].trimmedValue !== (serverData[prop] || ''),
    severConfiguredFormats = new Set(filter((prop) => serverData[prop] === true, booleanProps)),
    textPropsDirty = any(isTextPropDirty, ['username', 'hostname', 'apiKey']),
    booleanPropsDirty = !equals(severConfiguredFormats, formState.configuredFormatState.formats),
    password = formState.password.value;

  return {
    ...state,
    mustReenterPassword: (textPropsDirty || booleanPropsDirty) && password === FAKE_PASSWORD,
  };
}

const updatedComputedProps = compose(
  computeHasAllRequiredData,
  computeIsDirty,
  computeIsValid,
  computeMustReenterPassword
);

function loadRequested() {
  return {
    ...initialState,
    loading: true,
  };
}

function loadFulfilled(state, { payload }) {
  return setFormStateFromServerData({
    ...state,
    loading: false,
    isDirty: false,
    ...clearedErrors,
    submitMaskState: initialState.submitMaskState,
    submitMaskMessage: initialState.submitMaskMessage,
    serverData: payload,
    mustReenterPassword: false,
  });
}

const resetForm = (state) => (state.serverData ? loadFulfilled(state, { payload: state.serverData }) : initialState);

function loadFailed(state, { payload }) {
  // 404 is fine, it just means there is no configuration
  const error = payload.response && payload.response.status === 404 ? null : payload;

  return {
    ...initialState,
    loading: false,
    ...clearedErrors,
    loadError: Messages.getHttpErrorMessage(error),
  };
}

function saveRequested(state) {
  return {
    ...state,
    submitMaskState: false,
    submitMaskMessage: SUBMIT_MASK_SAVING_MESSAGE,
    testConfigSuccess: false,
    ...clearedErrors,
  };
}

function saveFulfilled(state, { payload }) {
  return setFormStateFromServerData({
    ...state,
    loading: false,
    submitMaskState: true,
    isDirty: false,
    ...clearedErrors,
    serverData: payload,
  });
}

function saveFailed(state, { payload }) {
  return {
    ...state,
    loading: false,
    submitMaskState: null,
    ...clearedErrors,
    saveError: Messages.getHttpErrorMessage(payload),
  };
}

function deleteRequested(state) {
  return {
    ...state,
    submitMaskState: false,
    submitMaskMessage: SUBMIT_MASK_DELETING_MESSAGE,
    ...clearedErrors,
  };
}

function deleteFulfilled() {
  return { ...initialState, submitMaskState: true, showDeleteModal: false, ...clearedErrors };
}

function deleteFailed(state, { payload }) {
  return {
    ...state,
    loading: false,
    submitMaskState: null,
    ...clearedErrors,
    deleteError: Messages.getHttpErrorMessage(payload),
  };
}

function testConfigRequested(state) {
  return {
    ...state,
    submitMaskState: false,
    submitMaskMessage: SUBMIT_MASK_TEST_CONFIG_MESSAGE,
  };
}

function testConfigFulfilled(state) {
  return {
    ...state,
    submitMaskState: true,
    testConfigError: null,
    testConfigSuccess: true,
  };
}

function testConfigFailed(state, { payload }) {
  return {
    ...state,
    submitMaskState: null,
    testConfigError: Messages.getHttpErrorMessage(payload),
    testConfigSuccess: false,
  };
}

function setEulaCheckbox(state, { payload }) {
  const stateWithUpdatedValue = {
    ...state,
    formState: {
      ...state.formState,
      eula: {
        value: payload,
        isPristine: false,
        validationErrors: validateCheckbox(payload),
        disabled: false,
      },
    },
  };
  return updatedComputedProps(stateWithUpdatedValue);
}

const setConfiguredFormats = (state, { payload }) => {
  const stateWithUpdatedValue = {
    ...state,
    formState: {
      ...state.formState,
      configuredFormatState: {
        formats: payload,
        isPristine: false,
        validationErrors: payload.size > 0 ? null : 'At least one format must be selected',
      },
    },
  };
  return updatedComputedProps(stateWithUpdatedValue);
};

const setTextInput = curryN(4, function setTextInput(fieldName, validator, state, { payload }) {
  const stateWithUpdatedValue = pathSet(
    ['formState', fieldName],
    nxTextInputStateHelpers.userInput(validator, payload),
    state
  );

  return updatedComputedProps(stateWithUpdatedValue);
});

const load = createAsyncThunk(`${REDUCER_NAME}/load`, (_, { rejectWithValue }) => {
  return axios.get(getZScalerConfigUrl()).then(prop('data')).catch(rejectWithValue);
});

const save = createAsyncThunk(`${REDUCER_NAME}/save`, (_, { getState, dispatch, rejectWithValue }) => {
  const formState = getState().zscalerConfig.formState,
    serverData = toServerData(formState);

  return axios
    .put(getZScalerConfigUrl(), serverData)
    .then(() => {
      startSubmitMaskSuccessTimer(dispatch);
      return serverData;
    })
    .catch(rejectWithValue);
});

const del = createAsyncThunk(`${REDUCER_NAME}/delete`, (_, { dispatch, rejectWithValue }) => {
  return axios
    .delete(getZScalerConfigUrl())
    .then(() => {
      startSubmitMaskSuccessTimer(dispatch);
    })
    .catch(rejectWithValue);
});

const testConfig = createAsyncThunk(`${REDUCER_NAME}/testConfig`, (_, { getState, dispatch, rejectWithValue }) => {
  const formState = getState().zscalerConfig.formState,
    serverData = toServerData(formState);

  return axios
    .post(getZScalerTestConfigUrl(), serverData)
    .then(prop('data'))
    .then((data) => {
      startSubmitMaskSuccessTimer(dispatch);
      return data;
    })
    .catch(rejectWithValue);
});

function startSubmitMaskSuccessTimer(dispatch) {
  setTimeout(() => {
    dispatch(actions.submitMaskTimerDone());
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

function toServerData(formState) {
  // pull the trimmedValue out of the input state object and convert empty strings to null
  const textPropMapper = ({ trimmedValue }) => trimmedValue || null;

  return {
    ...map(textPropMapper, pick(['hostname', 'username', 'apiKey'], formState)),
    password: formState.password.value || null,
    eulaAgreed: formState.eula.value || false,
    ...fromPairs(
      ['mavenFormatEnabled', 'npmFormatEnabled', 'pypiFormatEnabled', 'nugetFormatEnabled'].map((key) => [
        key,
        formState.configuredFormatState.formats.has(key),
      ])
    ),
  };
}

const zscalerConfigSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    resetForm: resetForm,
    setUsername: setTextInput('username', validateNonEmpty),
    setPassword: setTextInput('password', validateNonEmpty),
    setHostname: setTextInput('hostname', validateZScalerHostName),
    setApiKey: setTextInput('apiKey', validateZscalerApiKey),
    setShowDeleteModal: propSet('showDeleteModal'),
    submitMaskTimerDone: propSetConst('submitMaskState', null),
    setEulaCheckbox: setEulaCheckbox,
    setConfiguredFormats: setConfiguredFormats,
  },
  extraReducers: {
    [load.pending]: loadRequested,
    [load.fulfilled]: loadFulfilled,
    [load.rejected]: loadFailed,
    [save.pending]: saveRequested,
    [save.fulfilled]: saveFulfilled,
    [save.rejected]: saveFailed,
    [del.pending]: deleteRequested,
    [del.fulfilled]: deleteFulfilled,
    [del.rejected]: deleteFailed,
    [testConfig.pending]: testConfigRequested,
    [testConfig.fulfilled]: testConfigFulfilled,
    [testConfig.rejected]: testConfigFailed,
  },
});

export default zscalerConfigSlice.reducer;
export const actions = {
  ...zscalerConfigSlice.actions,
  load,
  save,
  del,
  testConfig,
};
