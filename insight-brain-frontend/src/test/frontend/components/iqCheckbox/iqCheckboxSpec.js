describe('iq-checkbox component', function() {

  var getVm, $scope;

  beforeEach(module('components'));
  beforeEach(inject(function($componentController, $rootScope) {
    $scope = $rootScope.$new();
    getVm = function(bindings) {
      return $componentController('iqCheckbox', {$scope: $scope}, bindings);
    };
  }));

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
