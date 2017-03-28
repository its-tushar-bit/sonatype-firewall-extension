describe('dashboard.utils.module', function() {

  beforeEach(module('dashboard.utils'));

  describe('filterToParams()', function() {
    var filterToParams;

    beforeEach(inject(function($injector) {
      filterToParams = $injector.get('filterToParams');
    }));

    it('sets maxResults incremented by 1', function() {
      expect(filterToParams(null, 1)).toEqual({maxResults: 2})
    });

    it('converts policyThreatLevel to string', function() {
      var params = filterToParams({policyThreatLevel: [2, 7]});
      expect(params.policyThreatLevelRange).toBe('2,7');
    });

    it('converts policyThreatTypes to string', function() {
      var params = filterToParams({policyThreatTypes: ['SECURITY', 'LICENSE']});
      expect(params.policyThreatCategories).toBe('SECURITY,LICENSE');
    });

    it('sets policyViolationStates to provided array of states', function() {
      var params = filterToParams({policyViolationStates: ['OPEN', 'WAIVED']});
      expect(params.policyViolationStates).toEqual(['OPEN', 'WAIVED']);
    });
  });
});
