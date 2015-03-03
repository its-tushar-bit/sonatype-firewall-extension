/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved. Includes
 * the third-party code listed at
 * http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 * trademark of Sonatype, Inc.
 */
/* global angular, clmBuildTimestamp */
(function() {
  'use strict';

  var module = angular.module('ListEditor', ['Validators']);

  module.directive('listEditor', function() {
    return {
      restrict: 'A',
      replace: true,
      transclude: true,
      templateUrl: '../assets/components/list-editor/list-editor.html?' + clmBuildTimestamp,
      scope: {
        cancel: '=cancel',
        doSave: '&save',
        validator: '=validator',
        label: '@label',
        placeholder: '@?',
        maxLength: '@maxLength',
        entries: '=entries',
        messages: '=?',
        regexes: '=?'
      },
      priority: 99,
      link: function(scope, element) {
        scope.isRegex = false;

        var inputScope = element.find('input').scope();
        scope.add = function() {
          if (scope.regexes && scope.isRegex) {
            scope.regexes.push(inputScope.currentEntry);
          } else {
            scope.entries.push(inputScope.currentEntry);
          }

          inputScope.currentEntry = '';
          //explicitly reset form state otherwise input remains $dirty
          scope.neditor.$setPristine();
        };
        scope.remove = function(index) {
          scope.entries.splice(index, 1);
          //rerun validator for currentEntry once entry is removed
          scope.neditor.currentEntry.$setViewValue(scope.neditor.currentEntry.$viewValue);
        };

        scope.removeRegex = function(index) {
          scope.regexes.splice(index, 1);
          //rerun validator for currentEntry once entry is removed
          scope.neditor.currentEntry.$setViewValue(scope.neditor.currentEntry.$viewValue);
        };

        scope.isRegexChanged = function() {
          scope.isRegex = !scope.isRegex;
          //rerun validator for currentEntry once is regex is toggled
          scope.neditor.currentEntry.$setViewValue(scope.neditor.currentEntry.$viewValue);
        };
      }
    };
  });

  module.directive('listEditorValidator', function() {
    return {
      restrict: 'A',
      require: 'ngModel',
      scope: false,
      priority: 99,
      link: function(scope, elm, attrs, ctrl) {
        var validate = function(newValue) {
          var unique = (!scope.isRegex && angular.isArray(scope.entries) && scope.entries.indexOf(newValue) === -1) ||
                       (scope.isRegex && angular.isArray(scope.regexes) && scope.regexes.indexOf(newValue) === -1),
              validation = typeof scope.validator === 'function' ? scope.validator(newValue, scope.isRegex) : true;

          ctrl.$setValidity('unique', unique);

          var isValid = true;
          for (var validity in validation) {
            if (validation.hasOwnProperty(validity)) {
              ctrl.$setValidity(validity, validation[validity]);
              isValid = isValid && validation[validity];
            }
          }

          return unique && isValid ? newValue : undefined;
        };

        ctrl.$parsers.unshift(validate);

        scope.$watch('entries', function(newValue, oldValue) {
          // Account for "reset" events
          if (angular.isArray(newValue) && oldValue !== undefined) {
            validate(elm.val());
          }
        });
      }
    };
  });
}());
