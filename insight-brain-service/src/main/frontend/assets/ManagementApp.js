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
    '$q', '$scope', '$state', '$stateParams', 'OrganizationStore', 'ApplicationStore',
    function($q, $scope, $state, $stateParams, organizationStore, applicationStore) {
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

      function create(organizations, applications) {
        var organizationApplications = [];

        for (var i = 0; i < organizations.length; i++) {
          var organization = organizations[i];
          var organizationApplication = {
            id: organization.id,
            name: organization.name,
            applications: [],
            isVisible: true
          };

          for (var j = applications.length - 1; j >= 0; j--) {
            var application = applications[j];
            if (application.organizationId === organization.id) {
              organizationApplication.applications.push({
                id: application.id,
                name: application.name,
                publicId: application.publicId,
                isVisible: true
              });

              applications.splice(j, 1);
            }
          }

          organizationApplication.isExpanded = isOrganizationOrChildSelected(organizationApplication);
          organizationApplications.push(organizationApplication);
        }

        $scope.organizations = organizationApplications;
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
              keys: [
                'publicId',
                'name'
              ]
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
          var organizations = angular.copy(results[0]),
              applications = angular.copy(results[1]);

          create(organizations, applications);
        }, function(error) {
          $scope.error = error;
        });
      };

      $scope.$watch('filter.value', function() {
        filter();
      }, function(error) {
        $scope.error = error;
      });

      $scope.doLoad();
  }]);
}());
