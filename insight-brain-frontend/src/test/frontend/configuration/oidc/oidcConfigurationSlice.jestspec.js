/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, { actions, initialState } from 'MainRoot/configuration/oidc/oidcConfigurationSlice';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { getOidcConfigurationUrl } from 'MainRoot/util/CLMLocation';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import { SELECT_COMPONENT } from 'MainRoot/applicationReport/applicationReportActions';
import * as authorizationUtil from 'MainRoot/util/authorizationUtil';

const { initialState: initUserInput } = nxTextInputStateHelpers;

describe('oidcConfigurationSlice', () => {
  let axiosMock;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    axiosMock.reset();
    jest.spyOn(authorizationUtil, 'checkPermissions').mockResolvedValue();
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  describe('Initial State', () => {
    it('should have correct initial state', () => {
      expect(initialState).toEqual({
        isLoading: false,
        submitState: null,
        submitMaskError: null,
        loadError: null,
        isConfigured: false,
        isDeleteModalShown: false,
        isDirty: false,
        configurationValues: {
          oauth2IdpJwksUrl: initUserInput(''),
          oauth2IdpJwsAlgorithm: initUserInput(''),
          oauth2IdpJwks: initUserInput(''),
          oauth2UsernameClaim: initUserInput(''),
          oauth2FirstNameClaim: initUserInput(''),
          oauth2LastNameClaim: initUserInput(''),
          oauth2EmailClaim: initUserInput(''),
          oauth2GroupsClaim: initUserInput(''),
          oauth2ExactMatchClaimsJson: initUserInput(''),
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
    });

    it('should return initial state when undefined state is passed', () => {
      expect(reducer(undefined, { type: 'unknown' })).toEqual(initialState);
    });
  });

  describe('Reducers', () => {
    describe('setConfigurationValues', () => {
      it('should update a configuration value', () => {
        const state = { ...initialState };
        const action = actions.setConfigurationValues({ name: 'oidcClientId', value: 'test-client-id' });

        const newState = reducer(state, action);

        expect(newState.configurationValues.oidcClientId.value).toBe('test-client-id');
      });

      it('should mark state as dirty when value changes', () => {
        const state = { ...initialState };
        const action = actions.setConfigurationValues({ name: 'oidcClientId', value: 'test-client-id' });

        const newState = reducer(state, action);

        expect(newState.isDirty).toBe(true);
      });

      it('should not mark state as dirty when value matches loaded value', () => {
        const loadedValues = {
          ...initialState.configurationValues,
          oidcClientId: initUserInput('test-client-id'),
        };
        const state = {
          ...initialState,
          loadedConfigurationValues: loadedValues,
          configurationValues: loadedValues,
        };
        const action = actions.setConfigurationValues({ name: 'oidcClientId', value: 'test-client-id' });

        const newState = reducer(state, action);

        expect(newState.isDirty).toBe(false);
      });

      // Test removed - backend handles validation

      it('should mark field as not pristine after change', () => {
        const state = { ...initialState };
        const action = actions.setConfigurationValues({ name: 'oidcClientId', value: 'test' });

        const newState = reducer(state, action);

        expect(newState.configurationValues.oidcClientId.isPristine).toBe(false);
      });
    });

    describe('restoreDefaultConfigurationValues', () => {
      it('should restore to loaded values', () => {
        const loadedValues = {
          ...initialState.configurationValues,
          oidcClientId: initUserInput('original-id'),
        };
        const state = {
          ...initialState,
          loadedConfigurationValues: loadedValues,
          configurationValues: {
            ...initialState.configurationValues,
            oidcClientId: initUserInput('modified-id'),
          },
          isDirty: true,
        };

        const newState = reducer(state, actions.restoreDefaultConfigurationValues());

        expect(newState.configurationValues.oidcClientId.value).toBe('original-id');
        expect(newState.isDirty).toBe(false);
      });

      it('should restore to initial values when no loaded values exist', () => {
        const state = {
          ...initialState,
          configurationValues: {
            ...initialState.configurationValues,
            oidcClientId: initUserInput('modified-id'),
          },
          isDirty: true,
        };

        const newState = reducer(state, actions.restoreDefaultConfigurationValues());

        expect(newState.configurationValues.oidcClientId.value).toBe('');
        expect(newState.isDirty).toBe(false);
      });
    });

    describe('maskTimerDone', () => {
      it('should reset submitState to null', () => {
        const state = { ...initialState, submitState: true };

        const newState = reducer(state, actions.maskTimerDone());

        expect(newState.submitState).toBeNull();
      });
    });

    describe('toggleDeleteModal', () => {
      it('should toggle isDeleteModalShown from false to true', () => {
        const state = { ...initialState, isDeleteModalShown: false };

        const newState = reducer(state, actions.toggleDeleteModal());

        expect(newState.isDeleteModalShown).toBe(true);
      });

      it('should toggle isDeleteModalShown from true to false', () => {
        const state = { ...initialState, isDeleteModalShown: true };

        const newState = reducer(state, actions.toggleDeleteModal());

        expect(newState.isDeleteModalShown).toBe(false);
      });
    });
  });

  describe('Async Thunks', () => {
    describe('loadOidcConfiguration', () => {
      it('should set isLoading to true when pending', () => {
        const state = { ...initialState };

        const newState = reducer(state, {
          type: actions.loadOidcConfiguration.pending.type,
        });

        expect(newState.isLoading).toBe(true);
      });

      it('should load configuration successfully', async () => {
        const mockData = {
          oauth2Configuration: {
            idpIssuer: 'https://auth.example.com',
            idpJwksUrl: 'https://auth.example.com/.well-known/jwks.json',
            idpJwsAlgorithm: 'RS256',
            usernameClaim: 'email',
          },
          oidcConfiguration: {
            idpIssuer: 'https://auth.example.com',
            clientId: 'test-client-id',
            clientSecret: 'test-secret',
            idpAuthorizationUrl: 'https://auth.example.com/authorize',
            idpTokenUrl: 'https://auth.example.com/token',
          },
        };

        axiosMock.onGet(getOidcConfigurationUrl()).reply(200, mockData);

        const dispatch = jest.fn();
        const getState = () => ({});
        const thunk = actions.loadOidcConfiguration();

        await thunk(dispatch, getState, undefined);

        const fulfilledAction = dispatch.mock.calls.find(
          (call) => call[0].type === actions.loadOidcConfiguration.fulfilled.type
        );

        expect(fulfilledAction).toBeTruthy();
      });

      it('should set configuration values when fulfilled', () => {
        const mockData = {
          oauth2Configuration: {
            idpIssuer: 'https://auth.example.com',
            idpJwksUrl: 'https://auth.example.com/.well-known/jwks.json',
            idpJwsAlgorithm: 'RS256',
          },
          oidcConfiguration: {
            idpIssuer: 'https://auth.example.com',
            clientId: 'test-client-id',
            clientSecret: 'test-secret',
            idpAuthorizationUrl: 'https://auth.example.com/authorize',
            idpTokenUrl: 'https://auth.example.com/token',
          },
        };

        const state = { ...initialState };
        const newState = reducer(state, {
          type: actions.loadOidcConfiguration.fulfilled.type,
          payload: { data: mockData },
        });

        expect(newState.isLoading).toBe(false);
        expect(newState.isConfigured).toBe(true);
        expect(newState.loadError).toBeNull();
        expect(newState.configurationValues.oidcClientId.value).toBe('test-client-id');
        expect(newState.configurationValues.oidcIdpIssuer.value).toBe('https://auth.example.com');
      });

      it('should handle 404 error gracefully', () => {
        const state = { ...initialState };
        const newState = reducer(state, {
          type: actions.loadOidcConfiguration.rejected.type,
          payload: { response: { status: 404 } },
        });

        expect(newState.isLoading).toBe(false);
        expect(newState.isConfigured).toBe(false);
        expect(newState.loadError).toBeNull();
      });

      it('should handle other errors', () => {
        const state = { ...initialState };
        const newState = reducer(state, {
          type: actions.loadOidcConfiguration.rejected.type,
          payload: { response: { status: 500, data: { message: 'Server Error' } } },
        });

        expect(newState.isLoading).toBe(false);
        expect(newState.isConfigured).toBe(false);
        expect(newState.loadError).toBeTruthy();
      });
    });

    describe('updateOidcConfiguration', () => {
      it('should set submitState to false when pending', () => {
        const state = { ...initialState };

        const newState = reducer(state, {
          type: actions.updateOidcConfiguration.pending.type,
        });

        expect(newState.submitState).toBe(false);
      });

      it('should update configuration successfully', async () => {
        axiosMock.onPut(getOidcConfigurationUrl()).reply(204);

        const state = {
          ...initialState,
          configurationValues: {
            ...initialState.configurationValues,
            oauth2IdpIssuer: initUserInput('https://auth.example.com'),
            oidcClientId: initUserInput('test-client-id'),
            oidcClientSecret: initUserInput('test-secret'),
          },
        };

        const dispatch = jest.fn();
        const getState = () => ({ oidcConfiguration: state });
        const thunk = actions.updateOidcConfiguration();

        await thunk(dispatch, getState, undefined);

        const fulfilledAction = dispatch.mock.calls.find(
          (call) => call[0].type === actions.updateOidcConfiguration.fulfilled.type
        );

        expect(fulfilledAction).toBeTruthy();
      });

      it('should mark as configured and not dirty when fulfilled', () => {
        const state = {
          ...initialState,
          configurationValues: {
            ...initialState.configurationValues,
            oidcClientId: initUserInput('test-client-id'),
          },
          isDirty: true,
        };

        const newState = reducer(state, {
          type: actions.updateOidcConfiguration.fulfilled.type,
          payload: {},
        });

        expect(newState.submitState).toBe(true);
        expect(newState.isConfigured).toBe(true);
        expect(newState.isDirty).toBe(false);
        expect(newState.submitMaskError).toBeNull();
      });

      it('should handle update failure', () => {
        const state = { ...initialState };
        const newState = reducer(state, {
          type: actions.updateOidcConfiguration.rejected.type,
          payload: { response: { status: 400, data: { message: 'Bad Request' } } },
        });

        expect(newState.submitState).toBeNull();
        expect(newState.submitMaskError).toBeTruthy();
      });
    });

    describe('deleteOidcConfiguration', () => {
      it('should close delete modal and set submitState when pending', () => {
        const state = { ...initialState, isDeleteModalShown: true };

        const newState = reducer(state, {
          type: actions.deleteOidcConfiguration.pending.type,
        });

        expect(newState.submitState).toBe(false);
        expect(newState.isDeleteModalShown).toBe(false);
      });

      it('should delete configuration successfully', async () => {
        axiosMock.onDelete(getOidcConfigurationUrl()).reply(204);

        const dispatch = jest.fn();
        const getState = () => ({});
        const thunk = actions.deleteOidcConfiguration();

        await thunk(dispatch, getState, undefined);

        const fulfilledAction = dispatch.mock.calls.find(
          (call) => call[0].type === actions.deleteOidcConfiguration.fulfilled.type
        );

        expect(fulfilledAction).toBeTruthy();
      });

      it('should reset configuration when fulfilled', () => {
        const state = {
          ...initialState,
          isConfigured: true,
          configurationValues: {
            ...initialState.configurationValues,
            oidcClientId: initUserInput('test-client-id'),
          },
          loadedConfigurationValues: {
            ...initialState.configurationValues,
            oidcClientId: initUserInput('test-client-id'),
          },
        };

        const newState = reducer(state, {
          type: actions.deleteOidcConfiguration.fulfilled.type,
          payload: {},
        });

        expect(newState.submitState).toBe(true);
        expect(newState.isConfigured).toBe(false);
        expect(newState.isDirty).toBe(false);
        expect(newState.submitMaskError).toBeNull();
        expect(newState.configurationValues.oidcClientId.value).toBe('');
        expect(newState.loadedConfigurationValues).toBeNull();
      });

      it('should handle delete failure', () => {
        const state = { ...initialState };
        const newState = reducer(state, {
          type: actions.deleteOidcConfiguration.rejected.type,
          payload: { response: { status: 404, data: { message: 'Not Found' } } },
        });

        expect(newState.submitState).toBeNull();
        expect(newState.submitMaskError).toBeTruthy();
      });
    });
  });

  describe('Action Creators', () => {
    describe('onOidcConfigurationValueChange', () => {
      it('should dispatch setConfigurationValues action', () => {
        const dispatch = jest.fn();
        const thunk = actions.onOidcConfigurationValueChange('test-value', 'oidcClientId');

        thunk(dispatch);

        expect(dispatch).toHaveBeenCalledWith(
          expect.objectContaining({
            type: actions.setConfigurationValues.type,
            payload: { value: 'test-value', name: 'oidcClientId' },
          })
        );
      });
    });

    describe('onRestoreConfigurationValue', () => {
      it('should restore a single field to initial value', () => {
        const dispatch = jest.fn();
        const thunk = actions.onRestoreConfigurationValue('oidcClientId');

        thunk(dispatch);

        expect(dispatch).toHaveBeenCalledWith(
          expect.objectContaining({
            type: actions.setConfigurationValues.type,
            payload: { value: '', name: 'oidcClientId' },
          })
        );
      });
    });

    describe('onRestoreConfigurationValues', () => {
      it('should dispatch restoreDefaultConfigurationValues action', () => {
        const dispatch = jest.fn();
        const thunk = actions.onRestoreConfigurationValues();

        thunk(dispatch);

        expect(dispatch).toHaveBeenCalledWith(
          expect.objectContaining({
            type: actions.restoreDefaultConfigurationValues.type,
          })
        );
      });
    });
  });

  describe('SELECT_COMPONENT action', () => {
    it('should reset to initial state when SELECT_COMPONENT is dispatched', () => {
      const modifiedState = {
        ...initialState,
        isConfigured: true,
        isDirty: true,
        configurationValues: {
          ...initialState.configurationValues,
          oidcClientId: initUserInput('modified'),
        },
      };

      const newState = reducer(modifiedState, { type: SELECT_COMPONENT });

      expect(newState).toEqual(initialState);
    });
  });

  describe('isDirty computation', () => {
    it('should compute isDirty correctly when values change', () => {
      const loadedValues = {
        ...initialState.configurationValues,
        oidcClientId: initUserInput('original'),
      };
      const state = {
        ...initialState,
        loadedConfigurationValues: loadedValues,
        configurationValues: loadedValues,
        isDirty: false,
      };

      const newState = reducer(state, actions.setConfigurationValues({ name: 'oidcClientId', value: 'modified' }));

      expect(newState.isDirty).toBe(true);
    });

    it('should compute isDirty as false when values match loaded values', () => {
      const loadedValues = {
        ...initialState.configurationValues,
        oidcClientId: initUserInput('test'),
      };
      const state = {
        ...initialState,
        loadedConfigurationValues: loadedValues,
        configurationValues: {
          ...initialState.configurationValues,
          oidcClientId: initUserInput('different'),
        },
        isDirty: true,
      };

      const newState = reducer(state, actions.setConfigurationValues({ name: 'oidcClientId', value: 'test' }));

      expect(newState.isDirty).toBe(false);
    });
  });
});
