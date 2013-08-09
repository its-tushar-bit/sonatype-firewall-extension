/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved. Includes
 *          the third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 *          trademark of Sonatype, Inc.
 */
/* global angular, $, clmBuildTimestamp, window */
(function () {
  'use strict';

  var organizationModule = angular.module('OrganizationModule', ['ui.compat', 'ManagementModule', 'Organization', 'CommonServices', 'CLMLocation'], ['$stateProvider', function ($stateProvider) {
    $stateProvider.state('management.organization', {
      parent: 'management',
      url: '/organization',
      controller: 'OrganizationController',
      templateUrl: '../organization-assets/components/organization-navigator.html?' + clmBuildTimestamp
    }).state('management.organization.view', {
      parent: 'management.organization',
      url: '/{organizationId}',
      controller: 'OrganizationEditorController',
      data: {
        passThroughAlerts: []
      },
      templateUrl: '../organization-assets/components/organization-editor.html?' + clmBuildTimestamp
    }).state('management.organization.view.policies', {
      parent: 'management.organization.view',
      url: '/policies',
      controller: 'PolicyController',
      data: {
        passThroughAlerts: []
      },
      templateUrl: '../policy-assets/components/policy/policy.html?' + clmBuildTimestamp
    }).state('management.organization.view.labels', {
      parent: 'management.organization.view',
      url: '/labels',
      controller: 'LabelController',
      templateUrl: '../policy-assets/components/label-editor/labels.html?' + clmBuildTimestamp
    }).state('management.organization.view.licenses', {
      parent: 'management.organization.view',
      url: '/licenses',
      controller: 'LicenseThreatGroupController',
      templateUrl: '../policy-assets/components/license-threat-group/license-threat-group.html?' + clmBuildTimestamp
    });
  }]);
}());

(function () {
  'use strict';

  var organizationModule = angular.module('Organization', [ 'AngularCommon', 'ui.compat', 'CLMAppLocation', 'ResourceModule', 'EditorTools' ]);

  organizationModule.controller('OrganizationController', [ '$scope', '$state', '$http', '$location', '$timeout', 'hudson', 'CLMLocations', 'OrganizationStore', function ($scope, $state, $http, $location, $timeout, hudson, CLMLocations, OrganizationStore) {
    function switchOrganization() {
      $scope.selectedOrganization = null;
      $scope.userIconSource = null;
      if ('_new_' == $scope.$state.params.organizationId) {
        $timeout(function () {
          $scope.selectedOrganization = OrganizationStore.create();
          $scope.origUserIconSource = $scope.userIconSource = '../assets/img/defaulticon_organization.png';
        }, 100);
      }
      if ($scope.$state.params.organizationId !== null && $scope.organizations) {
        for (var i = 0; i < $scope.organizations.length; i++) {
          if ($scope.$state.params.organizationId === $scope.organizations[i].id) {
            $timeout(function () {
              // don't want to infect the original data
              $scope.selectedOrganization = $scope.organizations[i].$clone();
              $scope.$broadcast('setOrganizationIcon');
            }, 100);
            return;
          }
        }
      }
    }

    $scope.isCurrentTab = function (tabName) {
      return $state.current.name.lastIndexOf(tabName) === $state.current.name.length - tabName.length;
    };


    $scope.$state = $state;

    // Store icon cache timestamps at higher scope so it is not reinstantiated with editor controller
    $scope.organizationIconTimestamp = {};

    $scope.$on('$stateChangeSuccess', function(event, toState, toParams, fromState){
      if (toState.data && toState.data.passThroughAlerts && fromState.data && fromState.data.passThroughAlerts) {
        angular.forEach(fromState.data.passThroughAlerts, function(alert) {
          toState.data.passThroughAlerts.push(alert);
        });
      }
    });

    $scope.doLoad = function () {
      $scope.error = null;
      OrganizationStore.get().then(function (results) {
        $scope.organizations = results;
        $scope.$watch('$state.params.organizationId', switchOrganization);
        switchOrganization();
      }, function (error) {
        $scope.error = error;
      });
    };

    $scope.doLoad();
  } ]);

  organizationModule.controller('OrganizationEditorController', [ '$scope', '$state', '$location', '$http', '$rootScope', 'regexFactory', 'CLMLocations', 'hudson', 'editorTools', 'CLMAppLocations', 'Messages', 'CLMAppLocations', function ($scope, $state, $location, $http, $rootScope, regexFactory, CLMLocations, hudson, editorTools, clmAppLocations, messages, CLMAppLocations) {
    var me = this;
    angular.extend(me, editorTools.getEditorController($scope, 'selectedOrganization.id', angular.element('[name=organizationId]'), angular.element('#iconUploadForm')));

    // Organization Editor controller will take care of managing its own icons
    function setOrganizationIcon() {
      // Reset icon cache on initial load and when icon is changed
      if (!$scope.organizationIconTimestamp[$scope.selectedOrganization.id]) {
        resetIconCache();
      } else {
        $scope.origUserIconSource = $scope.userIconSource = getUserIconSource();
      }
    }

    function resetIconCache() {
      if ($scope.selectedOrganization) {
        $scope.organizationIconTimestamp[$scope.selectedOrganization.id] = new Date().getTime();
        $scope.origUserIconSource = $scope.userIconSource = getUserIconSource();
      }
    }

    function getUserIconSource() {
      return '../rest/organization/icon/' + encodeURIComponent($scope.selectedOrganization.id) + '?' + $scope.organizationIconTimestamp[$scope.selectedOrganization.id];
    }

    $scope.addOrganizationSync = clmAppLocations.addIconSync();
    $scope.$on('setOrganizationIcon', setOrganizationIcon);
    $scope.$on('resetIconCache', resetIconCache);

    $scope.$state = $state;
    $scope.submitActive = false;

    if ($state.current.data && $state.current.data.passThroughAlerts) {
      angular.forEach($state.current.data.passThroughAlerts, function(alert) {
        $scope.pushAlert(alert);
      });
    }

    $scope.validateName = function (value) {
      $scope.organizationEditor.$invalid = false;

      var result = editorTools.validateName(value, $scope.selectedOrganization, $scope.organizations);

      if (result !== true) {
        $scope.organizationEditor.$invalid = true;
        return result;
      }
    };

    $scope.closeAlert = function (index) {
      $scope.alerts.splice(index, 1);
    };

    $scope.generateIcon = function () {
      me.generateIcon($scope.selectedOrganization.name);
    };

    $scope.fileChanged = function (element) {
      $scope.$apply(function () {
        $scope.userIconSource = me.getIconSource(element, '../assets/img/defaulticon_organization.png');
        $scope.hasRobotSource = false;
        $scope.iconChanged = true;
      });
    };

    $scope.encodeURIComponent = window.encodeURIComponent;

    $scope.canSaveEdit = function () {
      return !$scope.organizationEditor.$invalid && !$scope.submitActive;
    };

    $scope.cancelClick = function () {
      $scope.selectedOrganization.$revert();
      if ($scope.iconChanged) {
        $scope.userIconSource = $scope.origUserIconSource;
        $scope.iconChanged = false;
      }
    };

    $scope.isFormDirty = function () {
      if (!$scope.selectedOrganization) {
        return false;
      }
      var originalOrganization = $scope.selectedOrganization.$getOriginal();
      var currentOrganization = $scope.selectedOrganization;
      return currentOrganization.name !== originalOrganization.name || $scope.iconChanged;
    };

    // This needs to be invoked by onsubmit rather than ng-submit to
    // suppress submit when necessary
    $scope.saveClick = function () {
      if ($scope.submitActive) {
        return true;
      }

      if ($scope.organizationEditor.$invalid) {
        return false;
      }

      if (window.FormData) {
        var icon = angular.element('#file')[0];
        if (icon.files.length > 0) {
          if (icon.files[0].size > 5242880) {
            $scope.$apply(function () {
              $scope.alerts.push({
                type: 'error',
                msg: 'Icon file size must be smaller than 5 MB.'
              });
            });
            return false;
          }
        }
      }

      $scope.submitActive = true;

      $scope.selectedOrganization.$save().then(function (data) {
        me.saveIcon().then(function () {
          if ($state.params.organizationId === '_new_') {
            $state.transitionTo('management.organization.view.policies', { organizationId: $scope.selectedOrganization.id });
          }
        }, function(error) {
          if ($state.params.organizationId === '_new_') {
            $state.current.data.passThroughAlerts.push({
              type : 'error',
              msg : 'An error occurred while saving the icon. (' + error + ')'
            });
            $state.transitionTo('management.organization.view.policies', { organizationId: $scope.selectedOrganization.id });
          }
        });
      }, function (error) {
        $scope.submitActive = false;
        $scope.alerts.push({
          type: 'error',
          msg: 'An error occurred while saving the organization. (' + messages.getHttpErrorMessage(error) + ')'
        });
      });

      return false;
    };

    $scope.confirmDeleteOrganization = function (Organization) {
      $scope.selectedOrganization = Organization;
      $scope.deletedEnabled = true;
      $('#deleteOrganizationModal').modal('show');
    };

    $scope.deleteOrganization = function() {
      $scope.deletedEnabled = false;
      $('#deleteOrganizationModal').modal('hide');
      $scope.selectedOrganization.$delete().then(function(){
        $rootScope.$broadcast('organizations.delete', $scope.selectedOrganization.id);
        $state.transitionTo('management.organization');
      },function(){
        $scope.$broadcast('showServerError', arguments)
      });
    };
  } ]);

  organizationModule.service('OrganizationStore', [ 'CLMLocations', 'CLMResource', function (CLMLocations, clmResource) {
    return clmResource.getStore({
      id: 'id',
      url: CLMLocations.getOrganizationsUrl(),
      template: {
        id: null,
        name: null
      },
      params: {
        timestamp: new Date().getTime()
      }
    });
  } ]);


  organizationModule.service('OrganizationId', function ($state) {
    return {
      encoded: function () {
        var organizationId = $state.params.organizationId;
        return organizationId ? encodeURI(organizationId) : null;
      }
    };
  });
}());
