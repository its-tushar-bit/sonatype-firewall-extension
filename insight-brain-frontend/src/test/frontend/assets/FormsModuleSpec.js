/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import formsModule from '../../../main/frontend/FormsModule';

describe('Forms module', function() {

  beforeEach(angular.mock.module(formsModule.name));

  describe('clmInput', function () {
    var compile, scope;

    beforeEach(inject(function($rootScope, $compile) {
      scope = $rootScope.$new();
      compile = $compile;
      scope.field = 'test';
    }));

    it('Requires that the input have a "name" set', function() {
      expect(function() {
        compile('<form name="form"><input clm-input type="text" ng-model="field"></form>')(scope);
      }).toThrowError('The input must have a name');
    });

    it('Requires that the form have a "name" set', function() {
      expect(function() {
        compile('<form><input name="test" clm-input type="text" ng-model="field"></form>')(scope);
      }).toThrowError('The form must have a name');
    });

    it('Will reject a "messages" attr that is not an Object', function() {
      expect(function() {
        compile('<form><input messages="foo" name="test" clm-input type="text" ng-model="field"></form>')(scope);
      }).toThrowError('Messages provided to the input must be an Object!');
    });

    describe('Popover validation messages', function() {
      var element, input;

      function confirmAndRemovePopover(message, newValue) {
        //popover initially is present and showing the specified message
        var popover = input.data('popover');
        expect(popover).not.toBe(undefined);
        expect(popover.getContent()).toBe(message);
        expect(scope.form.test.$invalid).toBe(true);

        //if we fix the problem the popover is removed, and the input is no longer invalid
        scope.form.test.$setViewValue(newValue);
        scope.$digest();
        expect(input.data('popover')).toBe(undefined);
        expect(scope.form.test.$invalid).toBe(false);
      }

      describe('Popover triggers', function() {
        beforeEach(function() {
          element = compile('<form name="form"><input clm-input name="test" type="text" ng-model="field" ' +
              'required></form>')(scope);
          input = element.find('input');
        });

        it('Adds a popover if invalid input is seen', function() {
          //trigger 'required' validation error
          scope.form.test.$setViewValue('');
          scope.$digest();
          confirmAndRemovePopover('Please enter a value', 'test');
        });
      });

      describe('Popover specific messages', function() {
        var testCases = [
          {
            input: '<input clm-input name="test" type="number" ng-model="field">',
            newValue: 'a',
            expected: 'Please enter a valid number',
            finalValue: '1'
          },
          {
            input: '<input ng-minlength="2" clm-input name="test" type="text" ng-model="field">',
            newValue: 'a',
            expected: 'Minimum length is 2',
            finalValue: 'ab'
          },
          {
            input: '<input ng-maxlength="2" clm-input name="test" type="text" ng-model="field">',
            newValue: 'abc',
            expected: 'Maximum length is 2',
            finalValue: 'ab'
          },
          {
            input: '<input ng-pattern="/^\\s*\\w*\\s*$/" clm-input name="test" type="text" ng-model="field">',
            newValue: 'a b',
            expected: 'Must match pattern: /^\\s*\\w*\\s*$/',
            finalValue: 'ab'
          },
          {
            input: '<input min="1" clm-input name="test" type="number" ng-model="field">',
            newValue: '0',
            expected: 'Minimum allowed value is 1',
            finalValue: '1'
          },
          {
            input: '<input max="1" clm-input name="test" type="number" ng-model="field">',
            newValue: '2',
            expected: 'Maximum allowed value is 1',
            finalValue: '1'
          },
          {
            input: '<input max="1" clm-input name="test" type="number" ng-model="field" '
            + 'messages="{max: \'Overriding the default message\'}">',
            newValue: '2',
            expected: 'Overriding the default message',
            finalValue: '1'
          }
        ];

        function triggerValidationError(testCase) {
          it('Shows "' + testCase.expected + '" when set to "' + testCase.newValue + '" and "' + testCase.finalValue +
                  '" is a valid input', function() {
            element = compile('<form name="form">' + testCase.input + '</form>')(scope);
            input = element.find('input');
            scope.form.test.$setViewValue(testCase.newValue);
            scope.$digest();
            confirmAndRemovePopover(testCase.expected, testCase.finalValue);
          });
        }

        for (var i = 0; i < testCases.length; i++) {
          var testCase = testCases[i];
          triggerValidationError(testCase);
        }
      });
    });
  });
});
