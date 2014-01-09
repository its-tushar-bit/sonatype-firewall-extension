/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved. Includes
 *          the third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 *          trademark of Sonatype, Inc.
 */
(function() {
  'use strict';

  var applicationModule = angular.module('ApplicationModule',
      ['ui.router', 'AngularCommon', 'CLMLocation', 'ManagementModule', 'Policy', 'LicenseThreatGroup', 'Labels', 'ApplicationSecurityModule', 'Stores'],
      ['$stateProvider', function($stateProvider) {
        $stateProvider.state('management.application', {
          parent: 'management',
          url: '/application',
          controller: 'applicationController',
          templateUrl: '../application-assets/components/application-navigator.html?' + clmBuildTimestamp
        }).state('management.application.view', {
          parent: 'management.application',
          url: '/{applicationPublicId}',
          controller: 'applicationEditorController',
          data: {
            passThroughAlerts: []
          },
          templateUrl: '../application-assets/components/aoeditor.html?' + clmBuildTimestamp,
          resolve : {
            selectedApplication : function ($q, $stateParams, ApplicationStore) {
              if ($stateParams.applicationPublicId === '_new_')
                return ApplicationStore.create();

              var deferred = $q.defer();
              ApplicationStore.get().then(function (data) {
                for (var i=0; i<data.length; i++) {
                  if (data[i].publicId === $stateParams.applicationPublicId) {
                    deferred.resolve(data[i].$clone());
                    return;
                  }
                }
                deferred.resolve(null);
              }, /* Errors will be handled at state parent */ angular.noop);
              return deferred.promise;
            }
          }
        }).state('management.application.view.policies', {
          parent: 'management.application.view',
          url: '/policies',
          controller: 'PolicyController',
          data: {
            passThroughAlerts: []
          },
          templateUrl: '../policy-assets/components/policy/policy.html?' + clmBuildTimestamp
        }).state('management.application.view.labels', {
          parent: 'management.application.view',
          url: '/labels',
          controller: 'LabelController',
          templateUrl: '../policy-assets/components/label-editor/labels.html?' + clmBuildTimestamp
        }).state('management.application.view.licenses', {
          parent: 'management.application.view',
          url: '/licenses',
          controller: 'LicenseThreatGroupController',
          templateUrl: '../policy-assets/components/license-threat-group/license-threat-group.html?' + clmBuildTimestamp
        }).state('management.application.view.security', {
          parent: 'management.application.view',
          url: '/security',
          controller: 'AppSecurityController',
          templateUrl: '../policy-assets/components/app-security/app-security.html?' + clmBuildTimestamp
        });
      }]);

  applicationModule.controller('applicationController', [
    '$scope', '$state', '$location', 'ApplicationStore', 'CLMLocations',
    function($scope, $state, $location, ApplicationStore, CLMLocations) {
      $scope.location = $location;

      // Store icon cache timestamps at higher scope so it is not reinstantiated with editor controller
      $scope.applicationIconTimestamp = {};

      $scope.$state = $state;
      $scope.isCurrentTab = function(tabName) {
        return $state.current.name.lastIndexOf(tabName) === $state.current.name.length - tabName.length;
      };
      $scope.$on('$stateChangeSuccess', function(event, toState, toParams, fromState) {
        if (toState.data && toState.data.passThroughAlerts && fromState.data && fromState.data.passThroughAlerts) {
          angular.forEach(fromState.data.passThroughAlerts, function(alert) {
            toState.data.passThroughAlerts.push(alert);
          });
        }
      });

      $scope.doLoad = function() {
        $scope.error = null;
        ApplicationStore.get().then(function(applications) {
          $scope.applications = applications;
        }, function(error) {
          $scope.error = error;
        });
      };
      $scope.doLoad();
    }
  ]);

  applicationModule.controller('applicationEditorController',
      function($scope, $state, $http, $q, $modal, OrganizationStore, CLMLocations, CLMAppLocations, Messages,
               editorTools, ActionStore, policyEvaluator, selectedApplication)
      {
        var me = this;
        angular.extend(me,
            editorTools.getEditorController($scope, 'selectedApplication.id', angular.element('[name=applicationId]'),
                angular.element('#iconUploadForm')));

        // Application Editor controller will take care of managing its own icons
        function setApplicationIcon() {
          if ($scope.selectedApplication.publicId === null) {
            $scope.origUserIconSource = $scope.userIconSource = '../assets/img/defaulticon_application.png';
          }
          else if (!$scope.applicationIconTimestamp[$scope.selectedApplication.publicId]) {
            // Reset icon cache on initial load and when icon is changed
            resetIconCache();
          }
          else {
            $scope.origUserIconSource = $scope.userIconSource = getUserIconSource();
          }
        }

        function resetIconCache() {
          if ($scope.selectedApplication) {
            $scope.applicationIconTimestamp[$scope.selectedApplication.publicId] = new Date().getTime();
            $scope.origUserIconSource = $scope.userIconSource = getUserIconSource();
          }
        }

        function getUserIconSource() {
          return '../rest/application/icon/' + encodeURIComponent($scope.selectedApplication.publicId) + '?' +
              $scope.applicationIconTimestamp[$scope.selectedApplication.publicId];
        }

        function isExternalDestination(destination) {
          var application = $scope.selectedApplication;
          return !destination || (application && destination.indexOf('application/' + application.publicId) === -1);
        }

        $scope.$state = $state;
        $scope.submitActive = false;

        $scope.doLoad = function () {
          var ao = {
            addSync : CLMAppLocations.addIconSync(),
            isNew : function () {
              return $state.params.applicationPublicId === "_new_";
            },
            selected : selectedApplication,
            type : 'application',
            typeName : 'Application'
          };

          // The application is missing (changed appId, deleted or insuf perms)
          if (selectedApplication === null) {
            $scope.ao = angular.extend(ao, {
              getPublicId : function () {
                return $state.params.applicationPublicId;
              }
            });
            return;
          }

          $scope.error = null;

          var promises = [ActionStore.get()];
          if (selectedApplication.publicId) {
            promises.push($http.get(CLMLocations.getApplicationSummaryUrl(selectedApplication.publicId), {
              params: {
                timestamp: new Date().getTime()
              }
            }));
          }

          // New application, or an application without an organization
          if (!selectedApplication.organizationId) {
            promises.push(OrganizationStore.get());
          }
          
          $q.all(promises).then(function(results) {
            $scope.selectedApplication = selectedApplication;
            setApplicationIcon();

            $scope.state = {
              actionStageList: results[0][1]
            };
            $scope.ao = angular.extend(ao, {
              siblings : $scope.applications,
              getPublicId : function () {
                return selectedApplication.publicId;
              },
              getId : function () {
                return selectedApplication.id;
              }
            });

            if (selectedApplication.publicId) {
              $scope.applicationSummary = results[1].data;
              $scope.applicationSummary.stageCount = 0;
              angular.forEach($scope.applicationSummary.policyEvaluations, function(policyEvaluation, stage) {
                policyEvaluation.reportUrl = CLMLocations.getReportUrl($scope.applicationSummary.publicId,
                        policyEvaluation.scanId);
                $scope.applicationSummary.stageCount++;
              });
              if (results.length > 2) {
                $scope.organizations = results[2];
              }
            } else {
              $scope.organizations = results[1];
            }
          }, function (error) {
            $scope.error = error;
          });
        };
        $scope.doLoad();

        $scope.getOrganizationName = function() {
          return $scope.selectedApplication && $scope.selectedApplication.organizationName || "Select Organization";
        };

        $scope.setOrganization = function(organization) {
          $scope.selectedApplication.organizationId = organization.id;
          $scope.selectedApplication.organizationName = organization.name;
        };

        if ($state.current.data && $state.current.data.passThroughAlerts) {
          angular.forEach($state.current.data.passThroughAlerts, function(alert) {
            $scope.pushAlert(alert);
          });
        }

        $scope.generateIcon = function() {
          me.generateIcon($scope.selectedApplication.name);
        };

        $scope.fileChanged = function(element) {
          if (element.files && element.files.length > 0) {
            $scope.hasRobotSource = false;
            var file = element.files[0],
                src;
            if (window.URL) {
              src = window.URL.createObjectURL(file);
            }
            else if (window.webkitURL) {
              src = window.webkitURL.createObjectURL(file);
            }
            if (src) {
              $scope.$apply(function() {
                $scope.userIconSource = src;
                $scope.hasRobotSource = false;
              });
            }
            else {
              $scope.$apply(function() {
                $scope.userIconSource = '../assets/img/defaulticon_application.png';
                $scope.hasRobotSource = false;
              });
            }
          }
          else {
            $scope.$apply(function() {
              $scope.userIconSource = '../assets/img/defaulticon_application.png';
              $scope.hasRobotSource = false;
            });
          }
          $scope.$apply(function() {
            $scope.iconChanged = true;
          });
        };

        $scope.encodeURIComponent = window.encodeURIComponent;

        $scope.isFormDirty = function() {
          if (!$scope.selectedApplication) {
            return false;
          }
          var originalApplication = $scope.selectedApplication.$getOriginal(),
              currentApplication = $scope.selectedApplication,
              contactChanged = (currentApplication.contact || originalApplication.contact) ? !angular.equals(currentApplication.contact, originalApplication.contact) : false;

          return currentApplication.publicId != originalApplication.publicId ||
              currentApplication.name != originalApplication.name ||
              currentApplication.organizationId != originalApplication.organizationId ||
              contactChanged || $scope.iconChanged;
        };

        //make sure user is aware they are about to lose changes
        $scope.$on('pageChangeStarted', function(event, destination) {
          if (isExternalDestination(destination)) {
            if ($scope.isFormDirty() && !$scope.isPostingIcon) {
              event.preventDefault();
            }
          }
        });

        $scope.$on('pageChangeAccepted', function(event, destination) {
          if (isExternalDestination(destination)) {
            $scope.cancel();
          }
        });

        $scope.$on('resetIconCache', resetIconCache);

        $scope.canSaveEdit = function() {
          var applicationNameValid = $scope.aoEditorName && $scope.aoEditorName.$visible || $scope.ao.selected.name,
              applicationIdValid = !$scope.aoIdEditor || !$scope.aoIdEditor.$invalid && ($scope.appPublicIdForm && $scope.appPublicIdForm.$visible || $scope.selectedApplication.publicId);
          return !$scope.aoEditor.$invalid && !$scope.submitActive &&
              $scope.selectedApplication.organizationId && applicationNameValid && applicationIdValid;
        };

        $scope.cancel = function() {
          if ($scope.selectedApplication) {
            $scope.selectedApplication.$revert();
            if ($scope.aoEditorName.$visible) {
              $scope.aoEditorName.$cancel();
            }
            if ($scope.iconChanged) {
              $scope.userIconSource = $scope.origUserIconSource;
              $scope.hasRobotSource = false;
              $scope.iconChanged = false;
            }
          }
        };

        $scope.confirmDelete = function() {
          $modal.open({
            backdrop : 'static',
            keyboard : true,
            controller : 'DeleteResourceController',
            templateUrl : 'delete-app-modal',
            resolve : {
              selected : function () {
                return $scope.selectedApplication;
              }
            }
          }).result.then(function () {
            $state.transitionTo('management.application');
          }, function (error) {
            if (error) {
              $scope.$broadcast('showServerError', error);
            }
          });
        };

        // This needs to be invoked by onsubmit rather than ng-submit to suppress submit when necessary
        $scope.save = function() {
          if ($scope.submitActive || $scope.aoEditor.$invalid) {
            return;
          }
          if ($scope.appPublicIdForm && $scope.appPublicIdForm.$visible) {
            $scope.appPublicIdForm.$save();
          }
          if ($scope.aoEditorName.$visible) {
            $scope.aoEditorName.$save();
          }
          if (window.FormData) {
            var icon = angular.element('#file')[0];
            if (icon.files.length > 0) {
              if (icon.files[0].size > 5242880) {
                $scope.$apply(function() {
                  $scope.pushAlert({ type: 'error', msg: 'Icon file size must be smaller than 5 MB.' });
                });
                return false;
              }
            }
          }

          $scope.submitActive = true;

          var oldApplication = $scope.selectedApplication.$getOriginal();
          $scope.selectedApplication.contactInternalName = $scope.selectedApplication.contact ? $scope.selectedApplication.contact.internalName : null;
          $scope.selectedApplication.$save().then(function() {
            me.saveIcon().then(function() {
              if ($state.params.applicationPublicId === '_new_') {
                $state.transitionTo('management.application.view.policies',
                    { applicationPublicId: $scope.selectedApplication.publicId });
              }
              var changes = [];
              if (oldApplication.organizationId !== $scope.selectedApplication.organizationId) {
                changes.push({ field: 'organizationId', newValue: $scope.selectedApplication.organizationId });
              }
              if (oldApplication.name !== $scope.selectedApplication.name) {
                changes.push({ field: 'name', newValue: $scope.selectedApplication.name });
              }
              if (changes.length > 0) {
                $scope.$broadcast('ownerChanged', {
                  ownerId: $scope.selectedApplication.id,
                  changes: changes
                });
              }
            }, function(error) {
              if ($state.params.applicationPublicId === '_new_') {
                $state.current.data.passThroughAlerts.push({
                  type: 'error',
                  msg: 'An error occurred while saving the icon. (' + error + ')'
                });
                $state.transitionTo('management.application.view.policies',
                    { applicationPublicId: $scope.selectedApplication.publicId });
              }
            });
          }, function(error) {
            $scope.submitActive = false;
            $scope.alerts.push({
              type: 'error',
              msg: 'An error occurred while saving the application. (' + Messages.getHttpErrorMessage(error) + ')'
            });
          });

          return false;
        };

        $scope.reEvaluatePolicy = function(policyEvaluation) {
          return policyEvaluator.evaluate($scope.applicationSummary, policyEvaluation).then(angular.noop, function(error) {
            $scope.alerts.push({
              type: 'error',
              msg: 'An error occurred attempting to re-evaluate the policy. (' + Messages.getHttpErrorMessage(error) +
                  ')'
            });
          });
        };

        $scope.openImport = function () {
          $modal.open({
            backdrop : 'static',
            keyboard : false,
            templateUrl : 'import-policy-modal',
            controller : 'ImportPolicyController'
          }).result.then(function () {
            $scope.$broadcast('refresh', $scope.selectedApplication);
          }, angular.noop);
        };

        $scope.openEvalute = function () {
          $modal.open({
            backdrop : 'static',
            keyboard : false,
            templateUrl : 'evaluate-bundle-modal',
            controller : 'EvaluateBundleController',
            resolve: {
              selectedApplication: function() {
                return $scope.selectedApplication;
              }
            }
          }).result.then(function () {
            $scope.$broadcast('refresh', $scope.selectedApplication);
          }, angular.noop);
        };

        $scope.openContact = function () {
          $modal.open({
            backdrop : 'static',
            keyboard : true,
            templateUrl : 'contact-modal'
          }).result.then(function (contact) {
            selectedApplication.contact = contact;
          }, angular.noop);
        };
      });

  applicationModule.service('ApplicationId', [
    'commonCodeFactory', '$state', function(commonCodeFactory, $state) {
      // TODO Are ui-router parameters encoded or decoded?
      return {
        encoded: function() {
          var applicationPublicId = $state.params.applicationPublicId;
          return applicationPublicId ? encodeURI(applicationPublicId) : null;
        }
      };
    }
  ]);

  applicationModule.service('policyEvaluator', function($q, $http, CLMLocations) {
    return {
      evaluate: function(application, policyEvaluation) {
        var deferred = $q.defer();
        var stage = policyEvaluation.stage;
        $http.post(CLMLocations.evaluatePolicyUrl(application.publicId, policyEvaluation.scanId),
                stage).success(function(data) {
          policyEvaluation.time = new Date();
          for (var stageTypeId in application.policyEvaluationsResults) {
            if (stageTypeId === stage.stageTypeId) {
              application.policyEvaluationsResults[stageTypeId] = data;
              break;
            }
          }
          deferred.resolve(data);
        }).error(function(data, status, headers, config) {
              deferred.reject({ data: data, status: status, headers: headers, config: config });
            });
        return deferred.promise;
      }
    };
  });

  applicationModule.controller('ContactController', ['$scope', function ($scope) {
    $scope.alerts = [];

    $scope.setQueryResults = function (members, error) {
      $scope.queryResults = members;
      $scope.error = error;
    };

    $scope.selectUser = function (user) {
      $scope.$close(user);
    };

    $scope.$watch('queryString', function (newVal) {
      // clear the alerts
      $scope.alerts.length = 0;
    });
  }]);
}());
