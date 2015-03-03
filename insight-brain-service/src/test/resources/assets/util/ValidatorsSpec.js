describe('ValidatorsSpec', function() {
  'use strict';

  beforeEach(module('Validators'));

  describe('Unique Validator', function() {
    var scope, element, formController;
    beforeEach(inject(function($rootScope, $compile) {
      scope = angular.extend($rootScope, {
        model: {
          value: ''
        },
        array: ['foo']
      });
      element =
        $compile('<form><input name="input" type="text" unique-validator="array" ng-model="model.value"></form>')(scope);
      formController = element.controller('form');
    }));

    it('is invalid when a duplicate entry is input', function() {
      formController.input.$setViewValue('foo');
      expect(formController.$valid).toBe(false);
      expect(formController.input.$valid).toBe(false);
      expect(formController.input.$error.unique).toBe(true);
    });

    it('is valid when a duplicate entry is changed to non duplicate', function() {
      formController.input.$setViewValue('foo');
      expect(formController.$valid).toBe(false);
      formController.input.$setViewValue('bar');
      expect(formController.$valid).toBe(true);
      expect(formController.input.$valid).toBe(true);
      expect(formController.input.$error.unique).toBe(false);
    });

    it('is valid when the duplicate entry is removed from the array', function() {
      formController.input.$setViewValue('foo');
      expect(formController.$valid).toBe(false);
      scope.array.splice(0, 1);
      scope.$digest();
      expect(formController.$valid).toBe(true);
      expect(formController.input.$valid).toBe(true);
      expect(formController.input.$error.unique).toBe(false);
    });
  });

  describe('Input Validator', function() {
    var testCases = [ true, false ], scope;

    angular.forEach(testCases, function(testCase) {
      it('Supports a ' + testCase + ' validator', inject(function($rootScope, $compile) {
        var scope = angular.extend($rootScope, {
          model: {
            value: ''
          },
          validator: jasmine.createSpy().andReturn({
            validity: testCase
          })
        });
        var element =
          $compile('<form><input name="input" type="text" input-validator="validator" ng-model="model.value"></form>')(scope);

        var formController = element.controller('form');
        formController.input.$setViewValue('foo');
        expect(formController.$valid).toBe(testCase);
        expect(formController.input.$valid).toBe(testCase);
        expect(formController.input.$error.validity).toBe(!testCase);
      }));
    });
  });
});
