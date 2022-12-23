/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/OrgsAndPolicies/repositories/repositoriesConfigurationSlice';

describe('repositoriesConfigurationSlice', () => {
  describe('unknown action', () => {
    it('returns original state', () => {
      const state = Object.freeze({ foo: 'bar' });
      const action = {
        type: 'UNKNOWN',
      };
      const newState = reducer(state, action);
      expect(newState).toBe(state);
    });
  });

  describe('repositories/loadRepositories/pending action', () => {
    it('sets the loading flag to true, unsets loading error and submitMask', () => {
      const state = Object.freeze({
        loading: false,
        loadError: 'Loading error',
        submitMaskState: true,
      });

      const { loading, loadError, submitMaskState } = reducer(state, {
        type: 'repositories/loadRepositories/pending',
      });

      expect(loading).toBe(true);
      expect(loadError).toBeNull();
      expect(submitMaskState).toBeNull();
    });
  });

  describe('repositories/loadRepositories/fulfilled action', () => {
    it('sets loading flag to false, unsets the error and fills in the repositories details', () => {
      const state = Object.freeze({
        repositories: [],
        loading: true,
        loadError: 'Loading error',
        deleteError: null,
        unsortedRepositories: [],
      });

      const payload = [
        {
          managerInstanceId: 'e71926b22d414648938ca1faba2efec9',
          repository: {
            id: 'fca29f962d9a47aea59d516cde1e1970',
            publicId: 'central',
            enabled: true,
          },
        },
        {
          managerInstanceId: 'e71926b22d414648938ca1faba2efec9',
          repository: {
            id: '623ed14b5c114f79bea3276b313a90e8',
            publicId: 'releases-proxy',
            enabled: true,
          },
        },
      ];

      const { repositories, loading, loadError, unsortedRepositories } = reducer(state, {
        type: 'repositories/loadRepositories/fulfilled',
        payload,
      });

      expect(repositories).toEqual(payload);
      expect(loading).toBe(false);
      expect(loadError).toBeNull();
      expect(unsortedRepositories).toEqual(payload);
    });
  });

  describe('repositories/loadRepositories/rejected action', () => {
    it('sets the error to the payload and the loading flag to false', () => {
      const state = Object.freeze({
        loading: true,
        loadError: null,
      });

      const { loading, loadError } = reducer(state, {
        type: 'repositories/loadRepositories/rejected',
        payload: 'Loading error',
      });

      expect(loading).toBe(false);
      expect(loadError).toBe('Loading error');
    });
  });

  describe('repositories/deleteRepository/pending action', () => {
    it('sets the loading flag to true, unsets the delete error and sets submitMask flag to false', () => {
      const state = Object.freeze({
        loading: false,
        deleteError: 'Delete error',
        submitMaskState: true,
      });

      const { loading, deleteError, submitMaskState } = reducer(state, {
        type: 'repositories/deleteRepository/pending',
      });

      expect(loading).toBe(true);
      expect(deleteError).toBeNull();
      expect(submitMaskState).toBe(false);
    });
  });

  describe('repositories/deleteRepository/fulfilled action', () => {
    it('sets the loading flag to false, unsets delete error and sets submitMaskState flag to true', () => {
      const state = Object.freeze({
        loading: true,
        deleteError: 'Delete error',
        submitMaskState: false,
      });

      const { loading, deleteError, submitMaskState } = reducer(state, {
        type: 'repositories/deleteRepository/fulfilled',
      });

      expect(loading).toBe(false);
      expect(deleteError).toBeNull();
      expect(submitMaskState).toBe(true);
    });
  });

  describe('repositories/deleteRepository/rejected action', () => {
    it('sets the deleteError to the payload and the loading flag to false, unsets submitMaskState', () => {
      const state = Object.freeze({
        loading: true,
        deleteError: null,
        submitMaskState: false,
      });

      const { loading, deleteError, submitMaskState } = reducer(state, {
        type: 'repositories/deleteRepository/rejected',
        payload: 'Deleting error',
      });

      expect(loading).toBe(false);
      expect(deleteError).toBe('Deleting error');
      expect(submitMaskState).toBeNull();
    });
  });

  describe('repositories/openDeleteModal action', () => {
    it('sets the deleteModalInfo to the payload, opens modal window and unsets deleteError', () => {
      const state = Object.freeze({
        showDeleteModal: false,
        deleteError: 'Deleting error',
        deleteModalInfo: {},
      });

      const payload = { publicId: '1afd5ds23', id: 1 };

      const { showDeleteModal, deleteError, deleteModalInfo } = reducer(state, {
        type: 'repositories/openDeleteModal',
        payload,
      });

      expect(showDeleteModal).toBe(true);
      expect(deleteError).toBeNull();
      expect(deleteModalInfo).toEqual(payload);
    });
  });

  describe('repositories/resetSubmitMaskState action', () => {
    it('resets the state for submitMask flag', () => {
      const state = Object.freeze({
        submitMaskState: true,
      });

      const { submitMaskState } = reducer(state, {
        type: 'repositories/resetSubmitMaskState',
      });

      expect(submitMaskState).toBeNull();
    });
  });

  describe('repositories/setSort action', () => {
    it('sorts repositories in ascending order by repository name field if they were unsorted', () => {
      const state = Object.freeze({
        repositories: [
          {
            managerInstanceId: 'managerInstanceId1',
            repository: {
              id: '1',
              publicId: 'repositoryNameB',
              enabled: true,
            },
          },
          {
            managerInstanceId: 'managerInstanceId2',
            repository: {
              id: '2',
              publicId: 'repositoryNameC',
              enabled: true,
            },
          },
          {
            managerInstanceId: 'managerInstanceId3',
            repository: {
              id: '3',
              publicId: 'repositoryNameA',
              enabled: true,
            },
          },
        ],
        sortConfiguration: null,
      });

      const sortedRepositoriesByPublicId = [
        {
          managerInstanceId: 'managerInstanceId3',
          repository: {
            id: '3',
            publicId: 'repositoryNameA',
            enabled: true,
          },
        },
        {
          managerInstanceId: 'managerInstanceId1',
          repository: {
            id: '1',
            publicId: 'repositoryNameB',
            enabled: true,
          },
        },
        {
          managerInstanceId: 'managerInstanceId2',
          repository: {
            id: '2',
            publicId: 'repositoryNameC',
            enabled: true,
          },
        },
      ];

      const { repositories } = reducer(state, {
        type: 'repositories/setSort',
        payload: 'publicId',
      });

      expect(repositories).toEqual(sortedRepositoriesByPublicId);
    });
  });

  describe('repositories/setShowDeleteModal action', () => {
    it('sets the state for the showDeleteModal flag', () => {
      const state = Object.freeze({
        showDeleteModal: false,
      });

      const { showDeleteModal } = reducer(state, {
        type: 'repositories/setShowDeleteModal',
        payload: true,
      });

      expect(showDeleteModal).toBe(true);
    });
  });
});
