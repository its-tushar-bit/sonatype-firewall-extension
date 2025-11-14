/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { always, equals } from 'ramda';
import { nxTextInputStateHelpers, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { getOidcConfigurationUrl } from '../../util/CLMLocation';
import { Messages } from '../../util/CommonServices';
import { SELECT_COMPONENT } from 'MainRoot/applicationReport/applicationReportActions';
import { selectConfigurationValues } from './oidcConfigurationSelectors';
import { propSetConst } from '../../util/reduxToolkitUtil';
import { checkPermissions } from '../../util/authorizationUtil';
import { toggleBooleanProp } from '../../util/reduxUtil';

const REDUCER_NAME = 'oidcConfiguration';

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
    // OAuth2 Configuration fields
    oauth2IdpJwksUrl: initUserInput(''),
    oauth2IdpJwsAlgorithm: initUserInput(''),
    oauth2IdpJwks: initUserInput(''),
    oauth2UsernameClaim: initUserInput(''),
    oauth2FirstNameClaim: initUserInput(''),
    oauth2LastNameClaim: initUserInput(''),
    oauth2EmailClaim: initUserInput(''),
    oauth2GroupsClaim: initUserInput(''),
    oauth2ExactMatchClaimsJson: initUserInput(''),
    // OIDC Configuration fields - single idpIssuer used for both
    oidcIdpIssuer: initUserInput(''),
    oidcClientId: initUserInput(''),
    oidcClientSecret: initUserInput(''),
    oidcIdpAuthorizationUrl: initUserInput(''),
    oidcIdpTokenUrl: initUserInput(''),
    oidcAuthorizationCustomParamsJson: initUserInput(''),
    oidcTokenRequestCustomParamsJson: initUserInput(''),
  },
  loadedConfigurationValues: null,
});

const getRestartedConfigurationValues = (data) => ({
  // OAuth2 Configuration
  oauth2IdpJwksUrl: initUserInput(data.oauth2Configuration?.idpJwksUrl || ''),
  oauth2IdpJwsAlgorithm: initUserInput(data.oauth2Configuration?.idpJwsAlgorithm || ''),
  oauth2IdpJwks: initUserInput(data.oauth2Configuration?.idpJwks || ''),
  oauth2UsernameClaim: initUserInput(data.oauth2Configuration?.usernameClaim || ''),
  oauth2FirstNameClaim: initUserInput(data.oauth2Configuration?.firstNameClaim || ''),
  oauth2LastNameClaim: initUserInput(data.oauth2Configuration?.lastNameClaim || ''),
  oauth2EmailClaim: initUserInput(data.oauth2Configuration?.emailClaim || ''),
  oauth2GroupsClaim: initUserInput(data.oauth2Configuration?.groupsClaim || ''),
  oauth2ExactMatchClaimsJson: initUserInput(data.oauth2Configuration?.exactMatchClaimsJson || ''),
  // OIDC Configuration - single idpIssuer for both OIDC and OAuth2
  oidcIdpIssuer: initUserInput(data.oidcConfiguration?.idpIssuer || ''),
  oidcClientId: initUserInput(data.oidcConfiguration?.clientId || ''),
  oidcClientSecret: initUserInput(data.oidcConfiguration?.clientSecret || ''),
  oidcIdpAuthorizationUrl: initUserInput(data.oidcConfiguration?.idpAuthorizationUrl || ''),
  oidcIdpTokenUrl: initUserInput(data.oidcConfiguration?.idpTokenUrl || ''),
  oidcAuthorizationCustomParamsJson: initUserInput(data.oidcConfiguration?.authorizationCustomParamsJson || ''),
  oidcTokenRequestCustomParamsJson: initUserInput(data.oidcConfiguration?.tokenRequestCustomParamsJson || ''),
});

const getConfigurationPayload = (configurationValues) => ({
  oauth2Configuration: {
    // Use the single idpIssuer for both OIDC and OAuth2
    idpIssuer: configurationValues.oidcIdpIssuer.value,
    idpJwksUrl: configurationValues.oauth2IdpJwksUrl.value,
    idpJwsAlgorithm: configurationValues.oauth2IdpJwsAlgorithm.value,
    idpJwks: configurationValues.oauth2IdpJwks.value,
    usernameClaim: configurationValues.oauth2UsernameClaim.value,
    firstNameClaim: configurationValues.oauth2FirstNameClaim.value,
    lastNameClaim: configurationValues.oauth2LastNameClaim.value,
    emailClaim: configurationValues.oauth2EmailClaim.value,
    groupsClaim: configurationValues.oauth2GroupsClaim.value,
    exactMatchClaimsJson: configurationValues.oauth2ExactMatchClaimsJson.value,
  },
  oidcConfiguration: {
    idpIssuer: configurationValues.oidcIdpIssuer.value,
    clientId: configurationValues.oidcClientId.value,
    clientSecret: configurationValues.oidcClientSecret.value,
    idpAuthorizationUrl: configurationValues.oidcIdpAuthorizationUrl.value,
    idpTokenUrl: configurationValues.oidcIdpTokenUrl.value,
    authorizationCustomParamsJson: configurationValues.oidcAuthorizationCustomParamsJson.value,
    tokenRequestCustomParamsJson: configurationValues.oidcTokenRequestCustomParamsJson.value,
  },
});

// No-op validator - backend handles all validation
const noOpValidator = () => null;

const computeIsDirty = (newConfigurationValues, loadedConfigurationValues) => {
  const oldConfigurationValues = loadedConfigurationValues || initialState.configurationValues;

  const configKeys = Object.keys(oldConfigurationValues);
  const newConfigVal = {},
    oldConfigVal = {};

  // Get values from the new form state and old state to compare if there are any changes
  configKeys.forEach((k) => {
    newConfigVal[k] = newConfigurationValues[k].value;
    oldConfigVal[k] = oldConfigurationValues[k].value;
  });

  return !equals(newConfigVal, oldConfigVal);
};

const onOidcConfigurationValueChange = (value, name) => {
  return (dispatch) => {
    dispatch(actions.setConfigurationValues({ value, name }));
  };
};

const onRestoreConfigurationValue = (name) => {
  return (dispatch) => {
    dispatch(
      actions.setConfigurationValues({
        value: initialState.configurationValues[name].value,
        name,
      })
    );
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
  state.configurationValues = state.loadedConfigurationValues || initialState.configurationValues;
};

const setConfigurationValues = (state, { payload: { name, value } }) => {
  // No frontend validation - backend handles all validation
  state.configurationValues[name] = userInput(noOpValidator, value);
  state.isDirty = computeIsDirty(state.configurationValues, state.loadedConfigurationValues);
};

const loadOidcConfiguration = createAsyncThunk(`${REDUCER_NAME}/loadOidcConfiguration`, (_, { rejectWithValue }) => {
  return checkPermissions(['CONFIGURE_SYSTEM'])
    .then(() => axios.get(getOidcConfigurationUrl()))
    .then((response) => response)
    .catch(rejectWithValue);
});

const updateOidcConfiguration = createAsyncThunk(
  `${REDUCER_NAME}/updateOidcConfiguration`,
  (_, { rejectWithValue, getState, dispatch }) => {
    const configurationValues = selectConfigurationValues(getState());
    const payload = getConfigurationPayload(configurationValues);

    return axios
      .put(getOidcConfigurationUrl(), payload)
      .then((response) => {
        startMaskSuccessTimer(dispatch);
        return response;
      })
      .catch(rejectWithValue);
  }
);

const deleteOidcConfiguration = createAsyncThunk(
  `${REDUCER_NAME}/deleteOidcConfiguration`,
  (_, { rejectWithValue, dispatch }) => {
    return axios
      .delete(getOidcConfigurationUrl())
      .then((response) => {
        startMaskSuccessTimer(dispatch);
        return response;
      })
      .catch(rejectWithValue);
  }
);

const loadOidcConfigurationFulfilled = (state, { payload: { data } }) => {
  const latestConfigurationValues = getRestartedConfigurationValues(data);
  state.loadError = null;
  state.isConfigured = true;
  state.configurationValues = latestConfigurationValues;
  state.loadedConfigurationValues = latestConfigurationValues;
  state.isLoading = false;
};

const loadOidcConfigurationFailed = (state, { payload }) => {
  state.loadError = payload?.response?.status === 404 ? null : Messages.getHttpErrorMessage(payload);
  state.isConfigured = false;
  state.isLoading = false;
};

const updateOidcConfigurationFulfilled = (state) => {
  const configurationPayload = getConfigurationPayload(state.configurationValues);
  // Mask the client secret to match what the backend would return on GET
  // This prevents double encryption if the user clicks Save again without making changes
  if (configurationPayload.oidcConfiguration) {
    configurationPayload.oidcConfiguration.clientSecret = '*******';
  }
  const latestConfigurationValues = getRestartedConfigurationValues(configurationPayload);
  state.submitMaskError = null;
  state.isConfigured = true;
  state.loadedConfigurationValues = latestConfigurationValues;
  state.configurationValues = latestConfigurationValues;
  state.submitState = true;
  state.isDirty = false;
};

const updateOidcConfigurationFailed = (state, { payload }) => {
  state.submitMaskError = Messages.getHttpErrorMessage(payload);
  state.submitState = null;
};

const deleteOidcConfigurationRequested = (state) => {
  state.submitState = false;
  state.isDeleteModalShown = false;
};

const deleteOidcConfigurationFulfilled = (state) => {
  state.submitMaskError = null;
  state.isConfigured = false;
  state.configurationValues = initialState.configurationValues;
  state.loadedConfigurationValues = null;
  state.submitState = true;
  state.isDirty = false;
};

const deleteOidcConfigurationFailed = (state, { payload }) => {
  state.submitMaskError = Messages.getHttpErrorMessage(payload);
  state.submitState = null;
};

const oidcConfigurationSlice = createSlice({
  name: REDUCER_NAME,
  initialState: initialState,
  reducers: {
    setConfigurationValues,
    restoreDefaultConfigurationValues,
    maskTimerDone: propSetConst('submitState', null),
    toggleDeleteModal: toggleBooleanProp('isDeleteModalShown'),
  },
  extraReducers: {
    [loadOidcConfiguration.pending]: propSetConst('isLoading', true),
    [loadOidcConfiguration.fulfilled]: loadOidcConfigurationFulfilled,
    [loadOidcConfiguration.rejected]: loadOidcConfigurationFailed,

    [updateOidcConfiguration.pending]: propSetConst('submitState', false),
    [updateOidcConfiguration.fulfilled]: updateOidcConfigurationFulfilled,
    [updateOidcConfiguration.rejected]: updateOidcConfigurationFailed,

    [deleteOidcConfiguration.pending]: deleteOidcConfigurationRequested,
    [deleteOidcConfiguration.fulfilled]: deleteOidcConfigurationFulfilled,
    [deleteOidcConfiguration.rejected]: deleteOidcConfigurationFailed,

    [SELECT_COMPONENT]: always(initialState),
  },
});

export default oidcConfigurationSlice.reducer;
export const actions = {
  ...oidcConfigurationSlice.actions,
  loadOidcConfiguration,
  updateOidcConfiguration,
  deleteOidcConfiguration,
  onOidcConfigurationValueChange,
  onRestoreConfigurationValues,
  onRestoreConfigurationValue,
};
