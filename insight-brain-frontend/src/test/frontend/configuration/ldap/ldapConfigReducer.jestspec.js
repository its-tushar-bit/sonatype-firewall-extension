/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as textInputStateHelpers from '@sonatype/react-shared-components/components/NxTextInput/stateHelpers';
import { omit } from 'ramda';
import reduce, { initialState } from '../../../../main/frontend/configuration/ldap/ldapConfigSlice';

const { initialState: initUserInput, userInput } = textInputStateHelpers;

describe('ldapConfigReducer', () => {
  let otherObject;

  beforeEach(() => {
    otherObject = { value: 'that is no moon' };
  });

  describe('ldapConfig/loadAddPage/pending action', () => {
    it('sets loading to true', () => {
      const state = Object.freeze({
        other: otherObject,
        loading: false,
      });

      const { loading, other } = reduce(state, {
        type: 'ldapConfig/loadAddPage/pending',
      });

      expect(loading).toBe(true);
      expect(other).toBe(otherObject);
    });
  });

  describe('ldapConfig/loadAddPage/fulfilled action', () => {
    it('sets loading to false and clears errors', () => {
      const state = Object.freeze({
        other: otherObject,
        loading: true,
        saveError: 'error',
        loadError: 'foo',
      });

      const { loading, saveError, loadError, other } = reduce(state, {
        type: 'ldapConfig/loadAddPage/fulfilled',
      });

      expect(loading).toBe(false);
      expect(saveError).toBeNull();
      expect(loadError).toBeNull();
      expect(other).toBe(otherObject);
    });
  });

  describe('ldapConfig/loadAddPage/rejected action', () => {
    it('sets loading to false and sets loadError to payload', () => {
      const state = Object.freeze({
        other: otherObject,
        loading: true,
        saveError: 'error',
        loadError: null,
      });

      const { loading, saveError, loadError, other } = reduce(state, {
        type: 'ldapConfig/loadAddPage/rejected',
        payload: 'error',
      });

      expect(loading).toBe(false);
      expect(saveError).toBeNull();
      expect(loadError).toBe('error');
      expect(other).toBe(otherObject);
    });
  });

  describe('ldapConfig/loadEditPage/pending action', () => {
    it('sets loading to true', () => {
      const state = Object.freeze({
        other: otherObject,
        loading: false,
      });

      const { loading, other } = reduce(state, {
        type: 'ldapConfig/loadEditPage/pending',
      });

      expect(loading).toBe(true);
      expect(other).toBe(otherObject);
    });
  });

  describe('ldapConfig/loadEditPage/fulfilled action', () => {
    it('sets loading to false, clears errors, updates inputFields and serverData', () => {
      const state = Object.freeze({
        loading: true,
        saveError: 'error',
        loadError: 'foo',
        mustReenterPassword: false,
        inputFields: {},
        serverData: {},
        isDirty: false,
      });

      const payload = {
        server: {
          id: '200',
          name: 'server3',
        },
        connection: {
          id: null,
          serverId: '200',
          authenticationMethod: 'NONE',
          connectionTimeout: 30,
          hostname: null,
          port: 389,
          protocol: 'LDAP',
          referralIgnored: false,
          retryDelay: 30,
          saslRealm: null,
          systemPassword: null,
          systemUsername: null,
          searchBase: null,
        },
        mapping: {
          id: null,
          serverId: '200',
          userBaseDN: 'baseDn',
          userSubtree: false,
          groupMappingType: 'SIMPLE',
          userObjectClass: 'object class',
          userFilter: 'filter',
          userIDAttribute: 'idattr',
          userRealNameAttribute: 'realnameattr',
          userEmailAttribute: 'emailattr',
          userPasswordAttribute: 'passattr',
          groupBaseDN: '',
          groupObjectClass: '',
          groupIDAttribute: '',
          groupMemberAttribute: 'groupMemberAttr',
          groupMemberFormat: '',
          userMemberOfGroupAttribute: 'groupMemberFormat',
          groupSubtree: false,
          dynamicGroupSearchEnabled: false,
        },
        currentTab: 'edit-ldap-connection',
      };

      const { loading, saveError, loadError, inputFields, serverData, isDirty, mustReenterPassword } = reduce(state, {
        type: 'ldapConfig/loadEditPage/fulfilled',
        payload,
      });

      expect(loading).toBe(false);
      expect(saveError).toBeNull();
      expect(loadError).toBeNull();
      expect(isDirty).toBe(false);
      expect(mustReenterPassword).toBe(false);
      expect(inputFields.serverName).toEqual(initUserInput('server3'));
      expect(serverData).toEqual({ serverName: 'server3', ...payload });
    });
  });

  describe('ldapConfig/loadEditPage/rejected action', () => {
    it('sets loading to false and sets loadError to payload', () => {
      const state = Object.freeze({
        other: otherObject,
        loading: true,
        saveError: 'error',
        loadError: null,
      });

      const { loading, saveError, loadError, other } = reduce(state, {
        type: 'ldapConfig/loadEditPage/rejected',
        payload: 'error',
      });

      expect(loading).toBe(false);
      expect(saveError).toBeNull();
      expect(loadError).toBe('error');
      expect(other).toBe(otherObject);
    });
  });

  describe('ldapConfig/saveServerName/pending action', () => {
    it('sets saveMaskState to false', () => {
      const state = Object.freeze({
        other: otherObject,
        saveMaskState: true,
      });

      const { saveMaskState, other } = reduce(state, {
        type: 'ldapConfig/saveServerName/pending',
      });

      expect(saveMaskState).toBe(false);
      expect(other).toBe(otherObject);
    });
  });

  describe('ldapConfig/saveServerName/fulfilled action', () => {
    it('resets input fields, isDirty, loading and mask', () => {
      const state = Object.freeze({
        other: otherObject,
        loading: true,
        saveError: 'error',
        loadError: 'foo',
        saveMaskState: false,
        serverData: {},
        isDirty: true,
        inputFields: {
          serverName: 'abracadabra',
        },
      });

      const { loading, saveError, loadError, saveMaskState, serverData, inputFields, other, isDirty } = reduce(state, {
        type: 'ldapConfig/saveServerName/fulfilled',
        payload: {
          id: '200',
          name: 'server new',
          priority: 1,
        },
      });

      expect(loading).toBe(false);
      expect(saveError).toBeNull();
      expect(loadError).toBeNull();
      expect(isDirty).toBe(false);
      expect(saveMaskState).toBe(true);
      expect(serverData.server).toEqual({ id: '200', name: 'server new', priority: 1 });
      expect(inputFields.serverName).toEqual(initUserInput(''));
      expect(other).toBe(otherObject);
    });
  });

  describe('ldapConfig/saveServerName/rejected action', () => {
    it('sets loading to false, saveMask to null, sets saveError to payload', () => {
      const state = Object.freeze({
        other: otherObject,
        loading: true,
        saveError: null,
        saveMaskState: false,
      });

      const { loading, saveError, saveMaskState, other } = reduce(state, {
        type: 'ldapConfig/saveServerName/rejected',
        payload: 'save error',
      });

      expect(loading).toBe(false);
      expect(saveError).toBe('save error');
      expect(saveMaskState).toBeNull();
      expect(other).toBe(otherObject);
    });
  });

  describe('ldapConfig/saveMaskTimerDone action', () => {
    it('sets submitMaskState to null', () => {
      const state = Object.freeze({
        other: otherObject,
        saveMaskState: true,
      });

      const { saveMaskState, other } = reduce(state, {
        type: 'ldapConfig/saveMaskTimerDone',
      });

      expect(saveMaskState).toBeNull();
      expect(other).toBe(otherObject);
    });
  });

  describe('ldapConfig/removeMaskTimerDone action', () => {
    it('sets removeMaskState to null', () => {
      const state = Object.freeze({
        other: otherObject,
        removeMaskState: true,
      });

      const { removeMaskState, other } = reduce(state, {
        type: 'ldapConfig/removeMaskTimerDone',
      });

      expect(removeMaskState).toBeNull();
      expect(other).toBe(otherObject);
    });
  });

  describe('ldapConfig/resetForm', () => {
    it('resets state to initialState', () => {
      const state = Object.freeze({
        serverData: {
          id: '202',
        },
        isDirty: true,
        loading: true,
        loadError: 'error',
        saveError: 'error',
        saveMaskState: true,
        validationError: ['error'],
        inputFields: {
          serverName: initUserInput('server'),
        },
      });

      const newState = reduce(state, { type: 'ldapConfig/resetForm' });

      expect(newState).toEqual(initialState);
    });
  });

  describe('ldapConfig/setServerName', () => {
    describe('when payload is empty string', () => {
      it('sets the server name value and a validation error', () => {
        const state = Object.freeze({
          other: otherObject,
          inputFields: {
            serverName: initUserInput(''),
          },
          serverData: {},
        });

        const { inputFields, other } = reduce(state, {
          type: 'ldapConfig/setServerName',
          payload: '',
        });

        expect(inputFields.serverName.value).toBe('');
        expect(inputFields.serverName.trimmedValue).toBe('');
        expect(inputFields.serverName.isPristine).toBe(false);
        expect(inputFields.serverName.validationErrors).toBeTruthy();
        expect(other).toBe(otherObject);
      });
    });

    describe('when payload is non-empty string', () => {
      it('sets the server name value and no validation error', () => {
        const state = Object.freeze({
          other: otherObject,
          inputFields: {
            serverName: initUserInput(''),
          },
          serverData: {},
        });

        const { inputFields, other } = reduce(state, {
          type: 'ldapConfig/setServerName',
          payload: 'server',
        });

        expect(inputFields.serverName.value).toBe('server');
        expect(inputFields.serverName.trimmedValue).toBe('server');
        expect(inputFields.serverName.isPristine).toBe(false);
        expect(inputFields.serverName.validationErrors).toEqual([]);
        expect(other).toBe(otherObject);
      });
    });
  });

  describe('ldapConfig/resetAlertMessages', () => {
    it('sets successMessage and testConnectionErrorMessage to empty string, resets saveError and saveMaskState', () => {
      const state = Object.freeze({
        other: otherObject,
        successMessage: 'Sucecss!',
        testConnectionErrorMessage: 'Error for Test Conenction',
        saveError: 'error',
        saveMaskState: true,
      });

      const { successMessage, testConnectionErrorMessage, saveMaskState, saveError, other } = reduce(state, {
        type: 'ldapConfig/resetAlertMessages',
      });

      expect(successMessage).toBe('');
      expect(testConnectionErrorMessage).toBe('');
      expect(saveMaskState).toBeNull();
      expect(saveError).toBeNull();
      expect(other).toBe(otherObject);
    });
  });

  describe('ldapConfig/setReferralIgnored', () => {
    it('sets successMessage to empty string', () => {
      const state = Object.freeze({
        other: otherObject,
        isDirty: false,
        inputFields: {
          ...initialState.inputFields,
          referralIgnored: false,
        },
        serverData: {
          server: {},
          connection: {
            authenticationMethod: 'NONE',
            connectionTimeout: 31,
            hostname: 'ldap.forumsys.com',
            port: 389,
            protocol: 'LDAP',
            referralIgnored: true,
            retryDelay: 30,
            saslRealm: null,
            searchBase: 'dc=example,dc=com',
            systemUsername: 'uid=tesla,dc=example,dc=com',
          },
        },
      });

      const { isDirty, inputFields, other } = reduce(state, { type: 'ldapConfig/setReferralIgnored' });

      expect(isDirty).toBe(true);
      expect(inputFields.referralIgnored).toBe(true);
      expect(other).toBe(otherObject);
    });
  });

  describe('ldapConfig/setProtocol', () => {
    let defaultState = {
      other: otherObject,
      isDirty: false,
      inputFields: {
        ...initialState.inputFields,
      },
      serverData: {
        server: {},
        connection: {
          authenticationMethod: 'NONE',
          connectionTimeout: 31,
          hostname: 'ldap.forumsys.com',
          port: 389,
          protocol: 'LDAP',
          referralIgnored: true,
          retryDelay: 30,
          saslRealm: null,
          searchBase: 'dc=example,dc=com',
          systemUsername: 'uid=tesla,dc=example,dc=com',
        },
      },
    };

    it('sets protocol and port values', () => {
      const state = Object.freeze({
        ...defaultState,
        inputFields: {
          ...defaultState.inputFields,
          port: initUserInput('636'),
          protocol: 'LDAPS',
        },
        serverData: {
          ...defaultState.serverData,
          connection: {
            ...defaultState.serverData.connection,
            port: 636,
            protocol: 'LDAPS',
          },
        },
      });

      const { inputFields } = reduce(state, {
        type: 'ldapConfig/setProtocol',
        payload: 'LDAP',
      });

      expect(inputFields.protocol).toBe('LDAP');
      expect(inputFields.port.value).toBe('389');
      expect(inputFields.port.validationErrors).toBeNull();
    });

    it('sets only protocol value if custom port value was changed', () => {
      const state = Object.freeze({
        ...defaultState,
        inputFields: {
          ...defaultState.inputFields,
          port: initUserInput('1234'),
          protocol: 'LDAPS',
        },
        serverData: {
          ...defaultState.serverData,
          connection: {
            ...defaultState.serverData.connection,
            port: 636,
            protocol: 'LDAPS',
          },
        },
      });

      const { inputFields } = reduce(state, {
        type: 'ldapConfig/setProtocol',
        payload: 'LDAP',
      });

      expect(inputFields.port.value).toBe('1234');
      expect(inputFields.protocol).toBe('LDAP');
      expect(inputFields.port.validationErrors).toBeNull();
    });
  });

  describe('ldapConfig/setHostname', () => {
    let defaultState = {
      other: otherObject,
      isDirty: false,
      inputFields: {
        ...initialState.inputFields,
      },
      serverData: {
        server: {},
        connection: {
          authenticationMethod: 'NONE',
          connectionTimeout: 31,
          hostname: 'ldap.forumsys.com',
          port: 389,
          protocol: 'LDAP',
          referralIgnored: true,
          retryDelay: 30,
          saslRealm: null,
          searchBase: 'dc=example,dc=com',
          systemUsername: 'uid=tesla,dc=example,dc=com',
        },
      },
    };

    describe('when payload is empty string', () => {
      it('sets the hostname value and a validation error', () => {
        const state = Object.freeze({
          ...defaultState,
          inputFields: {
            ...defaultState.inputFields,
            hostname: initUserInput('some'),
          },
          serverData: {
            ...defaultState.serverData,
            connection: {
              ...defaultState.serverData.connection,
              hostname: 'some',
            },
          },
        });

        const { inputFields } = reduce(state, {
          type: 'ldapConfig/setHostname',
          payload: '',
        });

        expect(inputFields.hostname.value).toBe('');
        expect(inputFields.hostname.validationErrors).toBeTruthy();
      });
    });

    describe('when payload is non-empty string', () => {
      it('sets the hostname value and no validation error', () => {
        const state = Object.freeze({
          ...defaultState,
          inputFields: {
            ...defaultState.inputFields,
            hostname: initUserInput(''),
          },
          serverData: {
            ...defaultState.serverData,
            connection: {
              ...defaultState.serverData.connection,
              hostname: '',
            },
          },
        });

        const { inputFields } = reduce(state, {
          type: 'ldapConfig/setHostname',
          payload: 'hostname',
        });

        expect(inputFields.hostname.value).toBe('hostname');
        expect(inputFields.hostname.validationErrors).toBeNull();
      });
    });

    describe('when auth !== NONE', () => {
      it('sets mustReenterPassword to true if hostname changed and password was not changed', () => {
        const state = Object.freeze({
          ...defaultState,
          inputFields: {
            ...defaultState.inputFields,
            hostname: initUserInput('old host'),
            authenticationMethod: 'SIMPLE',
            systemPassword: initUserInput('oldpass'),
          },
          serverData: {
            ...defaultState.serverData,
            connection: {
              ...defaultState.serverData.connection,
              hostname: 'old host',
              authenticationMethod: 'SIMPLE',
              systemPassword: 'oldpass',
            },
          },
        });

        const { mustReenterPassword } = reduce(state, {
          type: 'ldapConfig/setHostname',
          payload: 'new host',
        });

        expect(mustReenterPassword).toBe(true);
      });

      it('sets mustReenterPassword to false if hostname was changed but password already have been updated', () => {
        const state = Object.freeze({
          ...defaultState,
          inputFields: {
            ...defaultState.inputFields,
            hostname: initUserInput('old host'),
            authenticationMethod: 'SIMPLE',
            systemPassword: initUserInput('oldpass1'),
          },
          serverData: {
            ...defaultState.serverData,
            connection: {
              ...defaultState.serverData.connection,
              hostname: 'old host',
              authenticationMethod: 'SIMPLE',
              systemPassword: 'oldpass',
            },
          },
        });

        const { mustReenterPassword } = reduce(state, {
          type: 'ldapConfig/setHostname',
          payload: 'new host',
        });

        expect(mustReenterPassword).toBe(false);
      });

      it('sets mustReenterPassword to true if hostname was changed twice', () => {
        const state = Object.freeze({
          ...defaultState,
          reservedSystemPassword: 'oldpass',
          inputFields: {
            ...defaultState.inputFields,
            hostname: initUserInput('old host'),
            authenticationMethod: 'SIMPLE',
            systemPassword: initUserInput('oldpass'),
          },
          serverData: {
            ...defaultState.serverData,
            connection: {
              ...defaultState.serverData.connection,
              hostname: 'old host',
              authenticationMethod: 'SIMPLE',
              systemPassword: 'oldpass',
            },
          },
        });

        const newState = reduce(state, { type: 'ldapConfig/setHostname', payload: 'new hostname' });
        const { mustReenterPassword } = reduce(newState, { type: 'ldapConfig/setHostname', payload: 'new hostname!!' });

        expect(mustReenterPassword).toBe(true);
      });

      it('sets mustReenterPassword to false if password is not set on the server and hostname is changed', () => {
        const state = Object.freeze({
          ...defaultState,
          reservedSystemPassword: 'oldpass',
          inputFields: {
            ...defaultState.inputFields,
            hostname: initUserInput('old host'),
            authenticationMethod: 'SIMPLE',
            systemPassword: initUserInput(''),
          },
          serverData: {
            ...defaultState.serverData,
            connection: {
              ...defaultState.serverData.connection,
              hostname: 'old host',
              authenticationMethod: 'SIMPLE',
              systemPassword: null,
            },
          },
        });

        const { mustReenterPassword } = reduce(state, { type: 'ldapConfig/setHostname', payload: 'new hostname' });

        expect(mustReenterPassword).toBe(false);
      });
    });
  });

  describe('ldapConfig/setPort', () => {
    let defaultState = {
      other: otherObject,
      isDirty: false,
      inputFields: {
        ...initialState.inputFields,
      },
      serverData: {
        server: {},
        connection: {
          authenticationMethod: 'NONE',
          connectionTimeout: 31,
          hostname: 'ldap.forumsys.com',
          port: 389,
          protocol: 'LDAP',
          referralIgnored: true,
          retryDelay: 30,
          saslRealm: null,
          searchBase: 'dc=example,dc=com',
          systemUsername: 'uid=tesla,dc=example,dc=com',
        },
      },
    };

    describe('when payload is empty string', () => {
      it('sets port value and a validation error', () => {
        const state = Object.freeze({
          ...defaultState,
          inputFields: {
            ...defaultState.inputFields,
            port: initUserInput('349'),
          },
          serverData: {
            ...defaultState.serverData,
            connection: {
              ...defaultState.serverData.connection,
              port: 349,
            },
          },
        });

        const { inputFields } = reduce(state, {
          type: 'ldapConfig/setPort',
          payload: '',
        });

        expect(inputFields.port.value).toBe('');
        expect(inputFields.port.validationErrors).toBeTruthy();
      });
    });

    describe('when payload is non-empty string', () => {
      it('sets port value and no validation error', () => {
        const state = Object.freeze({
          ...defaultState,
          inputFields: {
            ...defaultState.inputFields,
            port: initUserInput(''),
          },
          serverData: {
            ...defaultState.serverData,
            connection: {
              ...defaultState.serverData.connection,
              port: '',
            },
          },
        });

        const { inputFields } = reduce(state, {
          type: 'ldapConfig/setPort',
          payload: '123',
        });

        expect(inputFields.port.value).toBe('123');
        expect(inputFields.port.validationErrors).toEqual([]);
      });
    });

    describe('when auth !== NONE', () => {
      it('sets mustReenterPassword to true if port changed and password was not changed', () => {
        const state = Object.freeze({
          ...defaultState,
          inputFields: {
            ...defaultState.inputFields,
            port: initUserInput('123'),
            authenticationMethod: 'SIMPLE',
            systemPassword: initUserInput('oldpass'),
          },
          serverData: {
            ...defaultState.serverData,
            connection: {
              ...defaultState.serverData.connection,
              port: 123,
              authenticationMethod: 'SIMPLE',
              systemPassword: 'oldpass',
            },
          },
        });

        const { mustReenterPassword } = reduce(state, {
          type: 'ldapConfig/setPort',
          payload: '234',
        });

        expect(mustReenterPassword).toBe(true);
      });

      it('sets mustReenterPassword to false if port was changed but password already have been updated', () => {
        const state = Object.freeze({
          ...defaultState,
          inputFields: {
            ...defaultState.inputFields,
            port: initUserInput('123'),
            authenticationMethod: 'SIMPLE',
            systemPassword: initUserInput('oldpass1'),
          },
          serverData: {
            ...defaultState.serverData,
            connection: {
              ...defaultState.serverData.connection,
              port: 123,
              authenticationMethod: 'SIMPLE',
              systemPassword: 'oldpass',
            },
          },
        });

        const { mustReenterPassword } = reduce(state, {
          type: 'ldapConfig/setPort',
          payload: '234',
        });

        expect(mustReenterPassword).toBe(false);
      });

      it('sets mustReenterPassword to true if port was changed twice', () => {
        const state = Object.freeze({
          ...defaultState,
          reservedSystemPassword: 'oldpass',
          inputFields: {
            ...defaultState.inputFields,
            port: initUserInput('123'),
            authenticationMethod: 'SIMPLE',
            systemPassword: initUserInput('oldpass'),
          },
          serverData: {
            ...defaultState.serverData,
            connection: {
              ...defaultState.serverData.connection,
              port: 123,
              authenticationMethod: 'SIMPLE',
              systemPassword: 'oldpass',
            },
          },
        });

        const newState = reduce(state, { type: 'ldapConfig/setPort', payload: '234' });
        const { mustReenterPassword } = reduce(newState, { type: 'ldapConfig/setPort', payload: '2345' });

        expect(mustReenterPassword).toBe(true);
      });
    });
  });

  describe('ldapConfig/setSearchBase', () => {
    let defaultState = {
      other: otherObject,
      isDirty: false,
      inputFields: {
        ...initialState.inputFields,
        searchBase: initUserInput('dc=example,dc=com'),
      },
      serverData: {
        server: {},
        connection: {
          authenticationMethod: 'NONE',
          connectionTimeout: 31,
          hostname: 'ldap.forumsys.com',
          port: 389,
          protocol: 'LDAP',
          referralIgnored: true,
          retryDelay: 30,
          saslRealm: null,
          searchBase: 'dc=example,dc=com',
          systemUsername: 'uid=tesla,dc=example,dc=com',
        },
      },
    };

    describe('when payload is empty string', () => {
      it('sets the searchBase value and a validation error', () => {
        const state = Object.freeze({
          ...defaultState,
          inputFields: {
            ...defaultState.inputFields,
            searchBase: initUserInput('dc=example,dc=com'),
          },
          serverData: {
            ...defaultState.serverData,
            connection: {
              ...defaultState.serverData.connection,
              searchBase: 'dc=example,dc=com',
            },
          },
        });

        const { inputFields, isDirty } = reduce(state, {
          type: 'ldapConfig/setSearchBase',
          payload: '',
        });

        expect(inputFields.searchBase.value).toBe('');
        expect(inputFields.searchBase.validationErrors).toBeTruthy();
        expect(isDirty).toBe(true);
      });
    });

    describe('when payload is non-empty string', () => {
      it('sets the searchBase value and no validation error', () => {
        const state = Object.freeze({
          ...defaultState,
          inputFields: {
            ...defaultState.inputFields,
            searchBase: initUserInput(''),
          },
          serverData: {
            ...defaultState.serverData,
            connection: {
              ...defaultState.serverData.connection,
              searchBase: '',
            },
          },
        });

        const { inputFields } = reduce(state, {
          type: 'ldapConfig/setSearchBase',
          payload: 'searchBase',
        });

        expect(inputFields.searchBase.value).toBe('searchBase');
        expect(inputFields.searchBase.validationErrors).toBeNull();
      });
    });
  });

  describe('ldapConfig/setMethod', () => {
    it('sets authenticationMethod to payload', () => {
      const state = Object.freeze({
        other: otherObject,
        isDirty: false,
        inputFields: {
          ...initialState.inputFields,
          authenticationMethod: 'SIMPLE',
        },
        serverData: {
          server: {},
          connection: {
            authenticationMethod: 'SIMPLE',
            connectionTimeout: 31,
            hostname: 'ldap.forumsys.com',
            port: 389,
            protocol: 'LDAP',
            referralIgnored: true,
            retryDelay: 30,
            saslRealm: null,
            searchBase: 'dc=example,dc=com',
            systemUsername: 'uid=tesla,dc=example,dc=com',
          },
        },
      });

      const { isDirty, inputFields, other } = reduce(state, {
        type: 'ldapConfig/setMethod',
        payload: 'DIGESTMD5',
      });

      expect(isDirty).toBe(true);
      expect(inputFields.authenticationMethod).toBe('DIGESTMD5');
      expect(other).toBe(otherObject);
    });

    describe('when auth === NONE', () => {
      it('sets username, saslRealm and password to be blank', () => {
        const state = Object.freeze({
          ...initialState,
          inputFields: {
            ...initialState.inputFields,
            hostname: initUserInput('old host'),
            authenticationMethod: 'SIMPLE',
            saslRealm: initUserInput('testRealm'),
            systemUsername: initUserInput('testUsername'),
            systemPassword: initUserInput('testPassword'),
          },
        });

        const { inputFields } = reduce(state, {
          type: 'ldapConfig/setMethod',
          payload: 'NONE',
        });

        expect(inputFields.saslRealm.value).toBe('');
        expect(inputFields.systemUsername.value).toBe('');
        expect(inputFields.systemPassword.value).toBe('');
      });

      it('sets mustReenterPassword to false', () => {
        const state = Object.freeze({
          ...initialState,
          inputFields: {
            ...initialState.inputFields,
            hostname: initUserInput('old host'),
            authenticationMethod: 'SIMPLE',
            saslRealm: initUserInput('testRealm'),
            systemUsername: initUserInput('testUsername'),
            systemPassword: initUserInput('testPassword'),
          },
        });

        const { mustReenterPassword } = reduce(state, {
          type: 'ldapConfig/setMethod',
          payload: 'NONE',
        });

        expect(mustReenterPassword).toBe(false);
      });
    });
  });

  describe('group input actions', () => {
    let state;
    beforeEach(() => {
      state = {
        isDirty: false,
        inputFields: {
          serverName: initUserInput('server3'),
          userBaseDN: initUserInput(''),
          userSubtree: false,
          userObjectClass: initUserInput(''),
          userFilter: initUserInput(''),
          userIDAttribute: initUserInput(''),
          userRealNameAttribute: initUserInput(''),
          userEmailAttribute: initUserInput(''),
          userPasswordAttribute: initUserInput(''),
          groupMappingType: 'SIMPLE',
          groupBaseDN: initUserInput(''),
          groupSubtree: false,
          groupObjectClass: initUserInput('text'),
          groupIDAttribute: initUserInput(''),
          groupMemberAttribute: initUserInput(''),
          groupMemberFormat: initUserInput(''),
          userMemberOfGroupAttribute: initUserInput(''),
          dynamicGroupSearchEnabled: true,
        },
        serverData: {
          server: {
            id: '200',
            name: 'server3',
          },
          mapping: {
            serverId: '200',
            id: '404',
            dynamicGroupSearchEnabled: true,
            groupBaseDN: '',
            groupIDAttribute: '',
            groupMappingType: 'SIMPLE',
            groupMemberAttribute: '',
            groupMemberFormat: '',
            groupObjectClass: 'text',
            groupSubtree: false,
            userBaseDN: '',
            userEmailAttribute: '',
            userFilter: '',
            userIDAttribute: '',
            userMemberOfGroupAttribute: '',
            userObjectClass: '',
            userPasswordAttribute: '',
            userRealNameAttribute: '',
            userSubtree: false,
          },
        },
      };
    });

    describe('ldapConfig/setGroupObjectClass', () => {
      it('sets groupObjectClass to payload and isDirty', () => {
        state.inputFields.groupObjectClass = initUserInput('text');
        state.serverData.mapping.groupObjectClass = 'text';

        const { isDirty, inputFields } = reduce(state, {
          type: 'ldapConfig/setGroupObjectClass',
          payload: 'some text',
        });

        expect(isDirty).toBe(true);
        expect(inputFields.groupObjectClass.value).toBe('some text');
      });
    });

    describe('ldapConfig/setGroupBaseDN', () => {
      it('sets groupBaseDN to payload and isDirty', () => {
        state.inputFields.groupBaseDN = initUserInput('not base DN');
        state.serverData.mapping.groupBaseDN = 'not base DN';

        const { isDirty, inputFields } = reduce(state, {
          type: 'ldapConfig/setGroupBaseDN',
          payload: 'base DN',
        });

        expect(isDirty).toBe(true);
        expect(inputFields.groupBaseDN.value).toBe('base DN');
      });
    });

    describe('ldapConfig/setGroupIDAttribute', () => {
      it('sets groupIDAttribute to payload and isDirty', () => {
        state.inputFields.groupIDAttribute = initUserInput('old ID attribute');
        state.serverData.mapping.groupIDAttribute = 'old ID attribute';

        const { isDirty, inputFields } = reduce(state, {
          type: 'ldapConfig/setGroupIDAttribute',
          payload: 'new ID attribute',
        });

        expect(isDirty).toBe(true);
        expect(inputFields.groupIDAttribute.value).toBe('new ID attribute');
      });
    });

    describe('ldapConfig/setGroupMemberAttribute', () => {
      it('sets groupMemberAttribute to payload and isDirty', () => {
        state.inputFields.groupMemberAttribute = initUserInput('old member attribute');
        state.serverData.mapping.groupMemberAttribute = 'old member attribute';

        const { isDirty, inputFields } = reduce(state, {
          type: 'ldapConfig/setGroupMemberAttribute',
          payload: 'new member attribute',
        });

        expect(isDirty).toBe(true);
        expect(inputFields.groupMemberAttribute.value).toBe('new member attribute');
      });
    });

    describe('ldapConfig/setGroupMemberFormat', () => {
      it('sets groupMemberFormat to payload and isDirty', () => {
        state.inputFields.groupMemberFormat = initUserInput('old member format');
        state.serverData.mapping.groupMemberFormat = 'old member format';

        const { isDirty, inputFields } = reduce(state, {
          type: 'ldapConfig/setGroupMemberFormat',
          payload: 'new member format',
        });

        expect(isDirty).toBe(true);
        expect(inputFields.groupMemberFormat.value).toBe('new member format');
      });
    });

    describe('ldapConfig/setUserMemberOfGroupAttribute', () => {
      it('sets groupMemberFormat to payload and isDirty', () => {
        state.inputFields.userMemberOfGroupAttribute = initUserInput('old userMemberOfGroupAttribute');
        state.serverData.mapping.userMemberOfGroupAttribute = 'old userMemberOfGroupAttribute';

        const { isDirty, inputFields } = reduce(state, {
          type: 'ldapConfig/setUserMemberOfGroupAttribute',
          payload: 'new userMemberOfGroupAttribute',
        });

        expect(isDirty).toBe(true);
        expect(inputFields.userMemberOfGroupAttribute.value).toBe('new userMemberOfGroupAttribute');
      });
    });
  });

  describe('ldapConfig/setSaslRealm', () => {
    it('sets setSaslRealm to payload', () => {
      const state = Object.freeze({
        other: otherObject,
        isDirty: false,
        inputFields: {
          ...initialState.inputFields,
          saslRealm: initUserInput(''),
        },
        serverData: {
          server: {},
          connection: {
            authenticationMethod: 'SIMPLE',
            connectionTimeout: 31,
            hostname: 'ldap.forumsys.com',
            port: 389,
            protocol: 'LDAP',
            referralIgnored: true,
            retryDelay: 30,
            saslRealm: null,
            searchBase: 'dc=example,dc=com',
            systemUsername: 'uid=tesla,dc=example,dc=com',
          },
        },
      });

      const { isDirty, inputFields, other } = reduce(state, {
        type: 'ldapConfig/setSaslRealm',
        payload: 'sasl',
      });

      expect(isDirty).toBe(true);
      expect(inputFields.saslRealm.value).toBe('sasl');
      expect(other).toBe(otherObject);
    });
  });

  describe('ldapConfig/setUsername', () => {
    let defaultState = {
      other: otherObject,
      isDirty: false,
      inputFields: {
        ...initialState.inputFields,
        searchBase: initUserInput('dc=example,dc=com'),
      },
      serverData: {
        server: {},
        connection: {
          authenticationMethod: 'NONE',
          connectionTimeout: 31,
          hostname: 'ldap.forumsys.com',
          port: 389,
          protocol: 'LDAP',
          referralIgnored: true,
          retryDelay: 30,
          saslRealm: null,
          searchBase: 'dc=example,dc=com',
          systemUsername: 'uid=tesla,dc=example,dc=com',
        },
      },
    };

    describe('when payload is empty string', () => {
      it('sets the systemUsername value and a validation error', () => {
        const state = Object.freeze({
          ...defaultState,
          inputFields: {
            ...defaultState.inputFields,
            systemUsername: initUserInput('uid=tesla,dc=example,dc=com'),
          },
          serverData: {
            ...defaultState.serverData,
            connection: {
              ...defaultState.serverData.connection,
              systemUsername: 'uid=tesla,dc=example,dc=com',
            },
          },
        });

        const { inputFields, isDirty } = reduce(state, {
          type: 'ldapConfig/setUsername',
          payload: '',
        });

        expect(inputFields.systemUsername.value).toBe('');
        expect(inputFields.systemUsername.validationErrors).toBeTruthy();
        expect(isDirty).toBe(true);
      });
    });

    describe('when payload is non-empty string', () => {
      it('sets the systemUsername value and no validation error', () => {
        const state = Object.freeze({
          ...defaultState,
          inputFields: {
            ...defaultState.inputFields,
            systemUsername: initUserInput(''),
          },
          serverData: {
            ...defaultState.serverData,
            connection: {
              ...defaultState.serverData.connection,
              systemUsername: '',
            },
          },
        });

        const { inputFields } = reduce(state, {
          type: 'ldapConfig/setUsername',
          payload: 'uid=tesla,dc=example,dc=com',
        });

        expect(inputFields.systemUsername.value).toBe('uid=tesla,dc=example,dc=com');
        expect(inputFields.systemUsername.validationErrors).toBeNull();
      });
    });
  });

  describe('ldapConfig/setPassword', () => {
    let defaultState = {
      other: otherObject,
      isDirty: false,
      reservedSystemPassword: null,
      inputFields: {
        ...initialState.inputFields,
      },
      serverData: {
        server: {},
        connection: {
          authenticationMethod: 'NONE',
          connectionTimeout: 31,
          hostname: 'ldap.forumsys.com',
          port: 389,
          protocol: 'LDAP',
          referralIgnored: true,
          retryDelay: 30,
          saslRealm: null,
          searchBase: 'dc=example,dc=com',
          systemUsername: 'uid=tesla,dc=example,dc=com',
        },
      },
    };

    describe('when payload is empty string', () => {
      it('sets the systemPassword value and a validation error', () => {
        const state = Object.freeze({
          ...defaultState,
          inputFields: {
            ...defaultState.inputFields,
            systemPassword: initUserInput('FAKE'),
          },
          serverData: {
            ...defaultState.serverData,
            connection: {
              ...defaultState.serverData.connection,
              systemPassword: 'FAKE',
            },
          },
        });

        const { inputFields, reservedSystemPassword, isDirty } = reduce(state, {
          type: 'ldapConfig/setPassword',
          payload: '',
        });

        expect(inputFields.systemPassword.value).toBe('');
        expect(reservedSystemPassword).toBe('');
        expect(inputFields.systemPassword.validationErrors).toBeTruthy();
        expect(isDirty).toBe(true);
      });
    });

    describe('when payload is non-empty string', () => {
      it('sets the systemPassword value and no validation error', () => {
        const state = Object.freeze({
          ...defaultState,
          inputFields: {
            ...defaultState.inputFields,
            systemPassword: initUserInput(''),
          },
          serverData: {
            ...defaultState.serverData,
            connection: {
              ...defaultState.serverData.connection,
              systemPassword: '',
            },
          },
        });

        const { inputFields, reservedSystemPassword, isDirty } = reduce(state, {
          type: 'ldapConfig/setPassword',
          payload: 'NOT-FAKE',
        });

        expect(inputFields.systemPassword.value).toBe('NOT-FAKE');
        expect(inputFields.systemPassword.validationErrors).toBeNull();
        expect(reservedSystemPassword).toBe('NOT-FAKE');
        expect(isDirty).toBe(true);
      });
    });
  });

  describe('ldapConfig/setConnection', () => {
    let defaultState = {
      other: otherObject,
      isDirty: false,
      inputFields: {
        ...initialState.inputFields,
      },
      serverData: {
        server: {},
        connection: {
          authenticationMethod: 'NONE',
          connectionTimeout: 31,
          hostname: 'ldap.forumsys.com',
          port: 389,
          protocol: 'LDAP',
          referralIgnored: true,
          retryDelay: 30,
          saslRealm: null,
          searchBase: 'dc=example,dc=com',
          systemUsername: 'uid=tesla,dc=example,dc=com',
        },
      },
    };

    describe('when payload is empty string', () => {
      it('sets the connectionTimeout value and a validation error', () => {
        const state = Object.freeze({
          ...defaultState,
          inputFields: {
            ...defaultState.inputFields,
            connectionTimeout: initUserInput('31'),
          },
          serverData: {
            ...defaultState.serverData,
            connection: {
              ...defaultState.serverData.connection,
              connectionTimeout: 31,
            },
          },
        });

        const { inputFields, isDirty } = reduce(state, {
          type: 'ldapConfig/setConnection',
          payload: '',
        });

        expect(inputFields.connectionTimeout.value).toBe('');
        expect(inputFields.connectionTimeout.validationErrors).toBeFalsy();
        expect(isDirty).toBe(true);
      });
    });

    describe('when payload is non-empty string', () => {
      let state;
      beforeEach(() => {
        state = Object.freeze({
          ...defaultState,
          inputFields: {
            ...defaultState.inputFields,
            connectionTimeout: initUserInput('0'),
          },
          serverData: {
            ...defaultState.serverData,
            connection: {
              ...defaultState.serverData.connection,
              connectionTimeout: 0,
            },
          },
        });
      });

      it('sets the connectionTimeout value and validation error', () => {
        const { inputFields } = reduce(state, {
          type: 'ldapConfig/setConnection',
          payload: '9999',
        });

        expect(inputFields.connectionTimeout.value).toBe('9999');
        expect(inputFields.connectionTimeout.validationErrors).toBe('Integer between 0 (default) to 999');
      });

      it('sets the connectionTimeout value and no validation error', () => {
        const { inputFields } = reduce(state, {
          type: 'ldapConfig/setConnection',
          payload: '8',
        });

        expect(inputFields.connectionTimeout.value).toBe('8');
        expect(inputFields.connectionTimeout.validationErrors).toBeNull();
      });
    });
  });

  describe('ldapConfig/setInitialAddForm', () => {
    it('resets serverData serverName and inputFields serverName', () => {
      const state = {
        inputFields: {
          serverName: 'some name',
        },
        serverData: {
          serverName: 'some old name',
        },
      };

      const { inputFields, serverData } = reduce(state, { type: 'ldapConfig/setInitialAddForm' });

      expect(inputFields.serverName.value).toBe('');
      expect(serverData.serverName).toBe('');
    });
  });

  describe('ldapConfig/setRetryDelay', () => {
    let defaultState = {
      other: otherObject,
      isDirty: false,
      inputFields: {
        ...initialState.inputFields,
      },
      serverData: {
        server: {},
        connection: {
          authenticationMethod: 'NONE',
          connectionTimeout: 31,
          hostname: 'ldap.forumsys.com',
          port: 389,
          protocol: 'LDAP',
          referralIgnored: true,
          retryDelay: 30,
          saslRealm: null,
          searchBase: 'dc=example,dc=com',
          systemUsername: 'uid=tesla,dc=example,dc=com',
        },
      },
    };

    describe('when payload is empty string', () => {
      it('sets the retryDelay value and a validation error', () => {
        const state = Object.freeze({
          ...defaultState,
          inputFields: {
            ...defaultState.inputFields,
            retryDelay: initUserInput('31'),
          },
          serverData: {
            ...defaultState.serverData,
            connection: {
              ...defaultState.serverData.connection,
              retryDelay: 31,
            },
          },
        });

        const { inputFields, isDirty } = reduce(state, {
          type: 'ldapConfig/setRetryDelay',
          payload: '',
        });

        expect(inputFields.retryDelay.value).toBe('');
        expect(inputFields.retryDelay.validationErrors).toBeFalsy();
        expect(isDirty).toBe(true);
      });
    });

    describe('when payload is non-empty string', () => {
      let state;
      beforeEach(() => {
        state = Object.freeze({
          ...defaultState,
          inputFields: {
            ...defaultState.inputFields,
            retryDelay: initUserInput('0'),
          },
          serverData: {
            ...defaultState.serverData,
            connection: {
              ...defaultState.serverData.connection,
              retryDelay: 0,
            },
          },
        });
      });

      it('sets the retryDelay value and validation error', () => {
        const { inputFields } = reduce(state, {
          type: 'ldapConfig/setRetryDelay',
          payload: '9999',
        });

        expect(inputFields.retryDelay.value).toBe('9999');
        expect(inputFields.retryDelay.validationErrors).toBe('Integer between 0 (default) to 999');
      });

      it('sets the retryDelay value and no validation error', () => {
        const { inputFields } = reduce(state, {
          type: 'ldapConfig/setRetryDelay',
          payload: '8',
        });

        expect(inputFields.retryDelay.value).toBe('8');
        expect(inputFields.retryDelay.validationErrors).toBeNull();
      });
    });
  });

  describe('ldapConfig/removeServer/pending action', () => {
    it('sets removeMaskState to false', () => {
      const state = Object.freeze({
        other: otherObject,
        removeMaskState: true,
      });

      const { removeMaskState, other } = reduce(state, {
        type: 'ldapConfig/removeServer/pending',
      });

      expect(removeMaskState).toBe(false);
      expect(other).toBe(otherObject);
    });
  });

  describe('ldapConfig/removeServer/fulfilled action', () => {
    it('resets input fields, isDirty, loading and mask', () => {
      const state = Object.freeze({
        other: otherObject,
        removeMaskState: false,
        removeError: 'error!',
        isDirty: true,
      });

      const { removeMaskState, removeError, other, isDirty } = reduce(state, {
        type: 'ldapConfig/removeServer/fulfilled',
      });

      expect(removeError).toBeNull();
      expect(isDirty).toBe(false);
      expect(removeMaskState).toBe(true);
      expect(other).toBe(otherObject);
    });
  });

  describe('ldapConfig/removeServer/rejected action', () => {
    it('sets removeMaskState to null, sets removeError to payload', () => {
      const state = Object.freeze({
        other: otherObject,
        loading: true,
        removeError: null,
        removeMaskState: false,
      });

      const { removeError, removeMaskState, other } = reduce(state, {
        type: 'ldapConfig/removeServer/rejected',
        payload: 'remove error',
      });

      expect(removeError).toBe('remove error');
      expect(removeMaskState).toBeNull();
      expect(other).toBe(otherObject);
    });
  });

  describe('ldapConfig/testConnection/pending', () => {
    it('sets submitMaskMessage to Testing and saveMaskState to false', () => {
      const state = Object.freeze({
        other: otherObject,
        saveMaskState: true,
        submitMaskMessage: null,
      });

      const { saveMaskState, submitMaskMessage, other } = reduce(state, {
        type: 'ldapConfig/testConnection/pending',
      });

      expect(saveMaskState).toBe(false);
      expect(submitMaskMessage).toBe('Testing…');
      expect(other).toBe(otherObject);
    });
  });

  describe('ldapConfig/testConnection/fulfilled', () => {
    it('sets saveMaskState to true and successMessage to Success!', () => {
      const state = Object.freeze({
        other: otherObject,
        saveMaskState: false,
        successMessage: '',
      });

      const { saveMaskState, successMessage, other } = reduce(state, {
        type: 'ldapConfig/testConnection/fulfilled',
      });

      expect(successMessage).toBe('Success!');
      expect(saveMaskState).toBe(true);
      expect(other).toBe(otherObject);
    });
  });

  describe('ldapConfig/testConnection/rejected action', () => {
    it('sets saveMaskState to null, sets testConnectionErrorMessage to payload', () => {
      const state = Object.freeze({
        other: otherObject,
        testConnectionErrorMessage: '',
        saveMaskState: false,
      });

      const { testConnectionErrorMessage, saveMaskState, other } = reduce(state, {
        type: 'ldapConfig/testConnection/rejected',
        payload: 'test connection error',
      });

      expect(testConnectionErrorMessage).toBe('test connection error');
      expect(saveMaskState).toBeNull();
      expect(other).toBe(otherObject);
    });
  });

  const submitPending = (action) => {
    it('sets submitMaskMessage to Saving and saveMaskState to false, and clears errors', () => {
      const state = Object.freeze({
        other: otherObject,
        saveMaskState: true,
        submitMaskMessage: null,
        saveError: 'previous error',
        loadError: 'some load error',
      });

      const { saveMaskState, submitMaskMessage, saveError, loadError, other } = reduce(state, {
        type: action,
      });

      expect(saveMaskState).toBe(false);
      expect(submitMaskMessage).toBe('Saving…');
      expect(saveError).toBeNull();
      expect(loadError).toBeNull();
      expect(other).toBe(otherObject);
    });
  };

  const submitFailed = (action) => {
    it('sets saveMaskState to null, sets saveError to payload', () => {
      const state = Object.freeze({
        other: otherObject,
        loading: true,
        saveError: null,
        saveMaskState: false,
      });

      const { saveError, saveMaskState, other } = reduce(state, {
        type: action,
        payload: 'submit failed error',
      });

      expect(saveError).toBe('submit failed error');
      expect(saveMaskState).toBeNull();
      expect(other).toBe(otherObject);
    });
  };

  describe('ldapConfig/saveConnection/pending action', () => {
    submitPending('ldapConfig/saveConnection/pending');
  });

  describe('ldapConfig/saveConnection/fulfilled action', () => {
    it('sets loading to false, clears errors, updates inputFields and serverData', () => {
      const state = Object.freeze({
        saveError: 'error',
        loadError: 'foo',
        successMessage: '',
        saveMaskState: false,
        isDirty: true,
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
        },
      });

      const payload = {
        server: {
          name: 'server3',
          id: '200',
        },
        id: null,
        authenticationMethod: 'NONE',
        connectionTimeout: 30,
        hostname: null,
        port: 389,
        protocol: 'LDAP',
        referralIgnored: false,
        retryDelay: 30,
        saslRealm: null,
        searchBase: null,
        serverId: '200',
        systemPassword: 'FAKE_PASSWORD',
        systemUsername: null,
      };

      const {
        successMessage,
        saveError,
        saveMaskState,
        loadError,
        inputFields,
        serverData,
        isDirty,
        reservedSystemPassword,
      } = reduce(state, {
        type: 'ldapConfig/saveConnection/fulfilled',
        payload,
      });

      expect(saveError).toBeNull();
      expect(loadError).toBeNull();
      expect(isDirty).toBe(false);
      expect(successMessage).toBe('Configuration saved.');
      expect(reservedSystemPassword).toBe('FAKE_PASSWORD');
      expect(saveMaskState).toBe(true);
      expect(inputFields).toEqual({
        serverName: initUserInput('server3'),
        protocol: 'LDAP',
        port: initUserInput('389'),
        hostname: initUserInput(''),
        searchBase: initUserInput(''),
        authenticationMethod: 'NONE',
        saslRealm: initUserInput(''),
        systemUsername: initUserInput(''),
        systemPassword: initUserInput('FAKE_PASSWORD'),
        connectionTimeout: initUserInput('30'),
        retryDelay: initUserInput('30'),
        referralIgnored: false,
      });

      expect(serverData).toEqual({
        serverName: 'server3',
        server: payload.server,
        connection: omit(['server'], payload),
      });
    });
  });

  describe('ldapConfig/saveConnection/rejected action', () => {
    submitFailed('ldapConfig/saveConnection/rejected');
  });

  describe('ldapConfig/saveUserAndGroupSettings/pending action', () => {
    submitPending('ldapConfig/saveUserAndGroupSettings/pending');
  });

  describe('ldapConfig/saveUserAndGroupSettings/fulfilled action', () => {
    it('updates inputFields and serverData', () => {
      const state = Object.freeze({
        saveError: 'error',
        loadError: 'foo',
        successMessage: '',
        saveMaskState: false,
        isDirty: true,
        inputFields: {
          serverName: initUserInput(''),
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
        serverData: {},
      });

      const payload = {
        server: {
          name: 'server3',
          id: '200',
        },
        dynamicGroupSearchEnabled: true,
        groupBaseDN: '',
        groupIDAttribute: '',
        groupMappingType: 'SIMPLE',
        groupMemberAttribute: 'qwerty',
        groupMemberFormat: 'asdf',
        groupObjectClass: 'zxcv',
        groupSubtree: true,
        id: '404',
        serverId: '200',
        userBaseDN: '',
        userEmailAttribute: 'email',
        userFilter: 'amet',
        userIDAttribute: 'lorem',
        userMemberOfGroupAttribute: '',
        userObjectClass: 'ipsum',
        userPasswordAttribute: 'dolor',
        userRealNameAttribute: 'sit',
        userSubtree: true,
      };

      const { successMessage, saveError, saveMaskState, loadError, inputFields, serverData, isDirty } = reduce(state, {
        type: 'ldapConfig/saveUserAndGroupSettings/fulfilled',
        payload,
      });

      expect(saveError).toBeNull();
      expect(loadError).toBeNull();
      expect(isDirty).toBe(false);
      expect(successMessage).toBe('Configuration saved.');
      expect(saveMaskState).toBe(true);
      expect(inputFields).toEqual({
        serverName: initUserInput('server3'),
        dynamicGroupSearchEnabled: true,
        groupBaseDN: initUserInput(''),
        groupIDAttribute: initUserInput(''),
        groupMappingType: 'SIMPLE',
        groupMemberAttribute: initUserInput('qwerty'),
        groupMemberFormat: initUserInput('asdf'),
        groupObjectClass: initUserInput('zxcv'),
        groupSubtree: true,
        userBaseDN: initUserInput(''),
        userEmailAttribute: initUserInput('email'),
        userFilter: initUserInput('amet'),
        userIDAttribute: initUserInput('lorem'),
        userMemberOfGroupAttribute: initUserInput(''),
        userObjectClass: initUserInput('ipsum'),
        userPasswordAttribute: initUserInput('dolor'),
        userRealNameAttribute: initUserInput('sit'),
        userSubtree: true,
      });

      expect(serverData).toEqual({ serverName: 'server3', server: payload.server, mapping: omit(['server'], payload) });
    });
  });

  describe('ldapConfig/saveUserAndGroupSettings/pending action', () => {
    submitFailed('ldapConfig/saveUserAndGroupSettings/rejected');
  });

  describe('ldapConfig/checkLogin/fulfilled', () => {
    let initialState, newState;
    const action = {
      type: 'ldapConfig/checkLogin/fulfilled',
    };

    beforeEach(() => {
      initialState = {
        otherObject,
        checkLoginProps: {
          checkLoginSuccess: false,
          checkLoginError: 'some error',
        },
      };

      newState = reduce(initialState, action);
    });

    it('checks for immutability', () => {
      expect(newState.otherObject).toBe(otherObject);
    });

    it('sets checkLoginSuccess to true', () => {
      expect(newState.checkLoginProps.checkLoginSuccess).toBe(true);
    });

    it('sets checkLoginError to null', () => {
      expect(newState.checkLoginProps.checkLoginError).toBeNull();
    });
  });

  describe('ldapConfig/setInputField', () => {
    let initialState, newState;

    beforeEach(() => {
      initialState = {
        otherObject,
        checkLoginProps: {
          username: initUserInput(''),
          password: initUserInput(''),
        },
      };
    });

    describe('username', () => {
      const action = {
        type: 'ldapConfig/setInputField',
        payload: { field: 'username', value: 'some value' },
      };

      beforeEach(() => {
        newState = reduce(initialState, action);
      });

      it('checks for immutability', () => {
        expect(newState.otherObject).toBe(otherObject);
      });

      it('sets username field to the user input', () => {
        expect(newState.checkLoginProps.username.trimmedValue).toBe('some value');
      });
    });

    describe('password', () => {
      const action = {
        type: 'ldapConfig/setInputField',
        payload: { field: 'password', value: 'some value' },
      };

      beforeEach(() => {
        newState = reduce(initialState, action);
      });

      it('checks for immutability', () => {
        expect(newState.otherObject).toBe(otherObject);
      });

      it('sets password field to the user input', () => {
        expect(newState.checkLoginProps.password.trimmedValue).toBe('some value');
      });
    });
  });

  describe('ldapConfig/clearCheckLoginAlerts', () => {
    let initialState, newState;
    const action = {
      type: 'ldapConfig/clearCheckLoginAlerts',
    };

    beforeEach(() => {
      initialState = {
        otherObject,
        checkLoginProps: {
          checkLoginError: 'some error',
          checkLoginSuccess: true,
        },
      };

      newState = reduce(initialState, action);
    });

    it('checks for immutability', () => expect(newState.otherObject).toBe(otherObject));

    it('sets checkLoginError to null', () => expect(newState.checkLoginProps.checkLoginError).toBeNull());

    it('sets checkLoginSuccess to false', () => expect(newState.checkLoginProps.checkLoginSuccess).toBe(false));
  });

  describe('ldapConfig/resetCheckLoginModal', () => {
    let state, newState;
    const action = {
      type: 'ldapConfig/resetCheckLoginModal',
    };

    beforeEach(() => {
      state = {
        otherObject,
        checkLoginProps: {
          username: userInput('username'),
          password: userInput('password'),
          checkLoginSuccess: false,
          checkLoginError: 'some error',
        },
      };
      newState = reduce(state, action);
    });

    it('checks for immutability', () => {
      expect(newState.otherObject).toBe(otherObject);
    });

    it('resets the checkLoginProps', () => {
      expect(newState.checkLoginProps).toEqual(initialState.checkLoginProps);
    });
  });

  describe('ldapConfig/loadUserMapping', () => {
    describe('pending', () => {
      let state, newState;
      const action = {
        type: 'ldapConfig/loadUserMapping/pending',
      };

      beforeAll(() => {
        state = {
          userMapping: {
            isLdapUserMappingModalOpen: false,
            userList: [],
            loadError: 'error',
            sortAscending: true,
          },
        };
        newState = reduce(state, action);
      });

      it('sets userList to null', () => {
        expect(newState.userMapping.userList).toEqual(null);
      });
      it('sets loadError to null', () => {
        expect(newState.userMapping.userList).toEqual(null);
      });
    });

    describe('fulfilled', () => {
      let newState;
      const action = {
        type: 'ldapConfig/loadUserMapping/fulfilled',
        payload: [],
      };

      beforeAll(() => {
        newState = reduce(initialState, action);
      });

      it('sets the userList to payload', () => {
        expect(newState.userMapping.userList).toEqual(action.payload);
      });
      it('sets loadError to null', () => {
        expect(newState.userMapping.loadError).toEqual(null);
      });
    });

    describe('rejected', () => {
      let newState;
      const action = {
        type: 'ldapConfig/loadUserMapping/rejected',
        payload: 'msg',
      };

      beforeAll(() => {
        newState = reduce(initialState, action);
      });

      it('sets the userList to null', () => {
        expect(newState.userMapping.userList).toEqual(null);
      });
      it('sets loadError to payload', () => {
        expect(newState.userMapping.loadError).toEqual(action.payload);
      });
    });
  });

  describe('toggleUserMappingModalIsOpen', () => {
    it('toggles toggleUserMappingModalIsOpen', () => {
      const action = {
        type: 'ldapConfig/toggleUserMappingModalIsOpen',
      };
      const newState = reduce(initialState, action);

      expect(newState.userMapping.isLdapUserMappingModalOpen).toEqual(true);
    });
  });

  describe('toggleUserMappingSortOrder', () => {
    it('toggles sortAscending', () => {
      const action = {
        type: 'ldapConfig/toggleUserMappingSortOrder',
      };
      const newState = reduce(initialState, action);

      expect(newState.userMapping.sortAscending).toEqual(false);
    });
  });
});
