/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global inject, beforeEach, it, describe, expect */
import successMetricsModule from '../../../../../main/frontend/labs/successMetrics/module';

describe('summaryStatementTileSpec', function() {

  beforeEach(function() {
    angular.mock.module(successMetricsModule.name);
  });

  var getVm,
      $rootScope,
      deleteDeferred,
      mockOnDelete,
      mockDeleteModalService,
      mockSuccessMetricsDataService;

  beforeEach(inject(function($q, _$rootScope_, $componentController) {
    $rootScope = _$rootScope_;
    deleteDeferred = $q.defer();
    mockOnDelete = jasmine.createSpy('onDelete');
    mockSuccessMetricsDataService = {
      deleteSuccessMetricsReport: jasmine.createSpy('deleteSuccessMetricsReport')
    };
    mockDeleteModalService = {
      deleteCustom: jasmine.createSpy('deleteCustom').and.returnValue(deleteDeferred.promise)
    };
    getVm = function(bindings) {
      return $componentController('summaryStatementTile', {
        DeleteModalService: mockDeleteModalService,
        successMetricsDataService: mockSuccessMetricsDataService
      }, bindings);
    };
  }));

  it('Initializes properly with monthly report with active applications', function() {
    var vm = getVm({
      activeApplicationCount: 1,
      successMetricsReport: { includeLatestData: false }
    });

    vm.$onInit();

    $rootScope.$digest();

    expect(vm.showNoDataMessage).toBe(false);
    expect(vm.dateFormat).toBe('mediumDate');
  });

  it('Initializes properly with "latest data" report without active applications', function() {
    var vm = getVm({
      activeApplicationCount: 0,
      successMetricsReport: { includeLatestData: true }
    });

    vm.$onInit();

    $rootScope.$digest();

    expect(vm.showNoDataMessage).toBe(true);
    expect(vm.dateFormat).toBe('medium');
  });

  it('Uses DeleteModalService correctly and invokes onDelete callback upon successful delete', function() {
    var vm = getVm({
      onDelete: mockOnDelete,
      activeApplicationCount: 1,
      successMetricsReport: { id: '1', name: 'foo' }
    });
    vm.$onInit();
    deleteDeferred.resolve();

    vm.delete();

    $rootScope.$digest();

    expect(mockDeleteModalService.deleteCustom).toHaveBeenCalledWith('Delete Report',
        jasmine.stringMatching('foo'), jasmine.any(String), jasmine.any(Function));
    mockDeleteModalService.deleteCustom.calls.argsFor(0)[3]();
    expect(mockSuccessMetricsDataService.deleteSuccessMetricsReport).toHaveBeenCalledWith('1');
    expect(mockOnDelete).toHaveBeenCalled();
  });
});
