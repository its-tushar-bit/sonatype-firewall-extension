/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/OrgsAndPolicies/sourceControlRateLimits/sourceControlRateLimitsSlice';
import { SOURCE_CONTROL_RATE_LIMITS_ORGANIZATION_MOCK_DATA } from 'TestRoot/OrgsAndPolicies/sourceControlRateLimits/sourceControlRateLimitsMockData';
import moment from 'moment/moment';

describe('sourceControlRateLimitsSliceReducer', () => {
  describe('initial state', () => {
    it('returns the initial state given an undefined state', function () {
      const state = undefined;

      const newState = reducer(state, {});

      expect(newState).toEqual({
        loading: false,
        loadError: null,
        lastUpdated: null,
        serverData: null,
        data: null,
        userRateLimitsExpanded: {},
        userDefiningOwnersExpanded: {},
        userAssociatedApplicationsExpanded: {},
        sortColumn: 'user',
        sortDirection: 'asc',
      });
    });
  });

  describe('unknown action', () => {
    it('returns original state', function () {
      const state = Object.freeze({ foo: 'bar' });
      const action = {
        type: 'UNKNOWN',
      };

      const newState = reducer(state, action);

      expect(newState).toBe(state);
    });
  });

  describe('sourceControlRateLimits/load/pending action', () => {
    it('sets `loading` to `true` and `loadError` to `null`', function () {
      const state = {};
      const action = {
        type: 'sourceControlRateLimits/load/pending',
      };

      const newState = reducer(state, action);

      expect(newState).toEqual({
        loading: true,
        loadError: null,
      });
    });
  });

  describe('sourceControlRateLimits/load/fulfilled action', () => {
    it('sets `loading` to false and `serverData` to the payload with extra calculated data', () => {
      const state = {};
      const payload = SOURCE_CONTROL_RATE_LIMITS_ORGANIZATION_MOCK_DATA;
      const now = Date.now();
      jest.useFakeTimers();
      jest.setSystemTime(now);

      const newState = reducer(state, {
        type: 'sourceControlRateLimits/load/fulfilled',
        payload,
      });

      expect(newState.loading).toBe(false);
      expect(newState.serverData).not.toBeNull();
      expect(newState.serverData.userRateLimits[0].averageRemainingPercent).toEqual(80);
      expect(newState.serverData.userRateLimits[0].rateLimits[0].remainingPercent).toEqual(60);
      expect(newState.serverData.userRateLimits[0].rateLimits[1].remainingPercent).toEqual(100);
      expect(newState.serverData.userRateLimits[0].rateLimits[0].timeUntilReset).toEqual(
        moment(newState.serverData.userRateLimits[0].rateLimits[0].resetEpochTime * 1000).from(now, true)
      );
      expect(newState.serverData.userRateLimits[0].rateLimits[1].timeUntilReset).toEqual(
        moment(newState.serverData.userRateLimits[0].rateLimits[1].resetEpochTime * 1000).from(now, true)
      );
      expect(newState.lastUpdated.toString()).toEqual(new Date(now).toString());
    });
  });

  describe('sourceControlRateLimits/load/rejected action', () => {
    it('sets `loading` to `true` and `loadError` to `null`', function () {
      const state = {};
      const payload = 'error';
      const action = {
        type: 'sourceControlRateLimits/load/rejected',
        payload,
      };

      const newState = reducer(state, action);

      expect(newState).toEqual({
        loading: false,
        loadError: 'error',
      });
    });
  });

  describe('sourceControlRateLimits/setSort action', () => {
    it('sets the `sortColumn` to the payload', function () {
      const state = {
        serverData: SOURCE_CONTROL_RATE_LIMITS_ORGANIZATION_MOCK_DATA,
      };
      const payload = 'user';
      const action = {
        type: 'sourceControlRateLimits/setSort',
        payload,
      };

      const newState = reducer(state, action);

      expect(newState.sortColumn).toBe('user');
    });

    it('sets the `sortDirection` to the `asc` if it is a different column', function () {
      const state = {
        serverData: SOURCE_CONTROL_RATE_LIMITS_ORGANIZATION_MOCK_DATA,
      };
      const payload = 'user';
      const action = {
        type: 'sourceControlRateLimits/setSort',
        payload,
      };

      const newState = reducer(state, action);

      expect(newState.sortDirection).toBe('asc');
    });

    it('sets the `sortDirection` to the `desc` if it is the same column and it was `asc`', function () {
      const state = {
        serverData: SOURCE_CONTROL_RATE_LIMITS_ORGANIZATION_MOCK_DATA,
        sortColumn: 'user',
        sortDirection: 'asc',
      };
      const payload = 'user';
      const action = {
        type: 'sourceControlRateLimits/setSort',
        payload,
      };

      const newState = reducer(state, action);

      expect(newState.sortDirection).toBe('desc');
    });

    it('sets the `sortDirection` to the `asc` if it is the same column and it was `desc`', function () {
      const state = {
        serverData: SOURCE_CONTROL_RATE_LIMITS_ORGANIZATION_MOCK_DATA,
        sortColumn: 'user',
        sortDirection: 'desc',
      };
      const payload = 'user';
      const action = {
        type: 'sourceControlRateLimits/setSort',
        payload,
      };

      const newState = reducer(state, action);

      expect(newState.sortDirection).toBe('asc');
    });

    it('sorts the `userRateLimits` by the user ascending', function () {
      const state = {
        serverData: SOURCE_CONTROL_RATE_LIMITS_ORGANIZATION_MOCK_DATA,
        sortColumn: 'user',
        sortDirection: 'desc',
      };
      const payload = 'user';
      const action = {
        type: 'sourceControlRateLimits/setSort',
        payload,
      };

      const newState = reducer(state, action);

      expect(newState.sortColumn).toBe('user');
      expect(newState.sortDirection).toBe('asc');
      expect(newState.data.userRateLimits[0].user).toBe('userA');
      expect(newState.data.userRateLimits[1].user).toBe('userB');
    });

    it('sorts the `userRateLimits` by the user descending', function () {
      const state = {
        serverData: SOURCE_CONTROL_RATE_LIMITS_ORGANIZATION_MOCK_DATA,
        sortColumn: 'user',
        sortDirection: 'asc',
      };
      const payload = 'user';
      const action = {
        type: 'sourceControlRateLimits/setSort',
        payload,
      };

      const newState = reducer(state, action);

      expect(newState.sortColumn).toBe('user');
      expect(newState.sortDirection).toBe('desc');
      expect(newState.data.userRateLimits[0].user).toBe('userB');
      expect(newState.data.userRateLimits[1].user).toBe('userA');
    });

    it('sorts the `userRateLimits` by the number of definingOwners ascending', function () {
      const state = {
        serverData: SOURCE_CONTROL_RATE_LIMITS_ORGANIZATION_MOCK_DATA,
        sortColumn: 'definingOwners',
        sortDirection: 'desc',
      };
      const payload = 'definingOwners';
      const action = {
        type: 'sourceControlRateLimits/setSort',
        payload,
      };

      const newState = reducer(state, action);

      expect(newState.sortColumn).toBe('definingOwners');
      expect(newState.sortDirection).toBe('asc');
      expect(newState.data.userRateLimits[0].definingOwners.length).toBe(1);
      expect(newState.data.userRateLimits[1].definingOwners.length).toBe(2);
    });

    it('sorts the `userRateLimits` by the number of definingOwners descending', function () {
      const state = {
        serverData: SOURCE_CONTROL_RATE_LIMITS_ORGANIZATION_MOCK_DATA,
        sortColumn: 'definingOwners',
        sortDirection: 'asc',
      };
      const payload = 'definingOwners';
      const action = {
        type: 'sourceControlRateLimits/setSort',
        payload,
      };

      const newState = reducer(state, action);

      expect(newState.sortColumn).toBe('definingOwners');
      expect(newState.sortDirection).toBe('desc');
      expect(newState.data.userRateLimits[0].definingOwners.length).toBe(2);
      expect(newState.data.userRateLimits[1].definingOwners.length).toBe(1);
    });

    it('sorts the `userRateLimits` by the number of associatedApplications ascending', function () {
      const state = {
        serverData: SOURCE_CONTROL_RATE_LIMITS_ORGANIZATION_MOCK_DATA,
        sortColumn: 'associatedApplications',
        sortDirection: 'desc',
      };
      const payload = 'associatedApplications';
      const action = {
        type: 'sourceControlRateLimits/setSort',
        payload,
      };

      const newState = reducer(state, action);

      expect(newState.sortColumn).toBe('associatedApplications');
      expect(newState.sortDirection).toBe('asc');
      expect(newState.data.userRateLimits[0].associatedApplications.length).toBe(1);
      expect(newState.data.userRateLimits[1].associatedApplications.length).toBe(2);
    });

    it('sorts the `userRateLimits` by the number of associatedApplications descending', function () {
      const state = {
        serverData: SOURCE_CONTROL_RATE_LIMITS_ORGANIZATION_MOCK_DATA,
        sortColumn: 'associatedApplications',
        sortDirection: 'asc',
      };
      const payload = 'associatedApplications';
      const action = {
        type: 'sourceControlRateLimits/setSort',
        payload,
      };

      const newState = reducer(state, action);

      expect(newState.sortColumn).toBe('associatedApplications');
      expect(newState.sortDirection).toBe('desc');
      expect(newState.data.userRateLimits[0].associatedApplications.length).toBe(2);
      expect(newState.data.userRateLimits[1].associatedApplications.length).toBe(1);
    });

    it('sorts the `userRateLimits` by the `averageRemainingPercent` ascending', function () {
      const state = {
        serverData: SOURCE_CONTROL_RATE_LIMITS_ORGANIZATION_MOCK_DATA,
        sortColumn: 'averageRemainingPercent',
        sortDirection: 'desc',
      };
      const payload = 'averageRemainingPercent';
      const action = {
        type: 'sourceControlRateLimits/setSort',
        payload,
      };

      const newState = reducer(state, action);

      expect(newState.sortColumn).toBe('averageRemainingPercent');
      expect(newState.sortDirection).toBe('asc');
      expect(newState.data.userRateLimits[0].averageRemainingPercent).toBe(70);
      expect(newState.data.userRateLimits[1].averageRemainingPercent).toBe(80);
    });

    it('sorts the `userRateLimits` by the `averageRemainingPercent` descending', function () {
      const state = {
        serverData: SOURCE_CONTROL_RATE_LIMITS_ORGANIZATION_MOCK_DATA,
        sortColumn: 'averageRemainingPercent',
        sortDirection: 'asc',
      };
      const payload = 'averageRemainingPercent';
      const action = {
        type: 'sourceControlRateLimits/setSort',
        payload,
      };

      const newState = reducer(state, action);

      expect(newState.sortColumn).toBe('averageRemainingPercent');
      expect(newState.sortDirection).toBe('desc');
      expect(newState.data.userRateLimits[0].averageRemainingPercent).toBe(80);
      expect(newState.data.userRateLimits[1].averageRemainingPercent).toBe(70);
    });
  });

  describe('sourceControlRateLimits/toggleUserRateLimitsExpanded action', () => {
    it('toggles `userRateLimitsExpanded` for the given user to `true` if an entry does not exist', function () {
      const state = { userRateLimitsExpanded: {} };
      const payload = 'user';
      const action = {
        type: 'sourceControlRateLimits/toggleUserRateLimitsExpanded',
        payload,
      };

      const newState = reducer(state, action);

      expect(newState).toEqual({
        userRateLimitsExpanded: {
          user: true,
        },
      });
    });

    it('toggles `userRateLimitsExpanded` for the given user to `true` if the entry is `false`', function () {
      const state = {
        userRateLimitsExpanded: {
          user: false,
        },
      };
      const payload = 'user';
      const action = {
        type: 'sourceControlRateLimits/toggleUserRateLimitsExpanded',
        payload,
      };

      const newState = reducer(state, action);

      expect(newState).toEqual({
        userRateLimitsExpanded: {
          user: true,
        },
      });
    });

    it('toggles `userRateLimitsExpanded` for the given user to `false` if the entry is `true`', function () {
      const state = {
        userRateLimitsExpanded: {
          user: true,
        },
      };
      const payload = 'user';
      const action = {
        type: 'sourceControlRateLimits/toggleUserRateLimitsExpanded',
        payload,
      };

      const newState = reducer(state, action);

      expect(newState).toEqual({
        userRateLimitsExpanded: {
          user: false,
        },
      });
    });
  });

  afterEach(function () {
    jest.useRealTimers();
  });
});
