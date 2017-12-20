describe('dashboardCommonResultsSpec', function() {
  beforeEach(module('dashboard.utils'));

  var vm,
      dialogMock;

  beforeEach(module('dashboard.module', 'dashboard.utils'));

  beforeEach(inject(
      function($componentController) {
        dialogMock = jasmine.createSpyObj('Dialog', ['open']);
        vm = $componentController('dashboardCommonResults', {
          Dialog: dialogMock
        });
        vm.maxResults = 1;
        vm.needsAcknowledgement = false;
      }
  ));

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

  describe('$onChanges()', function() {
    it('opens Filter invalid dialog if got 403 error', function() {
      vm.$onChanges({error: {currentValue: {status: 403}}});
      expect(dialogMock.open).toHaveBeenCalled();
      expect(vm.errorMessage).toBeUndefined();
    });

    it('sets vm.errorMessage if got non 403 error', function() {
      vm.$onChanges({error: {currentValue: {status: 404}}});
      expect(dialogMock.open).not.toHaveBeenCalled();
      expect(vm.errorMessage).toBe('Error 404');
    });
  });
});
