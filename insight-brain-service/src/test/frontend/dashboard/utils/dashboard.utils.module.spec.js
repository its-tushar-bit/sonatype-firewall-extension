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
      var params = filterToParams({
        minPolicyThreatLevel: 2,
        maxPolicyThreatLevel: 7
      });
      expect(params.policyThreatLevelRange).toBe('2,7');
    });

    it('converts policyThreatLevel to undefined if minPolicyThreatLevel is undefined', function() {
      var params = filterToParams({
        minPolicyThreatLevel: undefined,
        maxPolicyThreatLevel: 7
      });
      expect(params.policyThreatLevelRange).toBeUndefined();
    });

    it('converts policyThreatLevel to undefined if maxPolicyThreatLevel is undefined', function() {
      var params = filterToParams({
        minPolicyThreatLevel: 2,
        maxPolicyThreatLevel: undefined
      });
      expect(params.policyThreatLevelRange).toBeUndefined();
    });

    it('sets applicationIds to provided array of ids', function() {
      var filter = {applicationFilters: ['app1', 'app2']};
      var params = filterToParams(filter);
      expect(params.applicationIds).toBe(filter.applicationFilters);
    });

    it('does not set policyThreatCategories if policyThreatCategoryFilters is empty', function() {
      var params = filterToParams({policyThreatCategoryFilters: []});
      expect(params.policyThreatCategories).toBeUndefined();
    });

    it('converts policyThreatCategoryFilters to string', function() {
      var params = filterToParams({policyThreatCategoryFilters: ['SECURITY', 'LICENSE']});
      expect(params.policyThreatCategories).toBe('SECURITY,LICENSE');
    });

    it('sets stageIds to provided array of stageTypes', function() {
      var filter = {stageTypeFilters: ['stage1']};
      var params = filterToParams(filter);
      expect(params.stageIds).toBe(filter.stageTypeFilters);
    });

    it('sets tagIds to provided array of tagFilters', function() {
      var filter = {tagFilters: ['tag1']};
      var params = filterToParams(filter);
      expect(params.tagIds).toBe(filter.tagFilters);
    });

    it('sets policyViolationStates to provided array of states', function() {
      var filter = {policyViolationStates: ['OPEN', 'WAIVED']};
      var params = filterToParams(filter);
      expect(params.policyViolationStates).toBe(filter.policyViolationStates);
    });

    it('sets maxDaysOld to provided value', function() {
      var filter = {maxDaysOld: 90};
      var params = filterToParams(filter);
      expect(params.maxDaysOld).toBe(filter.maxDaysOld);
    });
  });
});
