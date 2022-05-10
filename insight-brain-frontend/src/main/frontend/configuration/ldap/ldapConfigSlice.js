/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { nxTextInputStateHelpers, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { compose, clone, always, curryN, pick, any, complement, eqProps, __, omit, isNil } from 'ramda';
import { Messages } from '../../utilAngular/CommonServices';
import { isNilOrEmpty, pathSet } from '../../util/jsUtil';
import { stateGo } from '../../reduxUiRouter/routerActions';
import {
  getServerNameRequest,
  getConnectionData,
  getSettingsData,
  getRequiredFieldNames,
  getNewPortValue,
  modesFieldsMap,
  modes,
  getInitUserInputForFields,
  constructInitialInputFields,
} from './ldapConfigSliceUtil';
import {
  validateForm,
  combineValidators,
  validateNonEmpty,
  validateNameCharacters,
  validateMinMax,
} from '../../util/validationUtil';
import { propSetConst } from '../../util/reduxToolkitUtil';
import {
  getLdapConfigUrl,
  getLdapConnectionConfig,
  getLdapConnectionTest,
  getLdapLoginTest,
  getLdapUserMappingConfig,
  getLdapUserMappingTest,
} from '../../util/CLMLocation';
import { checkPermissions } from '../../util/authorizationUtil';

const { initialState: initUserInput, userInput } = nxTextInputStateHelpers;

const REDUCER_NAME = 'ldapConfig';
export const initialState = {
  serverData: {},
  inputFields: {
    serverName: initUserInput(''),

    protocol: 'LDAP',
    port: initUserInput('389'),
    hostname: initUserInput(''),
    searchBase: initUserInput(''),
    referralIgnored: false,
    authenticationMethod: 'NONE',
    saslRealm: initUserInput(''),
    systemUsername: initUserInput(''),
    systemPassword: initUserInput(''),
    connectionTimeout: initUserInput('30'),
    retryDelay: initUserInput('30'),

    userBaseDN: initUserInput(''),
    userSubtree: false,
    userObjectClass: initUserInput(''),
    userFilter: initUserInput(''),
    userIDAttribute: initUserInput(''),
    userRealNameAttribute: initUserInput(''),
    userEmailAttribute: initUserInput(''),
    userPasswordAttribute: initUserInput(''),
    groupMappingType: 'NONE',
    groupBaseDN: initUserInput(''),
    groupSubtree: false,
    groupObjectClass: initUserInput(''),
    groupIDAttribute: initUserInput(''),
    groupMemberAttribute: initUserInput(''),
    groupMemberFormat: initUserInput(''),
    userMemberOfGroupAttribute: initUserInput(''),
    dynamicGroupSearchEnabled: true,
  },
  reservedSystemPassword: null,
  isDirty: false,
  loading: false,
  loadError: null,
  saveError: null,
  submitMaskMessage: 'Saving…',
  saveMaskState: null,
  validationError: null,
  mustReenterPassword: false,
  successMessage: '',
  testConnectionErrorMessage: '',
  removeError: null,
  removeMaskState: null,
  userMapping: {
    isLdapUserMappingModalOpen: false,
    userList: null,
    loadError: null,
    sortAscending: true,
  },
  checkLoginProps: {
    username: initUserInput('', validateNonEmpty),
    password: initUserInput('', validateNonEmpty),
    checkLoginSuccess: false,
    checkLoginError: null,
  },
};

const clearedErrors = pick(['loadError', 'saveError'], initialState);

function loadAddPageFulfilled(state) {
  return {
    ...state,
    loading: false,
    ...clearedErrors,
  };
}

function loadAddPageFailed(state, { payload }) {
  return {
    ...state,
    loading: false,
    ...clearedErrors,
    loadError: Messages.getHttpErrorMessage(payload),
  };
}

function computeInitialConnectionTab(state) {
  const {
    serverData: { mapping, connection, server },
  } = state;
  return updatedComputedProps({
    ...state,
    reservedSystemPassword: connection.systemPassword,
    inputFields: {
      ...state.inputFields,
      ...constructInitialInputFields({ mapping, connection, server }),
    },
  });
}

function computeInitialMappingTab(state) {
  const {
    serverData: { mapping, connection, server },
  } = state;
  return updatedUserAndGroupComputedProps({
    ...state,
    inputFields: {
      ...state.inputFields,
      ...constructInitialInputFields({ mapping, connection, server }),
    },
  });
}

function loadEditPageFulfilled(state, { payload }) {
  const { server, connection, mapping } = payload;
  const newState = {
    ...state,
    loading: false,
    reservedSystemPassword: connection.systemPassword,
    ...clearedErrors,
    inputFields: {
      ...state.inputFields,
      ...constructInitialInputFields({ server, connection, mapping }),
    },
    serverData: {
      ...state.serverData,
      ...payload,
      serverName: server.name,
    },
  };

  if (payload.currentTab === 'edit-ldap-connection') {
    return updatedComputedProps(newState);
  } else {
    return updatedUserAndGroupComputedProps(newState);
  }
}

function loadEditPageFailed(state, { payload }) {
  return {
    ...state,
    loading: false,
    ...clearedErrors,
    loadError: Messages.getHttpErrorMessage(payload),
  };
}

function saveFulfilled(state, { payload }) {
  return {
    ...state,
    loading: false,
    saveMaskState: true,
    isDirty: false,
    ...clearedErrors,
    serverData: {
      ...state.serverData,
      server: payload,
      serverName: payload.name,
    },
    inputFields: {
      ...initialState.inputFields,
    },
  };
}

function saveFailed(state, { payload }) {
  return {
    ...state,
    loading: false,
    saveMaskState: null,
    ...clearedErrors,
    saveError: Messages.getHttpErrorMessage(payload),
  };
}

function connectionPending(state) {
  return {
    ...state,
    saveMaskState: false,
    submitMaskMessage: 'Saving…',
    ...clearedErrors,
  };
}

function saveConnectionFulfilled(state, { payload }) {
  return updatedComputedProps({
    ...state,
    saveMaskState: true,
    isDirty: false,
    successMessage: 'Configuration saved.',
    reservedSystemPassword: payload.systemPassword,
    ...clearedErrors,
    inputFields: {
      ...state.inputFields,
      serverName: initUserInput(payload.server.name),
      ...getInitUserInputForFields(
        [
          'port',
          'hostname',
          'searchBase',
          'saslRealm',
          'systemUsername',
          'systemPassword',
          'connectionTimeout',
          'retryDelay',
        ],
        payload
      ),
      protocol: payload.protocol,
      referralIgnored: payload.referralIgnored,
      authenticationMethod: payload.authenticationMethod,
    },
    serverData: {
      ...state.serverData,
      connection: omit(['server'], payload),
      server: payload.server,
      serverName: payload.server.name,
    },
  });
}

function saveUserAndGroupSettingsFulfilled(state, { payload }) {
  return updatedUserAndGroupComputedProps({
    ...state,
    saveMaskState: true,
    isDirty: false,
    successMessage: 'Configuration saved.',
    ...clearedErrors,
    inputFields: {
      ...state.inputFields,
      serverName: initUserInput(payload.server.name),
      ...getInitUserInputForFields(
        [
          'userBaseDN',
          'userObjectClass',
          'userFilter',
          'userIDAttribute',
          'userRealNameAttribute',
          'userEmailAttribute',
          'userPasswordAttribute',
          'groupBaseDN',
          'groupObjectClass',
          'groupIDAttribute',
          'groupMemberAttribute',
          'groupMemberFormat',
          'userMemberOfGroupAttribute',
        ],
        payload
      ),
      userSubtree: payload.userSubtree,
      groupMappingType: payload.groupMappingType,
      groupSubtree: payload.groupSubtree,
      dynamicGroupSearchEnabled: payload.dynamicGroupSearchEnabled,
    },
    serverData: {
      ...state.serverData,
      mapping: omit(['server'], payload),
      server: payload.server,
      serverName: payload.server.name,
    },
  });
}

function connectionFailed(state, { payload }) {
  return {
    ...state,
    saveError: Messages.getHttpErrorMessage(payload),
    saveMaskState: null,
  };
}

function testConnectionPending(state) {
  return {
    ...state,
    submitMaskMessage: 'Testing…',
    saveMaskState: false,
    ...clearedErrors,
  };
}

function testConnectionFulfilled(state) {
  return {
    ...state,
    saveMaskState: true,
    successMessage: 'Success!',
  };
}

function testConnectionFailed(state, { payload }) {
  return {
    ...state,
    saveMaskState: null,
    testConnectionErrorMessage: Messages.getHttpErrorMessage(payload),
  };
}

function resetAlertMessages(state) {
  return {
    ...state,
    testConnectionErrorMessage: '',
    successMessage: '',
    saveError: null,
    saveMaskState: null,
  };
}

function resetAllNotifications(state) {
  const newState = resetAlertMessages(state);
  return propSetConst('mustReenterPassword', false, newState);
}

function setInitialAddForm(state) {
  return {
    ...state,
    serverData: {
      ...state.serverData,
      serverName: '',
    },
    inputFields: {
      ...state.inputFields,
      serverName: initUserInput(''),
    },
  };
}

function removeServerFulfilled(state) {
  return {
    ...state,
    removeMaskState: true,
    removeError: null,
    isDirty: false,
  };
}

function removeServerFailed(state, { payload }) {
  return {
    ...state,
    removeMaskState: null,
    removeError: Messages.getHttpErrorMessage(payload),
  };
}

function setMethod(state, { payload }) {
  if (payload === 'NONE') {
    return updatedComputedProps(
      pathSet(['inputFields', 'authenticationMethod'], payload, {
        ...state,
        inputFields: {
          ...state.inputFields,
          saslRealm: initUserInput(''),
          systemUsername: initUserInput(''),
          systemPassword: initUserInput(''),
        },
        mustReenterPassword: false,
      })
    );
  }
  return updatedComputedProps(pathSet(['inputFields', 'authenticationMethod'], payload, state));
}

function setProtocol(state, { payload }) {
  let { port } = state.inputFields;
  const { port: computedPort, changed } = getNewPortValue(payload, port);

  if (changed) {
    return compose(
      updatedComputedProps,
      computeMustReenterPassword
    )({
      ...state,
      inputFields: {
        ...state.inputFields,
        protocol: payload,
        port: computedPort,
      },
    });
  }

  return updatedComputedProps({
    ...state,
    inputFields: {
      ...state.inputFields,
      protocol: payload,
      port: computedPort,
    },
  });
}

function setReferralIgnored(state) {
  const newState = pathSet(['inputFields', 'referralIgnored'], !state.inputFields.referralIgnored, state);
  return updatedComputedProps(newState);
}

function setGroupMappingType(state, { payload }) {
  const {
    serverData: { mapping },
  } = state;
  return updatedUserAndGroupComputedProps(
    pathSet(['inputFields', 'groupMappingType'], payload, {
      ...state,
      inputFields: {
        ...state.inputFields,
        groupBaseDN: initUserInput(mapping.groupBaseDN || ''),
        groupSubtree: mapping.groupSubtree,
        groupObjectClass: initUserInput(mapping.groupObjectClass || ''),
        groupIDAttribute: initUserInput(mapping.groupIDAttribute || ''),
        groupMemberAttribute: initUserInput(mapping.groupMemberAttribute || ''),
        groupMemberFormat: initUserInput(mapping.groupMemberFormat || ''),
        userMemberOfGroupAttribute: initUserInput(mapping.userMemberOfGroupAttribute || ''),
        dynamicGroupSearchEnabled: mapping.dynamicGroupSearchEnabled,
      },
    })
  );
}

function computeCreateIsDirty(state) {
  const {
    inputFields: { serverName },
    serverData,
  } = state;

  let isTextPropDirty = serverName.trimmedValue !== '';

  if (!isNilOrEmpty(serverData.server)) {
    isTextPropDirty = serverName.trimmedValue !== (serverData.server.name || '');
  }

  return propSetConst('isDirty', isTextPropDirty, state);
}

function computeUserGroupIsDirty(state) {
  const {
    inputFields,
    serverData: { mapping, server },
  } = state;
  const names = modesFieldsMap.get(modes.EDIT_MAPPING);
  const fields = pick(names, inputFields);

  if (!isNilOrEmpty(mapping)) {
    const isTextPropDirty = (prop) => fields[prop].trimmedValue !== (mapping[prop] || '');

    const textPropsDirty = any(isTextPropDirty, [
      'userBaseDN',
      'userObjectClass',
      'userFilter',
      'userIDAttribute',
      'userRealNameAttribute',
      'userEmailAttribute',
      'userPasswordAttribute',
      'groupBaseDN',
      'groupObjectClass',
      'groupIDAttribute',
      'groupMemberAttribute',
      'groupMemberFormat',
      'userMemberOfGroupAttribute',
    ]);
    const comboPropsDirty = any((prop) => fields[prop] !== mapping[prop], ['groupMappingType']);
    const booleanPropsDirty = any(complement(eqProps(__, fields, mapping)), [
      'userSubtree',
      'dynamicGroupSearchEnabled',
      'groupSubtree',
    ]);
    const isServerNameDirty = fields.serverName.trimmedValue !== server.name;

    return propSetConst('isDirty', isServerNameDirty || textPropsDirty || booleanPropsDirty || comboPropsDirty, state);
  }

  return state;
}

function computeIsDirty(state) {
  const {
    inputFields,
    serverData: { connection, server },
  } = state;
  const names = modesFieldsMap.get(modes.EDIT_CONNECTION);
  const fields = pick(names, inputFields);

  if (!isNilOrEmpty(connection)) {
    const isTextPropDirty = (prop) => fields[prop].trimmedValue !== (connection[prop] || '');
    const isNumberPropDirty = (prop) => fields[prop].trimmedValue !== (connection[prop].toString() || '');
    const isComboPropDirty = (prop) => fields[prop] !== connection[prop];

    const textPropsDirty = any(isTextPropDirty, [
      'hostname',
      'searchBase',
      'saslRealm',
      'systemUsername',
      'systemPassword',
    ]);
    const numberPropsDirty = any(isNumberPropDirty, ['port', 'connectionTimeout', 'retryDelay']);
    const comboPropsDirty = any(isComboPropDirty, ['protocol', 'authenticationMethod']);
    const booleanPropsDirty = any(complement(eqProps(__, fields, connection)), ['referralIgnored']);
    const isServerNameDirty = fields.serverName.trimmedValue !== server.name;

    return propSetConst(
      'isDirty',
      isServerNameDirty || textPropsDirty || booleanPropsDirty || numberPropsDirty || comboPropsDirty,
      state
    );
  }

  return state;
}

const computeValidationError = curryN(2, function computeValidationError(mode, state) {
  const { inputFields } = state;

  const names = getRequiredFieldNames(state, mode);
  const fields = pick(names, inputFields);
  const validationError = validateForm(clone(fields));

  return pathSet(['validationError'], validationError, state);
});

function computeMustReenterPassword(state) {
  const {
    inputFields,
    serverData: { connection },
    reservedSystemPassword,
  } = state;
  const { authenticationMethod, port, hostname, systemPassword } = pick(
    ['authenticationMethod', 'port', 'hostname', 'systemPassword'],
    inputFields
  );

  if (authenticationMethod !== 'NONE') {
    const hostNameWasGivenInitialy = !isNilOrEmpty(connection.hostname);
    const portOrHostNameWereChanged =
      port.trimmedValue !== connection.port.toString() || hostname.trimmedValue !== connection.hostname;
    const passwordWasNotChanged =
      connection.systemPassword === systemPassword.value || connection.systemPassword === reservedSystemPassword;

    const currentSystemPassword = systemPassword.value || reservedSystemPassword;

    if (hostNameWasGivenInitialy && portOrHostNameWereChanged && passwordWasNotChanged) {
      const newState = pathSet(['inputFields', 'systemPassword'], userInput(validateNonEmpty, ''), state);
      return propSetConst('mustReenterPassword', true, newState);
    }

    if (isNilOrEmpty(connection.systemPassword)) {
      return propSetConst('mustReenterPassword', false, state);
    }

    const newState = pathSet(
      ['inputFields', 'systemPassword'],
      userInput(
        validateNonEmpty,
        isNil(currentSystemPassword) ? connection.systemPassword || '' : currentSystemPassword
      ),
      state
    );
    return propSetConst('mustReenterPassword', false, newState);
  }

  return state;
}

function checkLoginFailed(state, { payload }) {
  return {
    ...state,
    checkLoginProps: {
      ...state.checkLoginProps,
      checkLoginSuccess: false,
      checkLoginError: payload,
    },
  };
}

function checkLoginSuccess(state) {
  return {
    ...state,
    checkLoginProps: {
      ...state.checkLoginProps,
      checkLoginSuccess: true,
      checkLoginError: null,
    },
  };
}

const updatedComputedProps = compose(computeIsDirty, computeValidationError(modes.EDIT_CONNECTION));
const updatedUserAndGroupComputedProps = compose(computeUserGroupIsDirty, computeValidationError(modes.EDIT_MAPPING));

const updatedCreateComputedProps = compose(computeCreateIsDirty, computeValidationError(modes.CREATE));

const getStateWithUpdatedValue = (fieldName, validator, state, { payload }) =>
  pathSet(['inputFields', fieldName], userInput(validator, payload), state);

const setTextInput = curryN(4, function setTextInput(fieldName, validator, state, { payload }) {
  return updatedComputedProps(getStateWithUpdatedValue(fieldName, validator, state, { payload }));
});

const setUserGroupTextInput = curryN(4, function setUserGroupTextInput(fieldName, validator, state, { payload }) {
  return updatedUserAndGroupComputedProps(getStateWithUpdatedValue(fieldName, validator, state, { payload }));
});

const setToggle = curryN(2, function setToggle(fieldName, state) {
  const newState = pathSet(['inputFields', fieldName], !state.inputFields[fieldName], state);
  return updatedUserAndGroupComputedProps(newState);
});

const setServerNameInput = curryN(4, function setServerNameInput(fieldName, validator, state, { payload }) {
  return updatedCreateComputedProps(getStateWithUpdatedValue(fieldName, validator, state, { payload }));
});

const setTextInputWithCompute = curryN(4, function setTextInputWithCompute(fieldName, validator, state, { payload }) {
  const newState = getStateWithUpdatedValue(fieldName, validator, state, { payload });
  return compose(updatedComputedProps, computeMustReenterPassword)(newState);
});

const setPasswordTextInput = curryN(4, function setPasswordTextInput(fieldName, validator, state, { payload }) {
  const newStateWithUpdatedPassword = getStateWithUpdatedValue(fieldName, validator, state, { payload });
  const newState = propSetConst('reservedSystemPassword', payload, newStateWithUpdatedPassword);

  return compose(updatedComputedProps, computeMustReenterPassword)(newState);
});

function startSaveMaskSuccessTimer(dispatch) {
  setTimeout(() => {
    dispatch(actions.saveMaskTimerDone());
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

function startRemoveMaskSuccessTimer(dispatch) {
  setTimeout(() => {
    dispatch(actions.removeMaskTimerDone());
    dispatch(stateGo('ldap-list'));
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

const loadAddPage = createAsyncThunk(`${REDUCER_NAME}/loadAddPage`, (_, { dispatch, rejectWithValue }) => {
  return checkPermissions(['CONFIGURE_SYSTEM'])
    .then(() => {
      dispatch(actions.setInitialAddForm());
      return always(true);
    })
    .catch(rejectWithValue);
});

const maybeLoadEditPage = createAsyncThunk(
  `${REDUCER_NAME}/maybeLoadEditPage`,
  ({ ldapId, currentTab }, { getState, dispatch }) => {
    const { server } = getState().ldapConfig.serverData;

    if (!server || (server && server.id !== ldapId)) {
      return dispatch(loadEditPage({ ldapId, currentTab }));
    }

    return currentTab === 'edit-ldap-connection'
      ? dispatch(actions.computeInitialConnectionTab())
      : dispatch(actions.computeInitialMappingTab());
  }
);

const loadEditPage = createAsyncThunk(`${REDUCER_NAME}/loadEditPage`, ({ ldapId, currentTab }, { rejectWithValue }) => {
  return checkPermissions(['CONFIGURE_SYSTEM'])
    .then(() => {
      const promises = [
        axios.get(getLdapConfigUrl()),
        axios.get(getLdapConnectionConfig(ldapId)),
        axios.get(getLdapUserMappingConfig(ldapId)),
      ];

      return Promise.all(promises).then(([{ data: servers }, { data: connection }, { data: mapping }]) => {
        const server = servers.find((server) => server.id === ldapId);
        if (!server) {
          throw `Could not find an LDAP server with ID ${ldapId}`;
        }
        return {
          mapping,
          connection,
          server,
          currentTab,
        };
      });
    })
    .catch(rejectWithValue);
});

const saveServerName = createAsyncThunk(
  `${REDUCER_NAME}/saveServerName`,
  (_, { getState, dispatch, rejectWithValue }) => {
    const formState = getState().ldapConfig.inputFields;
    const serverData = { name: formState.serverName.trimmedValue, id: null };

    return axios
      .post(getLdapConfigUrl(), serverData)
      .then(({ data }) => {
        startSaveMaskSuccessTimer(dispatch);
        dispatch(actions.resetIsDirty());
        dispatch(stateGo('edit-ldap-connection', { ldapId: data.id }));
        return data;
      })
      .catch(rejectWithValue);
  }
);

const saveConnection = createAsyncThunk(
  `${REDUCER_NAME}/saveConnection`,
  (_, { getState, dispatch, rejectWithValue }) => {
    const connection = getConnectionData(getState().ldapConfig);

    return getServerNameRequest(getState().ldapConfig)
      .then(({ data: server }) => {
        return axios.put(getLdapConnectionConfig(connection.serverId), connection).then(({ data }) => {
          startSaveMaskSuccessTimer(dispatch);
          return { ...data, server };
        });
      })
      .catch(rejectWithValue);
  }
);

const saveUserAndGroupSettings = createAsyncThunk(
  `${REDUCER_NAME}/saveUserAndGroupSettings`,
  (_, { getState, dispatch, rejectWithValue }) => {
    const settings = getSettingsData(getState().ldapConfig);

    return getServerNameRequest(getState().ldapConfig)
      .then(({ data: server }) => {
        return axios.put(getLdapUserMappingConfig(settings.serverId), settings).then(({ data }) => {
          startSaveMaskSuccessTimer(dispatch);
          return { ...data, server };
        });
      })
      .catch(rejectWithValue);
  }
);

const testConnection = createAsyncThunk(
  `${REDUCER_NAME}/testConnection`,
  (_, { getState, dispatch, rejectWithValue }) => {
    const connection = getConnectionData(getState().ldapConfig);
    return axios
      .put(getLdapConnectionTest(connection.serverId), connection)
      .then(({ data }) => {
        startSaveMaskSuccessTimer(dispatch);
        if (data.status !== 'OK') {
          return rejectWithValue(data.message);
        }
        return data;
      })
      .catch(rejectWithValue);
  }
);

const removeServer = createAsyncThunk(`${REDUCER_NAME}/removeServer`, (ldapId, { dispatch, rejectWithValue }) => {
  return axios
    .delete(getLdapConfigUrl(ldapId))
    .then(() => startRemoveMaskSuccessTimer(dispatch))
    .catch(rejectWithValue);
});

const serverNameValidator = combineValidators([validateNonEmpty, validateNameCharacters]);
const portValidator = combineValidators([validateNonEmpty, validateMinMax([1, 65535], 'Integer between 1 to 65535')]);
const timeoutsValidator = (value) => {
  if (value) {
    return validateMinMax([0, 999], 'Integer between 0 (default) to 999')(value);
  }

  return null;
};

const loadUserMapping = createAsyncThunk(`${REDUCER_NAME}/loadUserMapping`, (_, { getState, rejectWithValue }) => {
  const ldapState = getSettingsData(getState().ldapConfig);
  return axios
    .put(getLdapUserMappingTest(ldapState.serverId), ldapState)
    .then(({ data }) => data)
    .catch(rejectWithValue);
});

function setInputField(state, { payload: { value, field } }) {
  return {
    ...state,
    checkLoginProps: {
      ...state.checkLoginProps,
      [field]: userInput(validateNonEmpty, value),
    },
  };
}

function clearCheckLoginAlerts(state) {
  return {
    ...state,
    checkLoginProps: {
      ...state.checkLoginProps,
      checkLoginError: null,
      checkLoginSuccess: false,
    },
  };
}

function resetCheckLoginModal(state) {
  return {
    ...state,
    checkLoginProps: {
      ...initialState.checkLoginProps,
    },
  };
}

const checkLogin = createAsyncThunk(`${REDUCER_NAME}/checkLogin`, (ldapId, { rejectWithValue, getState }) => {
  const {
    ldapConfig: {
      checkLoginProps: { username, password },
    },
  } = getState();
  const checkLoginProps = {
    username: username.trimmedValue,
    password: password.trimmedValue,
  };
  const userMapping = getSettingsData(getState().ldapConfig);
  return axios
    .put(getLdapLoginTest(ldapId), { ...checkLoginProps, userMapping })
    .then(({ data: { status, message } }) => (status === 'OK' ? { status, message } : rejectWithValue(message)))
    .catch(rejectWithValue);
});

const ldapConfigSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setServerName: setServerNameInput('serverName', serverNameValidator),
    saveMaskTimerDone: propSetConst('saveMaskState', null),
    removeMaskTimerDone: propSetConst('removeMaskState', null),
    resetIsDirty: propSetConst('isDirty', false),
    resetForm: always(initialState),
    setInitialAddForm,
    resetAlertMessages,
    resetAllNotifications,
    resetRemoveServerError: propSetConst('removeError', null),
    computeInitialConnectionTab,
    computeInitialMappingTab,

    setProtocol,
    setHostname: setTextInputWithCompute('hostname', validateNonEmpty),
    setPort: setTextInputWithCompute('port', portValidator),
    setSearchBase: setTextInput('searchBase', validateNonEmpty),
    setReferralIgnored,

    setMethod,
    setSaslRealm: setTextInput('saslRealm', null),
    setUsername: setTextInput('systemUsername', validateNonEmpty),
    setPassword: setPasswordTextInput('systemPassword', validateNonEmpty),

    setConnection: setTextInput('connectionTimeout', timeoutsValidator),
    setRetryDelay: setTextInput('retryDelay', timeoutsValidator),

    setUserBaseDN: setUserGroupTextInput('userBaseDN', null),
    setUserSubtree: setToggle('userSubtree'),
    setUserObjectClass: setUserGroupTextInput('userObjectClass', validateNonEmpty),
    setUserFilter: setUserGroupTextInput('userFilter', null),
    setUserIDAttribute: setUserGroupTextInput('userIDAttribute', validateNonEmpty),
    setUserRealNameAttribute: setUserGroupTextInput('userRealNameAttribute', validateNonEmpty),
    setUserEmailAttribute: setUserGroupTextInput('userEmailAttribute', validateNonEmpty),
    setUserPasswordAttribute: setUserGroupTextInput('userPasswordAttribute', null),

    setGroupMappingType,
    setGroupBaseDN: setUserGroupTextInput('groupBaseDN', null),
    setGroupSubtree: setToggle('groupSubtree'),
    setGroupObjectClass: setUserGroupTextInput('groupObjectClass', validateNonEmpty),
    setGroupIDAttribute: setUserGroupTextInput('groupIDAttribute', validateNonEmpty),
    setGroupMemberAttribute: setUserGroupTextInput('groupMemberAttribute', validateNonEmpty),
    setGroupMemberFormat: setUserGroupTextInput('groupMemberFormat', validateNonEmpty),
    setUserMemberOfGroupAttribute: setUserGroupTextInput('userMemberOfGroupAttribute', validateNonEmpty),
    setDynamicGroupSearch: setToggle('dynamicGroupSearchEnabled'),
    toggleUserMappingModalIsOpen: (state) => {
      state.userMapping.isLdapUserMappingModalOpen = !state.userMapping.isLdapUserMappingModalOpen;
    },
    toggleUserMappingSortOrder: (state) => {
      state.userMapping.sortAscending = !state.userMapping.sortAscending;
    },

    setInputField,
    clearCheckLoginAlerts,
    resetCheckLoginModal,
  },
  extraReducers: {
    [loadAddPage.pending]: propSetConst('loading', true),
    [loadAddPage.fulfilled]: loadAddPageFulfilled,
    [loadAddPage.rejected]: loadAddPageFailed,

    [loadEditPage.pending]: propSetConst('loading', true),
    [loadEditPage.fulfilled]: loadEditPageFulfilled,
    [loadEditPage.rejected]: loadEditPageFailed,

    [saveConnection.pending]: connectionPending,
    [saveConnection.fulfilled]: saveConnectionFulfilled,
    [saveConnection.rejected]: connectionFailed,

    [saveUserAndGroupSettings.pending]: connectionPending,
    [saveUserAndGroupSettings.fulfilled]: saveUserAndGroupSettingsFulfilled,
    [saveUserAndGroupSettings.rejected]: connectionFailed,

    [testConnection.pending]: testConnectionPending,
    [testConnection.fulfilled]: testConnectionFulfilled,
    [testConnection.rejected]: testConnectionFailed,

    [saveServerName.pending]: propSetConst('saveMaskState', false),
    [saveServerName.fulfilled]: saveFulfilled,
    [saveServerName.rejected]: saveFailed,

    [removeServer.pending]: propSetConst('removeMaskState', false),
    [removeServer.fulfilled]: removeServerFulfilled,
    [removeServer.rejected]: removeServerFailed,

    [loadUserMapping.pending]: (state) => {
      state.userMapping.userList = null;
      state.userMapping.loadError = null;
    },
    [loadUserMapping.fulfilled]: (state, { payload }) => {
      state.userMapping.userList = payload;
      state.userMapping.loadError = null;
    },
    [loadUserMapping.rejected]: (state, { payload }) => {
      state.userMapping.userList = null;
      state.userMapping.loadError = Messages.getHttpErrorMessage(payload);
    },
    [checkLogin.fulfilled]: checkLoginSuccess,
    [checkLogin.rejected]: checkLoginFailed,
  },
});

export default ldapConfigSlice.reducer;

export const actions = {
  ...ldapConfigSlice.actions,
  loadAddPage,
  maybeLoadEditPage,
  saveServerName,
  saveConnection,
  testConnection,
  removeServer,
  saveUserAndGroupSettings,
  loadUserMapping,
  checkLogin,
};
