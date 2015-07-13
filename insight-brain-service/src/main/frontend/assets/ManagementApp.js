/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
 /* global Fuse */
(function() {
  'use strict';
  angular.module('managementApp',
    ['MainModule', 'OrganizationModule', 'ApplicationModule', 'Configuration', 'UserModule', 'RoleModule', 'LdapConfiguration']);
}());

(function() {
  'use strict';

  var managementModule = angular.module('ManagementModule', ['ui.router', 'Stores'], ['$stateProvider', function($stateProvider) {
    $stateProvider.state('management', {
      url: '/management',
      templateUrl: '../assets/management.html?' + clmBuildTimestamp,
      controller: 'ManagementController',
      data : {
        title : 'Management'
      }
    });
  }]);

  managementModule.controller('ManagementController', ['$scope', '$state', 'commonCodeFactory', function($scope, $state, commonCodeFactory) {
    $scope.$state = $state;
    $scope.syncAlerts = [];
    var error = commonCodeFactory.getEncodedQueryString('errorMessage');
    if (error) {
      $scope.syncAlerts.push({ type: 'error', msg: decodeURIComponent(error) });
    }
  }]);

  managementModule.controller('OwnerTreeViewController', [
    '$q', '$scope', '$state', '$stateParams', 'OrganizationStore', 'ApplicationStore', 'OwnerEditor',
    function($q, $scope, $state, $stateParams, organizationStore, applicationStore, OwnerEditor) {
      var organizations, applications;

      function newApplication(applicationResource) {
        var application = {
          id: applicationResource.id,
          name: applicationResource.name,
          organizationId: applicationResource.organizationId,
          publicId: applicationResource.publicId,
          isVisible: true
        };

        $scope.$watch(function() {
          return applicationResource.name;
        }, function(newApplicationName) {
          application.name = newApplicationName;
        });

        return application;
      }

      function newOrganization(organizationResource) {
        var organization = {
          id: organizationResource.id,
          name: organizationResource.name,
          applications: [],
          isVisible: true
        };

        $scope.$watch(function() {
          return organizationResource.name;
        }, function(newOrganizationName) {
          organization.name = newOrganizationName;
        });

        $scope.$watch(function() {
          return applications.length;
        }, function() {
          var organizationApplications = [];
          for (var i = 0; i < applications.length; i++) {
            if (applications[i].organizationId === organization.id) {
              organizationApplications.push(applications[i]);
            }
          }

          ownerCollectionChanged(organization.applications, organizationApplications, newApplication);
          organization.isExpanded = isOrganizationOrChildSelected(organization);
        });

        return organization;
      }

      function isOrganizationOrChildSelected(organization) {
        var isOrganizationViewed = $state.includes('management.organization-view', {organizationId: organization.id});
        if (isOrganizationViewed) {
          return true;
        }
        var isApplicationState = $state.includes('management.application-view');
        if (!isApplicationState) {
          return false;
        }

        for (var i = 0; i < organization.applications.length; i++) {
          var application = organization.applications[i];
          var isApplicationViewed = $stateParams.applicationPublicId === application.publicId;
          if (isApplicationViewed) {
            return true;
          }
        }

        return false;
      }

      function filter() {
        if (!$scope.organizations) {
          return;
        }

        var filterValue = $scope.filter.value;
        var filteredOrganizations = [];
        if (filterValue && filterValue.length >= 3) {
          var organizationFuse = new Fuse($scope.organizations, {
            id: 'id',
            threshold: 0.3,
            keys: [ 'name' ]
          });

          filteredOrganizations = organizationFuse.search(filterValue);
        }

        for (var i = 0; i < $scope.organizations.length; i++) {
          var organization = $scope.organizations[i],
              organizationVisible = false,
              anyApplicationVisible = false,
              filteredApplications;

          if (!filterValue || filterValue.length < 3 || filteredOrganizations.indexOf(organization.id) > -1) {
            organizationVisible = true;
          }

          if (filterValue && filterValue.length >= 3) {
            var applicationFuse = new Fuse(organization.applications, {
              id: 'id',
              threshold: 0.3,
              keys: [ 'name' ]
            });
            filteredApplications = applicationFuse.search(filterValue);
          }

          for (var j = 0; j < organization.applications.length; j++) {
            var application = organization.applications[j];

            application.isVisible = organizationVisible || !filterValue || filterValue.length < 3 ||
              filteredApplications.indexOf(application.id) > -1;
            anyApplicationVisible = anyApplicationVisible || application.isVisible;
          }

          organization.isExpanded = !filterValue ||
            filterValue.length < 3 ? organization.isExpanded : anyApplicationVisible;
          organization.isVisible = organizationVisible || anyApplicationVisible;
        }
      }

      // Check for added and deleted records between ownerCollection and newOwnerCollection. Update ownerCollection accordingly
      function ownerCollectionChanged(ownerCollection, newOwnerCollection, newOwnerConstructor) {
        var i, j,
            // Copy the collections as to not affect the underlying Resource Store nor the tree view model
            ownerCollectionCopy = angular.copy(ownerCollection),
            newOwnerCollectionCopy = angular.copy(newOwnerCollection);

        // Remove all entries existing in both collections
        for (i = ownerCollectionCopy.length - 1; i >= 0; i--) {
          var ownerCopy = ownerCollectionCopy[i];
          for (j = newOwnerCollectionCopy.length - 1; j >= 0; j--) {
            var newOwnerCopy = newOwnerCollectionCopy[j];
            if (ownerCopy.id === newOwnerCopy.id) {
              ownerCollectionCopy.splice(i, 1);
              newOwnerCollectionCopy.splice(j, 1);
              break;
            }
          }
        }

        // Add all new entries
        for (i = 0; i < newOwnerCollectionCopy.length; i++) {
          for (j = 0; j < newOwnerCollection.length; j++) {
            // Add the original entry to associate tree view item with the underlying data
            if (newOwnerCollectionCopy[i].id === newOwnerCollection[j].id) {
              var newOwner = newOwnerConstructor(newOwnerCollection[j]);
              ownerCollection.push(newOwner);
              break;
            }
          }
        }

        // Remove all deleted entries
        for (i = 0; i < ownerCollectionCopy.length; i++) {
          for (j = ownerCollection.length - 1; j >= 0; j--) {
            // Remove from the original collection
            if (ownerCollectionCopy[i].id === ownerCollection[j].id) {
              ownerCollection.splice(j, 1);
              break;
            }
          }
        }
      }

      $scope.$state = $state;
      $scope.filter = {
        value: ''
      };

      $scope.doLoad = function() {
        delete $scope.error;

        var loadPromises = [
          organizationStore.refresh(),
          applicationStore.refresh()
        ];

        $q.all(loadPromises).then(function(results) {
          $scope.organizations = [];
          organizations = results[0];
          applications = results[1];

          for (var i = 0; i < organizations.length; i++) {
            var organization = organizations[i];
            var organizationApplication = newOrganization(organization);
            $scope.organizations.push(organizationApplication);
          }

          $scope.$watch(function() {
            return organizations.length;
          }, function() {
            if ($scope.organizations.length !== organizations.length) {
              ownerCollectionChanged($scope.organizations, organizations, newOrganization);
            }
          });
        }, function(error) {
          $scope.error = error;
        });
      };

      $scope.createApplication = function (parent) {
        var application = applicationStore.create();
        application.organizationId = parent.id;
        OwnerEditor.open(application, 'application', applications);
      };

      $scope.createOrganization = function () {
        OwnerEditor.open(organizationStore.create(), 'organization', organizations);
      };

      $scope.$watch('filter.value', function() {
        filter();
      }, function(error) {
        $scope.error = error;
      });

      $scope.doLoad();
  }]);
}());
