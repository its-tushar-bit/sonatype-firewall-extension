describe('iq-radio component', function() {

  var getVm, $scope;

  beforeEach(module('components'));
  beforeEach(inject(function($componentController, $rootScope) {
    $scope = $rootScope.$new();
    getVm = function(bindings) {
      return $componentController('iqRadio', {$scope: $scope}, bindings);
    };
  }));

  describe('getId()', function() {
    it('uses scope id if inputId was not provided', function() {
      $scope.$id = 123123;
      expect(getVm().getId()).toBe('iq_radio_123123');
    });

    it('returns provided inputId', function() {
      expect(getVm({inputId: 'testId123'}).getId()).toBe('testId123');
    });
  });
});
