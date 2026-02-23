/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reduce from '../../../../main/frontend/dashboard/filter/dashboardFilterReducer';
import { dashboardFilterOptionsTab } from 'MainRoot/dashboard/filter/staticFilterEntries';

import 'TestRoot/assets/MockData';

describe('dashboardFilterReducer', () => {
  let otherObject;

  beforeEach(() => {
    otherObject = { value: 'test value' };
  });

  describe('unknown action', () => {
    it('returns original state', () => {
      const state = Object.freeze({ foo: 'bar' });
      const newState = reduce(state, { type: 'UNKNOWN' });

      expect(newState).toBe(state);
    });
  });

  describe('initial state', () => {
    it('is used if no state is provided', () => {
      const newState = reduce(undefined, { type: 'UNKNOWN' });
      expect(newState).not.toBeUndefined();
    });
  });

  describe('SET_DISPLAY_SAVE_FILTER_MODAL action', () => {
    it('sets the showSaveFilterModal value to the payload', () => {
      const state = Object.freeze({
        showSaveFilterModal: true,
        other: otherObject,
      });
      const { showSaveFilterModal, other } = reduce(state, {
        type: 'SET_DISPLAY_SAVE_FILTER_MODAL',
        payload: false,
      });

      expect(showSaveFilterModal).toBe(false);
      expect(other).toBe(otherObject);
    });
  });

  describe('LOAD_FILTER_REQUESTED action', () => {
    it('sets loading to true and resets filter', () => {
      const state = Object.freeze({
        loadError: 'error',
        loading: false,
        other: otherObject,
      });
      const { loading, loadError, other } = reduce(state, { type: 'LOAD_FILTER_REQUESTED' });

      expect(loading).toBe(true);
      expect(loadError).toBeNull();
      expect(other).toBe(otherObject);
    });
  });

  describe('LOAD_FILTER_FAILED action', () => {
    it('sets loading to false and sets error', () => {
      const state = Object.freeze({
        loadError: null,
        loading: true,
        other: otherObject,
      });
      const { loading, loadError, other } = reduce(state, {
        type: 'LOAD_FILTER_FAILED',
        payload: 'load filter error',
      });

      expect(loadError).toBe('load filter error');
      expect(loading).toBe(false);
      expect(other).toBe(otherObject);
    });
  });

  describe('FETCH_AVAILABLE_FILTER_OPTIONS_FULFILLED action', () => {
    let initState, action;

    beforeEach(() => {
      initState = {
        loading: true,
        other: otherObject,
      };
      action = {
        type: 'FETCH_AVAILABLE_FILTER_OPTIONS_FULFILLED',
        payload: {
          organizations: [
            {
              id: 'orgId1',
              name: 'OrganizationOne',
            },
            {
              id: 'orgId2',
              name: 'OrganizationTwo',
            },
            {
              id: 'ROOT_ORGANIZATION_ID',
              name: 'Root Organization',
            },
          ],
          applications: [
            {
              id: 'applicationIdZ',
              publicId: 'applicationPublicIdZ',
              name: 'ApplicationZ <b style="woah" class=\'evenmorewoah\'>&nbsp;shouldnotbebold</b>',
              organizationId: 'orgId1',
            },
            {
              id: 'applicationIdA',
              publicId: 'applicationPublicIdA',
              name: 'ApplicationA',
              organizationId: 'orgId2',
            },
            {
              id: 'applicationIdQ',
              publicId: 'applicationPublicIdQ',
              name: 'ApplicationQ',
              organizationId: 'orgId2',
            },
            {
              id: 'applicationIdR',
              publicId: 'applicationPublicIdR',
              name: 'ApplicationR',
              organizationId: 'orgId2',
            },
            {
              id: 'applicationIdS',
              publicId: 'applicationPublicIdS',
              name: 'ApplicationS',
              organizationId: 'noPermissionOrgId',
              organizationName: 'No Permission',
            },
            {
              id: 'applicationIdS2',
              publicId: 'applicationPublicIdS2',
              name: 'ApplicationS2',
              organizationId: 'noPermissionOrgId',
              organizationName: 'No Permission',
            },
          ],
          categories: [
            {
              id: 'tagId1',
              organizationId: 'orgId1',
              name: 'TagOne',
              description: 'Tag One Description',
            },
            {
              id: 'tagId2',
              organizationId: 'orgId2',
              name: 'TagTwo',
              description: 'Tag Two Description',
            },
          ],
          stages: MockData.getDashboardStageData(),
          repositories: [
            { managerInstanceId: '12345-67890', repository: { id: 'id-foo', publicId: 'foo' } },
            { managerInstanceId: '12345-67890', repository: { id: 'id-bar', publicId: 'bar' } },
            { managerInstanceId: '12345-67890', repository: { id: 'id-foobar', publicId: 'foobar' } },
          ],
        },
      };
    });

    it('does not set loading to false', () => {
      const state = Object.freeze(initState);
      const { loading, other } = reduce(state, action);

      expect(loading).toBe(true);
      expect(other).toBe(otherObject);
    });

    it('sets available filter options', () => {
      const state = Object.freeze(initState);
      const { other, applications, stages, categories, repositories } = reduce(state, action);

      expect(other).toBe(otherObject);

      expect(applications.length).toBe(action.payload.applications.length);
      expect(applications[0].id).toBe(action.payload.applications[0].id);
      expect(applications[1].id).toBe(action.payload.applications[1].id);

      expect(stages.length).toBe(MockData.getDashboardStageData().length);
      expect(stages[0].id).toBe(MockData.getDashboardStageData()[0].stageTypeId);
      expect(stages[0].name).toBe(MockData.getDashboardStageData()[0].stageName);
      expect(stages[1].id).toBe(MockData.getDashboardStageData()[1].stageTypeId);
      expect(stages[1].name).toBe(MockData.getDashboardStageData()[1].stageName);

      // one extra for uncategorized applications
      expect(categories.length).toBe(action.payload.categories.length + 1);
      expect(categories[0].id).toBe(null);
      expect(categories[0].name).toBe('uncategorized applications');

      expect(categories[1].id).toBe(action.payload.categories[0].id);
      // populates owner
      expect(categories[1].owner).toBe(action.payload.organizations[0].name);

      // transform repositories data
      expect(repositories[0].name).toBe('foo - 12345');
      expect(repositories[0].fullName).toBe('foo - 12345-67890');
      expect(repositories[1].name).toBe('bar - 12345');
      expect(repositories[1].fullName).toBe('bar - 12345-67890');
      expect(repositories[2].name).toBe('foobar - 12345');
      expect(repositories[2].fullName).toBe('foobar - 12345-67890');
    });
  });

  describe('applyFilter', () => {
    let initState, action, filterJson, initSelected;

    beforeEach(() => {
      initSelected = Object.freeze({
        organizations: {},
        applications: {},
        categories: {},
        stages: {},
        policyTypes: {},
        policyWaiverReasons: {},
        policyViolationStates: { OPEN: true },
        maxDaysOld: 30,
        policyThreatLevels: [2, 10],
        expirationDate: 'ALL',
      });
      filterJson = {
        organizationFilters: ['orgId1', 'orgId2', 'org3'],
        policyThreatCategoryFilters: ['QUALITY', 'OTHER', 'SECURITY'],
        repositoryFilters: ['id-foo', 'id-bar', 'id-foobar'],
        stageTypeFilters: ['release', 'stage-release', 'build'],
        tagFilters: ['tagId1', 'tagId2', null],
        applicationFilters: ['applicationIdZ', 'applicationIdA', 'applicationIdQ'],
        policyViolationStates: ['OPEN', 'WAIVED', 'LEGACY_VIOLATION'],
        maxDaysOld: 90,
        minPolicyThreatLevel: 3,
        maxPolicyThreatLevel: 6,
      };
      initState = {
        showAgeFilter: false,
        isViolationsTab: false,
        organizations: [
          { id: 'orgId1', name: 'OrganizationOne' },
          { id: 'orgId2', name: 'OrganizationTwo' },
          { id: 'noPermissionOrgId', name: 'No Permission' },
        ],
        applications: [
          { id: 'applicationIdZ', organizationId: 'orgId1' },
          { id: 'applicationIdA', organizationId: 'orgId2' },
          { id: 'applicationIdQ', organizationId: 'orgId2' },
          { id: 'applicationIdR', organizationId: 'orgId2' },
          { id: 'applicationIdS', organizationId: 'noPermissionOrgId' },
        ],
        repositories: [
          { repository: { fullName: 'id-foo - 12345-67890', id: 'id-foo', publicId: 'foo - 12345' } },
          { repository: { fullName: 'id-bar - 12345-67890', id: 'id-bar', publicId: 'bar - 12345' } },
          { repository: { fullName: 'id-foobar - 12345-67890', id: 'id-foobar', publicId: 'foobar - 12345' } },
        ],
        ages: [
          { name: 'past 24 hours', id: 1 },
          { name: 'past 7 days', id: 7 },
          { name: 'past 30 days', id: 30 },
          { name: 'past 90 days', id: 90 },
          { name: 'past 12 months', id: 365 },
          { name: 'all time', id: null },
        ],
        categories: [
          { id: null, name: 'uncategorized applications' },
          { id: 'tagId1', name: 'TagOne' },
        ],
        expirationDates: [
          { id: 'ALL', name: 'all' },
          { id: 'IN_24_HOURS', name: 'in 24 hours' },
        ],
        appliedFilter: initSelected,
        selected: initSelected,
        other: otherObject,
      };
    });

    const testApplyFilter = () => {
      it('sets filtersAreDirty to false', () => {
        initState.filtersAreDirty = true;
        const state = Object.freeze(initState);
        const { filtersAreDirty, other } = reduce(state, action);

        expect(filtersAreDirty).toBe(false);
        expect(other).toBe(otherObject);
      });

      it('replaces GRANDFATHERED state to LEGACY_VIOLATION for backward compatibility', () => {
        filterJson.policyViolationStates = ['OPEN', 'WAIVED', 'GRANDFATHERED'];
        const state = Object.freeze(initState);
        const { appliedFilter } = reduce(state, action);
        expect(appliedFilter.policyViolationStates).toEqual(new Set(['OPEN', 'WAIVED', 'LEGACY_VIOLATION']));
      });

      it('sets selected and appliedFilter', () => {
        const state = Object.freeze(initState);
        const { other, selected, appliedFilter } = reduce(state, action);

        expect(other).toBe(otherObject);

        expect(selected.policyTypes).toEqual(new Set(['QUALITY', 'OTHER', 'SECURITY']));
        expect(selected.stages).toEqual(new Set(['release', 'stage-release', 'build']));

        // skips selected category ids that do not exist in vm.categories
        expect(selected.categories).toEqual(new Set(['tagId1', null]));

        // removes orgs that are not visible (not in state.organizations)
        expect(selected.organizations).toEqual(new Set(['orgId1', 'orgId2']));

        // adds missing applications for selected organization
        expect(selected.applications).toEqual(
          new Set(['applicationIdZ', 'applicationIdA', 'applicationIdQ', 'applicationIdR'])
        );

        expect(selected.policyViolationStates).toEqual(new Set(['OPEN', 'WAIVED', 'LEGACY_VIOLATION']));

        expect(selected.maxDaysOld).toBe(90);
        expect(selected.policyThreatLevels).toEqual([3, 6]);

        // sets selected to appliedFilter
        expect(selected).toBe(appliedFilter);
      });

      it('sets selected age to default if filter maxDaysOld value is not recognised', () => {
        filterJson.maxDaysOld = 666;
        const state = Object.freeze(initState);
        const { selected } = reduce(state, action);

        expect(selected.maxDaysOld).toBe(30);
      });

      it('does not set selected and appliedFilter if filter is not provided', () => {
        const state = Object.freeze(initState);
        const { selected, appliedFilter, other } = reduce(state, {
          type: action.type,
          payload: {},
        });
        expect(selected).toBe(initState.selected);
        expect(appliedFilter).toBe(initState.appliedFilter);

        expect(other).toBe(otherObject);
      });
    };

    describe('FETCH_CURRENT_FILTER_FULFILLED action', () => {
      beforeEach(() => {
        action = {
          type: 'FETCH_CURRENT_FILTER_FULFILLED',
          payload: {
            filter: filterJson,
            basedOnFilterName: 'Test1',
          },
        };
      });

      it('sets loading to false', () => {
        initState.loading = true;
        const state = Object.freeze(initState);
        const { loading, other } = reduce(state, action);

        expect(loading).toBe(false);
        expect(other).toBe(otherObject);
      });

      it('sets needsAcknowledgement', () => {
        initState.needsAcknowledgement = false;
        action.payload.needsAcknowledgement = true;
        const state = Object.freeze(initState);
        const { needsAcknowledgement, other } = reduce(state, action);

        expect(needsAcknowledgement).toBe(true);
        expect(other).toBe(otherObject);
      });

      it('sets filterSidebarOpen to true if needsAcknowledgement', () => {
        initState.filterSidebarOpen = false;
        action.payload.needsAcknowledgement = true;
        const state = Object.freeze(initState);
        const { filterSidebarOpen, other } = reduce(state, action);

        expect(filterSidebarOpen).toBe(true);
        expect(other).toBe(otherObject);
      });

      testApplyFilter();
    });

    describe('APPLY_FILTER_FULFILLED action', () => {
      beforeEach(() => {
        action = {
          type: 'APPLY_FILTER_FULFILLED',
          payload: {
            filter: filterJson,
            basedOnFilterName: 'Test1',
          },
        };
      });

      it('always resets needsAcknowledgement', () => {
        initState.needsAcknowledgement = true;
        const state = Object.freeze(initState);
        const { needsAcknowledgement, other } = reduce(state, action);

        expect(needsAcknowledgement).toBe(false);
        expect(other).toBe(otherObject);
      });

      testApplyFilter();
    });
  });

  describe('APPLY_FILTER_FAILED action', () => {
    it('sets applyFilterError', () => {
      const state = Object.freeze({ applyFilterError: null, other: otherObject });

      expect(state.applyFilterError).toBeNull();

      const { applyFilterError, other } = reduce(state, {
        type: 'APPLY_FILTER_FAILED',
        payload: 'update filter error',
      });

      expect(applyFilterError).toBe('update filter error');
      expect(other).toBe(otherObject);
    });
  });

  describe('APPLY_FILTER_CANCELLED action', () => {
    it('resets applyFilterError', () => {
      const state = Object.freeze({
        applyFilterError: 'Error',
        other: otherObject,
      });

      expect(state.applyFilterError).toBe('Error');

      const { applyFilterError, other } = reduce(state, {
        type: 'APPLY_FILTER_CANCELLED',
      });
      expect(applyFilterError).toBeNull();
      expect(other).toBe(otherObject);
    });
  });

  describe('APPLY_FILTER_REQUESTED action', () => {
    it('resets applyFilterError and loadErrorFilterName', () => {
      const state = Object.freeze({
        applyFilterError: 'apply filter error',
        loadErrorFilterName: 'test filter',
        other: otherObject,
      });
      const newState = reduce(state, { type: 'APPLY_FILTER_REQUESTED' });
      expect(newState).toEqual({
        applyFilterError: null,
        loadErrorFilterName: null,
        other: otherObject,
      });
      expect(newState.other).toBe(otherObject);
    });
  });

  describe('APPLY_SAVED_FILTER_FAILED action', () => {
    it('sets loadErrorFilterName to payload', () => {
      const state = Object.freeze({
        loadErrorFilterName: null,
        other: otherObject,
      });

      const { loadErrorFilterName, other } = reduce(state, {
        type: 'APPLY_SAVED_FILTER_FAILED',
        payload: 'test filter name',
      });
      expect(loadErrorFilterName).toBe('test filter name');
      expect(other).toBe(otherObject);
    });
  });

  describe('DISPLAY_DELETE_FILTER_MODAL', () => {
    it('sets showDeleteFilterModal to true', () => {
      const state = Object.freeze({
        showDeleteFilterModal: false,
        showSaveFilterModal: true,
      });
      const newState = reduce(state, { type: 'DISPLAY_DELETE_FILTER_MODAL' });
      expect(newState.showDeleteFilterModal).toBe(true);
      expect(newState.showSaveFilterModal).toBe(false);
    });
  });

  describe('HIDE_DELETE_FILTER_MODAL', () => {
    it('sets showDeleteFilterModal to false', () => {
      const state = Object.freeze({
        showDeleteFilterModal: true,
      });
      const newState = reduce(state, { type: 'HIDE_DELETE_FILTER_MODAL' });
      expect(newState.showDeleteFilterModal).toBe(false);
    });
  });

  describe('@@reduxUiRouter/onFinish action', () => {
    let initState;
    const NxTabs = Object.entries(dashboardFilterOptionsTab);
    beforeEach(() => {
      initState = {
        showAgeFilter: false,
        showStagesFilter: false,
        showViolationStateFilter: false,
        showExpirationDateFilter: false,
        showRepositoriesFilter: false,
        selected: {
          maxDaysOld: 30,
        },
        other: otherObject,
      };
    });

    describe('showAgeFilter, showStagesFilter, showViolationStateFilter, showExpirationDateFilter, showRepositoriesFilter', () => {
      NxTabs.forEach((tab) => {
        const [route, filterValues] = tab;
        it(`is set to ${filterValues.showAgeFilter}, ${filterValues.showStagesFilter}, ${filterValues.showViolationStateFilter} ${filterValues.showRepositoriesFilter} and ${filterValues.showExpirationDateFilter} if the route is ${route}`, () => {
          const state = Object.freeze(initState);

          expect(state.showAgeFilter).toBe(false);
          expect(state.showStagesFilter).toBe(false);
          expect(state.showViolationStateFilter).toBe(false);
          expect(state.showExpirationDateFilter).toBe(false);
          expect(state.showRepositoriesFilter).toBe(false);

          const {
            showAgeFilter,
            showStagesFilter,
            showViolationStateFilter,
            showExpirationDateFilter,
            showRepositoriesFilter,
            other,
          } = reduce(state, {
            type: '@@reduxUiRouter/onFinish',
            payload: {
              toState: {
                name: route,
              },
              toParams: {},
            },
          });

          expect(showAgeFilter).toBe(filterValues.showAgeFilter);
          expect(showStagesFilter).toBe(filterValues.showStagesFilter);
          expect(showViolationStateFilter).toBe(filterValues.showViolationStateFilter);
          expect(showExpirationDateFilter).toBe(filterValues.showExpirationDateFilter);
          expect(showRepositoriesFilter).toBe(filterValues.showRepositoriesFilter);
          expect(other).toBe(otherObject);
        });
      });
    });
  });

  describe('TOGGLE_APPS_AND_ORGS action', () => {
    it('sets selected orgs and apps and sets filtersAreDirty to true', () => {
      const state = Object.freeze({
        other: otherObject,
        filtersAreDirty: false,
        selected: {},
      });
      const action = {
        type: 'TOGGLE_APPS_AND_ORGS',
        payload: {
          selectedOrganizations: new Set(['org1']),
          selectedApplications: new Set(['app1', 'app2']),
        },
      };
      const { selected, filtersAreDirty, other } = reduce(state, action);

      expect(selected.organizations).toBe(action.payload.selectedOrganizations);
      expect(selected.applications).toBe(action.payload.selectedApplications);
      expect(filtersAreDirty).toBe(true);
      expect(other).toBe(otherObject);
    });
  });

  describe('TOGGLE_REPOSITORIES action', () => {
    it('sets selected repositories and sets filtersAreDirty to true', () => {
      const state = Object.freeze({
        other: otherObject,
        filtersAreDirty: false,
        selected: {},
      });
      const action = {
        type: 'TOGGLE_REPOSITORIES',
        payload: new Set(['repo1', 'repo2']),
      };
      const { selected, filtersAreDirty, other } = reduce(state, action);

      expect(selected.repositories).toBe(action.payload);
      expect(filtersAreDirty).toBe(true);
      expect(other).toBe(otherObject);
    });
  });

  describe('SELECT_AGE action', () => {
    let initState;

    beforeEach(() => {
      initState = {
        other: otherObject,
        filtersAreDirty: false,
        ages: [
          { name: 'past 24 hours', id: 1 },
          { name: 'past 12 months', id: 365 },
          { name: 'all time', id: null },
        ],
        selected: {
          maxDaysOld: 365,
        },
      };
    });

    it('sets selected age and sets filtersAreDirty to true', () => {
      const state = Object.freeze(initState);
      const { selected, filtersAreDirty, other } = reduce(state, {
        type: 'SELECT_AGE',
        payload: 1,
      });

      expect(selected.maxDaysOld).toBe(1);
      expect(filtersAreDirty).toBe(true);
      expect(other).toBe(otherObject);
    });

    it('sets selected age to "all time" sets filtersAreDirty to true', () => {
      const state = Object.freeze(initState);
      const { selected, filtersAreDirty, other } = reduce(state, {
        type: 'SELECT_AGE',
        payload: null,
      });

      expect(selected.maxDaysOld).toBe(null);
      expect(filtersAreDirty).toBe(true);
      expect(other).toBe(otherObject);
    });
  });

  describe('SELECT_EXPIRATION_DATE action', () => {
    let initState;

    beforeEach(() => {
      initState = {
        other: otherObject,
        filtersAreDirty: false,
        expirationDates: [
          {
            id: 'ALL',
            name: 'all',
          },
          {
            id: 'IN_7_DAYS',
            name: 'in 7 days',
          },
        ],
        selected: {
          expirationDate: 'ALL',
        },
      };
    });

    it('sets selected expirationDate and sets filtersAreDirty to true', () => {
      const state = Object.freeze(initState);
      const { selected, filtersAreDirty, other } = reduce(state, {
        type: 'SELECT_EXPIRATION_DATE',
        payload: 'IN_7_DAYS',
      });

      expect(selected.expirationDate).toBe('IN_7_DAYS');
      expect(filtersAreDirty).toBe(true);
      expect(other).toBe(otherObject);
    });

    it('sets selected expirationDate to "all" sets filtersAreDirty to true', () => {
      const state = Object.freeze(initState);
      const { selected, filtersAreDirty, other } = reduce(state, {
        type: 'SELECT_EXPIRATION_DATE',
        payload: null,
      });

      expect(selected.expirationDate).toBe('ALL');
      expect(filtersAreDirty).toBe(true);
      expect(other).toBe(otherObject);
    });
  });

  describe('TOGGLE_FILTER action', () => {
    let initState;

    beforeEach(() => {
      initState = {
        other: otherObject,
        filtersAreDirty: false,
        appliedFilter: {},
        selected: {},
      };
    });

    it('sets selected categories and sets filtersAreDirty to true', () => {
      const state = Object.freeze(initState);
      const action = {
        type: 'TOGGLE_FILTER',
        payload: {
          filterName: 'categories',
          selectedIds: new Set(['cat1', 'cat2']),
        },
      };
      const { selected, filtersAreDirty, other } = reduce(state, action);

      expect(selected.categories).toBe(action.payload.selectedIds);
      expect(filtersAreDirty).toBe(true);
      expect(other).toBe(otherObject);
    });

    it('sets selected stages and sets filtersAreDirty to true', () => {
      const state = Object.freeze(initState);
      const action = {
        type: 'TOGGLE_FILTER',
        payload: {
          filterName: 'stages',
          selectedIds: new Set(['stage1', 'stage2']),
        },
      };
      const { selected, filtersAreDirty, other } = reduce(state, action);

      expect(selected.stages).toBe(action.payload.selectedIds);
      expect(filtersAreDirty).toBe(true);
      expect(other).toBe(otherObject);
    });

    it('sets selected policyTypes and sets filtersAreDirty to true', () => {
      const state = Object.freeze(initState);
      const action = {
        type: 'TOGGLE_FILTER',
        payload: {
          filterName: 'policyTypes',
          selectedIds: new Set(['SECURITY', 'LICENSE']),
        },
      };
      const { selected, filtersAreDirty, other } = reduce(state, action);

      expect(selected.policyTypes).toBe(action.payload.selectedIds);
      expect(filtersAreDirty).toBe(true);
      expect(other).toBe(otherObject);
    });

    it('sets selected policyViolationStates and sets filtersAreDirty to true', () => {
      const state = Object.freeze(initState);
      const action = {
        type: 'TOGGLE_FILTER',
        payload: {
          filterName: 'policyViolationStates',
          selectedIds: new Set(['OPEN', 'WAIVED']),
        },
      };
      const { selected, filtersAreDirty, other } = reduce(state, action);

      expect(selected.policyViolationStates).toBe(action.payload.selectedIds);
      expect(filtersAreDirty).toBe(true);
      expect(other).toBe(otherObject);
    });

    it('sets selected policyThreatLevels and sets filtersAreDirty to true', () => {
      const state = Object.freeze(initState);
      const action = {
        type: 'TOGGLE_FILTER',
        payload: {
          filterName: 'policyThreatLevels',
          selectedIds: [3, 8],
        },
      };
      const { selected, filtersAreDirty, other } = reduce(state, action);

      expect(selected.policyThreatLevels).toEqual([3, 8]);
      expect(filtersAreDirty).toBe(true);
      expect(other).toBe(otherObject);
    });
  });

  describe('REVERT_FILTER action', () => {
    it('sets selected filter to current appliedFilter, resets filtersAreDirty and loadErrorFilterName', () => {
      const state = Object.freeze({
        loadErrorFilterName: 'Test filter name',
        filtersAreDirty: true,
        appliedFilter: {
          organizations: new Set(['org1']),
          applications: new Set(['app1', 'org2']),
          categories: new Set(['cat1', 'cat2']),
          stages: new Set(['stage1', 'stage2']),
          policyTypes: new Set(['SECURITY', 'LICENSE']),
          policyWaiverReasons: new Set(),
          policyViolationStates: new Set(['OPEN', 'WAIVED']),
          maxDaysOld: 365,
          policyThreatLevels: [3, 8],
          expirationDate: 'ALL',
        },
        selected: {},
        other: otherObject,
      });
      const { loadErrorFilterName, filtersAreDirty, selected, other } = reduce(state, { type: 'REVERT_FILTER' });

      expect(loadErrorFilterName).toBeNull();
      expect(filtersAreDirty).toBe(false);
      expect(selected).toEqual(state.appliedFilter);
      expect(other).toBe(otherObject);
    });
  });

  describe('TOGGLE_FILTER_SIDEBAR action', () => {
    it('sets filterSidebarOpen to payload', () => {
      const state = Object.freeze({
        filterSidebarOpen: true,
        other: otherObject,
      });
      const newState = reduce(state, {
        type: 'TOGGLE_FILTER_SIDEBAR',
        payload: true,
      });
      expect(newState.filterSidebarOpen).toBe(true);
      expect(newState.other).toBe(otherObject); // other properties are not modified

      expect(reduce(newState, { type: 'TOGGLE_FILTER_SIDEBAR', payload: false }).filterSidebarOpen).toBe(false);
    });

    describe('when filters are dirty', () => {
      it('does not close filter sidebar', () => {
        const state = Object.freeze({
          filterSidebarOpen: true,
          filtersAreDirty: true,
          other: otherObject,
        });
        const newState = reduce(state, {
          type: 'TOGGLE_FILTER_SIDEBAR',
          payload: false,
        });
        expect(newState.filterSidebarOpen).toBe(true);
        expect(newState.other).toBe(otherObject); // other properties are not modified
      });

      it('opens filter sidebar', () => {
        const state = Object.freeze({
          filterSidebarOpen: false,
          filtersAreDirty: true,
          other: otherObject,
        });
        const newState = reduce(state, {
          type: 'TOGGLE_FILTER_SIDEBAR',
          payload: true,
        });
        expect(newState.filterSidebarOpen).toBe(true);
        expect(newState.other).toBe(otherObject); // other properties are not modified
      });
    });

    describe('when needsAcknowledgement is true', () => {
      it('does not close filter sidebar', () => {
        const state = Object.freeze({
          filterSidebarOpen: true,
          needsAcknowledgement: true,
          other: otherObject,
        });
        const newState = reduce(state, {
          type: 'TOGGLE_FILTER_SIDEBAR',
          payload: false,
        });
        expect(newState.filterSidebarOpen).toBe(true);
        expect(newState.other).toBe(otherObject); // other properties are not modified
      });

      it('opens filter sidebar', () => {
        const state = Object.freeze({
          filterSidebarOpen: false,
          needsAcknowledgement: true,
          other: otherObject,
        });
        const newState = reduce(state, {
          type: 'TOGGLE_FILTER_SIDEBAR',
          payload: true,
        });
        expect(newState.filterSidebarOpen).toBe(true);
        expect(newState.other).toBe(otherObject); // other properties are not modified
      });
    });
  });
});
