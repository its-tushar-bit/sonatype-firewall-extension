/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */

// global function
(function() {
  'use strict';

  var storesModule = angular.module('Stores', ['CLMLocation', 'CLMAppLocation', 'ResourceModule']);

  storesModule.service('ApplicationStore', [
    '$rootScope', 'CLMLocations', 'CLMResource', 'LastSelectedOrganization',
    function($rootScope, clmLocations, clmResource, LastSelectedOrganization) {
      var applicationStore = clmResource.getStore({
        id: 'publicId',
        url: clmLocations.getApplicationsUrl(),
        template: function() {
          var lastOrg = LastSelectedOrganization.get();
          return {
            id: null,
            publicId: null,
            name: null,
            organizationId: lastOrg.id,
            organizationName: lastOrg.name,
            contact: null
          };
        }
      });
      $rootScope.$on('organizations.delete', function() {
        applicationStore.refresh();
      });
      return applicationStore;
    }
  ]);

  storesModule.service('OrganizationStore', [
    'CLMLocations', 'CLMResource', function(CLMLocations, clmResource) {
      return clmResource.getStore({
        id: 'id',
        url: CLMLocations.getOrganizationsUrl(),
        template: {
          id: null,
          name: null
        }
      });
    }
  ]);

  storesModule.service('StageTypeStore', [
    'CLMResource', 'CLMLocations', function(CLMResource, CLMLocations) {
      var actionStageTypeStore = CLMResource.getStore({
        id: 'stageTypeId',
        url: CLMLocations.getActionStageUrl()
      });
      var dashboardStageTypeStore = CLMResource.getStore({
        id: 'stageTypeId',
        url: CLMLocations.getDashboardStageUrl()
      });
      var cliTypeStore = CLMResource.getStore({
        id: 'stageTypeId',
        url: CLMLocations.getCliStageUrl()
      });

      return {
        'get': function() {
          return cliTypeStore.get();
        },
        'getActionStages': function() {
          return actionStageTypeStore.get();
        },
        'getDashboardStages': function() {
          return dashboardStageTypeStore.get().then(function (result) {
            result.forEach(function (element) {
              if (element.stageTypeId === 'stage-release') {
                element.shortName = 'Stage';
              }
              else {
                element.shortName = element.stageName;
              }
            });
            return result;
          });
        }
      };
    }
  ]);

  storesModule.service('PolicyStore', [
    'ConstraintStore', 'CLMLocations', 'CLMAppLocations', 'CLMResource', '$q',
    function(constraintStore, clmLocations, clmAppLocations, clmResource, $q) {
      var conditionTypes = null,
      policyStoreTemplate = {
        id: 'id',
        template: function() {
          var o = {
            threatLevel: 5,
            constraints: [
              { conditions: [], operator: 'OR', id: '' + new Date().getTime() }
            ],
            actions: {}
          }, conditionType = conditionTypes.AgeInDays;
          o.constraints[0].conditions.push({
            conditionTypeId: conditionType.id,
            operator: conditionType.supportedOperators[0],
            value: null
          });
          return o;
        }
      },
      policyStores = {};

      return {
        get: function() {
          var ownerId = clmAppLocations.getEntityId(),
              store = policyStores[ownerId],
              deferred = $q.defer();
          if (!store) {
            constraintStore.get().then(function(results) {
              conditionTypes = {};
              angular.forEach(results[0], function(type) {
                conditionTypes[type.id] = type;
              });
              // Expire existing stores, prevents user from encountering stale data
              angular.forEach(policyStores, function(value, key) {
                policyStores[key] = null;
              });
              store = clmResource.getStore(angular.extend({ url: clmAppLocations.getPolicyUrl() },
                  policyStoreTemplate));
              policyStores[ownerId] = store;
              deferred.resolve(store);
            }, function(reason) {
              deferred.reject(reason);
            });
          }
          else {
            deferred.resolve(store);
          }
          return deferred.promise;
        },
        getConditionTypes: function() {
          return conditionTypes;
        }
      };
    }
  ]);

  storesModule.service('ConstraintStore', [
    'CLMLocations', 'CLMAppLocations', 'CLMResource', '$q', function(clmLocations, clmAppLocations, clmResource, $q) {
      var conditionTypeStore = clmResource.getStore({
        id: 'id',
        url: clmLocations.getConditionTypeUrl()
      });

      return {
        'get': function() {
          var conditionValueTypeStore = clmResource.getStore({
            id: 'id',
            url: clmAppLocations.getConditionValueTypeUrl()
          }),
          conditionDeferred = $q.all([conditionTypeStore.get(), conditionValueTypeStore.get()]);
          return conditionDeferred;
        }
      };
    }
  ]);

  /* A service which allows stores to be cached by a key, or if not provided the entity id.
   * Stores and their contents will be cached across the SPA.
   * configuration is the same as Resource except:
   *   getUrl a function that returns the store URL at the point the store is requested
   *   getKey (optional) a function that returns the key at the point the store is requested
   */
  function CachedStoreFactory(CLMResource, CLMAppLocations) {
    function CachedStore(config) {
      var store, stores = {};

      function refreshStore() {
        var key = config.getKey ? config.getKey() : CLMAppLocations.getEntityId();
        store = stores[key];
        if (!store) {
          store = stores[key] = CLMResource.getStore(angular.extend({ url: config.getUrl() }, config));
        }
      }

      return {
        get: function() {
          refreshStore();
          return store.get();
        },
        refresh: function() {
          refreshStore();
          return store.refresh();
        },
        create: function() {
          refreshStore();
          return store.create();
        }
      };
    }

    return {
      get: function(config) {
        return new CachedStore(config);
      }
    };
  }

  storesModule.service('CachedStore', ['CLMResource', 'CLMAppLocations', CachedStoreFactory]);

  storesModule.service('CachedHierarchyStore', ['HierarchyStore', 'CLMAppLocations', CachedStoreFactory]);
}());
