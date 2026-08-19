/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import { pick, mapObjIndexed, prop } from 'ramda';
import { validateNonEmpty } from '../../util/validationUtil';
import { getLdapConfigUrl } from '../../util/CLMLocation';

const { initialState: initUserInput, userInput } = nxTextInputStateHelpers;

export const getServerNameRequest = (state) => {
  const {
    inputFields,
    serverData: { server },
  } = state;
  const newServerName = inputFields.serverName.trimmedValue;

  if (newServerName === server.name) {
    return Promise.resolve({ data: { ...server } });
  }

  return axios.put(getLdapConfigUrl(), { ...server, name: newServerName });
};

export const getSettingsData = (state) => {
  const {
    inputFields,
    serverData: { mapping },
  } = state;
  const textFields = pick(
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
    inputFields
  );

  const textValues = mapObjIndexed(prop('trimmedValue'), textFields);

  return {
    id: mapping.id,
    serverId: mapping.serverId,
    ...textValues,
    groupMappingType: inputFields.groupMappingType,
    userSubtree: inputFields.userSubtree,
    groupSubtree: inputFields.groupSubtree,
    dynamicGroupSearchEnabled: inputFields.dynamicGroupSearchEnabled,
  };
};

export const getConnectionData = (state) => {
  const {
    inputFields,
    serverData: { connection },
  } = state;
  const textFields = pick(
    [
      'hostname',
      'searchBase',
      'saslRealm',
      'systemUsername',
      'systemPassword',
      'port',
      'connectionTimeout',
      'retryDelay',
    ],
    inputFields
  );
  const textValues = mapObjIndexed(prop('trimmedValue'), textFields);

  const comboValues = {
    protocol: inputFields.protocol,
    authenticationMethod: inputFields.authenticationMethod,
  };

  return {
    id: connection.id,
    ...textValues,
    ...comboValues,
    serverId: connection.serverId,
    referralIgnored: inputFields.referralIgnored,
  };
};

export const modes = {
  CREATE: 'create',
  EDIT_CONNECTION: 'connection',
  EDIT_MAPPING: 'mapping',
};

const createFormInputFields = ['serverName'];
const editConnectionFields = [
  'serverName',
  'protocol',
  'port',
  'hostname',
  'searchBase',
  'referralIgnored',
  'authenticationMethod',
  'saslRealm',
  'systemUsername',
  'systemPassword',
  'connectionTimeout',
  'retryDelay',
];
const editUserAndGroupFields = [
  'serverName',
  'userBaseDN',
  'userSubtree',
  'userObjectClass',
  'userFilter',
  'userIDAttribute',
  'userRealNameAttribute',
  'userEmailAttribute',
  'userPasswordAttribute',
  'groupMappingType',
  'groupBaseDN',
  'groupSubtree',
  'groupObjectClass',
  'groupIDAttribute',
  'groupMemberAttribute',
  'groupMemberFormat',
  'userMemberOfGroupAttribute',
  'dynamicGroupSearchEnabled',
];

export const modesFieldsMap = new Map([
  [modes.CREATE, createFormInputFields],
  [modes.EDIT_CONNECTION, editConnectionFields],
  [modes.EDIT_MAPPING, editUserAndGroupFields],
]);

export const modesRequiredFieldsMap = new Map([
  [modes.CREATE, createFormInputFields],
  [
    modes.EDIT_CONNECTION,
    ['serverName', 'protocol', 'port', 'hostname', 'searchBase', 'referralIgnored', 'authenticationMethod'],
  ],
  [modes.EDIT_MAPPING, ['userObjectClass', 'userIDAttribute', 'userRealNameAttribute', 'userEmailAttribute']],
]);

export const getRequiredFieldNames = (state, mode) => {
  let names = [...modesRequiredFieldsMap.get(mode)];
  let extended = [];

  if (mode === modes.EDIT_CONNECTION) {
    if (state.inputFields.authenticationMethod !== 'NONE') {
      extended = ['systemUsername', 'systemPassword'];
    }
    if (state.inputFields.connectionTimeout.value) {
      extended.push('connectionTimeout');
    }
    if (state.inputFields.retryDelay.value) {
      extended.push('retryDelay');
    }
  }

  if (mode === modes.EDIT_MAPPING) {
    if (state.inputFields.groupMappingType === 'STATIC') {
      extended = ['groupObjectClass', 'groupIDAttribute', 'groupMemberAttribute', 'groupMemberFormat'];
    } else if (state.inputFields.groupMappingType === 'DYNAMIC') {
      extended = ['userMemberOfGroupAttribute'];
    }
  }

  return [...names, ...extended];
};

export const protocolPortMap = new Map([
  ['LDAP', '389'],
  ['LDAPS', '636'],
]);

export const getNewPortValue = (payload, prevPort) => {
  if (
    (payload === 'LDAP' && prevPort.trimmedValue === '636') ||
    (payload === 'LDAPS' && prevPort.trimmedValue === '389')
  ) {
    return { port: userInput(validateNonEmpty, protocolPortMap.get(payload)), changed: true };
  }

  return { port: prevPort, changed: false };
};

export const getInitUserInputForFields = (names, payload) => {
  const fields = {};

  names.forEach((name) => {
    const value = payload[name] === null ? '' : String(payload[name]);
    fields[name] = initUserInput(value);
  });

  return fields;
};

export const constructInitialInputFields = ({ mapping, connection, server }) => {
  return {
    serverName: initUserInput(server.name),
    ...pick(['protocol', 'referralIgnored', 'authenticationMethod'], connection),
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
      connection
    ),
    ...pick(['userSubtree', 'groupMappingType', 'groupSubtree', 'dynamicGroupSearchEnabled'], mapping),
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
      mapping
    ),
  };
};
