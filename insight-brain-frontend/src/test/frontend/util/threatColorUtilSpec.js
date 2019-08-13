import getThreatColor from '../../../main/frontend/cip/cip.policy.violations/threatColorUtil';

describe('threatColorUtil', function() {
  describe('getThreatColor', function() {
    it('returns red color if 8 >= threat level <= 10', function() {
      expect(getThreatColor(10)).toEqual('red');
      expect(getThreatColor(8)).toEqual('red');
    });

    it('returns orange color if 4 >= threat level <= 7', function() {
      expect(getThreatColor(7)).toEqual('orange');
      expect(getThreatColor(4)).toEqual('orange');
    });

    it('returns yellow color if 2 >= threat level <= 3', function() {
      expect(getThreatColor(3)).toEqual('yellow');
      expect(getThreatColor(2)).toEqual('yellow');
    });

    it('returns darkblue color if threat level is 1', function() {
      expect(getThreatColor(1)).toEqual('darkblue');
    });

    it('returns blue color if threat level is 0', function() {
      expect(getThreatColor(0)).toEqual('blue');
    });
  });
});
