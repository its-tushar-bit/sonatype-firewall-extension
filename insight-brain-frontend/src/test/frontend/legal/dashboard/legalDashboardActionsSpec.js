/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { getLegalDashboardApplicationsUrl } from '../../../../main/frontend/util/CLMLocation';
import {
  LEGAL_DASHBOARD_LOAD_APPLICATIONS_FAILED,
  LEGAL_DASHBOARD_LOAD_APPLICATIONS_FULFILLED,
  LEGAL_DASHBOARD_LOAD_APPLICATIONS_REQUESTED,
  loadApplications
} from '../../../../main/frontend/legal/dashboard/legalDashboardActions';

describe('legalDashboardActions', function () {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);

  describe('loadApplications', function () {
    let store;

    beforeEach(function () {
      store = SpecUtil.mockReduxStore({});
    });

    it('immediately dispatches a LEGAL_DASHBOARD_LOAD_APPLICATIONS_REQUESTED action', function () {
      store.dispatch(loadApplications());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(LEGAL_DASHBOARD_LOAD_APPLICATIONS_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
    });

    it('dispatches a LEGAL_DASHBOARD_LOAD_APPLICATIONS_FULFILLED action with applications', function (done) {
      const applications = [{
        foo: 'bar'
      }];
      mockAxiosCalls({
        post: {
          [getLegalDashboardApplicationsUrl()]: Promise.resolve({ data: applications })
        }
      });

      store.dispatch(loadApplications()).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(LEGAL_DASHBOARD_LOAD_APPLICATIONS_FULFILLED);
        expect(actions[1].payload).toBe(applications);
        done();
      });
    });

    it('dispatches a LEGAL_DASHBOARD_LOAD_APPLICATIONS_FAILED action when API fails', function (done) {
      const errorTest = 'Error test';
      mockAxiosCalls({
        post: {
          [getLegalDashboardApplicationsUrl()]: Promise.reject(errorTest)
        }
      });

      store.dispatch(loadApplications()).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(LEGAL_DASHBOARD_LOAD_APPLICATIONS_FAILED);
        expect(actions[1].payload).toBe(errorTest);
        done();
      });
    });
  });
});
