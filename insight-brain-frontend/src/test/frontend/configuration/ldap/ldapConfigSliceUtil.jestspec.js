/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as textInputStateHelpers from '@sonatype/react-shared-components/components/NxTextInput/stateHelpers';
import {
  modes,
  getRequiredFieldNames,
  getNewPortValue,
  constructInitialInputFields,
} from '../../../../main/frontend/configuration/ldap/ldapConfigSliceUtil';

const { initialState: initUserInput } = textInputStateHelpers;

describe('ldapConfigSliceUtil', () => {
  describe('getRequiredFieldNames', () => {
    it('returns propper data for modes.CREATE', () => {
      expect(getRequiredFieldNames({}, modes.CREATE)).toEqual(['serverName']);
    });

    describe('returns propper data for modes.EDIT_CONNECTION', () => {
      const basicNames = [
        'serverName',
        'protocol',
        'port',
        'hostname',
        'searchBase',
        'referralIgnored',
        'authenticationMethod',
      ];
      it('when authenticationMethod === NONE and connectionTimeout/retryDelay are empty', () => {
        let state = Object.freeze({
          inputFields: {
            authenticationMethod: 'NONE',
            connectionTimeout: initUserInput(''),
            retryDelay: initUserInput(''),
          },
        });

        expect(getRequiredFieldNames(state, modes.EDIT_CONNECTION)).toEqual(basicNames);
      });

      it('when authenticationMethod === NONE and connectionTimeout/retryDelay are not empty', () => {
        let state = Object.freeze({
          inputFields: {
            authenticationMethod: 'NONE',
            connectionTimeout: initUserInput('12'),
            retryDelay: initUserInput('13'),
          },
        });

        expect(getRequiredFieldNames(state, modes.EDIT_CONNECTION)).toEqual([
          ...basicNames,
          'connectionTimeout',
          'retryDelay',
        ]);
      });

      it('when authenticationMethod !== NONE and connectionTimeout/retryDelay are empty', () => {
        let state = Object.freeze({
          inputFields: {
            authenticationMethod: 'SIMPLE',
            connectionTimeout: initUserInput(''),
            retryDelay: initUserInput(''),
          },
        });

        expect(getRequiredFieldNames(state, modes.EDIT_CONNECTION)).toEqual([
          ...basicNames,
          'systemUsername',
          'systemPassword',
        ]);
      });

      it('when authenticationMethod !== NONE and connectionTimeout/retryDelay are not empty', () => {
        let state = Object.freeze({
          inputFields: {
            authenticationMethod: 'SIMPLE',
            connectionTimeout: initUserInput('12'),
            retryDelay: initUserInput('13'),
          },
        });

        expect(getRequiredFieldNames(state, modes.EDIT_CONNECTION)).toEqual([
          ...basicNames,
          'systemUsername',
          'systemPassword',
          'connectionTimeout',
          'retryDelay',
        ]);
      });
    });

    describe('returns propper data for modes.EDIT_MAPPING', () => {
      const basicNames = ['userObjectClass', 'userIDAttribute', 'userRealNameAttribute', 'userEmailAttribute'];
      it('when groupMappingType === STATIC', () => {
        let state = Object.freeze({
          inputFields: {
            groupMappingType: 'STATIC',
          },
        });

        expect(getRequiredFieldNames(state, modes.EDIT_MAPPING)).toEqual([
          ...basicNames,
          'groupObjectClass',
          'groupIDAttribute',
          'groupMemberAttribute',
          'groupMemberFormat',
        ]);
      });

      it('when groupMappingType === DYNAMIC', () => {
        let state = Object.freeze({
          inputFields: {
            groupMappingType: 'DYNAMIC',
          },
        });

        expect(getRequiredFieldNames(state, modes.EDIT_MAPPING)).toEqual([...basicNames, 'userMemberOfGroupAttribute']);
      });
    });
  });

  describe('getNewPortValue', () => {
    it('returns port 636 if protocol is changed to LDAPS', () => {
      const { port } = getNewPortValue('LDAPS', initUserInput('389'));

      expect(port.value).toBe('636');
    });

    it('returns port 389 if protocol is changed to LDAPS', () => {
      const { port } = getNewPortValue('LDAP', initUserInput('636'));

      expect(port.value).toBe('389');
    });

    it('returns unchanged port value if port already has custom value', () => {
      const { port } = getNewPortValue('LDAP', initUserInput('8080'));

      expect(port.value).toBe('8080');
    });
  });

  describe('constructInitialInputFields', () => {
    it('transforms initial request data to input fields', () => {
      const serverData = {
        server: {
          name: 'serverName',
        },
        connection: {
          protocol: 'LDAP',
          referralIgnored: false,
          authenticationMethod: 'NONE',
          port: 345,
          hostname: 'name',
          searchBase: 'base',
          saslRealm: 'realm',
          systemUsername: 'systemUsername',
          systemPassword: 'systemPassword',
          connectionTimeout: 'connectionTimeout',
          retryDelay: 'retryDelay',
        },
        mapping: {
          userSubtree: false,
          groupMappingType: false,
          groupSubtree: false,
          dynamicGroupSearchEnabled: false,
          userBaseDN: 'userBaseDN',
          userObjectClass: 'userObjectClass',
          userFilter: 'userFilter',
          userIDAttribute: 'userIDAttribute',
          userRealNameAttribute: 'userRealNameAttribute',
          userEmailAttribute: 'userEmailAttribute',
          userPasswordAttribute: 'userPasswordAttribute',
          groupBaseDN: 'groupBaseDN',
          groupObjectClass: 'groupObjectClass',
          groupIDAttribute: 'groupIDAttribute',
          groupMemberAttribute: 'groupMemberAttribute',
          groupMemberFormat: null,
          userMemberOfGroupAttribute: null,
        },
      };

      expect(constructInitialInputFields(serverData)).toEqual({
        serverName: initUserInput('serverName'),
        protocol: 'LDAP',
        referralIgnored: false,
        authenticationMethod: 'NONE',
        port: initUserInput('345'),
        hostname: initUserInput('name'),
        searchBase: initUserInput('base'),
        saslRealm: initUserInput('realm'),
        systemUsername: initUserInput('systemUsername'),
        systemPassword: initUserInput('systemPassword'),
        connectionTimeout: initUserInput('connectionTimeout'),
        retryDelay: initUserInput('retryDelay'),
        userSubtree: false,
        groupMappingType: false,
        groupSubtree: false,
        dynamicGroupSearchEnabled: false,
        userBaseDN: initUserInput('userBaseDN'),
        userObjectClass: initUserInput('userObjectClass'),
        userFilter: initUserInput('userFilter'),
        userIDAttribute: initUserInput('userIDAttribute'),
        userRealNameAttribute: initUserInput('userRealNameAttribute'),
        userEmailAttribute: initUserInput('userEmailAttribute'),
        userPasswordAttribute: initUserInput('userPasswordAttribute'),
        groupBaseDN: initUserInput('groupBaseDN'),
        groupObjectClass: initUserInput('groupObjectClass'),
        groupIDAttribute: initUserInput('groupIDAttribute'),
        groupMemberAttribute: initUserInput('groupMemberAttribute'),
        groupMemberFormat: initUserInput(''),
        userMemberOfGroupAttribute: initUserInput(''),
      });
    });
  });
});
