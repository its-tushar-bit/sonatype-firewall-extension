/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
var validators = angular.module('Validators', []);
export default validators;

validators.directive('uniqueValidator', [
  '$parse',
  function ($parse) {
    return {
      restrict: 'A',
      require: 'ngModel',
      priority: 99,
      link: function (scope, elm, attrs, ctrl) {
        ctrl.$validators.unique = function (newValue) {
          var array = $parse(attrs.uniqueValidator)(scope);
          return angular.isArray(array) && array.indexOf(newValue) === -1;
        };

        scope.$watch(
          function () {
            return $parse(attrs.uniqueValidator)(scope);
          },
          function (newValue, oldValue) {
            // Account for "reset" events
            if (angular.isArray(newValue) && oldValue !== undefined) {
              ctrl.$$parseAndValidate();
            }
          },
          true
        );
      },
    };
  },
]);

validators.directive('inputValidator', [
  '$parse',
  function ($parse) {
    return {
      restrict: 'A',
      require: 'ngModel',
      priority: 99,
      link: function (scope, elm, attrs, ctrl) {
        var validate = function (newValue) {
          var validator = $parse(attrs.inputValidator)(scope);
          var validation = validator(newValue);

          var isValid = true;
          for (var validity in validation) {
            if (validation.hasOwnProperty(validity)) {
              ctrl.$setValidity(validity, validation[validity]);
              isValid = isValid && validation[validity];
            }
          }

          return isValid ? newValue : undefined;
        };

        ctrl.$parsers.unshift(validate);
      },
    };
  },
]);

validators.factory('validationHelper', function () {
  return {
    revalidateChildren: function (element) {
      angular.forEach(element.find('form'), function (form) {
        var formElement = angular.element(form);
        var formController = formElement.controller('form');
        angular.forEach(formElement.find('input'), function (input) {
          formController[input.name].$$parseAndValidate();
        });
      });
    },
  };
});
