describe('chartUtilsService', function() {
  beforeEach(module('successMetricsModule'));

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
    })
  });
});
