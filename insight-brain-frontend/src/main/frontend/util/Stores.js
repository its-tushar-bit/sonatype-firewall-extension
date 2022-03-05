/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import resourceModule from '../Resource';
import CLMLocationModule from '../util/CLMLocation';
import CLMContextLocationModule from '../util/CLMContextLocation';
import { fetchStageTypes } from '../stages/stagesActions';

var storesModule = angular.module('Stores', [
  CLMLocationModule.name,
  CLMContextLocationModule.name,
  resourceModule.name,
  'ngRedux',
]);

export default storesModule;

storesModule.service('ApplicationStore', [
  '$rootScope',
  'CLMLocations',
  'StoreFactory',
  'LastSelectedOrganization',
  'OrganizationStore',
  'store.observe.type.constant',
  function (
    $rootScope,
    clmLocations,
    StoreFactory,
    LastSelectedOrganization,
    OrganizationStore,
    StoreObserveTypeConstant
  ) {
    var applicationStore = StoreFactory.getStore({
      id: 'publicId',
      url: clmLocations.getApplicationsUrl(),
      type: 'application',
      transientProperties: ['organizationName', 'contact'],
      template: function () {
        var lastOrg = LastSelectedOrganization.get();
        return {
          id: null,
          publicId: null,
          name: null,
          organizationId: lastOrg.id,
          organizationName: lastOrg.name,
          contact: null,
        };
      },
    });

    OrganizationStore.observe(handleParentOrganizationChanges);

    return applicationStore;

    function handleParentOrganizationChanges(type, organizations) {
      var applications = applicationStore.peek();

      for (var i = applications.length - 1; i >= 0; i--) {
        findParentOrgAndModifyEntry(applications[i], i);
      }

      function findParentOrgAndModifyEntry(application, appIndex) {
        organizations.some(function (organization) {
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
  },
]);

storesModule.service('OrganizationStore', [
  'CLMLocations',
  'StoreFactory',
  function (CLMLocations, StoreFactory) {
    return StoreFactory.getStore({
      id: 'id',
      url: CLMLocations.getOrganizationsUrl(),
      type: 'organization',
      template: {
        id: null,
        name: null,
      },
    });
  },
]);

/**
 * Note that this module no longer actually uses Stores. Its external API never exposed that it was using stores,
 * and it has now been migrated to use the Stages stored in redux instead
 */
storesModule.service('StageTypeStore', [
  '$ngRedux',
  '$q',
  function ($ngRedux, $q) {
    const getCurrentStageState = (purpose) => $ngRedux.getState().stages[purpose];

    function stagesPromiseProvider(purpose) {
      return function () {
        const alreadyLoadedStageTypes = getCurrentStageState(purpose).stageTypes;

        if (alreadyLoadedStageTypes) {
          return $q.resolve(angular.copy(alreadyLoadedStageTypes));
        } else {
          let unsubscribe = null;

          const promise = $q(function (resolve, reject) {
            unsubscribe = $ngRedux.subscribe(function () {
              const stageState = getCurrentStageState(purpose),
                { stageTypes, error } = stageState;

              if (error) {
                reject(error);
              } else if (stageTypes) {
                resolve(angular.copy(stageTypes));
              }
            });
          }).finally(function () {
            if (unsubscribe) {
              unsubscribe();
            }
          });

          $ngRedux.dispatch(fetchStageTypes(purpose));

          return promise;
        }
      };
    }

    return {
      get: stagesPromiseProvider('cli'),
      getActionStages: stagesPromiseProvider('action'),
      getDashboardStages: stagesPromiseProvider('dashboard'),
    };
  },
]);

storesModule.service('PolicyHierarchyStore', [
  'CLMContextLocations',
  'CachedHierarchyStore',
  function (CLMContextLocations, CachedHierarchyStore) {
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
                value: null,
              },
            ],
            operator: 'OR',
          },
        ],
        actions: {},
        notifications: {
          userNotifications: [],
          roleNotifications: [],
          jiraNotifications: [],
          webhookNotifications: [],
        },
      },
      type: 'policy',
      getUrl: CLMContextLocations.getApplicablePolicies,
      crudUrl: CLMContextLocations.getPolicyUrl,
      field: 'policiesByOwner',
      storeField: 'policies',
    };

    return CachedHierarchyStore.get(policyStoreTemplate);
  },
]);

storesModule.service('WebhookStore', [
  'CLMLocations',
  'CachedStore',
  function (clmLocations, CachedStore) {
    var webhookStoreTemplate = {
      id: 'id',
      template: {
        id: null,
        url: null,
        description: null,
        secretKey: '',
        eventTypes: [],
      },
      type: 'webhook',
      getUrl: clmLocations.getWebhooksUrl,
    };

    return CachedStore.get(webhookStoreTemplate);
  },
]);

/* A service which allows stores to be cached by a key, or if not provided the entity id.
 * Stores and their contents will be cached across the SPA.
 * configuration is the same as Resource except:
 *   getUrl a function that returns the store URL at the point the store is requested
 *   getKey (optional) a function that returns the key at the point the store is requested
 */
function CachedStoreFactory(StoreFactory, CLMContextLocations) {
  function CachedStore(config) {
    var store,
      storeKey = null;

    function refreshStore() {
      var key = config.getKey ? config.getKey() : CLMContextLocations.getEntityId();
      if (!store || key !== storeKey) {
        store = StoreFactory.getStore(angular.extend({ url: config.getUrl() }, config));
        storeKey = key;
      }

      return store;
    }

    return {
      get: function () {
        return refreshStore().get();
      },
      getById: function (requiredId) {
        return refreshStore().getById(requiredId);
      },
      refresh: function () {
        return refreshStore().refresh();
      },
      create: function () {
        return refreshStore().create();
      },
    };
  }

  return {
    get: function (config) {
      return new CachedStore(config);
    },
  };
}

storesModule.service('CachedStore', ['StoreFactory', 'CLMContextLocations', CachedStoreFactory]);

storesModule.service('CachedHierarchyStore', ['HierarchyStoreFactory', 'CLMContextLocations', CachedStoreFactory]);
