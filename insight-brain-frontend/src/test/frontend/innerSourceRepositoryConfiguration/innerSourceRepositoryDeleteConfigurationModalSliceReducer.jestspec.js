/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reducer from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryDeleteConfigurationModalSlice';

describe('innerSourceRepositoryDeleteConfigurationModalSliceReducer', () => {
  describe('innerSourceRepositoryDeleteConfigurationModal/deleteConfiguration/pending action', () => {
    it('sets `deleteSubmitMaskState` to false, `deleteSubmitMaskMessage`, and `deleteConfigurationError` to null', () => {
      const state = {};

      const newState = reducer(state, {
        type: 'innerSourceRepositoryDeleteConfigurationModal/deleteConfiguration/pending',
      });

      expect(newState.deleteSubmitMaskState).toBeFalsy();
      expect(newState.deleteConfigurationError).toBeNull();
    });
  });

  describe('innerSourceRepositoryDeleteConfigurationModal/deleteConfiguration/fulfilled action', () => {
    it('sets the deleteSubmitMaskState to true', () => {
      const state = {};

      const newState = reducer(state, {
        type: 'innerSourceRepositoryDeleteConfigurationModal/deleteConfiguration/fulfilled',
      });

      expect(newState).toEqual({
        deleteSubmitMaskState: true,
      });
    });
  });

  describe('innerSourceRepositoryDeleteConfigurationModal/deleteConfiguration/rejected action', () => {
    it('sets `deleteSubmitMaskState` to null and `deleteConfigurationError` to the payload http error message', () => {
      const state = {};

      const newState = reducer(state, {
        type: 'innerSourceRepositoryDeleteConfigurationModal/deleteConfiguration/rejected',
        payload: 'someError',
      });

      expect(newState.deleteSubmitMaskState).toBeNull();
      expect(newState.deleteConfigurationError).toBe('someError');
    });
  });

  describe('innerSourceRepositoryDeleteConfigurationModal/openModal', () => {
    it('sets `openModal` to the true and `repositoryConectionId`', () => {
      const state = {};

      const newState = reducer(state, {
        type: 'innerSourceRepositoryDeleteConfigurationModal/openModal',
        payload: 'someRepositoryConnectionId',
      });

      expect(newState.showModal).toBeTruthy();
      expect(newState.repositoryConnectionId).toBe('someRepositoryConnectionId');
    });
  });
});
