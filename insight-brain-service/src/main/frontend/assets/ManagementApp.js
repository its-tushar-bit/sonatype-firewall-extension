/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
 /* global angular, clmBuildTimestamp */
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

  managementModule.controller('OrganizationTreeViewController', [
    '$q', '$scope', '$state', 'OrganizationStore', 'ApplicationStore', 'organizationTreeViewFactory',
    function($q, $scope, $state, organizationStore, applicationStore, organizationTreeViewFactory) {
      $scope.$state = $state;

      var loadPromises = [
        organizationStore.refresh(),
        applicationStore.refresh()
      ];

      $q.all(loadPromises).then(function(results) {
        var organizations = results[0],
            applications = results[1];

        $scope.organizations = organizationTreeViewFactory.create(organizations, applications);
      });

      $scope.$watch('filter', function() {
        organizationTreeViewFactory.filter($scope.organizations, $scope.filter);
      }, function(error) {
        $scope.error = error;
      });
  }]);

  managementModule.factory('organizationTreeViewFactory', ['$state', '$stateParams', function($state, $stateParams) {
    var me = {};
    me.isOrganizationOrChildSelected = function(organization) {
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
        var isApplicationViewed = $stateParams.applicationPublicId == application.publicId;
        if (isApplicationViewed) {
          return true;
        }
      }

      return false;
    };
    me.create = function(organizations, applications) {
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
          if (application.organizationId == organization.id) {
            organizationApplication.applications.push({
              id: application.id,
              name: application.name,
              publicId: application.publicId,
              isVisible: true
            });

            applications.splice(j, 1);
          }
        }

        organizationApplication.isExpanded = me.isOrganizationOrChildSelected(organizationApplication);
        organizationApplications.push(organizationApplication);
      }

      return organizationApplications;
    };
    me.filter = function(organizations, filter) {
      if (!organizations) {
        return;
      }

      var filteredOrganizations = [];
      if (filter && filter.length >= 3) {
        var organizationFuse = new Fuse(organizations, {
          id: 'id',
          threshold: 0.3,
          keys: [
            'id',
            'name'
          ]
        });

        filteredOrganizations = organizationFuse.search(filter);
      }

      for (var i = 0; i < organizations.length; i++) {
        var organization = organizations[i],
            organizationVisible = false,
            anyApplicationVisible = false,
            filteredApplications;

        if (!filter || filter.length < 3 || filteredOrganizations.indexOf(organization.id) > -1) {
          organizationVisible = true;
        }

        if (filter && filter.length >= 3) {
          var applicationFuse = new Fuse(organization.applications, {
            id: 'id',
            threshold: 0.3,
            keys: [
              'id',
              'publicId',
              'name'
            ]
          });
          filteredApplications = applicationFuse.search(filter);
        }

        for (var j = 0; j < organization.applications.length; j++) {
          var application = organization.applications[j];

          application.isVisible = organizationVisible || !filter || filter.length < 3 ||
          filteredApplications.indexOf(application.id) > -1;
          anyApplicationVisible = anyApplicationVisible || application.isVisible;
        }

        organization.isExpanded = !filter || filter.length < 3 ? organization.isExpanded : anyApplicationVisible;
        organization.isVisible = organizationVisible || anyApplicationVisible;
      }
    };
    return me;
  }]);
}());
