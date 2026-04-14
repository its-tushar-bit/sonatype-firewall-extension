/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, { VIEW_TYPES } from 'MainRoot/OrgsAndPolicies/repositories/repositoriesConfigurationSlice';

describe('repositoriesConfigurationSliceReducer', () => {
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
      const sortConfig = [
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
      const state = Object.freeze({
        currentView: VIEW_TYPES.CONTAINER,
        repositories: {
          [VIEW_TYPES.CONTAINER]: [],
          [VIEW_TYPES.MANAGER]: [],
        },
        originalRepositories: {
          [VIEW_TYPES.CONTAINER]: [],
          [VIEW_TYPES.MANAGER]: [],
        },
        loading: true,
        loadError: 'Loading error',
        deleteError: null,
        sortConfiguration: {
          [VIEW_TYPES.CONTAINER]: sortConfig,
          [VIEW_TYPES.MANAGER]: sortConfig,
        },
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
        {
          managerInstanceId: '1',
          repository: {
            id: '3',
            publicId: 'AA',
            enabled: true,
          },
        },
      ];

      const result = reducer(state, {
        type: 'repositories/loadRepositories/fulfilled',
        payload,
      });

      const containerRepos = result.repositories[VIEW_TYPES.CONTAINER];
      expect(containerRepos[0].repository.publicId).toBe('a');
      expect(containerRepos[0].managerInstanceId).toEqual('2');
      expect(containerRepos[0].repository.enabled).toBe(true);
      expect(containerRepos[1].repository.publicId).toBe('AA');
      expect(containerRepos[1].managerInstanceId).toBe('1');
      expect(containerRepos[1].repository.enabled).toBe(true);
      expect(containerRepos[2].repository.publicId).toBe('b');
      expect(containerRepos[2].managerInstanceId).toBe('1');
      expect(containerRepos[2].repository.enabled).toBe(true);
      expect(result.loading).toBe(false);
      expect(result.loadError).toBeNull();
    });
  });

  describe('repositories/loadRepositories/rejected action', () => {
    it('sets the error to the payload and the loading flag to false', () => {
      const state = Object.freeze({
        currentView: VIEW_TYPES.CONTAINER,
        repositories: {
          [VIEW_TYPES.CONTAINER]: [],
          [VIEW_TYPES.MANAGER]: [],
        },
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

    it('clears both repositories and originalRepositories to prevent stale data', () => {
      const state = Object.freeze({
        currentView: VIEW_TYPES.CONTAINER,
        repositories: {
          [VIEW_TYPES.CONTAINER]: [{ id: 1 }],
          [VIEW_TYPES.MANAGER]: [],
        },
        originalRepositories: {
          [VIEW_TYPES.CONTAINER]: [{ id: 1 }],
          [VIEW_TYPES.MANAGER]: [],
        },
        loading: true,
        loadError: null,
      });

      const result = reducer(state, {
        type: 'repositories/loadRepositories/rejected',
        payload: 'Loading error',
      });

      expect(result.repositories[VIEW_TYPES.CONTAINER]).toBeNull();
      expect(result.originalRepositories[VIEW_TYPES.CONTAINER]).toBeNull();
      expect(result.repositories[VIEW_TYPES.MANAGER]).toEqual([]);
      expect(result.originalRepositories[VIEW_TYPES.MANAGER]).toEqual([]);
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
        currentView: VIEW_TYPES.CONTAINER,
        repositories: {
          [VIEW_TYPES.CONTAINER]: [],
          [VIEW_TYPES.MANAGER]: [],
        },
        sortConfiguration: {
          [VIEW_TYPES.CONTAINER]: initialSortConfiguration,
          [VIEW_TYPES.MANAGER]: initialSortConfiguration,
        },
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

      const result = reducer(state, {
        type: 'repositories/sortRepositories',
        payload: 'publicId',
      });

      expect(result.sortConfiguration[VIEW_TYPES.CONTAINER]).toEqual(updatedSortConfiguration);
    });

    it('updates priority of keys in sort configuration based on the payload and current state', () => {
      const state = Object.freeze({
        currentView: VIEW_TYPES.CONTAINER,
        repositories: {
          [VIEW_TYPES.CONTAINER]: [],
          [VIEW_TYPES.MANAGER]: [],
        },
        sortConfiguration: {
          [VIEW_TYPES.CONTAINER]: initialSortConfiguration,
          [VIEW_TYPES.MANAGER]: initialSortConfiguration,
        },
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

      const result = reducer(state, {
        type: 'repositories/sortRepositories',
        payload: 'managerInstanceId',
      });

      expect(result.sortConfiguration[VIEW_TYPES.CONTAINER]).toEqual(updatedSortConfiguration);
    });

    it('sorts repositories based on the sort configuration', () => {
      const repos = [
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
        {
          managerInstanceId: '2',
          repository: {
            id: '5',
            publicId: 'AA',
            enabled: true,
          },
        },
      ];
      const state = Object.freeze({
        currentView: VIEW_TYPES.CONTAINER,
        repositories: {
          [VIEW_TYPES.CONTAINER]: repos,
          [VIEW_TYPES.MANAGER]: [],
        },
        sortConfiguration: {
          [VIEW_TYPES.CONTAINER]: initialSortConfiguration,
          [VIEW_TYPES.MANAGER]: initialSortConfiguration,
        },
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
            id: '5',
            publicId: 'AA',
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

      const result = reducer(state, {
        type: 'repositories/sortRepositories',
        payload: 'publicId',
      });

      expect(result.repositories[VIEW_TYPES.CONTAINER]).toEqual(sortedRepositories);
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

  describe('repositories/setShowEditRepositoryManagerNameModal action', () => {
    it('sets the edit repository manager name modal to be visible or not', () => {
      const state = Object.freeze({});

      const newState = reducer(state, {
        type: 'repositories/setShowEditRepositoryManagerNameModal',
        payload: true,
      });

      expect(newState).toEqual({
        showEditRepositoryManagerNameModal: true,
      });
    });
  });

  describe('repositories/setRepositoryManagerName action', () => {
    it('sets the repository manager name', () => {
      const state = Object.freeze({});

      const newState = reducer(state, {
        type: 'repositories/setRepositoryManagerName',
        payload: 'someRepositoryManagerName',
      });

      expect(newState).toEqual({
        editRepositoryManagerNameModalInfo: {
          managerName: 'someRepositoryManagerName',
        },
      });
    });
  });

  describe('repositories/openEditRepositoryManagerNameModal action', () => {
    it('sets the edit repository manager name modal to be visible and sets its information', () => {
      const state = Object.freeze({});

      const newState = reducer(state, {
        type: 'repositories/openEditRepositoryManagerNameModal',
        payload: {
          managerInstanceId: 'someManagerInstanceId',
          managerName: 'someManagerName',
          repoManagerId: 'repoManagerId1',
        },
      });

      expect(newState).toEqual({
        showEditRepositoryManagerNameModal: true,
        editRepositoryManagerNameModalInfo: {
          managerInstanceId: 'someManagerInstanceId',
          managerName: 'someManagerName',
          repoManagerId: 'repoManagerId1',
        },
        editRepositoryManagerNameError: null,
      });
    });
  });

  describe('repositories/editRepositoryManagerName/pending action', () => {
    it('sets the state appropriately', () => {
      const state = Object.freeze({});

      const newState = reducer(state, {
        type: 'repositories/editRepositoryManagerName/pending',
      });

      expect(newState).toEqual({
        loading: true,
        submitMaskState: false,
        editRepositoryManagerNameError: null,
      });
    });
  });

  describe('repositories/editRepositoryManagerName/fulfilled action', () => {
    it('sets the state appropriately', () => {
      const state = Object.freeze({});

      const newState = reducer(state, {
        type: 'repositories/editRepositoryManagerName/fulfilled',
      });

      expect(newState).toEqual({
        loading: false,
        submitMaskState: true,
      });
    });
  });

  describe('repositories/editRepositoryManagerName/rejected action', () => {
    it('sets the state appropriately', () => {
      const state = Object.freeze({});

      const newState = reducer(state, {
        type: 'repositories/editRepositoryManagerName/rejected',
        payload: 'someError',
      });

      expect(newState).toEqual({
        loading: false,
        submitMaskState: null,
        editRepositoryManagerNameError: 'someError',
      });
    });
  });

  describe('repositories/loadRepositoriesByManagerId/pending action', () => {
    it('sets the loading flag to true, unsets loading error and submitMask', () => {
      const state = Object.freeze({
        currentView: VIEW_TYPES.MANAGER,
        repositories: {
          [VIEW_TYPES.CONTAINER]: [],
          [VIEW_TYPES.MANAGER]: [],
        },
        loading: false,
        loadError: 'Loading error',
        submitMaskState: true,
      });

      const { loading, loadError, submitMaskState } = reducer(state, {
        type: 'repositories/loadRepositoriesByManagerId/pending',
      });

      expect(loading).toBe(true);
      expect(loadError).toBeNull();
      expect(submitMaskState).toBeNull();
    });
  });

  describe('repositories/loadRepositoriesByManagerId/fulfilled action', () => {
    it('sets loading flag to false, unsets the error and fills in the repositories details', () => {
      const sortConfig = [
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
      const state = Object.freeze({
        currentView: VIEW_TYPES.MANAGER,
        repositories: {
          [VIEW_TYPES.CONTAINER]: [],
          [VIEW_TYPES.MANAGER]: [],
        },
        originalRepositories: {
          [VIEW_TYPES.CONTAINER]: [],
          [VIEW_TYPES.MANAGER]: [],
        },
        loading: true,
        loadError: 'Loading error',
        deleteError: null,
        sortConfiguration: {
          [VIEW_TYPES.CONTAINER]: sortConfig,
          [VIEW_TYPES.MANAGER]: sortConfig,
        },
      });

      const payload = [
        {
          managerInstanceId: '1',
          repository: {
            id: '1',
            publicId: 'a',
            enabled: true,
          },
        },
        {
          managerInstanceId: '1',
          repository: {
            id: '2',
            publicId: 'b',
            enabled: false,
          },
        },
      ];

      const result = reducer(state, {
        type: 'repositories/loadRepositoriesByManagerId/fulfilled',
        payload,
      });

      const managerRepos = result.repositories[VIEW_TYPES.MANAGER];
      expect(managerRepos[0].repository.publicId).toBe('a');
      expect(managerRepos[0].managerInstanceId).toEqual('1');
      expect(managerRepos[0].repository.enabled).toBe(true);
      expect(managerRepos[1].repository.publicId).toBe('b');
      expect(managerRepos[1].managerInstanceId).toBe('1');
      expect(managerRepos[1].repository.enabled).toBe(false);
      expect(result.loading).toBe(false);
      expect(result.loadError).toBeNull();
    });
  });

  describe('repositories/loadRepositoriesByManagerId/rejected action', () => {
    it('sets the error to the payload and the loading flag to false', () => {
      const state = Object.freeze({
        currentView: VIEW_TYPES.MANAGER,
        repositories: {
          [VIEW_TYPES.CONTAINER]: [],
          [VIEW_TYPES.MANAGER]: [],
        },
        originalRepositories: {
          [VIEW_TYPES.CONTAINER]: [],
          [VIEW_TYPES.MANAGER]: [],
        },
        loading: true,
        loadError: null,
      });

      const { loading, loadError } = reducer(state, {
        type: 'repositories/loadRepositoriesByManagerId/rejected',
        payload: 'Loading error',
      });

      expect(loading).toBe(false);
      expect(loadError).toBe('Loading error');
    });

    it('clears both repositories and originalRepositories to prevent stale data', () => {
      const state = Object.freeze({
        currentView: VIEW_TYPES.MANAGER,
        repositories: {
          [VIEW_TYPES.CONTAINER]: [],
          [VIEW_TYPES.MANAGER]: [{ id: 1 }],
        },
        originalRepositories: {
          [VIEW_TYPES.CONTAINER]: [],
          [VIEW_TYPES.MANAGER]: [{ id: 1 }],
        },
        loading: true,
        loadError: null,
      });

      const result = reducer(state, {
        type: 'repositories/loadRepositoriesByManagerId/rejected',
        payload: 'Loading error',
      });

      expect(result.repositories[VIEW_TYPES.MANAGER]).toBeNull();
      expect(result.originalRepositories[VIEW_TYPES.MANAGER]).toBeNull();
      expect(result.repositories[VIEW_TYPES.CONTAINER]).toEqual([]);
      expect(result.originalRepositories[VIEW_TYPES.CONTAINER]).toEqual([]);
    });
  });

  describe('View Separation (NEXUS-41949 fix)', () => {
    describe('repositories/setCurrentView action', () => {
      it('sets the current view to CONTAINER', () => {
        const state = Object.freeze({
          currentView: VIEW_TYPES.MANAGER,
        });

        const { currentView } = reducer(state, {
          type: 'repositories/setCurrentView',
          payload: VIEW_TYPES.CONTAINER,
        });

        expect(currentView).toBe(VIEW_TYPES.CONTAINER);
      });

      it('sets the current view to MANAGER', () => {
        const state = Object.freeze({
          currentView: VIEW_TYPES.CONTAINER,
        });

        const { currentView } = reducer(state, {
          type: 'repositories/setCurrentView',
          payload: VIEW_TYPES.MANAGER,
        });

        expect(currentView).toBe(VIEW_TYPES.MANAGER);
      });
    });

    describe('filter state separation between views', () => {
      it('maintains independent repositoryPublicIdFilter for each view', () => {
        const initialState = {
          currentView: VIEW_TYPES.CONTAINER,
          repositoryPublicIdFilter: {
            [VIEW_TYPES.CONTAINER]: '',
            [VIEW_TYPES.MANAGER]: '',
          },
          repositoryFormatsFilter: {
            [VIEW_TYPES.CONTAINER]: new Set(),
            [VIEW_TYPES.MANAGER]: new Set(),
          },
          originalRepositories: {
            [VIEW_TYPES.CONTAINER]: [
              {
                repository: { publicId: 'repo1', format: 'maven' },
              },
            ],
            [VIEW_TYPES.MANAGER]: [
              {
                repository: { publicId: 'repo2', format: 'npm' },
              },
            ],
          },
          repositories: {
            [VIEW_TYPES.CONTAINER]: [],
            [VIEW_TYPES.MANAGER]: [],
          },
        };

        // Set filter in CONTAINER view
        const stateAfterContainerFilter = reducer(initialState, {
          type: 'repositories/setRepositoryPublicIdFilter',
          payload: 'testFilter',
        });

        // Container view should have filter applied
        expect(stateAfterContainerFilter.repositoryPublicIdFilter[VIEW_TYPES.CONTAINER]).toBe('testFilter');
        // Manager view should still be empty
        expect(stateAfterContainerFilter.repositoryPublicIdFilter[VIEW_TYPES.MANAGER]).toBe('');

        // Switch to MANAGER view
        const stateInManagerView = reducer(stateAfterContainerFilter, {
          type: 'repositories/setCurrentView',
          payload: VIEW_TYPES.MANAGER,
        });

        // Set different filter in MANAGER view
        const stateAfterManagerFilter = reducer(stateInManagerView, {
          type: 'repositories/setRepositoryPublicIdFilter',
          payload: 'differentFilter',
        });

        // Manager view should have its own filter
        expect(stateAfterManagerFilter.repositoryPublicIdFilter[VIEW_TYPES.MANAGER]).toBe('differentFilter');
        // Container view filter should remain unchanged
        expect(stateAfterManagerFilter.repositoryPublicIdFilter[VIEW_TYPES.CONTAINER]).toBe('testFilter');
      });

      it('maintains independent repositoryFormatsFilter for each view', () => {
        const initialState = {
          currentView: VIEW_TYPES.CONTAINER,
          repositoryPublicIdFilter: {
            [VIEW_TYPES.CONTAINER]: '',
            [VIEW_TYPES.MANAGER]: '',
          },
          repositoryFormatsFilter: {
            [VIEW_TYPES.CONTAINER]: new Set(),
            [VIEW_TYPES.MANAGER]: new Set(),
          },
          originalRepositories: {
            [VIEW_TYPES.CONTAINER]: [
              {
                repository: { publicId: 'repo1', format: 'maven' },
              },
            ],
            [VIEW_TYPES.MANAGER]: [
              {
                repository: { publicId: 'repo2', format: 'npm' },
              },
            ],
          },
          repositories: {
            [VIEW_TYPES.CONTAINER]: [],
            [VIEW_TYPES.MANAGER]: [],
          },
        };

        // Set format filter in CONTAINER view
        const mavenFilter = new Set(['maven']);
        const stateAfterContainerFilter = reducer(initialState, {
          type: 'repositories/setRepositoryFormatsFilter',
          payload: mavenFilter,
        });

        // Container view should have maven filter
        expect(stateAfterContainerFilter.repositoryFormatsFilter[VIEW_TYPES.CONTAINER]).toEqual(mavenFilter);
        // Manager view should still be empty
        expect(stateAfterContainerFilter.repositoryFormatsFilter[VIEW_TYPES.MANAGER]).toEqual(new Set());

        // Switch to MANAGER view
        const stateInManagerView = reducer(stateAfterContainerFilter, {
          type: 'repositories/setCurrentView',
          payload: VIEW_TYPES.MANAGER,
        });

        // Set different format filter in MANAGER view
        const npmFilter = new Set(['npm']);
        const stateAfterManagerFilter = reducer(stateInManagerView, {
          type: 'repositories/setRepositoryFormatsFilter',
          payload: npmFilter,
        });

        // Manager view should have npm filter
        expect(stateAfterManagerFilter.repositoryFormatsFilter[VIEW_TYPES.MANAGER]).toEqual(npmFilter);
        // Container view filter should remain maven
        expect(stateAfterManagerFilter.repositoryFormatsFilter[VIEW_TYPES.CONTAINER]).toEqual(mavenFilter);
      });
    });

    describe('sort state separation between views', () => {
      it('maintains independent sort configuration for each view', () => {
        const initialSortConfig = [
          { key: 'publicId', dir: 'asc' },
          { key: 'format', dir: 'asc' },
        ];

        const initialState = {
          currentView: VIEW_TYPES.CONTAINER,
          sortConfiguration: {
            [VIEW_TYPES.CONTAINER]: [...initialSortConfig],
            [VIEW_TYPES.MANAGER]: [...initialSortConfig],
          },
          repositories: {
            [VIEW_TYPES.CONTAINER]: [
              { repository: { publicId: 'b', format: 'maven' } },
              { repository: { publicId: 'a', format: 'npm' } },
            ],
            [VIEW_TYPES.MANAGER]: [
              { repository: { publicId: 'z', format: 'maven' } },
              { repository: { publicId: 'y', format: 'npm' } },
            ],
          },
        };

        // Sort in CONTAINER view by publicId (toggles to desc)
        const stateAfterContainerSort = reducer(initialState, {
          type: 'repositories/sortRepositories',
          payload: 'publicId',
        });

        // Container view sort should change to desc
        expect(stateAfterContainerSort.sortConfiguration[VIEW_TYPES.CONTAINER][0]).toEqual({
          key: 'publicId',
          dir: 'desc',
        });

        // Manager view sort should remain asc
        expect(stateAfterContainerSort.sortConfiguration[VIEW_TYPES.MANAGER][0]).toEqual({
          key: 'publicId',
          dir: 'asc',
        });
      });
    });

    describe('repository data separation between views', () => {
      it('maintains independent repositories for each view', () => {
        const containerRepos = [
          {
            managerInstanceId: '1',
            repository: { id: '1', publicId: 'containerRepo1' },
          },
          {
            managerInstanceId: '2',
            repository: { id: '2', publicId: 'containerRepo2' },
          },
        ];

        const managerRepos = [
          {
            managerInstanceId: '1',
            repository: { id: '3', publicId: 'managerRepo1' },
          },
        ];

        const initialState = {
          currentView: VIEW_TYPES.CONTAINER,
          repositories: {
            [VIEW_TYPES.CONTAINER]: [],
            [VIEW_TYPES.MANAGER]: [],
          },
          originalRepositories: {
            [VIEW_TYPES.CONTAINER]: [],
            [VIEW_TYPES.MANAGER]: [],
          },
          sortConfiguration: {
            [VIEW_TYPES.CONTAINER]: [{ key: 'publicId', dir: 'asc' }],
            [VIEW_TYPES.MANAGER]: [{ key: 'publicId', dir: 'asc' }],
          },
          loading: false,
          loadError: null,
        };

        // Load repositories for CONTAINER view
        const stateWithContainerRepos = reducer(initialState, {
          type: 'repositories/loadRepositories/fulfilled',
          payload: containerRepos,
        });

        expect(stateWithContainerRepos.repositories[VIEW_TYPES.CONTAINER]).toHaveLength(2);
        expect(stateWithContainerRepos.repositories[VIEW_TYPES.MANAGER]).toHaveLength(0);

        // Switch to MANAGER view
        const stateInManagerView = reducer(stateWithContainerRepos, {
          type: 'repositories/setCurrentView',
          payload: VIEW_TYPES.MANAGER,
        });

        // Load repositories for MANAGER view
        const stateWithManagerRepos = reducer(stateInManagerView, {
          type: 'repositories/loadRepositoriesByManagerId/fulfilled',
          payload: managerRepos,
        });

        // Manager view should have 1 repo
        expect(stateWithManagerRepos.repositories[VIEW_TYPES.MANAGER]).toHaveLength(1);
        expect(stateWithManagerRepos.repositories[VIEW_TYPES.MANAGER][0].repository.publicId).toBe('managerRepo1');

        // Container view should still have 2 repos
        expect(stateWithManagerRepos.repositories[VIEW_TYPES.CONTAINER]).toHaveLength(2);
      });
    });

    describe('repositories/resetViewFilters action', () => {
      it('resets repositoryPublicIdFilter for the specified view', () => {
        const state = Object.freeze({
          repositoryPublicIdFilter: {
            [VIEW_TYPES.CONTAINER]: 'test-filter',
            [VIEW_TYPES.MANAGER]: 'manager-filter',
          },
          repositoryFormatsFilter: {
            [VIEW_TYPES.CONTAINER]: new Set(['maven']),
            [VIEW_TYPES.MANAGER]: new Set(['npm']),
          },
          sortConfiguration: {
            [VIEW_TYPES.CONTAINER]: [
              { key: 'publicId', dir: 'desc' },
              { key: 'format', dir: 'asc' },
            ],
            [VIEW_TYPES.MANAGER]: [
              { key: 'format', dir: 'desc' },
              { key: 'publicId', dir: 'asc' },
            ],
          },
        });

        const result = reducer(state, {
          type: 'repositories/resetViewFilters',
          payload: VIEW_TYPES.CONTAINER,
        });

        // Container view should be reset
        expect(result.repositoryPublicIdFilter[VIEW_TYPES.CONTAINER]).toBe('');
        expect(result.repositoryFormatsFilter[VIEW_TYPES.CONTAINER]).toEqual(new Set());
        expect(result.sortConfiguration[VIEW_TYPES.CONTAINER]).toEqual([
          { key: 'publicId', dir: 'asc' },
          { key: 'format', dir: 'asc' },
          { key: 'repositoryType', dir: 'asc' },
          { key: 'managerInstanceId', dir: 'asc' },
        ]);

        // Manager view should remain unchanged
        expect(result.repositoryPublicIdFilter[VIEW_TYPES.MANAGER]).toBe('manager-filter');
        expect(result.repositoryFormatsFilter[VIEW_TYPES.MANAGER]).toEqual(new Set(['npm']));
        expect(result.sortConfiguration[VIEW_TYPES.MANAGER]).toEqual([
          { key: 'format', dir: 'desc' },
          { key: 'publicId', dir: 'asc' },
        ]);
      });

      it('resets repositoryFormatsFilter for the specified view', () => {
        const state = Object.freeze({
          repositoryPublicIdFilter: {
            [VIEW_TYPES.CONTAINER]: 'container-filter',
            [VIEW_TYPES.MANAGER]: 'manager-filter',
          },
          repositoryFormatsFilter: {
            [VIEW_TYPES.CONTAINER]: new Set(['maven', 'npm']),
            [VIEW_TYPES.MANAGER]: new Set(['docker']),
          },
          sortConfiguration: {
            [VIEW_TYPES.CONTAINER]: [{ key: 'publicId', dir: 'asc' }],
            [VIEW_TYPES.MANAGER]: [{ key: 'publicId', dir: 'asc' }],
          },
        });

        const result = reducer(state, {
          type: 'repositories/resetViewFilters',
          payload: VIEW_TYPES.MANAGER,
        });

        // Manager view should be reset
        expect(result.repositoryPublicIdFilter[VIEW_TYPES.MANAGER]).toBe('');
        expect(result.repositoryFormatsFilter[VIEW_TYPES.MANAGER]).toEqual(new Set());

        // Container view should remain unchanged
        expect(result.repositoryPublicIdFilter[VIEW_TYPES.CONTAINER]).toBe('container-filter');
        expect(result.repositoryFormatsFilter[VIEW_TYPES.CONTAINER]).toEqual(new Set(['maven', 'npm']));
      });

      it('resets sort configuration to initial state for the specified view', () => {
        const modifiedSortConfig = [
          { key: 'format', dir: 'desc' },
          { key: 'repositoryType', dir: 'desc' },
          { key: 'publicId', dir: 'asc' },
        ];

        const state = Object.freeze({
          repositoryPublicIdFilter: {
            [VIEW_TYPES.CONTAINER]: '',
            [VIEW_TYPES.MANAGER]: '',
          },
          repositoryFormatsFilter: {
            [VIEW_TYPES.CONTAINER]: new Set(),
            [VIEW_TYPES.MANAGER]: new Set(),
          },
          sortConfiguration: {
            [VIEW_TYPES.CONTAINER]: modifiedSortConfig,
            [VIEW_TYPES.MANAGER]: modifiedSortConfig,
          },
        });

        const result = reducer(state, {
          type: 'repositories/resetViewFilters',
          payload: VIEW_TYPES.CONTAINER,
        });

        // Container view should be reset to initial sort configuration
        expect(result.sortConfiguration[VIEW_TYPES.CONTAINER]).toEqual([
          { key: 'publicId', dir: 'asc' },
          { key: 'format', dir: 'asc' },
          { key: 'repositoryType', dir: 'asc' },
          { key: 'managerInstanceId', dir: 'asc' },
        ]);

        // Manager view should remain unchanged
        expect(result.sortConfiguration[VIEW_TYPES.MANAGER]).toEqual(modifiedSortConfig);
      });

      it('ensures cleanup on view unmount works correctly', () => {
        // Simulates the lifecycle: apply filters → unmount → remount
        const initialState = {
          currentView: VIEW_TYPES.CONTAINER,
          repositoryPublicIdFilter: {
            [VIEW_TYPES.CONTAINER]: '',
            [VIEW_TYPES.MANAGER]: '',
          },
          repositoryFormatsFilter: {
            [VIEW_TYPES.CONTAINER]: new Set(),
            [VIEW_TYPES.MANAGER]: new Set(),
          },
          sortConfiguration: {
            [VIEW_TYPES.CONTAINER]: [{ key: 'publicId', dir: 'asc' }],
            [VIEW_TYPES.MANAGER]: [{ key: 'publicId', dir: 'asc' }],
          },
          originalRepositories: {
            [VIEW_TYPES.CONTAINER]: [{ repository: { publicId: 'repo1', format: 'maven' } }],
            [VIEW_TYPES.MANAGER]: [],
          },
          repositories: {
            [VIEW_TYPES.CONTAINER]: [],
            [VIEW_TYPES.MANAGER]: [],
          },
        };

        // Step 1: Apply filter in container view
        const stateWithFilter = reducer(initialState, {
          type: 'repositories/setRepositoryPublicIdFilter',
          payload: 'test',
        });
        expect(stateWithFilter.repositoryPublicIdFilter[VIEW_TYPES.CONTAINER]).toBe('test');

        // Step 2: Unmount container view (cleanup runs)
        const stateAfterCleanup = reducer(stateWithFilter, {
          type: 'repositories/resetViewFilters',
          payload: VIEW_TYPES.CONTAINER,
        });

        // Step 3: Verify filters are cleared
        expect(stateAfterCleanup.repositoryPublicIdFilter[VIEW_TYPES.CONTAINER]).toBe('');
        expect(stateAfterCleanup.repositoryFormatsFilter[VIEW_TYPES.CONTAINER]).toEqual(new Set());
      });
    });

    describe('null safety (its-tushar-bit review)', () => {
      it('handles null originalRepositories when filtering', () => {
        const state = {
          currentView: VIEW_TYPES.CONTAINER,
          repositoryPublicIdFilter: {
            [VIEW_TYPES.CONTAINER]: 'test',
            [VIEW_TYPES.MANAGER]: '',
          },
          repositoryFormatsFilter: {
            [VIEW_TYPES.CONTAINER]: new Set(),
            [VIEW_TYPES.MANAGER]: new Set(),
          },
          originalRepositories: {
            [VIEW_TYPES.CONTAINER]: null,
            [VIEW_TYPES.MANAGER]: [],
          },
          repositories: {
            [VIEW_TYPES.CONTAINER]: [{ id: 1 }],
            [VIEW_TYPES.MANAGER]: [],
          },
        };

        const result = reducer(state, {
          type: 'repositories/setRepositoryPublicIdFilter',
          payload: 'newFilter',
        });

        expect(result.repositories[VIEW_TYPES.CONTAINER]).toBeNull();
        expect(result.repositoryPublicIdFilter[VIEW_TYPES.CONTAINER]).toBe('newFilter');
      });

      it('handles repositories with incomplete nested properties', () => {
        const state = {
          currentView: VIEW_TYPES.CONTAINER,
          repositoryPublicIdFilter: {
            [VIEW_TYPES.CONTAINER]: '',
            [VIEW_TYPES.MANAGER]: '',
          },
          repositoryFormatsFilter: {
            [VIEW_TYPES.CONTAINER]: new Set(['maven']),
            [VIEW_TYPES.MANAGER]: new Set(),
          },
          originalRepositories: {
            [VIEW_TYPES.CONTAINER]: [
              { repository: { publicId: 'repo1', format: 'maven' } },
              { repository: { publicId: 'repo2' } },
              { repository: null },
              null,
            ],
            [VIEW_TYPES.MANAGER]: [],
          },
          repositories: {
            [VIEW_TYPES.CONTAINER]: [],
            [VIEW_TYPES.MANAGER]: [],
          },
        };

        const result = reducer(state, {
          type: 'repositories/setRepositoryFormatsFilter',
          payload: new Set(['maven']),
        });

        expect(result.repositories[VIEW_TYPES.CONTAINER]).toHaveLength(1);
        expect(result.repositories[VIEW_TYPES.CONTAINER][0].repository.publicId).toBe('repo1');
      });
    });
  });
});
