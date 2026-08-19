/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import reducer from 'MainRoot/configuration/saml/samlConfigurationSlice';
import { uriTemplate } from 'MainRoot/util/urlUtil';

const SET_CONFIGURATION_VALUES = 'samlConfiguration/setConfigurationValues';
const SET_SELECT_CONFIGURATION_VALUES = 'samlConfiguration/setSelectConfigurationValues';
const RESTORE_DEFAULT_CONFIGURATION_VALUES = 'samlConfiguration/restoreDefaultConfigurationValues';
const LOAD_SAML_CONFIGURATION_REQUESTED = 'samlConfiguration/loadSAMLConfiguration/pending';
const LOAD_SAML_CONFIGURATION_FULFILLED = 'samlConfiguration/loadSAMLConfiguration/fulfilled';
const LOAD_SAML_CONFIGURATION_FAILED = 'samlConfiguration/loadSAMLConfiguration/rejected';
const UPDATE_SAML_CONFIGURATION_REQUESTED = 'samlConfiguration/updateSAMLConfiguration/pending';
const UPDATE_SAML_CONFIGURATION_FULFILLED = 'samlConfiguration/updateSAMLConfiguration/fulfilled';
const UPDATE_SAML_CONFIGURATION_FAILED = 'samlConfiguration/updateSAMLConfiguration/rejected';
const DELETE_SAML_CONFIGURATION_REQUESTED = 'samlConfiguration/deleteSAMLConfiguration/pending';
const DELETE_SAML_CONFIGURATION_FULFILLED = 'samlConfiguration/deleteSAMLConfiguration/fulfilled';
const DELETE_SAML_CONFIGURATION_FAILED = 'samlConfiguration/deleteSAMLConfiguration/rejected';

const { initialState: initUserInput } = nxTextInputStateHelpers;

describe('samlConfigurationReducer', () => {
  let mockState;

  beforeEach(() => {
    mockState = {
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
    };
  });

  describe('setConfigurationValues action', () => {
    const inputFields = [
      'identityProviderName',
      'entityId',
      'usernameAttributeName',
      'firstNameAttributeName',
      'lastNameAttributeName',
      'emailAttributeName',
      'groupsAttributeName',
      'identityProviderMetadataXml',
    ];

    for (let fieldKey of inputFields) {
      it(`change configuration value for ${fieldKey} field`, () => {
        const newState = reducer(mockState, {
          type: SET_CONFIGURATION_VALUES,
          payload: {
            name: fieldKey,
            value: `test value ${fieldKey}`,
          },
        });
        expect(newState.configurationValues[fieldKey].value).toBe(`test value ${fieldKey}`);
        expect(newState.isDirty).toBe(true);
      });
    }
  });

  describe('setSelectConfigurationValues action', () => {
    const inputFields = ['validateResponseSignature', 'validateAssertionSignature'];

    for (let fieldKey of inputFields) {
      it(`change ${fieldKey} select configuration values`, () => {
        const newState = reducer(mockState, {
          type: SET_SELECT_CONFIGURATION_VALUES,
          payload: {
            name: fieldKey,
            value: 'true',
          },
        });
        expect(newState.configurationValues[fieldKey]).toBe('true');
        expect(newState.isDirty).toBe(true);
      });
    }
  });

  describe('restoreDefaultConfigurationValues action', () => {
    it('restore to saved configuration values', () => {
      const newState = reducer(
        {
          ...mockState,
          loadedConfigurationValues: { ...mockState.configurationValues, validateResponseSignature: 'true' },
        },
        {
          type: RESTORE_DEFAULT_CONFIGURATION_VALUES,
        }
      );
      expect(newState.configurationValues.validateResponseSignature).toBe('true');
    });

    it('restore to default configuration values', () => {
      const newState = reducer(
        { ...mockState, configurationValues: { ...mockState.configurationValues, validateResponseSignature: 'true' } },
        {
          type: RESTORE_DEFAULT_CONFIGURATION_VALUES,
        }
      );
      expect(newState.configurationValues.validateResponseSignature).toBe('null');
    });
  });

  describe('setConfigurationValues action', () => {
    it('change configuration values', () => {
      const newState = reducer(mockState, {
        type: SET_CONFIGURATION_VALUES,
        payload: {
          name: 'identityProviderName',
          value: 'test',
        },
      });
      expect(newState.configurationValues.identityProviderName.value).toBe('test');
    });
  });

  describe('LOAD_SAML_CONFIGURATION_REQUESTED action', function () {
    it('sets isLoading flag to true', function () {
      const newState = reducer(mockState, {
        type: LOAD_SAML_CONFIGURATION_REQUESTED,
      });
      expect(newState.isLoading).toBe(true);
    });
  });

  describe('LOAD_SAML_CONFIGURATION_FULFILLED action', function () {
    it('sets configuration values', function () {
      const newState = reducer(mockState, {
        type: LOAD_SAML_CONFIGURATION_FULFILLED,
        payload: {
          data: {
            identityProviderName: 'identity provider test',
            entityId: uriTemplate`/api/v2/config/saml/metadata`,
            usernameAttributeName: 'username test',
            firstNameAttributeName: 'firstName test',
            lastNameAttributeName: 'lastName test',
            emailAttributeName: 'email test',
            groupsAttributeName: 'groups test',
            identityProviderMetadataXml: 'test',
            validateResponseSignature: 'true',
            validateAssertionSignature: 'true',
          },
        },
      });

      expect(newState.isLoading).toEqual(false);
      expect(newState.isConfigured).toEqual(true);
      expect(newState.loadError).toEqual(null);
      expect(newState.configurationValues.validateResponseSignature).toEqual('true');
      expect(newState.loadedConfigurationValues.validateResponseSignature).toEqual('true');
    });
  });

  describe('LOAD_SAML_CONFIGURATION_FAILED action', function () {
    it('adds loadError value', function () {
      const newState = reducer(mockState, {
        type: LOAD_SAML_CONFIGURATION_FAILED,
        payload: { response: {} },
      });

      expect(newState.isConfigured).toEqual(false);
      expect(newState.isLoading).toEqual(false);
      expect(newState.loadError).toEqual('Error');
    });
    it('sets to null loadError value', function () {
      const newState = reducer(mockState, {
        type: LOAD_SAML_CONFIGURATION_FAILED,
        payload: { response: { status: 404 } },
      });

      expect(newState.isConfigured).toEqual(false);
      expect(newState.isLoading).toEqual(false);
      expect(newState.loadError).toEqual(null);
    });
  });

  describe('UPDATE_SAML_CONFIGURATION_REQUESTED action', function () {
    it('sets submitState flag to false', function () {
      const newState = reducer(mockState, {
        type: UPDATE_SAML_CONFIGURATION_REQUESTED,
      });
      expect(newState.submitState).toBe(false);
    });
  });

  describe('UPDATE_SAML_CONFIGURATION_FULFILLED action', function () {
    it('updates loadedConfigurationValues', function () {
      const newState = reducer(mockState, {
        type: UPDATE_SAML_CONFIGURATION_FULFILLED,
      });

      expect(newState.submitState).toEqual(true);
      expect(newState.isConfigured).toEqual(true);
      expect(newState.submitMaskError).toEqual(null);
      expect(newState.loadedConfigurationValues.validateResponseSignature).toEqual('null');
      expect(newState.isDirty).toEqual(false);
    });
  });

  describe('UPDATE_SAML_CONFIGURATION_FAILED action', function () {
    it('adds submitMaskError value', function () {
      const newState = reducer(mockState, {
        type: UPDATE_SAML_CONFIGURATION_FAILED,
        payload: {},
      });

      expect(newState.isConfigured).toEqual(false);
      expect(newState.submitState).toEqual(null);
      expect(newState.submitMaskError).toEqual('Error');
    });
  });

  describe('DELETE_SAML_CONFIGURATION_REQUESTED action', function () {
    it('sets submitState flag to false', function () {
      const newState = reducer(mockState, {
        type: DELETE_SAML_CONFIGURATION_REQUESTED,
      });
      expect(newState.submitState).toBe(false);
      expect(newState.isDeleteModalShown).toBe(false);
    });
  });

  describe('DELETE_SAML_CONFIGURATION_FULFILLED action', function () {
    it('restore configurationValues to default values', function () {
      const newState = reducer(
        { ...mockState, configurationValues: { ...mockState.configurationValues, validateResponseSignature: 'true' } },
        {
          type: DELETE_SAML_CONFIGURATION_FULFILLED,
        }
      );

      expect(newState.submitState).toEqual(true);
      expect(newState.isConfigured).toEqual(false);
      expect(newState.submitMaskError).toEqual(null);
      expect(newState.isDeleteModalShown).toBe(false);
      expect(newState.loadedConfigurationValues).toEqual(null);
      expect(newState.configurationValues.validateResponseSignature).toEqual('null');
      expect(newState.isDirty).toEqual(false);
    });
  });

  describe('DELETE_SAML_CONFIGURATION_FAILED action', function () {
    it('adds submitMaskError value', function () {
      const newState = reducer(mockState, {
        type: DELETE_SAML_CONFIGURATION_FAILED,
        payload: {},
      });

      expect(newState.isConfigured).toEqual(false);
      expect(newState.submitState).toEqual(null);
      expect(newState.isDeleteModalShown).toBe(false);
      expect(newState.submitMaskError).toEqual('Error');
    });
  });
});
