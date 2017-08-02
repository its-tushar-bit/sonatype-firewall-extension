describe('dashboardCommonResultsSpec', function() {
  beforeEach(module('dashboard.utils'));

  var $state,
      vm;

  beforeEach(module('dashboard.module', 'dashboard.utils'));

  beforeEach(inject(
      function($componentController, _$state_) {
        $state = _$state_;

        vm = $componentController('dashboardCommonResults');
        vm.maxResults = 1;
        vm.needsAcknowledgement = false;
      }
  ));

  it('detects if violation state is true', function() {
    spyOn($state, 'is').and.returnValue(true);
    expect(vm.isViolationsState()).toBe(true);
  });

  it('detects if violation state is not true', function() {
    spyOn($state, 'is').and.returnValue(false);
    expect(vm.isViolationsState()).toBe(false);
  });

  describe('loadCommonResults', function() {
    it('returns true when nothing available', function() {
      expect(vm.results).toBeUndefined();
      expect(vm.needsAcknowledgement).toBe(false);
      expect(vm.maxResults).toBe(1);
      expect(vm.loadCommonResults()).toBe(true);
    });

    it('returns true when results array is empty', function() {
      vm.results = [];
      expect(vm.loadCommonResults()).toBe(true);
    });

    it('returns true when results array is not empty', function() {
      vm.results = [{}, {}];
      expect(vm.loadCommonResults()).toBe(true);
    });

    it('returns true when results array length is greater than maxResults', function() {
      vm.results = [{}, {}];
      expect(vm.maxResults).toBe(1);
      expect(vm.loadCommonResults()).toBe(true);
    });

    it('returns false when results array length is not greater than maxResults', function() {
      vm.results = [{}];
      expect(vm.maxResults).toBe(1);
      expect(vm.loadCommonResults()).toBe(false);
    });

    it('returns true when results array length is not greater than maxResults but needsAcknowledgement is true', function() {
      vm.results = [{}];
      vm.needsAcknowledgement = true;
      expect(vm.maxResults).toBe(1);
      expect(vm.loadCommonResults()).toBe(true);
    });
  });
});
