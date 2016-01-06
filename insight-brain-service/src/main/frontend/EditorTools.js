/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, AngularUtils, $ */
(function() {
  'use strict';
  var module = angular.module('EditorTools', ['CommonServices', 'CLMAppLocation', 'Stores', 'AngularCommon', 'xeditable', 'ngCookies']),
      validEvaluateBundleStages = ['build', 'stage-release', 'release', 'operate'];

  module.run(['editableOptions', function (editableOptions) {
    editableOptions.theme = 'bs2';
  }]);

  module.controller('EvaluateBundleController', [
    '$scope',
    '$http',
    '$timeout',
    '$window',
    '$cookies',
    'Messages',
    'CLMLocations',
    'selectedApplication',
    'ApplicationStore',
    'StageTypeStore',
    '$q',
    function($scope, $http, $timeout, $window, $cookies, messages, CLMLocations, selectedApplication, ApplicationStore,
            StageTypeStore, $q) {
      $scope.currentState = 'init';
      $scope.csrfTokenName = $http.defaults.xsrfHeaderName;
      $scope.csrfTokenValue = $cookies[$http.defaults.xsrfCookieName];

      function setError(message) {
        $scope.requestActive = false;
        //there are certain cases where the browser will not give us an error
        //as we would expect, so we will add something default in this case
        if (message) {
          $scope.alerts = [AngularUtils.toAlert(message)];
        } else {
          $scope.alerts = [AngularUtils.toAlert('Error uploading, please check the file.')];
        }
      }

      function getApplicationName(publicId) {
        for ( var i = 0 ; i < $scope.applications.length ; i++ ) {
          if ($scope.applications[i].publicId === publicId) {
            return $scope.applications[i].name;
          }
        }
      }

      function parseFilename(filename) {
        var idx = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));

        if (idx > -1) {
          return filename.substring(idx + 1);
        }

        return filename;
      }

      function doLoad() {
        $scope.state = 'loading';
        $q.all([StageTypeStore.get(), ApplicationStore.get()]).then(function(results) {
          $scope.state = 'ready';
          $scope.applications = results[1];
          $scope.stages = [];
          angular.forEach(results[0], function(stage) {
            if (validEvaluateBundleStages.indexOf(stage.stageTypeId) > -1) {
              $scope.stages.push(stage);
            }
          });
          $scope.bundle = {
            notify: 'true',
            applicationPublicId: selectedApplication ? selectedApplication.publicId : null
          };
          $scope.updateFormActionUrl();
        }, function(error){
          $scope.state = 'ready';
          setError(messages.getHttpErrorMessage(error));
        });
      }

      function doPoll() {
        if (!$scope.$$destroyed) {
          $http.get($scope.pollingUrl).then(function(response){
            $scope.evaluationStatus = response.data;
            if ($scope.evaluationStatus.error) {
              setError($scope.evaluationStatus.error);
            } else if ($scope.evaluationStatus.currentStep < $scope.evaluationStatus.totalSteps) {
              $timeout(doPoll,500);
            }
          },function(error){
            setError(messages.getHttpErrorMessage(error));
          });
        }
      }

      function getBundleUploadUrl() {
        return CLMLocations.getBundleUploadUrl($scope.bundle.applicationPublicId, $scope.bundle.stage, $scope.bundle.notify);
      }

      $scope.getProgressWidth = function () {
        return $scope.evaluationStatus ? ($scope.evaluationStatus.currentStep / $scope.evaluationStatus.totalSteps * 100) : '0';
      };

      $scope.doSubmit = function () {
        var fileElement = angular.element('#bundleFile')[0];
        $scope.state = 'polling';
        $scope.evaluationStatus = {currentStep: 1, totalSteps: 1, currentStepName: 'Uploading'};
        $scope.error = null;
        $scope.bundle.filename = parseFilename(fileElement.value);
        $scope.bundle.applicationName = getApplicationName($scope.bundle.applicationPublicId);
        $scope.pollingUrl = null;

        if ($window.FormData) {
          var form = new FormData();
          form.append('file', fileElement.files[0]);
          $http.post(getBundleUploadUrl(), form, {
            headers : {
              'Content-Type' : undefined
            },
            transformRequest: angular.identity
          }).success(function (data) {
            $scope.pollingUrl = CLMLocations.getEvaluationStatusUrl($scope.bundle.applicationPublicId, data.ticketId);
            doPoll();
          }).error(function () {
            $scope.evaluationStatus.error = messages.getHttpErrorMessage(arguments);
            $scope.evaluationStatus.currentStepName = 'Done';
            setError($scope.evaluationStatus.error);
          });
        }
        else {
          // IE9 case, trigger ng-upload
          $('form[name=evaluateBundle]').find('input[type=submit]').trigger('click');
        }
      };

      $scope.getReportUrl = function() {
        if ($scope.evaluationStatus.scanId) {
          return 'index.html#/reports/' + encodeURIComponent($scope.evaluationStatus.applicationPublicId) + '/' + $scope.evaluationStatus.scanId;
        }
        return '';
      };

      $scope.updateFormActionUrl = function() {
        $scope.evaluateBundleAction = getBundleUploadUrl();
      };

      $scope.isFormValid = function() {
        return $scope.bundle.file && $scope.bundle.applicationPublicId && $scope.bundle.stage && $scope.bundle.notify;
      };

      // Handler for ng-upload progress
      $scope.uploaded = function (content) {
        $scope.requestActive = false;
        $scope.error = null;
        var response;
        try {
          response = angular.fromJson(content);
        }
        catch(e) {
          response = content;
        }

        if (angular.isString(response)) {
          $scope.state = 'ready';
          setError(response);
        } else {
          $scope.state = 'polling';
          $scope.pollingUrl = CLMLocations.getEvaluationStatusUrl($scope.bundle.applicationPublicId, response.ticketId);
          doPoll();
        }
      };

      doLoad();
    }
  ]);
  
  module.directive('fileModel', [function () {
    return {
      scope : {
        fileModel : '='
      },
      link : function (scope, element) {
        element.on('change', function () {
          AngularUtils.safeApply(scope, function () {
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
      template : '<span ng-click="myForm.$show()"' +
          'editable-text="model[modelField]"' +
          'blur="submit"' +
          'onbeforesave="check($data)"' +
          'onshow="onShow()"' +
          'buttons="no"' +
          'e-form="myForm"' +
          'e-placeholder="{{emptyText}}">{{model[modelField] || emptyText}}</span>',
      restrict : 'A',
      scope : {
        duplicateArray : '=',
        duplicateIdField : '@',
        emptyText : '@',
        whitespaceCheck : '@',
        noSpaces : '@',
        model : '=',
        modelField : '@',
        invalid : '=?',
        eForm : '@'
      },
      priority : 99,
      link : function (scope, element) {

        scope.$watch('myForm', function (newVal) {
          var getter = $parse(scope.eForm);
          getter.assign(scope.$parent, newVal);
        });

        scope.onShow = function () {
          function change() {
            var val = (inputElement.val() || '').trim();

            AngularUtils.safeApply(scope, function () {
              if (val) {
                scope.myForm.$setError(null, scope.check(val) || '');
              } else {
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
          if(scope.noSpaces && val.match(spaceRegex)) {
            return 'Spaces or tabs are not allowed';
          }
          // check for invalid characters
          if (val.match(invalidCharsRegex)) {
            return 'Use valid characters: alphanumeric, "_", ".",' +
              (scope.noSpaces ? ' or' : '') + ' "-"' + (scope.noSpaces ? '' : ', or spaces');
          }
          // check for double spaces or tabs
          if (scope.whitespaceCheck  && val.match(/^ | {2,}|\t| $/)) {
            return 'No double spaces or tabs in name';
          }
          scope.invalid = false;
        };

        scope.check(scope.model ? scope.model[scope.modelField] : '');
      }
    };
  }]);

  module.directive('noSpaces', function () {
   var regexp = /\s/;
   return {
     require: 'ngModel',
     link: function (scope, element, attrs, ctrl) {
       ctrl.$validators.noSpaces = function (modelValue, viewValue) {
         if (ctrl.$isEmpty(modelValue)) {
           return true;
         }
         return !regexp.test(viewValue);
       };
     }
   };
  });

}());
