import successMetricsModule from '../../../../main/frontend/labs/successMetrics/module';

describe('chartUtilsService', function() {
  beforeEach(angular.mock.module(successMetricsModule.name));

  var chartUtilsService;

  beforeEach(inject(function(_chartUtilsService_) {
    chartUtilsService = _chartUtilsService_;
  }));

  describe('calculateTickInterval', function() {
    it('does not round tickInterval if it is less then 1', function() {
      expect(chartUtilsService.calculateTickInterval(4, 2)).toBe(0.5);
    });

    it('rounds tickInterval to multiples of 1 if between 1 and 5', function() {
      expect(chartUtilsService.calculateTickInterval(4, 13)).toBe(3);
    });

    it('rounds tickInterval to multiples of 5 if more then 5', function() {
      expect(chartUtilsService.calculateTickInterval(4, 44)).toBe(10);
    });

    it('returns 1 if maxValue is 0', function() {
      expect(chartUtilsService.calculateTickInterval(4, 0)).toBe(1);
    });

    it('returns .25 if maxValue is less than or equal to 1 and 4 ticks requested', function() {
      expect(chartUtilsService.calculateTickInterval(4, 1)).toBe(0.25);
    });

    it('returns .33 if maxValue is less than 1 and 3 ticks requested', function() {
      expect(chartUtilsService.calculateTickInterval(3, .99)).toBe(0.33);
    });
  });
});
