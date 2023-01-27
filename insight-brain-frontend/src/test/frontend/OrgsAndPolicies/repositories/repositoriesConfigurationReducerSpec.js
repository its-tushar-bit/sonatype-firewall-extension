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
        sortConfiguration: [
          {
            key: 'publicId',
            dir: 'asc',
          },
          {
            key: 'managerInstanceId',
            dir: 'asc',
          },
          {
            key: 'enabled',
            dir: 'asc',
          },
        ],
      });

      const payload = [
        {
          managerInstanceId: '1',
          repository: {
            id: '1',
            publicId: 'b',
            enabled: true,
          },
        },
        {
          managerInstanceId: '2',
          repository: {
            id: '2',
            publicId: 'a',
            enabled: true,
          },
        },
      ];

      const { repositories, loading, loadError } = reducer(state, {
        type: 'repositories/loadRepositories/fulfilled',
        payload,
      });

      expect(repositories[0].repository.publicId).toBe('a');
      expect(repositories[0].managerInstanceId).toEqual('2');
      expect(repositories[0].repository.enabled).toBe(true);
      expect(repositories[1].repository.publicId).toBe('b');
      expect(repositories[1].managerInstanceId).toBe('1');
      expect(repositories[1].repository.enabled).toBe(true);
      expect(loading).toBe(false);
      expect(loadError).toBeNull();
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

  describe('repositories/sortRepositories action', () => {
    const initialSortConfiguration = [
      {
        key: 'publicId',
        dir: 'asc',
      },
      {
        key: 'managerInstanceId',
        dir: 'asc',
      },
      {
        key: 'enabled',
        dir: 'asc',
      },
    ];

    it('updates direction in sort configuration based on the payload and current state', () => {
      const state = Object.freeze({
        repositories: [],
        sortConfiguration: initialSortConfiguration,
      });

      const updatedSortConfiguration = [
        {
          key: 'publicId',
          dir: 'desc',
        },
        {
          key: 'managerInstanceId',
          dir: 'asc',
        },
        {
          key: 'enabled',
          dir: 'asc',
        },
      ];

      const { sortConfiguration } = reducer(state, {
        type: 'repositories/sortRepositories',
        payload: 'publicId',
      });

      expect(sortConfiguration).toEqual(updatedSortConfiguration);
    });

    it('updates priority of keys in sort configuration based on the payload and current state', () => {
      const state = Object.freeze({
        repositories: [],
        sortConfiguration: initialSortConfiguration,
      });

      const updatedSortConfiguration = [
        {
          key: 'managerInstanceId',
          dir: 'asc',
        },
        {
          key: 'publicId',
          dir: 'asc',
        },
        {
          key: 'enabled',
          dir: 'asc',
        },
      ];

      const { sortConfiguration } = reducer(state, {
        type: 'repositories/sortRepositories',
        payload: 'managerInstanceId',
      });

      expect(sortConfiguration).toEqual(updatedSortConfiguration);
    });

    it('sorts repositories based on the sort configuration', () => {
      const state = Object.freeze({
        repositories: [
          {
            managerInstanceId: '1',
            repository: {
              id: '1',
              publicId: 'b',
              enabled: true,
            },
          },
          {
            managerInstanceId: '2',
            repository: {
              id: '2',
              publicId: 'a',
              enabled: true,
            },
          },
          {
            managerInstanceId: '1',
            repository: {
              id: '3',
              publicId: 'd',
              enabled: false,
            },
          },
          {
            managerInstanceId: '2',
            repository: {
              id: '4',
              publicId: 'd',
              enabled: false,
            },
          },
        ],
        sortConfiguration: initialSortConfiguration,
      });

      const sortedRepositories = [
        {
          managerInstanceId: '1',
          repository: {
            id: '3',
            publicId: 'd',
            enabled: false,
          },
        },
        {
          managerInstanceId: '2',
          repository: {
            id: '4',
            publicId: 'd',
            enabled: false,
          },
        },
        {
          managerInstanceId: '1',
          repository: {
            id: '1',
            publicId: 'b',
            enabled: true,
          },
        },
        {
          managerInstanceId: '2',
          repository: {
            id: '2',
            publicId: 'a',
            enabled: true,
          },
        },
      ];

      const { repositories } = reducer(state, {
        type: 'repositories/sortRepositories',
        payload: 'publicId',
      });

      expect(repositories).toEqual(sortedRepositories);
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
