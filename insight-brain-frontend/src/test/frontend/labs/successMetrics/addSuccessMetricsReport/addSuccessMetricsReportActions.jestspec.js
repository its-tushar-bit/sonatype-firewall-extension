/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import {
  ADD_SUCCESS_METRICS_REPORT_LOAD_ORGS_APPS_FAILED,
  ADD_SUCCESS_METRICS_REPORT_LOAD_ORGS_APPS_FULFILLED,
  ADD_SUCCESS_METRICS_REPORT_LOAD_ORGS_APPS_REQUESTED,
  ADD_SUCCESS_METRICS_REPORT_SET_ORGS_APPS,
  ADD_SUCCESS_METRICS_REPORT_SUBMIT_FAILED,
  ADD_SUCCESS_METRICS_REPORT_SUBMIT_FULFILLED,
  ADD_SUCCESS_METRICS_REPORT_SUBMIT_REQUESTED,
  loadOrgsAndApps,
  submit,
  toggleOrgsApps,
} from '../../../../../main/frontend/labs/successMetrics/addSuccessMetricsReport/addSuccessMetricsReportActions';
import {
  getApplicationsUrl,
  getOrganizationsUrl,
  getSuccessMetricsReportsUrl,
} from '../../../../../main/frontend/util/CLMLocation';

import 'TestRoot/SpecUtil';

describe('addSuccessMetricsReportActions', () => {
  let mockAxiosCall = SpecUtil.axiosMockerGenerator(axios);
  const applicationsUrl = getApplicationsUrl(),
    organizationsUrl = getOrganizationsUrl(),
    submitUrl = getSuccessMetricsReportsUrl();
  let state, store, actions;

  beforeEach(() => {
    state = {
      successMetrics: {
        addSuccessMetricsReport: {
          reportName: { trimmedValue: 'report name' },
          includeLatestData: true,
          isAllApplications: true,
          selectedOrgsAndApps: {
            organization: new Set([]),
            application: new Set([]),
          },
        },
      },
    };
    store = SpecUtil.mockReduxStore(state);
    actions = store.getActions();
  });

  describe('loadOrgsAndApps', () => {
    it(`filter the organizations and dispatches ${ADD_SUCCESS_METRICS_REPORT_LOAD_ORGS_APPS_FULFILLED} action`, (done) => {
      mockAxiosCall({
        get: {
          [applicationsUrl]: Promise.resolve({ data: [{}] }),
          [organizationsUrl]: Promise.resolve({ data: [{}, { id: 'ROOT_ORGANIZATION_ID' }] }),
        },
      });
      store.dispatch(loadOrgsAndApps()).then(() => {
        const { type, payload } = actions[3];
        expect(type).toBe(ADD_SUCCESS_METRICS_REPORT_LOAD_ORGS_APPS_FULFILLED);
        expect(payload).toEqual({
          organizations: [{}],
          applications: [{}],
        });
        done();
      });
      const [{ type }] = actions;
      expect(type).toBe(ADD_SUCCESS_METRICS_REPORT_LOAD_ORGS_APPS_REQUESTED);
    });

    it(`dispatches ${ADD_SUCCESS_METRICS_REPORT_LOAD_ORGS_APPS_FAILED} action`, (done) => {
      const error = 'some error';
      mockAxiosCall({
        get: {
          [applicationsUrl]: () => Promise.reject(error),
          [organizationsUrl]: () => Promise.reject(error),
        },
      });
      store.dispatch(loadOrgsAndApps()).then(() => {
        const { type, payload } = actions[3];
        expect(type).toBe(ADD_SUCCESS_METRICS_REPORT_LOAD_ORGS_APPS_FAILED);
        expect(payload).toEqual(error);
        done();
      });
      const [{ type }] = actions;
      expect(type).toBe(ADD_SUCCESS_METRICS_REPORT_LOAD_ORGS_APPS_REQUESTED);
    });
  });

  describe('submit', () => {
    const closeFn = () => {},
      data = {};
    describe('success', () => {
      let spy;

      beforeEach(() => {
        spy = jest.spyOn(axios, 'post').mockReturnValue(Promise.resolve({ data }));
      });

      it('calls service without organizationIds and without applicationIds', () => {
        store.dispatch(submit(closeFn));
        const body = {
          name: 'report name',
          includeLatestData: true,
          scope: {},
        };

        const [{ type: actionType }] = actions;
        expect(actionType).toBe(ADD_SUCCESS_METRICS_REPORT_SUBMIT_REQUESTED);
        expect(spy).toHaveBeenCalledWith(submitUrl, body);
      });

      it('calls service with organizationIds and with applicationIds', () => {
        state = {
          successMetrics: {
            addSuccessMetricsReport: {
              reportName: { trimmedValue: 'report name' },
              includeLatestData: true,
              isAllApplications: false,
              selectedOrgsAndApps: {
                organizations: new Set([]),
                applications: new Set([]),
              },
            },
          },
        };
        store = SpecUtil.mockReduxStore(state);
        actions = store.getActions();
        store.dispatch(submit(closeFn));
        const body = {
          name: 'report name',
          includeLatestData: true,
          scope: {
            organizationIds: [],
            applicationIds: [],
          },
        };
        const [{ type: actionType }] = actions;
        expect(actionType).toBe(ADD_SUCCESS_METRICS_REPORT_SUBMIT_REQUESTED);
        expect(spy).toHaveBeenCalledWith(submitUrl, body);
      });

      it(`dispatches an ${ADD_SUCCESS_METRICS_REPORT_SUBMIT_FULFILLED} action`, (done) => {
        store.dispatch(submit(closeFn)).then(() => {
          const [, { type: actionType }] = actions;
          expect(actionType).toBe(ADD_SUCCESS_METRICS_REPORT_SUBMIT_FULFILLED);
          done();
        });
        const [{ type: actionType }] = actions;
        expect(actionType).toBe(ADD_SUCCESS_METRICS_REPORT_SUBMIT_REQUESTED);
      });
    });

    describe('fail', () => {
      it(`dispatches ${ADD_SUCCESS_METRICS_REPORT_SUBMIT_FAILED} action`, (done) => {
        const error = 'some error';
        jest.spyOn(axios, 'post').mockImplementation(() => Promise.reject(error));
        store.dispatch(submit(closeFn)).then(() => {
          const [, { type: actionType, payload: actionPayload }] = actions;
          expect(actionType).toBe(ADD_SUCCESS_METRICS_REPORT_SUBMIT_FAILED);
          expect(actionPayload).toBe(error);
          done();
        });
        const [{ type: actionType }] = actions;
        expect(actionType).toBe(ADD_SUCCESS_METRICS_REPORT_SUBMIT_REQUESTED);
      });
    });
  });

  describe('toggleOrgsApps', () => {
    it(`dispatches an ${ADD_SUCCESS_METRICS_REPORT_SET_ORGS_APPS} action`, () => {
      const { type, payload } = store.dispatch(toggleOrgsApps(new Set([]), new Set([])));
      expect(type).toBe(ADD_SUCCESS_METRICS_REPORT_SET_ORGS_APPS);
      expect(payload).toEqual({ selectedApplications: new Set([]), selectedOrganizations: new Set([]) });
    });
  });
});
