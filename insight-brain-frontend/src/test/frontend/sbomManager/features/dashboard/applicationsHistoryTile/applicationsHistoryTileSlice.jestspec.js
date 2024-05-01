/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, {
  initialState,
} from 'MainRoot/sbomManager/features/dashboard/applicationsHistoryTile/applicationsHistoryTileSlice';
import { UI_ROUTER_ON_FINISH } from 'MainRoot/reduxUiRouter/routerActions';

describe('applicationsHistoryTile reducers have the correct state when the following reducer is dispatched', function () {
  describe('applicationsHistoryTile/loadApplicationsHistory', function () {
    it('/pending', () => {
      const state = {
        loading: true,
        loadError: null,
        totalScannedApplications: null,
        applicationsUpdatedLastYear: null,
        applicationsUpdatedLastMonth: null,
        applicationsUpdatedLastWeek: null,
      };

      const newState = reducer(state, {
        type: 'applicationsHistoryTile/loadApplicationsHistory/pending',
      });

      expect(newState.loading).toBe(true);
      expect(newState.loadError).toBe(null);
      expect(newState.totalScannedApplications).toBe(null);
      expect(newState.applicationsUpdatedLastYear).toBe(null);
      expect(newState.applicationsUpdatedLastMonth).toBe(null);
      expect(newState.applicationsUpdatedLastWeek).toBe(null);
    });

    it('/failed', () => {
      const state = {
        loading: true,
        loadError: null,
        totalScannedApplications: null,
        applicationsUpdatedLastYear: null,
        applicationsUpdatedLastMonth: null,
        applicationsUpdatedLastWeek: null,
      };

      const payload = {
        response: {
          data: 'payload error',
        },
      };

      const newState = reducer(state, {
        type: 'applicationsHistoryTile/loadApplicationsHistory/rejected',
        payload: payload,
      });

      expect(newState.loadError).toEqual({ response: { data: 'payload error' } });
      expect(newState.loading).toBe(false);
      expect(newState.totalScannedApplications).toBe(null);
      expect(newState.applicationsUpdatedLastYear).toBe(null);
      expect(newState.applicationsUpdatedLastMonth).toBe(null);
      expect(newState.applicationsUpdatedLastWeek).toBe(null);
    });

    it('/fulfilled', () => {
      const state = {
        loading: true,
        loadError: null,
        totalScannedApplications: null,
        applicationsUpdatedLastYear: null,
        applicationsUpdatedLastMonth: null,
        applicationsUpdatedLastWeek: null,
      };

      const newState = reducer(state, {
        type: 'applicationsHistoryTile/loadApplicationsHistory/fulfilled',
        payload: {
          totalScannedApplications: 1000,
          applicationsUpdatedLastYear: 2000,
          applicationsUpdatedLastMonth: 3000,
          applicationsUpdatedLastWeek: 4000,
        },
      });

      expect(newState.loading).toBe(false);
      expect(newState.loadError).toBe(null);
      expect(newState.totalScannedApplications).toBe(1000);
      expect(newState.applicationsUpdatedLastYear).toBe(2000);
      expect(newState.applicationsUpdatedLastMonth).toBe(3000);
      expect(newState.applicationsUpdatedLastWeek).toBe(4000);
    });
  });

  describe('UI_ROUTER_ON_FINISH', () => {
    it('clears state on onFinish', () => {
      const state = Object.freeze({
        loading: false,
        loadError: null,
        totalScannedApplications: 1000,
        applicationsUpdatedLastYear: 2000,
        applicationsUpdatedLastMonth: 3000,
        applicationsUpdatedLastWeek: 4000,
      });

      const newState = reducer(state, { type: UI_ROUTER_ON_FINISH });
      expect(newState).toEqual(initialState);
    });
  });
});
