/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectHostedRepositoriesListSlice,
  selectRepositories,
  selectLoading,
  selectLoadError,
  selectSortConfiguration,
  selectRepositoryFormatsFilter,
  selectManagerInstanceId,
  selectManagerBaseUrl,
  selectRepositoryManager,
  selectAvailableFormats,
} from 'MainRoot/hostedRepos/hostedReposListSelectors';

describe('hostedReposListSelectors', () => {
  let mockState;

  beforeEach(() => {
    mockState = {
      hostedReposList: {
        repositories: [{ publicId: 'repo1', format: 'npm' }],
        loading: false,
        loadError: null,
        sortConfiguration: [{ key: 'publicId', dir: 'asc' }],
        repositoryFormatsFilter: 'npm',
        availableFormats: ['npm', 'maven2'],
        availableFormatsLoading: false,
        managerInstanceId: 'nxrm-1',
        managerBaseUrl: 'https://nxrm.example.com',
      },
    };
  });

  describe('selectHostedRepositoriesListSlice', () => {
    it('returns the hostedReposList slice', () => {
      expect(selectHostedRepositoriesListSlice(mockState)).toEqual(mockState.hostedReposList);
    });
  });

  describe('selectRepositories', () => {
    it('returns the repositories array', () => {
      expect(selectRepositories(mockState)).toEqual([{ publicId: 'repo1', format: 'npm' }]);
    });
  });

  describe('selectLoading', () => {
    it('returns false when not loading', () => {
      expect(selectLoading(mockState)).toBe(false);
    });

    it('returns true when loading', () => {
      const loadingState = { hostedReposList: { ...mockState.hostedReposList, loading: true } };
      expect(selectLoading(loadingState)).toBe(true);
    });
  });

  describe('selectLoadError', () => {
    it('returns null when no error', () => {
      expect(selectLoadError(mockState)).toBeNull();
    });

    it('returns the error message when set', () => {
      mockState.hostedReposList.loadError = 'An error occurred';
      expect(selectLoadError(mockState)).toBe('An error occurred');
    });
  });

  describe('selectSortConfiguration', () => {
    it('returns the sort configuration array', () => {
      expect(selectSortConfiguration(mockState)).toEqual([{ key: 'publicId', dir: 'asc' }]);
    });
  });

  describe('selectRepositoryFormatsFilter', () => {
    it('returns the format filter value', () => {
      expect(selectRepositoryFormatsFilter(mockState)).toBe('npm');
    });
  });

  describe('selectManagerInstanceId', () => {
    it('returns the manager instance ID', () => {
      expect(selectManagerInstanceId(mockState)).toBe('nxrm-1');
    });
  });

  describe('selectManagerBaseUrl', () => {
    it('returns the manager base URL', () => {
      expect(selectManagerBaseUrl(mockState)).toBe('https://nxrm.example.com');
    });
  });

  describe('selectRepositoryManager', () => {
    it('returns combined manager object when instanceId is set', () => {
      expect(selectRepositoryManager(mockState)).toEqual({
        instanceId: 'nxrm-1',
        baseUrl: 'https://nxrm.example.com',
      });
    });

    it('returns null when instanceId is null', () => {
      mockState.hostedReposList.managerInstanceId = null;
      expect(selectRepositoryManager(mockState)).toBeNull();
    });

    it('returns null baseUrl when managerBaseUrl is null', () => {
      mockState.hostedReposList.managerBaseUrl = null;
      expect(selectRepositoryManager(mockState)).toEqual({
        instanceId: 'nxrm-1',
        baseUrl: null,
      });
    });
  });

  describe('selectAvailableFormats', () => {
    it('returns the available formats array', () => {
      expect(selectAvailableFormats(mockState)).toEqual(['npm', 'maven2']);
    });
  });
});
