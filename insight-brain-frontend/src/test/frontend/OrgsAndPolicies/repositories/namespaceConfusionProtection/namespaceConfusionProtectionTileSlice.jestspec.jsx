/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reducer from 'MainRoot/OrgsAndPolicies/repositories/namespaceConfusionProtectionTile/namespaceConfusionProtectionTileSlice';

describe('namespaceConfusionProtectionTileSlice', () => {
  describe('NamespaceConfusionProtectionTile/getComponentNamePatterns/pending action', () => {
    it('sets the loading flag to true, unsets loading error', () => {
      const state = Object.freeze({
        loadingComponentNamePatterns: false,
        errorComponentsTable: 'Loading error',
      });

      const { loadingComponentNamePatterns, errorComponentsTable } = reducer(state, {
        type: 'namespaceConfusionProtectionTile/getComponentNamePatterns/pending',
      });

      expect(loadingComponentNamePatterns).toBe(true);
      expect(errorComponentsTable).toBeNull();
    });
  });

  describe('namespaceConfusionProtectionTile/getComponentNamePatterns/fulfilled action', () => {
    it('sets loading flag to false, unsets the error and fills in the repository components details', () => {
      const state = Object.freeze({
        componentNamePatterns: {
          repository_managers: [],
        },
        hasNextPage: {
          repository_managers: null,
        },
        loadingComponentNamePatterns: true,
        errorComponentsTable: 'Loading error',
        namePatternsTableConfig: {
          page: 1,
          pageSize: 2,
        },
        currentFilterKey: 'repository_managers',
      });

      const payload = {
        proprietaryComponentNamePatterns: [
          {
            id: 'ea481e4a1eb347b5b1ee5dbecce663df',
            format: 'maven',
            namespacePattern: 'test-org-names',
            namePattern: 'intellij-dependencies',
            repositoryManagerInstanceId: '2fbfdad21dd94de69b17ab8c4565e99d',
            repositoryPublicId: '8098fb28cfc84ff99b3c34e66d2b9ccf',
            enabled: true,
          },
          {
            id: '0bb3dc7f90cf4deaa63bc524c3114c9b',
            format: 'maven',
            namespacePattern: 'numberslist',
            namePattern: 'maven-central',
            repositoryManagerInstanceId: 'c8d574691e664d908829fb72f04de655',
            repositoryPublicId: '8098fb28cfc84ff99b3c34e66d2b9ccf',
            enabled: true,
          },
        ],
        currentFilterKey: 'repository_managers',
      };

      const expectedState = {
        repository_managers: [
          {
            id: 'ea481e4a1eb347b5b1ee5dbecce663df',
            format: 'maven',
            namespacePattern: 'test-org-names',
            namePattern: 'intellij-dependencies',
            repositoryManagerInstanceId: '2fbfdad21dd94de69b17ab8c4565e99d',
            repositoryPublicId: '8098fb28cfc84ff99b3c34e66d2b9ccf',
            enabled: true,
          },
          {
            id: '0bb3dc7f90cf4deaa63bc524c3114c9b',
            format: 'maven',
            namespacePattern: 'numberslist',
            namePattern: 'maven-central',
            repositoryManagerInstanceId: 'c8d574691e664d908829fb72f04de655',
            repositoryPublicId: '8098fb28cfc84ff99b3c34e66d2b9ccf',
            enabled: true,
          },
        ],
      };

      const { componentNamePatterns, loadingComponentNamePatterns, errorComponentsTable } = reducer(state, {
        type: 'namespaceConfusionProtectionTile/getComponentNamePatterns/fulfilled',
        payload,
      });

      expect(componentNamePatterns).toEqual(expectedState);
      expect(loadingComponentNamePatterns).toBe(false);
      expect(errorComponentsTable).toBeNull();
    });
  });

  describe('namespaceConfusionProtectionTile/getComponentNamePatterns/rejected action', () => {
    it('sets the error to the payload and the loading flag to false', () => {
      const state = Object.freeze({
        loadingComponentNamePatterns: true,
        errorComponentsTable: null,
      });

      const { loadingComponentNamePatterns, errorComponentsTable } = reducer(state, {
        type: 'namespaceConfusionProtectionTile/getComponentNamePatterns/rejected',
        payload: 'Loading error',
      });

      expect(loadingComponentNamePatterns).toBe(false);
      expect(errorComponentsTable).toBe('Loading error');
    });
  });

  describe('namespaceConfusionProtectionTile/setSorting action', () => {
    it('changes sorting direction to the opposite if we sort the same column', () => {
      const state = Object.freeze({
        namePatternsTableConfig: {
          repository_managers: {
            sortFields: [
              {
                columnName: 'PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME',
                dir: 'asc',
              },
            ],
          },
        },
        currentFilterKey: 'repository_managers',
      });

      const expectedSortFields = [
        {
          columnName: 'PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME',
          dir: 'desc',
        },
      ];

      const {
        namePatternsTableConfig: {
          repository_managers: { sortFields },
        },
      } = reducer(state, {
        type: 'namespaceConfusionProtectionTile/setSorting',
        payload: 'PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME',
      });

      expect(sortFields).toEqual(expectedSortFields);
    });

    it('changes direction to asc if we sort different columns', () => {
      const state = Object.freeze({
        namePatternsTableConfig: {
          repository_managers: {
            sortFields: [
              {
                columnName: 'PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME',
                dir: 'asc',
              },
            ],
          },
        },
        currentFilterKey: 'repository_managers',
      });

      const expectedSortFields = [
        {
          columnName: 'REPOSITORY_MANAGER_INSTANCE_ID',
          dir: 'asc',
        },
        {
          columnName: 'PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME',
          dir: 'asc',
        },
      ];

      const {
        namePatternsTableConfig: {
          repository_managers: { sortFields },
        },
      } = reducer(state, {
        type: 'namespaceConfusionProtectionTile/setSorting',
        payload: 'REPOSITORY_MANAGER_INSTANCE_ID',
      });

      expect(sortFields).toEqual(expectedSortFields);
    });
  });

  describe('namespaceConfusionProtectionTile/increasePage action', () => {
    it('increases page counter', () => {
      const state = Object.freeze({
        namePatternsTableConfig: {
          repository_managers: {
            page: 1,
          },
        },
        currentFilterKey: 'repository_managers',
      });

      const {
        namePatternsTableConfig: {
          repository_managers: { page },
        },
      } = reducer(state, {
        type: 'namespaceConfusionProtectionTile/increasePage',
      });

      expect(page).toEqual(2);
    });
  });

  describe('namespaceConfusionProtectionTile/decreasePage action', () => {
    it('decrease page counter', () => {
      const state = Object.freeze({
        namePatternsTableConfig: {
          repository_managers: {
            page: 3,
          },
        },
        currentFilterKey: 'repository_managers',
      });

      const {
        namePatternsTableConfig: {
          repository_managers: { page },
        },
      } = reducer(state, {
        type: 'namespaceConfusionProtectionTile/decreasePage',
      });

      expect(page).toEqual(2);
    });
  });

  describe('namespaceConfusionProtectionTile/setFilter action', () => {
    it('sets new search filter value if filter does not exist', () => {
      const state = Object.freeze({
        namePatternsTableConfig: {
          repository_managers: {
            searchFilters: [],
          },
        },
        searchFiltersValues: {
          repository_managers: {
            PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME: '',
          },
        },
        currentFilterKey: 'repository_managers',
      });

      const payload = {
        filterName: 'PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME',
        filterValue: 'Filter',
        filterSection: 'repository_managers',
      };

      const expectedFilters = [
        {
          filterableField: 'PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME',
          value: 'Filter',
        },
      ];

      const {
        namePatternsTableConfig: {
          repository_managers: { searchFilters },
        },
      } = reducer(state, {
        type: 'namespaceConfusionProtectionTile/setFilter',
        payload,
      });

      expect(searchFilters).toEqual(expectedFilters);
    });
  });

  it('changes search filter value if filter already exist', () => {
    const state = Object.freeze({
      namePatternsTableConfig: {
        repository_managers: {
          searchFilters: [
            {
              filterableField: 'PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME',
              value: 'Old namespace value',
            },
          ],
          sortFields: [
            {
              columnName: 'PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME',
              dir: 'asc',
            },
          ],
        },
      },
      searchFiltersValues: {
        repository_managers: {
          PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME: '',
        },
      },
      currentFilterKey: 'repository_managers',
    });

    const payload = {
      filterName: 'PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME',
      filterValue: 'New namespace value',
      filterSection: 'repository_managers',
    };

    const expectedFilters = [
      {
        filterableField: 'PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME',
        value: 'New namespace value',
      },
    ];

    const {
      namePatternsTableConfig: {
        repository_managers: { searchFilters },
      },
    } = reducer(state, {
      type: 'namespaceConfusionProtectionTile/setFilter',
      payload,
    });

    expect(searchFilters).toEqual(expectedFilters);
  });

  it('removes search filter value if filter values is empty string', () => {
    const state = Object.freeze({
      namePatternsTableConfig: {
        repository_managers: {
          searchFilters: [
            {
              filterableField: 'PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME',
              value: 'namespace',
            },
          ],
        },
      },
      searchFiltersValues: {
        PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME: '',
      },
      currentFilterKey: 'repository_managers',
    });

    const payload = {
      filterName: 'PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME',
      filterValue: '',
    };

    const expectedFilters = [];

    const {
      namePatternsTableConfig: {
        repository_managers: { searchFilters },
      },
    } = reducer(state, {
      type: 'namespaceConfusionProtectionTile/setFilter',
      payload,
    });

    expect(searchFilters).toEqual(expectedFilters);
  });

  describe('NamespaceConfusionProtectionTile/updateComponentNamePattern/pending action', () => {
    it('sets the loading flag to true, unsets loading error', () => {
      const state = Object.freeze({
        updatingComponentNamePattern: false,
        errorUpdatingComponentNamePattern: 'Loading error',
      });

      const { updatingComponentNamePattern, errorUpdatingComponentNamePattern } = reducer(state, {
        type: 'namespaceConfusionProtectionTile/updateComponentNamePattern/pending',
      });

      expect(updatingComponentNamePattern).toBe(true);
      expect(errorUpdatingComponentNamePattern).toBeNull();
    });
  });

  describe('namespaceConfusionProtectionTile/updateComponentNamePattern/fulfilled action', () => {
    it('sets loading flag to false, unsets the error', () => {
      const state = Object.freeze({
        updatingComponentNamePattern: true,
        errorUpdatingComponentNamePattern: 'Loading error',
      });

      const payload = {
        component: {
          id: 'ea481e4a1eb347b5b1ee5dbecce663df',
          format: 'maven',
          namespacePattern: 'test-org-names',
          namePattern: 'intellij-dependencies',
          repositoryManagerInstanceId: '2fbfdad21dd94de69b17ab8c4565e99d',
          repositoryPublicId: '8098fb28cfc84ff99b3c34e66d2b9ccf',
          enabled: true,
        },
      };

      const { updatingComponentNamePattern, errorUpdatingComponentNamePattern } = reducer(state, {
        type: 'namespaceConfusionProtectionTile/updateComponentNamePattern/fulfilled',
        payload,
      });

      expect(updatingComponentNamePattern).toBe(false);
      expect(errorUpdatingComponentNamePattern).toBeNull();
    });
  });

  describe('namespaceConfusionProtectionTile/updateComponentNamePattern/rejected action', () => {
    it('sets the error to the payload and the loading flag to false', () => {
      const state = Object.freeze({
        updatingComponentNamePattern: true,
        errorUpdatingComponentNamePattern: null,
      });

      const { updatingComponentNamePattern, errorUpdatingComponentNamePattern } = reducer(state, {
        type: 'namespaceConfusionProtectionTile/updateComponentNamePattern/rejected',
        payload: 'Loading error',
      });

      expect(updatingComponentNamePattern).toBe(false);
      expect(errorUpdatingComponentNamePattern).toBe('Loading error');
    });
  });

  describe('namespaceConfusionProtectionTile/setCurrentFilterKey action', () => {
    it('set current key and states for the new key', () => {
      const state = Object.freeze({
        componentNamePatterns: {},
        hasNextPage: {},
        namePatternsTableConfig: {},
        searchFiltersValues: {},
        currentFilterKey: 'repository_managers',
      });

      const setCurrentFilterkey = 'new_key';

      const expectedState = {
        componentNamePatterns: {
          [setCurrentFilterkey]: [],
        },
        hasNextPage: {
          [setCurrentFilterkey]: null,
        },
        namePatternsTableConfig: {
          [setCurrentFilterkey]: {
            page: 1,
            pageSize: 6,
            searchFilters: [],
            sortFields: [
              {
                columnName: 'PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME',
                dir: 'asc',
              },
            ],
          },
        },
        searchFiltersValues: {
          [setCurrentFilterkey]: {
            PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME: '',
          },
        },
        currentFilterKey: setCurrentFilterkey,
      };

      const newState = reducer(state, {
        type: 'namespaceConfusionProtectionTile/setCurrentFilterKey',
        payload: 'new_key',
      });

      expect(newState).toEqual(expectedState);
    });
  });
});
