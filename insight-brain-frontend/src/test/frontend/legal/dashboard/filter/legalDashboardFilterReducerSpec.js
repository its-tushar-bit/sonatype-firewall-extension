/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import legalDashboardFilterReducer
  from '../../../../../main/frontend/legal/dashboard/filter/legalDashboardFilterReducer';

describe('legalDashboardFilterReducer', function() {
  let otherObject;

  beforeEach(inject(function() {
    otherObject = {value: 'test value'};
  }));

  describe('unknown action', function() {
    it('returns original state', function() {
      var state = Object.freeze({foo: 'bar'});
      var action = {
        type: 'UNKNOWN'
      };
      var newState = legalDashboardFilterReducer(state, action);
      expect(newState).toBe(state);
    });
  });

  describe('initial state', function() {
    it('is used if no state is provided', function() {
      var action = {
        type: 'UNKNOWN'
      };
      var newState = legalDashboardFilterReducer(undefined, action);
      expect(newState).not.toBeUndefined();
    });
  });

  describe('LOAD_LEGAL_FILTER_REQUESTED action', function() {
    it('sets loading to true and resets filter', function() {
      var state = Object.freeze({
        loadError: 'error',
        loading: false,
        other: otherObject
      });
      var action = {type: 'LOAD_LEGAL_FILTER_REQUESTED'};
      var newState = legalDashboardFilterReducer(state, action);
      expect(newState.loading).toBe(true);
      expect(newState.loadError).toBeNull();
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('LOAD_LEGAL_FILTER_FAILED action', function() {
    it('sets loading to false and sets error', function() {
      var state = Object.freeze({
        loadError: null,
        loading: true,
        other: otherObject
      });
      var action = {
        type: 'LOAD_LEGAL_FILTER_FAILED',
        payload: 'load filter error'
      };
      var newState = legalDashboardFilterReducer(state, action);
      expect(newState.loadError).toBe('load filter error');
      expect(newState.loading).toBe(false);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('FETCH_LEGAL_AVAILABLE_FILTER_OPTIONS_FULFILLED action', function() {
    var initState, action;

    beforeEach(function() {
      initState = {
        loading: true,
        other: otherObject
      };
      action = {
        type: 'FETCH_LEGAL_AVAILABLE_FILTER_OPTIONS_FULFILLED',
        payload: {
          organizations: [
            {
              id: 'orgId1',
              name: 'OrganizationOne'
            }, {
              id: 'orgId2',
              name: 'OrganizationTwo'
            }, {
              id: 'ROOT_ORGANIZATION_ID',
              name: 'Root Organization'
            }
          ],
          applications: [
            {
              id: 'applicationIdZ',
              publicId: 'applicationPublicIdZ',
              name: 'ApplicationZ <b style="woah" class=\'evenmorewoah\'>&nbsp;shouldnotbebold</b>',
              organizationId: 'orgId1'
            }, {
              id: 'applicationIdA',
              publicId: 'applicationPublicIdA',
              name: 'ApplicationA',
              organizationId: 'orgId2'
            }, {
              id: 'applicationIdQ',
              publicId: 'applicationPublicIdQ',
              name: 'ApplicationQ',
              organizationId: 'orgId2'
            }, {
              id: 'applicationIdR',
              publicId: 'applicationPublicIdR',
              name: 'ApplicationR',
              organizationId: 'orgId2'
            }, {
              id: 'applicationIdS',
              publicId: 'applicationPublicIdS',
              name: 'ApplicationS',
              organizationId: 'noPermissionOrgId',
              organizationName: 'No Permission'
            }, {
              id: 'applicationIdS2',
              publicId: 'applicationPublicIdS2',
              name: 'ApplicationS2',
              organizationId: 'noPermissionOrgId',
              organizationName: 'No Permission'
            }
          ],
          categories: [
            {
              id: 'tagId1',
              organizationId: 'orgId1',
              name: 'TagOne',
              description: 'Tag One Description'
            }, {
              id: 'tagId2',
              organizationId: 'orgId2',
              name: 'TagTwo',
              description: 'Tag Two Description'
            }
          ],
          stages: MockData.getDashboardStageData()
        }
      };
    });

    it('does not set loading to false', function() {
      var state = Object.freeze(initState);
      var newState = legalDashboardFilterReducer(state, action);
      expect(newState.loading).toBe(true);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });

    it('sets available filter options', function() {
      var state = Object.freeze(initState);
      var newState = legalDashboardFilterReducer(state, action);
      expect(newState.other).toBe(otherObject); // other properties are not modified

      expect(newState.applications.length).toBe(action.payload.applications.length);
      expect(newState.applications[0].id).toBe(action.payload.applications[0].id);
      expect(newState.applications[1].id).toBe(action.payload.applications[1].id);

      // since we have an application but no permissions to the org add 1
      // and remove ROOT org
      expect(newState.organizations.length).toBe(3);
      expect(newState.organizations[0]).toBe(action.payload.organizations[0]);
      expect(newState.organizations[1]).toBe(action.payload.organizations[1]);
      // no permission to org scenario
      expect(newState.organizations[2].id).toBe(action.payload.applications[4].organizationId);
      expect(newState.organizations[2].name).toBe(action.payload.applications[4].organizationName);

      expect(newState.stages.length).toBe(MockData.getDashboardStageData().length);
      expect(newState.stages[0].id).toBe(MockData.getDashboardStageData()[0].stageTypeId);
      expect(newState.stages[0].name).toBe(MockData.getDashboardStageData()[0].stageName);
      expect(newState.stages[1].id).toBe(MockData.getDashboardStageData()[1].stageTypeId);
      expect(newState.stages[1].name).toBe(MockData.getDashboardStageData()[1].stageName);

      // one extra for uncategorized applications
      expect(newState.categories.length).toBe(action.payload.categories.length + 1);
      expect(newState.categories[0].id).toBe(null);
      expect(newState.categories[0].name).toBe('uncategorized applications');

      expect(newState.categories[1].id).toBe(action.payload.categories[0].id);
      // populates owner
      expect(newState.categories[1].owner).toBe(action.payload.organizations[0].name);
    });
  });

  describe('applyFilter', function() {
    var initState, action, filterJson, initSelected;

    beforeEach(function() {
      initSelected = Object.freeze({
        organizations: {},
        applications: {},
        categories: {},
        stages: {},
        policyTypes: {},
        policyViolationStates: {OPEN: true},
        maxDaysOld: 30,
        policyThreatLevels: [2, 10]
      });
      filterJson = {
        organizationFilters: ['orgId1', 'orgId2', 'org3'],
        policyThreatCategoryFilters: ['QUALITY', 'OTHER', 'SECURITY'],
        stageTypeFilters: ['release', 'stage-release', 'build'],
        tagFilters: ['tagId1', 'tagId2', null],
        applicationFilters: ['applicationIdZ', 'applicationIdA', 'applicationIdQ'],
        policyViolationStates: ['OPEN', 'WAIVED', 'GRANDFATHERED'],
        maxDaysOld: 90,
        minPolicyThreatLevel: 3,
        maxPolicyThreatLevel: 6
      };
      initState = {
        showAgeFilter: false,
        isViolationsTab: false,
        organizations: [
          {id: 'orgId1', name: 'OrganizationOne'},
          {id: 'orgId2', name: 'OrganizationTwo'},
          {id: 'noPermissionOrgId', name: 'No Permission'}
        ],
        applications: [
          {id: 'applicationIdZ', organizationId: 'orgId1'},
          {id: 'applicationIdA', organizationId: 'orgId2'},
          {id: 'applicationIdQ', organizationId: 'orgId2'},
          {id: 'applicationIdR', organizationId: 'orgId2'},
          {id: 'applicationIdS', organizationId: 'noPermissionOrgId'}
        ],
        categories: [
          {id: null, name: 'uncategorized applications'},
          {id: 'tagId1', name: 'TagOne'}
        ],
        appliedFilter: initSelected,
        selected: initSelected,
        other: otherObject
      };
    });

    describe('FETCH_LEGAL_CURRENT_FILTER_FULFILLED action', function() {

      beforeEach(function() {
        action = {
          type: 'FETCH_LEGAL_CURRENT_FILTER_FULFILLED',
          payload: {
            filter: filterJson,
            basedOnFilterName: 'Test1'
          }
        };
      });

      it('sets loading to false', function() {
        initState.loading = true;
        var state = Object.freeze(initState);
        var newState = legalDashboardFilterReducer(state, action);
        expect(newState.loading).toBe(false);
        expect(newState.other).toBe(otherObject); // other properties are not modified
      });

      it('sets needsAcknowledgement', function() {
        initState.needsAcknowledgement = false;
        action.payload.needsAcknowledgement = true;
        var state = Object.freeze(initState);
        var newState = legalDashboardFilterReducer(state, action);
        expect(newState.needsAcknowledgement).toBe(true);
        expect(newState.other).toBe(otherObject); // other properties are not modified
      });

      testApplyFilter();
    });

    describe('APPLY_LEGAL_FILTER_FULFILLED action', function() {
      beforeEach(function() {
        action = {
          type: 'APPLY_LEGAL_FILTER_FULFILLED',
          payload: {
            filter: filterJson,
            basedOnFilterName: 'Test1'
          }
        };
      });

      it('always resets needsAcknowledgement', function() {
        initState.needsAcknowledgement = true;
        var state = Object.freeze(initState);
        var newState = legalDashboardFilterReducer(state, action);
        expect(newState.needsAcknowledgement).toBe(false);
        expect(newState.other).toBe(otherObject); // other properties are not modified
      });

      testApplyFilter();
    });

    function testApplyFilter() {
      it('sets filtersAreDirty to false', function() {
        initState.filtersAreDirty = true;
        var state = Object.freeze(initState);
        var newState = legalDashboardFilterReducer(state, action);
        expect(newState.filtersAreDirty).toBe(false);
        expect(newState.other).toBe(otherObject); // other properties are not modified
      });

      it('sets selected and appliedFilter', function() {
        var state = Object.freeze(initState);
        var newState = legalDashboardFilterReducer(state, action);
        expect(newState.other).toBe(otherObject); // other properties are not modified
        expect(newState.selected.stages).toEqual(new Set(['release', 'stage-release', 'build']));

        // skips selected category ids that do not exist in vm.categories
        expect(newState.selected.categories).toEqual(new Set(['tagId1', null]));

        // removes orgs that are not visible (not in state.organizations)
        expect(newState.selected.organizations).toEqual(new Set(['orgId1', 'orgId2']));

        // adds missing applications for selected organization
        expect(newState.selected.applications).toEqual(
            new Set(['applicationIdZ', 'applicationIdA', 'applicationIdQ', 'applicationIdR']));

        // sets selected to appliedFilter
        expect(newState.selected).toBe(newState.appliedFilter);
      });

      it('does not set selected and appliedFilter if filter is not provided', function() {
        var noFilterAction = {
          type: action.type,
          payload: {}
        };
        var state = Object.freeze(initState);
        var newState = legalDashboardFilterReducer(state, noFilterAction);
        expect(newState.selected).toBe(initState.selected);
        expect(newState.appliedFilter).toBe(initState.appliedFilter);
        expect(newState.other).toBe(otherObject); // other properties are not modified
      });
    }
  });

  describe('APPLY_LEGAL_FILTER_FAILED action', function() {
    it('sets applyFilterError', function() {
      var state = Object.freeze({applyFilterError: null, other: otherObject});
      var action = {
        type: 'APPLY_LEGAL_FILTER_FAILED',
        payload: 'update filter error'
      };
      expect(state.applyFilterError).toBeNull();
      var newState = legalDashboardFilterReducer(state, action);
      expect(newState.applyFilterError).toBe('update filter error');
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('APPLY_LEGAL_FILTER_REQUESTED action', function() {
    it('resets applyFilterError and loadErrorFilterName', function() {
      var state = Object.freeze({
        applyFilterError: 'apply filter error',
        loadErrorFilterName: 'test filter',
        other: otherObject
      });
      var action = {
        type: 'APPLY_LEGAL_FILTER_REQUESTED'
      };
      var newState = legalDashboardFilterReducer(state, action);
      expect(newState).toEqual({
        applyFilterError: null,
        loadErrorFilterName: null,
        other: otherObject
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('TOGGLE_LEGAL_APPS_AND_ORGS action', function() {
    it('sets selected orgs and apps and sets filtersAreDirty to true', function() {
      var state = Object.freeze({
        other: otherObject,
        filtersAreDirty: false,
        selected: {}
      });
      var action = {
        type: 'TOGGLE_LEGAL_APPS_AND_ORGS',
        payload: {
          selectedOrganizations: new Set(['org1']),
          selectedApplications: new Set(['app1', 'app2'])
        }
      };
      var newState = legalDashboardFilterReducer(state, action);
      expect(newState.selected.organizations).toBe(action.payload.selectedOrganizations);
      expect(newState.selected.applications).toBe(action.payload.selectedApplications);
      expect(newState.filtersAreDirty).toBe(true);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('TOGGLE_LEGAL_FILTER action', function() {
    var initState;

    beforeEach(function() {
      initState = {
        other: otherObject,
        filtersAreDirty: false,
        appliedFilter: {},
        selected: {}
      };
    });

    it('sets selected categories and sets filtersAreDirty to true', function() {
      var state = Object.freeze(initState);
      var action = {
        type: 'TOGGLE_LEGAL_FILTER',
        payload: {
          filterName: 'categories',
          selectedIds: new Set(['cat1', 'cat2'])
        }
      };
      var newState = legalDashboardFilterReducer(state, action);
      expect(newState.selected.categories).toBe(action.payload.selectedIds);
      expect(newState.filtersAreDirty).toBe(true);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });

    it('sets selected stages and sets filtersAreDirty to true', function() {
      var state = Object.freeze(initState);
      var action = {
        type: 'TOGGLE_LEGAL_FILTER',
        payload: {
          filterName: 'stages',
          selectedIds: new Set(['stage1', 'stage2'])
        }
      };
      var newState = legalDashboardFilterReducer(state, action);
      expect(newState.selected.stages).toBe(action.payload.selectedIds);
      expect(newState.filtersAreDirty).toBe(true);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('REVERT_LEGAL_FILTER action', function() {
    it('sets selected filter to current appliedFilter, resets filtersAreDirty and loadErrorFilterName', function() {
      var state = Object.freeze({
        loadErrorFilterName: 'Test filter name',
        filtersAreDirty: true,
        appliedFilter: {
          organizations: new Set(['org1']),
          applications: new Set(['app1', 'org2']),
          categories: new Set(['cat1', 'cat2']),
          stages: new Set(['stage1', 'stage2'])
        },
        selected: {},
        other: otherObject
      });
      const newState = legalDashboardFilterReducer(state, {type: 'REVERT_LEGAL_FILTER'});
      expect(newState.loadErrorFilterName).toBeNull();
      expect(newState.filtersAreDirty).toBe(false);
      expect(newState.selected).toEqual(state.appliedFilter);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });
});
