/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import dashboardResultsModule from '../../../../main/frontend/dashboard/results/module';

describe('dashboardResultsContainer', function() {
  let vm;

  beforeEach(angular.mock.module(dashboardResultsModule.name, function($provide) {
    SpecUtil.mockNgRedux($provide);
  }));

  beforeEach(inject(function($componentController) {
    vm = $componentController('dashboardResultsContainer');
  }));

  describe('$onDestroy()', function() {
    it('unsubscribes from redux store', function() {
      vm.$onInit();
      expect(vm.unsubscribe).not.toHaveBeenCalled();
      vm.$onDestroy();
      expect(vm.unsubscribe).toHaveBeenCalledTimes(1);
    });
  });

  describe('$onInit()', function() {
    it('calls loadFilter action', function() {
      vm.$onInit();
      expect(vm.loadFilter).toHaveBeenCalled();
    });
  });

  describe('isFilterLoaded()', function() {
    it('is true when not loading and no load error', function() {
      vm.filterLoading = false;
      vm.loadFilterError = null;
      expect(vm.isFilterLoaded()).toBe(true);
    });

    it('is false when loading', function() {
      vm.filterLoading = true;
      vm.loadFilterError = null;
      expect(vm.isFilterLoaded()).toBe(false);
    });

    it('is false when load error', function() {
      vm.filterLoading = false;
      vm.loadFilterError = 'load error';
      expect(vm.isFilterLoaded()).toBe(false);
    });

    it('is false when loading and  load error', function() {
      vm.filterLoading = true;
      vm.loadFilterError = 'load error';
      expect(vm.isFilterLoaded()).toBe(false);
    });
  });
});
