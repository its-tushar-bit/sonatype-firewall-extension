/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import validators from '../../../main/frontend/utilAngular/Validators';

describe('ValidatorsSpec', function () {
  beforeEach(angular.mock.module(validators.name));

  describe('Unique Validator', function () {
    var scope, element, formController;
    beforeEach(inject(function ($rootScope, $compile) {
      scope = angular.extend($rootScope, {
        model: {
          value: '',
        },
        array: ['foo'],
      });
      element = $compile(
        '<form><input name="input" type="text" unique-validator="array" ng-model="model.value">' + '</form>'
      )(scope);
      formController = element.controller('form');
    }));

    it('is invalid when a duplicate entry is input', function () {
      scope.$apply(function () {
        formController.input.$setViewValue('foo');
      });
      expect(formController.$valid).toBeFalsy();
      expect(formController.input.$valid).toBeFalsy();
      expect(formController.input.$error.unique).toBeTruthy();
    });

    it('is valid when a duplicate entry is changed to non duplicate', function () {
      scope.$apply(function () {
        formController.input.$setViewValue('foo');
      });
      expect(formController.$valid).toBeFalsy();

      scope.$apply(function () {
        formController.input.$setViewValue('bar');
      });
      expect(formController.$valid).toBeTruthy();
      expect(formController.input.$valid).toBeTruthy();
      expect(formController.input.$error.unique).toBeFalsy();
    });

    it('is valid when the duplicate entry is removed from the array', function () {
      scope.$apply(function () {
        formController.input.$setViewValue('foo');
      });
      expect(formController.$valid).toBeFalsy();

      scope.$apply(function () {
        scope.array.splice(0, 1);
      });
      expect(formController.$valid).toBeTruthy();
      expect(formController.input.$valid).toBeTruthy();
      expect(formController.input.$error.unique).toBeFalsy();
    });
  });

  describe('Input Validator', function () {
    var testCases = [true, false];

    angular.forEach(testCases, function (testCase) {
      it(
        'Supports a ' + testCase + ' validator',
        inject(function ($rootScope, $compile) {
          var scope = angular.extend($rootScope, {
            model: {
              value: '',
            },
            validator: jasmine.createSpy().and.returnValue({
              validity: testCase,
            }),
          });
          var element = $compile(
            '<form><input name="input" type="text" input-validator="validator" ng-model="model.value">' + '</form>'
          )(scope);

          var formController = element.controller('form');

          scope.$apply(function () {
            formController.input.$setViewValue('foo');
          });
          expect(formController.$valid)[testCase ? 'toBeTruthy' : 'toBeFalsy']();
          expect(formController.input.$valid)[testCase ? 'toBeTruthy' : 'toBeFalsy']();
          expect(formController.input.$error.validity)[!testCase ? 'toBeTruthy' : 'toBeFalsy']();
        })
      );
    });
  });
});
