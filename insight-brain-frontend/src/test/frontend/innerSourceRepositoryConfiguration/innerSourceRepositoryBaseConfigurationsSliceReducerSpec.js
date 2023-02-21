/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, {
  initialState,
} from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryBaseConfigurationsSlice';
import { getInitialState } from 'TestRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryBaseConfigurationsTestData';
import { SUBMIT_MASK_SAVING_CONFIGURATION_MESSAGE } from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryConfigurationModalSlice';

describe('innerSourceRepositoryBaseConfigurationsSliceReducer', () => {
  describe('initial state', () => {
    it('returns the initial state given an undefined state', function () {
      const state = undefined;

      const newState = reducer(state, {});

      expect(newState).toEqual(getInitialState());
    });
  });

  describe('unknown action', () => {
    it('returns original state', function () {
      const state = Object.freeze({ foo: 'bar' });
      const action = {
        type: 'UNKNOWN',
      };

      const newState = reducer(state, action);

      expect(newState).toBe(state);
    });
  });

  describe('innerSourceRepositoryBaseConfigurations/setEnabled action', () => {
    it('sets `enabled` to the payload and sets isDirty to true', function () {
      const state = {};
      const action = {
        type: 'innerSourceRepositoryBaseConfigurations/setEnabled',
        payload: true,
      };

      const newState = reducer(state, action);

      expect(newState).toEqual({
        formState: {
          enabled: true,
        },
        isDirty: true,
      });
    });
  });

  describe('innerSourceRepositoryBaseConfigurations/setAllowOverride action and sets isDirty to true', () => {
    it('sets `allowOverride` to the payload', function () {
      const state = {};
      const action = {
        type: 'innerSourceRepositoryBaseConfigurations/setAllowOverride',
        payload: true,
      };

      const newState = reducer(state, action);

      expect(newState).toEqual({
        formState: {
          allowOverride: true,
        },
        isDirty: true,
      });
    });
  });

  describe('innerSourceRepositoryBaseConfigurations/cancel action and sets isDirty to false', () => {
    it('resets the form to match the initial data if the server data does not exist', function () {
      const state = {};
      const action = {
        type: 'innerSourceRepositoryBaseConfigurations/cancel',
      };

      const newState = reducer(state, action);

      expect(newState).toEqual({ formState: initialState.formState, isDirty: false });
    });

    it('resets the form to match the server data if it does exist and sets isDirty to false', function () {
      const state = {
        serverData: {
          repositoryConnectionStatus: {
            enabled: true,
            allowOverride: false,
          },
        },
      };
      const action = {
        type: 'innerSourceRepositoryBaseConfigurations/cancel',
      };

      const newState = reducer(state, action);

      expect(newState).toEqual({
        ...state,
        formState: {
          enabled: true,
          allowOverride: false,
        },
        isDirty: false,
      });
    });
  });

  describe('innerSourceRepositoryBaseConfigurations/submitMaskTimerDone action', () => {
    it('sets the `submitMaskState` to `null`', () => {
      const state = {
        submitMaskState: true,
      };

      const newState = reducer(state, {
        type: 'innerSourceRepositoryBaseConfigurations/submitMaskTimerDone',
      });

      expect(newState.submitMaskState).toBeNull();
    });
  });

  describe('innerSourceRepositoryBaseConfigurations/load/pending action', () => {
    it('sets the initial state with `loading` to true and `loadError` to null', () => {
      const state = {};

      const newState = reducer(state, {
        type: 'innerSourceRepositoryBaseConfigurations/load/pending',
      });

      expect(newState).toEqual({ loading: true, loadError: null });
    });
  });

  describe('innerSourceRepositoryBaseConfigurations/load/fulfilled action', () => {
    it('sets `loading` to false, `serverData` to the payload, and `formState` to represent the payload', () => {
      const state = {};
      const payload = {
        repositoryConnectionStatus: {
          enabled: true,
          allowOverride: false,
        },
      };

      const newState = reducer(state, {
        type: 'innerSourceRepositoryBaseConfigurations/load/fulfilled',
        payload,
      });

      expect(newState.loading).toBeFalsy();
      expect(newState.serverData).toEqual(payload);
      expect(newState.formState).toEqual({
        enabled: true,
        allowOverride: false,
      });
    });
  });

  describe('innerSourceRepositoryBaseConfigurations/load/rejected action', () => {
    it('sets `loading` to false and `loadError` to the payload http error message', () => {
      const state = {};

      const newState = reducer(state, {
        type: 'innerSourceRepositoryBaseConfigurations/load/rejected',
        payload: 'someError',
      });

      expect(newState.loading).toBeFalsy();
      expect(newState.loadError).toBe('someError');
    });
  });

  describe('innerSourceRepositoryBaseConfigurations/save/pending action', () => {
    it('sets `submitMaskState` to false, the `submitMaskMessage`, and `saveError` to null', () => {
      const state = {};

      const newState = reducer(state, {
        type: 'innerSourceRepositoryBaseConfigurations/save/pending',
      });

      expect(newState.submitMaskState).toBeFalsy();
      expect(newState.submitMaskMessage).toBe(SUBMIT_MASK_SAVING_CONFIGURATION_MESSAGE);
      expect(newState.saveError).toBeNull();
    });
  });

  describe('innerSourceRepositoryBaseConfigurations/save/fulfilled action', () => {
    it('sets `submitMaskState` to true', () => {
      const state = {
        formState: getInitialState().formState,
      };

      const newState = reducer(state, {
        type: 'innerSourceRepositoryBaseConfigurations/save/fulfilled',
      });

      expect(newState.submitMaskState).toBeTruthy();
    });
  });

  describe('innerSourceRepositoryBaseConfigurations/save/rejected action', () => {
    it('sets `submitMaskState` to null and `saveError` to the payload http error message', () => {
      const state = {};

      const newState = reducer(state, {
        type: 'innerSourceRepositoryBaseConfigurations/save/rejected',
        payload: 'someError',
      });

      expect(newState.submitMaskState).toBeNull();
      expect(newState.saveError).toBe('someError');
    });
  });
});
