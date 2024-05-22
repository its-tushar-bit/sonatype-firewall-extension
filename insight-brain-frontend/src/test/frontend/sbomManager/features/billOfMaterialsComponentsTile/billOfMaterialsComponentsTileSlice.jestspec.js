/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, {
  SORT_BY_FIELDS,
  SORT_DIRECTION,
} from 'MainRoot/sbomManager/features/billOfMaterialsComponentsTile/billOfMaterialsComponentsTileSlice';

const componentTemplate = ({ hash, name, dependencyType }) =>
  Object.freeze({
    hash,
    packageUrl: `pkg:maven/com.package.${name}/${name}@1.2.3?type=jar`,
    name,
    version: '1.2.3',
    dependencyType,
    componentIdentifier: {
      format: 'maven',
      coordinates: {
        artifactId: name,
        extension: 'jar',
        groupId: `com.package.${name}`,
        version: '1.2.3',
      },
    },
    displayName: `com.package.${name} : ${name} : 1.2.3`,
    licenses: [
      {
        licenseId: 'MIT',
        licenseName: 'MIT',
      },
    ],
    vulnerabilitySeverityNoneCount: 0,
    vulnerabilitySeverityLowCount: 1,
    vulnerabilitySeverityMediumCount: 2,
    vulnerabilitySeverityHighCount: 3,
    vulnerabilitySeverityCriticalCount: 4,
    percentageAnnotated: 50.0,
  });

const componentResults = [
  componentTemplate({ hash: 'hash-1', name: 'alice', dependencyType: 'direct' }),
  componentTemplate({ hash: 'hash-2', name: 'bob', dependencyType: 'transitive' }),
];

describe('billOfMaterialsComponentsTileSlice', function () {
  const defaultSortConfiguration = Object.freeze({
    sortBy: SORT_BY_FIELDS.vulnerabilities,
    sortDirection: SORT_DIRECTION.DESC,
  });

  const paginationInitialState = Object.freeze({
    pageCount: 1,
    currentPage: 0,
  });

  const initialState = Object.freeze({
    loadingComponents: true,
    loadingComponentsErrorMessage: null,
    components: null,
    totalNumberOfComponents: null,

    sortConfiguration: { ...defaultSortConfiguration },

    pagination: { ...paginationInitialState },
  });

  describe('setLoadingComponents', () => {
    it('sets the correct loadingComponents value', () => {
      const state = {
        loadingComponents: false,
      };

      const newState = reducer(state, {
        type: 'billOfMaterialsComponentsTile/setLoadingComponents',
        payload: true,
      });

      expect(newState.loadingComponents).toBe(true);
    });
  });

  describe('resetLoadComponentsConfigurations', () => {
    it('resets loadComponents configurations', () => {
      const state = {
        sortConfiguration: {
          sortBy: SORT_BY_FIELDS.type,
          sortDirection: SORT_DIRECTION.ASC,
        },
        pagination: {
          pageCount: 99,
          currentPage: 42,
        },
      };

      const newState = reducer(state, {
        type: 'billOfMaterialsComponentsTile/resetLoadComponentsConfigurations',
      });

      expect(newState.sortConfiguration).toEqual(defaultSortConfiguration);
      expect(newState.pagination).toEqual(paginationInitialState);
    });
  });

  describe('billOfMaterialsComponentsTile/loadComponents', function () {
    it('/pending', () => {
      const state = { ...initialState };

      const newState = reducer(state, {
        type: 'billOfMaterialsComponentsTile/loadComponents/pending',
      });

      expect(newState.loadingComponents).toBe(true);
      expect(newState.loadingComponentsErrorMessage).toBe(null);

      expect(newState.components).toBe(null);
      expect(newState.totalNumberOfComponents).toBe(null);
    });

    it('/failed', () => {
      const state = { ...initialState };

      const payload = {
        response: {
          data: 'This is an error message',
        },
      };

      const newState = reducer(state, {
        type: 'billOfMaterialsComponentsTile/loadComponents/rejected',
        payload,
      });

      expect(newState.loadingComponents).toBe(false);
      expect(newState.loadingComponentsErrorMessage).toBe('This is an error message');

      expect(newState.components).toBe(null);
      expect(newState.totalNumberOfComponents).toBe(null);

      expect(newState.pagination).toEqual(paginationInitialState);
    });

    it('/fulfilled (calculates the correct totalNumberOfComponents)', () => {
      const state = {
        ...initialState,
        pagination: {
          pageCount: 1,
          currentPage: 123,
        },
      };

      const payload = {
        totalResultsCount: 101,
        results: [],
      };

      const newState = reducer(state, {
        type: 'billOfMaterialsComponentsTile/loadComponents/fulfilled',
        payload,
      });

      expect(newState.pagination.pageCount).toBe(3);
      expect(newState.pagination.currentPage).toBe(123);
    });

    it('/fulfilled', () => {
      const state = {
        ...initialState,
      };

      const payload = {
        totalResultsCount: 2,
        results: [...componentResults],
      };

      const newState = reducer(state, {
        type: 'billOfMaterialsComponentsTile/loadComponents/fulfilled',
        payload,
      });

      expect(newState.components[0].name).toBe('alice');
      expect(newState.components[0].dependencyType).toBe('direct');

      expect(newState.components[1].name).toBe('bob');
      expect(newState.components[1].dependencyType).toBe('transitive');
    });
  });

  describe('setSortByAndCycleDirection', () => {
    it('should only cycle between ASC and DESC when sortBy is set to the default field', () => {
      const state = { ...initialState };

      const newState1 = reducer(state, {
        type: 'billOfMaterialsComponentsTile/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.vulnerabilities,
      });

      expect(newState1.sortConfiguration.sortBy).toBe(SORT_BY_FIELDS.vulnerabilities);
      expect(newState1.sortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);

      const newState2 = reducer(newState1, {
        type: 'billOfMaterialsComponentsTile/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.vulnerabilities,
      });

      expect(newState2.sortConfiguration.sortBy).toBe(SORT_BY_FIELDS.vulnerabilities);
      expect(newState2.sortConfiguration.sortDirection).toBe(SORT_DIRECTION.DESC);

      const newState3 = reducer(newState2, {
        type: 'billOfMaterialsComponentsTile/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.vulnerabilities,
      });

      expect(newState3.sortConfiguration.sortBy).toBe(SORT_BY_FIELDS.vulnerabilities);
      expect(newState3.sortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);
    });

    it('should cycle a non-default field correctly', () => {
      const state = { ...initialState };

      const newState1 = reducer(state, {
        type: 'billOfMaterialsComponentsTile/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.type,
      });

      expect(newState1.sortConfiguration.sortBy).toBe(SORT_BY_FIELDS.type);
      expect(newState1.sortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);

      const newState2 = reducer(newState1, {
        type: 'billOfMaterialsComponentsTile/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.type,
      });

      expect(newState2.sortConfiguration.sortBy).toBe(SORT_BY_FIELDS.type);
      expect(newState2.sortConfiguration.sortDirection).toBe(SORT_DIRECTION.DESC);

      const newState3 = reducer(newState2, {
        type: 'billOfMaterialsComponentsTile/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.type,
      });

      expect(newState3.sortConfiguration).toEqual(defaultSortConfiguration);
    });

    it('should cycle a non-default field correctly after cycling the default field', () => {
      const state = { ...initialState };

      const newState0 = reducer(state, {
        type: 'billOfMaterialsComponentsTile/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.vulnerabilities,
      });

      expect(newState0.sortConfiguration.sortBy).toBe(SORT_BY_FIELDS.vulnerabilities);
      expect(newState0.sortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);

      const newState1 = reducer(newState0, {
        type: 'billOfMaterialsComponentsTile/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.percentageAnnotated,
      });

      expect(newState1.sortConfiguration.sortBy).toBe(SORT_BY_FIELDS.percentageAnnotated);
      expect(newState1.sortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);

      const newState2 = reducer(newState1, {
        type: 'billOfMaterialsComponentsTile/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.percentageAnnotated,
      });

      expect(newState2.sortConfiguration.sortBy).toBe(SORT_BY_FIELDS.percentageAnnotated);
      expect(newState2.sortConfiguration.sortDirection).toBe(SORT_DIRECTION.DESC);

      const newState3 = reducer(newState2, {
        type: 'billOfMaterialsComponentsTile/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.vulnerabilities,
      });

      expect(newState3.sortConfiguration.sortBy).toBe(SORT_BY_FIELDS.vulnerabilities);
      expect(newState3.sortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);
    });

    it('should cycle the default field correctly after cycling a non-default field', () => {
      const state = { ...initialState };

      const newState0 = reducer(state, {
        type: 'billOfMaterialsComponentsTile/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.type,
      });

      expect(newState0.sortConfiguration.sortBy).toBe(SORT_BY_FIELDS.type);
      expect(newState0.sortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);

      const newState1 = reducer(newState0, {
        type: 'billOfMaterialsComponentsTile/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.vulnerabilities,
      });

      expect(newState1.sortConfiguration.sortBy).toBe(SORT_BY_FIELDS.vulnerabilities);
      expect(newState1.sortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);

      const newState2 = reducer(newState1, {
        type: 'billOfMaterialsComponentsTile/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.vulnerabilities,
      });

      expect(newState2.sortConfiguration).toEqual(defaultSortConfiguration);

      const newState3 = reducer(newState2, {
        type: 'billOfMaterialsComponentsTile/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.vulnerabilities,
      });

      expect(newState3.sortConfiguration.sortBy).toBe(SORT_BY_FIELDS.vulnerabilities);
      expect(newState3.sortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);
    });
  });

  describe('setCurrentPage', () => {
    it('sets the correct currentPage value without changing pageCount', () => {
      const state = {
        ...initialState,
        pagination: {
          pageCount: 999,
          currentPage: 0,
        },
      };

      const newState = reducer(state, {
        type: 'billOfMaterialsComponentsTile/setCurrentPage',
        payload: 123,
      });

      expect(newState.pagination.pageCount).toBe(999);
      expect(newState.pagination.currentPage).toBe(123);
    });
  });
});
