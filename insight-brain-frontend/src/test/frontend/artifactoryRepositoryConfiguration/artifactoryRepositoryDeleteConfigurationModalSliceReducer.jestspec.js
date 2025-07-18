/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reducer from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryDeleteConfigurationModalSlice';

describe('artifactoryRepositoryDeleteConfigurationModalSliceReducer', () => {
  describe('artifactoryRepositoryDeleteConfigurationModal/deleteConfiguration/pending action', () => {
    it('sets `deleteSubmitMaskState` to false, `deleteSubmitMaskMessage`, and `deleteConfigurationError` to null', () => {
      const state = {};

      const newState = reducer(state, {
        type: 'artifactoryRepositoryDeleteConfigurationModal/deleteConfiguration/pending',
      });

      expect(newState.deleteSubmitMaskState).toBeFalsy();
      expect(newState.deleteConfigurationError).toBeNull();
    });
  });

  describe('artifactoryRepositoryDeleteConfigurationModal/deleteConfiguration/fulfilled action', () => {
    it('sets the deleteSubmitMaskState to true', () => {
      const state = {};

      const newState = reducer(state, {
        type: 'artifactoryRepositoryDeleteConfigurationModal/deleteConfiguration/fulfilled',
      });

      expect(newState).toEqual({
        deleteSubmitMaskState: true,
      });
    });
  });

  describe('artifactoryRepositoryDeleteConfigurationModal/deleteConfiguration/rejected action', () => {
    it('sets `deleteSubmitMaskState` to null and `deleteConfigurationError` to the payload http error message', () => {
      const state = {};

      const newState = reducer(state, {
        type: 'artifactoryRepositoryDeleteConfigurationModal/deleteConfiguration/rejected',
        payload: 'someError',
      });

      expect(newState.deleteSubmitMaskState).toBeNull();
      expect(newState.deleteConfigurationError).toBe('someError');
    });
  });

  describe('artifactoryRepositoryDeleteConfigurationModal/openModal', () => {
    it('sets `openModal` to the true and `repositoryConectionId`', () => {
      const state = {};

      const newState = reducer(state, {
        type: 'artifactoryRepositoryDeleteConfigurationModal/openModal',
        payload: 'someArtifactoryConnectionId',
      });

      expect(newState.showModal).toBeTruthy();
      expect(newState.artifactoryConnectionId).toBe('someArtifactoryConnectionId');
    });
  });
});
