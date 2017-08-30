/* global describe, beforeEach, it, expect, inject, Plottable */
describe('summary-statement-tile component', function() {
  beforeEach(module('successMetricsModule'));

  var getVm,
      $q,
      $rootScope;

  beforeEach(inject(function($componentController, _$q_, _$rootScope_) {
    getVm = function(mockSuccessMetricsDataService) {
      return $componentController('summaryStatementTile', { successMetricsDataService: mockSuccessMetricsDataService });
    };
    $q = _$q_;
    $rootScope = _$rootScope_;
  }));

  it('sets averagesData once the data is loaded', function() {
    var dataDeferred = $q.defer(),
        mockSuccessMetricsDataService = {
          getAveragesData: function() {
            return dataDeferred.promise;
          }
        },
        vm = getVm(mockSuccessMetricsDataService);

    expect(vm.averagesData).toBeUndefined();

    dataDeferred.resolve({
      activeApplicationCount: 3,
      totalEvaluations: 5,
      averagePolicyViolations: 5,
      averageCriticalPolicyViolations: 5,
      monthCount: 1,
      averageDiscoveredPolicyViolations: [
        {
          evaluationCount: 5,
          security: {
            low: 5,
            moderate: 8,
            severe: 2,
            critical: 0
          },
          license: {
            low: 0,
            moderate: 2,
            severe: 3,
            critical: 7
          },
          quality: {
            low: 15,
            moderate: 0,
            severe: 1,
            critical: 0
          },
          other: {
            low: 2,
            moderate: 2,
            severe: 2,
            critical: 2
          }
        }
      ]
    });

    $rootScope.$digest();

    expect(vm.averagesData).toBeDefined();
    expect(vm.averagesData.activeApplicationCount).toBe(3);
    expect(vm.averagesData.totalEvaluations).toBe(5);
    expect(vm.averagesData.averagePolicyViolations).toBe(5);
    expect(vm.averagesData.averageCriticalPolicyViolations).toBe(5);
    expect(vm.averagesData.monthCount).toBe(1);
  });
  
  it('clears any error message when doLoad is called', function() {
    var mockSuccessMetricsDataService = {
          getAveragesData: function() {
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
