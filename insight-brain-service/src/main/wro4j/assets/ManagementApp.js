/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
 /* global angular, clmBuildTimestamp, Fuse */
(function() {
  'use strict';
  angular.module('managementApp',
      [
        'MainModule', 'OrganizationModule', 'ApplicationModule', 'Configuration', 'UserModule', 'RoleModule',
        'LdapConfiguration', 'repository.manager.module'
      ]);
}());

(function() {
  'use strict';

  var managementModule = angular.module('ManagementModule', ['ui.router', 'root.organization.migrate'], ['$stateProvider', function($stateProvider) {
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
    '$q', '$scope', '$state', '$stateParams', '$timeout', 'OrganizationStore', 'ApplicationStore', 'ProductFeatures',
    function($q, $scope, $state, $stateParams, $timeout, organizationStore, applicationStore, ProductFeatures) {
      var organizations, applications, organizationWatcher, applicationWatcher,
          lastOrganizations = [], lastApplications = [];

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
          parentOrganizationId: organizationResource.parentOrganizationId,
          applications: [],
          isVisible: true,
          isExpanded: $state.includes('management.organization', { organizationId: organizationResource.id} )
        };

        $scope.$watch(function() {
          return organizationResource.name;
        }, function(newOrganizationName) {
          organization.name = newOrganizationName;
        });

        return organization;
      }

      function isOrganizationChildSelected(organization) {
        var isApplicationState = $state.includes('management.application');
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

      function organizationsCollectionChanged() {
        var difference = getCollectionDifference(organizations, lastOrganizations);

        if (difference.removed) {
          difference.removed.forEach(function(removedOrganization) {
            $scope.organizations.some(function(organization, organizationIndex) {
              if (removedOrganization.id === organization.id) {
                $scope.organizations.splice(organizationIndex, 1);
                return true;
              }
            });
          });
        }
        if (difference.added) {
          difference.added.forEach(function(addedOrganization) {
            $scope.organizations.push(newOrganization(addedOrganization));
          });
        }

        lastOrganizations = angular.copy(organizations);

        //set root org then dump it from the list
        $scope.organizations.some(function(organization, index) {
          if (!organization.parentOrganizationId && !organization.synthetic) {
            $scope.rootOrganization = organization;
            $scope.organizations.splice(index,1);
            return true;
          }
        });
      }

      function applicationsCollectionChanged() {
        var found,
            difference = getCollectionDifference(applications, lastApplications);

        if (difference.removed) {
          difference.removed.some(function(removedApplication) {
            $scope.organizations.some(function(organization) {
              found = false;
              if (removedApplication.organizationId === organization.id) {
                organization.applications.some(function(application, applicationIndex) {
                  if (removedApplication.id === application.id) {
                    organization.applications.splice(applicationIndex, 1);
                    found = true;
                    return found;
                  }
                });
              }
              return found;
            });
          });
        }
        if (difference.added) {
          var touchedOrganizations = {};
          difference.added.forEach(function(addedApplication) {
            found = false;
            $scope.organizations.some(function(organization) {
              if (addedApplication.organizationId === organization.id) {
                found = true;
                organization.applications.push(newApplication(addedApplication));
                touchedOrganizations[organization.id] = organization;
                return found;
              }
            });
            // Create synthetic organizations for application parents which the user does not have permissions
            // These do not need to be backed by a Resource as the user cannot edit them
            if (!found) {
              var syntheticOrganization = newOrganization({
                id: addedApplication.organizationId,
                name: addedApplication.organizationName
              });
              syntheticOrganization.synthetic = true;
              syntheticOrganization.applications.push(newApplication(addedApplication));
              $scope.organizations.push(syntheticOrganization);
              touchedOrganizations[syntheticOrganization.id] = syntheticOrganization;
            }
          });

          for (var key in touchedOrganizations) {
            if (touchedOrganizations.hasOwnProperty(key)) {
              touchedOrganizations[key].isExpanded = $state.includes('management.organization',
                  {organizationId: touchedOrganizations[key].id}) || isOrganizationChildSelected(touchedOrganizations[key]);
            }
          }
        }

        lastApplications = angular.copy(applications);
      }

      function getCollectionDifference(newCollection, oldCollection) {
        var removedOwners, addedOwners;

        if (oldCollection.length > newCollection.length) {
          var newCollectionIds = {};
          newCollection.forEach(function(newOwner) {
            newCollectionIds[newOwner.id] = true;
          });
          removedOwners = oldCollection.filter(function(oldOwner) {
            return !newCollectionIds[oldOwner.id];
          });
        } else {
          var oldCollectionIds = {};
          oldCollection.forEach(function(oldOwner) {
            oldCollectionIds[oldOwner.id] = true;
          });
          addedOwners = newCollection.filter(function(newOwner) {
            return !oldCollectionIds[newOwner.id];
          });
        }

        return {
          added: addedOwners,
          removed: removedOwners
        };
      }

      function clearCollectionWatchers() {
        if (organizationWatcher) {
          organizationWatcher();
        }
        if (applicationWatcher) {
          applicationWatcher();
        }
      }

      function assignSelectedParentOrganization() {
        $scope.organizations.some(function(organization){
          if (isOrganizationChildSelected(organization)) {
            $scope.selectedParentOrganization = organization;
            return true;
          }
        });
      }

      $scope.$state = $state;
      $scope.filter = {
        value: ''
      };

      $scope.showRoot = ProductFeatures.isAvailable('root-org');

      $scope.doLoad = function() {
        delete $scope.error;
        delete $scope.rootOrganization;
        clearCollectionWatchers();

        var loadPromises = [
          organizationStore.refresh(),
          applicationStore.refresh()
        ];

        $q.all(loadPromises).then(function(results) {
          $scope.organizations = [];
          organizations = results[0];
          applications = results[1];

          organizationsCollectionChanged();
          applicationsCollectionChanged();
          assignSelectedParentOrganization();

          $scope.$on('$stateChangeSuccess', function () {
            $scope.selectedParentOrganization = null;
            assignSelectedParentOrganization();
          });

          // Apply this after first digest to prevent collection changed event on first load
          $timeout(function() {
            organizationWatcher = $scope.$watch(function() {
              return organizations.length;
            }, organizationsCollectionChanged);
            applicationWatcher = $scope.$watch(function() {
              return applications.length;
            }, applicationsCollectionChanged);
          });
        }, function(error) {
          $scope.error = error;
        });
      };

      $scope.goToOrganizationIfNotSynthetic = function(organization) {
        if (!organization.synthetic) {
          $state.go('management.organization.policies', { organizationId: organization.id});
        }
      };

      $scope.$watch('filter.value', function() {
        filter();
      }, function(error) {
        $scope.error = error;
      });

      $scope.doLoad();
    }
  ]);
}());
