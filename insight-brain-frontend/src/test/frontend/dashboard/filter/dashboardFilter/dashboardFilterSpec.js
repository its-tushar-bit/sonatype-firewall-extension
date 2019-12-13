/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import dashboardFilterModule from '../../../../../main/frontend/dashboard/filter/module';

describe('dashboard.filter.controller', function() {

  var vm, $componentController;

  beforeEach(angular.mock.module(dashboardFilterModule.name, function($provide) {
    SpecUtil.mockNgRedux($provide);
  }));

  beforeEach(inject(function(_$componentController_) {
    $componentController = _$componentController_;

    vm = $componentController('dashboardFilter');
    vm.$onInit();
  }));

  describe('applyCurrentFilter()', function() {
    var filterJson = {
          organizationFilters: ['orgId1', 'orgId2'],
          applicationFilters: ['applicationIdZ', 'applicationIdA', 'applicationIdQ'],
          policyThreatCategoryFilters: ['QUALITY', 'OTHER', 'SECURITY'],
          stageTypeFilters: ['release', 'stage-release', 'build'],
          tagFilters: ['tagId1', 'tagId2', null],
          policyViolationStates: ['OPEN', 'WAIVED'],
          maxDaysOld: 90,
          minPolicyThreatLevel: 3,
          maxPolicyThreatLevel: 6
        },
        selected = {
          organizations: new Set(['orgId1', 'orgId2']),
          applications: new Set(['applicationIdZ', 'applicationIdA', 'applicationIdQ']),
          policyTypes: new Set(['QUALITY', 'OTHER', 'SECURITY']),
          stages: new Set(['release', 'stage-release', 'build']),
          categories: new Set(['tagId1', 'tagId2', null]),
          policyViolationStates: new Set(['OPEN', 'WAIVED']),
          maxDaysOld: 90,
          policyThreatLevels: [3, 6]
        };

    it('calls applyFilter action if filtersAreDirty', function() {
      vm.filtersAreDirty = true;
      vm.needsAcknowledgement = false;
      vm.appliedFilterName = 'current filter name';
      vm.selected = selected;
      vm.applyCurrentFilter();
      expect(vm.applyFilter).toHaveBeenCalledWith(filterJson, 'current filter name');
    });

    it('calls applyFilter action if filtersAreDirty is false but needsAcknowledgement', function() {
      vm.filtersAreDirty = false;
      vm.needsAcknowledgement = true;
      vm.appliedFilterName = 'current filter name';
      vm.selected = selected;
      vm.applyCurrentFilter();
      expect(vm.applyFilter).toHaveBeenCalledWith(filterJson, 'current filter name');
    });

    it('does not call applyFilter action if filters are not dirty and needsAcknowledgement is false', function() {
      vm.filtersAreDirty = false;
      vm.needsAcknowledgement = false;
      vm.appliedFilterName = 'current filter name';
      vm.selected = selected;
      vm.applyCurrentFilter();
      expect(vm.applyFilter).not.toHaveBeenCalled();
    });
  });

  describe('$onInit', function() {
    it('fired loadFilter action', function() {
      var myVm = $componentController('dashboardFilter');
      myVm.$onInit();
      expect(myVm.loadFilter).toHaveBeenCalled();
    });
  });

  describe('$onDestroy()', function() {
    it('unsubscribes from redux store', function() {
      expect(vm.unsubscribe).not.toHaveBeenCalled();
      vm.$onDestroy();
      expect(vm.unsubscribe).toHaveBeenCalledTimes(1);
    });
  });
});
