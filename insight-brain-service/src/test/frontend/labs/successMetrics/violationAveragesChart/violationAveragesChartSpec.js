/* global describe, beforeEach, it, expect, inject, Plottable */
describe('violation-averages-chart component', function() {
  beforeEach(module('successMetricsModule', 'legacyConfiguration'));

  var getVm;

  beforeEach(inject(function($componentController) {
    getVm = function(averagesData) {
      return $componentController('violationAveragesChart', null, { averagesData: averagesData });
    };
  }));

  it('sets the numeric values from the averagesData', function() {
    var averagesData = {
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
        },
        vm = getVm(averagesData);

    expect(vm.averageDiscoveredSecurity).toEqual(15);
    expect(vm.averageDiscoveredLicense).toEqual(12);
    expect(vm.averageDiscoveredQuality).toEqual(16);
    expect(vm.averageDiscoveredOther).toEqual(8);

    expect(vm.averageDiscoveredSecurityCritical).toEqual(0);
    expect(vm.averageDiscoveredLicenseCritical).toEqual(7);
    expect(vm.averageDiscoveredQualityCritical).toEqual(0);
    expect(vm.averageDiscoveredOtherCritical).toEqual(2);
  });

  it('sets vm.chart to a Plottable component', function() {
    var averagesData = {
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
        },
        vm = getVm(averagesData);

    expect(vm.chart instanceof Plottable.Component).toBe(true);
  });
});
