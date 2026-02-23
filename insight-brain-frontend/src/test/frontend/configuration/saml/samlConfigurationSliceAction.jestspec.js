/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { omit } from 'ramda';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import { actions } from 'MainRoot/configuration/saml/samlConfigurationSlice';
import { getSamlConfigurationUrl } from 'MainRoot/util/CLMLocation';
import { uriTemplate } from 'MainRoot/util/urlUtil';
import * as authorizationUtil from 'MainRoot/util/authorizationUtil';

import 'TestRoot/SpecUtil';

const {
  loadSAMLConfiguration,
  updateSAMLConfiguration,
  deleteSAMLConfiguration,
  onSAMLConfigurationValueChange,
  onSAMLConfigurationSelectValueChange,
  onRestoreConfigurationValues,
  onRestoreConfigurationValue,
} = actions;

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

const removeExtraDataFromActions = (actions) => actions.map((action) => omit(['meta', 'error', 'payload'], action));

describe('samlConfigurationActions', function () {
  let store, state, mockAxiosCalls;

  beforeEach(() => {
    state = {
      samlConfiguration: {
        isLoading: false,
        submitState: null,
        submitMaskError: null,
        loadError: null,
        isConfigured: false,
        configurationValues: {
          identityProviderName: initUserInput('identity provider'),
          entityId: initUserInput(uriTemplate`/api/v2/config/saml/metadata`),
          usernameAttributeName: initUserInput('username'),
          firstNameAttributeName: initUserInput('firstName'),
          lastNameAttributeName: initUserInput('lastName'),
          emailAttributeName: initUserInput('email'),
          groupsAttributeName: initUserInput('groups'),
          identityProviderMetadataXml: initUserInput('testxml'),
          validateResponseSignature: 'null',
          validateAssertionSignature: 'null',
        },
        loadedConfigurationValues: null,
      },
    };
    store = SpecUtil.mockReduxStore(state);
    mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  });

  describe('when not authorized', () => {
    it('dispatches LOAD_SAML_CONFIGURATION_REQUESTED and LOAD_SAML_CONFIGURATION_FAILED after a failed response', (done) => {
      jest.spyOn(authorizationUtil, 'checkPermissions').mockImplementation(() => Promise.reject('Error'));

      store.dispatch(loadSAMLConfiguration()).then(() => {
        const actions = removeExtraDataFromActions(store.getActions());
        expect(actions.length).toBe(2);

        expect(actions).toHaveActionsInOrder([
          { type: LOAD_SAML_CONFIGURATION_REQUESTED },
          { type: LOAD_SAML_CONFIGURATION_FAILED },
        ]);
        done();
      });
    });
  });

  describe('when authorized', () => {
    beforeEach(() => {
      jest.spyOn(authorizationUtil, 'checkPermissions').mockReturnValue(Promise.resolve());
    });

    describe('loadSAMLConfiguration', () => {
      it('dispatches LOAD_SAML_CONFIGURATION_REQUESTED and LOAD_SAML_CONFIGURATION_FULFILLED after a successful response', (done) => {
        const mockResponse = {
          data: {
            isLoading: false,
            submitState: null,
            submitMaskError: null,
            loadError: null,
            isConfigured: false,
            configurationValues: {
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
            loadedConfigurationValues: null,
          },
        };
        mockAxiosCalls({
          get: {
            [getSamlConfigurationUrl()]: Promise.resolve(mockResponse),
          },
        });

        store.dispatch(loadSAMLConfiguration()).then(() => {
          const actions = removeExtraDataFromActions(store.getActions());
          expect(actions.length).toBe(2);
          expect(actions).toHaveActionsInOrder([
            { type: LOAD_SAML_CONFIGURATION_REQUESTED },
            { type: LOAD_SAML_CONFIGURATION_FULFILLED },
          ]);
          done();
        });
      });

      it('dispatches LOAD_SAML_CONFIGURATION_REQUESTED and LOAD_SAML_CONFIGURATION_FAILED after a failed reponse', (done) => {
        mockAxiosCalls({
          get: {
            [getSamlConfigurationUrl()]: () => Promise.reject('error'),
          },
        });

        store.dispatch(loadSAMLConfiguration()).then(() => {
          const actions = removeExtraDataFromActions(store.getActions());
          expect(actions.length).toBe(2);
          expect(actions).toHaveActionsInOrder([
            { type: LOAD_SAML_CONFIGURATION_REQUESTED },
            { type: LOAD_SAML_CONFIGURATION_FAILED },
          ]);
          done();
        });
      });
    });

    describe('updateSAMLConfiguration', () => {
      it('dispatches UPDATE_SAML_CONFIGURATION_REQUESTED and UPDATE_SAML_CONFIGURATION_FULFILLED after a successful response', (done) => {
        mockAxiosCalls({
          put: {
            [getSamlConfigurationUrl()]: Promise.resolve({ data: {} }),
          },
        });

        store.dispatch(updateSAMLConfiguration()).then(() => {
          const actions = removeExtraDataFromActions(store.getActions());
          expect(actions.length).toBe(2);
          expect(actions).toHaveActionsInOrder([
            { type: UPDATE_SAML_CONFIGURATION_REQUESTED },
            { type: UPDATE_SAML_CONFIGURATION_FULFILLED },
          ]);
          done();
        });
      });

      it('dispatches UPDATE_SAML_CONFIGURATION_REQUESTED and UPDATE_SAML_CONFIGURATION_FAILED after a failed reponse', (done) => {
        mockAxiosCalls({
          put: {
            [getSamlConfigurationUrl()]: () => Promise.reject('error'),
          },
        });

        store.dispatch(updateSAMLConfiguration()).then(() => {
          const actions = removeExtraDataFromActions(store.getActions());
          expect(actions.length).toBe(2);
          expect(actions).toHaveActionsInOrder([
            { type: UPDATE_SAML_CONFIGURATION_REQUESTED },
            { type: UPDATE_SAML_CONFIGURATION_FAILED },
          ]);
          done();
        });
      });
    });

    describe('deleteSAMLConfiguration', () => {
      it('dispatches DELETE_SAML_CONFIGURATION_REQUESTED and DELETE_SAML_CONFIGURATION_FULFILLED after a successful response', (done) => {
        mockAxiosCalls({
          del: {
            [getSamlConfigurationUrl()]: Promise.resolve({}),
          },
        });

        store.dispatch(deleteSAMLConfiguration()).then(() => {
          const actions = removeExtraDataFromActions(store.getActions());
          expect(actions.length).toBe(2);
          expect(actions).toHaveActionsInOrder([
            { type: DELETE_SAML_CONFIGURATION_REQUESTED },
            { type: DELETE_SAML_CONFIGURATION_FULFILLED },
          ]);
          done();
        });
      });

      it('dispatches DELETE_SAML_CONFIGURATION_REQUESTED and DELETE_SAML_CONFIGURATION_FAILED after a failed reponse', (done) => {
        mockAxiosCalls({
          del: {
            [getSamlConfigurationUrl()]: () => Promise.reject('error'),
          },
        });

        store.dispatch(deleteSAMLConfiguration()).then(() => {
          const actions = removeExtraDataFromActions(store.getActions());
          expect(actions.length).toBe(2);
          expect(actions).toHaveActionsInOrder([
            { type: DELETE_SAML_CONFIGURATION_REQUESTED },
            { type: DELETE_SAML_CONFIGURATION_FAILED },
          ]);
          done();
        });
      });
    });

    describe('onSAMLConfigurationValueChange', () => {
      it('dispatches SET_CONFIGURATION_VALUES action', () => {
        store.dispatch(onSAMLConfigurationValueChange('identityProviderName', 'test'));
        const actions = store.getActions();

        expect(actions.length).toBe(1);
        expect(actions).toHaveActionType(SET_CONFIGURATION_VALUES);
      });
    });

    describe('onSAMLConfigurationSelectValueChange', () => {
      it('dispatches SET_SELECT_CONFIGURATION_VALUES action', () => {
        store.dispatch(onSAMLConfigurationSelectValueChange('validateResponseSignature', 'true'));
        const actions = store.getActions();

        expect(actions.length).toBe(1);
        expect(actions).toHaveActionType(SET_SELECT_CONFIGURATION_VALUES);
      });
    });

    describe('onRestoreConfigurationValues', () => {
      it('dispatches RESTORE_DEFAULT_CONFIGURATION_VALUES action', () => {
        store.dispatch(onRestoreConfigurationValues());
        const actions = store.getActions();

        expect(actions.length).toBe(1);
        expect(actions).toHaveActionType(RESTORE_DEFAULT_CONFIGURATION_VALUES);
      });
    });

    describe('onRestoreConfigurationValue', () => {
      it('dispatches SET_CONFIGURATION_VALUES action', () => {
        store.dispatch(onRestoreConfigurationValue('identityProviderName'));
        const actions = store.getActions();

        expect(actions.length).toBe(1);
        expect(actions).toHaveActionType(SET_CONFIGURATION_VALUES);
      });
    });
  });
});
