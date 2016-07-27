describe('number.input.with.string.value.spec.js', function() {
  var scope, inputElement;

  beforeEach(module('owner.manager.module'));

  beforeEach(inject(function($compile, $rootScope) {
    scope = $rootScope.$new();
    scope.numberModel = undefined;
    inputElement = $compile('<input type="number" ng-model="numberModel" number-input-with-string-value/>')(scope);
    scope.$digest();
  }));
  describe('Positive Cases', function() {
    it('Input changes to String Value', function() {
      inputElement.val(123);
      inputElement.trigger('input');
      expect(scope.numberModel).toBe('123');
    });
    
    describe('String Value changes to Input Number ', function() {
      it('with Integer', function() {
        scope.numberModel = '123';
        scope.$digest();
        expect(inputElement.val()).toBe('123');
      });

      it('with Float', function() {
        scope.numberModel = '123.123';
        scope.$digest();
        expect(inputElement.val()).toBe('123.123');
      });
    });
  });

  describe('Negative Cases', function() {
    it('Input is empty, Value should be undefined', function() {
      inputElement.val('');
      inputElement.trigger('input');
      expect(scope.numberModel).toBeUndefined();
    });

    it('String Value is undefined, input should be empty string', function() {
      scope.numberModel = undefined;
      scope.$digest();
      expect(inputElement.val()).toBe('');
    });
  });
});
