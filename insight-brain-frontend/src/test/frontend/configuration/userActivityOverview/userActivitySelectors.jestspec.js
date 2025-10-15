/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  selectUserActivitySlice,
  selectUserActivityData,
  selectTotalUsers,
  selectDateRange,
  selectPagination,
  selectSearchFilter,
  selectUserActivityLoading,
  selectUserActivityError,
  selectUserActivityExporting,
  selectUserActivityExportError,
  selectFilterDrawerOpen,
  selectSelectedFilters,
  selectAppliedFilters,
  selectFiltersAreDirty,
  selectSelectedAge,
  selectAppliedAge,
  selectUserActivityState,
  selectIsUserActivityTrackingEnabled,
} from 'MainRoot/configuration/userActivityOverview/userActivitySelectors';

describe('userActivitySelectors', () => {
  let mockState;

  beforeEach(() => {
    mockState = {
      userActivity: {
        users: [
          { username: 'user1', loginCount: 5, lastActive: '2023-01-01' },
          { username: 'user2', loginCount: 3, lastActive: '2023-01-02' },
        ],
        totalUsers: 2,
        dateRange: {
          startDate: '2023-01-01',
          endDate: '2023-01-31',
        },
        pagination: {
          limit: 25,
          offset: 0,
          hasMore: true,
        },
        loading: true,
        loadError: 'Load failed',
        exporting: false,
        exportError: null,
        searchFilter: 'test search',
        filterDrawerOpen: true,
        selectedFilters: { selectedAge: 90 },
        appliedFilters: { selectedAge: 30 },
        filtersAreDirty: true,
      },
      productFeatures: {
        productFeatures: {
          'user-activity-tracking': true,
        },
      },
    };
  });

  describe('selectUserActivitySlice', () => {
    it('should return the userActivity slice', () => {
      expect(selectUserActivitySlice(mockState)).toEqual(mockState.userActivity);
    });

    it('should return empty object when userActivity slice does not exist', () => {
      const stateWithoutSlice = {};
      expect(selectUserActivitySlice(stateWithoutSlice)).toEqual({});
    });
  });

  describe('selectUserActivityData', () => {
    it('should return users array', () => {
      expect(selectUserActivityData(mockState)).toEqual(mockState.userActivity.users);
    });

    it('should return empty array when users not present', () => {
      const stateWithoutUsers = { userActivity: {} };
      expect(selectUserActivityData(stateWithoutUsers)).toEqual([]);
    });
  });

  describe('selectTotalUsers', () => {
    it('should return totalUsers count', () => {
      expect(selectTotalUsers(mockState)).toBe(2);
    });

    it('should return 0 when totalUsers not present', () => {
      const stateWithoutTotal = { userActivity: {} };
      expect(selectTotalUsers(stateWithoutTotal)).toBe(0);
    });
  });

  describe('selectDateRange', () => {
    it('should return date range object', () => {
      expect(selectDateRange(mockState)).toEqual({
        startDate: '2023-01-01',
        endDate: '2023-01-31',
      });
    });

    it('should return default date range when not present', () => {
      const stateWithoutDateRange = { userActivity: {} };
      expect(selectDateRange(stateWithoutDateRange)).toEqual({
        startDate: null,
        endDate: null,
      });
    });
  });

  describe('selectPagination', () => {
    it('should return pagination object', () => {
      expect(selectPagination(mockState)).toEqual({
        limit: 25,
        offset: 0,
        hasMore: true,
      });
    });

    it('should return default pagination when not present', () => {
      const stateWithoutPagination = { userActivity: {} };
      expect(selectPagination(stateWithoutPagination)).toEqual({
        limit: 100,
        offset: 0,
        hasMore: false,
      });
    });
  });

  describe('selectSearchFilter', () => {
    it('should return search filter string', () => {
      expect(selectSearchFilter(mockState)).toBe('test search');
    });

    it('should return empty string when search filter not present', () => {
      const stateWithoutSearch = { userActivity: {} };
      expect(selectSearchFilter(stateWithoutSearch)).toBe('');
    });
  });

  describe('selectUserActivityLoading', () => {
    it('should return loading state', () => {
      expect(selectUserActivityLoading(mockState)).toBe(true);
    });

    it('should return false when loading not present', () => {
      const stateWithoutLoading = { userActivity: {} };
      expect(selectUserActivityLoading(stateWithoutLoading)).toBe(false);
    });
  });

  describe('selectUserActivityError', () => {
    it('should return load error', () => {
      expect(selectUserActivityError(mockState)).toBe('Load failed');
    });

    it('should return undefined when load error not present', () => {
      const stateWithoutError = { userActivity: {} };
      expect(selectUserActivityError(stateWithoutError)).toBeUndefined();
    });
  });

  describe('selectUserActivityExporting', () => {
    it('should return exporting state', () => {
      expect(selectUserActivityExporting(mockState)).toBe(false);
    });

    it('should return false when exporting not present', () => {
      const stateWithoutExporting = { userActivity: {} };
      expect(selectUserActivityExporting(stateWithoutExporting)).toBe(false);
    });
  });

  describe('selectUserActivityExportError', () => {
    it('should return export error', () => {
      expect(selectUserActivityExportError(mockState)).toBeNull();
    });

    it('should return undefined when export error not present', () => {
      const stateWithoutExportError = { userActivity: {} };
      expect(selectUserActivityExportError(stateWithoutExportError)).toBeUndefined();
    });
  });

  describe('selectFilterDrawerOpen', () => {
    it('should return filter drawer open state', () => {
      expect(selectFilterDrawerOpen(mockState)).toBe(true);
    });

    it('should return false when filter drawer open not present', () => {
      const stateWithoutDrawer = { userActivity: {} };
      expect(selectFilterDrawerOpen(stateWithoutDrawer)).toBe(false);
    });
  });

  describe('selectSelectedFilters', () => {
    it('should return selected filters object', () => {
      expect(selectSelectedFilters(mockState)).toEqual({ selectedAge: 90 });
    });

    it('should return default selected filters when not present', () => {
      const stateWithoutSelected = { userActivity: {} };
      expect(selectSelectedFilters(stateWithoutSelected)).toEqual({ selectedAge: 30 });
    });
  });

  describe('selectAppliedFilters', () => {
    it('should return applied filters object', () => {
      expect(selectAppliedFilters(mockState)).toEqual({ selectedAge: 30 });
    });

    it('should return default applied filters when not present', () => {
      const stateWithoutApplied = { userActivity: {} };
      expect(selectAppliedFilters(stateWithoutApplied)).toEqual({ selectedAge: 30 });
    });
  });

  describe('selectFiltersAreDirty', () => {
    it('should return filters are dirty state', () => {
      expect(selectFiltersAreDirty(mockState)).toBe(true);
    });

    it('should return false when filters are dirty not present', () => {
      const stateWithoutDirty = { userActivity: {} };
      expect(selectFiltersAreDirty(stateWithoutDirty)).toBe(false);
    });
  });

  describe('selectSelectedAge', () => {
    it('should return selected age from selected filters', () => {
      expect(selectSelectedAge(mockState)).toBe(90);
    });

    it('should return default age when selected age not present', () => {
      const stateWithoutSelectedAge = {
        userActivity: {
          selectedFilters: {},
        },
      };
      expect(selectSelectedAge(stateWithoutSelectedAge)).toBe(30);
    });
  });

  describe('selectAppliedAge', () => {
    it('should return applied age from applied filters', () => {
      expect(selectAppliedAge(mockState)).toBe(30);
    });

    it('should return default age when applied age not present', () => {
      const stateWithoutAppliedAge = {
        userActivity: {
          appliedFilters: {},
        },
      };
      expect(selectAppliedAge(stateWithoutAppliedAge)).toBe(30);
    });
  });

  describe('selectUserActivityState', () => {
    it('should return combined user activity state', () => {
      const result = selectUserActivityState(mockState);

      expect(result).toEqual({
        users: mockState.userActivity.users,
        totalUsers: mockState.userActivity.totalUsers,
        loading: mockState.userActivity.loading,
        loadError: mockState.userActivity.loadError,
        exporting: mockState.userActivity.exporting,
        exportError: mockState.userActivity.exportError,
        dateRange: mockState.userActivity.dateRange,
        pagination: mockState.userActivity.pagination,
      });
    });
  });

  describe('selectIsUserActivityTrackingEnabled', () => {
    it('should return true when user-activity-tracking is enabled', () => {
      expect(selectIsUserActivityTrackingEnabled(mockState)).toBe(true);
    });

    it('should return false when user-activity-tracking is disabled', () => {
      const stateWithDisabledFeature = {
        ...mockState,
        productFeatures: {
          productFeatures: {
            'user-activity-tracking': false,
          },
        },
      };
      expect(selectIsUserActivityTrackingEnabled(stateWithDisabledFeature)).toBe(false);
    });

    it('should return false when feature flag not present', () => {
      const stateWithoutFeature = {
        ...mockState,
        productFeatures: {
          productFeatures: {},
        },
      };
      expect(selectIsUserActivityTrackingEnabled(stateWithoutFeature)).toBe(false);
    });

    it('should return false when productFeatures slice not present', () => {
      const stateWithoutProductFeatures = {
        userActivity: mockState.userActivity,
      };
      expect(selectIsUserActivityTrackingEnabled(stateWithoutProductFeatures)).toBe(false);
    });
  });
});
