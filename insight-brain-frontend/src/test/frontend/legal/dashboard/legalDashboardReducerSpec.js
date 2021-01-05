/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import legalDashboardReducer from '../../../../main/frontend/legal/dashboard/legalDashboardReducer';
import {
  LEGAL_DASHBOARD_LOAD_APPLICATIONS_FAILED,
  LEGAL_DASHBOARD_LOAD_APPLICATIONS_FULFILLED,
  LEGAL_DASHBOARD_LOAD_APPLICATIONS_REQUESTED
} from '../../../../main/frontend/legal/dashboard/legalDashboardActions';

describe('legalDashboardReducer', function () {
  describe('initial state', function () {
    it('is used if no state is provided', function () {
      const action = { type: 'UNKNOWN' };
      const newState = legalDashboardReducer(undefined, action);
      expect(newState).not.toBeUndefined();
    });

    it('has default fields', function () {
      const action = { type: 'UNKNOWN' };
      const newState = legalDashboardReducer(undefined, action);

      expect(newState.loading).toBeFalsy();
      expect(newState.loadError).toBeNull();
      expect(newState.applications.length).toBe(0);
      expect(newState.components.length).toBe(0);
    });
  });

  describe('unknown action', function () {
    it('returns original state', function () {
      const state = { foo: 'bar' };
      const action = {
        type: 'UNKNOWN'
      };
      const newState = legalDashboardReducer(state, action);
      expect(newState).toBe(state);
    });
  });

  describe('LEGAL_DASHBOARD_LOAD_APPLICATIONS_REQUESTED action', function () {
    it('sets loading to true and error to null', function () {
      const newState = legalDashboardReducer(undefined, {
        type: LEGAL_DASHBOARD_LOAD_APPLICATIONS_REQUESTED
      });

      expect(newState.loading).toBeTruthy();
      expect(newState.loadError).toBeNull();
    });
  });

  describe('LEGAL_DASHBOARD_LOAD_APPLICATIONS_FULFILLED action', function () {
    it('sets loading to false, applications to payload and error to null', function () {
      const state = {
        legalDashboard: {
          loading: true,
          error: null
        },
        applications: []
      };
      const applications = [{
        foo: 'bar'
      }];
      const newState = legalDashboardReducer(state, {
        type: LEGAL_DASHBOARD_LOAD_APPLICATIONS_FULFILLED,
        payload: applications
      });

      expect(newState.loading).toBeFalsy();
      expect(newState.loadError).toBeNull();
      expect(newState.applications).toBe(applications);
    });
  });

  describe('LEGAL_DASHBOARD_LOAD_APPLICATIONS_FAILED action', function () {
    it('sets loading to false and error to payload', function () {
      const state = {
        legalDashboard: {
          loading: true,
          error: null
        }
      };
      const errorTest = 'Error test';
      const newState = legalDashboardReducer(state, {
        type: LEGAL_DASHBOARD_LOAD_APPLICATIONS_FAILED,
        payload: errorTest
      });

      expect(newState.loading).toBeFalsy();
      expect(newState.loadError).toBe(errorTest);
    });
  });
});
