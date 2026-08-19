/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectAllComponentsList,
  selectDisplayedComponentList,
  selectSelectedComponent,
  selectAggregatedComponentsList,
  selectSelectedComponentInAggregatedList,
  selectSelectedComponentIndexInAggregatedList,
  selectHasUnscannedComponents,
  selectLoadError,
  selectIsLoading,
  selectReportParameters,
  selectIsDependenciesLoading,
  selectDependencyTreeData,
  selectDependencyTreeIsAvailable,
  selectApplicationReportSlice,
  selectDependencyTreeRouterParams,
  selectDependencyTreeUnavailableMessage,
  selectIsPolicyTypeFilterEnabled,
  selectActiveProxyFailedViolationCount,
} from 'MainRoot/applicationReport/applicationReportSelectors';
import { dependencyTreeData } from '../dependencyTree/dependencyTreeMockData';

describe('applicationReportSelectors', () => {
  const mockState = {
    router: {
      currentParams: {
        hash: 'some-component-hash',
        violationId: 'some-policy-violation-id',
      },
    },
    applicationReport: {
      dependencyTree: dependencyTreeData,
      selectedReport: {
        allEntries: [
          {
            hash: 'a-component-hash',
          },
          {
            derivedComponentName: 'My Component',
            hash: 'some-component-hash',
            componentIdentifier: { format: 'maven' },
            derivedDependencyType: 'transitive',
          },
          {
            hash: 'another-component-hash',
          },
          {
            hash: 'and-another-component-hash',
          },
          {
            hash: 'and-another-component-hash-bites-the-dust',
          },
        ],
        displayedEntries: [
          {
            hash: 'a-component-hash',
          },
          {
            derivedComponentName: 'My Component',
            hash: 'some-component-hash',
            componentIdentifier: { format: 'maven' },
            derivedDependencyType: 'transitive',
          },
          {
            hash: 'another-component-hash',
          },
        ],
        aggregatedEntries: [
          {
            hash: 'a-component-hash',
          },
          {
            derivedComponentName: 'My Component',
            hash: 'some-component-hash',
            componentIdentifier: { format: 'maven' },
            derivedDependencyType: 'transitive',
            policyViolationId: 'some-policy-violation-id',
          },
          {
            hash: 'another-component-hash',
          },
        ],
      },
      reportHasUnscannedComponents: false,
      loadError: false,
      pendingLoads: new Set(['test']),
    },
  };

  describe('selectAllComponentsList', () => {
    it('returns all components loaded in the selected report', () => {
      const expected = [
        {
          hash: 'a-component-hash',
        },
        {
          derivedComponentName: 'My Component',
          hash: 'some-component-hash',
          componentIdentifier: { format: 'maven' },
          derivedDependencyType: 'transitive',
        },
        {
          hash: 'another-component-hash',
        },
        {
          hash: 'and-another-component-hash',
        },
        {
          hash: 'and-another-component-hash-bites-the-dust',
        },
      ];
      const actual = selectAllComponentsList(mockState);
      expect(actual).toEqual(expected);
    });
  });

  describe('selectDisplayedComponentList', () => {
    it('returns only the components loaded that are displayed in the selected report', () => {
      const expected = [
        {
          hash: 'a-component-hash',
        },
        {
          derivedComponentName: 'My Component',
          hash: 'some-component-hash',
          componentIdentifier: { format: 'maven' },
          derivedDependencyType: 'transitive',
        },
        {
          hash: 'another-component-hash',
        },
      ];
      const actual = selectDisplayedComponentList(mockState);
      expect(actual).toEqual(expected);
    });
  });

  describe('selectSelectedComponent', () => {
    it('derives selectedComponent from router params hash and the selectedReport displayed component list', () => {
      const expected = {
        derivedComponentName: 'My Component',
        hash: 'some-component-hash',
        componentIdentifier: { format: 'maven' },
        derivedDependencyType: 'transitive',
      };
      const actual = selectSelectedComponent(mockState);
      expect(actual).toEqual(expected);
    });

    it('returns undefined if the router hash does not have a matching component in the selectedReport displayed component list', () => {
      const state = {
        ...mockState,
        router: {
          currentParams: {
            hash: 'i-just-typed-this-in-i-wonder-what-happens',
          },
        },
      };
      const actual = selectSelectedComponent(state);
      expect(actual).not.toBeDefined();
    });

    it('returns undefined if the selectedReport does not have a displayed component list', () => {
      const state = {
        ...mockState,
        applicationReport: {
          ...mockState.applicationReport,
          selectedReport: {},
        },
      };
      const actual = selectSelectedComponent(state);
      expect(actual).not.toBeDefined();
    });
  });

  describe('selectAggregatedComponentsList', () => {
    it('selects the aggregatedEntries prop from the state', () => {
      const expected = [
        {
          hash: 'a-component-hash',
        },
        {
          derivedComponentName: 'My Component',
          hash: 'some-component-hash',
          componentIdentifier: { format: 'maven' },
          derivedDependencyType: 'transitive',
          policyViolationId: 'some-policy-violation-id',
        },
        {
          hash: 'another-component-hash',
        },
      ];
      const actual = selectAggregatedComponentsList(mockState);
      expect(actual).toEqual(expected);
    });
  });

  describe('selectSelectedComponentInAggregatedList', () => {
    it('derives selectedComponent from router params hash and the selectedReport aggregated component list', () => {
      const expected = {
        derivedComponentName: 'My Component',
        hash: 'some-component-hash',
        componentIdentifier: { format: 'maven' },
        derivedDependencyType: 'transitive',
        policyViolationId: 'some-policy-violation-id',
      };
      const actual = selectSelectedComponentInAggregatedList(mockState);
      expect(actual).toEqual(expected);
    });

    it('returns undefined if the selected report does not have aggregated components', () => {
      const state = {
        ...mockState,
        applicationReport: {
          ...mockState.applicationReport,
          selectedReport: {},
        },
      };
      const actual = selectSelectedComponentInAggregatedList(state);
      expect(actual).not.toBeDefined();
    });

    it('returns undefined if the router hash does not have a matching component in the selectedReport aggregated components list', () => {
      const state = {
        ...mockState,
        router: {
          currentParams: {
            hash: 'i-just-typed-this-in-i-wonder-what-happens',
          },
        },
      };
      const actual = selectSelectedComponentInAggregatedList(state);
      expect(actual).not.toBeDefined();
    });
  });

  describe('selectSelectedComponentIndexInAggregatedList', () => {
    it('derives the componentIndex from selectedComponent and the aggregated components list in the selectedReport', () => {
      const expected = 1;
      const actual = selectSelectedComponentIndexInAggregatedList(mockState);
      expect(actual).toEqual(expected);
    });
    it('returns -1 if the selectedComponent is not present in the aggregated components list in the selectedReport', () => {
      const state = {
        ...mockState,
        router: {
          currentParams: {
            hash: 'i-just-typed-this-in-i-wonder-what-happens',
          },
        },
      };
      const expected = -1;
      const actual = selectSelectedComponentIndexInAggregatedList(state);
      expect(actual).toEqual(expected);
    });
  });

  describe('selectHasUnscannedComponents', () => {
    it('selects reportHasUnscannedComponents from the state', () => {
      const actual = selectHasUnscannedComponents(mockState);
      expect(actual).toEqual(false);
    });
  });

  describe('selectLoadError', () => {
    it('selects loadError from the state', () => {
      const actual = selectLoadError(mockState);
      expect(actual).toEqual(false);
    });
  });

  describe('selectIsLoading', () => {
    it('selects isLoading getting the size of the pending loads in the state', () => {
      const actual = selectIsLoading(mockState);
      expect(actual).toEqual(true);
    });
  });

  describe('selectIsDependenciesLoading', () => {
    it('selects isLoading for dependencies.json', () => {
      const actual = selectIsDependenciesLoading(mockState);
      expect(actual).toEqual(false);
    });
  });

  it('selectDependencyTreeData', () => {
    const expected = dependencyTreeData;
    const actual = selectDependencyTreeData(mockState);

    expect(actual).toEqual(expected);
  });

  describe('selectDependencyTreeIsAvailable', () => {
    it('returns false if dependencyTree is null', () => {
      const state = {
        ...mockState,
        applicationReport: {
          ...mockState.applicationReport,
          dependencyTree: null,
        },
      };
      const actual = selectDependencyTreeIsAvailable(state);

      expect(actual).toEqual(false);
    });

    it('returns false if dependencyTree is empty', () => {
      const state = {
        ...mockState,
        applicationReport: {
          ...mockState.applicationReport,
          dependencyTree: [],
        },
      };
      const actual = selectDependencyTreeIsAvailable(state);

      expect(actual).toEqual(false);
    });
  });

  describe('selectDependencyTreeUnavailableMessage', () => {
    it('returns Please re-scan the application if dependencyTree is null', () => {
      const state = {
        ...mockState,
        applicationReport: {
          ...mockState.applicationReport,
          dependencyTree: null,
        },
      };
      const actual = selectDependencyTreeUnavailableMessage(state);

      expect(actual).toEqual('Please re-scan the application');
    });

    it('returns Dependency tree not available if dependencyTree is empty', () => {
      const state = {
        ...mockState,
        applicationReport: {
          ...mockState.applicationReport,
          dependencyTree: [],
        },
      };
      const actual = selectDependencyTreeUnavailableMessage(state);

      expect(actual).toEqual('Dependency tree not available');
    });
    it('returns empty if dependencyTree is not empty', () => {
      const state = {
        ...mockState,
        applicationReport: {
          ...mockState.applicationReport,
          dependencyTree: { tree: 'sometree' },
        },
      };
      const actual = selectDependencyTreeUnavailableMessage(state);

      expect(actual).toEqual('');
    });
  });

  describe('selectReportParameters', () => {
    it('selectes the `reportParameters` prop from the state', () => {
      const state = {
        ...mockState,
        applicationReport: {
          ...mockState.applicationReport,
          reportParameters: {
            appId: 'appId',
            scanId: 'scanId',
          },
        },
      };
      const actual = selectReportParameters(state);
      expect(actual).toEqual({
        appId: 'appId',
        scanId: 'scanId',
      });
    });
  });

  describe('selectDependencyTreeRouterParams', () => {
    it('is composed from the following selector', () => {
      expect(selectDependencyTreeRouterParams.dependencies).toEqual([selectApplicationReportSlice]);
    });

    it('selects dependencyTreePageRouterParams', () => {
      const actualSelection = selectDependencyTreeRouterParams.resultFunc({ dependencyTreePageRouterParams: null });

      expect(actualSelection).toBeNull();
    });
  });

  describe('selectIsPolicyTypeFilterEnabled', () => {
    it('selects policyTypeFilterEnabled', () => {
      const state = {
        ...mockState,
        applicationReport: {
          ...mockState.applicationReport,
          policyTypeFilterEnabled: true,
        },
      };

      expect(selectIsPolicyTypeFilterEnabled(state)).toBe(true);
    });
  });

  describe('selectActiveProxyFailedViolationCount', () => {
    it('returns 0 if activeProxyFailedViolationCount is not present', () => {
      const state = {
        applicationReport: {
          selectedReport: {},
        },
      };
      expect(selectActiveProxyFailedViolationCount(state)).toBe(0);
    });

    it('returns the value of activeProxyFailedViolationCount if present', () => {
      const state = {
        applicationReport: {
          selectedReport: { activeProxyFailedViolationCount: 5 },
        },
      };
      expect(selectActiveProxyFailedViolationCount(state)).toBe(5);
    });
  });
});
