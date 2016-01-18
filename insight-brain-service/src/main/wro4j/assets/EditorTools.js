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
      $scope.csrfTokenValue = $cookies.get($http.defaults.xsrfCookieName);

      function setError(message) {
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
        if ($scope.evaluationStatus && $scope.evaluationStatus.scanId) {
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
      $scope.uploaded = function (content, completed) {
        if (!completed || content.length === 0) {
          return;
        }
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

  module.controller('ImportPolicyController', ['$scope', '$http', '$timeout', '$window', '$cookies', 'Messages', 'CLMAppLocations', function ($scope, $http, $timeout, $window, $cookies, messages, clmAppLocations) {
    function fileCheck() {
      if (!fileElement) {
        fileElement =  angular.element('form[name=importPolicy] input[type=file]')[0];
      }
      $scope.btnDisabled = fileElement.files !== undefined && fileElement.files.length === 0;

      if (!$scope.$$destroyed) {
        $timeout(fileCheck, 100);
      }
    }
    function setError(message) {
      $scope.requestActive = false;
      //there are certain cases where the browser will not give us an error
      //as we would expect, so we will add something default in this case
      if (message) {
        $scope.error = [AngularUtils.toAlert(message)];
      }
      else {
        $scope.error = [AngularUtils.toAlert('Error uploading, please check the file.')];
      }
    }
    var fileElement = null;

    $scope.btnDisabled = true;
    $scope.importPolicyUrl = clmAppLocations.getIeImportPolicyUrl();
    $scope.csrfTokenName = $http.defaults.xsrfHeaderName;
    $scope.csrfTokenValue = $cookies.get($http.defaults.xsrfCookieName);

    $timeout(fileCheck, 100);

    $scope.doSubmit = function () {
      $scope.requestActive = true;
      $scope.error = null;
      var form = $('form[name=importPolicy]');

      if ($window.FormData) {
        var formData = new FormData(form[0]);
        $http.post(clmAppLocations.getImportPolicyUrl(), formData, {
          headers: {
            'Content-Type': undefined
          },
          transformRequest: angular.identity
        }).success(function (data) {
          $scope.$close(data);
        }).error(function () {
          setError(messages.getHttpErrorMessage(arguments));
        });
      } else {
        // IE9 case, trigger ng-upload
        form.find('input[type=submit]').trigger('click');
      }
    };

    // Handler for ng-upload progress
    $scope.uploaded = function (content, complete) {
      if (complete) {
        $scope.requestActive = false;
        if (content.length === 0) {
          // success
          $scope.$close();
        } else {
          $scope.error = [AngularUtils.toAlert(content)];
        }
      }
    };
  }]);

  module.service('editorTools', ['$parse', '$q', '$timeout', 'regexFactory', '$http', '$cookies', 'CLMAppLocations', 'Messages',
      function($parse, $q, $timeout, regexFactory, $http, $cookies, CLMAppLocations, Messages) {
        function EditorController($scope, idSelector, hiddenId, form) {
          var defer, me = this;
          $scope.isPostingIcon = false;

          $scope.alerts = [];
          $scope.hasRobotSource = false;
          $scope.csrfTokenName = $http.defaults.xsrfHeaderName;
          $scope.csrfTokenValue = $cookies.get($http.defaults.xsrfCookieName);

          $scope.pushAlert = function(obj) {
            $scope.alerts.length = 0;
            $scope.alerts.push(obj);
          };

          $scope.iconUploadComplete = function(content, completed) {
            if (completed && content) {
              content = angular.fromJson(content);
              $scope.submitActive = false;
              $scope.isUploadingIcon = false;
              if (content.length === 0) {
                $scope.iconChanged = false;
                $scope.hasRobotSource = false;
                $scope.$emit('resetIconCache');
                defer.resolve(null);
              }
              else {
                $scope.pushAlert({ type: 'error', msg: content });
                defer.reject(content);
              }
            }
          };

          me.generateIcon = function(name) {
            var hash = 0;
            // Once the user has already generated a robot by hashing the name, continue to provide random robots
            if (!name || $scope.hasRobotSource) {
              hash = Math.floor(Math.random() * 10000);
            }
            else {
              for (var i = 0; i < name.length; i++) {
                var charAtI = name.charCodeAt(i);
                /*jslint bitwise: true */
                hash = ((hash << 5) - hash) + charAtI;
                hash = hash & hash;
              }
            }
            $scope.robotHash = hash;
            $scope.hasRobotSource = true;
            $scope.iconChanged = true;
          };

          me.getIconSource = function(element, defaultSource) {
            if (element.files && element.files.length > 0) {
              var file = element.files[0], src;
              if (window.URL) {
                src = window.URL.createObjectURL(file);
              }
              else if (window.webkitURL) {
                src = window.webkitURL.createObjectURL(file);
              }
              if (src) {
                return src;
              }
            }

            return defaultSource;
          };

          me.saveIcon = function() {
            defer = $q.defer();

            if (!$scope.iconChanged) {
              $scope.submitActive = false;
              defer.resolve(null);
              return defer.promise;
            }

            // Angular modal does not adjust value of form element so when posting these values need to be set
            hiddenId.val($parse(idSelector)($scope));
            // With new app/org, this method is called before a digest loop with the id set
            angular.element('[name=' + $scope.ao.type + 'Id]').val($scope.ao.getId());
            angular.element('[name=hasRobotSource]').val($scope.hasRobotSource);
            angular.element('[name=robotHash]').val($scope.robotHash);

            if (window.FormData) {
              $scope.isUploadingIcon = true;

              var formData = new FormData(form[0]);
              var icon = angular.element('#file')[0];
              if (icon.files.length > 0) {
                formData.append('file', icon.files[0]);
              }

              $http.post(CLMAppLocations.addIcon(), formData, {
                headers : {
                  'Content-Type' : undefined
                },
                transformRequest: angular.identity
              }).success(function (data) {
                $scope.submitActive = false;
                $scope.isUploadingIcon = false;
                $scope.iconChanged = false;
                $scope.$emit('resetIconCache');
                defer.resolve(data);
              }).error(function () {
                var msg = Messages.getHttpErrorMessage(arguments);
                $scope.isUploadingIcon = false;
                $scope.submitActive = false;
                $scope.pushAlert({ type: 'error', msg: msg });
                defer.reject(msg);
              });
            }
            else {
              $scope.isPostingIcon = true;
              $scope.isUploadingIcon = true;
              $('#iconUploadForm').find('input[type=submit]').trigger('click');
            }
            return defer.promise;
          };
        }

        return {
          messages: {
            required: 'Name is required',
            validNameCharacters: 'Use valid characters: alphanumeric, "_", ".", "-", or spaces',
            spaces: 'No leading, trailing or double spaces or tabs',
            duplicate: 'Name is already in use'
          },
          getEditorController: function($scope, idSelector, hiddenId, form) {
            return new EditorController($scope, idSelector, hiddenId, form);
          }
        };
      }]);
}());
