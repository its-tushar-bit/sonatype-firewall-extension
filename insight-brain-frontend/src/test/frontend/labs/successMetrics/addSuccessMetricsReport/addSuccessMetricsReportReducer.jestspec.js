/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import {
  ADD_SUCCESS_METRICS_REPORT_LOAD_ORGS_APPS_FAILED,
  ADD_SUCCESS_METRICS_REPORT_LOAD_ORGS_APPS_FULFILLED,
  ADD_SUCCESS_METRICS_REPORT_LOAD_ORGS_APPS_REQUESTED,
  ADD_SUCCESS_METRICS_REPORT_SET_INCLUDE_LATEST_DATA,
  ADD_SUCCESS_METRICS_REPORT_SET_IS_ALL_APPLICATIONS,
  ADD_SUCCESS_METRICS_REPORT_SET_ORGS_APPS,
  ADD_SUCCESS_METRICS_REPORT_SET_REPORT_NAME,
  ADD_SUCCESS_METRICS_REPORT_SUBMIT_FAILED,
  ADD_SUCCESS_METRICS_REPORT_SUBMIT_FULFILLED,
  ADD_SUCCESS_METRICS_REPORT_SUBMIT_MASK_STATE_DONE,
  ADD_SUCCESS_METRICS_REPORT_SUBMIT_REQUESTED,
} from '../../../../../main/frontend/labs/successMetrics/addSuccessMetricsReport/addSuccessMetricsReportActions';
import reduce, {
  initialState as actualInitialState,
} from '../../../../../main/frontend/labs/successMetrics/addSuccessMetricsReport/addSuccessMetricsReportReducer';

const { initialState: initUserInput } = nxTextInputStateHelpers;

describe('addSuccessMetricsReportReducer', () => {
  let initialState,
    otherObject = {};

  beforeEach(() => {
    initialState = { ...actualInitialState };
  });

  describe(`${ADD_SUCCESS_METRICS_REPORT_LOAD_ORGS_APPS_REQUESTED} action`, () => {
    it('returns the initial state', () => {
      const action = {
        type: ADD_SUCCESS_METRICS_REPORT_LOAD_ORGS_APPS_REQUESTED,
      };
      const newState = reduce(initialState, action);
      expect(newState).toEqual(initialState);
    });
  });

  describe(`${ADD_SUCCESS_METRICS_REPORT_LOAD_ORGS_APPS_FULFILLED} action`, () => {
    let action,
      newState,
      organizations = [{}],
      applications = [{}, {}];
    beforeEach(() => {
      action = {
        type: ADD_SUCCESS_METRICS_REPORT_LOAD_ORGS_APPS_FULFILLED,
        payload: {
          organizations,
          applications,
        },
      };
      const state = {
        otherObject,
        loading: true,
      };
      newState = reduce(state, action);
    });

    it('does not affect other props', () => {
      expect(newState.otherObject).toBe(otherObject);
    });

    it('sets loading to false', () => {
      expect(newState.loading).toBe(false);
    });

    it('fills the organizations and applications properties', () => {
      expect(newState.organizations).toEqual(organizations);
      expect(newState.applications).toEqual(applications);
    });
  });

  describe(`${ADD_SUCCESS_METRICS_REPORT_LOAD_ORGS_APPS_FAILED} action`, () => {
    let payload = 'some error happened',
      newState;

    beforeEach(() => {
      payload = 'some error happened';
      const action = {
        type: ADD_SUCCESS_METRICS_REPORT_LOAD_ORGS_APPS_FAILED,
        payload,
      };
      const state = {
        otherObject,
        loadError: null,
      };
      newState = reduce(state, action);
    });

    it('does not affect other props', () => {
      expect(newState.otherObject).toBe(otherObject);
    });

    it('sets load error property', () => {
      expect(newState.loadError).toBe(payload);
    });
  });

  describe(`${ADD_SUCCESS_METRICS_REPORT_SET_ORGS_APPS} action`, () => {
    let newState,
      payload = { selectedOrganizations: new Set([{}]), selectedApplications: new Set([{}]) };

    beforeEach(() => {
      const action = {
        type: ADD_SUCCESS_METRICS_REPORT_SET_ORGS_APPS,
        payload,
      };
      const state = {
        otherObject,
        selectedOrgsAndApps: {
          organizations: new Set([]),
          applications: new Set([]),
        },
      };
      newState = reduce(state, action);
    });

    it('does not affect other properties', () => {
      expect(newState.otherObject).toBe(otherObject);
    });

    it('sets the organizations and applications selected', () => {
      expect(newState.selectedOrgsAndApps.organizations).toEqual(payload.selectedOrganizations);
      expect(newState.selectedOrgsAndApps.applications).toEqual(payload.selectedApplications);
    });
  });

  describe(`${ADD_SUCCESS_METRICS_REPORT_SET_REPORT_NAME} action`, () => {
    let newState,
      payload = { value: 'Report Name', reports: [] };

    beforeEach(() => {
      const action = {
        type: ADD_SUCCESS_METRICS_REPORT_SET_REPORT_NAME,
        payload,
      };
      const state = {
        otherObject,
        reportName: initUserInput(''),
      };
      newState = reduce(state, action);
    });

    it('does not affect other props', () => {
      expect(newState.otherObject).toBe(otherObject);
    });

    it('fills reportName prop', () => {
      expect(newState.reportName.trimmedValue).toBe(payload.value);
    });

    describe('duplication validator', () => {
      it('sets validation error', () => {
        let newState,
          payload = { value: 'Report name', reports: [{ name: 'report name' }] };
        const state = {
          reportName: initUserInput(''),
        };
        const action = {
          type: ADD_SUCCESS_METRICS_REPORT_SET_REPORT_NAME,
          payload,
        };
        newState = reduce(state, action);
        expect(newState.reportName.validationErrors.length).toBe(1);
      });

      it('sets validation error', () => {
        let newState,
          payload = { value: 'Report name', reports: [{ name: 'Report name' }] };
        const state = {
          reportName: initUserInput(''),
        };
        const action = {
          type: ADD_SUCCESS_METRICS_REPORT_SET_REPORT_NAME,
          payload,
        };
        newState = reduce(state, action);
        expect(newState.reportName.validationErrors.length).toBe(1);
      });
    });
  });

  describe(`${ADD_SUCCESS_METRICS_REPORT_SET_INCLUDE_LATEST_DATA} action`, () => {
    it('fills includeLatestData prop', () => {
      const action = {
        type: ADD_SUCCESS_METRICS_REPORT_SET_INCLUDE_LATEST_DATA,
        payload: false,
      };
      const state = {
        otherObject,
        includeLatestData: true,
      };
      const newState = reduce(state, action);
      expect(newState.includeLatestData).toBe(false);
      expect(newState.otherObject).toBe(otherObject);
    });
  });

  describe(`${ADD_SUCCESS_METRICS_REPORT_SET_IS_ALL_APPLICATIONS} action`, () => {
    it('sets isAllApplication prop', () => {
      const action = {
        type: ADD_SUCCESS_METRICS_REPORT_SET_IS_ALL_APPLICATIONS,
        payload: false,
      };
      const state = {
        otherObject,
        isAllApplications: true,
      };
      const newState = reduce(state, action);

      expect(newState.isAllApplications).toBe(false);
      expect(newState.otherObject).toBe(otherObject);
    });
  });

  describe(`${ADD_SUCCESS_METRICS_REPORT_SUBMIT_REQUESTED} action`, () => {
    it('sets false to submitMaskState', () => {
      const action = {
        type: ADD_SUCCESS_METRICS_REPORT_SUBMIT_REQUESTED,
      };
      const state = {
        otherObject,
        submitMaskState: null,
      };
      const newState = reduce(state, action);
      expect(newState.submitMaskState).toBe(false);
      expect(newState.otherObject).toBe(otherObject);
    });
  });

  describe(`${ADD_SUCCESS_METRICS_REPORT_SUBMIT_FULFILLED} action`, () => {
    it('sets true to submitMaskState', () => {
      const action = {
        type: ADD_SUCCESS_METRICS_REPORT_SUBMIT_FULFILLED,
      };
      const state = {
        otherObject,
        submitMaskState: false,
      };
      const newState = reduce(state, action);
      expect(newState.submitMaskState).toBe(true);
      expect(newState.otherObject).toBe(otherObject);
    });
  });

  describe(`${ADD_SUCCESS_METRICS_REPORT_SUBMIT_MASK_STATE_DONE} action`, () => {
    it('sets null to submitMaskState', () => {
      const action = {
        type: ADD_SUCCESS_METRICS_REPORT_SUBMIT_MASK_STATE_DONE,
      };
      const state = {
        otherObject,
        submitMaskState: false,
      };
      const newState = reduce(state, action);
      expect(newState.submitMaskState).toBe(null);
      expect(newState.otherObject).toBe(otherObject);
    });
  });

  describe(`${ADD_SUCCESS_METRICS_REPORT_SUBMIT_FAILED} action`, () => {
    let payload = 'some error happened',
      newState;

    beforeEach(() => {
      const action = {
        type: ADD_SUCCESS_METRICS_REPORT_SUBMIT_FAILED,
        payload,
      };
      const state = {
        otherObject,
        submitMaskState: true,
        submitError: null,
      };
      newState = reduce(state, action);
    });

    it('does not affect other props', () => {
      expect(newState.otherObject).toBe(otherObject);
    });

    it('fills submitError prop', () => {
      expect(newState.submitError).toBe(payload);
    });

    it('sets null to submitMaskState', () => {
      expect(newState.submitMaskState).toBeNull();
    });
  });
});
