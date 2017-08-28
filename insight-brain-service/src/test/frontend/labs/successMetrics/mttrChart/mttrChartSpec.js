/* global describe, beforeEach, it, expect, inject, Plottable */
describe('mttr-chart component', function() {
  beforeEach(module('successMetricsModule', 'legacyConfiguration'));

  var getVm;

  beforeEach(inject(function($componentController) {
    getVm = function(mttrData) {
      return $componentController('mttrChart', null, { mttrData: mttrData });
    };
  }));

  it('creates the mttr chart with passed-in data', function() {
    var mttrData = [
          {timePeriodStart: 1483254000000, mttrInSeconds: null, criticalMttrInSeconds: null},
          {timePeriodStart: 1485932400000, mttrInSeconds: 1209714, criticalMttrInSeconds: 1209714},
          {timePeriodStart: 1488351600000, mttrInSeconds: 484000, criticalMttrInSeconds: 484000}
        ],
        vm = getVm(mttrData);

    expect(vm.mttrChart).toBeDefined();
  });
});
