/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  gotoNewVulnerability,
  LOAD_SIDEBAR_NAV_LIST_FAILED,
  LOAD_SIDEBAR_NAV_LIST_FULFILLED,
  LOAD_SIDEBAR_NAV_LIST_REQUESTED,
  loadSidebarNav
} from '../../../main/frontend/sidebarNav/sidebarNavListActions';
import * as RouterActions from '../../../main/frontend/reduxUiRouter/routerActions';
import * as DashboardFilterActions from '../../../main/frontend/dashboard/filter/dashboardFilterActions';

describe('sidebarNavListActions', function() {

  describe('gotoNewVulnerability', function() {
    it('calls stateGo with the correct parameters', function() {
      const id = 12345;
      const stateGoSpy = spyOn(RouterActions, 'stateGo');

      gotoNewVulnerability(id);
      expect(stateGoSpy).toHaveBeenCalledWith('sidebarView.violation', { id });
    });
  });

  describe('loadSidebarNav', function() {
    let store;
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
          sortFields: ['firstOccurrenceTime'],
          results: { foo: 'bar'}
        }
      },
      stages: {
        dashboard: {
          stageTypes: null
        }
      },
      sidebarNavList: {
        sidebarId: '333',
        sidebarReference: 'filter'
      }
    };

    beforeEach(function() {
      store = SpecUtil.mockReduxStore(initialState);
    });

    it('dispatches LOAD_SIDEBAR_NAV_LIST_REQUESTED immediately', function() {
      spyOn(DashboardFilterActions, 'loadFilter').and.callThrough();

      store.dispatch(loadSidebarNav(stateParams));

      expect(DashboardFilterActions.loadFilter).toHaveBeenCalled();
      expect(store.getActions()[0].type).toEqual(LOAD_SIDEBAR_NAV_LIST_REQUESTED);
      expect(store.getActions()[0].payload).toEqual({
        sidebarReference: 'filter',
        sidebarId: '12345',
        contentType: 'violation'
      });
    });

    it('doesn\'t call loadFilter or fail if type is null', function() {
      spyOn(DashboardFilterActions, 'loadFilter').and.callThrough();

      store.dispatch(loadSidebarNav({
        type: null,
        sidebarReference: 'filter',
        sidebarId: '423'
      }));

      expect(store.getActions().length).toBe(1);
      expect(DashboardFilterActions.loadFilter).not.toHaveBeenCalled();
    });

    it('dispatches LOAD_SIDEBAR_NAV_LIST_FULFILLED with violations results data', function(done) {
      spyOn(DashboardFilterActions, 'loadFilter').and.returnValue(Promise.resolve({}));

      store.dispatch(loadSidebarNav({
        type: 'violation',
        sidebarReference: 'filter',
        sidebarId: '423'
      })).then(() => {
        expect(DashboardFilterActions.loadFilter).toHaveBeenCalledWith('violations');
        expect(store.getActions()[2].type).toEqual(LOAD_SIDEBAR_NAV_LIST_FULFILLED);
        expect(store.getActions()[2].payload).toEqual({
          data: { foo: 'bar' },
          contentType: 'violations',
          backButtonStateName: 'dashboard.overview.violations'
        });

        done();
      });
    });

    it('dispatches LOAD_SIDEBAR_NAV_LIST_FAILED when the response fails', function(done) {
      const responseError = 'errrr!';

      spyOn(DashboardFilterActions, 'loadFilter').and.returnValue(Promise.reject(responseError));

      store.dispatch(loadSidebarNav({
        type: 'violation',
        sidebarReference: 'filter',
        sidebarId: '424'
      })).then(() => {
        expect(store.getActions()[2].type).toEqual(LOAD_SIDEBAR_NAV_LIST_FAILED);
        expect(store.getActions()[2].payload).toEqual(responseError);

        done();
      });

      expect(DashboardFilterActions.loadFilter).toHaveBeenCalledWith('violations');
    });

    it('dispatches LOAD_SIDEBAR_NAV_LIST_FAILED if an unknown sidebarReference is passed in', function() {
      spyOn(DashboardFilterActions, 'loadFilter');

      store.dispatch(loadSidebarNav({
        type: 'violation',
        sidebarReference: 'thisisnotreal',
        sidebarId: '425'
      }));

      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[0].type).toEqual(LOAD_SIDEBAR_NAV_LIST_REQUESTED);
      expect(store.getActions()[1].type).toEqual(LOAD_SIDEBAR_NAV_LIST_FAILED);
      expect(store.getActions()[1].payload).toEqual('Unknown sidebarReference: thisisnotreal');
      expect(DashboardFilterActions.loadFilter).not.toHaveBeenCalled();
    });
  });
});
