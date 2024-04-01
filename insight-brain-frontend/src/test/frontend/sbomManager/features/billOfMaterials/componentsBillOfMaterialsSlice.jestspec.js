/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reducer from 'MainRoot/sbomManager/features/componentsTile/componentsBillOfMaterialsSlice';

const componentResults = [
  {
    hash: '12345',
    packageUrl: 'pkg:a',
    componentIdentifier: {
      format: 'maven',
      coordinates: {
        artifactId: 'foo',
        extension: 'foo',
        groupId: 'foo',
        version: '1',
      },
    },
    displayName: 'a1',
    licenses: [
      {
        licenseId: 'Apache-2.0',
        licenseName: 'Apache-2.0',
      },
    ],
    vulnerabilitySeverityNoneCount: 0,
    vulnerabilitySeverityLowCount: 1,
    vulnerabilitySeverityMediumCount: 2,
    vulnerabilitySeverityHighCount: 3,
    vulnerabilitySeverityCriticalCount: 4,
  },
  {
    hash: '67890',
    packageUrl: 'pkg:z',
    componentIdentifier: {
      format: 'maven',
      coordinates: {
        artifactId: 'bar',
        extension: 'bar',
        groupId: 'bar',
        version: '2',
      },
    },
    displayName: 'z2',
    licenses: [
      {
        licenseId: 'Apache-2.0',
        licenseName: 'Apache-2.0',
      },
    ],
    vulnerabilitySeverityNoneCount: 0,
    vulnerabilitySeverityLowCount: 5,
    vulnerabilitySeverityMediumCount: 6,
    vulnerabilitySeverityHighCount: 7,
    vulnerabilitySeverityCriticalCount: 8,
  },
];

describe('componentsBillOfMaterialsTile reducers have the correct state when the following reducer is dispatched', function () {
  describe('componentsBillOfMaterialsTile/loadSbomTableData', function () {
    it('/pending', () => {
      const state = {
        results: null,
        loading: false,
      };

      const newState = reducer(state, {
        type: 'componentsBillOfMaterialsTile/loadSbomTableData/pending',
      });

      expect(newState.results).toBe(null);
      expect(newState.loading).toBe(true);
    });

    it('/failed', () => {
      const state = {
        error: null,
        loading: false,
      };

      const payload = {
        response: {
          data: 'payload error',
        },
      };

      const newState = reducer(state, {
        type: 'componentsBillOfMaterialsTile/loadSbomTableData/rejected',
        payload: payload,
      });

      expect(newState.error).toBe('payload error');
      expect(newState.loading).toBe(false);
    });

    it('/failed (network error)', () => {
      const state = {
        error: null,
        loading: false,
      };

      const payload = {
        message: 'network error',
      };

      const newState = reducer(state, {
        type: 'componentsBillOfMaterialsTile/loadSbomTableData/rejected',
        payload: payload,
      });

      expect(newState.error).toBe('network error');
      expect(newState.loading).toBe(false);
    });

    it('/fulfilled', () => {
      const state = {
        results: null,
        numResults: null,
        loading: false,
        pageCount: 0,
        applicationId: null,
      };

      const payload = [componentResults[0]];

      const newState = reducer(state, {
        type: 'componentsBillOfMaterialsTile/loadSbomTableData/fulfilled',
        payload: payload,
      });

      expect(newState.results).toHaveLength(1);
      expect(newState.results[0].displayName).toBe('a1');
      expect(newState.loading).toBe(false);
    });
  });

  it('componentsBillOfMaterialsTile/toggleSortDir', () => {
    const state = {
      sortDir: 'asc',
      results: [componentResults[0]],
    };

    const newState = reducer(state, {
      type: 'componentsBillOfMaterialsTile/toggleSortDir',
    });

    expect(newState.sortDir).toBe('desc');
  });

  it('componentsBillOfMaterialsTile/sortResults', () => {
    const state = {
      sortDir: 'desc',
      results: componentResults,
    };

    const newState = reducer(state, {
      type: 'componentsBillOfMaterialsTile/sortResults',
    });

    expect(newState.results).toHaveLength(2);
    expect(newState.results[0].displayName).toBe('z2');
  });
});
