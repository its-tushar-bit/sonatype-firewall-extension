/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, {
  initialState,
  actions,
} from 'MainRoot/firewall/containerImageWaiver/addContainerImageWaiverPageSlice';
import { FIREWALL_FIREWALLPAGE_CONTAINERS } from 'MainRoot/constants/states/firewall';
import 'TestRoot/SpecUtil';

import { activeViolationsResult } from './data';

describe('addContainerImageWaiverPageSlice', () => {
  describe('addContainerImageWaiverPage/load', () => {
    it('pending', () => {
      const newState = reducer(initialState, {
        type: 'addContainerImageWaiverPage/load/pending',
      });

      expect(newState.loading).toBe(true);
    });

    it('rejected', () => {
      const newState = reducer(initialState, {
        type: 'addContainerImageWaiverPage/load/rejected',
        payload: 'Something went wrong.',
      });

      expect(newState.loading).toBe(false);
      expect(newState.error).toBe('Something went wrong.');
    });

    it('fulfilled', () => {
      const newState = reducer(initialState, {
        type: 'addContainerImageWaiverPage/load/fulfilled',
        payload: activeViolationsResult,
      });

      expect(newState.loading).toBe(false);
      expect(newState.containerImageName).toBe('alpine : 3.6.5');
      expect(newState.failViolationsCount).toBe(3);
      expect(newState.affectedComponentsCount).toBe(3);
      expect(newState.policyNameList).toEqual([
        { policyName: 'docker-all', threatLevelCategory: 'critical' },
        { policyName: 'docker-policy-2.7.6-r0', threatLevelCategory: 'severe' },
        { policyName: 'docker-policy-2.5.5-r2', threatLevelCategory: 'moderate' },
      ]);
      expect(newState.threatLevelCounts).toEqual({ critical: 1, severe: 1, moderate: 1 });
    });
  });

  describe('addContainerImageWaiverPage/save', () => {
    it('pending', () => {
      const newState = reducer(initialState, {
        type: 'addContainerImageWaiverPage/save/pending',
      });

      expect(newState.submitMaskState).toBe(false);
    });

    it('rejected', () => {
      const newState = reducer(initialState, {
        type: 'addContainerImageWaiverPage/save/rejected',
        payload: 'Something went wrong.',
      });

      expect(newState.loading).toBe(false);
      expect(newState.submitMaskState).toBe(null);
      expect(newState.submitError).toBe('Something went wrong.');
    });

    it('fulfilled', () => {
      const date = new Date();
      date.setDate(date.getDate() + 5);
      const formattedDatePlus5Day = date.toISOString();
      const payload = {
        expiryTime: formattedDatePlus5Day,
        waiverReasonId: 'Test waiver id',
        comment: 'Test waiver comment',
      };

      const newState = reducer(initialState, {
        type: 'addContainerImageWaiverPage/save/fulfilled',
        payload: payload,
      });

      expect(newState.loading).toBe(false);
      expect(newState.submitMaskState).toBe(true);
    });
  });

  describe('addContainerImageWaiverPage/setExpiryTime', () => {
    it('set expiry time', () => {
      const newState = reducer(initialState, {
        type: 'addContainerImageWaiverPage/setExpiryTime',
        payload: 14,
      });

      expect(newState.expiryTime).toBe(14);
    });

    it('set custom expiry time and a validation error when the payload is in the past', () => {
      const date = new Date();
      date.setDate(date.getDate() - 10);
      const minus10DayISODate = date.toISOString();
      const newState = reducer(initialState, {
        type: 'addContainerImageWaiverPage/setCustomExpiryTime',
        payload: minus10DayISODate,
      });

      expect(newState.customExpiryTime.value).toEqual(minus10DayISODate);
      expect(newState.customExpiryTime.isPristine).toBe(false);
      expect(newState.customExpiryTime.validationErrors).toContain('Date must be in the future');
    });

    it('set custom expiry time and no validation error when the payload is in the future', () => {
      const date = new Date();
      date.setDate(date.getDate() + 10);
      const plus10DayISODate = date.toISOString();
      const newState = reducer(initialState, {
        type: 'addContainerImageWaiverPage/setCustomExpiryTime',
        payload: plus10DayISODate,
      });

      expect(newState.customExpiryTime.value).toEqual(plus10DayISODate);
      expect(newState.customExpiryTime.isPristine).toBe(false);
      expect(newState.customExpiryTime.validationErrors).toBeFalsy();
    });
  });

  describe('addContainerImageWaiverPage/setWaiverReason', () => {
    it('set waiver reason', () => {
      const newState = reducer(initialState, {
        type: 'addContainerImageWaiverPage/setWaiverReason',
        payload: 'Test waiver reason',
      });

      expect(newState.waiverReasonId).toBe('Test waiver reason');
    });
  });

  describe('addContainerImageWaiverPage/setWaiverComment', () => {
    it('set waiver reason', () => {
      const newState = reducer(initialState, {
        type: 'addContainerImageWaiverPage/setWaiverComment',
        payload: 'Test waiver comment',
      });

      expect(newState.waiverComments.value).toBe('Test waiver comment');
      expect(newState.waiverComments.isPristine).toBe(false);
      expect(newState.waiverComments.validationErrors).toBeFalsy();
    });
  });

  describe('returnToContainerReportPage', () => {
    it('preserves origin when navigating back to the container report', () => {
      const store = SpecUtil.mockReduxStore({});

      store.dispatch(
        actions.returnToContainerReportPage('test-public-id', 'test-scan-id', FIREWALL_FIREWALLPAGE_CONTAINERS)
      );

      expect(store.getActions()).toHaveAction({
        type: '@@reduxUiRouter/stateGo',
        payload: {
          to: 'firewall.containerReport',
          params: {
            publicId: 'test-public-id',
            scanId: 'test-scan-id',
            origin: FIREWALL_FIREWALLPAGE_CONTAINERS,
          },
          options: undefined,
        },
      });
    });
  });
});
