/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved. Includes
 *          the third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 *          trademark of Sonatype, Inc.
 */
(function () {
  'use strict';

  var applicationModule = angular.module('ApplicationModule', ['ui.compat', 'ManagementModule', 'Policy', 'LicenseThreatGroup', 'Labels', 'AngularCommon', 'CLMLocation'], ['$stateProvider', function ($stateProvider) {
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
      templateUrl: '../application-assets/components/application-editor.html?' + clmBuildTimestamp
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
    });
  }]);

  applicationModule.controller('applicationController', ['$scope', '$state', '$timeout', '$location', 'applicationStore', function ($scope, $state, $timeout, $location, applicationStore) {
    function switchApplication() {
      $scope.selectedApplication = null;
      $scope.userIconSource = null;
      if ('_new_' === $scope.$state.params.applicationPublicId) {
        $timeout(function () {
          $scope.selectedApplication = applicationStore.create();
          $scope.origUserIconSource = $scope.userIconSource = '../assets/img/defaulticon_application.png';
        }, 100);
      } else if ($scope.$state.params.applicationPublicId !== null && $scope.applications) {
        for (var i = 0; i < $scope.applications.length; i++) {
          if ($scope.$state.params.applicationPublicId === $scope.applications[i].publicId) {
            $timeout(function () {
              $scope.selectedApplication = $scope.applications[i].$clone();
              $scope.$broadcast('setApplicationIcon');
            }, 100);
            return;
          }
        }
        // TODO We might want to consider reloading the store at this point?
      }
    }

    $scope.location = $location;

    // Store icon cache timestamps at higher scope so it is not reinstantiated with editor controller
    $scope.applicationIconTimestamp = {};

    $scope.$state = $state;
    $scope.isCurrentTab = function (tabName) {
      return $state.current.name.lastIndexOf(tabName) === $state.current.name.length - tabName.length;
    };
    $scope.$on('$stateChangeSuccess', function(event, toState, toParams, fromState){
      if (toState.data && toState.data.passThroughAlerts && fromState.data && fromState.data.passThroughAlerts) {
        angular.forEach(fromState.data.passThroughAlerts, function(alert) {
          toState.data.passThroughAlerts.push(alert);
        });
      }
    });

    $scope.doLoad = function () {
      $scope.error = null;
      applicationStore.get().then(function (applications) {
        $scope.applications = applications;
        switchApplication();
        $scope.$watch('$state.params.applicationPublicId', switchApplication);
      }, function (error) {
        $scope.error = error;
      });
    };
    $scope.doLoad();
  }]);

  applicationModule.controller('applicationEditorController', function ($scope, $state, applicationStore, OrganizationStore, CLMAppLocations, Messages, $http, hudson, editorTools) {
    var me = this;
    angular.extend(me, editorTools.getEditorController($scope, 'selectedApplication.id', angular.element('[name=applicationId]'), angular.element('#iconUploadForm')));

    // Application Editor controller will take care of managing its own icons
    function setApplicationIcon() {
      // Reset icon cache on initial load and when icon is changed
      if (!$scope.applicationIconTimestamp[$scope.selectedApplication.publicId]) {
        resetIconCache();
      } else {
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
      return '../rest/application/icon/' + encodeURIComponent($scope.selectedApplication.publicId) + '?' + $scope.applicationIconTimestamp[$scope.selectedApplication.publicId];
    }

    $scope.addApplicationSync = CLMAppLocations.addIconSync();
    $scope.$on('setApplicationIcon', setApplicationIcon);
    $scope.$on('resetIconCache', resetIconCache);

    $scope.$state = $state;
    $scope.submitActive = false;

    OrganizationStore.get().then(function (results) {
      $scope.organizations = results;
    });

    $scope.getOrganizationName = function (organizationId) {
      if (!organizationId) {
        return "Select Organization";
      }

      if ($scope.organizations) {
        for (var i = 0; i < $scope.organizations.length; i++) {
          var organizationIter = $scope.organizations[i];
          if (organizationIter.id === organizationId) {
            return organizationIter.name;
          }
        }
      }
    };

    $scope.changeOrganization = function (organization) {
      $scope.selectedApplication.organizationId = organization.id;
    };

    $scope.messages = editorTools.messages;

    if ($state.current.data && $state.current.data.passThroughAlerts) {
      angular.forEach($state.current.data.passThroughAlerts, function(alert) {
        $scope.pushAlert(alert);
      });
    }

    $scope.generateIcon = function () {
      me.generateIcon($scope.selectedApplication.name);
    };

    $scope.fileChanged = function (element) {
      if (element.files && element.files.length > 0) {
        $scope.hasRobotSource = false;
        var file = element.files[0],
        src;
        if (window.URL) {
          src = window.URL.createObjectURL(file);
        } else if (window.webkitURL) {
          src = window.webkitURL.createObjectURL(file);
        }
        if (src) {
          $scope.$apply(function () {
            $scope.userIconSource = src;
            $scope.hasRobotSource = false;
          });
        } else {
          $scope.$apply(function () {
            $scope.userIconSource = '../assets/img/defaulticon_application.png';
            $scope.hasRobotSource = false;
          });
        }
      } else {
        $scope.$apply(function () {
          $scope.userIconSource = '../assets/img/defaulticon_application.png';
          $scope.hasRobotSource = false;
        });
      }
      $scope.$apply(function () {
        $scope.iconChanged = true;
      });
    };

    $scope.encodeURIComponent = window.encodeURIComponent;

    $scope.isFormDirty = function () {
      if (!$scope.selectedApplication) {
        return false;
      }
      var originalApplication = $scope.selectedApplication.$getOriginal();
      var currentApplication = $scope.selectedApplication;
      return currentApplication.publicId !== originalApplication.publicId || currentApplication.name !== originalApplication.name
      || currentApplication.organizationId !== originalApplication.organizationId || $scope.iconChanged;
    };

    //make sure user is aware they are about to lose changes
    $scope.$on('pageChangeStarted', function (event, destination) {
      var application = $scope.selectedApplication;
      if (!destination || (application && destination.indexOf('application/' + application.publicId) === -1)) {
        if ($scope.isFormDirty() && !$scope.isPostingIcon) {
          event.preventDefault();
        }
      }
    });

    $scope.$on('pageChangeAccepted', function () {
      $scope.cancel();
    });

    $scope.canSaveEdit = function () {
      return $scope.isFormDirty() && !$scope.applicationEditor.$invalid && !$scope.submitActive && $scope.selectedApplication.organizationId;
    };

    $scope.cancel = function () {
      if ($scope.selectedApplication) {
        $scope.selectedApplication.$revert();
        if ($scope.iconChanged) {
          $scope.userIconSource = $scope.origUserIconSource;
          $scope.iconChanged = false;
        }
      }
    };

    $scope.confirmDeleteApplication = function (application) {
      $scope.selectedApplication = application;
      $scope.deletedEnabled = true;
      $('#deleteApplicationModal').modal('show');
    };

    $scope.deleteApplication = function () {
      $scope.deletedEnabled = false;
      $http['delete'](CLMAppLocations.getEntityUrl()).success(function () {
        angular.forEach($scope.applications, function (applicationCandidate, key) {
          if (applicationCandidate.id === $scope.selectedApplication.id) {
            $scope.applications.splice(key, 1);
            return false;
          }
        });
        $('#deleteApplicationModal').modal('hide');
        $state.transitionTo('management.application');
      }).error(function () {
        $('#deleteApplicationModal').modal('hide');
        $scope.$broadcast('showServerError', arguments);
      });
    };

    // This needs to be invoked by onsubmit rather than ng-submit to suppress submit when necessary
    $scope.save = function () {
      if ($scope.submitActive) {
        return true;
      }

      if (!$scope.applicationEditor.$valid) {
        return false;
      }

      if (window.FormData) {
        var icon = angular.element('#file')[0];
        if (icon.files.length > 0) {
          if (icon.files[0].size > 5242880) {
            $scope.$apply(function () {
              $scope.pushAlert({ type: 'error', msg: 'Icon file size must be smaller than 5 MB.' });
            });
            return false;
          }
        }
      }

      $scope.submitActive = true;

      $scope.selectedApplication.$save().then(function() {
        me.saveIcon().then(function () {
          if ($state.params.applicationPublicId === '_new_') {
            $state.transitionTo('management.application.view.policies', { applicationPublicId: $scope.selectedApplication.publicId });
          }
        }, function(error) {
          if ($state.params.applicationPublicId === '_new_') {
            $state.current.data.passThroughAlerts.push({
              type : 'error',
              msg : 'An error occurred while saving the icon. (' + error + ')'
            });
            $state.transitionTo('management.application.view.policies', { applicationPublicId: $scope.selectedApplication.publicId });
          }
        });
      }, function(error) {
        $scope.alerts.push({
          type : 'error',
          msg : 'An error occurred while saving the application. (' + Messages.getHttpErrorMessage(error) + ')'
        });
      });

      return false;
    };
  });

  applicationModule.service('applicationStore', ['CLMLocations', 'CLMResource', function (clmLocations, clmResource) {
    var applicationStore = clmResource.getStore({
      id: 'id',
      url: clmLocations.getApplicationsUrl(),
      template: { id: null, publicId: null, name: null, organizationId: null },
      params: {
        timestamp: new Date().getTime()
      }
    });
    return applicationStore;
  }]);

  applicationModule.service('ApplicationId', ['commonCodeFactory', '$state', function (commonCodeFactory, $state) {
    // TODO Are ui-router parameters encoded or decoded?
    return {
      encoded: function () {
        var applicationPublicId = $state.params.applicationPublicId;
        return applicationPublicId ? encodeURI(applicationPublicId) : null;
      }
    };
  }]);
}());
