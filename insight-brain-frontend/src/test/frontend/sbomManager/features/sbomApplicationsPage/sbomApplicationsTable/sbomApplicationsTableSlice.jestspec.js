/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, {
  SORT_BY_FIELDS,
  SORT_DIRECTION,
} from 'MainRoot/sbomManager/features/sbomApplicationsPage/sbomApplicationsTable/sbomApplicationsTableSlice';

const applicationTemplate = (applicationName, sbomVersion) =>
  Object.freeze({
    applicationName,
    sbomVersion,
    applicationInternalId: 'app-internal-id',
    applicationPublicId: 'app-public-id',
    importDate: '2024-12-12T00:00:00+0000',
    vulnerabilitySummary: {
      critical: 4,
      high: 3,
      medium: 2,
      low: 1,
      none: 0,
    },
    policyViolationSummary: {
      critical: 444,
      severe: 333,
      moderate: 222,
      low: 111,
    },
    releaseStatusPercentage: 1.0,
  });

const mockApplications = Object.freeze([
  applicationTemplate('app-1', 'version-1'),
  applicationTemplate('app-2', 'version-2'),
]);

describe('sbomApplicationsTableSlice', function () {
  const defaultSortConfiguration = Object.freeze({
    sortBy: SORT_BY_FIELDS.importDate,
    sortDirection: SORT_DIRECTION.DESC,
  });

  const defaultPagination = Object.freeze({
    pageCount: 1,
    currentPage: 0,
  });

  const initialState = Object.freeze({
    loading: true,
    errorMessage: null,

    applications: null,
    applicationsTotalCount: null,

    sortConfiguration: { ...defaultSortConfiguration },
    pagination: { ...defaultPagination },

    applicationNameRawFilterTerm: '',
  });

  describe('sbomApplicationsTable/setLoading', () => {
    it('sets the correct loading value', () => {
      const state = Object.freeze({
        loading: false,
      });

      const newState = reducer(state, {
        type: 'sbomApplicationsTable/setLoading',
        payload: true,
      });

      expect(newState.loading).toBe(true);
    });
  });

  describe('sbomApplicationsTable/resetConfigurations', () => {
    it('resets all configurations', () => {
      const state = Object.freeze({
        sortConfiguration: {
          sortBy: SORT_BY_FIELDS.vulnerabilities,
          sortDirection: SORT_DIRECTION.DESC,
        },
        pagination: {
          pageCount: 99,
          currentPage: 42,
        },
      });

      const newState = reducer(state, {
        type: 'sbomApplicationsTable/resetConfigurations',
      });

      expect(newState.sortConfiguration).toEqual(defaultSortConfiguration);
      expect(newState.pagination).toEqual(defaultPagination);
    });
  });

  describe('sbomApplicationsTable/loadApplications', function () {
    it('/pending', () => {
      const state = Object.freeze({ ...initialState });

      const newState = reducer(state, {
        type: 'sbomApplicationsTable/loadApplications/pending',
      });

      expect(newState.loading).toBe(true);
      expect(newState.errorMessage).toBe(null);

      expect(newState.applications).toBe(null);
      expect(newState.applicationsTotalCount).toBe(null);
    });

    it('/failed', () => {
      const state = Object.freeze({ ...initialState });

      const payload = Object.freeze({
        response: {
          data: 'This is an error message',
        },
      });

      const newState = reducer(state, {
        type: 'sbomApplicationsTable/loadApplications/rejected',
        payload,
      });

      expect(newState.loading).toBe(false);
      expect(newState.errorMessage).toBe('This is an error message');

      expect(newState.applications).toBe(null);
      expect(newState.applicationsTotalCount).toBe(null);

      expect(newState.pagination).toEqual(defaultPagination);
    });

    it('/fulfilled (calculates the correct totalNumberOfComponents)', () => {
      const state = Object.freeze({
        ...initialState,
        pagination: {
          pageCount: 1,
          currentPage: 0,
        },
      });

      const payload = Object.freeze({
        applications: [],
        totalCount: 101,
      });

      const newState = reducer(state, {
        type: 'sbomApplicationsTable/loadApplications/fulfilled',
        payload,
      });

      expect(newState.pagination.pageCount).toBe(3);
      expect(newState.pagination.currentPage).toBe(0);
    });

    it('/fulfilled', () => {
      const state = Object.freeze({
        ...initialState,
      });

      const payload = Object.freeze({
        applications: [...mockApplications],
        totalCount: 2,
      });

      const newState = reducer(state, {
        type: 'sbomApplicationsTable/loadApplications/fulfilled',
        payload,
      });

      expect(newState.applications[0].applicationName).toBe('app-1');
      expect(newState.applications[0].sbomVersion).toBe('version-1');

      expect(newState.applications[1].applicationName).toBe('app-2');
      expect(newState.applications[1].sbomVersion).toBe('version-2');
    });
  });

  describe('setSortByAndDirection', () => {
    it('sets the correct sort configuration', () => {
      const state = Object.freeze({ ...initialState });

      const newState = reducer(state, {
        type: 'sbomApplicationsTable/setSortByAndDirection',
        payload: {
          sortBy: SORT_BY_FIELDS.vulnerabilities,
          sortDirection: SORT_DIRECTION.DESC,
        },
      });

      expect(newState.sortConfiguration.sortBy).toBe(SORT_BY_FIELDS.vulnerabilities);
      expect(newState.sortConfiguration.sortDirection).toBe(SORT_DIRECTION.DESC);
    });

    it('sets the default configuration when undefined is passed', () => {
      const state = Object.freeze({ ...initialState });

      const newState = reducer(state, {
        type: 'sbomApplicationsTable/setSortByAndDirection',
        payload: {
          sortBy: undefined,
          sortDirection: undefined,
        },
      });

      expect(newState.sortConfiguration.sortBy).toBe(SORT_BY_FIELDS.importDate);
      expect(newState.sortConfiguration.sortDirection).toBe(SORT_DIRECTION.DESC);
    });
  });

  describe('setSortByAndCycleDirection', () => {
    it('should only cycle between ASC and DESC when sortBy is set to the default field', () => {
      const state = Object.freeze({ ...initialState });

      const newState1 = reducer(state, {
        type: 'sbomApplicationsTable/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.importDate,
      });

      expect(newState1.sortConfiguration.sortBy).toBe(SORT_BY_FIELDS.importDate);
      expect(newState1.sortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);

      const newState2 = reducer(newState1, {
        type: 'sbomApplicationsTable/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.importDate,
      });

      expect(newState2.sortConfiguration.sortBy).toBe(SORT_BY_FIELDS.importDate);
      expect(newState2.sortConfiguration.sortDirection).toBe(SORT_DIRECTION.DESC);

      const newState3 = reducer(newState2, {
        type: 'sbomApplicationsTable/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.importDate,
      });

      expect(newState3.sortConfiguration.sortBy).toBe(SORT_BY_FIELDS.importDate);
      expect(newState3.sortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);
    });

    it('should cycle a non-default field correctly', () => {
      const state = Object.freeze({ ...initialState });

      const newState1 = reducer(state, {
        type: 'sbomApplicationsTable/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.vulnerabilities,
      });

      expect(newState1.sortConfiguration.sortBy).toBe(SORT_BY_FIELDS.vulnerabilities);
      expect(newState1.sortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);

      const newState2 = reducer(newState1, {
        type: 'sbomApplicationsTable/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.vulnerabilities,
      });

      expect(newState2.sortConfiguration.sortBy).toBe(SORT_BY_FIELDS.vulnerabilities);
      expect(newState2.sortConfiguration.sortDirection).toBe(SORT_DIRECTION.DESC);

      const newState3 = reducer(newState2, {
        type: 'sbomApplicationsTable/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.vulnerabilities,
      });

      expect(newState3.sortConfiguration).toEqual(defaultSortConfiguration);
    });

    it('should cycle a non-default field correctly after cycling the default field', () => {
      const state = Object.freeze({ ...initialState });

      const newState0 = reducer(state, {
        type: 'sbomApplicationsTable/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.importDate,
      });

      expect(newState0.sortConfiguration.sortBy).toBe(SORT_BY_FIELDS.importDate);
      expect(newState0.sortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);

      const newState1 = reducer(newState0, {
        type: 'sbomApplicationsTable/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.vulnerabilities,
      });

      expect(newState1.sortConfiguration.sortBy).toBe(SORT_BY_FIELDS.vulnerabilities);
      expect(newState1.sortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);

      const newState2 = reducer(newState1, {
        type: 'sbomApplicationsTable/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.vulnerabilities,
      });

      expect(newState2.sortConfiguration.sortBy).toBe(SORT_BY_FIELDS.vulnerabilities);
      expect(newState2.sortConfiguration.sortDirection).toBe(SORT_DIRECTION.DESC);

      const newState3 = reducer(newState2, {
        type: 'sbomApplicationsTable/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.importDate,
      });

      expect(newState3.sortConfiguration.sortBy).toBe(SORT_BY_FIELDS.importDate);
      expect(newState3.sortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);
    });

    it('should cycle the default field correctly after cycling a non-default field', () => {
      const state = Object.freeze({ ...initialState });

      const newState0 = reducer(state, {
        type: 'sbomApplicationsTable/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.vulnerabilities,
      });

      expect(newState0.sortConfiguration.sortBy).toBe(SORT_BY_FIELDS.vulnerabilities);
      expect(newState0.sortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);

      const newState1 = reducer(newState0, {
        type: 'sbomApplicationsTable/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.vulnerabilities,
      });

      expect(newState1.sortConfiguration.sortBy).toBe(SORT_BY_FIELDS.vulnerabilities);
      expect(newState1.sortConfiguration.sortDirection).toBe(SORT_DIRECTION.DESC);

      const newState2 = reducer(newState1, {
        type: 'sbomApplicationsTable/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.importDate,
      });

      expect(newState2.sortConfiguration.sortBy).toBe(SORT_BY_FIELDS.importDate);
      expect(newState2.sortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);

      const newState3 = reducer(newState2, {
        type: 'sbomApplicationsTable/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.importDate,
      });

      expect(newState3.sortConfiguration).toEqual(defaultSortConfiguration);
    });

    it('should set sortBy and cycle sortDirection for SORT_BY_FIELDS.latestVersion', () => {
      const newState = reducer(initialState, {
        type: 'sbomApplicationsTable/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.latestVersion,
      });

      expect(newState.sortConfiguration.sortBy).toBe(SORT_BY_FIELDS.latestVersion);
      expect(newState.sortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);

      const newState2 = reducer(newState, {
        type: 'sbomApplicationsTable/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.latestVersion,
      });

      expect(newState2.sortConfiguration.sortDirection).toBe(SORT_DIRECTION.DESC);
    });

    it('should set sortBy and cycle sortDirection for SORT_BY_FIELDS.name', () => {
      const newState = reducer(initialState, {
        type: 'sbomApplicationsTable/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.name,
      });

      expect(newState.sortConfiguration.sortBy).toBe(SORT_BY_FIELDS.name);
      expect(newState.sortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);

      const newState2 = reducer(newState, {
        type: 'sbomApplicationsTable/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.name,
      });

      expect(newState2.sortConfiguration.sortDirection).toBe(SORT_DIRECTION.DESC);
    });
  });

  describe('setApplicationNameRawFilterTerm', () => {
    it('sets the correct applicationNameRawFilterTerm state', () => {
      const state = Object.freeze({
        ...initialState,
        applicationNameRawFilterTerm: '',
      });

      const newState = reducer(state, {
        type: 'sbomApplicationsTable/setApplicationNameRawFilterTerm',
        payload: 'Hello',
      });

      expect(newState.applicationNameRawFilterTerm).toBe('Hello');
    });
  });

  describe('setCurrentPage', () => {
    it('sets the correct currentPage value without changing pageCount', () => {
      const state = Object.freeze({
        ...initialState,
        pagination: {
          pageCount: 999,
          currentPage: 0,
        },
      });

      const newState = reducer(state, {
        type: 'sbomApplicationsTable/setCurrentPage',
        payload: 123,
      });

      expect(newState.pagination.pageCount).toBe(999);
      expect(newState.pagination.currentPage).toBe(123);
    });
  });
});
