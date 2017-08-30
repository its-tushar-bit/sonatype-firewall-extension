/* global describe, beforeEach, it, expect, inject, Plottable */
describe('violation-averages-chart component', function() {
  beforeEach(module('successMetricsModule', 'legacyConfiguration'));

  var getVm,
      $q,
      $rootScope;

  beforeEach(inject(function($componentController, _$q_, _$rootScope_) {
    getVm = function(mockSuccessMetricsDataService) {
      return $componentController('violationAveragesChart', { successMetricsDataService: mockSuccessMetricsDataService });
    };
    $q = _$q_;
    $rootScope = _$rootScope_;
  }));

  it('sets the numeric values from the data returned by the successMetricsDataService', inject(function($q) {
    var mockSuccessMetricsDataService = {
          getAveragesData: function() {
            return $q.resolve({
              security: {
                averageDiscoveredLow: 5,
                averageDiscoveredModerate: 8,
                averageDiscoveredSevere: 2,
                averageDiscoveredCritical: 0
              },
              license: {
                averageDiscoveredLow: 0,
                averageDiscoveredModerate: 2,
                averageDiscoveredSevere: 3,
                averageDiscoveredCritical: 7
              },
              quality: {
                averageDiscoveredLow: 15,
                averageDiscoveredModerate: 0,
                averageDiscoveredSevere: 1,
                averageDiscoveredCritical: 0
              },
              other: {
                averageDiscoveredLow: 2,
                averageDiscoveredModerate: 2,
                averageDiscoveredSevere: 2,
                averageDiscoveredCritical: 2
              }
            });
          }
        },
        vm = getVm(mockSuccessMetricsDataService);

    $rootScope.$digest();

    expect(vm.averageDiscoveredSecurity).toEqual(15);
    expect(vm.averageDiscoveredLicense).toEqual(12);
    expect(vm.averageDiscoveredQuality).toEqual(16);
    expect(vm.averageDiscoveredOther).toEqual(8);

    expect(vm.averageDiscoveredSecurityCritical).toEqual(0);
    expect(vm.averageDiscoveredLicenseCritical).toEqual(7);
    expect(vm.averageDiscoveredQualityCritical).toEqual(0);
    expect(vm.averageDiscoveredOtherCritical).toEqual(2);
  }));

  it('sets vm.chart to a promise of a component', function() {
    var mockSuccessMetricsDataService = {
          getAveragesData: function() {
            return $q.resolve({
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
            });
          }
        },
        vm = getVm(mockSuccessMetricsDataService),
        chart;

    vm.chart.then(function(c) {
      chart = c;
    });

    $rootScope.$digest();

    expect(chart instanceof Plottable.Component).toEqual(true);
  });

  it('sets isLoaded once the data is loaded', function() {
    var dataDeferred = $q.defer(),
        mockSuccessMetricsDataService = {
          getAveragesData: function() {
            return dataDeferred.promise;
          }
        },
        vm = getVm(mockSuccessMetricsDataService);

    expect(vm.isLoaded).toEqual(false);

    dataDeferred.resolve({
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
    });

    $rootScope.$digest();

    expect(vm.isLoaded).toEqual(true);
  });

  it('sets the error message and rejects the vm.chart promise if the data promise is rejected', function() {
    var mockSuccessMetricsDataService = {
          getAveragesData: function() {
            return $q.reject('error message');
          }
        },
        vm = getVm(mockSuccessMetricsDataService);

    $rootScope.$digest();

    expect(vm.error).toEqual('error message');
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
