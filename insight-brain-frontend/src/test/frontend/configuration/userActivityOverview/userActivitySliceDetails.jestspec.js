/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { configureStore } from '@reduxjs/toolkit';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import userActivitySlice, {
  loadUserActivityDetail,
  loadFilterOptions,
  applyDetailsFilters,
  setDetailsCurrentUser,
  clearDetailsData,
  toggleDetailsFilterDrawer,
  setSelectedActivityTypes,
  setSelectedDomains,
  setSelectedErrorTypes,
  revertDetailsFilters,
} from 'MainRoot/configuration/userActivityOverview/userActivitySlice';

describe('userActivitySlice - Details', () => {
  let axiosMock, store;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    store = configureStore({
      reducer: {
        userActivity: userActivitySlice,
      },
      middleware: (getDefaultMiddleware) =>
        getDefaultMiddleware({
          serializableCheck: false,
        }),
    });
  });

  describe('initial state', () => {
    it('should have correct initial state for details page', () => {
      const state = store.getState().userActivity;

      expect(state.detailsCurrentUser).toBeNull();
      expect(state.detailsActivities).toEqual([]);
      expect(state.detailsTotalActivities).toBe(0);
      expect(state.detailsLoading).toBe(false);
      expect(state.detailsLoadError).toBeNull();
      expect(state.detailsFilterDrawerOpen).toBe(false);
      expect(state.detailsFiltersAreDirty).toBe(false);
      expect(state.detailsSelectedFilters).toEqual({
        selectedActivityTypes: [],
        selectedDomains: [],
        selectedErrorTypes: [],
      });
      expect(state.detailsAppliedFilters).toEqual({
        selectedActivityTypes: [],
        selectedDomains: [],
        selectedErrorTypes: [],
      });
      expect(state.filterOptions).toEqual({
        activityTypes: [],
        domains: [],
        errorTypes: [],
      });
      expect(state.filterOptionsLoading).toBe(false);
      expect(state.filterOptionsError).toBeNull();
    });
  });

  describe('synchronous actions', () => {
    it('should handle setDetailsCurrentUser', () => {
      store.dispatch(setDetailsCurrentUser('testuser'));

      const state = store.getState().userActivity;
      expect(state.detailsCurrentUser).toBe('testuser');
    });

    it('should handle clearDetailsData', () => {
      // Set some data first
      store.dispatch(setDetailsCurrentUser('testuser'));

      // Clear it
      store.dispatch(clearDetailsData());

      const state = store.getState().userActivity;
      expect(state.detailsCurrentUser).toBeNull();
      expect(state.detailsActivities).toEqual([]);
      expect(state.detailsTotalActivities).toBe(0);
      expect(state.detailsLoadError).toBeNull();
    });

    it('should handle toggleDetailsFilterDrawer', () => {
      store.dispatch(toggleDetailsFilterDrawer(true));

      let state = store.getState().userActivity;
      expect(state.detailsFilterDrawerOpen).toBe(true);

      store.dispatch(toggleDetailsFilterDrawer(false));
      state = store.getState().userActivity;
      expect(state.detailsFilterDrawerOpen).toBe(false);
    });

    describe('filter actions', () => {
      it('should handle setSelectedActivityTypes and update dirty state', () => {
        store.dispatch(setSelectedActivityTypes(['login', 'logout']));

        const state = store.getState().userActivity;
        expect(state.detailsSelectedFilters.selectedActivityTypes).toEqual(['login', 'logout']);
        expect(state.detailsFiltersAreDirty).toBe(true);
      });

      it('should handle setSelectedDomains and update dirty state', () => {
        store.dispatch(setSelectedDomains(['api', 'authentication']));

        const state = store.getState().userActivity;
        expect(state.detailsSelectedFilters.selectedDomains).toEqual(['api', 'authentication']);
        expect(state.detailsFiltersAreDirty).toBe(true);
      });

      it('should handle setSelectedErrorTypes and update dirty state', () => {
        store.dispatch(setSelectedErrorTypes(['Success', 'Error']));

        const state = store.getState().userActivity;
        expect(state.detailsSelectedFilters.selectedErrorTypes).toEqual(['Success', 'Error']);
        expect(state.detailsFiltersAreDirty).toBe(true);
      });

      it('should handle revertDetailsFilters', () => {
        // Set some selected filters
        store.dispatch(setSelectedActivityTypes(['login']));
        store.dispatch(setSelectedDomains(['api']));

        // Revert filters
        store.dispatch(revertDetailsFilters());

        const state = store.getState().userActivity;
        expect(state.detailsSelectedFilters).toEqual(state.detailsAppliedFilters);
        expect(state.detailsFiltersAreDirty).toBe(false);
      });
    });
  });

  describe('async actions', () => {
    describe('loadUserActivityDetail', () => {
      it('should handle successful load', async () => {
        const mockResponse = {
          data: {
            username: 'testuser',
            activities: [
              {
                timestamp: '2024-03-13T14:30:45.123Z',
                type: 'login',
                method: 'POST',
                uri: '/api/v2/auth/login',
                ipAddress: '192.168.1.100',
                userAgent: 'Mozilla/5.0',
                success: true,
              },
            ],
            totalActivities: 1,
          },
        };

        axiosMock.onGet().reply(200, mockResponse.data);

        await store.dispatch(
          loadUserActivityDetail({
            username: 'testuser',
            startUtcDate: '2024-03-10',
            endUtcDate: '2024-03-13',
          })
        );

        const state = store.getState().userActivity;
        expect(state.detailsLoading).toBe(false);
        expect(state.detailsLoadError).toBeNull();
        expect(state.detailsActivities).toEqual(mockResponse.data.activities);
        expect(state.detailsTotalActivities).toBe(1);
        expect(state.detailsCurrentUser).toBe('testuser');
      });

      it('should handle loading state', () => {
        axiosMock.onGet().reply(() => new Promise(() => {})); // Never resolves

        store.dispatch(
          loadUserActivityDetail({
            username: 'testuser',
            startUtcDate: '2024-03-10',
            endUtcDate: '2024-03-13',
          })
        );

        const state = store.getState().userActivity;
        expect(state.detailsLoading).toBe(true);
        expect(state.detailsLoadError).toBeNull();
      });
    });

    describe('loadFilterOptions', () => {
      it('should handle successful load', async () => {
        const mockResponse = {
          data: {
            activityTypes: ['login', 'EVALUATE_APPLICATION'],
            domains: ['api', 'ui'],
            errorTypes: ['Success', 'Error'],
          },
        };

        axiosMock.onGet().reply(200, mockResponse.data);

        await store.dispatch(loadFilterOptions());

        const state = store.getState().userActivity;
        expect(state.filterOptionsLoading).toBe(false);
        expect(state.filterOptionsError).toBeNull();
        expect(state.filterOptions).toEqual(mockResponse.data);
      });

      it('should handle loading state', () => {
        axiosMock.onGet().reply(() => new Promise(() => {})); // Never resolves

        store.dispatch(loadFilterOptions());

        const state = store.getState().userActivity;
        expect(state.filterOptionsLoading).toBe(true);
        expect(state.filterOptionsError).toBeNull();
      });
    });

    describe('applyDetailsFilters', () => {
      it('should update applied filters on success', async () => {
        const mockResponse = {
          data: {
            username: 'testuser',
            activities: [],
            totalActivities: 0,
          },
        };

        axiosMock.onGet().reply(200, mockResponse.data);

        // Set some selected filters first
        store.dispatch(setSelectedActivityTypes(['login']));
        store.dispatch(setSelectedDomains(['api']));

        await store.dispatch(applyDetailsFilters({ username: 'testuser' }));

        const state = store.getState().userActivity;
        expect(state.detailsAppliedFilters.selectedActivityTypes).toEqual(['login']);
        expect(state.detailsAppliedFilters.selectedDomains).toEqual(['api']);
        expect(state.detailsFiltersAreDirty).toBe(false);
      });
    });
  });
});
