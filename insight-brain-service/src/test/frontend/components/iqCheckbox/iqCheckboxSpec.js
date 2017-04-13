describe('iq-checkbox component', function() {

  var getVm, $scope;

  beforeEach(module('components'));
  beforeEach(inject(function($componentController, $rootScope) {
    $scope = $rootScope.$new();
    getVm = function(bindings) {
      return $componentController('iqCheckbox', {$scope: $scope}, bindings);
    }
  }));

  describe('getId()', function() {
    it('uses scope id if inputId was not provided', function() {
      $scope.$id = 123123;
      expect(getVm().getId()).toBe('iq_checkbox_123123');
    });

    it('returns provided inputId', function() {
      expect(getVm({inputId: 'testId123'}).getId()).toBe('testId123');
    });
  });

  describe('hasLabel()', function() {

    it('is truthy if label is provided', function() {
      expect(getVm({label: 'test'}).hasLabel()).toBeTruthy();
    });

    it('is falsy if label is not provided', function() {
      expect(getVm().hasLabel()).toBeFalsy();
    });

    it('is falsy if label is empty string', function() {
      expect(getVm({label: ''}).hasLabel()).toBeFalsy();
    });
  });
});
