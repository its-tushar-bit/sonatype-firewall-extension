/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
import commonServicesModule from './util/CommonServices';
import angularCommonModule from './util/AngularCommon';
import CLMContextLocationModule from './util/CLMContextLocation';
import storesModule from './util/Stores';

var module = angular.module('EditorTools',
    [
      commonServicesModule.name, CLMContextLocationModule.name, storesModule.name, angularCommonModule.name,
      'xeditable', 'ngCookies'
    ]);

module.run(['editableOptions', function (editableOptions) {
  editableOptions.theme = 'bs2';
}]);

module.directive('fileModel', [function () {
  return {
    scope: {
      fileModel: '='
    },
    link: function (scope, element) {
      element.on('change', function () {
        scope.$applyAsync(function () {
          scope.fileModel = element.val();
        });
      });
    }
  };
}]);

module.directive('clmEditable', ['$parse', 'regexFactory', function ($parse, regexFactory) {
  var invalidCharsRegex = new RegExp('[^-\\. _' + regexFactory.allLetters().source + '0-9]', 'i');
  var spaceRegex = new RegExp('\\s', 'i');
  return {
    template: '<span ng-click="myForm.$show()"' +
        'editable-text="model[modelField]"' +
        'blur="submit"' +
        'onbeforesave="check($data)"' +
        'onshow="onShow()"' +
        'buttons="no"' +
        'e-form="myForm"' +
        'e-placeholder="{{emptyText}}">{{model[modelField] || emptyText}}</span>',
    restrict: 'A',
    scope: {
      duplicateArray: '=',
      duplicateIdField: '@',
      emptyText: '@',
      whitespaceCheck: '@',
      noSpaces: '@',
      model: '=',
      modelField: '@',
      invalid: '=?',
      eForm: '@'
    },
    priority: 99,
    link: function (scope, element) {

      scope.$watch('myForm', function (newVal) {
        var getter = $parse(scope.eForm);
        getter.assign(scope.$parent, newVal);
      });

      scope.onShow = function () {
        function change() {
          var val = (inputElement.val() || '').trim();

          scope.$applyAsync(function () {
            if (val) {
              scope.myForm.$setError(null, scope.check(val) || '');
            }
            else {
              scope.invalid = true;
            }
          });
        }
        var inputElement = angular.element('input', element);
        change();

        inputElement.keyup(change);
      };

      scope.check = function (val) {
        val = val || '';
        scope.invalid = true;
        // check duplicate
        if (scope.duplicateArray) {
          var duplicate = false,
              lowercaseVal = val.toLowerCase();

          angular.forEach(scope.duplicateArray, function (candidate) {
            if (candidate[scope.duplicateIdField] !== scope.model[scope.duplicateIdField] &&
                    (candidate[scope.modelField] || '').toLowerCase() === lowercaseVal) {
              duplicate = true;
            }
          });
          if (duplicate) {
            return 'Already in use';
          }
        }
        // check if spaces are not allowed
        if (scope.noSpaces && val.match(spaceRegex)) {
          return 'Spaces or tabs are not allowed';
        }
        // check for invalid characters
        if (val.match(invalidCharsRegex)) {
          return 'Use valid characters: alphanumeric, "_", ".",' +
            (scope.noSpaces ? ' or' : '') + ' "-"' + (scope.noSpaces ? '' : ', or spaces');
        }
        // check for double spaces or tabs
        if (scope.whitespaceCheck && val.match(/^ | {2,}|\t| $/)) {
          return 'No double spaces or tabs in name';
        }
        scope.invalid = false;
      };

      scope.check(scope.model ? scope.model[scope.modelField] : '');
    }
  };
}]);

module.directive('noSpaces', function() {
  var regexp = /\s/;
  return {
    require: 'ngModel',
    link: function(scope, element, attrs, ctrl) {
      ctrl.$validators.noSpaces = function(modelValue, viewValue) {
        if (ctrl.$isEmpty(modelValue)) {
          return true;
        }
        return !regexp.test(viewValue);
      };
    }
  };
});

export default module;
