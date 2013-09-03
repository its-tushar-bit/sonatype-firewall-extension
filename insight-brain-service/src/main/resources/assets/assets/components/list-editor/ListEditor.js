/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved. Includes
 * the third-party code listed at
 * http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 * trademark of Sonatype, Inc.
 */
/*global angular, $ */
(function() {
  'use strict';

  var module = angular.module('ListEditor', []);

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
        placeHolder: '@placeHolder',
        maxLength: '@maxLength',
        entries: '=entries',
        setError: '=setError'
      },
      priority: 99,
      link: function(scope, elm, attrs, ctrl) {
        scope.add = function() {
          scope.entries.push(scope.currentEntry);
          scope.currentEntry = '';
        };
        scope.remove = function(index) {
          scope.entries.splice(index, 1);
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
          var unique = angular.isArray(scope.entries) && scope.entries.indexOf(newValue) === -1,
              validation = typeof scope.validator === "function" ? scope.validator(newValue) : true,
              validInput = typeof validation === "string" ? false : validation != false;

          ctrl.$setValidity('unique', unique);
          ctrl.$setValidity('validInput', validInput);

          if (typeof scope.setError === "function") {
            if (!unique) {
              scope.setError('Enter a unique value');
            }
            else if (!validInput) {
              scope.setError(typeof validation === "string" ? validation : 'Invalid ' + scope.placeHolder);
            }
            else {
              scope.setError(null);
            }
          }
          return unique && validInput ? newValue : undefined;
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

  module.directive('entersubmit', function() {
    return {
      restrict: 'A',
      require: 'ngModel',
      scope: false,
      link: function(scope, element, attrs, ctrl) {
        element.bind('keydown', function(e) {
          if (e.keyCode === 13) { // Enter
            e.preventDefault();
            if (ctrl.$valid) {
              element.trigger('submit');
            }
          }
        });
      }
    };
  });
}());