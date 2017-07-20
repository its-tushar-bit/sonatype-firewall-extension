/* global describe, beforeEach, it, expect, inject, Plottable */
describe('mttr-chart component', function() {
  beforeEach(module('successMetricsModule', 'legacyConfiguration'));

  var getVm,
      $q,
      $rootScope;

  beforeEach(inject(function($componentController, _$q_, _$rootScope_) {
    getVm = function(mockSuccessMetricsDataService) {
      return $componentController('mttrChart', {successMetricsDataService: mockSuccessMetricsDataService});
    };
    $q = _$q_;
    $rootScope = _$rootScope_;
  }));

  it('loads mttr chart without errors', function() {
    var mockSuccessMetricsDataService = {
          getMttrData: function() {
            return $q.resolve([
              {"timePeriodStart": 1483254000000, "mttrInSeconds": null, "criticalMttrInSeconds": null},
              {"timePeriodStart": 1485932400000, "mttrInSeconds": 1209714, "criticalMttrInSeconds": 1209714},
              {"timePeriodStart": 1488351600000, "mttrInSeconds": 484000, "criticalMttrInSeconds": 484000}
            ]);
          }
        },
        vm = getVm(mockSuccessMetricsDataService);

    expect(vm.isLoaded).toBeUndefined();

    $rootScope.$digest();

    expect(vm.isLoaded).toBeTruthy();
    expect(vm.mttrChart).toBeDefined();
    expect(vm.error).toBeUndefined();
  });

  it('sets the error message and rejects the vm.chart promise if the data promise is rejected', function() {
    var mockSuccessMetricsDataService = {
          getMttrData: function() {
            return $q.reject('error message');
          }
        },
        vm = getVm(mockSuccessMetricsDataService);

    $rootScope.$digest();

    expect(vm.error).toEqual('error message');
  });

  it('clears any error message when doLoad is called', function() {
    var mockSuccessMetricsDataService = {
          getMttrData: function() {
            return $q.reject('error message');
          }
        },
        vm = getVm(mockSuccessMetricsDataService);

    $rootScope.$digest();

    expect(vm.error).toBeDefined();

    vm.doLoad();

    expect(vm.error).toBeUndefined();
  });
});
