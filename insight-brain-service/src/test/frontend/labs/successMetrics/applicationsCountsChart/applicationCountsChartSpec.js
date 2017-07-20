/* global describe, beforeEach, it, expect, inject, Plottable */
describe('application-counts-chart component', function() {
  beforeEach(module('successMetricsModule', 'legacyConfiguration'));

  var getVm,
      $q,
      $rootScope;

  beforeEach(inject(function($componentController, _$q_, _$rootScope_) {
    getVm = function(mockSuccessMetricsDataService) {
      return $componentController('applicationCountsChart', { successMetricsDataService: mockSuccessMetricsDataService });
    };
    $q = _$q_;
    $rootScope = _$rootScope_;
  }));

  it('sets the numeric values from the data returned by the successMetricsDataService', function() {
    var mockSuccessMetricsDataService = {
          getApplicationCountsData: function() {
            return $q.resolve({
              totalApplications: 5,
              activeApplications: 4,
              total: {
                applicationsWithViolations: 3,
                applicationsWithCriticalViolations: 2
              },
              security: {
                applicationsWithViolations: 2,
                applicationsWithCriticalViolations: 2
              },
              license: {
                applicationsWithViolations: 1,
                applicationsWithCriticalViolations: 1
              },
              quality: {
                applicationsWithViolations: 1,
                applicationsWithCriticalViolations: 0
              },
              other: {
                applicationsWithViolations: 0,
                applicationsWithCriticalViolations: 0
              }
            });
          }
        },
        vm = getVm(mockSuccessMetricsDataService);

    $rootScope.$digest();

    expect(vm.applicationCount).toBe(4);
    expect(vm.applicationCountSecurity).toBe(2);
    expect(vm.applicationCountLicense).toBe(1);
    expect(vm.applicationCountQuality).toBe(1);
    expect(vm.applicationCountOther).toBe(0);
    expect(vm.applicationCountTotalViolating).toBe(3);
    expect(vm.applicationCountSecurityCritical).toBe(2);
    expect(vm.applicationCountLicenseCritical).toBe(1);
    expect(vm.applicationCountQualityCritical).toBe(0);
    expect(vm.applicationCountOtherCritical).toBe(0);
    expect(vm.applicationCountTotalViolatingCritical).toBe(2);
  });

  it('sets the error message and rejects the vm.chart promise if the data promise is rejected', function() {
    var mockSuccessMetricsDataService = {
          getApplicationCountsData: function() {
            return $q.reject('error message');
          }
        },
        vm = getVm(mockSuccessMetricsDataService);

    $rootScope.$digest();

    expect(vm.error).toEqual('error message');
  });

  it('clears any error message when doLoad is called', function() {
    var mockSuccessMetricsDataService = {
          getApplicationCountsData: function() {
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
