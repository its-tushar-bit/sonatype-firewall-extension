/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import reducer, {
  initialState,
  loadUserActivity,
  exportUserActivityData,
  applyFilters,
  searchUsers,
  clearErrors,
  toggleFilterDrawer,
  setSelectedAge,
  revertFilters,
  setSearchFilter,
  USER_ACTIVITY_PAGE_SIZE,
} from 'MainRoot/configuration/userActivityOverview/userActivitySlice';
import { configureStore } from '@reduxjs/toolkit';

describe('userActivitySlice', () => {
  let axiosMock, store;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    store = configureStore({
      reducer: { userActivity: reducer },
      middleware: (getDefaultMiddleware) =>
        getDefaultMiddleware({
          serializableCheck: false,
        }),
    });
  });

  describe('initialState', () => {
    it('should have correct initial state', () => {
      expect(initialState).toEqual({
        users: [],
        totalUsers: 0,
        dateRange: {
          startDate: null,
          endDate: null,
        },
        pagination: {
          limit: 100,
          offset: 0,
          hasMore: false,
        },
        loading: false,
        loadError: null,
        exporting: false,
        exportError: null,
        searchFilter: '',
        filterDrawerOpen: false,
        selectedFilters: { selectedAge: 30 },
        appliedFilters: { selectedAge: 30 },
        filtersAreDirty: false,
        // Details page state
        detailsCurrentUser: null,
        detailsActivities: [],
        detailsTotalActivities: 0,
        detailsLoading: false,
        detailsLoadError: null,
        detailsExporting: false,
        detailsExportError: null,
        detailsPagination: { limit: USER_ACTIVITY_PAGE_SIZE, offset: 0, hasMore: false },
        detailsFilterDrawerOpen: false,
        detailsSelectedFilters: {
          selectedActivityTypes: [],
          selectedDomains: [],
          selectedErrorTypes: [],
        },
        detailsAppliedFilters: {
          selectedActivityTypes: [],
          selectedDomains: [],
          selectedErrorTypes: [],
        },
        detailsFiltersAreDirty: false,
        // Filter options
        filterOptions: {
          activityTypes: [],
          domains: [],
          errorTypes: [],
        },
        filterOptionsLoading: false,
        filterOptionsError: null,
      });
    });
  });

  describe('reducers', () => {
    describe('clearErrors', () => {
      it('should clear load and export errors', () => {
        const state = {
          ...initialState,
          loadError: 'Load error',
          exportError: 'Export error',
        };

        const newState = reducer(state, clearErrors());

        expect(newState.loadError).toBeNull();
        expect(newState.exportError).toBeNull();
      });
    });

    describe('setSearchFilter', () => {
      it('should update search filter', () => {
        const newState = reducer(initialState, setSearchFilter('test search'));

        expect(newState.searchFilter).toBe('test search');
      });
    });

    describe('toggleFilterDrawer', () => {
      it('should toggle filter drawer when no payload', () => {
        const newState = reducer(initialState, toggleFilterDrawer());

        expect(newState.filterDrawerOpen).toBe(true);
      });

      it('should set filter drawer to specific value when payload provided', () => {
        const newState = reducer(initialState, toggleFilterDrawer(true));

        expect(newState.filterDrawerOpen).toBe(true);
      });
    });

    describe('setSelectedAge', () => {
      it('should update selectedAge and mark filters as dirty', () => {
        const newState = reducer(initialState, setSelectedAge(90));

        expect(newState.selectedFilters.selectedAge).toBe(90);
        expect(newState.filtersAreDirty).toBe(true);
      });

      it('should not mark filters as dirty if same as applied', () => {
        const state = {
          ...initialState,
          appliedFilters: { selectedAge: 90 },
        };

        const newState = reducer(state, setSelectedAge(90));

        expect(newState.selectedFilters.selectedAge).toBe(90);
        expect(newState.filtersAreDirty).toBe(false);
      });
    });

    describe('revertFilters', () => {
      it('should copy applied filters to selected and mark as clean', () => {
        const state = {
          ...initialState,
          selectedFilters: { selectedAge: 90 },
          appliedFilters: { selectedAge: 30 },
          filtersAreDirty: true,
        };

        const newState = reducer(state, revertFilters());

        expect(newState.selectedFilters).toEqual({ selectedAge: 30 });
        expect(newState.filtersAreDirty).toBe(false);
      });
    });
  });

  describe('loadUserActivity thunk', () => {
    const mockUserData = {
      users: [
        { username: 'testuser1', loginCount: 5, lastActive: '2023-01-01' },
        { username: 'testuser2', loginCount: 3, lastActive: '2023-01-02' },
      ],
      totalUsers: 2,
    };

    it('should handle successful user activity loading', async () => {
      axiosMock.onGet().reply(200, mockUserData);

      await store.dispatch(loadUserActivity({}));

      const state = store.getState().userActivity;
      expect(state.loading).toBe(false);
      expect(state.loadError).toBeNull();
      expect(state.users).toEqual(mockUserData.users);
      expect(state.totalUsers).toBe(mockUserData.totalUsers);
    });

    it('should handle failed user activity loading', async () => {
      axiosMock.onGet().reply(500, 'Server error');

      await store.dispatch(loadUserActivity({}));

      const state = store.getState().userActivity;
      expect(state.loading).toBe(false);
      expect(state.loadError).toBeDefined();
      expect(state.users).toEqual([]);
    });

    it('should set loading state while request is pending', () => {
      axiosMock.onGet().reply(() => new Promise(() => {})); // Never resolves

      store.dispatch(loadUserActivity({}));

      const state = store.getState().userActivity;
      expect(state.loading).toBe(true);
    });

    it('should include query parameters in request', async () => {
      axiosMock.onGet().reply(200, mockUserData);

      const params = {
        startUtcDate: '2023-01-01',
        endUtcDate: '2023-01-31',
        username: 'testuser',
        limit: 25,
        offset: 5, // Use non-zero offset to ensure it appears in URL
      };

      await store.dispatch(loadUserActivity(params));

      const actualUrl = axiosMock.history.get[0].url;
      const expectedParams = new URLSearchParams({
        startUtcDate: '2023-01-01',
        endUtcDate: '2023-01-31',
        username: 'testuser',
        limit: '25',
        offset: '5',
      });

      expect(actualUrl).toContain('/api/v2/userActivity');
      expect(actualUrl).toContain(expectedParams.toString());
    });
  });

  describe('exportUserActivityData thunk', () => {
    beforeEach(() => {
      // Mock window.URL and document methods for blob download
      global.URL.createObjectURL = jest.fn(() => 'mock-blob-url');
      global.URL.revokeObjectURL = jest.fn();
      document.createElement = jest.fn(() => ({
        setAttribute: jest.fn(),
        click: jest.fn(),
        remove: jest.fn(),
      }));
      document.body.appendChild = jest.fn();
    });

    it('should handle successful export', async () => {
      const mockBlob = new Blob(['csv,data'], { type: 'text/csv' });
      axiosMock.onGet().reply(200, mockBlob);

      await store.dispatch(exportUserActivityData({ format: 'csv' }));

      const state = store.getState().userActivity;
      expect(state.exporting).toBe(false);
      expect(state.exportError).toBeNull();
    });

    it('should handle failed export', async () => {
      axiosMock.onGet().reply(500, 'Export failed');

      await store.dispatch(exportUserActivityData({ format: 'csv' }));

      const state = store.getState().userActivity;
      expect(state.exporting).toBe(false);
      expect(state.exportError).toBeDefined();
    });

    it('should set exporting state while request is pending', () => {
      axiosMock.onGet().reply(() => new Promise(() => {}));

      store.dispatch(exportUserActivityData({ format: 'csv' }));

      const state = store.getState().userActivity;
      expect(state.exporting).toBe(true);
    });
  });

  describe('applyFilters thunk', () => {
    it('should dispatch loadUserActivity with date range from selectedFilters', async () => {
      const mockUserData = { users: [], totalUsers: 0 };
      axiosMock.onGet().reply(200, mockUserData);

      const state = {
        userActivity: {
          ...initialState,
          selectedFilters: { selectedAge: 90 },
        },
      };

      const store = configureStore({
        reducer: { userActivity: reducer },
        preloadedState: state,
        middleware: (getDefaultMiddleware) =>
          getDefaultMiddleware({
            serializableCheck: false,
          }),
      });

      await store.dispatch(applyFilters({ username: 'test', limit: 25, offset: 0 }));

      const finalState = store.getState().userActivity;
      expect(finalState.appliedFilters).toEqual({ selectedAge: 90 });
      expect(finalState.filtersAreDirty).toBe(false);
    });
  });

  describe('searchUsers thunk', () => {
    it('should immediately update search filter', () => {
      store.dispatch(searchUsers('test'));

      // Should immediately update search filter
      const state = store.getState().userActivity;
      expect(state.searchFilter).toBe('test');
    });

    it('should update search filter regardless of length', () => {
      // Test short search
      store.dispatch(searchUsers('te'));
      let state = store.getState().userActivity;
      expect(state.searchFilter).toBe('te');

      // Test longer search
      store.dispatch(searchUsers('test search'));
      state = store.getState().userActivity;
      expect(state.searchFilter).toBe('test search');

      // Test empty search
      store.dispatch(searchUsers(''));
      state = store.getState().userActivity;
      expect(state.searchFilter).toBe('');
    });
  });
});
