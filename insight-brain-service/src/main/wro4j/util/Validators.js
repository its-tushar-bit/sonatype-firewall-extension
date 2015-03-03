/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
(function() {
  'use strict';

  var validators = angular.module('Validators', []);

  validators.directive('uniqueValidator', ['$parse', function($parse) {
    return {
      restrict: 'A',
      require: 'ngModel',
      priority: 99,
      link: function(scope, elm, attrs, ctrl) {
        var validate = function(newValue) {
          var array = $parse(attrs.uniqueValidator)(scope);
          var unique = angular.isArray(array) && array.indexOf(newValue) === -1;
          ctrl.$setValidity('unique', unique);

          return unique ? newValue : undefined;
        };

        ctrl.$parsers.unshift(validate);

        scope.$watch(attrs.uniqueValidator, function(newValue, oldValue) {
          // Account for "reset" events
          if (angular.isArray(newValue) && oldValue !== undefined) {
            validate(elm.val());
          }
        });
      }
    };
  }]);

  validators.directive('inputValidator', ['$parse', function($parse) {
    return {
      restrict: 'A',
      require: 'ngModel',
      priority: 99,
      link: function(scope, elm, attrs, ctrl) {
        var validate = function(newValue) {
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
      }
    };
  }]);

  validators.factory('validationHelper', function() {
    return {
      revalidateChildren: function(element) {
        angular.forEach(element.find('form'), function(form) {
          var formElement = angular.element(form);
          var formController = formElement.controller('form');
          angular.forEach(formElement.find('input'), function(input) {
            formController[input.name].$setViewValue(formController[input.name].$viewValue);
          });
        });
      }
    };
  });
}());
