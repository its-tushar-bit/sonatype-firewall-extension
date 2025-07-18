/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, {
  initialState,
} from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryBaseConfigurationsSlice';
import { getInitialState } from 'TestRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryBaseConfigurationsTestData';
import { SUBMIT_MASK_SAVING_CONFIGURATION_MESSAGE } from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryConfigurationModalSlice';

describe('artifactoryRepositoryBaseConfigurationsSliceReducer', () => {
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

  describe('artifactoryRepositoryBaseConfigurations/setEnabled action', () => {
    it('sets `enabled` to the payload and sets isDirty to true', function () {
      const state = {};
      const action = {
        type: 'artifactoryRepositoryBaseConfigurations/setEnabled',
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

  describe('artifactoryRepositoryBaseConfigurations/setAllowOverride action', () => {
    it('sets `allowOverride` to the payload and sets isDirty to true', function () {
      const state = {};
      const action = {
        type: 'artifactoryRepositoryBaseConfigurations/setAllowOverride',
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

  describe('artifactoryRepositoryBaseConfigurations/cancel action', () => {
    it('resets the form to match the initial data if the server data does not exist and sets isDirty to false', function () {
      const state = {};
      const action = {
        type: 'artifactoryRepositoryBaseConfigurations/cancel',
      };

      const newState = reducer(state, action);

      expect(newState).toEqual({ formState: initialState.formState, isDirty: false });
    });

    it('resets the form to match the server data if it does exist and sets isDirty to false', function () {
      const state = {
        serverData: {
          artifactoryConnectionStatus: {
            enabled: true,
            allowOverride: false,
          },
        },
      };
      const action = {
        type: 'artifactoryRepositoryBaseConfigurations/cancel',
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

  describe('artifactoryRepositoryBaseConfigurations/submitMaskTimerDone action', () => {
    it('sets the `submitMaskState` to `null`', () => {
      const state = {
        submitMaskState: true,
      };

      const newState = reducer(state, {
        type: 'artifactoryRepositoryBaseConfigurations/submitMaskTimerDone',
      });

      expect(newState.submitMaskState).toBeNull();
    });
  });

  describe('artifactoryRepositoryBaseConfigurations/load/pending action', () => {
    it('sets the initial state with `loading` to true and `loadError` to null', () => {
      const state = {};

      const newState = reducer(state, {
        type: 'artifactoryRepositoryBaseConfigurations/load/pending',
      });

      expect(newState).toEqual({ loading: true, loadError: null });
    });
  });

  describe('artifactoryRepositoryBaseConfigurations/load/fulfilled action', () => {
    it('sets `loading` to false, `serverData` to the payload, and `formState` to represent the payload', () => {
      const state = {};
      const payload = {
        artifactoryConnectionStatus: {
          enabled: true,
          allowOverride: false,
        },
      };

      const newState = reducer(state, {
        type: 'artifactoryRepositoryBaseConfigurations/load/fulfilled',
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

  describe('artifactoryRepositoryBaseConfigurations/load/rejected action', () => {
    it('sets `loading` to false and `loadError` to the payload http error message', () => {
      const state = {};

      const newState = reducer(state, {
        type: 'artifactoryRepositoryBaseConfigurations/load/rejected',
        payload: 'someError',
      });

      expect(newState.loading).toBeFalsy();
      expect(newState.loadError).toBe('someError');
    });
  });

  describe('artifactoryRepositoryBaseConfigurations/save/pending action', () => {
    it('sets `submitMaskState` to false, the `submitMaskMessage`, and `saveError` to null', () => {
      const state = {};

      const newState = reducer(state, {
        type: 'artifactoryRepositoryBaseConfigurations/save/pending',
      });

      expect(newState.submitMaskState).toBeFalsy();
      expect(newState.submitMaskMessage).toBe(SUBMIT_MASK_SAVING_CONFIGURATION_MESSAGE);
      expect(newState.saveError).toBeNull();
    });
  });

  describe('artifactoryRepositoryBaseConfigurations/save/fulfilled action', () => {
    it('sets `submitMaskState` to true', () => {
      const state = {
        formState: getInitialState().formState,
      };

      const newState = reducer(state, {
        type: 'artifactoryRepositoryBaseConfigurations/save/fulfilled',
      });

      expect(newState.submitMaskState).toBeTruthy();
    });
  });

  describe('artifactoryRepositoryBaseConfigurations/save/rejected action', () => {
    it('sets `submitMaskState` to null and `saveError` to the payload http error message', () => {
      const state = {};

      const newState = reducer(state, {
        type: 'artifactoryRepositoryBaseConfigurations/save/rejected',
        payload: 'someError',
      });

      expect(newState.submitMaskState).toBeNull();
      expect(newState.saveError).toBe('someError');
    });
  });
});
