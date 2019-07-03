import getPolicyThreatIndicatorLevel from '../../../main/frontend/util/getPolicyThreatIndicatorLevel';

describe('getPolicyThreatIndicatorLevel', function() {
  it('returns the correct threat indicator level for each valid threat level', function() {
    expect(getPolicyThreatIndicatorLevel(null)).toBe('ignore');
    expect(getPolicyThreatIndicatorLevel(undefined)).toBe('ignore');
    expect(getPolicyThreatIndicatorLevel(0)).toBe('ignore');
    expect(getPolicyThreatIndicatorLevel(1)).toBe('none');
    expect(getPolicyThreatIndicatorLevel(2)).toBe('moderate');
    expect(getPolicyThreatIndicatorLevel(3)).toBe('moderate');
    expect(getPolicyThreatIndicatorLevel(4)).toBe('severe');
    expect(getPolicyThreatIndicatorLevel(5)).toBe('severe');
    expect(getPolicyThreatIndicatorLevel(6)).toBe('severe');
    expect(getPolicyThreatIndicatorLevel(7)).toBe('severe');
    expect(getPolicyThreatIndicatorLevel(8)).toBe('critical');
    expect(getPolicyThreatIndicatorLevel(9)).toBe('critical');
    expect(getPolicyThreatIndicatorLevel(10)).toBe('critical');
  });
});
