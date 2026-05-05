/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, { initialState } from 'MainRoot/hostedRepos/hostedReposListSlice';

describe('hostedReposList slice', () => {
  describe('sortRepositories', () => {
    it('toggles direction when clicking the current primary sort column', () => {
      const state = {
        ...initialState,
        sortConfiguration: [{ key: 'publicId', dir: 'asc' }],
      };

      const newState = reducer(state, { type: 'hostedReposList/sortRepositories', payload: 'publicId' });

      expect(newState.sortConfiguration[0]).toEqual({ key: 'publicId', dir: 'desc' });
    });

    it('toggles from desc to asc when clicking the current primary sort column again', () => {
      const state = {
        ...initialState,
        sortConfiguration: [{ key: 'format', dir: 'desc' }],
      };

      const newState = reducer(state, { type: 'hostedReposList/sortRepositories', payload: 'format' });

      expect(newState.sortConfiguration[0]).toEqual({ key: 'format', dir: 'asc' });
    });

    it('promotes a secondary sort column to primary and retains its direction', () => {
      const state = {
        ...initialState,
        sortConfiguration: [
          { key: 'publicId', dir: 'asc' },
          { key: 'format', dir: 'desc' },
        ],
      };

      const newState = reducer(state, { type: 'hostedReposList/sortRepositories', payload: 'format' });

      expect(newState.sortConfiguration[0]).toEqual({ key: 'format', dir: 'desc' });
      expect(newState.sortConfiguration[1]).toEqual({ key: 'publicId', dir: 'asc' });
    });

    it('adds a new column as primary sort with asc direction when column is not in configuration', () => {
      const state = {
        ...initialState,
        sortConfiguration: [{ key: 'publicId', dir: 'asc' }],
      };

      const newState = reducer(state, {
        type: 'hostedReposList/sortRepositories',
        payload: 'lastScannedTime',
      });

      expect(newState.sortConfiguration[0]).toEqual({ key: 'lastScannedTime', dir: 'asc' });
      expect(newState.sortConfiguration[1]).toEqual({ key: 'publicId', dir: 'asc' });
    });
  });

  describe('setRepositoryFormatsFilter', () => {
    it('sets the repositoryFormatsFilter', () => {
      const state = { ...initialState, repositoryFormatsFilter: '' };

      const newState = reducer(state, {
        type: 'hostedReposList/setRepositoryFormatsFilter',
        payload: 'maven2',
      });

      expect(newState.repositoryFormatsFilter).toBe('maven2');
    });

    it('clears the repositoryFormatsFilter when set to empty string', () => {
      const state = { ...initialState, repositoryFormatsFilter: 'npm' };

      const newState = reducer(state, {
        type: 'hostedReposList/setRepositoryFormatsFilter',
        payload: '',
      });

      expect(newState.repositoryFormatsFilter).toBe('');
    });
  });

  describe('hostedReposList/loadRepositories/pending', () => {
    it('sets loading to true and clears loadError', () => {
      const state = { ...initialState, loading: false, loadError: 'some error' };

      const newState = reducer(state, { type: 'hostedReposList/loadRepositories/pending' });

      expect(newState.loading).toBe(true);
      expect(newState.loadError).toBeNull();
    });
  });

  describe('hostedReposList/loadRepositories/fulfilled', () => {
    it('sets repositories, totalCount, and manager info from payload', () => {
      const state = { ...initialState, loading: true };
      const payload = {
        repositories: [{ publicId: 'repo1', format: 'npm' }],
        totalCount: 1,
        manager: { instanceId: 'nxrm-1', baseUrl: 'https://nxrm.example.com' },
      };

      const newState = reducer(state, {
        type: 'hostedReposList/loadRepositories/fulfilled',
        payload,
      });

      expect(newState.loading).toBe(false);
      expect(newState.loadError).toBeNull();
      expect(newState.repositories).toEqual(payload.repositories);
      expect(newState.totalCount).toBe(1);
      expect(newState.managerInstanceId).toBe('nxrm-1');
      expect(newState.managerBaseUrl).toBe('https://nxrm.example.com');
    });

    it('defaults repositories to empty array when payload repositories is falsy', () => {
      const state = { ...initialState, loading: true };

      const newState = reducer(state, {
        type: 'hostedReposList/loadRepositories/fulfilled',
        payload: { repositories: null, totalCount: 0, manager: { instanceId: 'nxrm-1', baseUrl: null } },
      });

      expect(newState.repositories).toEqual([]);
    });

    it('sets manager info even when repositories list is empty', () => {
      const state = { ...initialState, loading: true };

      const newState = reducer(state, {
        type: 'hostedReposList/loadRepositories/fulfilled',
        payload: { repositories: [], totalCount: 0, manager: { instanceId: 'nxrm-1', baseUrl: 'https://nxrm.example.com' } },
      });

      expect(newState.managerInstanceId).toBe('nxrm-1');
      expect(newState.managerBaseUrl).toBe('https://nxrm.example.com');
    });
  });

  describe('hostedReposList/loadRepositories/rejected', () => {
    it('sets loadError and clears repositories', () => {
      const state = { ...initialState, loading: true, repositories: [{ publicId: 'repo1' }] };

      const newState = reducer(state, {
        type: 'hostedReposList/loadRepositories/rejected',
        payload: { response: { status: 500, data: { message: 'Server Error' } } },
      });

      expect(newState.loading).toBe(false);
      expect(newState.repositories).toEqual([]);
      expect(newState.loadError).toBeTruthy();
    });
  });

  describe('hostedReposList/loadAvailableFormats/fulfilled', () => {
    it('sets availableFormats from payload', () => {
      const state = { ...initialState, availableFormatsLoading: true };

      const newState = reducer(state, {
        type: 'hostedReposList/loadAvailableFormats/fulfilled',
        payload: ['maven2', 'npm', 'pypi'],
      });

      expect(newState.availableFormatsLoading).toBe(false);
      expect(newState.availableFormats).toEqual(['maven2', 'npm', 'pypi']);
    });
  });

  describe('hostedReposList/loadAvailableFormats/rejected', () => {
    it('clears availableFormats and sets loading to false', () => {
      const state = { ...initialState, availableFormatsLoading: true, availableFormats: ['npm'] };

      const newState = reducer(state, {
        type: 'hostedReposList/loadAvailableFormats/rejected',
      });

      expect(newState.availableFormatsLoading).toBe(false);
      expect(newState.availableFormats).toEqual([]);
    });
  });
});
