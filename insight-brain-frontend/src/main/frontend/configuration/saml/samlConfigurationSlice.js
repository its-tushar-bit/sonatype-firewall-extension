/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { always, omit, equals } from 'ramda';
import { nxTextInputStateHelpers, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { uriTemplate } from '../../util/urlUtil';
import { getSamlConfigurationUrl } from '../../util/CLMLocation';
import { Messages } from '../../utilAngular/CommonServices';
import { SELECT_COMPONENT } from 'MainRoot/applicationReport/applicationReportActions';
import { selectConfigurationValues } from './samlConfigurationSelectors';
import { propSetConst } from '../../util/reduxToolkitUtil';
import { checkPermissions } from '../../util/authorizationUtil';
import { toggleBooleanProp } from '../../util/reduxUtil';

const REDUCER_NAME = 'samlConfiguration';

const { initialState: initUserInput, userInput } = nxTextInputStateHelpers;

export const initialState = Object.freeze({
  isLoading: false,
  submitState: null,
  submitMaskError: null,
  loadError: null,
  isConfigured: false,
  isDeleteModalShown: false,
  isDirty: false,
  configurationValues: {
    identityProviderName: initUserInput('identity provider'),
    entityId: initUserInput(uriTemplate`/api/v2/config/saml/metadata`),
    usernameAttributeName: initUserInput('username'),
    firstNameAttributeName: initUserInput('firstName'),
    lastNameAttributeName: initUserInput('lastName'),
    emailAttributeName: initUserInput('email'),
    groupsAttributeName: initUserInput('groups'),
    identityProviderMetadataXml: initUserInput(''),
    validateResponseSignature: 'null',
    validateAssertionSignature: 'null',
  },
  loadedConfigurationValues: null,
});

const getRestartedConfigurationValues = (configurationValues) => ({
  emailAttributeName: initUserInput(configurationValues.emailAttributeName),
  entityId: initUserInput(configurationValues.entityId),
  firstNameAttributeName: initUserInput(configurationValues.firstNameAttributeName),
  groupsAttributeName: initUserInput(configurationValues.groupsAttributeName),
  identityProviderMetadataXml: initUserInput(configurationValues.identityProviderMetadataXml),
  identityProviderName: initUserInput(configurationValues.identityProviderName),
  lastNameAttributeName: initUserInput(configurationValues.lastNameAttributeName),
  usernameAttributeName: initUserInput(configurationValues.usernameAttributeName),
  validateAssertionSignature: `${configurationValues.validateAssertionSignature}`,
  validateResponseSignature: `${configurationValues.validateResponseSignature}`,
});

const getConfigurationValues = (configurationValues) => ({
  identityProviderName: configurationValues.identityProviderName.value,
  entityId: configurationValues.entityId.value,
  usernameAttributeName: configurationValues.usernameAttributeName.value,
  firstNameAttributeName: configurationValues.firstNameAttributeName.value,
  lastNameAttributeName: configurationValues.lastNameAttributeName.value,
  emailAttributeName: configurationValues.emailAttributeName.value,
  groupsAttributeName: configurationValues.groupsAttributeName.value,
  identityProviderMetadataXml: configurationValues.identityProviderMetadataXml.value,
  validateResponseSignature: configurationValues.validateResponseSignature,
  validateAssertionSignature: configurationValues.validateAssertionSignature,
});

const configurationValuesValidator = (value) => (value.length ? null : 'Required field');

const computeIsDirty = (newConfigurationValues, loadedConfigurationValues) => {
  const oldConfigurationValues = loadedConfigurationValues
    ? loadedConfigurationValues
    : initialState.configurationValues;

  const configKeys = Object.keys(oldConfigurationValues);
  const newConfigVal = {},
    oldConfigVal = {},
    selectFieldsKeys = new Set(['validateResponseSignature', 'validateAssertionSignature']);

  // Get values from the new form state and old state to compare if there are any changes
  configKeys.forEach((k) => {
    newConfigVal[k] = selectFieldsKeys.has(k) ? newConfigurationValues[k] : newConfigurationValues[k].value;
    oldConfigVal[k] = selectFieldsKeys.has(k) ? oldConfigurationValues[k] : oldConfigurationValues[k].value;
  });

  return !equals(newConfigVal, oldConfigVal);
};

const onSAMLConfigurationValueChange = (value, name) => {
  return (dispatch) => {
    dispatch(actions.setConfigurationValues({ value, name }));
  };
};

const onSAMLConfigurationSelectValueChange = (value, name) => {
  return (dispatch) => {
    dispatch(actions.setSelectConfigurationValues({ value, name }));
  };
};

const onRestoreConfigurationValue = (name) => {
  return (dispatch) => {
    dispatch(actions.onSAMLConfigurationValueChange(initialState.configurationValues[name].value, name));
  };
};

const onRestoreConfigurationValues = () => {
  return (dispatch) => {
    dispatch(actions.restoreDefaultConfigurationValues());
  };
};

const startMaskSuccessTimer = (dispatch) => {
  setTimeout(() => {
    dispatch(actions.maskTimerDone());
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
};

const restoreDefaultConfigurationValues = (state) => {
  state.isDirty = false;
  state.configurationValues = state.loadedConfigurationValues
    ? state.loadedConfigurationValues
    : initialState.configurationValues;
};

const setConfigurationValues = (state, { payload: { name, value } }) => {
  state.configurationValues[name] = userInput(configurationValuesValidator, value);
  state.isDirty = computeIsDirty(state.configurationValues, state.loadedConfigurationValues);
};

const setSelectConfigurationValues = (state, { payload: { name, value } }) => {
  state.configurationValues[name] = value;
  state.isDirty = computeIsDirty(state.configurationValues, state.loadedConfigurationValues);
};

const loadSAMLConfiguration = createAsyncThunk(`${REDUCER_NAME}/loadSAMLConfiguration`, (_, { rejectWithValue }) => {
  return checkPermissions(['CONFIGURE_SYSTEM'])
    .then(() => axios.get(getSamlConfigurationUrl()))
    .then((response) => response)
    .catch(rejectWithValue);
});

const updateSAMLConfiguration = createAsyncThunk(
  `${REDUCER_NAME}/updateSAMLConfiguration`,
  (_, { rejectWithValue, getState, dispatch }) => {
    const configurationValues = selectConfigurationValues(getState());
    const formData = new FormData();
    const payload = omit(['identityProviderMetadataXml'], getConfigurationValues(configurationValues));
    formData.append('identityProviderXml', configurationValues.identityProviderMetadataXml.value);
    formData.append('samlConfiguration', JSON.stringify(payload));

    return axios
      .put(getSamlConfigurationUrl(), formData)
      .then((response) => {
        startMaskSuccessTimer(dispatch);
        return response;
      })
      .catch(rejectWithValue);
  }
);

const deleteSAMLConfiguration = createAsyncThunk(
  `${REDUCER_NAME}/deleteSAMLConfiguration`,
  (_, { rejectWithValue, dispatch }) => {
    return axios
      .delete(getSamlConfigurationUrl())
      .then((response) => {
        startMaskSuccessTimer(dispatch);
        return response;
      })
      .catch(rejectWithValue);
  }
);

const loadSAMLConfigurationFulfilled = (state, { payload: { data } }) => {
  const latestConfigurationValues = getRestartedConfigurationValues(data);
  state.loadError = null;
  state.isConfigured = true;
  state.configurationValues = latestConfigurationValues;
  state.loadedConfigurationValues = latestConfigurationValues;
  state.isLoading = false;
};

const loadSAMLConfigurationFailed = (state, { payload }) => {
  state.loadError = payload.response.status === 404 ? null : Messages.getHttpErrorMessage(payload);
  state.isConfigured = false;
  state.isLoading = false;
};

const updateSAMLConfigurationFulfilled = (state) => {
  const configurationValues = getConfigurationValues(state.configurationValues);
  const latestConfigurationValues = getRestartedConfigurationValues(configurationValues);
  state.submitMaskError = null;
  state.isConfigured = true;
  state.loadedConfigurationValues = latestConfigurationValues;
  state.configurationValues = latestConfigurationValues;
  state.submitState = true;
  state.isDirty = false;
};

const updateSAMLConfigurationFailed = (state, { payload }) => {
  state.submitMaskError = Messages.getHttpErrorMessage(payload);
  state.submitState = null;
};

const deleteSAMLConfigurationRequested = (state) => {
  state.submitState = false;
  state.isDeleteModalShown = false;
};

const deleteSAMLConfigurationFulfilled = (state) => {
  state.submitMaskError = null;
  state.isConfigured = false;
  state.configurationValues = initialState.configurationValues;
  state.loadedConfigurationValues = null;
  state.submitState = true;
  state.isDirty = false;
};

const deleteSAMLConfigurationFailed = (state, { payload }) => {
  state.submitMaskError = Messages.getHttpErrorMessage(payload);
  state.submitState = null;
};

const samlConfigurationSlice = createSlice({
  name: REDUCER_NAME,
  initialState: initialState,
  reducers: {
    setConfigurationValues,
    setSelectConfigurationValues,
    restoreDefaultConfigurationValues,
    maskTimerDone: propSetConst('submitState', null),
    toggleDeleteModal: toggleBooleanProp('isDeleteModalShown'),
  },
  extraReducers: {
    [loadSAMLConfiguration.pending]: propSetConst('isLoading', true),
    [loadSAMLConfiguration.fulfilled]: loadSAMLConfigurationFulfilled,
    [loadSAMLConfiguration.rejected]: loadSAMLConfigurationFailed,

    [updateSAMLConfiguration.pending]: propSetConst('submitState', false),
    [updateSAMLConfiguration.fulfilled]: updateSAMLConfigurationFulfilled,
    [updateSAMLConfiguration.rejected]: updateSAMLConfigurationFailed,

    [deleteSAMLConfiguration.pending]: deleteSAMLConfigurationRequested,
    [deleteSAMLConfiguration.fulfilled]: deleteSAMLConfigurationFulfilled,
    [deleteSAMLConfiguration.rejected]: deleteSAMLConfigurationFailed,

    [SELECT_COMPONENT]: always(initialState),
  },
});

export default samlConfigurationSlice.reducer;
export const actions = {
  ...samlConfigurationSlice.actions,
  loadSAMLConfiguration,
  updateSAMLConfiguration,
  deleteSAMLConfiguration,
  onSAMLConfigurationValueChange,
  onSAMLConfigurationSelectValueChange,
  onRestoreConfigurationValues,
  onRestoreConfigurationValue,
};
