/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import resourceModule from '../Resource';
import CLMLocationModule from '../util/CLMLocation';
import CLMContextLocationModule from '../utilAngular/CLMContextLocation';

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
  function ($rootScope, clmLocations, StoreFactory, LastSelectedOrganization) {
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

    return applicationStore;
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
