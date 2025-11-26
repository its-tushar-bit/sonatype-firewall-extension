/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import userTokensConfigurationReducer, {
  actions,
  initialState,
} from 'MainRoot/configuration/userTokensConfiguration/userTokensConfigurationSlice';

describe('userTokensConfigurationSlice', () => {
  it('should return the initial state', () => {
    expect(userTokensConfigurationReducer(undefined, { type: 'unknown' })).toEqual(initialState);
  });

  describe('toggleExpirationEnabled', () => {
    it('should toggle expiration enabled from false to true', () => {
      const actual = userTokensConfigurationReducer(initialState, actions.toggleExpirationEnabled());
      expect(actual.formState.expirationEnabled).toBe(true);
      expect(actual.isDirty).toBe(true);
    });

    it('should toggle expiration enabled from true to false', () => {
      const state = {
        ...initialState,
        formState: {
          ...initialState.formState,
          expirationEnabled: true,
        },
        serverData: {
          userTokenDefaultExpirationDays: 30,
        },
      };
      const actual = userTokensConfigurationReducer(state, actions.toggleExpirationEnabled());
      expect(actual.formState.expirationEnabled).toBe(false);
      expect(actual.isDirty).toBe(true);
    });
  });

  describe('setExpirationDays', () => {
    it('should update expiration days value', () => {
      const state = {
        ...initialState,
        formState: {
          ...initialState.formState,
          expirationEnabled: true,
        },
      };
      const actual = userTokensConfigurationReducer(state, actions.setExpirationDays('60'));
      expect(actual.formState.expirationDays.value).toBe('60');
      expect(actual.isDirty).toBe(true);
    });

    it('should validate expiration days - empty value', () => {
      const actual = userTokensConfigurationReducer(initialState, actions.setExpirationDays(''));
      expect(actual.formState.expirationDays.validationErrors).toBe('Must be non-empty.');
    });

    it('should validate expiration days - non-numeric value', () => {
      const actual = userTokensConfigurationReducer(initialState, actions.setExpirationDays('abc'));
      expect(actual.formState.expirationDays.validationErrors).toBe('Must be a valid integer.');
    });

    it('should validate expiration days - below minimum', () => {
      const actual = userTokensConfigurationReducer(initialState, actions.setExpirationDays('0'));
      expect(actual.formState.expirationDays.validationErrors).toBe('Must be at least 1 day.');
    });

    it('should validate expiration days - above maximum', () => {
      const actual = userTokensConfigurationReducer(initialState, actions.setExpirationDays('500'));
      expect(actual.formState.expirationDays.validationErrors).toBe('Must be at most 365 days.');
    });

    it('should validate expiration days - valid value', () => {
      const actual = userTokensConfigurationReducer(initialState, actions.setExpirationDays('100'));
      expect(actual.formState.expirationDays.validationErrors).toBeNull();
    });
  });

  describe('resetForm', () => {
    it('should reset form to server data', () => {
      const state = {
        ...initialState,
        formState: {
          userTokensEnabled: true,
          expirationEnabled: true,
          expirationDays: { value: '90', isPristine: false, validationErrors: null },
        },
        serverData: {
          userTokenDefaultExpirationDays: 30,
        },
        isDirty: true,
      };
      const actual = userTokensConfigurationReducer(state, actions.resetForm());
      expect(actual.formState.expirationEnabled).toBe(true);
      expect(actual.formState.expirationDays.value).toBe('30');
      expect(actual.isDirty).toBe(false);
    });
  });

  describe('load', () => {
    it('should handle load pending', () => {
      const actual = userTokensConfigurationReducer(initialState, actions.load.pending());
      expect(actual.loading).toBe(true);
      expect(actual.loadError).toBeNull();
    });

    it('should handle load fulfilled', () => {
      const payload = {
        userTokenDefaultExpirationDays: 60,
      };
      const actual = userTokensConfigurationReducer(initialState, actions.load.fulfilled(payload));
      expect(actual.loading).toBe(false);
      expect(actual.serverData).toEqual(payload);
      expect(actual.formState.expirationEnabled).toBe(true);
      expect(actual.formState.expirationDays.value).toBe('60');
    });

    it('should handle load rejected', () => {
      const error = { response: { status: 500 }, message: 'Load failed' };
      const action = {
        type: actions.load.rejected.type,
        payload: error,
        error: { message: 'Rejected' },
      };
      const actual = userTokensConfigurationReducer(initialState, action);
      expect(actual.loading).toBe(false);
      expect(actual.loadError).toBeTruthy();
    });

    it('should handle load rejected with 404', () => {
      const error = { response: { status: 404 }, message: 'Not found' };
      const action = {
        type: actions.load.rejected.type,
        payload: error,
        error: { message: 'Rejected' },
      };
      const actual = userTokensConfigurationReducer(initialState, action);
      expect(actual.loading).toBe(false);
      expect(actual.loadError).toBeNull();
    });
  });

  describe('update', () => {
    it('should handle update pending', () => {
      const actual = userTokensConfigurationReducer(initialState, actions.update.pending());
      expect(actual.submitMaskState).toBe(false);
      expect(actual.updateError).toBeNull();
    });

    it('should handle update fulfilled', () => {
      const state = {
        ...initialState,
        formState: {
          userTokensEnabled: true,
          expirationEnabled: true,
          expirationDays: { value: '90', isPristine: false, validationErrors: null },
        },
        isDirty: true,
      };
      const actual = userTokensConfigurationReducer(state, actions.update.fulfilled());
      expect(actual.submitMaskState).toBe(true);
      expect(actual.isDirty).toBe(false);
      expect(actual.serverData.userTokenDefaultExpirationDays).toBe(90);
    });

    it('should handle update fulfilled with expiration disabled', () => {
      const state = {
        ...initialState,
        formState: {
          userTokensEnabled: true,
          expirationEnabled: false,
          expirationDays: { value: '90', isPristine: false, validationErrors: null },
        },
        isDirty: true,
      };
      const actual = userTokensConfigurationReducer(state, actions.update.fulfilled());
      expect(actual.submitMaskState).toBe(true);
      expect(actual.isDirty).toBe(false);
      expect(actual.serverData.userTokenDefaultExpirationDays).toBeNull();
    });

    it('should handle update rejected', () => {
      const error = { response: { status: 500 }, message: 'Update failed' };
      const action = {
        type: actions.update.rejected.type,
        payload: error,
        error: { message: 'Rejected' },
      };
      const actual = userTokensConfigurationReducer(initialState, action);
      expect(actual.submitMaskState).toBeNull();
      expect(actual.updateError).toBeTruthy();
    });
  });

  describe('submitMaskTimerDone', () => {
    it('should clear submit mask state', () => {
      const state = {
        ...initialState,
        submitMaskState: true,
      };
      const actual = userTokensConfigurationReducer(state, actions.submitMaskTimerDone());
      expect(actual.submitMaskState).toBeNull();
    });
  });
});
