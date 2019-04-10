/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
import resourceModule from '../Resource';
import CLMLocationModule from '../util/CLMLocation';
import CLMContextLocationModule from '../util/CLMContextLocation';

var storesModule = angular.module('Stores',
    [CLMLocationModule.name, CLMContextLocationModule.name, resourceModule.name]);

export default storesModule;

storesModule.service('ApplicationStore', [
  '$rootScope', 'CLMLocations', 'StoreFactory', 'LastSelectedOrganization', 'OrganizationStore',
  'store.observe.type.constant',
  function($rootScope, clmLocations, StoreFactory, LastSelectedOrganization, OrganizationStore,
           StoreObserveTypeConstant) {
    var applicationStore = StoreFactory.getStore({
      id: 'publicId',
      url: clmLocations.getApplicationsUrl(),
      type: 'application',
      transientProperties: ['organizationName', 'contact'],
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

    OrganizationStore.observe(handleParentOrganizationChanges);

    return applicationStore;

    function handleParentOrganizationChanges(type, organizations) {
      var applications = applicationStore.peek();

      for (var i = applications.length - 1; i >= 0; i--) {
        findParentOrgAndModifyEntry(applications[i], i);
      }

      function findParentOrgAndModifyEntry(application, appIndex) {
        organizations.some(function(organization) {
          if (organization.id === application.organizationId) {
            switch (type) {
              case StoreObserveTypeConstant.UPDATE:
                application.organizationName = organization.name;
                application.$getOriginal().organizationName = organization.name;
                break;
              case StoreObserveTypeConstant.DELETE:
                applicationStore._removeFromStoreByIndex(appIndex);
                break;
            }

            return true;
          }
        });
      }
    }
  }
]);

storesModule.service('OrganizationStore', [
  'CLMLocations', 'StoreFactory', function(CLMLocations, StoreFactory) {
    return StoreFactory.getStore({
      id: 'id',
      url: CLMLocations.getOrganizationsUrl(),
      type: 'organization',
      template: {
        id: null,
        name: null
      }
    });
  }
]);

storesModule.service('StageTypeStore', [
  'StoreFactory', 'CLMLocations', function(StoreFactory, CLMLocations) {
    var actionStageTypeStore = StoreFactory.getStore({
      id: 'stageTypeId',
      url: CLMLocations.getActionStageUrl()
    });
    var dashboardStageTypeStore = StoreFactory.getStore({
      id: 'stageTypeId',
      url: CLMLocations.getDashboardStageUrl()
    });
    var cliTypeStore = StoreFactory.getStore({
      id: 'stageTypeId',
      url: CLMLocations.getCliStageUrl()
    });

    return {
      'get': function() {
        return cliTypeStore.get().then(function(result) {
          return angular.copy(result);
        });
      },
      'getActionStages': function() {
        return actionStageTypeStore.get().then(function (result) {
          return addStageShortName(angular.copy(result));
        });
      },
      'getDashboardStages': function() {
        return dashboardStageTypeStore.get().then(function (result) {
          return addStageShortName(angular.copy(result));
        });
      }
    };

    function addStageShortName(result) {
      result.forEach(function (element) {
        if (element.stageTypeId === 'stage-release') {
          element.shortName = 'Stage';
        }
        else {
          element.shortName = element.stageName;
        }
      });
      return result;
    }
  }
]);

storesModule.service('PolicyStore', [
  'ConstraintStore', 'CLMLocations', 'CLMContextLocations', 'StoreFactory', '$q',
  function(constraintStore, clmLocations, clmAppLocations, StoreFactory, $q) {
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
                },
                conditionType = conditionTypes.AgeInDays;
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
            store = StoreFactory.getStore(angular.extend({ url: clmAppLocations.getPolicyUrl() },
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

storesModule.service('ConstraintStore', ['CLMLocations', 'CLMContextLocations', 'StoreFactory', '$q',
  function(clmLocations, clmAppLocations, StoreFactory, $q) {
    var conditionTypeStore = StoreFactory.getStore({
      id: 'id',
      url: clmLocations.getConditionTypeUrl()
    });

    return {
      'get': function() {
        var conditionValueTypeStore = StoreFactory.getStore({
              id: 'id',
              url: clmAppLocations.getConditionValueTypeUrl()
            }),
            conditionDeferred = $q.all([conditionTypeStore.get(), conditionValueTypeStore.get()]);
        return conditionDeferred;
      }
    };
  }
]);

storesModule.service('PolicyHierarchyStore', [
  'CLMContextLocations', 'CachedHierarchyStore', function(CLMContextLocations, CachedHierarchyStore) {
    var policyStoreTemplate = {
      template: {
        id: undefined,
        name: undefined,
        threatLevel: 5,
        constraints: [
          {
            id: '' + new Date().getTime(),
            conditions: [
              {
                conditionTypeId: 'AgeInDays',
                operator: 'older than',
                value: null
              }
            ],
            operator: 'OR'
          }
        ],
        actions: {},
        notifications: {
          userNotifications: [],
          roleNotifications: [],
          jiraNotifications: [],
          webhookNotifications: []
        }
      },
      type: 'policy',
      getUrl: CLMContextLocations.getApplicablePolicies,
      crudUrl: CLMContextLocations.getPolicyUrl,
      field: 'policiesByOwner',
      storeField: 'policies'
    };

    return CachedHierarchyStore.get(policyStoreTemplate);
  }
]);

storesModule.service('WebhookStore', [
  'CLMLocations', 'CachedStore', function(clmLocations, CachedStore) {
    var webhookStoreTemplate = {
      id: 'id',
      template: {
        id: null,
        url: null,
        secretKey: '',
        eventTypes: []
      },
      type: 'webhook',
      getUrl: clmLocations.getWebhooksUrl
    };

    return CachedStore.get(webhookStoreTemplate);
  }
]);

storesModule.service('ProprietaryConfigHierarchyStore', [
  'CLMContextLocations', 'CachedHierarchyStore', function(CLMContextLocations, CachedHierarchyStore) {
    var proprietaryConfigStoreTemplate = {
      template: {
        id: undefined,
        packages: [],
        regexes: []
      },
      type: 'proprietary configuration',
      getUrl: CLMContextLocations.getProprietaryConfigUrl,
      crudUrl: CLMContextLocations.getProprietaryConfigUrl,
      field: 'proprietaryConfigByOwners',
      storeField: 'proprietaryConfig',
      id: 'ownerId'
    };

    return CachedHierarchyStore.get(proprietaryConfigStoreTemplate);
  }
]);

/* A service which allows stores to be cached by a key, or if not provided the entity id.
 * Stores and their contents will be cached across the SPA.
 * configuration is the same as Resource except:
 *   getUrl a function that returns the store URL at the point the store is requested
 *   getKey (optional) a function that returns the key at the point the store is requested
 */
function CachedStoreFactory(StoreFactory, CLMContextLocations) {
  function CachedStore(config) {
    var store, storeKey = null;

    function refreshStore() {
      var key = config.getKey ? config.getKey() : CLMContextLocations.getEntityId();
      if (!store || key !== storeKey) {
        store = StoreFactory.getStore(angular.extend({ url: config.getUrl() }, config));
        storeKey = key;
      }

      return store;
    }

    return {
      get: function() {
        return refreshStore().get();
      },
      getById: function(requiredId) {
        return refreshStore().getById(requiredId);
      },
      refresh: function() {
        return refreshStore().refresh();
      },
      create: function() {
        return refreshStore().create();
      }
    };
  }

  return {
    get: function(config) {
      return new CachedStore(config);
    }
  };
}

storesModule.service('CachedStore', ['StoreFactory', 'CLMContextLocations', CachedStoreFactory]);

storesModule.service('CachedHierarchyStore', ['HierarchyStoreFactory', 'CLMContextLocations', CachedStoreFactory]);
