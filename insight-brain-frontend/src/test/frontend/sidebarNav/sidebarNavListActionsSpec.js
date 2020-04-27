/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import {
  gotoNewVulnerability,
  LOAD_SIDEBAR_NAV_LIST_FAILED,
  LOAD_SIDEBAR_NAV_LIST_FULFILLED,
  LOAD_SIDEBAR_NAV_LIST_REQUESTED,
  loadSidebarNav
} from '../../../main/frontend/sidebarNav/sidebarNavListActions';
import { getNewestRisksUrl } from '../../../main/frontend/util/CLMLocation';
import { MAX_RESULTS } from '../../../main/frontend/dashboard/services/dashboard.data.service';
import * as DashboardUtilsModule from '../../../main/frontend/dashboard/utils/dashboard.utils.module';
import * as RouterActions from '../../../main/frontend/reduxUiRouter/routerActions';

describe('sidebarNavListActions', function() {

  describe('gotoNewVulnerability', function() {
    it('calls stateGo with the correct parameters', function() {
      const id = 12345;
      const stateGoSpy = spyOn(RouterActions, 'stateGo');

      gotoNewVulnerability(id);
      expect(stateGoSpy).toHaveBeenCalledWith('violation', { id });
    });
  });

  describe('loadSidebarNav', function() {
    let store, createDashboardDataRequestPayloadSpy;
    const stateParams = {
      type: 'violation',
      sidebarReference: 'filter',
      sidebarId: '12345'
    };
    const initialState = {
      dashboardFilter: {
        appliedFilter: 'dashboardAppliedFilter'
      },
      dashboard: {
        violations: {
          sortFields: ['firstOccurrenceTime']
        }
      },
      sidebarNavList: {
        sidebarId: '333',
        sidebarReference: 'filter'
      }
    };

    beforeEach(function() {
      store = SpecUtil.mockReduxStore(initialState);
      createDashboardDataRequestPayloadSpy = spyOn(
          DashboardUtilsModule, 'createDashboardDataRequestPayload').and.returnValue('violationsRequest');
    });

    it('dispatches LOAD_SIDEBAR_NAV_LIST_REQUESTED immediately', function() {
      spyOn(axios, 'get').and.returnValue(Promise.resolve());

      store.dispatch(loadSidebarNav(stateParams));

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0].type).toEqual(LOAD_SIDEBAR_NAV_LIST_REQUESTED);
      expect(store.getActions()[0].payload).toEqual({ sidebarReference: 'filter', sidebarId: '12345' });
    });

    it('doesn\'t dispatch LOAD_SIDEBAR_NAV_LIST_REQUESTED if called with the same parameters', function() {
      spyOn(axios, 'get').and.returnValue(Promise.resolve());
      store = SpecUtil.mockReduxStore(initialState);

      const stateParams = {
        type: 'violation',
        sidebarReference: 'filter',
        sidebarId: '333'
      };
      store.dispatch(loadSidebarNav(stateParams));

      expect(store.getActions().length).toBe(0);
    });

    it('doesn\'t dispatch LOAD_SIDEBAR_NAV_LIST_REQUESTED if sidebarId is undefined on the state.params', function() {
      spyOn(axios, 'get').and.returnValue(Promise.resolve());
      store = SpecUtil.mockReduxStore({
        dashboardFilter: {
          appliedFilter: 'dashboardAppliedFilter'
        },
        dashboard: {
          violations: {
            sortFields: ['firstOccurrenceTime']
          }
        },
        sidebarNavList: {
          sidebarId: null,
          sidebarReference: 'filter'
        }
      });

      const stateParams = {
        type: 'violation',
        sidebarReference: 'filter',
        sidebarId: undefined
      };
      store.dispatch(loadSidebarNav(stateParams));

      expect(store.getActions().length).toBe(0);
    });

    it('dispatches LOAD_SIDEBAR_NAV_LIST_REQUESTED if called with a different sidebarReference', function() {
      spyOn(axios, 'get').and.returnValue(Promise.resolve());
      store = SpecUtil.mockReduxStore(initialState);

      const stateParams = {
        type: 'violation',
        sidebarId: '555',
        sidebarReference: 'sidebarReferenceFilter'
      };
      store.dispatch(loadSidebarNav(stateParams));

      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[0].type).toEqual(LOAD_SIDEBAR_NAV_LIST_REQUESTED);
      expect(store.getActions()[1].type).toEqual(LOAD_SIDEBAR_NAV_LIST_FAILED);

      stateParams.sidebarReference = 'newSidebarReferenceFilter';
      store.dispatch(loadSidebarNav(stateParams));

      expect(store.getActions().length).toBe(4);
      expect(store.getActions()[0].type).toEqual(LOAD_SIDEBAR_NAV_LIST_REQUESTED);
      expect(store.getActions()[1].type).toEqual(LOAD_SIDEBAR_NAV_LIST_FAILED);
      expect(store.getActions()[2].type).toEqual(LOAD_SIDEBAR_NAV_LIST_REQUESTED);
      expect(store.getActions()[3].type).toEqual(LOAD_SIDEBAR_NAV_LIST_FAILED);
    });

    it('dispatches LOAD_SIDEBAR_NAV_LIST_REQUESTED if called with a different sidebarId', function() {
      spyOn(axios, 'get').and.returnValue(Promise.resolve());
      store = SpecUtil.mockReduxStore(initialState);

      const stateParams = {
        type: 'violation',
        sidebarReference: 'filter',
        sidebarId: '555'
      };
      store.dispatch(loadSidebarNav(stateParams));

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0].type).toEqual(LOAD_SIDEBAR_NAV_LIST_REQUESTED);

      stateParams.sidebarId = '999';
      store.dispatch(loadSidebarNav(stateParams));

      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[0].type).toEqual(LOAD_SIDEBAR_NAV_LIST_REQUESTED);
      expect(store.getActions()[1].type).toEqual(LOAD_SIDEBAR_NAV_LIST_REQUESTED);
    });

    it('dispatches LOAD_SIDEBAR_NAV_LIST_FULFILLED with response data and metadata for violations', function(done) {
      const responseData = { dashboardResults: { foo: 'bar' } };

      spyOn(axios, 'post').and.returnValue(Promise.resolve({ data: responseData }));

      store.dispatch(loadSidebarNav({
        type: 'violation',
        sidebarReference: 'filter',
        sidebarId: '423'
      })).then(() => {
        expect(store.getActions().length).toBe(2);
        expect(store.getActions()[1].type).toEqual(LOAD_SIDEBAR_NAV_LIST_FULFILLED);
        expect(store.getActions()[1].payload).toEqual({
          data: { foo: 'bar' },
          contentType: 'violations',
          backButtonStateName: 'dashboard.overview.violations'
        });

        done();
      });

      expect(store.getActions().length).toBe(1);
      expect(createDashboardDataRequestPayloadSpy).toHaveBeenCalledWith(
          'dashboardAppliedFilter',
          MAX_RESULTS,
          ['AGE']
      );
      expect(axios.post).toHaveBeenCalledWith(getNewestRisksUrl(), 'violationsRequest');
    });

    it('dispatches LOAD_SIDEBAR_NAV_LIST_FAILED when the response fails', function(done) {
      const responseError = 'errrr!';

      spyOn(axios, 'post').and.returnValue(Promise.reject(responseError));

      store.dispatch(loadSidebarNav({
        type: 'violation',
        sidebarReference: 'filter',
        sidebarId: '424'
      })).then(() => {
        expect(store.getActions().length).toBe(2);
        expect(store.getActions()[1].type).toEqual(LOAD_SIDEBAR_NAV_LIST_FAILED);
        expect(store.getActions()[1].payload).toEqual(responseError);

        done();
      });

      expect(store.getActions().length).toBe(1);
      expect(axios.post).toHaveBeenCalledWith(getNewestRisksUrl(), 'violationsRequest');
      expect(createDashboardDataRequestPayloadSpy).toHaveBeenCalledWith(
          'dashboardAppliedFilter',
          MAX_RESULTS,
          ['AGE']
      );
    });

    it('dispatches LOAD_SIDEBAR_NAV_LIST_FAILED if an unknown sidebarReference is passed in', function() {
      spyOn(axios, 'post');

      store.dispatch(loadSidebarNav({
        type: 'violation',
        sidebarReference: 'thisisnotreal',
        sidebarId: '425'
      }));

      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[0].type).toEqual(LOAD_SIDEBAR_NAV_LIST_REQUESTED);
      expect(store.getActions()[1].type).toEqual(LOAD_SIDEBAR_NAV_LIST_FAILED);
      expect(store.getActions()[1].payload).toEqual('Unknown sidebarReference: thisisnotreal');
      expect(axios.post).not.toHaveBeenCalled();
      expect(createDashboardDataRequestPayloadSpy).not.toHaveBeenCalled();
    });
  });
});
