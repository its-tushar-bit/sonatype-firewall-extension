/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reducer from 'MainRoot/OrgsAndPolicies/repositories/repositoryResultsSummaryPage/repositoryResultsSummaryPageSlice';

describe('repositoryResultsSummaryPageSlice', () => {
  describe('repositoryResultsSummaryPage/getRepositoryInformation', () => {
    it('sets the repositoryInfo object when action is fulfilled', () => {
      const state = Object.freeze({
        repositoryInfo: {
          publicId: null,
        },
        loadingRepositoryInformation: false,
        errorRepositoryInformation: null,
      });
      const expectedObj = {
        repository: {
          publicId: 'testRepo',
        },
        loadingRepositoryInformation: false,
        errorRepositoryInformation: null,
      };

      const { repositoryInfo } = reducer(state, {
        type: 'repositoryResultsSummaryPage/getRepositoryInformation/fulfilled',
        payload: expectedObj,
      });
      expect(repositoryInfo).toEqual(expectedObj.repository);
    });

    it('sets the repositoryInfo object when action is rejected', () => {
      const state = {
        repository: {
          publicId: 'testRepo',
        },
        loadingRepositoryInformation: false,
        errorRepositoryInformation: null,
      };

      const expectedObj = Object.freeze({
        repositoryInfo: null,
        loadingRepositoryInformation: false,
        errorRepositoryInformation: 'Error',
      });

      const { repositoryInfo } = reducer(state, {
        type: 'repositoryResultsSummaryPage/getRepositoryInformation/rejected',
        payload: expectedObj,
      });
      expect(repositoryInfo).toEqual(expectedObj.repositoryInfo);
    });
  });

  describe('repositoryResultsSummaryPage/getRepositorySummary', () => {
    it('sets the repositoryResultsSummaryPage object when action is fulfilled', () => {
      const state = Object.freeze({
        affectedComponentCount: null,
        criticalViolationCount: null,
        knownComponentCount: null,
        moderateViolationCount: null,
        quarantinedComponentCount: null,
        severeViolationCount: null,
        totalComponentCount: null,
        loadingSummaryTile: false,
        errorSummaryTile: null,
      });
      const expectedObj = {
        affectedComponentCount: 1,
        criticalViolationCount: 2,
        knownComponentCount: 3,
        moderateViolationCount: 4,
        quarantinedComponentCount: 5,
        severeViolationCount: 6,
        totalComponentCount: 7,
        loadingSummaryTile: false,
        errorSummaryTile: null,
      };

      const repositorySummaryTileState = reducer(state, {
        type: 'repositoryResultsSummaryPage/getRepositorySummary/fulfilled',
        payload: expectedObj,
      });
      expect(repositorySummaryTileState).toEqual(expectedObj);
    });

    it('sets the repositoryResultsSummaryPage object when action is rejected', () => {
      const state = Object.freeze({
        affectedComponentCount: 1,
        criticalViolationCount: 2,
        knownComponentCount: 3,
        moderateViolationCount: 4,
        quarantinedComponentCount: 5,
        severeViolationCount: 6,
        totalComponentCount: 7,
        loadingSummaryTile: false,
        errorSummaryTile: null,
      });
      const expectedObj = {
        affectedComponentCount: null,
        criticalViolationCount: null,
        knownComponentCount: null,
        moderateViolationCount: null,
        quarantinedComponentCount: null,
        severeViolationCount: null,
        totalComponentCount: null,
        loadingSummaryTile: false,
        errorSummaryTile: 'Error',
      };

      const repositorySummaryTileState = reducer(state, {
        type: 'repositoryResultsSummaryPage/getRepositorySummary/rejected',
        payload: expectedObj,
      });
      expect(repositorySummaryTileState).toEqual(expectedObj);
    });

    it('sets the repositoryResultsSummaryPage object when action is pending', () => {
      const state = Object.freeze({
        affectedComponentCount: null,
        criticalViolationCount: null,
        knownComponentCount: null,
        moderateViolationCount: null,
        quarantinedComponentCount: null,
        severeViolationCount: null,
        totalComponentCount: null,
        loadingSummaryTile: false,
        errorSummaryTile: null,
      });
      const expectedObj = {
        affectedComponentCount: null,
        criticalViolationCount: null,
        knownComponentCount: null,
        moderateViolationCount: null,
        quarantinedComponentCount: null,
        severeViolationCount: null,
        totalComponentCount: null,
        loadingSummaryTile: true,
        errorSummaryTile: null,
      };

      const repositorySummaryTileState = reducer(state, {
        type: 'repositoryResultsSummaryPage/getRepositorySummary/pending',
        payload: expectedObj,
      });
      expect(repositorySummaryTileState).toEqual(expectedObj);
    });
  });

  describe('repositoryResultsSummaryPage/getRepositoryComponents/pending action', () => {
    it('sets the loading flag to true, unsets loading error', () => {
      const state = Object.freeze({
        loadingRepositoryComponents: false,
        errorComponentsTable: 'Loading error',
      });

      const { loadingRepositoryComponents, errorComponentsTable } = reducer(state, {
        type: 'repositoryResultsSummaryPage/getRepositoryComponents/pending',
      });

      expect(loadingRepositoryComponents).toBe(true);
      expect(errorComponentsTable).toBeNull();
    });
  });

  describe('repositoryResultsSummaryPage/getRepositoryComponents/fulfilled action', () => {
    it('sets loading flag to false, unsets the error and fills in the repository components details', () => {
      const state = Object.freeze({
        repositoryComponents: [],
        loadingRepositoryComponents: true,
        errorComponentsTable: 'Loading error',
        unsortedComponents: [],
        hasMoreResults: null,
        componentsRequestBody: {
          pageSize: 1,
        },
      });

      const payload = {
        repositoryResultsDetails: [
          {
            threatLevel: 4,
            policyName: 'Security-High',
            lastEvaluationTime: null,
            quarantineTime: null,
            componentDisplayText: 'Component name 1',
            waived: false,
          },
        ],
        hasNextPage: true,
      };

      const expectedState = [
        {
          threatLevel: 4,
          policyName: 'Security-High',
          lastEvaluationTime: null,
          quarantineTime: null,
          componentDisplayText: 'Component name 1',
          waived: false,
        },
      ];

      const {
        repositoryComponents,
        loadingRepositoryComponents,
        errorComponentsTable,
        hasMoreResults,
        unsortedComponents,
      } = reducer(state, {
        type: 'repositoryResultsSummaryPage/getRepositoryComponents/fulfilled',
        payload,
      });

      expect(repositoryComponents).toEqual(expectedState);
      expect(loadingRepositoryComponents).toBe(false);
      expect(errorComponentsTable).toBeNull();
      expect(unsortedComponents).toEqual(expectedState);
      expect(hasMoreResults).toBe(true);
    });

    it('stores filterCount as filteredTotalCount for bulk waive responses', () => {
      const state = Object.freeze({
        repositoryComponents: [],
        loadingRepositoryComponents: true,
        errorComponentsTable: 'Loading error',
        unsortedComponents: [],
        hasMoreResults: null,
        filteredTotalCount: null,
        componentsRequestBody: {
          pageSize: 12,
        },
      });

      const payload = {
        repositoryResultsDetails: [],
        hasNextPage: false,
        totalCount: 30,
        filterCount: 12,
      };

      const { filteredTotalCount } = reducer(state, {
        type: 'repositoryResultsSummaryPage/getRepositoryComponentsForBulkWaive/fulfilled',
        payload,
      });

      expect(filteredTotalCount).toBe(12);
    });
  });

  describe('repositoryResultsSummaryPage/getRepositoryComponents/rejected action', () => {
    it('sets the error to the payload and the loading flag to false', () => {
      const state = Object.freeze({
        loadingRepositoryComponents: true,
        errorComponentsTable: null,
      });

      const { loadingRepositoryComponents, errorComponentsTable } = reducer(state, {
        type: 'repositoryResultsSummaryPage/getRepositoryComponents/rejected',
        payload: 'Loading error',
      });

      expect(loadingRepositoryComponents).toBe(false);
      expect(errorComponentsTable).toBe('Loading error');
    });
  });

  describe('repositoryResultsSummaryPage/toggleAggregate action', () => {
    it('sets aggregate to false if it is true and sets the page to 1', () => {
      const state = Object.freeze({
        componentsRequestBody: {
          aggregate: true,
        },
      });

      const {
        componentsRequestBody: { aggregate, page },
      } = reducer(state, {
        type: 'repositoryResultsSummaryPage/toggleAggregate',
      });

      expect(aggregate).toBeFalsy();
      expect(page).toBe(1);
    });

    it('sets aggregate to true if it is false and sets the page to 1', () => {
      const state = Object.freeze({
        componentsRequestBody: {
          aggregate: false,
        },
      });

      const {
        componentsRequestBody: { aggregate, page },
      } = reducer(state, {
        type: 'repositoryResultsSummaryPage/toggleAggregate',
      });

      expect(aggregate).toBeTruthy();
      expect(page).toBe(1);
    });
  });

  describe('repositoryResultsSummaryPage/setSorting action', () => {
    it('changes sorting direction to the opposite if we sort the same column', () => {
      const state = Object.freeze({
        componentsRequestBody: {
          sortFields: [
            {
              sortableField: 'POLICY_THREAT_LEVEL',
              asc: true,
              sortPriority: 1,
            },
          ],
        },
      });

      const expectedSortFields = [
        {
          sortableField: 'POLICY_THREAT_LEVEL',
          asc: false,
          sortPriority: 1,
        },
      ];

      const {
        componentsRequestBody: { sortFields },
      } = reducer(state, {
        type: 'repositoryResultsSummaryPage/setSorting',
        payload: 'POLICY_THREAT_LEVEL',
      });

      expect(sortFields).toEqual(expectedSortFields);
    });

    it('changes the sorting priority and direction of the first element', () => {
      const state = Object.freeze({
        componentsRequestBody: {
          sortFields: [
            {
              sortableField: 'QUARANTINE_TIME',
              asc: true,
              sortPriority: 1,
            },
            {
              sortableField: 'POLICY_THREAT_LEVEL',
              asc: false,
              sortPriority: 2,
            },
            {
              sortableField: 'POLICY_NAME',
              asc: true,
              sortPriority: 3,
            },
            {
              sortableField: 'COMPONENT_COORDINATES',
              asc: true,
              sortPriority: 4,
            },
          ],
        },
      });

      const expectedSortFields = [
        {
          sortableField: 'POLICY_THREAT_LEVEL',
          asc: true,
          sortPriority: 1,
        },
        {
          sortableField: 'QUARANTINE_TIME',
          asc: true,
          sortPriority: 2,
        },
        {
          sortableField: 'POLICY_NAME',
          asc: true,
          sortPriority: 3,
        },
        {
          sortableField: 'COMPONENT_COORDINATES',
          asc: true,
          sortPriority: 4,
        },
      ];

      const {
        componentsRequestBody: { sortFields },
      } = reducer(state, {
        type: 'repositoryResultsSummaryPage/setSorting',
        payload: 'POLICY_THREAT_LEVEL',
      });

      expect(sortFields).toEqual(expectedSortFields);
    });
  });

  describe('repositoryResultsSummaryPage/increasePage action', () => {
    it('increases page counter', () => {
      const state = Object.freeze({
        componentsRequestBody: {
          page: 1,
        },
      });

      const {
        componentsRequestBody: { page },
      } = reducer(state, {
        type: 'repositoryResultsSummaryPage/increasePage',
      });

      expect(page).toEqual(2);
    });
  });

  describe('repositoryResultsSummaryPage/decreasePage action', () => {
    it('decrease page counter', () => {
      const state = Object.freeze({
        componentsRequestBody: {
          page: 3,
        },
      });

      const {
        componentsRequestBody: { page },
      } = reducer(state, {
        type: 'repositoryResultsSummaryPage/decreasePage',
      });

      expect(page).toEqual(2);
    });
  });

  describe('repositoryResultsSummaryPage/setFilter action', () => {
    it('sets new search filter value if filter does not exist', () => {
      const state = Object.freeze({
        componentsRequestBody: {
          searchFilters: [],
        },
        searchFiltersValues: {
          POLICY_NAME: '',
          COMPONENT_COORDINATES: '',
        },
      });

      const payload = {
        filterName: 'COMPONENT_COORDINATES',
        filterValue: 'component name',
      };

      const expectedFilters = [
        {
          filterableField: 'COMPONENT_COORDINATES',
          value: 'component name',
        },
      ];

      const {
        componentsRequestBody: { searchFilters },
      } = reducer(state, {
        type: 'repositoryResultsSummaryPage/setFilter',
        payload,
      });

      expect(searchFilters).toEqual(expectedFilters);
    });
  });

  it('changes search filter value if filter already exist', () => {
    const state = Object.freeze({
      componentsRequestBody: {
        searchFilters: [
          {
            filterableField: 'POLICY_NAME',
            value: 'Old policy name value',
          },
        ],
      },
      searchFiltersValues: {
        POLICY_NAME: '',
        COMPONENT_COORDINATES: '',
      },
    });

    const payload = {
      filterName: 'POLICY_NAME',
      filterValue: 'New policy name value',
    };

    const expectedFilters = [
      {
        filterableField: 'POLICY_NAME',
        value: 'New policy name value',
      },
    ];

    const {
      componentsRequestBody: { searchFilters },
    } = reducer(state, {
      type: 'repositoryResultsSummaryPage/setFilter',
      payload,
    });

    expect(searchFilters).toEqual(expectedFilters);
  });

  it('removes search filter value if filter values is empty string', () => {
    const state = Object.freeze({
      componentsRequestBody: {
        searchFilters: [
          {
            filterableField: 'COMPONENT_COORDINATES',
            value: 'Component name',
          },
        ],
      },
      searchFiltersValues: {
        POLICY_NAME: '',
        COMPONENT_COORDINATES: '',
      },
    });

    const payload = {
      filterName: 'COMPONENT_COORDINATES',
      filterValue: '',
    };

    const expectedFilters = [];

    const {
      componentsRequestBody: { searchFilters },
    } = reducer(state, {
      type: 'repositoryResultsSummaryPage/setFilter',
      payload,
    });

    expect(searchFilters).toEqual(expectedFilters);
  });
});
