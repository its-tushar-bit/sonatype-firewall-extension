/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, clmBuildTimestamp, window */
(function() {
  'use strict';

  angular.module('OrganizationModule', ['ui.router', 'ManagementModule', 'Organization', 'Stores'], [
    '$stateProvider', function($stateProvider) {
      $stateProvider.state('management.organization', {
        parent: 'management',
        url: '/organization/{organizationId}',
        controller: 'OrganizationEditorController',
        data: {
          passThroughAlerts: []
        },
        templateUrl: '../application-assets/components/aoeditor.html?' + clmBuildTimestamp,
        resolve : {
          selectedOrganization : ['$q', '$stateParams', 'OrganizationStore', function ($q, $stateParams, OrganizationStore) {
            if ($stateParams.organizationId === '_new_') {
              return OrganizationStore.create();
            }

            var deferred = $q.defer();
            OrganizationStore.get().then(function (data) {
              for (var i=0; i<data.length; i++) {
                if (data[i].id === $stateParams.organizationId) {
                  deferred.resolve(data[i].$clone());
                  return;
                }
              }
              deferred.resolve(null);
            }, /* Errors will be handled at state parent */ angular.noop);
            return deferred.promise;
          }]
        }
      }).state('management.organization.policies', {
        parent: 'management.organization',
        url: '/policies',
        controller: 'PolicyController',
        data: {
          passThroughAlerts: []
        },
        templateUrl: '../policy-assets/components/policy/policy.html?' + clmBuildTimestamp
      }).state('management.organization.policies.new', {
        parent: 'management.organization.policies',
        url: '/new',
        controller: 'PolicyController',
        data: {
          passThroughAlerts: []
        },
        templateUrl: '../policy-assets/components/policy/policy.html?' + clmBuildTimestamp
      }).state('management.organization.labels', {
        parent: 'management.organization',
        url: '/labels',
        controller: 'LabelController',
        templateUrl: '../policy-assets/components/label-editor/labels.html?' + clmBuildTimestamp
      }).state('management.organization.labels.new', {
        parent: 'management.organization.labels',
        url: '/new',
        controller: 'LabelController',
        data: {
          passThroughAlerts: []
        },
        templateUrl: '../policy-assets/components/label-editor/labels.html?' + clmBuildTimestamp
      }).state('management.organization.licenses', {
        parent: 'management.organization',
        url: '/licenses',
        controller: 'LicenseThreatGroupController',
        templateUrl: '../policy-assets/components/license-threat-group/license-threat-group.html?' +
            clmBuildTimestamp
      }).state('management.organization.licenses.new', {
        parent: 'management.organization.licenses',
        url: '/new',
        controller: 'LicenseThreatGroupController',
        data: {
          passThroughAlerts: []
        },
        templateUrl: '../policy-assets/components/license-threat-group/license-threat-group.html?' +
        clmBuildTimestamp
      }).state('management.organization.security', {
        parent: 'management.organization',
        url: '/security',
        controller: 'AppSecurityController',
        templateUrl: '../policy-assets/components/app-security/app-security.html?' + clmBuildTimestamp,
        resolve : {
          isAuthorized : function () {
            return true;
          }
        }
      }).state('management.organization.tags', {
        parent: 'management.organization',
        url: '/tags',
        controller: 'TagController',
        templateUrl: '../policy-assets/components/tag-editor/tags.html?' + clmBuildTimestamp
      }).state('management.organization.tags.new', {
        parent: 'management.organization.tags',
        url: '/new',
        controller: 'TagController',
        templateUrl: '../policy-assets/components/tag-editor/tags.html?' + clmBuildTimestamp
      });
    }
  ]);
}());

(function() {
  'use strict';

  var organizationModule = angular.module('Organization',
      ['AngularCommon', 'ApplicationSecurityModule', 'CLMAppLocation', 'CommonServices', 'EditorTools', 'Labels',
       'LicenseThreatGroup', 'Policy', 'ResourceModule', 'Tags', 'ui.router']);

  organizationModule.controller('OrganizationEditorController', [
    '$scope', '$state', '$location', '$http', '$rootScope', '$modal', 'regexFactory', 'CLMLocations', 'editorTools',
    'CLMAppLocations', 'Messages', 'CLMAppLocations', 'selectedOrganization', 'ErrorDialog', 'LastSelectedOrganization',
    function($scope, $state, $location, $http, $rootScope, $modal, regexFactory, CLMLocations, editorTools,
             clmAppLocations, messages, CLMAppLocations, selectedOrganization, ErrorDialog, LastSelectedOrganization)
    {
      var me = this;
      angular.extend(me,
          editorTools.getEditorController($scope, 'selectedOrganization.id', angular.element('[name=organizationId]'),
              angular.element('#iconUploadForm')));

      $scope.isCurrentTab = function(tabName) {
        return $state.current.name.lastIndexOf(tabName) === $state.current.name.length - tabName.length;
      };

      // Store icon cache timestamps at higher scope so it is not reinstantiated with editor controller
      $scope.organizationIconTimestamp = {};

      $scope.$on('$stateChangeSuccess', function(event, toState, toParams, fromState) {
        if (toState.data && toState.data.passThroughAlerts && fromState.data && fromState.data.passThroughAlerts) {
          angular.forEach(fromState.data.passThroughAlerts, function(alert) {
            toState.data.passThroughAlerts.push(alert);
          });
        }
      });

      // Organization Editor controller will take care of managing its own icons
      function setOrganizationIcon() {
        if ($scope.selectedOrganization.id === null) {
          $scope.origUserIconSource = $scope.userIconSource = '../assets/img/defaulticon_organization.png';
        }
        // Reset icon cache on initial load and when icon is changed
        else if (!$scope.organizationIconTimestamp[$scope.selectedOrganization.id]) {
          resetIconCache();
        }
        else {
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
        return '../rest/organization/icon/' + encodeURIComponent($scope.selectedOrganization.id) + '?' +
            $scope.organizationIconTimestamp[$scope.selectedOrganization.id];
      }

      function isExternalDestination(toState, toParams) {
        var organization = $scope.selectedOrganization;

        // Navigating outside IQ or to unknown state
        if (!toState || !toParams) {
          return true;
        }

        // Organization not loaded yet
        if (!organization) {
          return true;
        }

        // Navigating to a state outside of the current organization
        return toState.parent.indexOf('management.organization') !== 0 || toParams.organizationId !== organization.id;
      }

      function doLoad() {
        $scope.ao = {
          addSync : clmAppLocations.addIconSync(),
          getId : function () {
            if (this.selected) {
              return this.selected.id || $state.params.organizationId;
            }
            return $state.params.organizationId;
          },
          getPublicId : function () {
            return this.getId();
          },
          isNew : function () {
            return $state.params.organizationId === '_new_';
          },
          type : 'organization',
          typeName : 'Organization'
        };

        if (selectedOrganization !== null) {
          $scope.selectedOrganization = selectedOrganization;
          $scope.$state = $state;
          $scope.submitActive = false;

          $scope.ao.selected = $scope.selectedOrganization;
          $scope.ao.siblings = $scope.organizations;

          setOrganizationIcon();
        }
      }

      $scope.$on('resetIconCache', resetIconCache);

      //make sure user is aware they are about to lose changes
      $scope.$on('pageChangeStarted', function(event, toState, toParams) {
        if (isExternalDestination(toState, toParams)) {
          if ($scope.isFormDirty() && !$scope.isPostingIcon) {
            event.preventDefault();
          }
        }
      });

      $scope.$on('pageChangeAccepted', function(event, toState, toParams) {
        if (isExternalDestination(toState, toParams)) {
          $scope.cancel();
        }
      });
      
      $scope.$on('$stateChangeStart', function(event, toState){
        //if we are going to the new app page, make sure to set the default org
        if ($scope.selectedOrganization && toState.name === 'management.application') {
          LastSelectedOrganization.set({
            id: $scope.selectedOrganization.id,
            name: $scope.selectedOrganization.name
          });
        }
      });

      if ($state.current.data && $state.current.data.passThroughAlerts) {
        angular.forEach($state.current.data.passThroughAlerts, function(alert) {
          $scope.pushAlert(alert);
        });
      }

      $scope.closeAlert = function(index) {
        $scope.alerts.splice(index, 1);
      };

      $scope.generateIcon = function() {
        me.generateIcon($scope.selectedOrganization.name);
      };

      $scope.$watch('selectedFile', function (newValue, oldValue) {
        var element = angular.element('#file');

        if (oldValue !== newValue) {
          $scope.userIconSource = me.getIconSource(element[0], '../assets/img/defaulticon_organization.png');
          $scope.hasRobotSource = false;
          $scope.iconChanged = true;
        }
      });

      $scope.encodeURIComponent = window.encodeURIComponent;

      $scope.canSaveEdit = function() {
        return !$scope.aoEditor.$invalid && !$scope.submitActive && ($scope.aoEditorName && $scope.aoEditorName.$visible || $scope.ao.selected.name);
      };

      $scope.cancel = function() {
        $scope.selectedOrganization.$revert();
        if ($scope.iconChanged) {
          $scope.userIconSource = $scope.origUserIconSource;
          $scope.hasRobotSource = false;
          $scope.iconChanged = false;
        }
      };

      $scope.isFormDirty = function() {
        if (!$scope.selectedOrganization) {
          return false;
        }
        var originalOrganization = $scope.selectedOrganization.$getOriginal();
        var currentOrganization = $scope.selectedOrganization;
        return currentOrganization.name !== originalOrganization.name || $scope.iconChanged;
      };

      // This needs to be invoked by onsubmit rather than ng-submit to
      // suppress submit when necessary
      $scope.save = function() {
        if ($scope.submitActive) {
          return true;
        }

        if ($scope.aoEditor.$invalid) {
          return false;
        }
        if ($scope.aoEditorName.$visible) {
          $scope.aoEditorName.$save();
        }

        if (window.FormData) {
          var icon = angular.element('#file')[0];
          if (icon.files.length > 0) {
            if (icon.files[0].size > 5242880) {
              $scope.$apply(function() {
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

        $scope.selectedOrganization.$save().then(function() {
          me.saveIcon().then(function() {
            if ($state.params.organizationId === '_new_') {
              $state.transitionTo('management.organization.policies',
                  { organizationId: $scope.selectedOrganization.id });
            }
          }, function(error) {
            if ($state.params.organizationId === '_new_') {
              $state.current.data.passThroughAlerts.push({
                type: 'error',
                msg: 'An error occurred while saving the icon. (' + error + ')'
              });
              $state.transitionTo('management.organization.policies',
                  { organizationId: $scope.selectedOrganization.id });
            }
          });
        }, function(error) {
          $scope.submitActive = false;
          $scope.alerts.push({
            type: 'error',
            msg: 'An error occurred while saving the organization. (' + messages.getHttpErrorMessage(error) + ')'
          });
        });

        return false;
      };

      $scope.confirmDelete = function() {
        $modal.open({
          backdrop : 'static',
          keyboard : true,
          controller : 'DeleteResourceController',
          templateUrl : 'delete-org-modal-template',
          resolve : {
            selected : function () {
              return $scope.selectedOrganization;
            }
          }
        }).result.then(function () {
          $rootScope.$broadcast('organizations.delete', $scope.selectedOrganization.id);
          $state.transitionTo('management');
        }, function (error) {
          if (error) {
            ErrorDialog.open(error[0]);
          }
        });
      };

      $scope.openImport = function () {
        $modal.open({
          backdrop : 'static',
          keyboard : false,
          templateUrl : 'import-policy-modal',
          controller : 'ImportPolicyController'
        }).result.then(function () {
          $scope.$broadcast('refresh', $scope.selectedOrganization);
        }, angular.noop);
      };
      
      $scope.openEvaluate = function () {
        $modal.open({
          backdrop : 'static',
          keyboard : false,
          templateUrl : 'evaluate-bundle-modal',
          controller : 'EvaluateBundleController',
          resolve: {
            selectedApplication: function() {
              return null;
            }
          }
        }).result.then(function () {
          $scope.$broadcast('refresh', $scope.selectedOrganization);
        }, angular.noop);
      };

      if (!$scope.organizations) {
        $scope.$watch('organizations', function () {
          doLoad();
        });
      } else {
        doLoad();
      }
    }
  ]);
}());
