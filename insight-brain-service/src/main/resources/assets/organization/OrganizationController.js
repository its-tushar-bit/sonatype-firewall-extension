/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved. Includes
 *          the third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 *          trademark of Sonatype, Inc.
 */
/* global angular, $, clmBuildTimestamp, window */
(function() {
  'use strict';

  var organizationModule = angular.module('OrganizationModule', ['ui.router', 'ManagementModule', 'Organization'], [
        '$stateProvider', function($stateProvider) {
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
            templateUrl: '../application-assets/components/aoeditor.html?' + clmBuildTimestamp,
            resolve : {
              selectedOrganization : function ($q, $stateParams, OrganizationStore) {
                if ($stateParams.organizationId === '_new_')
                  return OrganizationStore.create();

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
              }
            }
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
            templateUrl: '../policy-assets/components/license-threat-group/license-threat-group.html?' +
                clmBuildTimestamp
          }).state('management.organization.view.security', {
            parent: 'management.organization.view',
            url: '/security',
            controller: 'AppSecurityController',
            templateUrl: '../policy-assets/components/app-security/app-security.html?' + clmBuildTimestamp
          }).state('management.organization.view.tags', {
              parent: 'management.organization.view',
              url: '/tags',
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

  organizationModule.controller('OrganizationController', [
    '$scope', '$state', '$http', '$location', 'CLMLocations', 'OrganizationStore',
    function($scope, $state, $http, $location, CLMLocations, OrganizationStore) {
      $scope.isCurrentTab = function(tabName) {
        return $state.current.name.lastIndexOf(tabName) === $state.current.name.length - tabName.length;
      };

      $scope.$state = $state;

      // Store icon cache timestamps at higher scope so it is not reinstantiated with editor controller
      $scope.organizationIconTimestamp = {};

      $scope.$on('$stateChangeSuccess', function(event, toState, toParams, fromState) {
        if (toState.data && toState.data.passThroughAlerts && fromState.data && fromState.data.passThroughAlerts) {
          angular.forEach(fromState.data.passThroughAlerts, function(alert) {
            toState.data.passThroughAlerts.push(alert);
          });
        }
      });

      $scope.doLoad = function() {
        $scope.error = null;
        OrganizationStore.get().then(function(results) {
          $scope.organizations = results;
        }, function(error) {
          $scope.error = error;
        });
      };

      $scope.doLoad();
    }
  ]);

  organizationModule.controller('OrganizationEditorController', [
    '$scope', '$state', '$location', '$http', '$rootScope', '$modal', 'regexFactory', 'CLMLocations', 'editorTools',
    'CLMAppLocations', 'Messages', 'CLMAppLocations', 'selectedOrganization', '$q',
    function($scope, $state, $location, $http, $rootScope, $modal, regexFactory, CLMLocations, editorTools,
             clmAppLocations, messages, CLMAppLocations, selectedOrganization, $q)
    {
      var me = this;
      angular.extend(me,
          editorTools.getEditorController($scope, 'selectedOrganization.id', angular.element('[name=organizationId]'),
              angular.element('#iconUploadForm')));

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

      function isExternalDestination(destination) {
        var organization = $scope.selectedOrganization;
        return !destination || (organization && destination.indexOf('organization/' + organization.id) === -1);
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
            return $state.params.organizationId === "_new_";
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

      $scope.fileChanged = function(element) {
        $scope.$apply(function() {
          $scope.userIconSource = me.getIconSource(element, '../assets/img/defaulticon_organization.png');
          $scope.hasRobotSource = false;
          $scope.iconChanged = true;
        });
      };

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

        $scope.selectedOrganization.$save().then(function(data) {
          me.saveIcon().then(function() {
            if ($state.params.organizationId === '_new_') {
              $state.transitionTo('management.organization.view.policies',
                  { organizationId: $scope.selectedOrganization.id });
            }
          }, function(error) {
            if ($state.params.organizationId === '_new_') {
              $state.current.data.passThroughAlerts.push({
                type: 'error',
                msg: 'An error occurred while saving the icon. (' + error + ')'
              });
              $state.transitionTo('management.organization.view.policies',
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
          templateUrl : 'delete-org-modal',
          resolve : {
            selected : function () {
              return $scope.selectedOrganization;
            }
          }
        }).result.then(function () {
          $rootScope.$broadcast('organizations.delete', $scope.selectedOrganization.id);
          $state.transitionTo('management.organization');
        }, function (error) {
          if (error) {
            $scope.$broadcast('showServerError', error);
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
      
      $scope.openEvalute = function () {
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

  organizationModule.service('OrganizationStore', [
    'CLMLocations', 'CLMResource', function(CLMLocations, clmResource) {
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
    }
  ]);

  organizationModule.service('OrganizationId', function($state) {
    return {
      encoded: function() {
        var organizationId = $state.params.organizationId;
        return organizationId ? encodeURI(organizationId) : null;
      }
    };
  });
}());
