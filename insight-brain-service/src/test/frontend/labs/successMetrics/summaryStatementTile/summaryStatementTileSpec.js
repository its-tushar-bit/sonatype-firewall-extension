/* global inject, beforeEach, it, describe, expect */
describe('summaryStatementTileSpec', function() {

  beforeEach(function() {
    module('utility.services');
    module('successMetricsModule');
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

  it('Initializes properly with active applications', function() {
    var vm = getVm({ averagesData: { activeApplicationCount: 1 } });
    vm.$onInit();

    $rootScope.$digest();

    expect(vm.showNoDataMessage).toBe(false);
  });

  it('Initializes properly without active applications', function() {
    var vm = getVm({ averagesData: { activeApplicationCount: 0 } });
    vm.$onInit();

    $rootScope.$digest();

    expect(vm.showNoDataMessage).toBe(true);
  });

  it('Uses DeleteModalService correctly and invokes onDelete callback upon successful delete', function() {
    var vm = getVm({
      onDelete: mockOnDelete,
      averagesData: { activeApplicationCount: 1 },
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
